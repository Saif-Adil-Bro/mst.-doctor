package com.health.knowledge.ui.habit

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.health.knowledge.data.local.entity.HabitEntity
import com.health.knowledge.data.local.entity.ReminderConfigEntity
import com.health.knowledge.habit.HabitAlarmReceiver
import com.health.knowledge.habit.HabitAudioManager
import com.health.knowledge.habit.HabitScheduleCalculator
import com.health.knowledge.ui.theme.onePxBorder
import com.health.knowledge.ui.theme.solidOverSurface
import kotlin.math.max

val habitCategoryOptions = listOf(
    HabitScheduleCalculator.CATEGORY_MEAL_LINKED to ("🍽️ খাবার ও পানি" to "খাবারের আগে ও পরে পানি পানের রুটিন"),
    HabitScheduleCalculator.CATEGORY_MEDICATION to ("💊 ওষুধ ও ভেষজ" to "নির্দিষ্ট সময়ে ওষুধ বা ভেষজ সেবন"),
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

/**
 * Icon Menu & Quick Action Card placed directly below the Hero / Daily Tip section on HomeScreen.
 */
@Composable
fun HomeHabitQuickMenuCard(
    activeHabitCount: Int,
    nextPostCountdownText: String?,
    onOpenHabitModule: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.38f).solidOverSurface()
    val borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)

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
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.AlarmOn,
                                contentDescription = "Habit Reminder",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(26.dp)
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
                            text = "খাবারের আগে-পরে পানি পান, ওষুধ ও বাংলা ভয়েস অ্যালার্ম",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                            text = "খুলুন",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // Quick feature pills inside the icon menu card
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HabitFeatureMiniChip(icon = "💧", text = "খাবারের ৩০মি আগে পানি")
                HabitFeatureMiniChip(icon = "✅", text = "খাবার শেষে অটো ৪৫মি টাইমার")
                HabitFeatureMiniChip(icon = "🔊", text = "বাংলা ভয়েস ও টোন")
            }

            if (!nextPostCountdownText.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(10.dp),
                    border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⏱️", fontSize = 14.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = nextPostCountdownText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitFeatureMiniChip(icon: String, text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(8.dp),
        border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 11.sp)
            Spacer(Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Sleep Window DND & Voice/Sound Tester Tab Content.
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
    val baseSurface = MaterialTheme.colorScheme.surface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Do-Not-Disturb (DND) Sleep Protection Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f).solidOverSurface()
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🌙 ঘুমের সময় DND সুরক্ষা (Sleep Window)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "রাতের ঘুমের সময়ে কোনো পোস্ট-ইভেন্ট রিমাইন্ডার পড়লে অডিও স্বয়ংক্রিয়ভাবে সাইলেন্ট/বন্ধ থাকবে",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = sleepDndEnabled,
                        onCheckedChange = onToggleSleepDnd,
                        modifier = Modifier.testTag("sleep_dnd_switch")
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Sleep Start Time Button
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m ->
                                    onUpdateSleepWindow(h, m, sleepEndHour, sleepEndMinute)
                                },
                                sleepStartHour,
                                sleepStartMinute,
                                false
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ঘুম শুরুর সময়", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = HabitScheduleCalculator.formatHourMinuteBengali(sleepStartHour, sleepStartMinute),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Sleep End Time Button
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, h, m ->
                                    onUpdateSleepWindow(sleepStartHour, sleepStartMinute, h, m)
                                },
                                sleepEndHour,
                                sleepEndMinute,
                                false
                            ).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ঘুম শেষের সময়", style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = HabitScheduleCalculator.formatHourMinuteBengali(sleepEndHour, sleepEndMinute),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. Bangla TTS & Bundled Sound Presets Studio Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f).solidOverSurface()
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "🔊 বাংলা ভয়েস (TTS) ও সাউন্ড টোন টেস্টার",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "অ্যালার্ম বাজার সময় বাংলা ভয়েস প্রম্পট (bn-BD -> en-US ফলব্যাক) এবং কাস্টম সাউন্ড টোন কীভাবে শোনাবে তা নিচে ট্যাপ করে পরীক্ষা করুন:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FilledTonalButton(
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
                    Text("🗣️ বাংলা ভয়েস প্রম্পট + Water Drop টেস্ট করুন", fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            onPreviewAlert(
                                HabitScheduleCalculator.AUDIO_PRESET_TONE,
                                HabitAudioManager.SOUND_WATER_DROP,
                                ""
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Text("💧 Water Drop", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            onPreviewAlert(
                                HabitScheduleCalculator.AUDIO_PRESET_TONE,
                                HabitAudioManager.SOUND_SOFT_BELL,
                                ""
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Text("🔔 Soft Bell", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    OutlinedButton(
                        onClick = {
                            onPreviewAlert(
                                HabitScheduleCalculator.AUDIO_PRESET_TONE,
                                HabitAudioManager.SOUND_MEDICATION_CHIME,
                                ""
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp)
                    ) {
                        Text("💊 Med Chime", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 3. Mathematical Schedule Rules Explanation Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.28f).solidOverSurface()
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.30f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "📐 স্মার্ট অ্যাঙ্কর শিডিউলিং লজিক",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "• প্রি-ইভেন্ট (T_pre): মূল সময়ের ৩০ মিনিট আগে (কাস্টমাইজযোগ্য) পানি পানের রিমাইন্ডার দেয়।\n" +
                        "• মূল ইভেন্ট (T_anchor): ঠিক নির্ধারিত সময়ে নোটিফিকেশনে [সম্পন্ন করেছি] ও [১০মি স্নুজ] বাটন দেখায়।\n" +
                        "• ডাইনামিক পোস্ট-ইভেন্ট (T_post): আপনি যখনই 'সম্পন্ন করেছি' বাটনে ট্যাপ করবেন, ঠিক সেই মুহূর্ত (T_done) থেকে ৪৫ মিনিট পর পরবর্তী রিমাইন্ডার বাজবে। আর ট্যাপ করতে ভুলে গেলে ৩০ মিনিট গ্রেস পিরিয়ড ধরে স্বয়ংক্রিয় ফলব্যাক রিমাইন্ডার বাজবে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Full-width Add / Edit Habit Dialog.
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
    var title by remember { mutableStateOf(initialHabit?.title ?: "") }
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

    val (anchorH, anchorM) = HabitScheduleCalculator.parseAnchorTime(anchorTime)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (initialHabit == null) "➕ নতুন স্মার্ট হ্যাবিট রুটিন" else "✏️ রুটিন সম্পাদনা করুন",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "বন্ধ করুন")
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("রুটিনের নাম (যেমন: দুপুরের খাবার ও পানি)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("habit_title_input")
                )

                // Category Selector
                Text("ক্যাটাগরি নির্বাচন করুন:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    habitCategoryOptions.forEach { (catKey, labelPair) ->
                        FilterChip(
                            selected = category == catKey,
                            onClick = {
                                category = catKey
                                if (catKey == HabitScheduleCalculator.CATEGORY_MEDICATION) {
                                    presetSoundKey = HabitAudioManager.SOUND_MEDICATION_CHIME
                                }
                            },
                            label = { Text(labelPair.first) }
                        )
                    }
                }

                // Anchor Time Picker
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp),
                    border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "⏰ মূল অ্যাঙ্কর সময় (T_anchor)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${HabitScheduleCalculator.format24hToBengali(anchorTime)} ($anchorTime)",
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
                            }
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("সময় বদলান")
                        }
                    }
                }

                // Pre-Event Offset
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp),
                    border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💧 প্রি-ইভেন্ট রিমাইন্ডার ($preOffset মিনিট আগে)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Switch(checked = hasPre, onCheckedChange = { hasPre = it })
                        }
                        if (hasPre) {
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

                // Post-Event Offset
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(14.dp),
                    border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✅ ডাইনামিক পোস্ট-ইভেন্ট ($postOffset মিনিট পরে)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Switch(checked = hasPost, onCheckedChange = { hasPost = it })
                        }
                        if (hasPost) {
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

                // Repeat Days Selector (1..7)
                Text("সপ্তাহের যে দিনগুলোতে বাজবে:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    bengaliWeekDays.forEach { (dayNum, dayLabel) ->
                        val isSelected = dayNum in repeatDays
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val next = repeatDays.toMutableSet()
                                if (isSelected && next.size > 1) {
                                    next.remove(dayNum)
                                } else {
                                    next.add(dayNum)
                                }
                                repeatDays = next
                            },
                            label = { Text(dayLabel) }
                        )
                    }
                }

                // Audio Mode & Sound Tone Picker
                Text("অ্যালার্ম অডিও ও বাংলা ভয়েস মোড:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        HabitScheduleCalculator.AUDIO_TTS_VOICE to "🗣️ বাংলা ভয়েস + টোন",
                        HabitScheduleCalculator.AUDIO_PRESET_TONE to "🔔 প্রিসেট সাউন্ড টোন",
                        HabitScheduleCalculator.AUDIO_SYSTEM_DEFAULT to "📱 সিস্টেম ডিফল্ট"
                    ).forEach { (modeKey, modeLabel) ->
                        FilterChip(
                            selected = audioMode == modeKey,
                            onClick = { audioMode = modeKey },
                            label = { Text(modeLabel) }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        HabitAudioManager.SOUND_WATER_DROP to "💧 Water Drop",
                        HabitAudioManager.SOUND_SOFT_BELL to "🔔 Soft Bell",
                        HabitAudioManager.SOUND_MEDICATION_CHIME to "💊 Med Chime"
                    ).forEach { (soundKey, soundLabel) ->
                        FilterChip(
                            selected = presetSoundKey == soundKey,
                            onClick = { presetSoundKey = soundKey },
                            label = { Text(soundLabel) }
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val sample = exactMsg.ifBlank {
                                "${title.ifBlank { "স্বাস্থ্য রুটিন" }} এর সময় হয়েছে।"
                            }
                            onPreviewAlert(audioMode, presetSoundKey, sample)
                        }
                    ) {
                        Text("🔊 টেস্ট")
                    }
                }

                // Optional custom voice/notification messages
                OutlinedTextField(
                    value = preMsg,
                    onValueChange = { preMsg = it },
                    label = { Text("প্রি-ইভেন্ট মেসেজ/ভয়েস (ঐচ্ছিক)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = exactMsg,
                    onValueChange = { exactMsg = it },
                    label = { Text("মূল সময়ের মেসেজ/ভয়েস (ঐচ্ছিক)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = postMsg,
                    onValueChange = { postMsg = it },
                    label = { Text("পোস্ট-ইভেন্ট মেসেজ/ভয়েস (ঐচ্ছিক)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("বাতিল")
                    }
                    Spacer(Modifier.width(8.dp))
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
