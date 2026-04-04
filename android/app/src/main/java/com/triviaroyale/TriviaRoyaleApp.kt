package com.triviaroyale

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.triviaroyale.ads.QuizInterstitialManager
import com.triviaroyale.ads.QuizRewardedAdManager
import com.google.firebase.auth.FirebaseUser
import com.triviaroyale.data.GameState
import com.triviaroyale.data.QuizContentManager
import com.triviaroyale.data.QuizRepository
import com.triviaroyale.data.DynamicQuizRepository
import com.triviaroyale.firebase.DeviceIdentityManager
import com.triviaroyale.firebase.FirebaseBootstrap
import com.triviaroyale.firebase.FirebaseCloudRepository
import com.triviaroyale.firebase.FirebaseSessionManager
import com.triviaroyale.firebase.SessionStatus
import com.triviaroyale.security.SecurityEnvironment
import com.triviaroyale.ui.screens.AchievementsScreen
import com.triviaroyale.ui.screens.ClanDiscoveryScreen
import com.triviaroyale.ui.screens.ClansScreen
import com.triviaroyale.ui.screens.ExploreGenresScreen
import com.triviaroyale.ui.screens.GenreQuizScreen
import com.triviaroyale.ui.screens.HelpScreen
import com.triviaroyale.ui.screens.HomeScreen
import com.triviaroyale.ui.screens.LightningQuizScreen
import com.triviaroyale.ui.screens.ProfileScreen
import com.triviaroyale.ui.screens.PrivacyPolicyScreen
import com.triviaroyale.ui.screens.SettingsScreen
import com.triviaroyale.ui.screens.SignInScreen
import com.triviaroyale.ui.screens.SplashScreen
import com.triviaroyale.ui.screens.TasksScreen
import com.triviaroyale.ui.screens.TermsScreen
import com.triviaroyale.ui.screens.UsernameSetupScreen
import com.triviaroyale.ui.screens.WalletScreen
import com.triviaroyale.ui.theme.Cyan400
import com.triviaroyale.ui.theme.OnSurfaceVariant
import com.triviaroyale.ui.theme.Primary
import com.triviaroyale.ui.theme.Purple500
import com.triviaroyale.ui.theme.Surface
import com.triviaroyale.ui.theme.Zinc800
import com.triviaroyale.ui.theme.Zinc950
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Filled.Home)
    object Clans : Screen("clans?tab=season", "Clans", Icons.Filled.EmojiEvents)
    object Profile : Screen("profile", "Profile", Icons.Filled.Person)
}

@Composable
fun TriviaRoyaleApp() {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var loadedGameState by remember { mutableStateOf<GameState?>(null) }
    val sessionManager = remember { FirebaseSessionManager(appContext) }
    val cloudRepository = remember {
        FirebaseCloudRepository(appContext).also { repo ->
            QuizRepository.cloudRepository = repo
        }
    }
    val quizInterstitialManager = remember { QuizInterstitialManager(appContext) }
    val quizRewardedAdManager = remember { QuizRewardedAdManager(appContext) }
    val deviceIdentityManager = remember { DeviceIdentityManager(appContext) }
    val blockedEnvironment = remember { SecurityEnvironment.blocksSensitiveCloudFeatures() }
    val sessionState by sessionManager.state.collectAsState()
    var isPreparingAuthenticatedSession by remember { mutableStateOf(false) }
    var needsUsernameSetup by remember { mutableStateOf(false) }
    var suggestedUsername by remember { mutableStateOf("") }
    var usernameSetupError by remember { mutableStateOf<String?>(null) }
    var isSavingUsername by remember { mutableStateOf(false) }
    var authScreenReady by remember { mutableStateOf(false) }
    var postAuthError by remember { mutableStateOf<String?>(null) }
    var lastUploadedLocalSaveAt by remember { mutableStateOf(0L) }
    var canWriteCloudBackup by remember { mutableStateOf(false) }

    LaunchedEffect(appContext) {
        sessionManager.bootstrapSession()
    }

    LaunchedEffect(quizRewardedAdManager) {
        quizRewardedAdManager.preloadIfNeeded()
    }

    LaunchedEffect(appContext) {
        loadedGameState = withContext(Dispatchers.IO) {
            QuizContentManager.initialize(appContext)
            GameState(appContext)
        }
    }

    val gameState = loadedGameState ?: run {
        SplashScreen()
        return
    }
    val appState by gameState.state.collectAsState()

    suspend fun syncProfile(user: FirebaseUser, state: GameState.State = gameState.currentState): Result<Long> {
        return runCatching {
            cloudRepository.syncProfile(user, state)
        }
    }

    suspend fun refreshWallet(uid: String = gameState.currentState.uid): Result<Unit> {
        return runCatching {
            if (uid.isBlank()) return@runCatching
            val syncResult = cloudRepository.syncCoinBalance(gameState.buildCoinSecuritySnapshot())
            gameState.applyValidatedCoinBalance(syncResult.acceptedCoins, syncResult.suspicious)
            // Also sync active abilities
            runCatching {
                val abilities = cloudRepository.fetchActiveAbilities()
                gameState.importActiveAbilities(abilities.map { info ->
                    GameState.ActiveAbility(
                        id = info.id,
                        effect = info.effect,
                        name = info.name,
                        activatedAt = info.activatedAt,
                        expiresAt = info.expiresAt
                    )
                })
            }
        }
    }

    suspend fun saveCloudBackupIfNeeded(
        state: GameState.State = gameState.currentState,
        force: Boolean = false
    ) {
        if (!canWriteCloudBackup || state.uid.isBlank()) {
            return
        }
        val localSaveAt = state.lastLocalSaveAt
        if (!force && localSaveAt <= lastUploadedLocalSaveAt) {
            return
        }
        runCatching {
            cloudRepository.syncCoinBalance(gameState.buildCoinSecuritySnapshot())
        }.onSuccess { result ->
            gameState.applyValidatedCoinBalance(result.acceptedCoins, result.suspicious)
        }
        runCatching {
            cloudRepository.saveGameBackup(gameState.currentState)
        }.onSuccess {
            lastUploadedLocalSaveAt = maxOf(lastUploadedLocalSaveAt, gameState.currentState.lastLocalSaveAt)
        }
    }

    suspend fun deleteAccount(): Result<Unit> {
        return runCatching {
            cloudRepository.deleteUserAccount()
            gameState.resetCompletely()
            sessionManager.signOut()
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        scope.launch {
            sessionManager.finishGoogleSignIn(result.data)
        }
    }

    LaunchedEffect(sessionState.status, sessionState.user?.uid) {
        if (sessionState.status != SessionStatus.AUTHENTICATED) {
            isPreparingAuthenticatedSession = false
            needsUsernameSetup = false
            suggestedUsername = ""
            authScreenReady = false
            canWriteCloudBackup = false
            usernameSetupError = null
            isSavingUsername = false
            return@LaunchedEffect
        }

        isPreparingAuthenticatedSession = true
        authScreenReady = false
        canWriteCloudBackup = false
        usernameSetupError = null
        postAuthError = null
        if (blockedEnvironment) {
            postAuthError = "Sensitive cloud features are blocked on emulators."
            sessionManager.signOut()
            isPreparingAuthenticatedSession = false
            return@LaunchedEffect
        }
        val user = sessionState.user ?: run {
            isPreparingAuthenticatedSession = false
            return@LaunchedEffect
        }
        val hadMeaningfulLocalProgress = gameState.hasMeaningfulLocalProgress()

        fun failProtectedSession(message: String) {
            postAuthError = message
            canWriteCloudBackup = false
            sessionManager.signOut()
            isPreparingAuthenticatedSession = false
        }

        val remoteProfile = runCatching { cloudRepository.loadProfile(user.uid) }.getOrNull()
        gameState.activateAuthenticatedProfile(
            uid = user.uid,
            email = user.email,
            remoteDisplayName = remoteProfile?.displayName?.takeIf { it.isNotBlank() },
            photoUrl = remoteProfile?.photoUrl ?: user.photoUrl?.toString()
        )

        val remoteBackupResult = runCatching { cloudRepository.loadGameBackup() }
        val remoteBackupError = remoteBackupResult.exceptionOrNull()
        if (remoteBackupError != null && !hadMeaningfulLocalProgress) {
            if (FirebaseBootstrap.isDebugBuild()) {
                android.util.Log.w(
                    "TriviaRoyaleApp",
                    "loadGameBackup failed in debug (continuing without backup): ${remoteBackupError.message}"
                )
            } else {
                failProtectedSession(
                    remoteBackupError.message ?: "Could not restore your protected profile. Sign in again."
                )
                return@LaunchedEffect
            }
        }
        val remoteBackup = remoteBackupResult.getOrNull()
        var importedRemoteBackup = false
        remoteBackup
            ?.takeIf {
                !gameState.hasMeaningfulLocalProgress() ||
                    it.updatedAt > gameState.currentState.lastLocalSaveAt
            }
            ?.let { backup ->
                importedRemoteBackup = gameState.importCloudStateJson(
                    stateJson = backup.stateJson,
                    updatedAt = backup.updatedAt,
                    uid = user.uid,
                    displayName = remoteProfile?.displayName ?: user.displayName,
                    email = user.email,
                    photoUrl = remoteProfile?.photoUrl ?: user.photoUrl?.toString()
                )
            }

        suggestedUsername = GameState.sanitizeUsername(
            remoteProfile?.displayName ?: user.displayName.orEmpty()
        )
        needsUsernameSetup = gameState.needsUsernameSetup()

        val deviceId = runCatching { deviceIdentityManager.getDeviceId() }.getOrNull()
        if (!deviceId.isNullOrBlank()) {
            val registrationResult = runCatching { cloudRepository.registerDevice(deviceId) }
            val registrationError = registrationResult.exceptionOrNull()
            if (registrationError != null && cloudRepository.isProtectedCloudFailure(registrationError)) {
                failProtectedSession(
                    registrationError.message ?: "This build could not be verified for protected cloud features."
                )
                return@LaunchedEffect
            }
            val registration = registrationResult.getOrNull()
            if (registration?.blocked == true) {
                postAuthError = "This device already has the maximum number of accounts."
                sessionManager.signOut()
                isPreparingAuthenticatedSession = false
                return@LaunchedEffect
            }
        }

        runCatching { QuizContentManager.syncCatalog(cloudRepository) }
        val walletRefreshResult = refreshWallet(user.uid)
        val walletRefreshError = walletRefreshResult.exceptionOrNull()
        if (walletRefreshError != null && cloudRepository.isProtectedCloudFailure(walletRefreshError)) {
            failProtectedSession(
                walletRefreshError.message ?: "This build could not be verified for protected cloud features."
            )
            return@LaunchedEffect
        }

        if (!needsUsernameSetup) {
            syncProfile(user, gameState.currentState)
        }
        // Prefetch the verified daily board in the background so the Clans area
        // can render its Today tab instantly once that UI is wired in.
        scope.launch {
            runCatching { cloudRepository.fetchVerifiedDailyLeaderboard() }
        }
        if (importedRemoteBackup) {
            lastUploadedLocalSaveAt = gameState.currentState.lastLocalSaveAt
        }
        canWriteCloudBackup = remoteBackupError == null
        isPreparingAuthenticatedSession = false
        authScreenReady = true
    }

    DisposableEffect(lifecycleOwner, sessionState.status, sessionState.user?.uid) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && sessionState.status == SessionStatus.AUTHENTICATED) {
                scope.launch {
                    gameState.syncVolatileProgress()
                    saveCloudBackupIfNeeded()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(sessionState.status, sessionState.user?.uid, authScreenReady, appState.lastLocalSaveAt) {
        if (sessionState.status != SessionStatus.AUTHENTICATED || !authScreenReady || appState.uid.isBlank()) {
            return@LaunchedEffect
        }
        delay(5000)
        saveCloudBackupIfNeeded(appState)
    }

    when (sessionState.status) {
        SessionStatus.LOADING -> SplashScreen()

        SessionStatus.CONFIG_REQUIRED,
        SessionStatus.SIGNED_OUT -> {
            SignInScreen(
                isWorking = sessionState.isWorking,
                errorMessage = if (blockedEnvironment) {
                    "Sensitive cloud features are blocked on emulators."
                } else {
                    postAuthError ?: sessionState.errorMessage
                },
                missingConfigKeys = if (sessionState.status == SessionStatus.CONFIG_REQUIRED) {
                    com.triviaroyale.firebase.FirebaseBootstrap.missingKeys()
                } else {
                    emptyList()
                },
                googleSignInEnabled = com.triviaroyale.firebase.FirebaseBootstrap.isConfigured() &&
                    com.triviaroyale.firebase.FirebaseBootstrap.isGoogleSignInConfigured() &&
                    !blockedEnvironment,
                onGoogleSignIn = {
                    runCatching { sessionManager.buildGoogleSignInIntent() }
                        .onSuccess { googleSignInLauncher.launch(it) }
                }
            )
        }

        SessionStatus.AUTHENTICATED -> {
            when {
                !authScreenReady || isPreparingAuthenticatedSession -> SplashScreen()
                needsUsernameSetup -> UsernameSetupScreen(
                    suggestedUsername = suggestedUsername,
                    isWorking = isSavingUsername,
                    errorMessage = usernameSetupError,
                    onSaveUsername = { username ->
                        scope.launch {
                            val user = sessionState.user ?: return@launch
                            isSavingUsername = true
                            usernameSetupError = null
                            gameState.setUsername(username)
                            syncProfile(user)
                                .onSuccess {
                                    scope.launch {
                                        refreshWallet(user.uid)
                                        saveCloudBackupIfNeeded(force = true)
                                    }
                                    authScreenReady = true
                                    needsUsernameSetup = false
                                }
                                .onFailure { error ->
                                    usernameSetupError = error.message ?: "Could not save username right now."
                                }
                            isSavingUsername = false
                        }
                    }
                )

                else -> TriviaRoyaleShell(
                    gameState = gameState,
                    cloudRepository = cloudRepository,
                    quizInterstitialManager = quizInterstitialManager,
                    quizRewardedAdManager = quizRewardedAdManager,
                    onDeleteAccount = { deleteAccount() },
                    onLogOut = {
                        gameState.resetCompletely()
                        sessionManager.signOut()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TriviaRoyaleShell(
    gameState: GameState,
    cloudRepository: FirebaseCloudRepository,
    quizInterstitialManager: QuizInterstitialManager,
    quizRewardedAdManager: QuizRewardedAdManager,
    onDeleteAccount: suspend () -> Result<Unit>,
    onLogOut: () -> Unit,
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavScreens = listOf(Screen.Home, Screen.Clans, Screen.Profile)
    val showBars = currentRoute in listOf(
        "home",
        "clans?tab={tab}",
        "clanDiscovery",
        "tasks",
        "profile",
        "exploreGenres",
        "achievements",
        "wallet",
        "settings",
        "help",
        "privacyPolicy"
    )

    Scaffold(
        containerColor = Surface,
        topBar = {
            if (showBars) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Zinc950.copy(alpha = 0.9f))
                        .drawBehind {
                            drawLine(
                                color = Color(0xFF27272A),
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp)
                        .height(56.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Filled.LocalFireDepartment,
                                contentDescription = null,
                                tint = Purple500,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                "Trivia Royale",
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    brush = Brush.horizontalGradient(listOf(Purple500, Cyan400))
                                )
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (currentRoute == "home") {
                                Surface(
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                                    color = Zinc800.copy(alpha = 0.5f),
                                    onClick = { navController.navigate("tasks") }
                                ) {
                                    Text(
                                        "Tasks",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Surface(
                                    shape = androidx.compose.foundation.shape.RoundedCornerShape(50),
                                    color = Zinc800.copy(alpha = 0.5f),
                                    onClick = { navController.navigate("wallet") }
                                ) {
                                    Text(
                                        "Shop",
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (showBars) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = Zinc950.copy(alpha = 0.95f),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                        )
                        .navigationBarsPadding()
                ) {
                    HorizontalDivider(thickness = 1.dp, color = Color(0xFF27272A))
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        bottomNavScreens.forEach { screen ->
                            val isSelected = when {
                                screen.route == "home" -> currentRoute in listOf("home", "exploreGenres", "wallet", "tasks")
                                screen.route.startsWith("clans") -> currentRoute in listOf("clans?tab={tab}", "clanDiscovery")
                                screen.route == "profile" -> currentRoute in listOf("profile", "achievements", "settings", "help", "privacyPolicy", "terms")
                                else -> currentRoute == screen.route
                            }
                            NavigationBarItem(
                                icon = {
                                    Icon(
                                        screen.icon,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        screen.title.uppercase(),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            letterSpacing = 1.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    )
                                },
                                selected = isSelected,
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFFF1F5F9),
                                    selectedTextColor = Color(0xFFF1F5F9),
                                    unselectedIconColor = Color(0xFF737373),
                                    unselectedTextColor = Color(0xFF737373),
                                    indicatorColor = Color(0xFF334155).copy(alpha = 0.5f)
                                ),
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            inclusive = false
                                            saveState = false
                                        }
                                        launchSingleTop = true
                                        restoreState = false
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("home") {
                HomeScreen(
                    gameState = gameState,
                    quizRewardedAdManager = quizRewardedAdManager,
                    navController = navController
                )
            }
            composable(
                "clans?tab={tab}",
                arguments = listOf(
                    navArgument("tab") {
                        type = NavType.StringType
                        defaultValue = "season"
                    }
                )
            ) { backStackEntry ->
                ClansScreen(
                    gameState = gameState,
                    cloudRepository = cloudRepository,
                    navController = navController,
                    initialTab = backStackEntry.arguments?.getString("tab") ?: "season"
                )
            }
            composable("clanDiscovery") {
                ClanDiscoveryScreen(
                    cloudRepository = cloudRepository,
                    navController = navController
                )
            }
            composable("profile") {
                ProfileScreen(gameState = gameState, cloudRepository = cloudRepository, navController = navController)
            }
            composable("lightningQuiz") {
                LightningQuizScreen(
                    gameState = gameState,
                    cloudRepository = cloudRepository,
                    quizInterstitialManager = quizInterstitialManager,
                    quizRewardedAdManager = quizRewardedAdManager,
                    navController = navController
                )
            }
            composable("exploreGenres") {
                ExploreGenresScreen(
                    gameState = gameState,
                    quizRewardedAdManager = quizRewardedAdManager,
                    navController = navController
                )
            }
            composable(
                "genreQuiz/{genre}?autostart={autostart}&returnToWar={returnToWar}",
                arguments = listOf(
                    navArgument("genre") { type = NavType.StringType },
                    navArgument("autostart") {
                        type = NavType.BoolType
                        defaultValue = false
                    },
                    navArgument("returnToWar") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val rawGenre = backStackEntry.arguments?.getString("genre") ?: "General Knowledge"
                val genre = Uri.decode(rawGenre)
                val autoStart = backStackEntry.arguments?.getBoolean("autostart") ?: false
                val returnToWar = backStackEntry.arguments?.getBoolean("returnToWar") ?: false
                GenreQuizScreen(
                    genre = genre,
                    gameState = gameState,
                    cloudRepository = cloudRepository,
                    quizInterstitialManager = quizInterstitialManager,
                    quizRewardedAdManager = quizRewardedAdManager,
                    navController = navController,
                    autoStart = autoStart,
                    returnToWar = returnToWar
                )
            }
            composable("achievements") {
                AchievementsScreen(gameState = gameState, navController = navController)
            }
            composable("wallet") {
                WalletScreen(
                    gameState = gameState,
                    cloudRepository = cloudRepository,
                    navController = navController
                )
            }
            composable("tasks") {
                TasksScreen(
                    gameState = gameState,
                    cloudRepository = cloudRepository,
                    quizRewardedAdManager = quizRewardedAdManager,
                    navController = navController
                )
            }
            composable("settings") {
                SettingsScreen(
                    gameState = gameState,
                    navController = navController,
                    onOpenPrivacyPolicy = { navController.navigate("privacyPolicy") },
                    onOpenTerms = { navController.navigate("terms") },
                    onLogOut = onLogOut
                )
            }
            composable("help") {
                HelpScreen(
                    navController = navController,
                    onDeleteAccount = onDeleteAccount
                )
            }
            composable("privacyPolicy") {
                PrivacyPolicyScreen(navController = navController)
            }
            composable("terms") {
                TermsScreen(navController = navController)
            }
        }
    }
}
