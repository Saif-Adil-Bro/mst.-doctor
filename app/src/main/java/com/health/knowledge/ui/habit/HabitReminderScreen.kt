package com.health.knowledge.ui.habit

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.knowledge.data.local.entity.HabitEntity
import com.health.knowledge.data.local.entity.HabitExecutionLogEntity
import com.health.knowledge.data.local.entity.ReminderConfigEntity
import com.health.knowledge.habit.HabitScheduleCalculator
import com.health.knowledge.ui.theme.onePxBorder
import com.health.knowledge.ui.theme.solidOverSurface
import com.health.knowledge.util.HealthNotificationHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitReminderScreen(
    onBack: () -> Unit,
    viewModel: HabitViewModel = hiltViewModel()
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val habits by viewModel.habits.collectAsState()
    val configsByHabit by viewModel.configsByHabit.collectAsState()
    val executionLogs by viewModel.executionLogs.collectAsState()
    val activeHabitCount by viewModel.activeHabitCount.collectAsState()
    val nowMillis by viewModel.nowMillis.collectAsState()

    val sleepDndEnabled by viewModel.sleepDndEnabled.collectAsState()
    val sleepStartHour by viewModel.sleepStartHour.collectAsState()
    val sleepStartMinute by viewModel.sleepStartMinute.collectAsState()
    val sleepEndHour by viewModel.sleepEndHour.collectAsState()
    val sleepEndMinute by viewModel.sleepEndMinute.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showEditorDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<HabitEntity?>(null) }
    var habitToDelete by remember { mutableStateOf<HabitEntity?>(null) }

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                context,
                "⚠️ সঠিক সময়ে অ্যালার্ম পেতে Notification পারমিশন Allow করুন",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.ensureInitialized(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !HealthNotificationHelper.hasNotificationPermission(context)
        ) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (showEditorDialog) {
        HabitEditorDialog(
            initialHabit = editingHabit,
            initialConfigs = editingHabit?.let { configsByHabit[it.id].orEmpty() }.orEmpty(),
            onDismiss = {
                showEditorDialog = false
                editingHabit = null
            },
            onPreviewAlert = { mode, soundKey, text ->
                viewModel.previewAudioAlert(mode, soundKey, text)
            },
            onSave = { existingId, title, category, anchorTime, hasPre, preOffset, hasPost, postOffset, repeatDays, audioMode, presetSoundKey, preMsg, exactMsg, postMsg ->
                viewModel.saveHabit(
                    existingHabitId = existingId,
                    title = title,
                    category = category,
                    anchorTime = anchorTime,
                    hasPreReminder = hasPre,
                    preOffsetMins = preOffset,
                    hasPostReminder = hasPost,
                    postOffsetMins = postOffset,
                    repeatDays = repeatDays,
                    audioMode = audioMode,
                    presetSoundKey = presetSoundKey,
                    preMessage = preMsg,
                    exactMessage = exactMsg,
                    postMessage = postMsg
                ) {
                    showEditorDialog = false
                    editingHabit = null
                    Toast.makeText(context, "✅ হ্যাবিট রুটিন ও এক্স্যাক্ট অ্যালার্ম সেভ হয়েছে!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    habitToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { habitToDelete = null },
            title = { Text("রুটিন মুছে ফেলবেন?") },
            text = { Text("\"${target.title}\" রুটিনটি এবং এর সকল অ্যালার্ম মুছে ফেলা হবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHabit(target.id)
                        habitToDelete = null
                        Toast.makeText(context, "🗑️ রুটিন মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("মুছে ফেলুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { habitToDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("habit_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "ফিরে যান",
                                tint = Color.White
                            )
                        }
                        Column {
                            Text(
                                text = "⏰ স্মার্ট হ্যাবিট ও রুটিন",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${HabitScheduleCalculator.toBengaliDigits(activeHabitCount.toString())}টি সক্রিয় রুটিন • অফলাইন এক্স্যাক্ট অ্যালার্ম",
                                color = Color.White.copy(alpha = 0.86f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = {
                            editingHabit = null
                            showEditorDialog = true
                        },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("add_habit_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("নতুন রুটিন", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("⏰ সক্রিয় রুটিন (${habits.size})", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("🌙 ঘুম ও ভয়েস", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("📜 লগ হিস্ট্রি", fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> {
                    ActiveHabitsListTab(
                        habits = habits,
                        configsByHabit = configsByHabit,
                        executionLogs = executionLogs,
                        nowMillis = nowMillis,
                        onMarkDoneClick = { habit ->
                            viewModel.markAnchorCompletedNow(habit) { postTimeMillis ->
                                if (postTimeMillis != null) {
                                    val timeFormatted = HabitScheduleCalculator.formatMillisToBengaliTime(postTimeMillis)
                                    Toast.makeText(
                                        context,
                                        "✅ সম্পন্ন! ${habit.postOffsetMins} মিনিট পর ($timeFormatted) পরবর্তী রিমাইন্ডার বাজবে",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    Toast.makeText(
                                        context,
                                        "✅ সম্পন্ন মার্ক করা হয়েছে (ঘুমের DND সময়ের কারণে পোস্ট-অ্যালার্ম সাইলেন্ট রাখা হয়েছে)",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        },
                        onSnoozeClick = { habit ->
                            viewModel.snoozeHabitPhase(habit, HabitScheduleCalculator.PHASE_EXACT_EVENT, 10) { nextTime ->
                                val formatted = HabitScheduleCalculator.formatMillisToBengaliTime(nextTime)
                                Toast.makeText(context, "💤 ১০ মিনিট স্নুজ করা হয়েছে ($formatted)", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onTestAlertClick = { habit, phase ->
                            viewModel.triggerTestHabitAlert(context, habit, phase)
                            Toast.makeText(context, "🔔 টেস্ট অ্যালার্ম ও বাংলা ভয়েস চালু হয়েছে!", Toast.LENGTH_SHORT).show()
                        },
                        onToggleActive = viewModel::toggleHabitActive,
                        onEditClick = { habit ->
                            editingHabit = habit
                            showEditorDialog = true
                        },
                        onDeleteClick = { habitToDelete = it },
                        onAddFirstHabit = {
                            editingHabit = null
                            showEditorDialog = true
                        }
                    )
                }

                1 -> {
                    SleepWindowAndAudioTabContent(
                        sleepDndEnabled = sleepDndEnabled,
                        sleepStartHour = sleepStartHour,
                        sleepStartMinute = sleepStartMinute,
                        sleepEndHour = sleepEndHour,
                        sleepEndMinute = sleepEndMinute,
                        onToggleSleepDnd = viewModel::setSleepDndEnabled,
                        onUpdateSleepWindow = viewModel::updateSleepWindow,
                        onPreviewAlert = viewModel::previewAudioAlert
                    )
                }

                2 -> {
                    ExecutionLogsTabContent(
                        logs = executionLogs,
                        habitsById = habits.associateBy { it.id },
                        onClearLogs = {
                            viewModel.clearHistoryLogs()
                            Toast.makeText(context, "🧹 সম্পন্ন লগ হিস্ট্রি মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveHabitsListTab(
    habits: List<HabitEntity>,
    configsByHabit: Map<String, List<ReminderConfigEntity>>,
    executionLogs: List<HabitExecutionLogEntity>,
    nowMillis: Long,
    onMarkDoneClick: (HabitEntity) -> Unit,
    onSnoozeClick: (HabitEntity) -> Unit,
    onTestAlertClick: (HabitEntity, String) -> Unit,
    onToggleActive: (HabitEntity) -> Unit,
    onEditClick: (HabitEntity) -> Unit,
    onDeleteClick: (HabitEntity) -> Unit,
    onAddFirstHabit: () -> Unit
) {
    val pendingPostLogsByHabit = remember(executionLogs, nowMillis) {
        executionLogs
            .filter {
                it.phase == HabitScheduleCalculator.PHASE_POST_EVENT &&
                    it.status == HabitScheduleCalculator.STATUS_PENDING &&
                    it.scheduledTime > nowMillis
            }
            .associateBy { it.habitId }
    }

    if (habits.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⏰", fontSize = 48.sp)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "কোনো হ্যাবিট রুটিন যুক্ত নেই",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "খাবারের আগে ও পরে পানি পান বা ওষুধের রিমাইন্ডার সেট করতে নিচের বাটনে ট্যাপ করুন",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = onAddFirstHabit) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("নতুন হ্যাবিট রুটিন যোগ করুন")
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "habit_intro_banner") {
            Surface(
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f).solidOverSurface(),
                shape = RoundedCornerShape(14.dp),
                border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💡", fontSize = 20.sp)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "খাবার বা মূল কাজ শেষ করার সাথে সাথে '✅ সম্পন্ন করেছি' বাটনে ট্যাপ করুন—তাহলে ঠিক সেই মুহূর্ত থেকে পরবর্তী পানি/ওষুধের পোস্ট-রিমাইন্ডার গণনা শুরু হবে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        items(habits, key = { it.id }) { habit ->
            HabitRoutineItemCard(
                habit = habit,
                configs = configsByHabit[habit.id].orEmpty(),
                pendingPostLog = pendingPostLogsByHabit[habit.id],
                nowMillis = nowMillis,
                onMarkDoneClick = { onMarkDoneClick(habit) },
                onSnoozeClick = { onSnoozeClick(habit) },
                onTestAlertClick = { phase -> onTestAlertClick(habit, phase) },
                onToggleActive = { onToggleActive(habit) },
                onEditClick = { onEditClick(habit) },
                onDeleteClick = { onDeleteClick(habit) }
            )
        }
    }
}

@Composable
private fun HabitRoutineItemCard(
    habit: HabitEntity,
    configs: List<ReminderConfigEntity>,
    pendingPostLog: HabitExecutionLogEntity?,
    nowMillis: Long,
    onMarkDoneClick: () -> Unit,
    onSnoozeClick: () -> Unit,
    onTestAlertClick: (String) -> Unit,
    onToggleActive: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isActive = habit.isActive == 1
    val cardBg = if (isActive) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f).solidOverSurface()
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.14f).solidOverSurface()
    }
    val borderColor = if (isActive) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    }

    val categoryBadge = when (habit.category) {
        HabitScheduleCalculator.CATEGORY_MEAL_LINKED -> "🍽️ খাবার ও পানি"
        HabitScheduleCalculator.CATEGORY_MEDICATION -> "💊 ওষুধ ও ভেষজ"
        HabitScheduleCalculator.CATEGORY_HYDRATION -> "💧 হাইড্রেশন"
        else -> "⏰ কাস্টম রুটিন"
    }

    val exactCfg = configs.firstOrNull { it.phase == HabitScheduleCalculator.PHASE_EXACT_EVENT }
    val audioBadge = when (exactCfg?.audioMode) {
        HabitScheduleCalculator.AUDIO_TTS_VOICE -> "🗣️ বাংলা ভয়েস + টোন"
        HabitScheduleCalculator.AUDIO_PRESET_TONE -> "🔔 প্রিসেট টোন"
        else -> "📱 সিস্টেম সাউন্ড"
    }

    val (anchorH, anchorM) = HabitScheduleCalculator.parseAnchorTime(habit.anchorTime)
    val preTimeMillis = HabitScheduleCalculator.calculateNextOccurrenceMillis(
        anchorTime = habit.anchorTime,
        signedOffsetMins = -habit.preOffsetMins,
        repeatDaysJson = habit.repeatDays,
        nowMillis = nowMillis - 12 * 3600_000L
    )
    val preFormatted = HabitScheduleCalculator.formatMillisToBengaliTime(preTimeMillis)
    val anchorFormatted = HabitScheduleCalculator.formatHourMinuteBengali(anchorH, anchorM)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = onePxBorder(borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Title + Category + Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = categoryBadge,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = audioBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = habit.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Switch(
                    checked = isActive,
                    onCheckedChange = { onToggleActive() }
                )
            }

            // 3-Phase Timeline Row (Pre -> Anchor -> Dynamic Post)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Phase 1: Pre-Event
                PhaseTimelineBadge(
                    title = "১. প্রি-ইভেন্ট",
                    subtitle = if (habit.hasPreReminder == 1) {
                        "${HabitScheduleCalculator.toBengaliDigits(habit.preOffsetMins.toString())}মি আগে ($preFormatted)"
                    } else {
                        "বন্ধ আছে"
                    },
                    enabled = habit.hasPreReminder == 1 && isActive,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTestAlertClick(HabitScheduleCalculator.PHASE_PRE_EVENT) }
                )

                // Phase 2: Exact Anchor Event
                PhaseTimelineBadge(
                    title = "২. মূল সময়",
                    subtitle = anchorFormatted,
                    enabled = isActive,
                    isPrimary = true,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTestAlertClick(HabitScheduleCalculator.PHASE_EXACT_EVENT) }
                )

                // Phase 3: Post-Event
                PhaseTimelineBadge(
                    title = "৩. পোস্ট-ইভেন্ট",
                    subtitle = if (habit.hasPostReminder == 1) {
                        "সম্পন্নের ${HabitScheduleCalculator.toBengaliDigits(habit.postOffsetMins.toString())}মি পর"
                    } else {
                        "বন্ধ আছে"
                    },
                    enabled = habit.hasPostReminder == 1 && isActive,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onTestAlertClick(HabitScheduleCalculator.PHASE_POST_EVENT) }
                )
            }

            // Active Dynamic Post-Event Countdown Banner (if scheduled)
            if (pendingPostLog != null && pendingPostLog.scheduledTime > nowMillis) {
                val remainingMins = max(1L, (pendingPostLog.scheduledTime - nowMillis + 59_000L) / 60_000L)
                val targetTimeFormatted = HabitScheduleCalculator.formatMillisToBengaliTime(pendingPostLog.scheduledTime)
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Text("⏱️", fontSize = 15.sp)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "পোস্ট-ইভেন্ট রিমাইন্ডার বাজবে: $targetTimeFormatted (${HabitScheduleCalculator.toBengaliDigits(remainingMins.toString())} মিনিট বাকি)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Repeat Days Row
            val activeDays = HabitScheduleCalculator.parseRepeatDays(habit.repeatDays)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "দিন:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                bengaliWeekDays.forEach { (dayNum, dayLabel) ->
                    val on = dayNum in activeDays
                    Surface(
                        color = if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f) else Color.Transparent,
                        shape = RoundedCornerShape(6.dp),
                        border = onePxBorder(
                            if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                            color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Action Buttons: [✅ খাবার/রুটিন সম্পন্ন করেছি (Mark Done)] + [🔔 টেস্ট] + [💤 স্নুজ] + [✏️/🗑️]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onMarkDoneClick,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mark_done_${habit.id}"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "সম্পন্ন করেছি (${HabitScheduleCalculator.toBengaliDigits(habit.postOffsetMins.toString())}মি টাইমার)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                FilledTonalIconButton(
                    onClick = { onTestAlertClick(HabitScheduleCalculator.PHASE_EXACT_EVENT) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "টেস্ট অ্যালার্ম ও ভয়েস",
                        modifier = Modifier.size(18.dp)
                    )
                }

                FilledTonalIconButton(
                    onClick = onSnoozeClick,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        Icons.Default.Snooze,
                        contentDescription = "১০ মিনিট স্নুজ",
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "এডিট রুটিন",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "ডিলিট রুটিন",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PhaseTimelineBadge(
    title: String,
    subtitle: String,
    enabled: Boolean,
    isPrimary: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        !enabled -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        isPrimary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        !enabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        isPrimary -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(10.dp),
        border = onePxBorder(borderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ExecutionLogsTabContent(
    logs: List<HabitExecutionLogEntity>,
    habitsById: Map<String, HabitEntity>,
    onClearLogs: () -> Unit
) {
    val dateFormatter = remember {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    }
    val baseSurface = MaterialTheme.colorScheme.surface

    if (logs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📜", fontSize = 44.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "এখনো কোনো রুটিন লগ রেকর্ড হয়নি",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "logs_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "রুটিন ও অ্যালার্ম ট্র্যাকিং লগ (${logs.size})",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onClearLogs) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("হিস্ট্রি মুছুন")
                }
            }
        }

        items(logs, key = { it.id }) { log ->
            val habitTitle = habitsById[log.habitId]?.title ?: "স্বাস্থ্য রুটিন"
            val phaseLabel = when (log.phase) {
                HabitScheduleCalculator.PHASE_PRE_EVENT -> "💧 প্রি-ইভেন্ট"
                HabitScheduleCalculator.PHASE_EXACT_EVENT -> "⏰ মূল ইভেন্ট"
                HabitScheduleCalculator.PHASE_POST_EVENT -> "✅ পোস্ট-ইভেন্ট"
                else -> log.phase
            }
            val (statusLabel, statusColor) = when (log.status) {
                HabitScheduleCalculator.STATUS_COMPLETED -> "✅ সম্পন্ন" to Color(0xFF2E7D32)
                HabitScheduleCalculator.STATUS_PENDING -> "⏳ অপেক্ষমাণ" to Color(0xFFE65100)
                HabitScheduleCalculator.STATUS_SNOOZED -> "💤 স্নুজড" to Color(0xFF1565C0)
                HabitScheduleCalculator.STATUS_DISMISSED -> "🔕 বন্ধ/DND" to Color(0xFF616161)
                else -> "⚠️ মিসড" to Color(0xFFC62828)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f).solidOverSurface()
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$habitTitle • $phaseLabel",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        val timeToShow = log.executedTime ?: log.scheduledTime
                        Text(
                            text = "সময়: ${dateFormatter.format(Date(timeToShow))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        color = statusColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(50),
                        border = onePxBorder(statusColor.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
