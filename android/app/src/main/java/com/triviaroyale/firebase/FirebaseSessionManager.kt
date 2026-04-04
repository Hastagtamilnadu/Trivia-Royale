package com.triviaroyale.firebase

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.triviaroyale.BuildConfig

enum class SessionStatus {
    LOADING,
    CONFIG_REQUIRED,
    SIGNED_OUT,
    AUTHENTICATED
}

data class SessionState(
    val status: SessionStatus = SessionStatus.LOADING,
    val user: FirebaseUser? = null,
    val isWorking: Boolean = false,
    val errorMessage: String? = null
)

@Suppress("DEPRECATION")
class FirebaseSessionManager(private val context: Context) {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state.asStateFlow()

    suspend fun bootstrapSession() {
        if (!FirebaseBootstrap.ensureInitialized(context)) {
            _state.value = SessionState(
                status = SessionStatus.CONFIG_REQUIRED,
                errorMessage = "Missing ${FirebaseBootstrap.missingKeys().joinToString()}"
            )
            return
        }

        val currentUser = auth.currentUser
        _state.value = if (currentUser != null) {
            SessionState(status = SessionStatus.AUTHENTICATED, user = currentUser)
        } else {
            SessionState(status = SessionStatus.SIGNED_OUT)
        }
    }

    fun buildGoogleSignInIntent(): Intent {
        require(FirebaseBootstrap.isGoogleSignInConfigured()) {
            "firebaseWebClientId is missing from local.properties"
        }
        return GoogleSignIn
            .getClient(context, googleOptions())
            .signInIntent
    }

    suspend fun finishGoogleSignIn(data: Intent?) {
        if (!FirebaseBootstrap.isConfigured()) {
            _state.value = SessionState(
                status = SessionStatus.CONFIG_REQUIRED,
                errorMessage = "Firebase config is incomplete"
            )
            return
        }
        if (!FirebaseBootstrap.isGoogleSignInConfigured()) {
            _state.value = SessionState(
                status = SessionStatus.SIGNED_OUT,
                errorMessage = "Google sign-in is not configured for this Firebase project yet."
            )
            return
        }

        _state.value = _state.value.copy(isWorking = true, errorMessage = null)
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(data).awaitTask()
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            val authResult = auth.signInWithCredential(credential).awaitTask()
            val user = checkNotNull(authResult.user) { "Google sign-in returned no Firebase user" }
            _state.value = SessionState(status = SessionStatus.AUTHENTICATED, user = user)
        } catch (error: Exception) {
            _state.value = SessionState(
                status = SessionStatus.SIGNED_OUT,
                errorMessage = formatAuthError(error, "Google sign-in failed")
            )
        }
    }

    fun signOut() {
        if (FirebaseBootstrap.isConfigured()) {
            auth.signOut()
            if (FirebaseBootstrap.isGoogleSignInConfigured()) {
                runCatching {
                    GoogleSignIn.getClient(context, googleOptions()).signOut()
                }
            }
        }
        _state.value = SessionState(status = SessionStatus.SIGNED_OUT)
    }

    private fun formatAuthError(error: Exception, fallback: String): String {
        val message = error.message.orEmpty()
        return when {
            message.contains("CONFIGURATION_NOT_FOUND", ignoreCase = true) ->
                "Firebase Authentication is not initialized for this project yet."
            message.contains("OPERATION_NOT_ALLOWED", ignoreCase = true) ->
                "This Firebase sign-in method is not enabled for the project yet."
            message.contains("Unknown calling package name", ignoreCase = true) ->
                "Google Play services could not verify this app build. Register the app's SHA fingerprints in Firebase and reinstall a signed build."
            message.isNotBlank() -> message
            else -> fallback
        }
    }

    private fun googleOptions(): GoogleSignInOptions {
        return GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(BuildConfig.FIREBASE_WEB_CLIENT_ID)
            .requestEmail()
            .build()
    }
}
