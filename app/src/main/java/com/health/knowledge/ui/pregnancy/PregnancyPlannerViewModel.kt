package com.health.knowledge.ui.pregnancy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.knowledge.data.local.dao.HabitDao
import com.health.knowledge.data.local.entity.HabitEntity
import com.health.knowledge.data.local.entity.ReminderConfigEntity
import com.health.knowledge.habit.HabitAudioManager
import com.health.knowledge.habit.HabitScheduleCalculator
import com.health.knowledge.habit.HabitScheduler
import com.health.knowledge.pregnancy.PregnancyPlannerEngine
import com.health.knowledge.pregnancy.PregnancyReminderPreset
import com.health.knowledge.util.UserPreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PregnancyPlannerViewModel @Inject constructor(
    private val prefsManager: UserPreferencesManager,
    private val habitDao: HabitDao,
    private val habitScheduler: HabitScheduler,
    private val audioManager: HabitAudioManager
) : ViewModel() {

    val stageMode: StateFlow<String> = prefsManager.pregnancyStageMode
    val pregnancyStartMillis: StateFlow<Long> = prefsManager.pregnancyStartMillis
    val completedChecks: StateFlow<Set<String>> = prefsManager.pregnancyCompletedChecks

    val allHabits: StateFlow<List<HabitEntity>> = habitDao.getAllHabitsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _quoteIndex = MutableStateFlow(0)
    val quoteIndex: StateFlow<Int> = _quoteIndex.asStateFlow()

    fun setStageMode(mode: String) {
        prefsManager.setPregnancyStageMode(mode)
    }

    fun updateStartDateMillis(startMillis: Long) {
        prefsManager.setPregnancyStartMillis(startMillis)
    }

    fun updateFromExpectedDeliveryDateMillis(eddMillis: Long) {
        val computedStart = PregnancyPlannerEngine.startMillisFromEdd(eddMillis)
        prefsManager.setPregnancyStartMillis(computedStart)
    }

    fun toggleCheckItem(itemKey: String) {
        prefsManager.togglePregnancyCheckItem(itemKey)
    }

    fun nextQuote() {
        val quotes = PregnancyPlannerEngine.emotionalAndMotivationalQuotes
        if (quotes.isNotEmpty()) {
            _quoteIndex.value = (_quoteIndex.value + 1) % quotes.size
        }
    }

    fun speakBengaliPrompt(text: String) {
        audioManager.playReminderAlert(
            audioMode = HabitScheduleCalculator.AUDIO_TTS_VOICE,
            presetSoundKey = HabitAudioManager.SOUND_SOFT_BELL,
            spokenText = text
        )
    }

    /**
     * Enables or toggles a pregnancy reminder preset directly in the Smart Habit & Routine Engine
     * so exact alarms and Bengali TTS voice prompts work seamlessly offline.
     */
    fun togglePregnancyReminderPreset(
        preset: PregnancyReminderPreset,
        onResult: (isNowActive: Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val existing = habitDao.getHabitById(preset.id)
            if (existing != null && existing.isActive == 1) {
                val updated = existing.copy(isActive = 0)
                habitDao.upsertHabit(updated)
                habitScheduler.cancelAllForHabit(updated.id)
                onResult(false)
            } else {
                val habit = HabitEntity(
                    id = preset.id,
                    title = preset.titleBn,
                    category = preset.category,
                    anchorTime = preset.anchorTime,
                    hasPreReminder = 1,
                    preOffsetMins = preset.preOffsetMins,
                    hasPostReminder = 1,
                    postOffsetMins = preset.postOffsetMins,
                    repeatDays = "[1,2,3,4,5,6,7]",
                    isActive = 1,
                    createdAt = System.currentTimeMillis()
                )
                val configs = listOf(
                    ReminderConfigEntity(
                        id = "cfg_pre_${preset.id}",
                        habitId = preset.id,
                        phase = HabitScheduleCalculator.PHASE_PRE_EVENT,
                        messageTemplate = "${preset.titleBn}-এর প্রস্তুতি নিন।",
                        audioMode = HabitScheduleCalculator.AUDIO_TTS_VOICE,
                        presetSoundKey = HabitAudioManager.SOUND_SOFT_BELL,
                        ttsText = "${preset.titleBn}-এর প্রস্তুতি নিন।"
                    ),
                    ReminderConfigEntity(
                        id = "cfg_exact_${preset.id}",
                        habitId = preset.id,
                        phase = HabitScheduleCalculator.PHASE_EXACT_EVENT,
                        messageTemplate = preset.messageBn,
                        audioMode = HabitScheduleCalculator.AUDIO_TTS_VOICE,
                        presetSoundKey = HabitAudioManager.SOUND_SOFT_BELL,
                        ttsText = preset.messageBn
                    ),
                    ReminderConfigEntity(
                        id = "cfg_post_${preset.id}",
                        habitId = preset.id,
                        phase = HabitScheduleCalculator.PHASE_POST_EVENT,
                        messageTemplate = "${preset.titleBn} সম্পন্ন হয়েছে কি না যাচাই করুন।",
                        audioMode = HabitScheduleCalculator.AUDIO_TTS_VOICE,
                        presetSoundKey = HabitAudioManager.SOUND_WATER_DROP,
                        ttsText = "${preset.titleBn} সম্পন্ন হয়েছে কি না যাচাই করুন।"
                    )
                )
                habitDao.saveHabitWithConfigs(habit, configs)
                habitScheduler.scheduleHabit(habit)
                onResult(true)
            }
        }
    }
}
