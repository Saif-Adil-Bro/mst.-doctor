package com.health.knowledge.ui.habit

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import com.health.knowledge.habit.HabitDateAndReportEngine
import com.health.knowledge.habit.HabitPeriodReport
import com.health.knowledge.habit.HabitScheduleCalculator
import com.health.knowledge.habit.ReportPeriod
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
    var showExitConfirmDialog by remember { mutableStateOf(false) }

    BackHandler { showExitConfirmDialog = true }

    val context = LocalContext.current
    val habits by viewModel.habits.collectAsState()
    val configsByHabit by viewModel.configsByHabit.collectAsState()
    val executionLogs by viewModel.executionLogs.collectAsState()
    val activeHabitCount by viewModel.activeHabitCount.collectAsState()
    val nowMillis by viewModel.nowMillis.collectAsState()
    val selectedDateMillis by viewModel.selectedDateMillis.collectAsState()
    val reportPeriod by viewModel.reportPeriod.collectAsState()

    val sleepDndEnabled by viewModel.sleepDndEnabled.collectAsState()
    val sleepStartHour by viewModel.sleepStartHour.collectAsState()
    val sleepStartMinute by viewModel.sleepStartMinute.collectAsState()
    val sleepEndHour by viewModel.sleepEndHour.collectAsState()
    val sleepEndMinute by viewModel.sleepEndMinute.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var showEditorDialog by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<HabitEntity?>(null) }
    var habitToDelete by remember { mutableStateOf<HabitEntity?>(null) }

    val dateStripItems = remember(habits, executionLogs, selectedDateMillis, nowMillis) {
        HabitDateAndReportEngine.buildDateStripItems(
            habits = habits,
            logs = executionLogs,
            selectedDateMillis = selectedDateMillis,
            nowMillis = nowMillis
        )
    }

    val periodReport = remember(habits, executionLogs, selectedDateMillis, nowMillis, reportPeriod) {
        HabitDateAndReportEngine.computePeriodReport(
            habits = habits,
            logs = executionLogs,
            selectedDateMillis = selectedDateMillis,
            nowMillis = nowMillis,
            period = reportPeriod
        )
    }

    val isViewingToday = remember(selectedDateMillis, nowMillis) {
        HabitDateAndReportEngine.isSameDay(selectedDateMillis, nowMillis)
    }

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
                    Toast.makeText(context, "✅ সেভ হয়েছে!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    habitToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { habitToDelete = null },
            title = { Text("মুছে ফেলবেন?") },
            text = { Text("\"${target.title}\" মুছে ফেলা হবে।") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteHabit(target.id)
                        habitToDelete = null
                        Toast.makeText(context, "মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("মুছুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { habitToDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "হ্যাবিট প্ল্যানার বন্ধ করবেন?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("আপনি কি হ্যাবিট প্ল্যানার পেজটি ক্লোজ করে ফিরে যেতে চান?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmDialog = false
                        onBack()
                    },
                    modifier = Modifier.testTag("habit_exit_confirm_ok")
                ) {
                    Text("ওকে")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showExitConfirmDialog = false },
                    modifier = Modifier.testTag("habit_exit_confirm_cancel")
                ) {
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
                            onClick = { showExitConfirmDialog = true },
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
                                text = "হ্যাবিট প্ল্যানার",
                                color = Color.White,
                                fontSize = 19.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${HabitScheduleCalculator.toBengaliDigits(activeHabitCount.toString())}টি চালু",
                                color = Color.White.copy(alpha = 0.86f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Pure Icon Button for '+' without text
                    Surface(
                        color = Color.White,
                        shape = CircleShape,
                        modifier = Modifier
                            .size(40.dp)
                            .testTag("add_habit_button")
                            .clickable {
                                editingHabit = null
                                showEditorDialog = true
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "নতুন রুটিন",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
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
                        text = { Text("⏰ তালিকা", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("🌙 সাউন্ড", fontWeight = FontWeight.SemiBold) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("📜 ইতিহাস", fontWeight = FontWeight.SemiBold) }
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Horizontal Scrollable Date Strip on Tracker & History Tabs
            if (selectedTab == 0 || selectedTab == 2) {
                HorizontalDateStripBar(
                    dateItems = dateStripItems,
                    selectedDateMillis = selectedDateMillis,
                    onSelectDate = viewModel::selectDate,
                    onJumpToToday = viewModel::jumpToToday
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> {
                        ActiveHabitsDashboardTab(
                            habits = habits,
                            configsByHabit = configsByHabit,
                            executionLogs = executionLogs,
                            selectedDateMillis = selectedDateMillis,
                            isViewingToday = isViewingToday,
                            selectedPeriod = reportPeriod,
                            periodReport = periodReport,
                            nowMillis = nowMillis,
                            onSelectPeriod = viewModel::selectReportPeriod,
                            onToggleHabitForDate = { habit ->
                                viewModel.toggleHabitForDate(habit, selectedDateMillis) { isNowCompleted, postTimeMillis, blockedAsFuture ->
                                    when {
                                        blockedAsFuture -> {
                                            val anchorBn = HabitScheduleCalculator.format24hToBengali(habit.anchorTime)
                                            Toast.makeText(
                                                context,
                                                "🔒 বর্তমান সময়ের পরবর্তী ($anchorBn) রুটিনে এখনই টিক দেওয়া যাবে না",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        isNowCompleted && postTimeMillis != null -> {
                                            val timeFormatted = HabitScheduleCalculator.formatMillisToBengaliTime(postTimeMillis)
                                            Toast.makeText(
                                                context,
                                                "✅ সম্পন্ন! পরের অ্যালার্ম: $timeFormatted",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                        isNowCompleted -> {
                                            Toast.makeText(context, "✅ সম্পন্ন!", Toast.LENGTH_SHORT).show()
                                        }
                                        else -> {
                                            Toast.makeText(context, "↩️ বাকি হিসেবে মার্ক করা হয়েছে", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            },
                            onSnoozeClick = { habit ->
                                viewModel.snoozeHabitPhase(habit, HabitScheduleCalculator.PHASE_EXACT_EVENT, 10) { nextTime ->
                                    val formatted = HabitScheduleCalculator.formatMillisToBengaliTime(nextTime)
                                    Toast.makeText(context, "💤 ১০মি স্নুজ ($formatted)", Toast.LENGTH_SHORT).show()
                                }
                            },
                            onTestAlertClick = { habit, phase ->
                                viewModel.triggerTestHabitAlert(context, habit, phase)
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
                            habits = habits,
                            selectedPeriod = reportPeriod,
                            periodReport = periodReport,
                            pendingPostLogsByHabit = emptyMap(),
                            isViewingToday = isViewingToday,
                            nowMillis = nowMillis,
                            onSelectPeriod = viewModel::selectReportPeriod,
                            onClearLogs = {
                                viewModel.clearHistoryLogs()
                                Toast.makeText(context, "ইতিহাস মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
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
    selectedDateMillis: Long,
    isViewingToday: Boolean,
    selectedPeriod: ReportPeriod,
    periodReport: HabitPeriodReport,
    nowMillis: Long,
    onSelectPeriod: (ReportPeriod) -> Unit,
    onToggleHabitForDate: (HabitEntity) -> Unit,
    onSnoozeClick: (HabitEntity) -> Unit,
    onTestAlertClick: (HabitEntity, String) -> Unit,
    onToggleActive: (HabitEntity) -> Unit,
    onEditClick: (HabitEntity) -> Unit,
    onDeleteClick: (HabitEntity) -> Unit,
    onAddFirstHabit: () -> Unit
) {
    var expandedHabitIds by remember { mutableStateOf(setOf<String>()) }

    val pendingPostLogsByHabit = remember(executionLogs, nowMillis, isViewingToday) {
        if (!isViewingToday) emptyMap()
        else {
            executionLogs
                .filter {
                    it.phase == HabitScheduleCalculator.PHASE_POST_EVENT &&
                        it.status == HabitScheduleCalculator.STATUS_PENDING &&
                        it.scheduledTime > nowMillis
                }
                .associateBy { it.habitId }
        }
    }

    val completedHabitIdsForSelectedDate = remember(executionLogs, selectedDateMillis) {
        HabitDateAndReportEngine.getCompletedHabitIdsForDate(
            logs = executionLogs,
            dateMillis = selectedDateMillis
        )
    }

    val habitsForSelectedDate = remember(habits, selectedDateMillis) {
        val scheduled = HabitDateAndReportEngine.getHabitsForDate(habits, selectedDateMillis)
        if (scheduled.isNotEmpty()) scheduled else habits
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
                    text = "কোনো রুটিন নেই",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(14.dp))
                FilledIconButton(
                    onClick = onAddFirstHabit,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "যোগ করুন")
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
        // 1. Period Filter Chips (দৈনিক | সাপ্তাহিক | মাসিক | বাৎসরিক)
        item(key = "dashboard_period_filter") {
            PeriodReportFilterRow(
                selectedPeriod = selectedPeriod,
                onSelectPeriod = onSelectPeriod
            )
        }

        // 2. Period Report Summary Card (Progress Ring + Stats + Bar Chart + Next Alarm Banner)
        item(key = "dashboard_summary_hero") {
            HabitPeriodReportSummaryCard(
                report = periodReport,
                habits = habits,
                pendingPostLogsByHabit = pendingPostLogsByHabit,
                isViewingToday = isViewingToday,
                nowMillis = nowMillis,
                showNextAlarmBanner = true
            )
        }

        // 3. Collapsible Routine Cards for Selected Date
        items(habitsForSelectedDate, key = { it.id }) { habit ->
            val isExpanded = habit.id in expandedHabitIds
            val isScheduledOnDay = HabitDateAndReportEngine.isHabitScheduledOnDate(habit, selectedDateMillis)
            val isCompleted = habit.id in completedHabitIdsForSelectedDate
            val isFutureTimeSlot = !isCompleted && HabitDateAndReportEngine.isHabitTimeSlotInFuture(
                dateMillis = selectedDateMillis,
                anchorTime = habit.anchorTime,
                nowMillis = nowMillis
            )
            CollapsibleHabitRoutineCard(
                habit = habit,
                configs = configsByHabit[habit.id].orEmpty(),
                pendingPostLog = pendingPostLogsByHabit[habit.id],
                isCompletedOnDate = isCompleted,
                isScheduledOnDate = isScheduledOnDay,
                isFutureTimeSlot = isFutureTimeSlot,
                isExpanded = isExpanded,
                nowMillis = nowMillis,
                onToggleExpand = {
                    expandedHabitIds = if (isExpanded) {
                        expandedHabitIds - habit.id
                    } else {
                        expandedHabitIds + habit.id
                    }
                },
                onMarkDoneClick = { onToggleHabitForDate(habit) },
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
 * Clean Collapsible Routine Card (Date-aware completion toggle, Future-Time Lock, Zero Truncation, Pure Icon Buttons).
 */
@Composable
private fun CollapsibleHabitRoutineCard(
    habit: HabitEntity,
    configs: List<ReminderConfigEntity>,
    pendingPostLog: HabitExecutionLogEntity?,
    isCompletedOnDate: Boolean,
    isScheduledOnDate: Boolean,
    isFutureTimeSlot: Boolean,
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
        isCompletedOnDate -> Color(0xFF2E7D32).copy(alpha = 0.55f)
        pendingPostLog != null -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        isFutureTimeSlot -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
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
        HabitScheduleCalculator.AUDIO_TTS_VOICE -> "🗣️ ভয়েস"
        HabitScheduleCalculator.AUDIO_PRESET_TONE -> "🔔 টিউন"
        else -> "📱 ডিফল্ট"
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
            // ── COLLAPSED COMPACT ROW ──
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
                        modifier = Modifier.size(42.dp)
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
                        Spacer(Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = anchorFormatted,
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
                                        text = "${HabitScheduleCalculator.toBengaliDigits(minsLeft.toString())}মি বাকি",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (isCompletedOnDate) {
                                Surface(
                                    color = Color(0xFF2E7D32).copy(alpha = 0.14f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "সম্পন্ন",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF2E7D32),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (isFutureTimeSlot) {
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(50)
                                ) {
                                    Text(
                                        text = "আসন্ন",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            } else if (!isScheduledOnDate) {
                                Text(
                                    text = "বিরতি",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else if (!isActive) {
                                Text(
                                    text = "বন্ধ",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.width(8.dp))

                // Date-Specific Toggle Button (Locked for future slots) + Expand Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Button(
                        onClick = onMarkDoneClick,
                        modifier = Modifier.testTag("mark_done_${habit.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = when {
                            isCompletedOnDate -> ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2E7D32),
                                contentColor = Color.White
                            )
                            isFutureTimeSlot -> ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            else -> ButtonDefaults.buttonColors()
                        },
                        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = when {
                                isCompletedOnDate -> "✓ সম্পন্ন"
                                isFutureTimeSlot -> "🔒 সময় হয়নি"
                                else -> "✓ টিক দিন"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "বিস্তারিত",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // ── EXPANDED DETAILS SECTION ──
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

                    // 3 Equal Timeline Columns
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PhaseTimelineBadge(
                            title = if (habit.hasPreReminder == 1) {
                                "${HabitScheduleCalculator.toBengaliDigits(habit.preOffsetMins.toString())}মি আগে"
                            } else {
                                "আগে"
                            },
                            subtitle = if (habit.hasPreReminder == 1) preFormatted else "বন্ধ",
                            enabled = habit.hasPreReminder == 1 && isActive,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onTestAlertClick(HabitScheduleCalculator.PHASE_PRE_EVENT) }
                        )

                        PhaseTimelineBadge(
                            title = "মূল সময়",
                            subtitle = anchorFormatted,
                            enabled = isActive,
                            isPrimary = true,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onTestAlertClick(HabitScheduleCalculator.PHASE_EXACT_EVENT) }
                        )

                        PhaseTimelineBadge(
                            title = if (habit.hasPostReminder == 1) {
                                "${HabitScheduleCalculator.toBengaliDigits(habit.postOffsetMins.toString())}মি পরে"
                            } else {
                                "পরে"
                            },
                            subtitle = if (habit.hasPostReminder == 1) "পানি/ওষুধ" else "বন্ধ",
                            enabled = habit.hasPostReminder == 1 && isActive,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onTestAlertClick(HabitScheduleCalculator.PHASE_POST_EVENT) }
                        )
                    }

                    // Weekdays + Audio BadgeRow
                    val activeDays = HabitScheduleCalculator.parseRepeatDays(habit.repeatDays)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                                    fontSize = 11.sp,
                                    fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                                    color = if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = audioBadge,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    // Bottom Action Bar: Pure Icon Buttons + Active Switch (No text next to common icons)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalIconButton(
                                onClick = { onTestAlertClick(HabitScheduleCalculator.PHASE_EXACT_EVENT) },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    Icons.Default.VolumeUp,
                                    contentDescription = "শুনুন",
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            FilledTonalIconButton(
                                onClick = onSnoozeClick,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    Icons.Default.Snooze,
                                    contentDescription = "স্নুজ",
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            FilledTonalIconButton(
                                onClick = onEditClick,
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "এডিট",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            FilledTonalIconButton(
                                onClick = onDeleteClick,
                                modifier = Modifier.size(38.dp),
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                )
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "মুছুন",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Switch(
                            checked = isActive,
                            onCheckedChange = { onToggleActive() }
                        )
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
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
private fun ExecutionLogsTabContent(
    habits: List<HabitEntity>,
    selectedPeriod: ReportPeriod,
    periodReport: HabitPeriodReport,
    pendingPostLogsByHabit: Map<String, HabitExecutionLogEntity>,
    isViewingToday: Boolean,
    nowMillis: Long,
    onSelectPeriod: (ReportPeriod) -> Unit,
    onClearLogs: () -> Unit
) {
    val habitsById = remember(habits) { habits.associateBy { it.id } }
    var statusFilter by remember { mutableStateOf("ALL") }
    val dateFormatter = remember {
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
    }

    val filteredLogs = remember(periodReport.filteredLogs, statusFilter) {
        val base = periodReport.filteredLogs
        when (statusFilter) {
            "COMPLETED" -> base.filter { it.status == HabitScheduleCalculator.STATUS_COMPLETED }
            "PENDING" -> base.filter { it.status == HabitScheduleCalculator.STATUS_PENDING }
            "OTHER" -> base.filter {
                it.status != HabitScheduleCalculator.STATUS_COMPLETED &&
                    it.status != HabitScheduleCalculator.STATUS_PENDING
            }
            else -> base
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Period Filter Chips (দৈনিক | সাপ্তাহিক | মাসিক | বাৎসরিক)
        item(key = "history_period_filter") {
            PeriodReportFilterRow(
                selectedPeriod = selectedPeriod,
                onSelectPeriod = onSelectPeriod
            )
        }

        // 2. Period Report Summary Card & Bar Chart
        item(key = "history_period_summary_card") {
            HabitPeriodReportSummaryCard(
                report = periodReport,
                habits = habits,
                pendingPostLogsByHabit = pendingPostLogsByHabit,
                isViewingToday = isViewingToday,
                nowMillis = nowMillis,
                showNextAlarmBanner = false
            )
        }

        // 3. Status Filter Chips + Clear Icon Button
        item(key = "logs_header_and_filters") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL" to "সব",
                        "COMPLETED" to "সম্পন্ন",
                        "PENDING" to "বাকি",
                        "OTHER" to "অন্যান্য"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = statusFilter == key,
                            onClick = { statusFilter = key },
                            label = { Text(label, fontSize = 12.sp) }
                        )
                    }
                }

                // Pure Icon Button for clearing history
                IconButton(onClick = onClearLogs) {
                    Icon(
                        Icons.Default.DeleteSweep,
                        contentDescription = "মুছুন",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        if (filteredLogs.isEmpty()) {
            item(key = "empty_logs_for_period") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📜", fontSize = 36.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "এই সময়ে কোনো লগ নেই",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredLogs, key = { it.id }) { log ->
                val habitTitle = habitsById[log.habitId]?.title ?: "রুটিন"
                val phaseLabel = when (log.phase) {
                    HabitScheduleCalculator.PHASE_PRE_EVENT -> "আগে"
                    HabitScheduleCalculator.PHASE_EXACT_EVENT -> "মূল সময়"
                    HabitScheduleCalculator.PHASE_POST_EVENT -> "পরে"
                    else -> ""
                }
                val (statusLabel, statusColor) = when (log.status) {
                    HabitScheduleCalculator.STATUS_COMPLETED -> "সম্পন্ন" to Color(0xFF2E7D32)
                    HabitScheduleCalculator.STATUS_PENDING -> "বাকি" to Color(0xFFE65100)
                    HabitScheduleCalculator.STATUS_SNOOZED -> "স্নুজ" to Color(0xFF1565C0)
                    HabitScheduleCalculator.STATUS_DISMISSED -> "বন্ধ" to Color(0xFF616161)
                    else -> "মিসড" to Color(0xFFC62828)
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
}
