package com.health.knowledge.habit

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.health.knowledge.MainActivity
import com.health.knowledge.data.local.dao.HabitDao
import com.health.knowledge.data.local.entity.HabitEntity
import com.health.knowledge.data.local.entity.HabitExecutionLogEntity
import com.health.knowledge.util.HealthNotificationHelper
import com.health.knowledge.util.UserPreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.abs

/**
 * Event-Driven BroadcastReceiver & Notification Subsystem for Smart Habit & Routine Engine.
 *
 * Notification Channels:
 * - `health_urgent`: Priority High/Max, bypassed DND if permitted, full-screen intent capability for critical medications.
 * - `health_routine`: Priority Default/High, audible sound/TTS for water & meal alerts.
 *
 * Interactive Notification Actions:
 * - `ACTION_MEAL_DONE`: Triggered from notification bar without opening app UI. Schedules dynamic T_post.
 * - `ACTION_SNOOZE`: Adds 10 minutes to current notification and reschedules alarm.
 * - `ACTION_DISMISS`: Closes notification and marks status as DISMISSED.
 */
@AndroidEntryPoint
class HabitAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var habitDao: HabitDao

    @Inject
    lateinit var habitScheduler: HabitScheduler

    @Inject
    lateinit var audioManager: HabitAudioManager

    @Inject
    lateinit var prefsManager: UserPreferencesManager

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_HABIT_ALARM -> handleHabitAlarmTrigger(context, intent)
                    ACTION_MEAL_DONE -> handleMealDoneAction(context, intent)
                    ACTION_SNOOZE -> handleSnoozeAction(context, intent)
                    ACTION_DISMISS -> handleDismissAction(context, intent)
                    ACTION_PHASE_DONE -> handlePhaseDoneAction(context, intent)

                    Intent.ACTION_BOOT_COMPLETED,
                    Intent.ACTION_MY_PACKAGE_REPLACED,
                    Intent.ACTION_TIME_CHANGED,
                    Intent.ACTION_TIMEZONE_CHANGED,
                    "android.intent.action.QUICKBOOT_POWERON" -> {
                        habitScheduler.rescheduleAllActiveHabits()
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun handleHabitAlarmTrigger(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val phase = intent.getStringExtra(EXTRA_PHASE) ?: return
        val logId = intent.getStringExtra(EXTRA_LOG_ID)
        val scheduledTime = intent.getLongExtra(EXTRA_SCHEDULED_TIME, System.currentTimeMillis())

        val habit = habitDao.getHabitById(habitId) ?: return
        if (habit.isActive == 0) return

        val now = System.currentTimeMillis()

        // Sleep Window DND Protection check
        val sleepDndEnabled = prefsManager.habitSleepDndEnabled.value
        val inSleepWindow = HabitScheduleCalculator.isInsideSleepWindow(
            triggerTimeMillis = now,
            sleepStartHour = prefsManager.habitSleepStartHour.value,
            sleepStartMinute = prefsManager.habitSleepStartMinute.value,
            sleepEndHour = prefsManager.habitSleepEndHour.value,
            sleepEndMinute = prefsManager.habitSleepEndMinute.value
        )

        if (sleepDndEnabled && inSleepWindow && phase == HabitScheduleCalculator.PHASE_POST_EVENT) {
            // Cancel/suppress post-event trigger inside sleep window
            if (!logId.isNullOrBlank()) {
                habitDao.updateLogStatus(logId, HabitScheduleCalculator.STATUS_DISMISSED, now)
            }
            return
        }

        val config = habitDao.getConfigForHabitPhase(habitId, phase)
        val defaultMessage = defaultPhaseMessage(habit, phase)
        val messageText = config?.messageTemplate?.takeIf { it.isNotBlank() } ?: defaultMessage
        val spokenText = config?.ttsText?.takeIf { it.isNotBlank() } ?: messageText
        val audioMode = config?.audioMode ?: HabitScheduleCalculator.AUDIO_TTS_VOICE
        val presetKey = config?.presetSoundKey ?: defaultPresetSound(habit.category, phase)

        // Check Overlapping Triggers (within <= 2 minutes of last fired habit alarm)
        val isOverlapping = HabitScheduleCalculator.areTriggersOverlapping(lastFiredAlarmMillis, now)
        lastFiredAlarmMillis = now
        activeGroupedAlertsCount = if (isOverlapping) activeGroupedAlertsCount + 1 else 1

        // Display notification with interactive actions
        showHabitNotification(
            context = context,
            habit = habit,
            phase = phase,
            logId = logId ?: "log_${phase.lowercase()}_${habit.id}",
            messageText = messageText,
            isOverlappingGroup = isOverlapping,
            silenceAudioForSleepWindow = sleepDndEnabled && inSleepWindow
        )

        // Play TTS / Audio unless silenced by Sleep Window DND or overlapping audio collision
        if (!(sleepDndEnabled && inSleepWindow) && !isOverlapping) {
            audioManager.playReminderAlert(
                audioMode = audioMode,
                presetSoundKey = presetKey,
                spokenText = spokenText
            )
        }

        // If this was a POST_EVENT, mark the pending log as COMPLETED once delivered
        if (phase == HabitScheduleCalculator.PHASE_POST_EVENT && !logId.isNullOrBlank()) {
            habitDao.updateLogStatus(logId, HabitScheduleCalculator.STATUS_COMPLETED, now)
        }

        // Reschedule recurring daily alarms for PRE_EVENT or EXACT_EVENT
        when (phase) {
            HabitScheduleCalculator.PHASE_PRE_EVENT -> habitScheduler.schedulePreEvent(habit)
            HabitScheduleCalculator.PHASE_EXACT_EVENT -> habitScheduler.scheduleAnchorEvent(habit)
            else -> {
                // Dynamic POST_EVENT is one-shot per meal completion
            }
        }
    }

    /**
     * ACTION_MEAL_DONE:
     * Triggered from notification bar without opening app UI.
     * Passes habit_id and timestamp T_done, marks EXACT_EVENT completed, and schedules dynamic T_post.
     */
    private suspend fun handleMealDoneAction(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val now = System.currentTimeMillis()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notifId != -1) {
            manager.cancel(notifId)
        }

        val habit = habitDao.getHabitById(habitId) ?: return
        val postTriggerMillis = habitScheduler.scheduleDynamicPostEvent(habitId, completedTimestamp = now)

        // Show subtle confirmation notification if post-reminder was scheduled
        if (postTriggerMillis != null && HealthNotificationHelper.hasNotificationPermission(context)) {
            ensureHabitChannels(context)
            val formattedPostTime = HabitScheduleCalculator.formatMillisToBengaliTime(postTriggerMillis)
            val confirmNotif = NotificationCompat.Builder(context, CHANNEL_HEALTH_ROUTINE)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("✅ ${habit.title} সম্পন্ন!")
                .setContentText("পরবর্তী রিমাইন্ডার (${habit.postOffsetMins} মি. পর) সেট হয়েছে: $formattedPostTime")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setAutoCancel(true)
                .setTimeoutAfter(12_000L)
                .build()
            manager.notify(notificationIdFor(habitId, "CONFIRM_POST"), confirmNotif)
        }
    }

    /**
     * ACTION_SNOOZE:
     * Adds 10 minutes to current notification and reschedules alarm.
     */
    private suspend fun handleSnoozeAction(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val phase = intent.getStringExtra(EXTRA_PHASE) ?: HabitScheduleCalculator.PHASE_EXACT_EVENT
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notifId != -1) {
            manager.cancel(notifId)
        }

        habitScheduler.scheduleSnooze(habitId = habitId, phase = phase, snoozeMins = 10)
    }

    /**
     * ACTION_DISMISS:
     * Closes notification and marks status as DISMISSED.
     */
    private suspend fun handleDismissAction(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val phase = intent.getStringExtra(EXTRA_PHASE) ?: HabitScheduleCalculator.PHASE_EXACT_EVENT
        val logId = intent.getStringExtra(EXTRA_LOG_ID)
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val now = System.currentTimeMillis()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notifId != -1) {
            manager.cancel(notifId)
        }

        if (!logId.isNullOrBlank()) {
            habitDao.updateLogStatus(logId, HabitScheduleCalculator.STATUS_DISMISSED, now)
        } else {
            habitDao.upsertExecutionLog(
                HabitExecutionLogEntity(
                    id = "log_dismiss_${habitId}_${phase}_${now / 60_000L}",
                    habitId = habitId,
                    phase = phase,
                    scheduledTime = now,
                    executedTime = now,
                    status = HabitScheduleCalculator.STATUS_DISMISSED
                )
            )
        }
    }

    /**
     * ACTION_PHASE_DONE:
     * Marks PRE_EVENT or POST_EVENT as COMPLETED from notification bar.
     */
    private suspend fun handlePhaseDoneAction(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(EXTRA_HABIT_ID) ?: return
        val phase = intent.getStringExtra(EXTRA_PHASE) ?: HabitScheduleCalculator.PHASE_PRE_EVENT
        val logId = intent.getStringExtra(EXTRA_LOG_ID)
        val notifId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val now = System.currentTimeMillis()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (notifId != -1) {
            manager.cancel(notifId)
        }

        if (!logId.isNullOrBlank()) {
            habitDao.updateLogStatus(logId, HabitScheduleCalculator.STATUS_COMPLETED, now)
        } else {
            habitDao.upsertExecutionLog(
                HabitExecutionLogEntity(
                    id = "log_done_${habitId}_${phase}_${now / 60_000L}",
                    habitId = habitId,
                    phase = phase,
                    scheduledTime = now,
                    executedTime = now,
                    status = HabitScheduleCalculator.STATUS_COMPLETED
                )
            )
        }
    }

    companion object {
        const val ACTION_HABIT_ALARM = "com.health.knowledge.ACTION_HABIT_ALARM"
        const val ACTION_MEAL_DONE = "com.health.knowledge.ACTION_MEAL_DONE"
        const val ACTION_SNOOZE = "com.health.knowledge.ACTION_SNOOZE"
        const val ACTION_DISMISS = "com.health.knowledge.ACTION_DISMISS"
        const val ACTION_PHASE_DONE = "com.health.knowledge.ACTION_PHASE_DONE"

        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_PHASE = "extra_phase"
        const val EXTRA_LOG_ID = "extra_log_id"
        const val EXTRA_IS_FALLBACK_POST = "extra_is_fallback_post"
        const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        const val CHANNEL_HEALTH_URGENT = "health_urgent"
        const val CHANNEL_HEALTH_ROUTINE = "health_routine"
        const val GROUP_KEY_HABIT_ALERTS = "com.health.knowledge.GROUP_HABIT_ALERTS"
        private const val SUMMARY_NOTIFICATION_ID = 49000

        @Volatile
        private var lastFiredAlarmMillis: Long = 0L

        @Volatile
        private var activeGroupedAlertsCount: Int = 0

        fun ensureHabitChannels(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

                val urgentChannel = NotificationChannel(
                    CHANNEL_HEALTH_URGENT,
                    "জরুরি ওষুধ অ্যালার্ম",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "ওষুধ ও প্রধান স্বাস্থ্য অ্যালার্ম"
                    enableVibration(true)
                    setBypassDnd(true)
                }
                manager.createNotificationChannel(urgentChannel)

                val routineChannel = NotificationChannel(
                    CHANNEL_HEALTH_ROUTINE,
                    "খাবার ও পানি অ্যালার্ম",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "খাবার ও পানি পানের নিয়মিত অ্যালার্ম"
                    enableVibration(true)
                }
                manager.createNotificationChannel(routineChannel)
            }
        }

        fun showHabitNotification(
            context: Context,
            habit: HabitEntity,
            phase: String,
            logId: String,
            messageText: String,
            isOverlappingGroup: Boolean = false,
            silenceAudioForSleepWindow: Boolean = false
        ) {
            if (!HealthNotificationHelper.hasNotificationPermission(context)) return
            ensureHabitChannels(context)

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val notifId = notificationIdFor(habit.id, phase)
            val isUrgent = habit.category == HabitScheduleCalculator.CATEGORY_MEDICATION ||
                phase == HabitScheduleCalculator.PHASE_EXACT_EVENT
            val channelId = if (isUrgent) CHANNEL_HEALTH_URGENT else CHANNEL_HEALTH_ROUTINE

            val phaseBadge = when (phase) {
                HabitScheduleCalculator.PHASE_PRE_EVENT -> "${HabitScheduleCalculator.toBengaliDigits(habit.preOffsetMins.toString())}মি আগে"
                HabitScheduleCalculator.PHASE_EXACT_EVENT -> HabitScheduleCalculator.format24hToBengali(habit.anchorTime)
                HabitScheduleCalculator.PHASE_POST_EVENT -> "${HabitScheduleCalculator.toBengaliDigits(habit.postOffsetMins.toString())}মি পরে"
                else -> "অ্যালার্ম"
            }

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("open_habit_module", true)
            }
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                notifId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Action 1: Mark Done
            val doneIntent = Intent(context, HabitAlarmReceiver::class.java).apply {
                action = if (phase == HabitScheduleCalculator.PHASE_EXACT_EVENT) ACTION_MEAL_DONE else ACTION_PHASE_DONE
                putExtra(EXTRA_HABIT_ID, habit.id)
                putExtra(EXTRA_PHASE, phase)
                putExtra(EXTRA_LOG_ID, logId)
                putExtra(EXTRA_NOTIFICATION_ID, notifId)
            }
            val donePendingIntent = PendingIntent.getBroadcast(
                context,
                notifId + 1,
                doneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Action 2: Snooze 10m
            val snoozeIntent = Intent(context, HabitAlarmReceiver::class.java).apply {
                action = ACTION_SNOOZE
                putExtra(EXTRA_HABIT_ID, habit.id)
                putExtra(EXTRA_PHASE, phase)
                putExtra(EXTRA_LOG_ID, logId)
                putExtra(EXTRA_NOTIFICATION_ID, notifId)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context,
                notifId + 2,
                snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Action 3: Dismiss
            val dismissIntent = Intent(context, HabitAlarmReceiver::class.java).apply {
                action = ACTION_DISMISS
                putExtra(EXTRA_HABIT_ID, habit.id)
                putExtra(EXTRA_PHASE, phase)
                putExtra(EXTRA_LOG_ID, logId)
                putExtra(EXTRA_NOTIFICATION_ID, notifId)
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context,
                notifId + 3,
                dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("${habit.title} • $phaseBadge")
                .setContentText(messageText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(messageText))
                .setPriority(if (isUrgent) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
                .setCategory(if (isUrgent) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)
                .setGroup(GROUP_KEY_HABIT_ALERTS)
                .addAction(android.R.drawable.checkbox_on_background, "সম্পন্ন", donePendingIntent)
                .addAction(android.R.drawable.ic_popup_reminder, "স্নুজ", snoozePendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "বন্ধ", dismissPendingIntent)

            if (silenceAudioForSleepWindow) {
                builder.setSilent(true)
            }
            if (isUrgent) {
                builder.setFullScreenIntent(contentPendingIntent, true)
            }

            manager.notify(notifId, builder.build())

            // Overlapping Triggers (< 2 mins): post a single group summary notification
            if (isOverlappingGroup) {
                val summaryNotif = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("🌿 স্বাস্থ্য জ্ঞান • একত্রে একাধিক রুটিন অ্যালার্ট")
                    .setContentText("আপনার $activeGroupedAlertsCount টি স্বাস্থ্য ও পানি পানের রিমাইন্ডার সক্রিয় হয়েছে")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setGroup(GROUP_KEY_HABIT_ALERTS)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .build()
                manager.notify(SUMMARY_NOTIFICATION_ID, summaryNotif)
            }
        }

        fun notificationIdFor(habitId: String, phase: String): Int {
            return 40000 + (abs("${habitId}_$phase".hashCode()) % 8000) * 4
        }

        fun defaultPhaseMessage(habit: HabitEntity, phase: String): String {
            return when (phase) {
                HabitScheduleCalculator.PHASE_PRE_EVENT ->
                    "${habit.title}-এর ${habit.preOffsetMins} মিনিট বাকি। এখন ১ গ্লাস পানি পান বা প্রস্তুতি সম্পন্ন করুন।"
                HabitScheduleCalculator.PHASE_EXACT_EVENT ->
                    "${habit.title}-এর নির্ধারিত সময় হয়েছে। শেষ করে 'সম্পন্ন করেছি' বাটনে ট্যাপ করুন।"
                HabitScheduleCalculator.PHASE_POST_EVENT ->
                    "${habit.title} শেষ করার ${habit.postOffsetMins} মিনিট পূর্ণ হয়েছে। এখন পরবর্তী পানি বা ভেষজ গ্রহণ করুন।"
                else -> habit.title
            }
        }

        fun defaultPresetSound(category: String, phase: String): String {
            return when {
                category == HabitScheduleCalculator.CATEGORY_MEDICATION -> HabitAudioManager.SOUND_MEDICATION_CHIME
                phase == HabitScheduleCalculator.PHASE_PRE_EVENT || phase == HabitScheduleCalculator.PHASE_POST_EVENT ->
                    HabitAudioManager.SOUND_WATER_DROP
                else -> HabitAudioManager.SOUND_SOFT_BELL
            }
        }
    }
}
