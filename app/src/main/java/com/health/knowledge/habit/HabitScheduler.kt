package com.health.knowledge.habit

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.health.knowledge.data.local.dao.HabitDao
import com.health.knowledge.data.local.entity.HabitEntity
import com.health.knowledge.data.local.entity.HabitExecutionLogEntity
import com.health.knowledge.data.local.entity.ReminderConfigEntity
import com.health.knowledge.util.UserPreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Core Exact Alarm Scheduler for Smart Habit & Routine Engine.
 *
 * Implements:
 * - schedulePreEvent(habit)
 * - scheduleAnchorEvent(habit) (plus fallback post-event T_anchor + 30m + Δt_post)
 * - scheduleDynamicPostEvent(habitId, completedTimestamp) (Idempotent on Mark Done)
 * - scheduleSnooze(habitId, phase, snoozeMins)
 * - rescheduleAllActiveHabits() (Boot & Time-change resilience)
 */
@Singleton
class HabitScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitDao: HabitDao,
    private val prefsManager: UserPreferencesManager
) {

    /**
     * Schedules all enabled phases (Pre-Event, Anchor Event, and Fallback Post-Event) for a habit.
     */
    suspend fun scheduleHabit(habit: HabitEntity) {
        if (habit.isActive == 0) {
            cancelAllForHabit(habit.id)
            return
        }
        if (habit.hasPreReminder == 1) {
            schedulePreEvent(habit)
        } else {
            cancelAlarmForPhase(habit.id, HabitScheduleCalculator.PHASE_PRE_EVENT)
        }
        scheduleAnchorEvent(habit)
    }

    /**
     * 1. Pre-Event Trigger: T_pre = T_anchor - Δt_pre
     */
    suspend fun schedulePreEvent(habit: HabitEntity): Long? {
        if (habit.isActive == 0 || habit.hasPreReminder == 0) return null

        val signedPreOffset = -abs(habit.preOffsetMins)
        val triggerMillis = HabitScheduleCalculator.calculateNextOccurrenceMillis(
            anchorTime = habit.anchorTime,
            signedOffsetMins = signedPreOffset,
            repeatDaysJson = habit.repeatDays
        )

        // Overwrite any previous pending PRE_EVENT log for this habit
        habitDao.deletePendingLogsForPhase(habit.id, HabitScheduleCalculator.PHASE_PRE_EVENT)
        val logId = "log_pre_${habit.id}"
        habitDao.upsertExecutionLog(
            HabitExecutionLogEntity(
                id = logId,
                habitId = habit.id,
                phase = HabitScheduleCalculator.PHASE_PRE_EVENT,
                scheduledTime = triggerMillis,
                executedTime = null,
                status = HabitScheduleCalculator.STATUS_PENDING
            )
        )

        setExactHabitAlarm(
            habitId = habit.id,
            phase = HabitScheduleCalculator.PHASE_PRE_EVENT,
            logId = logId,
            triggerAtMillis = triggerMillis,
            isFallbackPost = false
        )
        return triggerMillis
    }

    /**
     * 2. Anchor Event Trigger: Fires at exact T_anchor with [Mark Done] and [Snooze 10m].
     * Also schedules Case B Fallback Post-Event: T_post = T_anchor + 30m + Δt_post
     * in case the user does not confirm within the grace period.
     */
    suspend fun scheduleAnchorEvent(habit: HabitEntity): Long? {
        if (habit.isActive == 0) return null

        val anchorTriggerMillis = HabitScheduleCalculator.calculateNextOccurrenceMillis(
            anchorTime = habit.anchorTime,
            signedOffsetMins = 0,
            repeatDaysJson = habit.repeatDays
        )

        habitDao.deletePendingLogsForPhase(habit.id, HabitScheduleCalculator.PHASE_EXACT_EVENT)
        val anchorLogId = "log_exact_${habit.id}"
        habitDao.upsertExecutionLog(
            HabitExecutionLogEntity(
                id = anchorLogId,
                habitId = habit.id,
                phase = HabitScheduleCalculator.PHASE_EXACT_EVENT,
                scheduledTime = anchorTriggerMillis,
                executedTime = null,
                status = HabitScheduleCalculator.STATUS_PENDING
            )
        )

        setExactHabitAlarm(
            habitId = habit.id,
            phase = HabitScheduleCalculator.PHASE_EXACT_EVENT,
            logId = anchorLogId,
            triggerAtMillis = anchorTriggerMillis,
            isFallbackPost = false
        )

        // Schedule Case B Fallback Post-Event if has_post_reminder == 1 and no dynamic post-event is earlier
        if (habit.hasPostReminder == 1) {
            val fallbackTriggerMillis = HabitScheduleCalculator.calculateFallbackPostEventTriggerMillis(
                anchorTimeMillis = anchorTriggerMillis,
                postOffsetMins = habit.postOffsetMins,
                defaultDurationMins = HabitScheduleCalculator.DEFAULT_GRACE_DURATION_MINS
            )
            // Only register fallback if there is no active manual dynamic post-event pending before anchor
            val existingPendingPost = habitDao.getPendingLogsForPhase(habit.id, HabitScheduleCalculator.PHASE_POST_EVENT)
            val hasActiveDynamicSoon = existingPendingPost.any {
                it.scheduledTime in (System.currentTimeMillis() + 1000L)..anchorTriggerMillis
            }
            if (!hasActiveDynamicSoon) {
                schedulePostEventInternal(
                    habit = habit,
                    triggerMillis = fallbackTriggerMillis,
                    isFallback = true
                )
            }
        } else {
            cancelAlarmForPhase(habit.id, HabitScheduleCalculator.PHASE_POST_EVENT)
        }

        return anchorTriggerMillis
    }

    /**
     * 3. Post-Event Dynamic Trigger (Case A: User confirms at T_done):
     *    T_post = T_done + Δt_post
     *
     * Idempotent on Mark Done:
     * If user taps "Mark as Done" multiple times, overwrites existing pending post-event alarms
     * matching (habitId, phase='POST_EVENT') instead of creating duplicate alarms.
     */
    suspend fun scheduleDynamicPostEvent(
        habitId: String,
        completedTimestamp: Long = System.currentTimeMillis()
    ): Long? {
        val habit = habitDao.getHabitById(habitId) ?: return null

        // Record anchor completion in execution logs
        val anchorLogId = "log_done_${habitId}_${completedTimestamp / 60_000L}"
        habitDao.deletePendingLogsForPhase(habitId, HabitScheduleCalculator.PHASE_EXACT_EVENT)
        habitDao.upsertExecutionLog(
            HabitExecutionLogEntity(
                id = anchorLogId,
                habitId = habitId,
                phase = HabitScheduleCalculator.PHASE_EXACT_EVENT,
                scheduledTime = completedTimestamp,
                executedTime = completedTimestamp,
                status = HabitScheduleCalculator.STATUS_COMPLETED
            )
        )

        if (habit.hasPostReminder == 0) {
            return null
        }

        val dynamicPostMillis = HabitScheduleCalculator.calculateDynamicPostEventTriggerMillis(
            completedTimeMillis = completedTimestamp,
            postOffsetMins = habit.postOffsetMins
        )

        return schedulePostEventInternal(
            habit = habit,
            triggerMillis = dynamicPostMillis,
            isFallback = false
        )
    }

    private suspend fun schedulePostEventInternal(
        habit: HabitEntity,
        triggerMillis: Long,
        isFallback: Boolean
    ): Long? {
        // Idempotency: cancel previous POST_EVENT alarm & remove old pending POST_EVENT logs
        cancelAlarmForPhase(habit.id, HabitScheduleCalculator.PHASE_POST_EVENT)
        habitDao.deletePendingLogsForPhase(habit.id, HabitScheduleCalculator.PHASE_POST_EVENT)

        // Check Sleep Window DND protection:
        // If calculated T_post falls inside the sleep window and sleep DND is enabled,
        // cancel/suppress the post-event trigger and log as DISMISSED (Sleep DND Protected).
        val sleepDndEnabled = prefsManager.habitSleepDndEnabled.value
        val inSleepWindow = HabitScheduleCalculator.isInsideSleepWindow(
            triggerTimeMillis = triggerMillis,
            sleepStartHour = prefsManager.habitSleepStartHour.value,
            sleepStartMinute = prefsManager.habitSleepStartMinute.value,
            sleepEndHour = prefsManager.habitSleepEndHour.value,
            sleepEndMinute = prefsManager.habitSleepEndMinute.value
        )

        val postLogId = "log_post_${habit.id}"
        if (sleepDndEnabled && inSleepWindow && !isFallback) {
            habitDao.upsertExecutionLog(
                HabitExecutionLogEntity(
                    id = "${postLogId}_dnd_${triggerMillis / 60_000L}",
                    habitId = habit.id,
                    phase = HabitScheduleCalculator.PHASE_POST_EVENT,
                    scheduledTime = triggerMillis,
                    executedTime = System.currentTimeMillis(),
                    status = HabitScheduleCalculator.STATUS_DISMISSED
                )
            )
            return null
        }

        habitDao.upsertExecutionLog(
            HabitExecutionLogEntity(
                id = postLogId,
                habitId = habit.id,
                phase = HabitScheduleCalculator.PHASE_POST_EVENT,
                scheduledTime = triggerMillis,
                executedTime = null,
                status = HabitScheduleCalculator.STATUS_PENDING
            )
        )

        setExactHabitAlarm(
            habitId = habit.id,
            phase = HabitScheduleCalculator.PHASE_POST_EVENT,
            logId = postLogId,
            triggerAtMillis = triggerMillis,
            isFallbackPost = isFallback
        )
        return triggerMillis
    }

    /**
     * Snoozes a phase notification by `snoozeMins` (default 10 minutes) and reschedules exact alarm.
     */
    suspend fun scheduleSnooze(
        habitId: String,
        phase: String,
        snoozeMins: Int = 10
    ): Long {
        val now = System.currentTimeMillis()
        val snoozedTriggerMillis = now + (snoozeMins.coerceAtLeast(1) * 60_000L)

        // Log SNOOZED status
        val snoozeHistoryId = "log_snooze_${habitId}_${phase}_${now / 60_000L}"
        habitDao.upsertExecutionLog(
            HabitExecutionLogEntity(
                id = snoozeHistoryId,
                habitId = habitId,
                phase = phase,
                scheduledTime = now,
                executedTime = now,
                status = HabitScheduleCalculator.STATUS_SNOOZED
            )
        )

        // Create pending log for the snoozed trigger
        habitDao.deletePendingLogsForPhase(habitId, phase)
        val pendingLogId = "log_${phase.lowercase()}_${habitId}"
        habitDao.upsertExecutionLog(
            HabitExecutionLogEntity(
                id = pendingLogId,
                habitId = habitId,
                phase = phase,
                scheduledTime = snoozedTriggerMillis,
                executedTime = null,
                status = HabitScheduleCalculator.STATUS_PENDING
            )
        )

        setExactHabitAlarm(
            habitId = habitId,
            phase = phase,
            logId = pendingLogId,
            triggerAtMillis = snoozedTriggerMillis,
            isFallbackPost = false
        )
        return snoozedTriggerMillis
    }

    /**
     * Cancels all scheduled alarms (Pre, Exact, Post) for a habit.
     */
    suspend fun cancelAllForHabit(habitId: String) {
        cancelAlarmForPhase(habitId, HabitScheduleCalculator.PHASE_PRE_EVENT)
        cancelAlarmForPhase(habitId, HabitScheduleCalculator.PHASE_EXACT_EVENT)
        cancelAlarmForPhase(habitId, HabitScheduleCalculator.PHASE_POST_EVENT)
        habitDao.deletePendingLogsForPhase(habitId, HabitScheduleCalculator.PHASE_PRE_EVENT)
        habitDao.deletePendingLogsForPhase(habitId, HabitScheduleCalculator.PHASE_EXACT_EVENT)
        habitDao.deletePendingLogsForPhase(habitId, HabitScheduleCalculator.PHASE_POST_EVENT)
    }

    fun cancelAlarmForPhase(habitId: String, phase: String) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, HabitAlarmReceiver::class.java).apply {
                action = HabitAlarmReceiver.ACTION_HABIT_ALARM
            }
            val requestCode = alarmRequestCode(habitId, phase)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (_: Exception) {
            // Ignore cancellation errors
        }
    }

    /**
     * Reloads and re-registers all active habits on device boot, app startup, or time change.
     */
    suspend fun rescheduleAllActiveHabits() {
        val activeHabits = habitDao.getActiveHabitsSnapshot()
        for (habit in activeHabits) {
            scheduleHabit(habit)
        }
    }

    /**
     * Seeds 3 ready-made starter habits on first launch so the user has an immediate working dashboard.
     */
    suspend fun seedDefaultHabitsIfNeeded() {
        if (habitDao.getHabitCount() > 0) return

        val now = System.currentTimeMillis()

        val lunchHabit = HabitEntity(
            id = "habit_default_lunch",
            title = "দুপুরের খাবার ও পানি পান রুটিন",
            category = HabitScheduleCalculator.CATEGORY_MEAL_LINKED,
            anchorTime = "14:00",
            hasPreReminder = 1,
            preOffsetMins = 30,
            hasPostReminder = 1,
            postOffsetMins = 45,
            repeatDays = "[1,2,3,4,5,6,7]",
            isActive = 1,
            createdAt = now
        )
        val lunchConfigs = listOf(
            ReminderConfigEntity(
                id = "cfg_lunch_pre",
                habitId = lunchHabit.id,
                phase = HabitScheduleCalculator.PHASE_PRE_EVENT,
                messageTemplate = "দুপুরের খাবারের ৩০ মিনিট বাকি। হজমশক্তি ভালো রাখতে এখন ১ গ্লাস পানি পান করুন।",
                audioMode = HabitScheduleCalculator.AUDIO_TTS_VOICE,
                presetSoundKey = HabitAudioManager.SOUND_WATER_DROP,
                ttsText = "দুপুরের খাবারের ত্রিশ মিনিট বাকি। এখন এক গ্লাস পানি পান করুন।"
            ),
            ReminderConfigEntity(
                id = "cfg_lunch_exact",
                habitId = lunchHabit.id,
                phase = HabitScheduleCalculator.PHASE_EXACT_EVENT,
                messageTemplate = "দুপুরের খাবারের সময় হয়েছে। খাবার শেষে 'সম্পন্ন করেছি' বাটনে ট্যাপ করুন।",
                audioMode = HabitScheduleCalculator.AUDIO_TTS_VOICE,
                presetSoundKey = HabitAudioManager.SOUND_SOFT_BELL,
                ttsText = "দুপুরের খাবারের সময় হয়েছে। খাবার শেষে সম্পন্ন বাটনে ট্যাপ করুন।"
            ),
            ReminderConfigEntity(
                id = "cfg_lunch_post",
                habitId = lunchHabit.id,
                phase = HabitScheduleCalculator.PHASE_POST_EVENT,
                messageTemplate = "খাবার খাওয়ার ৪৫ মিনিট পূর্ণ হয়েছে। এখন কুসুম গরম পানি বা প্রয়োজনীয় ভেষজ পান করার উপযুক্ত সময়।",
                audioMode = HabitScheduleCalculator.AUDIO_TTS_VOICE,
                presetSoundKey = HabitAudioManager.SOUND_WATER_DROP,
                ttsText = "খাবার খাওয়ার পঁয়তাল্লিশ মিনিট পূর্ণ হয়েছে। এখন পানি পান করার উপযুক্ত সময়।"
            )
        )

        val dinnerHabit = HabitEntity(
            id = "habit_default_dinner",
            title = "রাতের খাবার ও হজম সুরক্ষা রুটিন",
            category = HabitScheduleCalculator.CATEGORY_MEAL_LINKED,
            anchorTime = "20:30",
            hasPreReminder = 1,
            preOffsetMins = 30,
            hasPostReminder = 1,
            postOffsetMins = 45,
            repeatDays = "[1,2,3,4,5,6,7]",
            isActive = 1,
            createdAt = now + 1
        )
        val dinnerConfigs = listOf(
            ReminderConfigEntity(
                id = "cfg_dinner_pre",
                habitId = dinnerHabit.id,
                phase = HabitScheduleCalculator.PHASE_PRE_EVENT,
                messageTemplate = "রাতের খাবারের ৩০ মিনিট আগে ১ গ্লাস পানি পান করুন, এতে গ্যাস্ট্রিক ও বদহজম দূর হয়।",
                audioMode = HabitScheduleCalculator.AUDIO_TTS_VOICE,
                presetSoundKey = HabitAudioManager.SOUND_WATER_DROP,
                ttsText = "রাতের খাবারের ত্রিশ মিনিট আগে এক গ্লাস পানি পান করুন।"
            ),
            ReminderConfigEntity(
                id = "cfg_dinner_exact",
                habitId = dinnerHabit.id,
                phase = HabitScheduleCalculator.PHASE_EXACT_EVENT,
                messageTemplate = "রাতের খাবারের সময় হয়েছে। খাবার শেষ করে 'সম্পন্ন করেছি' চাপুন।",
                audioMode = HabitScheduleCalculator.AUDIO_TTS_VOICE,
                presetSoundKey = HabitAudioManager.SOUND_SOFT_BELL,
                ttsText = "রাতের খাবারের সময় হয়েছে।"
            ),
            ReminderConfigEntity(
                id = "cfg_dinner_post",
                habitId = dinnerHabit.id,
                phase = HabitScheduleCalculator.PHASE_POST_EVENT,
                messageTemplate = "রাতের খাবারের ৪৫ মিনিট পার হয়েছে। এখন পরিমিত পানি পান ও হালকা হাঁটাহাঁটি করুন।",
                audioMode = HabitScheduleCalculator.AUDIO_TTS_VOICE,
                presetSoundKey = HabitAudioManager.SOUND_WATER_DROP,
                ttsText = "রাতের খাবারের পঁয়তাল্লিশ মিনিট পার হয়েছে। এখন পানি পান করুন।"
            )
        )

        val morningMedHabit = HabitEntity(
            id = "habit_default_morning_med",
            title = "সকালের খালি পেটে পানি ও ভেষজ/ওষুধ",
            category = HabitScheduleCalculator.CATEGORY_MEDICATION,
            anchorTime = "07:30",
            hasPreReminder = 1,
            preOffsetMins = 20,
            hasPostReminder = 1,
            postOffsetMins = 30,
            repeatDays = "[1,2,3,4,5,6,7]",
            isActive = 1,
            createdAt = now + 2
        )
        val morningConfigs = listOf(
            ReminderConfigEntity(
                id = "cfg_morning_pre",
                habitId = morningMedHabit.id,
                phase = HabitScheduleCalculator.PHASE_PRE_EVENT,
                messageTemplate = "সকালের ভেষজ বা ওষুধের ২০ মিনিট আগে বাসি মুখে কুসুম গরম পানি পান করুন।",
                audioMode = HabitScheduleCalculator.AUDIO_TTS_VOICE,
                presetSoundKey = HabitAudioManager.SOUND_WATER_DROP,
                ttsText = "সকালের ভেষজ বা ওষুধের বিশ মিনিট আগে কুসুম গরম পানি পান করুন।"
            ),
            ReminderConfigEntity(
                id = "cfg_morning_exact",
                habitId = morningMedHabit.id,
                phase = HabitScheduleCalculator.PHASE_EXACT_EVENT,
                messageTemplate = "সকালের নির্ধারিত ভেষজ বা ওষুধ সেবনের সময় হয়েছে। সেবন শেষে 'সম্পন্ন করেছি' চাপুন।",
                audioMode = HabitScheduleCalculator.AUDIO_TTS_VOICE,
                presetSoundKey = HabitAudioManager.SOUND_MEDICATION_CHIME,
                ttsText = "সকালের নির্ধারিত ভেষজ বা ওষুধ সেবনের সময় হয়েছে।"
            ),
            ReminderConfigEntity(
                id = "cfg_morning_post",
                habitId = morningMedHabit.id,
                phase = HabitScheduleCalculator.PHASE_POST_EVENT,
                messageTemplate = "ওষুধ বা ভেষজ সেবনের ৩০ মিনিট পূর্ণ হয়েছে। এখন সকালের নাস্তা করতে পারেন।",
                audioMode = HabitScheduleCalculator.AUDIO_TTS_VOICE,
                presetSoundKey = HabitAudioManager.SOUND_SOFT_BELL,
                ttsText = "ওষুধ সেবনের ত্রিশ মিনিট পূর্ণ হয়েছে। এখন সকালের নাস্তা করতে পারেন।"
            )
        )

        habitDao.saveHabitWithConfigs(lunchHabit, lunchConfigs)
        habitDao.saveHabitWithConfigs(dinnerHabit, dinnerConfigs)
        habitDao.saveHabitWithConfigs(morningMedHabit, morningConfigs)

        rescheduleAllActiveHabits()
    }

    private fun setExactHabitAlarm(
        habitId: String,
        phase: String,
        logId: String,
        triggerAtMillis: Long,
        isFallbackPost: Boolean
    ) {
        try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, HabitAlarmReceiver::class.java).apply {
                action = HabitAlarmReceiver.ACTION_HABIT_ALARM
                putExtra(HabitAlarmReceiver.EXTRA_HABIT_ID, habitId)
                putExtra(HabitAlarmReceiver.EXTRA_PHASE, phase)
                putExtra(HabitAlarmReceiver.EXTRA_LOG_ID, logId)
                putExtra(HabitAlarmReceiver.EXTRA_IS_FALLBACK_POST, isFallbackPost)
                putExtra(HabitAlarmReceiver.EXTRA_SCHEDULED_TIME, triggerAtMillis)
            }
            val requestCode = alarmRequestCode(habitId, phase)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

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
        } catch (_: Exception) {
            // Ignored if restricted by OS
        }
    }

    companion object {
        fun alarmRequestCode(habitId: String, phase: String): Int {
            return abs("${habitId}_$phase".hashCode())
        }
    }
}
