package com.health.knowledge.ui.habit

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.ui.graphics.StrokeCap
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
import java.util.Calendar
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
                                text = "⏰ হ্যাবিট প্ল্যানার",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${HabitScheduleCalculator.toBengaliDigits(activeHabitCount.toString())}টি সক্রিয় রুটিন • অফলাইন স্মার্ট অ্যালার্ম",
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
                        text = { Text("⏰ রুটিন (${habits.size})", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("🌙 ঘুম ও ভয়েস", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("📜 হিস্ট্রি", fontWeight = FontWeight.SemiBold) }
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
                    ActiveHabitsDashboardTab(
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
private fun ActiveHabitsDashboardTab(
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
    // Track which habit cards are expanded (Progressive Disclosure: collapsed by default)
    var expandedHabitIds by remember { mutableStateOf(setOf<String>()) }

    val pendingPostLogsByHabit = remember(executionLogs, nowMillis) {
        executionLogs
            .filter {
                it.phase == HabitScheduleCalculator.PHASE_POST_EVENT &&
                    it.status == HabitScheduleCalculator.STATUS_PENDING &&
                    it.scheduledTime > nowMillis
            }
            .associateBy { it.habitId }
    }

    // Calculate today's completed habits count for the Progress Ring
    val startOfTodayMillis = remember(nowMillis) {
        Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val completedHabitIdsToday = remember(executionLogs, startOfTodayMillis) {
        executionLogs
            .filter {
                it.status == HabitScheduleCalculator.STATUS_COMPLETED &&
                    (it.executedTime ?: it.scheduledTime) >= startOfTodayMillis
            }
            .map { it.habitId }
            .toSet()
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
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Top Dashboard Hero Card: Today's Progress Ring + Next Alarm / Post-Meal Countdown Banner
        item(key = "dashboard_summary_hero") {
            HabitDashboardSummaryHeroCard(
                habits = habits,
                completedHabitIdsToday = completedHabitIdsToday,
                pendingPostLogsByHabit = pendingPostLogsByHabit,
                nowMillis = nowMillis
            )
        }

        // Section Header with Expand/Collapse All toggle
        item(key = "routines_section_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📋 আপনার রুটিন তালিকা (বিস্তারিত দেখতে কার্ডে ট্যাপ করুন)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val allExpanded = expandedHabitIds.size == habits.size
                Text(
                    text = if (allExpanded) "সব সংক্ষিপ্ত করুন ▲" else "সব প্রসারিত করুন ▼",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        expandedHabitIds = if (allExpanded) {
                            emptySet()
                        } else {
                            habits.map { it.id }.toSet()
                        }
                    }
                )
            }
        }

        // 2. Collapsible Routine Cards (Progressive Disclosure)
        items(habits, key = { it.id }) { habit ->
            val isExpanded = habit.id in expandedHabitIds
            CollapsibleHabitRoutineCard(
                habit = habit,
                configs = configsByHabit[habit.id].orEmpty(),
                pendingPostLog = pendingPostLogsByHabit[habit.id],
                isCompletedToday = habit.id in completedHabitIdsToday,
                isExpanded = isExpanded,
                nowMillis = nowMillis,
                onToggleExpand = {
                    expandedHabitIds = if (isExpanded) {
                        expandedHabitIds - habit.id
                    } else {
                        expandedHabitIds + habit.id
                    }
                },
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

/**
 * 1. Top Dashboard Hero Card: Today's Progress Ring + Next Alarm / Post-Meal Countdown Banner.
 */
@Composable
private fun HabitDashboardSummaryHeroCard(
    habits: List<HabitEntity>,
    completedHabitIdsToday: Set<String>,
    pendingPostLogsByHabit: Map<String, HabitExecutionLogEntity>,
    nowMillis: Long
) {
    val activeHabits = remember(habits) { habits.filter { it.isActive == 1 } }
    val totalActive = activeHabits.size.coerceAtLeast(1)
    val completedActiveCount = activeHabits.count { it.id in completedHabitIdsToday }
    val rawProgress = (completedActiveCount.toFloat() / totalActive.toFloat()).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 500),
        label = "habit_progress_ring"
    )
    val percentInt = (rawProgress * 100).toInt()

    // Find either the earliest active dynamic post-meal countdown OR the next upcoming scheduled trigger
    val earliestPendingPost = pendingPostLogsByHabit.values.minByOrNull { it.scheduledTime }

    val nextUpcomingTriggerPair = remember(activeHabits, nowMillis) {
        activeHabits.flatMap { habit ->
            val list = mutableListOf<Triple<HabitEntity, String, Long>>()
            if (habit.hasPreReminder == 1) {
                val preMillis = HabitScheduleCalculator.calculateNextOccurrenceMillis(
                    anchorTime = habit.anchorTime,
                    signedOffsetMins = -habit.preOffsetMins,
                    repeatDaysJson = habit.repeatDays,
                    nowMillis = nowMillis
                )
                list.add(Triple(habit, "প্রি-রিমাইন্ডার", preMillis))
            }
            val exactMillis = HabitScheduleCalculator.calculateNextOccurrenceMillis(
                anchorTime = habit.anchorTime,
                signedOffsetMins = 0,
                repeatDaysJson = habit.repeatDays,
                nowMillis = nowMillis
            )
            list.add(Triple(habit, "মূল সময়", exactMillis))
            list
        }.minByOrNull { it.third }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_dashboard_hero_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.36f).solidOverSurface()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.40f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left: Animated Today's Progress Ring
            Box(
                modifier = Modifier.size(74.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    strokeWidth = 7.dp,
                    strokeCap = StrokeCap.Round
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 7.dp,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${HabitScheduleCalculator.toBengaliDigits(percentInt.toString())}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "${HabitScheduleCalculator.toBengaliDigits(completedActiveCount.toString())}/${HabitScheduleCalculator.toBengaliDigits(activeHabits.size.toString())} সম্পন্ন",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Right: Next Alarm Banner / Active Post-Meal Countdown
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "আজকের রুটিন অগ্রগতি",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (earliestPendingPost != null) {
                    val habitTitle = habits.firstOrNull { it.id == earliestPendingPost.habitId }?.title ?: "রুটিন"
                    val minsLeft = max(1L, (earliestPendingPost.scheduledTime - nowMillis + 59_000L) / 60_000L)
                    val timeFormatted = HabitScheduleCalculator.formatMillisToBengaliTime(earliestPendingPost.scheduledTime)

                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Text(
                                text = "⏱️ চলমান পোস্ট-রিমাইন্ডার (${HabitScheduleCalculator.toBengaliDigits(minsLeft.toString())} মি. বাকি)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "$habitTitle • $timeFormatted",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else if (nextUpcomingTriggerPair != null) {
                    val (nextHabit, phaseName, triggerTime) = nextUpcomingTriggerPair
                    val timeFormatted = HabitScheduleCalculator.formatMillisToBengaliTime(triggerTime)

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Text(
                                text = "🔔 পরবর্তী অ্যালার্ম • $phaseName ($timeFormatted)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = nextHabit.title,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Text(
                        text = "সকল রুটিন অফলাইনে সক্রিয় রয়েছে।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * 2. Clean Collapsible Routine Card (Progressive Disclosure):
 * - Collapsed View: Shows only Category Icon, Title, Main Anchor Time, and primary [✅ সম্পন্ন] button.
 * - Expanded View (on card tap): Reveals the 3-step visual timeline, active days, and secondary actions.
 */
@Composable
private fun CollapsibleHabitRoutineCard(
    habit: HabitEntity,
    configs: List<ReminderConfigEntity>,
    pendingPostLog: HabitExecutionLogEntity?,
    isCompletedToday: Boolean,
    isExpanded: Boolean,
    nowMillis: Long,
    onToggleExpand: () -> Unit,
    onMarkDoneClick: () -> Unit,
    onSnoozeClick: () -> Unit,
    onTestAlertClick: (String) -> Unit,
    onToggleActive: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val isActive = habit.isActive == 1
    val cardBg = if (isActive) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f).solidOverSurface()
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f).solidOverSurface()
    }
    val borderColor = when {
        pendingPostLog != null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        isActive -> MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    }

    val categoryEmoji = when (habit.category) {
        HabitScheduleCalculator.CATEGORY_MEAL_LINKED -> "🍽️"
        HabitScheduleCalculator.CATEGORY_MEDICATION -> "💊"
        HabitScheduleCalculator.CATEGORY_HYDRATION -> "💧"
        else -> "⏰"
    }

    val (anchorH, anchorM) = HabitScheduleCalculator.parseAnchorTime(habit.anchorTime)
    val anchorFormatted = HabitScheduleCalculator.formatHourMinuteBengali(anchorH, anchorM)
    val preTimeMillis = HabitScheduleCalculator.calculateNextOccurrenceMillis(
        anchorTime = habit.anchorTime,
        signedOffsetMins = -habit.preOffsetMins,
        repeatDaysJson = habit.repeatDays,
        nowMillis = nowMillis - 12 * 3600_000L
    )
    val preFormatted = HabitScheduleCalculator.formatMillisToBengaliTime(preTimeMillis)

    val exactCfg = configs.firstOrNull { it.phase == HabitScheduleCalculator.PHASE_EXACT_EVENT }
    val audioBadge = when (exactCfg?.audioMode) {
        HabitScheduleCalculator.AUDIO_TTS_VOICE -> "🗣️ বাংলা ভয়েস"
        HabitScheduleCalculator.AUDIO_PRESET_TONE -> "🔔 সাউন্ড টোন"
        else -> "📱 সিস্টেম সাউন্ড"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("habit_card_${habit.id}")
            .clickable { onToggleExpand() },
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
            // ── COLLAPSED COMPACT ROW (Only Main Time + Title + Mark Done Button) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Category Icon Box
                    Surface(
                        color = if (isActive) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = onePxBorder(
                            if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(categoryEmoji, fontSize = 20.sp)
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = habit.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(3.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "⏰ $anchorFormatted",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (pendingPostLog != null && pendingPostLog.scheduledTime > nowMillis) {
                                val minsLeft = max(1L, (pendingPostLog.scheduledTime - nowMillis + 59_000L) / 60_000L)
                                Surface(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "⏱️ ${HabitScheduleCalculator.toBengaliDigits(minsLeft.toString())}মি বাকি",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (isCompletedToday) {
                                Surface(
                                    color = Color(0xFF2E7D32).copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "✓ আজ সম্পন্ন",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (!isActive) {
                                Text(
                                    text = "• বন্ধ আছে",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Primary Mark Done Button + Expand Chevron
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = onMarkDoneClick,
                        modifier = Modifier.testTag("mark_done_${habit.id}"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "সম্পন্ন",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "সংক্ষিপ্ত করুন" else "বিস্তারিত দেখুন",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── EXPANDED DETAILS SECTION (Revealed on Card Tap) ──
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(
                        thickness = 0.8.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    // Active Post-Event Countdown Banner (if active)
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
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⏱️", fontSize = 15.sp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "পরবর্তী পোস্ট-রিমাইন্ডার বাজবে: $targetTimeFormatted (${HabitScheduleCalculator.toBengaliDigits(remainingMins.toString())} মিনিট বাকি)",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // 3-Phase Connected Timeline Badges (Tap any phase to test voice/sound)
                    Text(
                        text = "৩-ধাপের অ্যালার্ম টাইমলাইন (যেকোনো ধাপে ট্যাপ করে ভয়েস টেস্ট করুন):",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                        Text("➔", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)

                        PhaseTimelineBadge(
                            title = "২. মূল সময়",
                            subtitle = anchorFormatted,
                            enabled = isActive,
                            isPrimary = true,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onTestAlertClick(HabitScheduleCalculator.PHASE_EXACT_EVENT) }
                        )

                        Text("➔", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)

                        PhaseTimelineBadge(
                            title = "৩. পোস্ট-ইভেন্ট",
                            subtitle = if (habit.hasPostReminder == 1) {
                                "সম্পন্নের +${HabitScheduleCalculator.toBengaliDigits(habit.postOffsetMins.toString())}মি পর"
                            } else {
                                "বন্ধ আছে"
                            },
                            enabled = habit.hasPostReminder == 1 && isActive,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onTestAlertClick(HabitScheduleCalculator.PHASE_POST_EVENT) }
                        )
                    }

                    // Weekdays + Audio Badge + Active Switch Row
                    val activeDays = HabitScheduleCalculator.parseRepeatDays(habit.repeatDays)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            bengaliWeekDays.forEach { (dayNum, dayLabel) ->
                                val on = dayNum in activeDays
                                Surface(
                                    color = if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp),
                                    border = onePxBorder(
                                        if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                        else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Text(
                                        text = dayLabel,
                                        fontSize = 10.sp,
                                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                                        color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = audioBadge,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isActive) "চালু" else "বন্ধ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(4.dp))
                            Switch(
                                checked = isActive,
                                onCheckedChange = { onToggleActive() }
                            )
                        }
                    }

                    // Clean Secondary Action Toolbar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { onTestAlertClick(HabitScheduleCalculator.PHASE_EXACT_EVENT) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("টেস্ট ভয়েস", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = onSnoozeClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("১০মি স্নুজ", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        FilledTonalIconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "এডিট রুটিন",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        FilledTonalIconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(36.dp),
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "ডিলিট রুটিন",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
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
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
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
    var statusFilter by remember { mutableStateOf("ALL") }
    val dateFormatter = remember {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    }

    val filteredLogs = remember(logs, statusFilter) {
        when (statusFilter) {
            "COMPLETED" -> logs.filter { it.status == HabitScheduleCalculator.STATUS_COMPLETED }
            "PENDING" -> logs.filter { it.status == HabitScheduleCalculator.STATUS_PENDING }
            "OTHER" -> logs.filter {
                it.status != HabitScheduleCalculator.STATUS_COMPLETED &&
                    it.status != HabitScheduleCalculator.STATUS_PENDING
            }
            else -> logs
        }
    }

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
        item(key = "logs_header_and_filters") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "রুটিন ট্র্যাকিং লগ (${filteredLogs.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onClearLogs) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("হিস্ট্রি মুছুন")
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL" to "সব (${logs.size})",
                        "COMPLETED" to "✅ সম্পন্ন",
                        "PENDING" to "⏳ অপেক্ষমাণ",
                        "OTHER" to "💤 স্নুজড/বন্ধ"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = statusFilter == key,
                            onClick = { statusFilter = key },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }

        items(filteredLogs, key = { it.id }) { log ->
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
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f).solidOverSurface()
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
                            text = habitTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        val timeToShow = log.executedTime ?: log.scheduledTime
                        Text(
                            text = "$phaseLabel • ${dateFormatter.format(Date(timeToShow))}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.width(8.dp))

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
