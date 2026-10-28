package com.health.knowledge.util

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.health.knowledge.data.local.dao.ArticleDao
import com.health.knowledge.data.local.entity.ArticleEntity
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
    private val articleDao: ArticleDao
) {
    private val _isSeeding = MutableStateFlow(false)
    val isSeeding: StateFlow<Boolean> = _isSeeding.asStateFlow()

    suspend fun seedIfEmpty() {
        val count = articleDao.getTotalCount()
        if (count == 0) {
            _isSeeding.value = true
            try {
                val articles = loadBundledArticles()
                articleDao.insertAll(articles)
            } finally {
                _isSeeding.value = false
            }
        } else {
            // Normalize existing bundled articles' timestamps and clean up any duplicate cloud posts
            articleDao.normalizeBundledTimestamps()
            articleDao.deduplicateCloudArticles()
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
        fun toEntity(index: Int) = ArticleEntity(
            messageId = messageId,
            title = title,
            content = content,
            date = date,
            reactions = reactions,
            medicines = medicines,
            categories = categories,
            hasPhoto = hasPhoto,
            photoPath = photoPath,
            searchKeywords = buildKeywords(),
            syncedFromFirebase = false,
            firebaseDocId = "",
            // Historical base timestamp (Nov 2023) + index so newly added posts are always newest
            createdAt = 1700000000000L + (id.takeIf { it > 0 } ?: index).toLong()
        )

        private fun buildKeywords(): String {
            return "$title $content $medicines $categories"
                .lowercase()
                .split("\\s+".toRegex())
                .filter { it.length > 1 }
                .distinct()
                .joinToString(" ")
        }
    }
}
