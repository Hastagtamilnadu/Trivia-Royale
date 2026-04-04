package com.triviaroyale.data

import com.triviaroyale.firebase.FirebaseCloudRepository

data class GenreTopPlayerSnapshot(
    val genre: String,
    val displayName: String,
    val bestScore: Int,
    val accuracy: Int
)

object GenreLeaderboardSessionStore {
    @Volatile
    private var cached: Map<String, GenreTopPlayerSnapshot> = emptyMap()

    suspend fun refresh(@Suppress("UNUSED_PARAMETER") cloudRepository: FirebaseCloudRepository) {
        cached = emptyMap()
    }

    fun getTopPlayer(genre: String): GenreTopPlayerSnapshot? {
        return cached[genre]
    }

    fun clear() {
        cached = emptyMap()
    }
}
