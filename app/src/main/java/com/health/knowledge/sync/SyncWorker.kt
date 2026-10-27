package com.health.knowledge.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.health.knowledge.data.local.dao.ArticleDao
import com.health.knowledge.data.remote.FirestoreDataSource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Background worker that syncs new Firebase articles to local Room DB.
 *
 * HOW IT WORKS:
 * 1. Runs automatically every 6 hours (when internet available)
 * 2. Fetches only NEW articles from Firestore (after last sync time)
 * 3. Saves them to Room DB — now available OFFLINE forever
 * 4. User sees updated content even without internet next time
 */
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

            // Fetch only articles newer than last sync
            val newArticles = if (lastSync == 0L) {
                firestoreDataSource.fetchAllArticles()
            } else {
                firestoreDataSource.fetchNewArticlesSince(lastSync)
            }

            if (newArticles.isNotEmpty()) {
                val entities = newArticles.mapNotNull { firebaseArticle ->
                    // Skip if already exists in local DB
                    val exists = articleDao.countByFirebaseDocId(firebaseArticle.firebaseDocId) > 0
                    if (exists) null else firebaseArticle.toEntity()
                }
                articleDao.insertAll(entities)
            }

            // Save last sync time
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

        /**
         * Schedule periodic sync every 6 hours, only when internet is available.
         * This is the KEY method to call at app startup.
         */
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
                ExistingPeriodicWorkPolicy.KEEP,  // Don't restart if already scheduled
                syncRequest
            )
        }

        /**
         * Trigger an immediate one-time sync (e.g., on app open).
         */
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
