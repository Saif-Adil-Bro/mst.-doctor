package com.health.knowledge.sync

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
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

        // Avoid duplicate notification if AlarmManager already fired today
        if (hasFiredToday(appContext)) {
            return Result.success()
        }

        val activeArticles = articleDao.getAllActiveSnapshot()
        if (activeArticles.isEmpty()) return Result.success()

        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val offset = prefsManager.dailyTipOffset.value
        val featured = activeArticles[(dayOfYear + offset).mod(activeArticles.size)]

        markFiredToday(appContext)
        showTipNotification(
            context = appContext,
            articleId = featured.id,
            title = featured.title,
            content = featured.content
        )
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "HealthKnowledge_DailyTipReminder"
        private const val ALARM_REQUEST_CODE = 10088
        private const val PREFS_ALARM = "daily_tip_alarm_prefs"
        private const val KEY_LAST_FIRED_DAY = "last_fired_year_day"

        private fun currentYearDayKey(): String {
            val cal = Calendar.getInstance()
            return "${cal.get(Calendar.YEAR)}_${cal.get(Calendar.DAY_OF_YEAR)}"
        }

        fun hasFiredToday(context: Context): Boolean {
            val prefs = context.getSharedPreferences(PREFS_ALARM, Context.MODE_PRIVATE)
            return prefs.getString(KEY_LAST_FIRED_DAY, null) == currentYearDayKey()
        }

        fun markFiredToday(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_ALARM, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_LAST_FIRED_DAY, currentYearDayKey()).apply()
        }

        fun clearFiredToday(context: Context) {
            val prefs = context.getSharedPreferences(PREFS_ALARM, Context.MODE_PRIVATE)
            prefs.edit().remove(KEY_LAST_FIRED_DAY).apply()
        }

        /**
         * Schedules or cancels daily recurring health tip notification using BOTH:
         * 1) AlarmManager (for exact/window time-of-day delivery even in Doze/MIUI)
         * 2) WorkManager PeriodicWorkRequest (as a resilient background fallback)
         */
        fun scheduleOrCancel(
            context: Context,
            enabled: Boolean,
            targetHour: Int = 8,
            targetMinute: Int = 0,
            forceReschedule: Boolean = false
        ) {
            val workManager = WorkManager.getInstance(context)
            if (!enabled) {
                cancelExactAlarm(context)
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }

            if (forceReschedule) {
                // If user changed reminder time or re-enabled, allow firing today if new time is ahead
                clearFiredToday(context)
            }

            // 1. Schedule exact / window alarm via AlarmManager
            scheduleExactAlarm(context, targetHour, targetMinute)

            // 2. Schedule WorkManager backup without resetting timer on normal app launches
            val initialDelayMs = calculateInitialDelayMs(targetHour, targetMinute)
            val request = PeriodicWorkRequestBuilder<DailyTipNotificationWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                .build()

            val policy = if (forceReschedule) {
                ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE
            } else {
                ExistingPeriodicWorkPolicy.KEEP
            }

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                policy,
                request
            )
        }

        fun scheduleExactAlarm(
            context: Context,
            targetHour: Int,
            targetMinute: Int
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val triggerAtMillis = System.currentTimeMillis() + calculateInitialDelayMs(targetHour, targetMinute)
            val pendingIntent = buildAlarmPendingIntent(context)

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    } else {
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerAtMillis,
                            pendingIntent
                        )
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } catch (_: SecurityException) {
                // Graceful fallback if exact alarm permission is denied by OS
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }

        private fun cancelExactAlarm(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            alarmManager.cancel(buildAlarmPendingIntent(context))
        }

        private fun buildAlarmPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, DailyTipAlarmReceiver::class.java).apply {
                action = DailyTipAlarmReceiver.ACTION_DAILY_TIP_ALARM
            }
            return PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
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
            return (target.timeInMillis - now.timeInMillis).coerceAtLeast(1000L)
        }

        fun showTipNotification(
            context: Context,
            articleId: Int,
            title: String,
            content: String = ""
        ): Boolean {
            return HealthNotificationHelper.showDailyTipNotification(
                context = context,
                articleId = articleId,
                title = title,
                content = content
            )
        }
    }
}
