package com.health.knowledge.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.health.knowledge.data.local.dao.ArticleDao
import com.health.knowledge.util.UserPreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@AndroidEntryPoint
class DailyTipAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var articleDao: ArticleDao

    @Inject
    lateinit var prefsManager: UserPreferencesManager

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return

        when (action) {
            ACTION_DAILY_TIP_ALARM -> {
                val pendingResult = goAsync()
                CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
                    try {
                        if (prefsManager.dailyReminderEnabled.value) {
                            val activeArticles = articleDao.getAllActiveSnapshot()
                            if (activeArticles.isNotEmpty()) {
                                val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                                val offset = prefsManager.dailyTipOffset.value
                                val featured = activeArticles[(dayOfYear + offset).mod(activeArticles.size)]

                                DailyTipNotificationWorker.markFiredToday(context)
                                DailyTipNotificationWorker.showTipNotification(
                                    context = context,
                                    articleId = featured.id,
                                    title = featured.title,
                                    content = featured.content
                                )
                            }
                            // Schedule next day's exact alarm
                            DailyTipNotificationWorker.scheduleExactAlarm(
                                context = context,
                                targetHour = prefsManager.dailyReminderHour.value,
                                targetMinute = prefsManager.dailyReminderMinute.value
                            )
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }

            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                if (prefsManager.dailyReminderEnabled.value) {
                    DailyTipNotificationWorker.scheduleOrCancel(
                        context = context,
                        enabled = true,
                        targetHour = prefsManager.dailyReminderHour.value,
                        targetMinute = prefsManager.dailyReminderMinute.value,
                        forceReschedule = true
                    )
                }
            }
        }
    }

    companion object {
        const val ACTION_DAILY_TIP_ALARM = "com.health.knowledge.ACTION_DAILY_TIP_ALARM"
    }
}
