package com.health.knowledge.data.repository

import com.health.knowledge.data.local.dao.ArticleDao
import com.health.knowledge.data.local.entity.ArticleEntity
import com.health.knowledge.data.remote.FirestoreDataSource
import com.health.knowledge.domain.model.Article
import com.health.knowledge.util.UserPreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ArticleRepository @Inject constructor(
    private val articleDao: ArticleDao,
    private val firestoreDataSource: FirestoreDataSource,
    private val prefsManager: UserPreferencesManager
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

    fun observeActiveCount(): Flow<Int> = articleDao.observeActiveCount()

    suspend fun getById(id: Int): ArticleEntity? = articleDao.getById(id)

    fun observeById(id: Int): Flow<ArticleEntity?> = articleDao.observeById(id)

    suspend fun updateBookmark(id: Int, bookmarked: Boolean) =
        articleDao.updateBookmark(id, bookmarked)

    suspend fun getTotalCount(): Int = articleDao.getTotalCount()

    suspend fun getFirebaseSyncedCount(): Int = articleDao.getFirebaseSyncedCount()

    // ── Per-IP Global Reaction (Optimistic Local + Atomic Firestore Sync) ────
    suspend fun toggleReaction(article: Article) {
        val wantReact = !article.isReacted
        val optimisticCount = if (wantReact) {
            article.reactions + 1
        } else {
            (article.reactions - 1).coerceAtLeast(0)
        }

        // 1. Optimistic local update for instant UI feedback
        articleDao.updateReaction(
            id = article.id,
            reactions = optimisticCount,
            isReacted = wantReact
        )

        // 2. Atomic Firestore Transaction to count globally across all users/IPs
        withTimeoutOrNull(5000L) {
            val ipHash = prefsManager.getClientIpHash()
            val txResult = firestoreDataSource.toggleReactionGlobalTransaction(
                postKey = article.postKey,
                firebaseDocId = article.firebaseDocId,
                ipHash = ipHash,
                wantReact = wantReact,
                fallbackBaseCount = article.reactions
            )
            txResult.getOrNull()?.let { (globalCount, reactedState) ->
                articleDao.updateReaction(
                    id = article.id,
                    reactions = globalCount,
                    isReacted = reactedState
                )
            }
        }
    }

    /**
     * Refreshes the live global reaction count from Firestore when viewing a post.
     */
    suspend fun refreshLiveReactions(article: Article) {
        withTimeoutOrNull(4000L) {
            val ipHash = prefsManager.getClientIpHash()
            val (globalCount, hasIpReacted) = firestoreDataSource.fetchLiveReactionStatus(
                postKey = article.postKey,
                ipHash = ipHash
            )
            if (globalCount != null) {
                articleDao.updateReaction(
                    id = article.id,
                    reactions = globalCount,
                    isReacted = hasIpReacted ?: article.isReacted
                )
            }
        }
    }

    // ── Admin: Add new article (saves locally + syncs to Firebase) ───────────
    suspend fun addArticleToFirebase(
        title: String,
        content: String,
        medicines: String,
        categories: String,
        photoUrl: String,
        date: String,
        isPinned: Boolean = false
    ): Result<String> {
        val now = System.currentTimeMillis()
        val cleanPhotoUrl = photoUrl.trim()
        val firestoreArticle = FirestoreDataSource.FirestoreArticle(
            title = title,
            content = content,
            date = date,
            medicines = medicines,
            categories = categories,
            hasPhoto = cleanPhotoUrl.isNotBlank(),
            photoUrl = cleanPhotoUrl,
            isPinned = isPinned,
            isReviewed = true,
            createdAt = now
        )

        val remoteDocId = withTimeoutOrNull(5000L) {
            firestoreDataSource.addArticle(firestoreArticle).getOrNull()
        } ?: ""

        val entity = firestoreArticle.copy(firebaseDocId = remoteDocId).toEntity()
        articleDao.insert(entity)

        return Result.success(remoteDocId)
    }

    // ── Admin: Pin / Unpin globally ──────────────────────────────────────────
    suspend fun togglePinArticle(article: Article) {
        val newPin = !article.isPinned
        articleDao.updatePin(article.id, newPin)

        withTimeoutOrNull(4000L) {
            firestoreDataSource.setPinnedGlobal(
                postKey = article.postKey,
                firebaseDocId = article.firebaseDocId,
                isPinned = newPin
            )
        }
    }

    // ── Admin: Edit / Review post globally (including CDN photoUrl) ──────────
    suspend fun updateArticleGlobally(
        article: Article,
        title: String,
        content: String,
        medicines: String,
        categories: String,
        photoUrl: String,
        isReviewed: Boolean = true
    ) {
        val cleanPhotoUrl = photoUrl.trim()
        val keywords = "$title $content $medicines $categories".lowercase()
        articleDao.updateArticleContent(
            id = article.id,
            title = title,
            content = content,
            medicines = medicines,
            categories = categories,
            photoPath = cleanPhotoUrl,
            hasPhoto = cleanPhotoUrl.isNotBlank(),
            searchKeywords = keywords,
            isReviewed = isReviewed
        )

        withTimeoutOrNull(4500L) {
            firestoreDataSource.updateArticleGlobal(
                postKey = article.postKey,
                firebaseDocId = article.firebaseDocId,
                title = title,
                content = content,
                medicines = medicines,
                categories = categories,
                photoUrl = cleanPhotoUrl,
                isReviewed = isReviewed
            )
        }
    }

    // ── Admin: Global Delete ─────────────────────────────────────────────────
    suspend fun deleteArticleGlobally(article: Article) {
        articleDao.markDeleted(article.id)

        withTimeoutOrNull(4500L) {
            firestoreDataSource.deleteArticleGlobal(
                postKey = article.postKey,
                firebaseDocId = article.firebaseDocId
            )
        }
    }
}
