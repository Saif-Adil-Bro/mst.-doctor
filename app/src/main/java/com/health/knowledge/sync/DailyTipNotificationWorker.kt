package com.health.knowledge.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.health.knowledge.data.local.dao.ArticleDao
import com.health.knowledge.util.HealthNotificationHelper
import com.health.knowledge.util.UserPreferencesManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.Calendar
import java.util.concurrent.TimeUnit

@HiltWorker
class DailyTipNotificationWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val articleDao: ArticleDao,
    private val prefsManager: UserPreferencesManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        if (!prefsManager.dailyReminderEnabled.value) {
            return Result.success()
        }

        val activeArticles = articleDao.getAllActiveSnapshot()
        if (activeArticles.isEmpty()) return Result.success()

        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val featured = activeArticles[dayOfYear % activeArticles.size]

        // Requirement: Show only title in the notification; clicking it opens the article detail page
        showTipNotification(
            context = appContext,
            articleId = featured.id,
            title = featured.title
        )
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "HealthKnowledge_DailyTipReminder"

        /**
         * Schedules or cancels daily recurring health tip notification.
         * Default time is morning 8:00 AM (targetHour = 8, targetMinute = 0).
         * Calculates initial delay so it triggers precisely at the user's chosen time.
         */
        fun scheduleOrCancel(
            context: Context,
            enabled: Boolean,
            targetHour: Int = 8,
            targetMinute: Int = 0
        ) {
            val workManager = WorkManager.getInstance(context)
            if (!enabled) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }

            val initialDelayMs = calculateInitialDelayMs(targetHour, targetMinute)

            val request = PeriodicWorkRequestBuilder<DailyTipNotificationWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
                request
            )
        }

        fun calculateInitialDelayMs(targetHour: Int, targetMinute: Int): Long {
            val now = Calendar.getInstance()
            val target = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, targetHour)
                set(Calendar.MINUTE, targetMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (before(now) || timeInMillis <= now.timeInMillis) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return (target.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
        }

        fun showTipNotification(
            context: Context,
            articleId: Int,
            title: String
        ) {
            HealthNotificationHelper.showDailyTipNotification(
                context = context,
                articleId = articleId,
                title = title
            )
        }
    }
}
