package com.triviaroyale.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.triviaroyale.firebase.FirebaseCloudRepository
import com.triviaroyale.firebase.QuizCategoryMetadata

data class QuizBankOverride(
    val genre: String,
    val category: String,
    val questions: List<Question>,
    val updatedAt: Long = 0L
)

private data class CachedQuizBank(
    val bankId: String,
    val version: Int,
    val updatedAt: Long,
    val questions: List<Question>
)

private data class CachedQuizContent(
    val lastCatalogSyncAt: Long = 0L,
    val categoryMetadata: Map<String, QuizCategoryMetadata> = emptyMap(),
    val banks: Map<String, CachedQuizBank> = emptyMap()
)

object QuizContentManager {
    private const val PREFS_NAME = "quiz_content_cache"
    private const val KEY_CACHE = "quiz_content_cache_v2"

    private val gson = Gson()

    @Volatile
    private var initialized = false

    @Volatile
    private var cache = CachedQuizContent()

    private lateinit var appContext: Context

    fun initialize(context: Context) {
        if (initialized) return

        synchronized(this) {
            if (initialized) return
            appContext = context.applicationContext
            cache = loadCache()
            initialized = true
        }
    }

    fun getBankOverride(genre: String, category: String): List<Question>? {
        if (!initialized) return null
        return cache.banks[bankKey(genre, category)]?.questions
    }

    fun getOverridesForGenre(genre: String): Map<String, List<Question>> {
        if (!initialized) return emptyMap()
        val prefix = "$genre::"
        return cache.banks.entries
            .filter { it.key.startsWith(prefix) }
            .associate { entry ->
                entry.key.removePrefix(prefix) to entry.value.questions
            }
    }

    fun getKnownCategoriesForGenre(genre: String): List<String> {
        if (!initialized) return emptyList()
        return cache.categoryMetadata.values
            .filter { it.genre == genre }
            .map { it.category }
            .distinct()
            .sorted()
    }

    fun getKnownQuestionCount(genre: String, category: String): Int {
        if (!initialized) return 0
        return cache.categoryMetadata[bankKey(genre, category)]?.questionCount ?: 0
    }

    fun getKnownGenreQuestionCount(genre: String): Int {
        if (!initialized) return 0
        return cache.categoryMetadata.values
            .filter { it.genre == genre }
            .sumOf { it.questionCount }
    }

    suspend fun syncCatalog(cloudRepository: FirebaseCloudRepository): Boolean {
        if (!initialized) return false

        val updatedMetadata = cloudRepository.fetchUpdatedQuizCategoryMetadata(cache.lastCatalogSyncAt)
        if (updatedMetadata.isEmpty()) {
            return false
        }

        val mergedMetadata = cache.categoryMetadata.toMutableMap()
        updatedMetadata.forEach { metadata ->
            mergedMetadata[bankKey(metadata.genre, metadata.category)] = metadata
        }

        val newestUpdatedAt = updatedMetadata.maxOfOrNull { it.updatedAt } ?: cache.lastCatalogSyncAt
        cache = cache.copy(
            lastCatalogSyncAt = maxOf(cache.lastCatalogSyncAt, newestUpdatedAt),
            categoryMetadata = mergedMetadata
        )
        saveCache(cache)
        return true
    }

    suspend fun ensureCategoryFresh(
        cloudRepository: FirebaseCloudRepository,
        genre: String,
        category: String
    ): Boolean {
        if (!initialized) return false

        syncCatalog(cloudRepository)
        val key = bankKey(genre, category)
        val metadata = cache.categoryMetadata[key] ?: return false
        val cachedBank = cache.banks[key]
        if (cachedBank != null && cachedBank.version >= metadata.version) {
            return false
        }

        val override = cloudRepository.fetchQuizBankOverride(metadata.bankId) ?: return false
        val questions = override.questions.filter(::isValidQuestion)
        if (questions.isEmpty()) {
            return false
        }

        val nextBanks = cache.banks.toMutableMap()
        nextBanks[key] = CachedQuizBank(
            bankId = metadata.bankId,
            version = metadata.version,
            updatedAt = metadata.updatedAt,
            questions = questions
        )
        cache = cache.copy(banks = nextBanks)
        saveCache(cache)
        return true
    }

    suspend fun ensureGenreFresh(
        cloudRepository: FirebaseCloudRepository,
        genre: String,
        categories: List<String>
    ): Boolean {
        if (!initialized) return false

        syncCatalog(cloudRepository)
        var changed = false
        categories.forEach { category ->
            changed = ensureCategoryFresh(cloudRepository, genre, category) || changed
        }
        return changed
    }

    private fun isValidQuestion(question: Question): Boolean {
        return question.question.isNotBlank() &&
            question.options.size == 4 &&
            question.options.all { it.isNotBlank() } &&
            question.answer in 0..3
    }

    private fun bankKey(genre: String, category: String): String = "$genre::$category"

    private fun prefs() = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun loadCache(): CachedQuizContent {
        val json = prefs().getString(KEY_CACHE, null) ?: return CachedQuizContent()
        return runCatching {
            val type = object : TypeToken<CachedQuizContent>() {}.type
            gson.fromJson<CachedQuizContent>(json, type) ?: CachedQuizContent()
        }.getOrDefault(CachedQuizContent())
    }

    private fun saveCache(content: CachedQuizContent) {
        prefs().edit()
            .putString(KEY_CACHE, gson.toJson(content))
            .apply()
    }
}
