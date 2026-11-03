package com.health.knowledge.ui.habit

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
    HabitScheduleCalculator.CATEGORY_MEAL_LINKED to "🍽️ খাবার",
    HabitScheduleCalculator.CATEGORY_MEDICATION to "💊 ওষুধ",
    HabitScheduleCalculator.CATEGORY_HYDRATION to "💧 পানি",
    HabitScheduleCalculator.CATEGORY_CUSTOM to "⏰ অন্যান্য"
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
        title = "দুপুরের খাবার",
        category = HabitScheduleCalculator.CATEGORY_MEAL_LINKED,
        anchorTime = "14:00",
        preOffset = 30,
        postOffset = 45,
        soundKey = HabitAudioManager.SOUND_WATER_DROP
    ),
    QuickHabitTemplate(
        label = "🌙 রাতের খাবার",
        title = "রাতের খাবার",
        category = HabitScheduleCalculator.CATEGORY_MEAL_LINKED,
        anchorTime = "20:30",
        preOffset = 30,
        postOffset = 45,
        soundKey = HabitAudioManager.SOUND_WATER_DROP
    ),
    QuickHabitTemplate(
        label = "💊 সকালের ওষুধ",
        title = "সকালের ওষুধ",
        category = HabitScheduleCalculator.CATEGORY_MEDICATION,
        anchorTime = "07:30",
        preOffset = 20,
        postOffset = 30,
        soundKey = HabitAudioManager.SOUND_MEDICATION_CHIME
    ),
    QuickHabitTemplate(
        label = "💧 বিকালের পানি",
        title = "বিকালের পানি",
        category = HabitScheduleCalculator.CATEGORY_HYDRATION,
        anchorTime = "17:00",
        preOffset = 15,
        postOffset = 30,
        soundKey = HabitAudioManager.SOUND_SOFT_BELL
    )
)

/**
 * Clean, Scannable Icon Menu Card placed directly below the Hero / Daily Tip section on HomeScreen.
 * Uses pure icon button on the right (no text next to common arrow icon).
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                            contentDescription = "হ্যাবিট প্ল্যানার",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "হ্যাবিট প্ল্যানার",
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
                                text = "${HabitScheduleCalculator.toBengaliDigits(activeHabitCount.toString())}টি চালু",
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
                            "খাবার, পানি ও ওষুধের সময়সূচি"
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

            // Pure Icon Button without text
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "খুলুন",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Clean Night-Time Quiet Hours & Bangla Voice / Sound Studio Tab Content.
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
        // 1. Night Quiet Hours Card (No technical "DND" or "Sleep Window" terms)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f).solidOverSurface()
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
                                text = "রাতের নীরব সময়",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (sleepDndEnabled) "ঘুমের সময় অ্যালার্ম নীরব থাকবে" else "নীরব মোড বন্ধ আছে",
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
                                text = "🛌 শুরু",
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
                                text = "☀️ শেষ",
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

        // 2. Bangla Voice & Sound Studio Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f).solidOverSurface()
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("🗣️", fontSize = 20.sp)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "বাংলা ভয়েস ও টিউন",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "ট্যাপ করে ভয়েস ও টিউন শুনুন",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Pure Icon Button for Voice Test
                    FilledIconButton(
                        onClick = {
                            onPreviewAlert(
                                HabitScheduleCalculator.AUDIO_TTS_VOICE,
                                HabitAudioManager.SOUND_WATER_DROP,
                                "দুপুরের খাবারের ত্রিশ মিনিট বাকি। এখন এক গ্লাস পানি পান করুন।"
                            )
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = "বাংলা ভয়েস শুনুন",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SoundStudioPreviewTile(
                        emoji = "💧",
                        title = "পানির শব্দ",
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
                        title = "মৃদু ঘণ্টা",
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
                        title = "ওষুধ টিউন",
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
    }
}

@Composable
private fun SoundStudioPreviewTile(
    emoji: String,
    title: String,
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
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 3-Step Guided Wizard Dialog with a Live Preview Card and crisp 1-2 word Bengali labels.
 * Common icons (Close, Back, Next, Volume) use pure IconButtons without duplicate text.
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
    var currentStep by remember { mutableIntStateOf(1) }

    var title by remember { mutableStateOf(initialHabit?.title ?: "দুপুরের খাবার") }
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
                .fillMaxWidth(0.95f)
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
                    Text(
                        text = if (initialHabit == null) "নতুন রুটিন" else "রুটিন সম্পাদনা",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ")
                    }
                }

                // ── LIVE TIMELINE PREVIEW CARD (3 Equal Columns, Zero Overlap) ──
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f).solidOverSurface(),
                    shape = RoundedCornerShape(14.dp),
                    border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = title.ifBlank { "স্বাস্থ্য রুটিন" },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${HabitScheduleCalculator.toBengaliDigits(repeatDays.size.toString())} দিন",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LivePreviewStepPill(
                                label = if (hasPre) "${HabitScheduleCalculator.toBengaliDigits(preOffset.toString())}মি আগে" else "আগে",
                                value = if (hasPre) preTimeFormatted else "বন্ধ",
                                active = hasPre,
                                modifier = Modifier.weight(1f)
                            )
                            LivePreviewStepPill(
                                label = "মূল সময়",
                                value = anchorTimeFormatted,
                                active = true,
                                isHighlight = true,
                                modifier = Modifier.weight(1f)
                            )
                            LivePreviewStepPill(
                                label = if (hasPost) "${HabitScheduleCalculator.toBengaliDigits(postOffset.toString())}মি পরে" else "পরে",
                                value = if (hasPost) postSampleFormatted else "বন্ধ",
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
                        label = "সময়",
                        selected = currentStep == 1,
                        onClick = { currentStep = 1 },
                        modifier = Modifier.weight(1f)
                    )
                    WizardStepTabChip(
                        stepNum = "২",
                        label = "টাইমার",
                        selected = currentStep == 2,
                        onClick = { currentStep = 2 },
                        modifier = Modifier.weight(1f)
                    )
                    WizardStepTabChip(
                        stepNum = "৩",
                        label = "সাউন্ড",
                        selected = currentStep == 3,
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
                                        text = "প্রিসেট:",
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
                                    label = { Text("নাম") },
                                    placeholder = { Text("যেমন: দুপুরের খাবার") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("habit_title_input")
                                )

                                Text(
                                    text = "ধরন:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    habitCategoryOptions.forEach { (catKey, catLabel) ->
                                        val isSelected = category == catKey
                                        Surface(
                                            color = if (isSelected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f)
                                            },
                                            shape = RoundedCornerShape(10.dp),
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
                                            Text(
                                                text = catLabel,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                            )
                                        }
                                    }
                                }

                                // Clean Anchor Time Picker Card (No T_anchor or 14:00 jargon)
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                    shape = RoundedCornerShape(14.dp),
                                    border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)),
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
                                                text = "মূল সময়",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = anchorTimeFormatted,
                                                style = MaterialTheme.typography.titleMedium,
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
                                            },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text("বদলান", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        2 -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Pre-Event Offset Card
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
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
                                                    text = "আগের অ্যালার্ম",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = if (hasPre) "$preTimeFormatted (${HabitScheduleCalculator.toBengaliDigits(preOffset.toString())}মি আগে)" else "বন্ধ",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Switch(checked = hasPre, onCheckedChange = { hasPre = it })
                                        }
                                        if (hasPre) {
                                            Spacer(Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf(15, 20, 30, 45, 60).forEach { mins ->
                                                    val selected = preOffset == mins
                                                    Surface(
                                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = onePxBorder(
                                                            if (selected) MaterialTheme.colorScheme.primary
                                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                        ),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable { preOffset = mins }
                                                    ) {
                                                        Text(
                                                            text = "${HabitScheduleCalculator.toBengaliDigits(mins.toString())}মি",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.padding(vertical = 8.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Post-Event Offset Card
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
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
                                                    text = "পরের অ্যালার্ম",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = if (hasPost) "সম্পন্নের ${HabitScheduleCalculator.toBengaliDigits(postOffset.toString())}মি পরে" else "বন্ধ",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                            Switch(checked = hasPost, onCheckedChange = { hasPost = it })
                                        }
                                        if (hasPost) {
                                            Spacer(Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                listOf(15, 30, 45, 60, 90).forEach { mins ->
                                                    val selected = postOffset == mins
                                                    Surface(
                                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                                        shape = RoundedCornerShape(8.dp),
                                                        border = onePxBorder(
                                                            if (selected) MaterialTheme.colorScheme.primary
                                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                                        ),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clickable { postOffset = mins }
                                                    ) {
                                                        Text(
                                                            text = "${HabitScheduleCalculator.toBengaliDigits(mins.toString())}মি",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                            textAlign = TextAlign.Center,
                                                            modifier = Modifier.padding(vertical = 8.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // 7-Day Selector
                                Text(
                                    text = "দিন:",
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
                                    text = "ধরন:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        HabitScheduleCalculator.AUDIO_TTS_VOICE to "🗣️ বাংলা ভয়েস",
                                        HabitScheduleCalculator.AUDIO_PRESET_TONE to "🔔 টিউন",
                                        HabitScheduleCalculator.AUDIO_SYSTEM_DEFAULT to "📱 ডিফল্ট"
                                    ).forEach { (modeKey, modeLabel) ->
                                        val selected = audioMode == modeKey
                                        Surface(
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                                            shape = RoundedCornerShape(10.dp),
                                            border = onePxBorder(
                                                if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { audioMode = modeKey }
                                        ) {
                                            Text(
                                                text = modeLabel,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "সাউন্ড:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    // Pure Icon Button for audio test
                                    FilledTonalIconButton(
                                        onClick = {
                                            val sample = exactMsg.ifBlank {
                                                "${title.ifBlank { "স্বাস্থ্য রুটিন" }} এর সময় হয়েছে।"
                                            }
                                            onPreviewAlert(audioMode, presetSoundKey, sample)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.VolumeUp,
                                            contentDescription = "শুনুন",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        HabitAudioManager.SOUND_WATER_DROP to "💧 পানির শব্দ",
                                        HabitAudioManager.SOUND_SOFT_BELL to "🔔 মৃদু ঘণ্টা",
                                        HabitAudioManager.SOUND_MEDICATION_CHIME to "💊 ওষুধ টিউন"
                                    ).forEach { (soundKey, soundLabel) ->
                                        val selected = presetSoundKey == soundKey
                                        Surface(
                                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f),
                                            shape = RoundedCornerShape(10.dp),
                                            border = onePxBorder(
                                                if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                            ),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { presetSoundKey = soundKey }
                                        ) {
                                            Text(
                                                text = soundLabel,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
                                            )
                                        }
                                    }
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
                                                text = "কাস্টম মেসেজ (ঐচ্ছিক)",
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
                                                    label = { Text("আগের মেসেজ") },
                                                    maxLines = 2,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                OutlinedTextField(
                                                    value = exactMsg,
                                                    onValueChange = { exactMsg = it },
                                                    label = { Text("মূল সময়ের মেসেজ") },
                                                    maxLines = 2,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                                OutlinedTextField(
                                                    value = postMsg,
                                                    onValueChange = { postMsg = it },
                                                    label = { Text("পরের মেসেজ") },
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

                // ── BOTTOM WIZARD NAVIGATION BAR (Pure Icon for Back, Clean 1-2 Word Button for Next/Save) ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedIconButton(
                            onClick = { currentStep -= 1 },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "পেছনে"
                            )
                        }
                    } else {
                        TextButton(onClick = onDismiss) {
                            Text("বাতিল")
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentStep < 3) {
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
                                    Text("সেভ")
                                }
                            }

                            FilledIconButton(
                                onClick = { currentStep += 1 },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "পরবর্তী"
                                )
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
                                Text("সেভ করুন", fontWeight = FontWeight.Bold)
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
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WizardStepTabChip(
    stepNum: String,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    }
    val fg = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        color = bg,
        shape = RoundedCornerShape(10.dp),
        border = onePxBorder(
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "$stepNum. $label",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = fg,
                maxLines = 1
            )
        }
    }
}
