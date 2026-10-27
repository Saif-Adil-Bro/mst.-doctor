package com.health.knowledge.sync

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.health.knowledge.MainActivity
import com.health.knowledge.data.local.dao.ArticleDao
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

        showTipNotification(
            context = appContext,
            articleId = featured.id,
            title = "🌿 আজকের স্বাস্থ্য টিপস: ${featured.title}",
            body = featured.content.take(160).replace("\n", " ") + "..."
        )
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "HealthKnowledge_DailyTipReminder"
        private const val CHANNEL_ID = "daily_health_tips_channel"
        private const val NOTIFICATION_ID = 2026

        fun scheduleOrCancel(context: Context, enabled: Boolean) {
            val workManager = WorkManager.getInstance(context)
            if (!enabled) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }

            val request = PeriodicWorkRequestBuilder<DailyTipNotificationWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(12, TimeUnit.HOURS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun showTipNotification(
            context: Context,
            articleId: Int,
            title: String,
            body: String
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
                if (!granted) return
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "দৈনিক স্বাস্থ্য টিপস রিমাইন্ডার",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "প্রতিদিন একটি উপকারী আয়ুর্বেদিক ও প্রাকৃতিক স্বাস্থ্য টিপস"
                }
                manager.createNotificationChannel(channel)
            }

            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_article_id", articleId)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                articleId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            manager.notify(NOTIFICATION_ID, notification)
        }
    }
}
