package com.health.knowledge.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.health.knowledge.data.local.dao.ArticleDao
import com.health.knowledge.data.remote.FirestoreDataSource
import com.health.knowledge.util.HealthNotificationHelper
import com.health.knowledge.util.UserPreferencesManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val firestoreDataSource: FirestoreDataSource,
    private val articleDao: ArticleDao,
    private val prefsManager: UserPreferencesManager
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val prefs = applicationContext.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
            val lastSync = prefs.getLong("last_sync_timestamp", 0L)
            val lastMetaSync = prefs.getLong("last_meta_sync_timestamp", 0L)

            val myDeviceId = prefsManager.getDeviceAuthorId()
            val myDocIds = prefsManager.getMySubmittedDocIds()

            // 1. Fetch only NEW articles from Firestore without creating duplicates
            val newArticles = if (lastSync == 0L) {
                firestoreDataSource.fetchAllArticles()
            } else {
                firestoreDataSource.fetchNewArticlesSince(lastSync)
            }

            if (newArticles.isNotEmpty()) {
                val seenDocIds = mutableSetOf<String>()
                val entities = newArticles.mapNotNull { firebaseArticle ->
                    if (firebaseArticle.firebaseDocId.isNotBlank() && !seenDocIds.add(firebaseArticle.firebaseDocId)) {
                        return@mapNotNull null
                    }
                    val existing = articleDao.findExistingCloudOrUserArticle(
                        docId = firebaseArticle.firebaseDocId,
                        title = firebaseArticle.title
                    )
                    if (existing != null) {
                        if (existing.firebaseDocId.isBlank() && firebaseArticle.firebaseDocId.isNotBlank()) {
                            articleDao.updateFirebaseDocId(existing.id, firebaseArticle.firebaseDocId)
                        }
                        null
                    } else {
                        firebaseArticle.toEntity(myDeviceId = myDeviceId, myDocIds = myDocIds)
                    }
                }
                if (entities.isNotEmpty()) {
                    articleDao.insertAll(entities)
                    articleDao.deduplicateCloudArticles()
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

            // 2. Fetch ONLY updated postMeta overrides since lastMetaSync and apply diff-checked Room updates
            val metaList = firestoreDataSource.fetchUpdatedPostMetaSince(lastMetaSync)
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
                    if (!targetEntity.isDeleted) {
                        articleDao.markDeleted(targetEntity.id)
                    }
                    continue
                }

                meta.isPinned?.let { pinned ->
                    if (pinned != targetEntity.isPinned) {
                        articleDao.updatePin(targetEntity.id, pinned)
                    }
                }

                if (meta.hasContentOverride && meta.title.isNotBlank() && meta.content.isNotBlank()) {
                    val resolvedSource = meta.source.ifBlank { targetEntity.source }
                    val resolvedPhoto = meta.photoUrl.ifBlank { targetEntity.photoPath }
                    val hasPhoto = resolvedPhoto.isNotBlank()
                    val contentChanged = meta.title != targetEntity.title ||
                        meta.content != targetEntity.content ||
                        meta.medicines != targetEntity.medicines ||
                        meta.categories != targetEntity.categories ||
                        resolvedPhoto != targetEntity.photoPath ||
                        hasPhoto != targetEntity.hasPhoto ||
                        meta.isReviewed != targetEntity.isReviewed ||
                        resolvedSource != targetEntity.source
                    if (contentChanged) {
                        val keywords = "${meta.title} ${meta.content} ${meta.medicines} ${meta.categories} $resolvedSource".lowercase()
                        articleDao.updateArticleContent(
                            id = targetEntity.id,
                            title = meta.title,
                            content = meta.content,
                            medicines = meta.medicines,
                            categories = meta.categories,
                            photoPath = resolvedPhoto,
                            hasPhoto = hasPhoto,
                            searchKeywords = keywords,
                            isReviewed = meta.isReviewed,
                            source = resolvedSource
                        )
                    }
                }

                meta.reactions?.let { remoteReactions ->
                    if (remoteReactions >= 0 && remoteReactions != targetEntity.reactions) {
                        articleDao.updateReactionsCountOnly(targetEntity.id, remoteReactions)
                    }
                }

                meta.verifiedCount?.let { remoteVerified ->
                    if (remoteVerified >= 0 && remoteVerified != targetEntity.verifiedCount) {
                        articleDao.updateVerifiedCountOnly(targetEntity.id, remoteVerified)
                    }
                }
            }

            val now = System.currentTimeMillis()
            prefs.edit()
                .putBoolean("has_completed_initial_cloud_sync", true)
                .putLong("last_sync_timestamp", now)
                .putLong("last_meta_sync_timestamp", now)
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
