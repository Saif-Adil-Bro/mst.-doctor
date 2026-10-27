package com.health.knowledge.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.health.knowledge.data.local.dao.ArticleDao
import com.health.knowledge.data.remote.FirestoreDataSource
import com.health.knowledge.util.HealthNotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val firestoreDataSource: FirestoreDataSource,
    private val articleDao: ArticleDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = applicationContext.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
            val lastSync = prefs.getLong("last_sync_timestamp", 0L)

            // 1. Fetch new articles from Firestore
            val newArticles = if (lastSync == 0L) {
                firestoreDataSource.fetchAllArticles()
            } else {
                firestoreDataSource.fetchNewArticlesSince(lastSync)
            }

            if (newArticles.isNotEmpty()) {
                val entities = newArticles.mapNotNull { firebaseArticle ->
                    val exists = articleDao.countByFirebaseDocId(firebaseArticle.firebaseDocId) > 0
                    if (exists) null else firebaseArticle.toEntity()
                }
                if (entities.isNotEmpty()) {
                    articleDao.insertAll(entities)
                    if (lastSync > 0L) {
                        val firstApproved = entities.firstOrNull { it.isReviewed && !it.isDraft }
                        if (firstApproved != null) {
                            val inserted = articleDao.getByFirebaseDocId(firstApproved.firebaseDocId)
                            val targetId = inserted?.id ?: 0
                            HealthNotificationHelper.showNewPostNotification(
                                context = applicationContext,
                                articleId = targetId,
                                title = firstApproved.title,
                                content = firstApproved.content
                            )
                        }
                    }
                }
            }

            // 2. Fetch global postMeta overrides (pins, edits, global deletes, reactions, cover photos)
            val metaList = firestoreDataSource.fetchAllPostMeta()
            for (meta in metaList) {
                val targetEntity = when {
                    meta.postKey.startsWith("fb_") -> {
                        val docId = meta.postKey.removePrefix("fb_")
                        articleDao.getByFirebaseDocId(docId)
                    }
                    meta.postKey.startsWith("bundled_") -> {
                        val keyPart = meta.postKey.removePrefix("bundled_")
                        articleDao.getByMessageId(keyPart)
                            ?: keyPart.toIntOrNull()?.let { articleDao.getById(it) }
                    }
                    else -> null
                } ?: continue

                if (meta.isDeleted) {
                    articleDao.markDeleted(targetEntity.id)
                    continue
                }

                meta.isPinned?.let { pinned ->
                    articleDao.updatePin(targetEntity.id, pinned)
                }

                if (meta.hasContentOverride && meta.title.isNotBlank() && meta.content.isNotBlank()) {
                    val keywords = "${meta.title} ${meta.content} ${meta.medicines} ${meta.categories}".lowercase()
                    val resolvedPhoto = meta.photoUrl.ifBlank { targetEntity.photoPath }
                    articleDao.updateArticleContent(
                        id = targetEntity.id,
                        title = meta.title,
                        content = meta.content,
                        medicines = meta.medicines,
                        categories = meta.categories,
                        photoPath = resolvedPhoto,
                        hasPhoto = resolvedPhoto.isNotBlank(),
                        searchKeywords = keywords,
                        isReviewed = meta.isReviewed
                    )
                }

                meta.reactions?.let { remoteReactions ->
                    if (remoteReactions >= 0) {
                        articleDao.updateReactionsCountOnly(targetEntity.id, remoteReactions)
                    }
                }
            }

            prefs.edit()
                .putLong("last_sync_timestamp", System.currentTimeMillis())
                .apply()

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "HealthKnowledge_SyncWorker"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
            )
        }

        fun syncNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val syncRequest = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueue(syncRequest)
        }
    }
}
