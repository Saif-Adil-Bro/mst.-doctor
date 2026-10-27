package com.health.knowledge.data.repository

import com.health.knowledge.data.local.dao.ArticleDao
import com.health.knowledge.data.local.entity.ArticleEntity
import com.health.knowledge.data.remote.FirestoreDataSource
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val articleDao: ArticleDao,
    private val firestoreDataSource: FirestoreDataSource
) {
    // ── Read from LOCAL Room DB (always fast, always offline) ────────────────
    fun searchArticles(query: String): Flow<List<ArticleEntity>> {
        return if (query.isBlank()) {
            articleDao.getAllArticles()
        } else {
            articleDao.searchArticles(query.trim())
        }
    }

    fun getAllArticles(): Flow<List<ArticleEntity>> = articleDao.getAllArticles()

    fun getByCategory(category: String): Flow<List<ArticleEntity>> =
        articleDao.getByCategory(category)

    fun getBookmarked(): Flow<List<ArticleEntity>> = articleDao.getBookmarked()

    suspend fun getById(id: Int): ArticleEntity? = articleDao.getById(id)

    suspend fun updateBookmark(id: Int, bookmarked: Boolean) =
        articleDao.updateBookmark(id, bookmarked)

    suspend fun getTotalCount(): Int = articleDao.getTotalCount()

    suspend fun getFirebaseSyncedCount(): Int = articleDao.getFirebaseSyncedCount()

    // ── Admin: Add new article to Firebase (syncs to all devices) ───────────
    suspend fun addArticleToFirebase(
        title: String,
        content: String,
        medicines: String,
        categories: String,
        date: String
    ): Result<String> {
        val article = FirestoreDataSource.FirestoreArticle(
            title = title,
            content = content,
            date = date,
            medicines = medicines,
            categories = categories,
            createdAt = System.currentTimeMillis()
        )
        val result = firestoreDataSource.addArticle(article)

        // Also save locally immediately (don't wait for background sync)
        if (result.isSuccess) {
            val entity = article.copy(firebaseDocId = result.getOrDefault("")).toEntity()
            articleDao.insert(entity)
        }
        return result
    }
}
