package com.health.knowledge.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.reflect.TypeToken
import com.health.knowledge.data.local.dao.ArticleDao
import com.health.knowledge.data.local.dao.TibbeNabawiDao
import com.health.knowledge.data.local.entity.ArticleEntity
import com.health.knowledge.data.local.entity.TibbeNabawiEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeds the Room database with bundled JSON data on first install.
 * This data works completely OFFLINE — no internet needed.
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    @ApplicationContext private val context: Context,
    private val articleDao: ArticleDao,
    private val tibbeNabawiDao: TibbeNabawiDao
) {
    private val _isSeeding = MutableStateFlow(false)
    val isSeeding: StateFlow<Boolean> = _isSeeding.asStateFlow()

    fun isInitialSeedCompleted(): Boolean {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("has_completed_initial_seed", false)
    }

    suspend fun seedIfEmpty() {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        val count = articleDao.getTotalCount()
        if (count == 0) {
            _isSeeding.value = true
            try {
                val articles = loadBundledArticles()
                articleDao.insertAll(articles)
                prefs.edit()
                    .putBoolean("has_completed_initial_seed", true)
                    .putBoolean("has_normalized_bundled_v1", true)
                    .apply()
            } finally {
                _isSeeding.value = false
            }
        } else {
            if (!prefs.getBoolean("has_completed_initial_seed", false)) {
                prefs.edit().putBoolean("has_completed_initial_seed", true).apply()
            }
            if (!prefs.getBoolean("has_normalized_bundled_v1", false)) {
                // Run one-time normalization & deduplication instead of rewriting Room on every app launch
                articleDao.normalizeBundledTimestamps()
                articleDao.deduplicateCloudArticles()
                prefs.edit().putBoolean("has_normalized_bundled_v1", true).apply()
            }
            upgradeBundledArticlesParagraphsIfNeeded()
        }

        seedTibbeNabawiIfEmpty()
    }

    private suspend fun seedTibbeNabawiIfEmpty() {
        try {
            val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
            val hasUpgradedTibbe = prefs.getBoolean("has_upgraded_tibbe_v7", false)
            val tibbeCount = tibbeNabawiDao.getCount()
            if (tibbeCount == 0 || !hasUpgradedTibbe) {
                val tibbePosts = loadBundledTibbeNabawiPosts()
                if (tibbePosts.isNotEmpty()) {
                    tibbeNabawiDao.insertAll(tibbePosts)
                    prefs.edit().putBoolean("has_upgraded_tibbe_v7", true).apply()
                }
            }
        } catch (_: Exception) {
            // Ignore fallback
        }
    }

    private suspend fun upgradeBundledArticlesParagraphsIfNeeded() {
        val prefs = context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
        val hasUpgraded = prefs.getBoolean("has_upgraded_article_paragraphs_v2", false)
        if (!hasUpgraded) {
            try {
                val freshArticles = loadBundledArticles()
                val existingActive = articleDao.getAllActiveSnapshot()
                val bundledExisting = existingActive.filter { !it.syncedFromFirebase && !it.isMine }
                val freshMap = freshArticles.associateBy { it.messageId.ifBlank { it.title.trim() } }

                for (art in bundledExisting) {
                    val key = art.messageId.ifBlank { art.title.trim() }
                    val fresh = freshMap[key]
                    if (fresh != null && (fresh.content != art.content || !art.content.contains("\n"))) {
                        articleDao.updateArticleContent(
                            id = art.id,
                            title = art.title,
                            content = fresh.content,
                            medicines = art.medicines,
                            categories = art.categories,
                            photoPath = art.photoPath,
                            hasPhoto = art.hasPhoto,
                            searchKeywords = fresh.searchKeywords,
                            isReviewed = art.isReviewed,
                            source = art.source
                        )
                    }
                }
                prefs.edit().putBoolean("has_upgraded_article_paragraphs_v2", true).apply()
            } catch (_: Exception) {
                // Ignore fallback
            }
        }
    }

    private fun loadBundledArticles(): List<ArticleEntity> {
        return try {
            val json = context.assets
                .open("health_articles.json")
                .bufferedReader()
                .use { it.readText() }
                .removePrefix("\uFEFF") // strip UTF-8 BOM if present

            val type = object : TypeToken<List<BundledArticle>>() {}.type
            val bundled: List<BundledArticle> = Gson().fromJson(json, type)

            bundled.mapIndexed { idx, item -> item.toEntity(idx) }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Mirrors the JSON structure from health_articles.json
    data class BundledArticle(
        val id: Int = 0,
        val messageId: String = "",
        val title: String = "",
        val content: String = "",
        val date: String = "",
        val reactions: Int = 0,
        val medicines: String = "",
        val categories: String = "",
        val hasPhoto: Boolean = false,
        val photoPath: String = "",
        val syncedFromFirebase: Boolean = false,
        val firebaseDocId: String = ""
    ) {
        fun toEntity(index: Int): ArticleEntity {
            val formatted = ArticleTextFormatter.formatBengaliParagraphs(content)
            return ArticleEntity(
                messageId = messageId,
                title = title,
                content = formatted,
                date = date,
                reactions = reactions,
                medicines = medicines,
                categories = categories,
                hasPhoto = hasPhoto,
                photoPath = photoPath,
                searchKeywords = buildKeywords(formatted),
                syncedFromFirebase = false,
                firebaseDocId = "",
                // Historical base timestamp (Nov 2023) + index so newly added posts are always newest
                createdAt = 1700000000000L + (id.takeIf { it > 0 } ?: index).toLong()
            )
        }

        private fun buildKeywords(cleanContent: String): String {
            return "$title $cleanContent $medicines $categories"
                .lowercase()
                .split("\\s+".toRegex())
                .filter { it.length > 1 }
                .distinct()
                .joinToString(" ")
        }
    }

    private fun loadBundledTibbeNabawiPosts(): List<TibbeNabawiEntity> {
        return try {
            val json = context.assets
                .open("tibbe_nabawi_posts.json")
                .bufferedReader()
                .use { it.readText() }
                .removePrefix("\uFEFF")

            val type = object : TypeToken<List<BundledTibbeNabawiPost>>() {}.type
            val bundled: List<BundledTibbeNabawiPost> = Gson().fromJson(json, type)

            bundled.map { it.toEntity() }
        } catch (_: Exception) {
            emptyList()
        }
    }

    data class BundledTibbeNabawiPost(
        val id: Int = 0,
        val title: String = "",
        val food_name: String? = null,
        val arabic_name: String? = null,
        val quotes: JsonElement? = null,
        val arabic_text: String? = null,
        val hadith_arabic: String? = null,
        val arabic_hadith: String? = null,
        val category: String = "",
        val diseases: List<String>? = null,
        val properties: BundledProperties? = null,
        val usage_instructions: String? = null,
        val details: String = "",
        val source_pages: List<Int>? = null
    ) {
        data class BundledProperties(
            val benefits: String? = null,
            val nature: String? = null,
            val taste: String? = null
        )

        private fun extractQuotesRaw(element: JsonElement?): String? {
            if (element == null || element.isJsonNull) return null
            if (element.isJsonPrimitive) {
                val s = element.asString.trim()
                return if (s.isNotBlank()) s else null
            }
            return element.toString()
        }

        private fun extractQuotesSearchText(element: JsonElement?): String {
            if (element == null || element.isJsonNull) return ""
            if (element.isJsonPrimitive) return element.asString
            val sb = StringBuilder()
            if (element.isJsonArray) {
                for (item in element.asJsonArray) {
                    if (item.isJsonPrimitive) {
                        sb.append(" ").append(item.asString)
                    } else if (item.isJsonObject) {
                        for ((_, v) in item.asJsonObject.entrySet()) {
                            if (v != null && v.isJsonPrimitive) {
                                sb.append(" ").append(v.asString)
                            }
                        }
                    }
                }
            } else if (element.isJsonObject) {
                for ((_, v) in element.asJsonObject.entrySet()) {
                    if (v != null && v.isJsonPrimitive) {
                        sb.append(" ").append(v.asString)
                    }
                }
            }
            return sb.toString()
        }

        fun toEntity(): TibbeNabawiEntity {
            val quotesRaw = extractQuotesRaw(quotes)
            val quotesSearchText = extractQuotesSearchText(quotes)
            val formattedDetails = ArticleTextFormatter.formatBengaliParagraphs(details)
            val finalDetails = formattedDetails
            val extraArabic = (arabic_text ?: hadith_arabic ?: arabic_hadith)?.trim()?.takeIf { it.isNotBlank() }
            val diseasesList = diseases ?: emptyList()
            val diseasesStr = diseasesList.joinToString(",")
            val sourcePagesStr = (source_pages ?: emptyList()).joinToString(", ")
            val food = food_name.orEmpty().trim()
            val arabic = arabic_name?.trim()?.takeIf { it.isNotBlank() }
            val benefitsText = properties?.benefits?.let { ArticleTextFormatter.formatBengaliParagraphs(it) }
            val natureText = properties?.nature?.trim()?.takeIf { it.isNotBlank() }
            val tasteText = properties?.taste?.trim()?.takeIf { it.isNotBlank() }
            val usageText = usage_instructions?.let { ArticleTextFormatter.formatBengaliParagraphs(it) }

            val searchKeywords = "$title $food ${arabic.orEmpty()} $quotesSearchText ${extraArabic.orEmpty()} $category $diseasesStr ${benefitsText.orEmpty()} $finalDetails"
                .lowercase()
                .split("\\s+".toRegex())
                .filter { it.length > 1 }
                .distinct()
                .joinToString(" ")

            return TibbeNabawiEntity(
                id = id,
                title = title.trim(),
                foodName = food,
                arabicName = arabic,
                quotes = quotesRaw,
                category = category.trim(),
                diseases = diseasesStr,
                benefits = benefitsText,
                nature = natureText,
                taste = tasteText,
                usageInstructions = usageText,
                details = finalDetails,
                sourcePages = sourcePagesStr,
                isBookmarked = false,
                searchKeywords = searchKeywords
            )
        }
    }
}
