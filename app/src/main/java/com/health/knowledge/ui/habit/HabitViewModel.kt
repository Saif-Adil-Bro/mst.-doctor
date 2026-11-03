package com.health.knowledge.ui.habit

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.knowledge.data.local.dao.HabitDao
import com.health.knowledge.data.local.entity.HabitEntity
import com.health.knowledge.data.local.entity.HabitExecutionLogEntity
import com.health.knowledge.data.local.entity.ReminderConfigEntity
import com.health.knowledge.habit.HabitAlarmReceiver
import com.health.knowledge.habit.HabitAudioManager
import com.health.knowledge.habit.HabitScheduleCalculator
import com.health.knowledge.habit.HabitScheduler
import com.health.knowledge.util.UserPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class HabitViewModel @Inject constructor(
    private val habitDao: HabitDao,
    private val habitScheduler: HabitScheduler,
    private val audioManager: HabitAudioManager,
    private val prefsManager: UserPreferencesManager
) : ViewModel() {

    val habits: StateFlow<List<HabitEntity>> = habitDao.getAllHabitsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeHabitCount: StateFlow<Int> = habitDao.getActiveHabitCountFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val configsByHabit: StateFlow<Map<String, List<ReminderConfigEntity>>> =
        habitDao.getAllReminderConfigsFlow()
            .map { list -> list.groupBy { it.habitId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val executionLogs: StateFlow<List<HabitExecutionLogEntity>> =
        habitDao.getRecentExecutionLogsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sleepDndEnabled: StateFlow<Boolean> = prefsManager.habitSleepDndEnabled
    val sleepStartHour: StateFlow<Int> = prefsManager.habitSleepStartHour
    val sleepStartMinute: StateFlow<Int> = prefsManager.habitSleepStartMinute
    val sleepEndHour: StateFlow<Int> = prefsManager.habitSleepEndHour
    val sleepEndMinute: StateFlow<Int> = prefsManager.habitSleepEndMinute

    // Live clock ticker (updates every 15 seconds) for dynamic post-event countdown badges
    private val _nowMillis = MutableStateFlow(System.currentTimeMillis())
    val nowMillis: StateFlow<Long> = _nowMillis.asStateFlow()

    init {
        viewModelScope.launch {
            habitScheduler.seedDefaultHabitsIfNeeded()
        }
        viewModelScope.launch {
            while (isActive) {
                _nowMillis.value = System.currentTimeMillis()
                delay(15_000L)
            }
        }
    }

    fun ensureInitialized(context: Context) {
        viewModelScope.launch {
            HabitAlarmReceiver.ensureHabitChannels(context)
            habitScheduler.seedDefaultHabitsIfNeeded()
        }
    }

    /**
     * Marks an anchor event (e.g. meal or medicine) as completed right now (T_done)
     * and dynamically schedules the post-event reminder at T_post = T_done + Δt_post.
     * Overwrites any previous pending POST_EVENT alarm for this habit (idempotent).
     */
    fun markAnchorCompletedNow(
        habit: HabitEntity,
        onScheduled: (Long?) -> Unit
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            _nowMillis.value = now
            val postTime = habitScheduler.scheduleDynamicPostEvent(
                habitId = habit.id,
                completedTimestamp = now
            )
            _nowMillis.value = System.currentTimeMillis()
            onScheduled(postTime)
        }
    }

    fun snoozeHabitPhase(
        habit: HabitEntity,
        phase: String = HabitScheduleCalculator.PHASE_EXACT_EVENT,
        snoozeMins: Int = 10,
        onSnoozed: (Long) -> Unit
    ) {
        viewModelScope.launch {
            val nextTime = habitScheduler.scheduleSnooze(habit.id, phase, snoozeMins)
            _nowMillis.value = System.currentTimeMillis()
            onSnoozed(nextTime)
        }
    }

    fun toggleHabitActive(habit: HabitEntity) {
        viewModelScope.launch {
            val newActive = if (habit.isActive == 1) 0 else 1
            val updated = habit.copy(isActive = newActive)
            habitDao.upsertHabit(updated)
            if (newActive == 1) {
                habitScheduler.scheduleHabit(updated)
            } else {
                habitScheduler.cancelAllForHabit(updated.id)
            }
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            habitScheduler.cancelAllForHabit(habitId)
            habitDao.deleteHabitById(habitId)
        }
    }

    fun saveHabit(
        existingHabitId: String?,
        title: String,
        category: String,
        anchorTime: String,
        hasPreReminder: Boolean,
        preOffsetMins: Int,
        hasPostReminder: Boolean,
        postOffsetMins: Int,
        repeatDays: Set<Int>,
        audioMode: String,
        presetSoundKey: String,
        preMessage: String,
        exactMessage: String,
        postMessage: String,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch {
            val habitId = existingHabitId ?: "habit_${UUID.randomUUID().toString().take(8)}"
            val repeatJson = HabitScheduleCalculator.formatRepeatDaysJson(repeatDays)
            val habit = HabitEntity(
                id = habitId,
                title = title.trim().ifBlank { "স্বাস্থ্য রুটিন" },
                category = category,
                anchorTime = anchorTime,
                hasPreReminder = if (hasPreReminder) 1 else 0,
                preOffsetMins = preOffsetMins.coerceIn(5, 180),
                hasPostReminder = if (hasPostReminder) 1 else 0,
                postOffsetMins = postOffsetMins.coerceIn(5, 240),
                repeatDays = repeatJson,
                isActive = 1,
                createdAt = System.currentTimeMillis()
            )

            val finalPreMsg = preMessage.trim().ifBlank {
                HabitAlarmReceiver.defaultPhaseMessage(habit, HabitScheduleCalculator.PHASE_PRE_EVENT)
            }
            val finalExactMsg = exactMessage.trim().ifBlank {
                HabitAlarmReceiver.defaultPhaseMessage(habit, HabitScheduleCalculator.PHASE_EXACT_EVENT)
            }
            val finalPostMsg = postMessage.trim().ifBlank {
                HabitAlarmReceiver.defaultPhaseMessage(habit, HabitScheduleCalculator.PHASE_POST_EVENT)
            }

            val configs = listOf(
                ReminderConfigEntity(
                    id = "cfg_pre_$habitId",
                    habitId = habitId,
                    phase = HabitScheduleCalculator.PHASE_PRE_EVENT,
                    messageTemplate = finalPreMsg,
                    audioMode = audioMode,
                    presetSoundKey = presetSoundKey,
                    ttsText = finalPreMsg
                ),
                ReminderConfigEntity(
                    id = "cfg_exact_$habitId",
                    habitId = habitId,
                    phase = HabitScheduleCalculator.PHASE_EXACT_EVENT,
                    messageTemplate = finalExactMsg,
                    audioMode = audioMode,
                    presetSoundKey = presetSoundKey,
                    ttsText = finalExactMsg
                ),
                ReminderConfigEntity(
                    id = "cfg_post_$habitId",
                    habitId = habitId,
                    phase = HabitScheduleCalculator.PHASE_POST_EVENT,
                    messageTemplate = finalPostMsg,
                    audioMode = audioMode,
                    presetSoundKey = presetSoundKey,
                    ttsText = finalPostMsg
                )
            )

            habitDao.saveHabitWithConfigs(habit, configs)
            habitScheduler.scheduleHabit(habit)
            _nowMillis.value = System.currentTimeMillis()
            onSaved()
        }
    }

    /**
     * Immediately plays the selected TTS voice or bundled preset sound for live user testing.
     */
    fun previewAudioAlert(
        audioMode: String,
        presetSoundKey: String,
        spokenText: String
    ) {
        audioManager.playReminderAlert(
            audioMode = audioMode,
            presetSoundKey = presetSoundKey,
            spokenText = spokenText
        )
    }

    /**
     * Fires an immediate interactive notification + voice/tone alert for a habit so the user can test
     * the notification bar `[Mark Done]` / `[Snooze 10m]` buttons and audio output right away.
     */
    fun triggerTestHabitAlert(
        context: Context,
        habit: HabitEntity,
        phase: String = HabitScheduleCalculator.PHASE_EXACT_EVENT
    ) {
        viewModelScope.launch {
            val config = habitDao.getConfigForHabitPhase(habit.id, phase)
            val msg = config?.messageTemplate?.takeIf { it.isNotBlank() }
                ?: HabitAlarmReceiver.defaultPhaseMessage(habit, phase)
            val spoken = config?.ttsText?.takeIf { it.isNotBlank() } ?: msg
            val audioMode = config?.audioMode ?: HabitScheduleCalculator.AUDIO_TTS_VOICE
            val soundKey = config?.presetSoundKey
                ?: HabitAlarmReceiver.defaultPresetSound(habit.category, phase)

            HabitAlarmReceiver.showHabitNotification(
                context = context,
                habit = habit,
                phase = phase,
                logId = "log_test_${habit.id}",
                messageText = msg,
                isOverlappingGroup = false,
                silenceAudioForSleepWindow = false
            )
            audioManager.playReminderAlert(
                audioMode = audioMode,
                presetSoundKey = soundKey,
                spokenText = spoken
            )
        }
    }

    fun setSleepDndEnabled(enabled: Boolean) {
        prefsManager.setHabitSleepDndEnabled(enabled)
    }

    fun updateSleepWindow(startHour: Int, startMinute: Int, endHour: Int, endMinute: Int) {
        prefsManager.setHabitSleepWindow(startHour, startMinute, endHour, endMinute)
    }

    fun clearHistoryLogs() {
        viewModelScope.launch {
            habitDao.clearCompletedHistoryLogs()
        }
    }
}
