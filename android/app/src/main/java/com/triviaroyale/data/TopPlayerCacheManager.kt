package com.triviaroyale.data

import android.content.Context
import com.google.gson.Gson
import com.triviaroyale.firebase.FirebaseCloudRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TopPlayerSpotlight(
    val displayName: String,
    val rp: Int,
    val accuracy: Int,
    val cachedForDay: String
)

object TopPlayerCacheManager {
    private const val PREFS_NAME = "top_player_cache"
    private const val KEY_DAY = "day"
    private const val KEY_JSON = "json"

    private val gson = Gson()

    @Volatile
    private var initialized = false

    private lateinit var appContext: Context

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            appContext = context.applicationContext
            initialized = true
        }
    }

    suspend fun refreshIfNeeded(@Suppress("UNUSED_PARAMETER") cloudRepository: FirebaseCloudRepository): Boolean {
        if (!initialized) return false
        return false
    }

    fun getCached(): TopPlayerSpotlight? {
        if (!initialized) return null
        val json = prefs().getString(KEY_JSON, null) ?: return null
        return runCatching { gson.fromJson(json, TopPlayerSpotlight::class.java) }.getOrNull()
    }

    private fun prefs() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun todayKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }
}
