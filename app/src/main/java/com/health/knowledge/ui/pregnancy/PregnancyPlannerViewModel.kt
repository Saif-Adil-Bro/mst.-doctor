package com.health.knowledge.ui.pregnancy

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.health.knowledge.data.local.dao.HabitDao
import com.health.knowledge.data.local.entity.HabitEntity
import com.health.knowledge.data.local.entity.ReminderConfigEntity
import com.health.knowledge.data.repository.ArticleRepository
import com.health.knowledge.domain.model.Article
import com.health.knowledge.domain.model.toDomain
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class PregnancyPlannerViewModel @Inject constructor(
    private val prefsManager: UserPreferencesManager,
    private val repository: ArticleRepository,
    private val habitDao: HabitDao,
    private val habitScheduler: HabitScheduler,
    private val audioManager: HabitAudioManager
) : ViewModel() {

    companion object {
        const val CAT_PREGNANCY_ROOT = "প্রেগন্যান্সি"
        const val CAT_PRE_CONCEPTION = "প্রেগন্যান্সি_পূর্ব"
        const val CAT_POST_CONCEPTION = "প্রেগন্যান্সি_গর্ভকালীন"
        const val CAT_MALE_FATHERHOOD = "প্রেগন্যান্সি_পুরুষ"
    }

    val isOnboarded: StateFlow<Boolean> = prefsManager.pregnancyOnboarded
    val userGender: StateFlow<String> = prefsManager.pregnancyGender
    val stageMode: StateFlow<String> = prefsManager.pregnancyStageMode
    val pregnancyStartMillis: StateFlow<Long> = prefsManager.pregnancyStartMillis
    val cycleDays: StateFlow<Int> = prefsManager.pregnancyCycleDays
    val selectedSymptoms: StateFlow<Set<String>> = prefsManager.pregnancySymptoms
    val completedChecks: StateFlow<Set<String>> = prefsManager.pregnancyCompletedChecks
    val isAdminMode: StateFlow<Boolean> = prefsManager.isAdminMode

    private val _isRefreshingPosts = MutableStateFlow(false)
    val isRefreshingPosts: StateFlow<Boolean> = _isRefreshingPosts.asStateFlow()

    private val _isSubmittingPost = MutableStateFlow(false)

    /**
     * All Firestore-synced & local articles belonging to the "প্রেগন্যান্সি — আগে ও পরে" module.
     * Sorted with Pinned first, then Newest Firestore posts first.
     */
    val pregnancyArticles: StateFlow<List<Article>> = repository.getAllArticles()
        .map { entities ->
            entities.map { it.toDomain() }
                .filter { article ->
                    article.isReviewed && !article.isDraft && isPregnancyRelatedArticle(article)
                }
                .sortedWith(
                    compareByDescending<Article> { it.isPinned }
                        .thenByDescending { it.syncedFromFirebase || it.isMine }
                        .thenByDescending { it.createdAt }
                        .thenByDescending { it.id }
                )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allHabits: StateFlow<List<HabitEntity>> = habitDao.getAllHabitsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _quoteIndex = MutableStateFlow(0)
    val quoteIndex: StateFlow<Int> = _quoteIndex.asStateFlow()

    init {
        syncPregnancyPostsFromFirestore()
    }

    private fun isPregnancyRelatedArticle(article: Article): Boolean {
        val cats = article.categories
        if (cats.any {
                it.contains("প্রেগন্যান্সি") ||
                    it.contains("গর্ভ") ||
                    it.contains("মাতৃত্ব") ||
                    it == CAT_PRE_CONCEPTION ||
                    it == CAT_POST_CONCEPTION ||
                    it == CAT_MALE_FATHERHOOD
            }
        ) {
            return true
        }
        val titleAndContent = "${article.title} ${article.content}"
        return titleAndContent.contains("গর্ভবতী") ||
            titleAndContent.contains("গর্ভকালীন") ||
            titleAndContent.contains("গর্ভধারণ") ||
            titleAndContent.contains("প্রেগন্যান্সি") ||
            titleAndContent.contains("প্রসব") ||
            titleAndContent.contains("নবজাতক")
    }

    fun syncPregnancyPostsFromFirestore() {
        if (_isRefreshingPosts.value) return
        viewModelScope.launch {
            _isRefreshingPosts.value = true
            try {
                repository.syncFromCloudNow()
            } finally {
                _isRefreshingPosts.value = false
            }
        }
    }

    /**
     * Admin-Only: Verifies admin password against encrypted Firestore/local hash.
     */
    fun verifyAdminGlobal(password: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = repository.verifyAndUnlockAdminGlobal(password)
            onResult(ok)
        }
    }

    /**
     * Admin-Only: Resets admin password using Master Recovery Code and syncs to Firestore.
     */
    fun resetAdminPasswordGlobal(
        recoveryCode: String,
        newPassword: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val res = repository.resetAdminPasswordWithRecoveryGlobal(recoveryCode, newPassword)
            if (res.isSuccess) {
                onResult(true, "✅ নতুন পাসওয়ার্ড এনক্রিপ্ট করে Firestore-এ আপডেট হয়েছে!")
            } else {
                onResult(false, res.exceptionOrNull()?.localizedMessage ?: "পাসওয়ার্ড রিসেট ব্যর্থ হয়েছে")
            }
        }
    }

    /**
     * Admin-Only: Publishes a new post directly to Firestore in the Pregnancy Module.
     * Normal users cannot call this (guarded by `isAdminMode`).
     */
    fun submitAdminPregnancyPost(
        title: String,
        content: String,
        medicines: String,
        stageTag: String,
        photoUrl: String,
        source: String,
        isPinned: Boolean,
        onResult: (Boolean, String) -> Unit
    ) {
        if (!isAdminMode.value) {
            onResult(false, "⛔ শুধুমাত্র অ্যাডমিন প্রেগন্যান্সি মডিউলে পোস্ট করতে পারবেন!")
            return
        }
        if (_isSubmittingPost.value) return
        if (title.isBlank() || content.isBlank()) {
            onResult(false, "শিরোনাম ও বিস্তারিত বিবরণ পূরণ করুন")
            return
        }

        viewModelScope.launch {
            _isSubmittingPost.value = true
            try {
                val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
                val combinedCategories = buildList {
                    add(CAT_PREGNANCY_ROOT)
                    if (stageTag.isNotBlank()) add(stageTag)
                    add("নারী_স্বাস্থ্য")
                }.distinct().joinToString(",")

                val res = repository.addArticleToFirebase(
                    title = title.trim(),
                    content = content.trim(),
                    medicines = medicines.trim(),
                    categories = combinedCategories,
                    photoUrl = photoUrl.trim(),
                    date = dateStr,
                    isPinned = isPinned,
                    source = source.trim()
                )
                if (res.isSuccess) {
                    onResult(true, "✅ প্রেগন্যান্সি পোস্ট সফলভাবে Firestore-এ প্রকাশিত হয়েছে!")
                } else {
                    onResult(false, "❌ পোস্ট আপলোড করতে সমস্যা হয়েছে")
                }
            } finally {
                _isSubmittingPost.value = false
            }
        }
    }

    fun toggleBookmark(articleId: Int, currentBookmark: Boolean) {
        viewModelScope.launch {
            repository.updateBookmark(articleId, !currentBookmark)
        }
    }

    fun reactToArticle(article: Article) {
        viewModelScope.launch {
            repository.toggleReaction(article)
        }
    }

    fun submitVerification(
        article: Article,
        verifierName: String,
        verifierContact: String,
        verifierComment: String,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.submitVerificationWithDetails(
                article = article,
                verifierName = verifierName,
                verifierContact = verifierContact,
                verifierComment = verifierComment
            )
            onComplete()
        }
    }

    fun removeVerification(article: Article, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.removeVerification(article)
            onComplete()
        }
    }

    fun togglePinArticle(article: Article) {
        if (!isAdminMode.value) return
        viewModelScope.launch {
            repository.togglePinArticle(article)
        }
    }

    fun deleteArticle(article: Article) {
        if (!isAdminMode.value) return
        viewModelScope.launch {
            repository.deleteArticleGlobally(article)
        }
    }

    fun completeOnboarding(
        gender: String,
        stageMode: String,
        startMillis: Long,
        cycleDays: Int,
        symptoms: Set<String>
    ) {
        prefsManager.savePregnancyProfileSetup(
            gender = gender,
            stageMode = stageMode,
            startMillis = startMillis,
            cycleDays = cycleDays,
            symptoms = symptoms
        )
    }

    fun reopenOnboarding() {
        prefsManager.setPregnancyOnboarded(false)
    }

    fun setGender(gender: String) {
        prefsManager.setPregnancyGender(gender)
    }

    fun setStageMode(mode: String) {
        prefsManager.setPregnancyStageMode(mode)
    }

    fun updateStartDateMillis(startMillis: Long) {
        prefsManager.setPregnancyStartMillis(startMillis)
    }

    fun updateFromExpectedDeliveryDateMillis(eddMillis: Long) {
        val computedStart = PregnancyPlannerEngine.startMillisFromEdd(
            eddMillis = eddMillis,
            cycleDays = cycleDays.value
        )
        prefsManager.setPregnancyStartMillis(computedStart)
    }

    fun updateCycleDays(days: Int) {
        prefsManager.setPregnancyCycleDays(days)
    }

    fun toggleSymptom(symptomId: String) {
        prefsManager.togglePregnancySymptom(symptomId)
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
