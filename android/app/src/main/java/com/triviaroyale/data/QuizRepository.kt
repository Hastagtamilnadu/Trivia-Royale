package com.triviaroyale.data

import com.triviaroyale.firebase.FirebaseCloudRepository

data class Question(
    val question: String,
    val options: List<String>,
    val answer: Int, // index of correct answer
    val difficulty: Double = 0.5
)

/**
 * Quiz repository that delegates to the dynamic server-backed system.
 * Maintains the same public API surface for backward compatibility.
 *
 * All question-fetching methods are now suspend functions that try:
 * 1. Dynamic server-cached questions (via DynamicQuizRepository)
 * 2. QuizContentManager overrides (existing Firestore override system)
 * 3. Fallback embedded questions
 */
object QuizRepository {

    // Cached cloud repository reference — set during app init
    @Volatile
    var cloudRepository: FirebaseCloudRepository? = null

    /**
     * Fetches questions for a genre (suspend — call from coroutine).
     */
    suspend fun getQuestionsSuspend(genre: String? = null, count: Int = 10): List<Question> {
        val resolvedGenre = genre ?: "General Knowledge"

        // Check overrides first
        val overridePool = QuizContentManager.getOverridesForGenre(resolvedGenre).values.flatten()
        if (overridePool.isNotEmpty()) {
            return overridePool.shuffled().take(count.coerceAtMost(overridePool.size))
        }

        return DynamicQuizRepository.getQuestions(cloudRepository, resolvedGenre, count)
    }

    /**
     * Fetches questions for a category (suspend).
     */
    suspend fun getQuestionsForCategorySuspend(genre: String, category: String, count: Int = 10): List<Question> {
        // Check overrides first
        val overrideQuestions = QuizContentManager.getBankOverride(genre, category)
        if (!overrideQuestions.isNullOrEmpty()) {
            return overrideQuestions.shuffled().take(count.coerceAtMost(overrideQuestions.size))
        }

        return DynamicQuizRepository.getQuestionsForCategory(cloudRepository, genre, category, count)
    }

    /**
     * Fetches lightning round questions (suspend).
     */
    suspend fun getLightningRoundQuestionsSuspend(count: Int = 30): List<Question> {
        return DynamicQuizRepository.getLightningRoundQuestions(cloudRepository, count)
    }

    /**
     * Fetches daily challenge questions (suspend).
     */
    suspend fun getDailyChallengeQuestionsSuspend(count: Int = 8): List<Question> {
        return DynamicQuizRepository.getDailyChallengeQuestions(cloudRepository, count)
    }

    // ── Synchronous wrappers (for backward compatibility) ────────────────────
    // These use fallback questions when called synchronously (no server fetch)

    fun getQuestions(genre: String? = null, count: Int = 10): List<Question> {
        val resolvedGenre = genre ?: "General Knowledge"

        // Check overrides
        val overridePool = QuizContentManager.getOverridesForGenre(resolvedGenre).values.flatten()
        if (overridePool.isNotEmpty()) {
            return overridePool.shuffled().take(count.coerceAtMost(overridePool.size))
        }

        // Use fallback for sync calls
        return QuizFallbackQuestions.getQuestions(resolvedGenre, null, count)
    }

    fun getQuestionsForCategory(genre: String, category: String, count: Int = 10): List<Question> {
        val overrideQuestions = QuizContentManager.getBankOverride(genre, category)
        if (!overrideQuestions.isNullOrEmpty()) {
            return overrideQuestions.shuffled().take(count.coerceAtMost(overrideQuestions.size))
        }

        return QuizFallbackQuestions.getQuestions(genre, category, count)
    }

    fun getLightningRoundQuestions(count: Int = 30): List<Question> {
        return QuizFallbackQuestions.getAllGenreQuestions().shuffled().take(count)
    }

    fun getDailyChallengeQuestions(count: Int = 8): List<Question> {
        return QuizFallbackQuestions.getQuestions("General Knowledge", null, count)
    }

    // ── Metadata methods (unchanged API) ────────────────────────────────────

    suspend fun getCategoryNamesSuspend(genre: String): List<String> {
        val local = QuizContentManager.getKnownCategoriesForGenre(genre)
        if (local.isNotEmpty()) return local

        return DynamicQuizRepository.getCategoryNames(cloudRepository, genre)
    }

    fun getCategoryNames(genre: String): List<String> {
        val local = QuizContentManager.getKnownCategoriesForGenre(genre)
        if (local.isNotEmpty()) return local

        return QuizFallbackQuestions.getCategoryNames(genre)
    }

    fun getQuestionCountForCategory(genre: String, category: String): Int {
        return DynamicQuizRepository.getQuestionCountForCategory(genre, category)
    }

    fun getTopPlayerCategoryQuestionCount(genre: String): Int {
        return DynamicQuizRepository.getQuestionCountForGenre(genre)
    }

    data class Genre(
        val name: String,
        val icon: String, // Material icon name
        val quizCount: Int
    )

    val genres: List<Genre>
        get() = buildList {
            if (isIplSeasonActive()) {
                add(Genre("IPL", "sports_cricket", getTopPlayerCategoryQuestionCount("IPL")))
            }
            addAll(
                listOf(
                    Genre("Sports", "sports_basketball", getTopPlayerCategoryQuestionCount("Sports")),
                    Genre("Movies", "movie", getTopPlayerCategoryQuestionCount("Movies")),
                    Genre("Science", "science", getTopPlayerCategoryQuestionCount("Science")),
                    Genre("History", "history_edu", getTopPlayerCategoryQuestionCount("History")),
                    Genre("Music", "music_note", getTopPlayerCategoryQuestionCount("Music")),
                    Genre("Tech", "computer", getTopPlayerCategoryQuestionCount("Tech")),
                    Genre("Entertainment", "theater_comedy", getTopPlayerCategoryQuestionCount("Entertainment")),
                    Genre("Geography", "public", getTopPlayerCategoryQuestionCount("Geography")),
                    Genre("Art", "palette", getTopPlayerCategoryQuestionCount("Art")),
                    Genre("Literature", "menu_book", getTopPlayerCategoryQuestionCount("Literature")),
                    Genre("General Knowledge", "school", getTopPlayerCategoryQuestionCount("General Knowledge")),
                    Genre("Pop Culture", "trending_up", getTopPlayerCategoryQuestionCount("Pop Culture"))
                )
            )
        }
}
