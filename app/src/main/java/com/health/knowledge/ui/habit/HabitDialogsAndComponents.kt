package com.health.knowledge.ui.habit

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.health.knowledge.data.local.entity.HabitEntity
import com.health.knowledge.data.local.entity.ReminderConfigEntity
import com.health.knowledge.habit.HabitAudioManager
import com.health.knowledge.habit.HabitScheduleCalculator
import com.health.knowledge.ui.theme.onePxBorder
import com.health.knowledge.ui.theme.solidOverSurface
import java.util.Calendar
import kotlin.math.max

val habitCategoryOptions = listOf(
    HabitScheduleCalculator.CATEGORY_MEAL_LINKED to ("🍽️ খাবার ও পানি" to "খাবারের আগে ও পরে পানি পান"),
    HabitScheduleCalculator.CATEGORY_MEDICATION to ("💊 ওষুধ ও ভেষজ" to "নির্দিষ্ট সময়ে ওষুধ বা ভেষজ"),
    HabitScheduleCalculator.CATEGORY_HYDRATION to ("💧 হাইড্রেশন" to "নিয়মিত পানি পানের রিমাইন্ডার"),
    HabitScheduleCalculator.CATEGORY_CUSTOM to ("⏰ কাস্টম রুটিন" to "যেকোনো ব্যক্তিগত স্বাস্থ্য অভ্যাস")
)

val bengaliWeekDays = listOf(
    1 to "সোম",
    2 to "মঙ্গল",
    3 to "বুধ",
    4 to "বৃহঃ",
    5 to "শুক্র",
    6 to "শনি",
    7 to "রবি"
)

private data class QuickHabitTemplate(
    val label: String,
    val title: String,
    val category: String,
    val anchorTime: String,
    val preOffset: Int,
    val postOffset: Int,
    val soundKey: String
)

private val quickHabitTemplates = listOf(
    QuickHabitTemplate(
        label = "🍽️ দুপুরের খাবার",
        title = "দুপুরের খাবার ও পানি পান রুটিন",
        category = HabitScheduleCalculator.CATEGORY_MEAL_LINKED,
        anchorTime = "14:00",
        preOffset = 30,
        postOffset = 45,
        soundKey = HabitAudioManager.SOUND_WATER_DROP
    ),
    QuickHabitTemplate(
        label = "🌙 রাতের খাবার",
        title = "রাতের খাবার ও হজম সুরক্ষা রুটিন",
        category = HabitScheduleCalculator.CATEGORY_MEAL_LINKED,
        anchorTime = "20:30",
        preOffset = 30,
        postOffset = 45,
        soundKey = HabitAudioManager.SOUND_WATER_DROP
    ),
    QuickHabitTemplate(
        label = "💊 সকালের ওষুধ/ভেষজ",
        title = "সকালের খালি পেটে পানি ও ভেষজ/ওষুধ",
        category = HabitScheduleCalculator.CATEGORY_MEDICATION,
        anchorTime = "07:30",
        preOffset = 20,
        postOffset = 30,
        soundKey = HabitAudioManager.SOUND_MEDICATION_CHIME
    ),
    QuickHabitTemplate(
        label = "☕ বিকালের গ্রিন টি/পানি",
        title = "বিকালের পানি ও ভেষজ চা রুটিন",
        category = HabitScheduleCalculator.CATEGORY_HYDRATION,
        anchorTime = "17:00",
        preOffset = 15,
        postOffset = 30,
        soundKey = HabitAudioManager.SOUND_SOFT_BELL
    )
)

/**
 * Clean, Scannable Icon Menu & Quick Action Card placed directly below the Hero / Daily Tip section on HomeScreen.
 */
@Composable
fun HomeHabitQuickMenuCard(
    activeHabitCount: Int,
    nextPostCountdownText: String?,
    onOpenHabitModule: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.36f).solidOverSurface()
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("habit_reminder_menu_card")
            .clickable { onOpenHabitModule() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = onePxBorder(borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AlarmOn,
                                contentDescription = "Habit Planner",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "হ্যাবিট রিমাইন্ডার",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                shape = RoundedCornerShape(50),
                                border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "${HabitScheduleCalculator.toBengaliDigits(activeHabitCount.toString())}টি সক্রিয়",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = if (!nextPostCountdownText.isNullOrBlank()) {
                                "⏱️ $nextPostCountdownText"
                            } else {
                                "খাবারের আগে-পরে পানি পান, ওষুধ ও বাংলা ভয়েস রুটিন"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (!nextPostCountdownText.isNullOrBlank()) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (!nextPostCountdownText.isNullOrBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.width(8.dp))

                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "প্ল্যানার",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Redesigned Sleep Window DND & Interactive Bangla Voice / Sound Studio Tab Content.
 */
@Composable
fun SleepWindowAndAudioTabContent(
    sleepDndEnabled: Boolean,
    sleepStartHour: Int,
    sleepStartMinute: Int,
    sleepEndHour: Int,
    sleepEndMinute: Int,
    onToggleSleepDnd: (Boolean) -> Unit,
    onUpdateSleepWindow: (Int, Int, Int, Int) -> Unit,
    onPreviewAlert: (String, String, String) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Visual Night-Time DND Sleep Protection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f).solidOverSurface()
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🌙", fontSize = 20.sp)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ঘুমের সময় DND সুরক্ষা",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (sleepDndEnabled) "সক্রিয় • ঘুমের সময় পোস্ট-অ্যালার্ম সাইলেন্ট থাকবে" else "নিষ্ক্রিয় আছে",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (sleepDndEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = sleepDndEnabled,
                        onCheckedChange = onToggleSleepDnd,
                        modifier = Modifier.testTag("sleep_dnd_switch")
                    )
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Sleep Start Time Card
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        onUpdateSleepWindow(h, m, sleepEndHour, sleepEndMinute)
                                    },
                                    sleepStartHour,
                                    sleepStartMinute,
                                    false
                                ).show()
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🛌 ঘুম শুরুর সময়",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = HabitScheduleCalculator.formatHourMinuteBengali(sleepStartHour, sleepStartMinute),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Sleep End Time Card
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        onUpdateSleepWindow(sleepStartHour, sleepStartMinute, h, m)
                                    },
                                    sleepEndHour,
                                    sleepEndMinute,
                                    false
                                ).show()
                            }
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "☀️ ঘুম শেষের সময়",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = HabitScheduleCalculator.formatHourMinuteBengali(sleepEndHour, sleepEndMinute),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // 2. Interactive Bangla Voice (TTS) & Bundled Sound Studio
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f).solidOverSurface()
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🔊", fontSize = 20.sp)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "বাংলা ভয়েস ও সাউন্ড স্টুডিও",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "ট্যাপ করে বাংলা ভয়েস প্রম্পট ও কাস্টম সাউন্ড টোন শুনুন",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {
                        onPreviewAlert(
                            HabitScheduleCalculator.AUDIO_TTS_VOICE,
                            HabitAudioManager.SOUND_WATER_DROP,
                            "দুপুরের খাবারের ত্রিশ মিনিট বাকি। হজমশক্তি ভালো রাখতে এখন এক গ্লাস পানি পান করুন।"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("🗣️ বাংলা ভয়েস প্রম্পট টেস্ট করুন", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SoundStudioPreviewTile(
                        emoji = "💧",
                        title = "Water Drop",
                        subtitle = "পানি পান",
                        onClick = {
                            onPreviewAlert(
                                HabitScheduleCalculator.AUDIO_PRESET_TONE,
                                HabitAudioManager.SOUND_WATER_DROP,
                                ""
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SoundStudioPreviewTile(
                        emoji = "🔔",
                        title = "Soft Bell",
                        subtitle = "খাবার রুটিন",
                        onClick = {
                            onPreviewAlert(
                                HabitScheduleCalculator.AUDIO_PRESET_TONE,
                                HabitAudioManager.SOUND_SOFT_BELL,
                                ""
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    SoundStudioPreviewTile(
                        emoji = "💊",
                        title = "Med Chime",
                        subtitle = "ওষুধ ও ভেষজ",
                        onClick = {
                            onPreviewAlert(
                                HabitScheduleCalculator.AUDIO_PRESET_TONE,
                                HabitAudioManager.SOUND_MEDICATION_CHIME,
                                ""
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Concise Smart Schedule Formula Guide
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f).solidOverSurface()
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.30f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "💡 কীভাবে ৩-ধাপের রুটিন কাজ করে?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "১. প্রি-ইভেন্ট: খাবারের ৩০ মিনিট আগে পানি পানের কথা মনে করিয়ে দেয়।\n" +
                        "২. মূল সময়: খাবারের সময় নোটিফিকেশনে [✅ সম্পন্ন করেছি] বাটন দেখায়।\n" +
                        "৩. ডাইনামিক পোস্ট-ইভেন্ট: আপনি যখনই 'সম্পন্ন' চাপবেন, ঠিক সেই মুহূর্ত থেকে ৪৫ মিনিট পর পরবর্তী রিমাইন্ডার বাজবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun SoundStudioPreviewTile(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

/**
 * 3-Step Guided Wizard Dialog with a Live Calculated Timeline Preview Card at the top.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditorDialog(
    initialHabit: HabitEntity?,
    initialConfigs: List<ReminderConfigEntity>,
    onDismiss: () -> Unit,
    onPreviewAlert: (String, String, String) -> Unit,
    onSave: (
        existingId: String?,
        title: String,
        category: String,
        anchorTime: String,
        hasPre: Boolean,
        preOffset: Int,
        hasPost: Boolean,
        postOffset: Int,
        repeatDays: Set<Int>,
        audioMode: String,
        presetSoundKey: String,
        preMsg: String,
        exactMsg: String,
        postMsg: String
    ) -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableIntStateOf(1) } // 1, 2, or 3

    var title by remember { mutableStateOf(initialHabit?.title ?: "দুপুরের খাবার ও পানি পান রুটিন") }
    var category by remember { mutableStateOf(initialHabit?.category ?: HabitScheduleCalculator.CATEGORY_MEAL_LINKED) }
    var anchorTime by remember { mutableStateOf(initialHabit?.anchorTime ?: "14:00") }
    var hasPre by remember { mutableStateOf((initialHabit?.hasPreReminder ?: 1) == 1) }
    var preOffset by remember { mutableIntStateOf(initialHabit?.preOffsetMins ?: 30) }
    var hasPost by remember { mutableStateOf((initialHabit?.hasPostReminder ?: 1) == 1) }
    var postOffset by remember { mutableIntStateOf(initialHabit?.postOffsetMins ?: 45) }
    var repeatDays by remember {
        mutableStateOf(
            HabitScheduleCalculator.parseRepeatDays(initialHabit?.repeatDays ?: "[1,2,3,4,5,6,7]")
        )
    }

    val exactCfg = initialConfigs.firstOrNull { it.phase == HabitScheduleCalculator.PHASE_EXACT_EVENT }
    val preCfg = initialConfigs.firstOrNull { it.phase == HabitScheduleCalculator.PHASE_PRE_EVENT }
    val postCfg = initialConfigs.firstOrNull { it.phase == HabitScheduleCalculator.PHASE_POST_EVENT }

    var audioMode by remember {
        mutableStateOf(exactCfg?.audioMode ?: HabitScheduleCalculator.AUDIO_TTS_VOICE)
    }
    var presetSoundKey by remember {
        mutableStateOf(exactCfg?.presetSoundKey ?: HabitAudioManager.SOUND_WATER_DROP)
    }
    var preMsg by remember { mutableStateOf(preCfg?.messageTemplate ?: "") }
    var exactMsg by remember { mutableStateOf(exactCfg?.messageTemplate ?: "") }
    var postMsg by remember { mutableStateOf(postCfg?.messageTemplate ?: "") }
    var showCustomMessages by remember {
        mutableStateOf(preMsg.isNotBlank() || exactMsg.isNotBlank() || postMsg.isNotBlank())
    }

    // Live calculation of Pre-Event, Anchor, and Fallback/Dynamic Post-Event times
    val (anchorH, anchorM) = HabitScheduleCalculator.parseAnchorTime(anchorTime)
    val anchorCal = remember(anchorH, anchorM) {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, anchorH)
            set(Calendar.MINUTE, anchorM)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }
    val preCalculatedMillis = HabitScheduleCalculator.calculatePreEventTriggerMillis(anchorCal.timeInMillis, preOffset)
    val preTimeFormatted = HabitScheduleCalculator.formatMillisToBengaliTime(preCalculatedMillis)
    val anchorTimeFormatted = HabitScheduleCalculator.formatHourMinuteBengali(anchorH, anchorM)
    val postSampleMillis = HabitScheduleCalculator.calculateDynamicPostEventTriggerMillis(anchorCal.timeInMillis, postOffset)
    val postSampleFormatted = HabitScheduleCalculator.formatMillisToBengaliTime(postSampleMillis)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (initialHabit == null) "✨ নতুন হ্যাবিট রুটিন উইজার্ড" else "✏️ রুটিন কাস্টমাইজেশন উইজার্ড",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "ধাপ ${HabitScheduleCalculator.toBengaliDigits(currentStep.toString())}/৩ • ${
                                when (currentStep) {
                                    1 -> "রুটিনের নাম ও মূল সময়"
                                    2 -> "প্রি ও পোস্ট টাইমার এবং বার"
                                    else -> "বাংলা ভয়েস ও সাউন্ড স্টুডিও"
                                }
                            }",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                    }
                }

                // ── LIVE TIMELINE PREVIEW CARD (Always visible at top of wizard) ──
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f).solidOverSurface(),
                    shape = RoundedCornerShape(16.dp),
                    border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "👁️ লাইভ প্রিভিউ: ${title.ifBlank { "স্বাস্থ্য রুটিন" }}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${HabitScheduleCalculator.toBengaliDigits(repeatDays.size.toString())} দিন/সপ্তাহ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LivePreviewStepPill(
                                label = "১. প্রি-ইভেন্ট",
                                value = if (hasPre) preTimeFormatted else "বন্ধ",
                                sub = if (hasPre) "-${HabitScheduleCalculator.toBengaliDigits(preOffset.toString())}মি" else "",
                                active = hasPre,
                                modifier = Modifier.weight(1f)
                            )
                            Text("➔", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            LivePreviewStepPill(
                                label = "২. মূল সময়",
                                value = anchorTimeFormatted,
                                sub = anchorTime,
                                active = true,
                                isHighlight = true,
                                modifier = Modifier.weight(1f)
                            )
                            Text("➔", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            LivePreviewStepPill(
                                label = "৩. পোস্ট-ইভেন্ট",
                                value = if (hasPost) "+${HabitScheduleCalculator.toBengaliDigits(postOffset.toString())}মি পর" else "বন্ধ",
                                sub = if (hasPost) "আনুমানিক $postSampleFormatted" else "",
                                active = hasPost,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // ── 3-STEP INDICATOR PILLS ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    WizardStepTabChip(
                        stepNum = "১",
                        label = "নাম ও সময়",
                        selected = currentStep == 1,
                        completed = currentStep > 1,
                        onClick = { currentStep = 1 },
                        modifier = Modifier.weight(1f)
                    )
                    WizardStepTabChip(
                        stepNum = "২",
                        label = "টাইমার ও দিন",
                        selected = currentStep == 2,
                        completed = currentStep > 2,
                        onClick = { currentStep = 2 },
                        modifier = Modifier.weight(1f)
                    )
                    WizardStepTabChip(
                        stepNum = "৩",
                        label = "ভয়েস ও টোন",
                        selected = currentStep == 3,
                        completed = false,
                        onClick = { currentStep = 3 },
                        modifier = Modifier.weight(1f)
                    )
                }

                // ── STEP CONTENT ──
                AnimatedContent(targetState = currentStep, label = "wizard_step_content") { step ->
                    when (step) {
                        1 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                if (initialHabit == null) {
                                    Text(
                                        text = "⚡ এক ট্যাপে রেডিমেড টেমপ্লেট বেছে নিন:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        quickHabitTemplates.forEach { tpl ->
                                            AssistChip(
                                                onClick = {
                                                    title = tpl.title
                                                    category = tpl.category
                                                    anchorTime = tpl.anchorTime
                                                    preOffset = tpl.preOffset
                                                    postOffset = tpl.postOffset
                                                    presetSoundKey = tpl.soundKey
                                                },
                                                label = { Text(tpl.label, fontSize = 12.sp) }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = title,
                                    onValueChange = { title = it },
                                    label = { Text("রুটিনের নাম") },
                                    placeholder = { Text("যেমন: দুপুরের খাবার ও পানি পান") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("habit_title_input")
                                )

                                Text(
                                    text = "ক্যাটাগরি নির্বাচন করুন:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    habitCategoryOptions.chunked(2).forEach { rowItems ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            rowItems.forEach { (catKey, labelPair) ->
                                                val isSelected = category == catKey
                                                Surface(
                                                    color = if (isSelected) {
                                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                                                    } else {
                                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                                    },
                                                    shape = RoundedCornerShape(12.dp),
                                                    border = onePxBorder(
                                                        if (isSelected) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                    ),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable {
                                                            category = catKey
                                                            if (catKey == HabitScheduleCalculator.CATEGORY_MEDICATION) {
                                                                presetSoundKey = HabitAudioManager.SOUND_MEDICATION_CHIME
                                                            }
                                                        }
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Text(
                                                            text = labelPair.first,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                        )
                                                        Text(
                                                            text = labelPair.second,
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Large Digital Anchor Time Picker Card
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            TimePickerDialog(
                                                context,
                                                { _, h, m ->
                                                    anchorTime = "%02d:%02d".format(h, m)
                                                },
                                                anchorH,
                                                anchorM,
                                                false
                                            ).show()
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "⏰ মূল অ্যাঙ্কর সময় (T_anchor)",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "$anchorTimeFormatted ($anchorTime)",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        FilledTonalButton(
                                            onClick = {
                                                TimePickerDialog(
                                                    context,
                                                    { _, h, m ->
                                                        anchorTime = "%02d:%02d".format(h, m)
                                                    },
                                                    anchorH,
                                                    anchorM,
                                                    false
                                                ).show()
                                            }
                                        ) {
                                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("সময় বদলান")
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Pre-Event Offset Card
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "💧 ১. প্রি-ইভেন্ট রিমাইন্ডার",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = if (hasPre) "মূল সময়ের ${HabitScheduleCalculator.toBengaliDigits(preOffset.toString())} মিনিট আগে ($preTimeFormatted)" else "প্রি-রিমাইন্ডার বন্ধ আছে",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Switch(checked = hasPre, onCheckedChange = { hasPre = it })
                                        }
                                        if (hasPre) {
                                            Spacer(Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf(15, 20, 30, 45, 60).forEach { mins ->
                                                    FilterChip(
                                                        selected = preOffset == mins,
                                                        onClick = { preOffset = mins },
                                                        label = { Text("${HabitScheduleCalculator.toBengaliDigits(mins.toString())} মি. আগে") }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Post-Event Offset Card
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "✅ ৩. ডাইনামিক পোস্ট-ইভেন্ট রিমাইন্ডার",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = if (hasPost) "খাবার/কাজ সম্পন্নের ${HabitScheduleCalculator.toBengaliDigits(postOffset.toString())} মিনিট পর বাজবে" else "পোস্ট-রিমাইন্ডার বন্ধ আছে",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Switch(checked = hasPost, onCheckedChange = { hasPost = it })
                                        }
                                        if (hasPost) {
                                            Spacer(Modifier.height(6.dp))
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .horizontalScroll(rememberScrollState()),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf(15, 30, 45, 60, 90).forEach { mins ->
                                                    FilterChip(
                                                        selected = postOffset == mins,
                                                        onClick = { postOffset = mins },
                                                        label = { Text("${HabitScheduleCalculator.toBengaliDigits(mins.toString())} মি. পরে") }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 7-Day Selector
                                Text(
                                    text = "📅 সপ্তাহের কোন কোন দিন চালু থাকবে:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    bengaliWeekDays.forEach { (dayNum, dayLabel) ->
                                        val isSelected = dayNum in repeatDays
                                        Surface(
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(10.dp),
                                            border = onePxBorder(
                                                if (isSelected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    val next = repeatDays.toMutableSet()
                                                    if (isSelected && next.size > 1) {
                                                        next.remove(dayNum)
                                                    } else {
                                                        next.add(dayNum)
                                                    }
                                                    repeatDays = next
                                                }
                                        ) {
                                            Text(
                                                text = dayLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(vertical = 10.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        else -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "🔊 অ্যালার্ম বাজার ধরন নির্বাচন করুন:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        HabitScheduleCalculator.AUDIO_TTS_VOICE to "🗣️ বাংলা ভয়েস + টোন",
                                        HabitScheduleCalculator.AUDIO_PRESET_TONE to "🔔 শুধু সাউন্ড টোন",
                                        HabitScheduleCalculator.AUDIO_SYSTEM_DEFAULT to "📱 সিস্টেম সাউন্ড"
                                    ).forEach { (modeKey, modeLabel) ->
                                        FilterChip(
                                            selected = audioMode == modeKey,
                                            onClick = { audioMode = modeKey },
                                            label = { Text(modeLabel) }
                                        )
                                    }
                                }

                                Text(
                                    text = "🎵 সাউন্ড টোন প্রিসেট:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        HabitAudioManager.SOUND_WATER_DROP to "💧 Water Drop",
                                        HabitAudioManager.SOUND_SOFT_BELL to "🔔 Soft Bell",
                                        HabitAudioManager.SOUND_MEDICATION_CHIME to "💊 Med Chime"
                                    ).forEach { (soundKey, soundLabel) ->
                                        FilterChip(
                                            selected = presetSoundKey == soundKey,
                                            onClick = { presetSoundKey = soundKey },
                                            label = { Text(soundLabel, fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                FilledTonalButton(
                                    onClick = {
                                        val sample = exactMsg.ifBlank {
                                            "${title.ifBlank { "স্বাস্থ্য রুটিন" }} এর নির্ধারিত সময় হয়েছে।"
                                        }
                                        onPreviewAlert(audioMode, presetSoundKey, sample)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("🔊 ভয়েস ও সাউন্ড টোন বাজিয়ে শুনুন", fontWeight = FontWeight.Bold)
                                }

                                // Collapsible optional custom Bangla messages
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showCustomMessages = !showCustomMessages },
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "✍️ কাস্টম বাংলা ভয়েস মেসেজ (ঐচ্ছিক)",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Icon(
                                                imageVector = if (showCustomMessages) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null
                                            )
                                        }
                                        AnimatedVisibility(
                                            visible = showCustomMessages,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(top = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = preMsg,
                                                    onValueChange = { preMsg = it },
                                                    label = { Text("প্রি-ইভেন্ট ভয়েস/মেসেজ") },
                                                    maxLines = 2,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                OutlinedTextField(
                                                    value = exactMsg,
                                                    onValueChange = { exactMsg = it },
                                                    label = { Text("মূল সময়ের ভয়েস/মেসেজ") },
                                                    maxLines = 2,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                OutlinedTextField(
                                                    value = postMsg,
                                                    onValueChange = { postMsg = it },
                                                    label = { Text("পোস্ট-ইভেন্ট ভয়েস/মেসেজ") },
                                                    maxLines = 2,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── BOTTOM WIZARD NAVIGATION BAR ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep -= 1 },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("পূর্ববর্তী")
                        }
                    } else {
                        TextButton(onClick = onDismiss) {
                            Text("বাতিল")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (currentStep < 3) {
                            // Also allow quick direct save if editing an existing habit
                            if (initialHabit != null) {
                                OutlinedButton(
                                    onClick = {
                                        onSave(
                                            initialHabit.id,
                                            title,
                                            category,
                                            anchorTime,
                                            hasPre,
                                            max(5, preOffset),
                                            hasPost,
                                            max(5, postOffset),
                                            repeatDays,
                                            audioMode,
                                            presetSoundKey,
                                            preMsg,
                                            exactMsg,
                                            postMsg
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("সরাসরি সেভ")
                                }
                            }

                            Button(
                                onClick = { currentStep += 1 },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("পরবর্তী ধাপ", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        } else {
                            Button(
                                onClick = {
                                    onSave(
                                        initialHabit?.id,
                                        title,
                                        category,
                                        anchorTime,
                                        hasPre,
                                        max(5, preOffset),
                                        hasPost,
                                        max(5, postOffset),
                                        repeatDays,
                                        audioMode,
                                        presetSoundKey,
                                        preMsg,
                                        exactMsg,
                                        postMsg
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("save_habit_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("সেভ ও অ্যালার্ম চালু করুন", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LivePreviewStepPill(
    label: String,
    value: String,
    sub: String,
    active: Boolean,
    isHighlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        color = when {
            !active -> MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
            isHighlight -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            else -> MaterialTheme.colorScheme.surface
        },
        shape = RoundedCornerShape(10.dp),
        border = onePxBorder(
            if (isHighlight) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (sub.isNotBlank()) {
                Text(
                    text = sub,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun WizardStepTabChip(
    stepNum: String,
    label: String,
    selected: Boolean,
    completed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = when {
        selected -> MaterialTheme.colorScheme.primary
        completed -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }
    val fg = when {
        selected -> MaterialTheme.colorScheme.onPrimary
        completed -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        border = onePxBorder(
            if (selected || completed) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (completed) "✓" else stepNum,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = fg
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
