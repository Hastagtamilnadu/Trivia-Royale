package com.triviaroyale.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.triviaroyale.firebase.FirebaseCloudRepository

/**
 * Dynamic quiz question repository that fetches questions from the server,
 * caches them locally, and provides deduplication across sessions.
 *
 * Flow: Local cache → Server fetch → Fallback questions
 */
object DynamicQuizRepository {
    private const val TAG = "DynamicQuizRepo"
    private const val PREFS_NAME = "dynamic_quiz_cache"
    private const val KEY_QUESTION_CACHE = "question_cache_v1"
    private const val KEY_METADATA_CACHE = "metadata_cache_v1"
    private const val KEY_LAST_METADATA_SYNC = "last_metadata_sync"
    private const val KEY_SEEN_HASHES = "seen_hashes_v1"

    // Cache questions for 24 hours
    private const val CACHE_TTL_MS = 24L * 60 * 60 * 1000
    // Metadata refresh every 6 hours
    private const val METADATA_TTL_MS = 6L * 60 * 60 * 1000
    // Keep track of last 500 seen question hashes for dedup
    private const val MAX_SEEN_HASHES = 500

    private val gson = Gson()

    @Volatile
    private var initialized = false
    private lateinit var appContext: Context

    // In-memory caches
    private var questionCache = mutableMapOf<String, CachedGenreQuestions>()
    private var metadataCache = mutableMapOf<String, CachedGenreMetadata>()
    private var seenHashes = mutableSetOf<String>()

    private data class CachedGenreQuestions(
        val genre: String,
        val category: String?, // null = all categories mixed
        val questions: List<CachedQuestion>,
        val fetchedAt: Long
    )

    data class CachedQuestion(
        val hash: String,
        val question: String,
        val options: List<String>,
        val answer: Int,
        val difficulty: Double,
        val genre: String,
        val category: String
    )

    private data class CachedGenreMetadata(
        val genre: String,
        val totalQuestions: Int,
        val categories: List<String>,
        val fetchedAt: Long
    )

    fun initialize(context: Context) {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            appContext = context.applicationContext
            loadCaches()
            initialized = true
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Fetches questions for a genre. Tries cache first, then server, then fallback.
     */
    suspend fun getQuestions(
        cloudRepository: FirebaseCloudRepository?,
        genre: String,
        count: Int = 10
    ): List<Question> {
        return getQuestionsInternal(cloudRepository, genre, null, count)
    }

    /**
     * Fetches questions for a specific genre + category.
     */
    suspend fun getQuestionsForCategory(
        cloudRepository: FirebaseCloudRepository?,
        genre: String,
        category: String,
        count: Int = 10
    ): List<Question> {
        return getQuestionsInternal(cloudRepository, genre, category, count)
    }

    /**
     * Fetches questions for lightning round (multi-genre mix).
     */
    suspend fun getLightningRoundQuestions(
        cloudRepository: FirebaseCloudRepository?,
        count: Int = 30
    ): List<Question> {
        val genresToSample = listOf(
            "Sports", "Movies", "Science", "History", "Music",
            "Tech", "Entertainment", "Geography", "Art", "Literature",
            "General Knowledge", "Pop Culture"
        )
        val perGenre = (count / genresToSample.size).coerceAtLeast(2)
        val allQuestions = mutableListOf<Question>()

        for (genre in genresToSample) {
            val questions = getQuestionsInternal(cloudRepository, genre, null, perGenre)
            allQuestions.addAll(questions)
        }

        return allQuestions.shuffled().take(count)
    }

    /**
     * Fetches questions for the daily challenge.
     */
    suspend fun getDailyChallengeQuestions(
        cloudRepository: FirebaseCloudRepository?,
        count: Int = 8
    ): List<Question> {
        return getQuestionsInternal(cloudRepository, "General Knowledge", null, count)
    }

    /**
     * Returns cached category names for a genre, or fetches from server.
     */
    suspend fun getCategoryNames(
        cloudRepository: FirebaseCloudRepository?,
        genre: String
    ): List<String> {
        val cached = metadataCache[genre]
        if (cached != null && !isMetadataStale(cached.fetchedAt)) {
            return cached.categories
        }

        // Try to refresh from server
        if (cloudRepository != null) {
            try {
                refreshMetadata(cloudRepository)
                return metadataCache[genre]?.categories.orEmpty()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to refresh metadata", e)
            }
        }

        return cached?.categories ?: QuizFallbackQuestions.getCategoryNames(genre)
    }

    /**
     * Returns question count for a genre (from metadata cache).
     */
    fun getQuestionCountForGenre(genre: String): Int {
        val serverCount = metadataCache[genre]?.totalQuestions ?: 0
        val fallbackCount = QuizFallbackQuestions.getQuestionCountForGenre(genre)
        // Always show at least 10 so the UI never displays "0 Quizzes"
        return maxOf(serverCount, fallbackCount, 10)
    }

    /**
     * Returns question count for a specific category.
     */
    fun getQuestionCountForCategory(genre: String, category: String): Int {
        val cached = questionCache[cacheKey(genre, category)]
        if (cached != null) return cached.questions.size

        // Estimate from genre total
        val genreTotal = getQuestionCountForGenre(genre)
        val categoryCount = metadataCache[genre]?.categories?.size ?: 4
        return genreTotal / categoryCount.coerceAtLeast(1)
    }

    /**
     * Refreshes metadata from server (genre question counts, category lists).
     */
    suspend fun refreshMetadata(cloudRepository: FirebaseCloudRepository) {
        try {
            val entries = cloudRepository.fetchDynamicQuizMetadata()
            val now = System.currentTimeMillis()
            for (entry in entries) {
                metadataCache[entry.genre] = CachedGenreMetadata(
                    genre = entry.genre,
                    totalQuestions = entry.totalQuestions,
                    categories = entry.categories,
                    fetchedAt = now
                )
            }
            saveMetadataCache()
            prefs().edit()
                .putLong(KEY_LAST_METADATA_SYNC, now)
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Metadata refresh failed", e)
        }
    }

    /**
     * Clears all local caches. Called on logout.
     */
    fun clearCaches() {
        questionCache.clear()
        metadataCache.clear()
        seenHashes.clear()
        if (initialized) {
            prefs().edit().clear().apply()
        }
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private suspend fun getQuestionsInternal(
        cloudRepository: FirebaseCloudRepository?,
        genre: String,
        category: String?,
        count: Int
    ): List<Question> {
        val key = cacheKey(genre, category)

        // 1. Try local cache first (if not stale)
        val cached = questionCache[key]
        if (cached != null && !isCacheStale(cached.fetchedAt)) {
            val available = cached.questions
                .filter { it.hash !in seenHashes }
                .shuffled()
                .take(count)
            if (available.size >= count / 2) {
                markSeen(available)
                return available.map(::toQuestion)
            }
        }

        // 2. Try server fetch
        if (cloudRepository != null) {
            try {
                val result = cloudRepository.fetchDynamicQuizQuestions(
                    genre = genre,
                    category = category,
                    count = count.coerceAtLeast(20), // Fetch extra for cache
                    excludeHashes = seenHashes.toList().takeLast(100)
                )
                if (result.questions.isNotEmpty()) {
                    val cachedQuestions = result.questions.map { dq ->
                        CachedQuestion(
                            hash = dq.hash,
                            question = dq.question,
                            options = dq.options,
                            answer = dq.answer,
                            difficulty = dq.difficulty,
                            genre = dq.genre,
                            category = dq.category
                        )
                    }

                    // Merge with existing cache
                    val existing = questionCache[key]?.questions.orEmpty()
                    val merged = (cachedQuestions + existing)
                        .distinctBy { it.hash }
                        .take(200) // Cap cached questions per genre

                    questionCache[key] = CachedGenreQuestions(
                        genre = genre,
                        category = category,
                        questions = merged,
                        fetchedAt = System.currentTimeMillis()
                    )
                    saveQuestionCache()

                    val selected = cachedQuestions
                        .filter { it.hash !in seenHashes }
                        .shuffled()
                        .take(count)
                    markSeen(selected)
                    return selected.map(::toQuestion)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Server fetch failed for $genre/$category", e)
            }
        }

        // 3. Use stale cache if available
        if (cached != null && cached.questions.isNotEmpty()) {
            val available = cached.questions.shuffled().take(count)
            markSeen(available)
            return available.map(::toQuestion)
        }

        // 4. Fallback to embedded questions
        Log.w(TAG, "Using fallback questions for $genre")
        return QuizFallbackQuestions.getQuestions(genre, category, count)
    }

    private fun toQuestion(cached: CachedQuestion): Question {
        return Question(
            question = cached.question,
            options = cached.options,
            answer = cached.answer,
            difficulty = cached.difficulty
        )
    }

    private fun markSeen(questions: List<CachedQuestion>) {
        for (q in questions) {
            seenHashes.add(q.hash)
        }
        // Trim oldest hashes if over limit
        if (seenHashes.size > MAX_SEEN_HASHES) {
            val trimmed = seenHashes.toList().takeLast(MAX_SEEN_HASHES).toMutableSet()
            seenHashes = trimmed
        }
        saveSeenHashes()
    }

    private fun isCacheStale(fetchedAt: Long): Boolean {
        return System.currentTimeMillis() - fetchedAt > CACHE_TTL_MS
    }

    private fun isMetadataStale(fetchedAt: Long): Boolean {
        return System.currentTimeMillis() - fetchedAt > METADATA_TTL_MS
    }

    private fun cacheKey(genre: String, category: String?): String {
        return if (category.isNullOrBlank()) genre else "$genre::$category"
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    private fun prefs() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadCaches() {
        // Load question cache
        val questionsJson = prefs().getString(KEY_QUESTION_CACHE, null)
        if (questionsJson != null) {
            runCatching {
                val type = object : TypeToken<Map<String, CachedGenreQuestions>>() {}.type
                val loaded = gson.fromJson<Map<String, CachedGenreQuestions>>(questionsJson, type)
                if (loaded != null) questionCache = loaded.toMutableMap()
            }
        }

        // Load metadata cache
        val metadataJson = prefs().getString(KEY_METADATA_CACHE, null)
        if (metadataJson != null) {
            runCatching {
                val type = object : TypeToken<Map<String, CachedGenreMetadata>>() {}.type
                val loaded = gson.fromJson<Map<String, CachedGenreMetadata>>(metadataJson, type)
                if (loaded != null) metadataCache = loaded.toMutableMap()
            }
        }

        // Load seen hashes
        val seenJson = prefs().getString(KEY_SEEN_HASHES, null)
        if (seenJson != null) {
            runCatching {
                val type = object : TypeToken<Set<String>>() {}.type
                val loaded = gson.fromJson<Set<String>>(seenJson, type)
                if (loaded != null) seenHashes = loaded.toMutableSet()
            }
        }
    }

    private fun saveQuestionCache() {
        prefs().edit()
            .putString(KEY_QUESTION_CACHE, gson.toJson(questionCache))
            .apply()
    }

    private fun saveMetadataCache() {
        prefs().edit()
            .putString(KEY_METADATA_CACHE, gson.toJson(metadataCache))
            .apply()
    }

    private fun saveSeenHashes() {
        prefs().edit()
            .putString(KEY_SEEN_HASHES, gson.toJson(seenHashes))
            .apply()
    }
}
