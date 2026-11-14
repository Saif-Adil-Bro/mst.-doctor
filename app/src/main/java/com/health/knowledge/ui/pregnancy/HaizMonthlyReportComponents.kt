package com.health.knowledge.ui.pregnancy

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventAvailable
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
import com.health.knowledge.habit.HabitDateAndReportEngine
import com.health.knowledge.habit.HabitScheduleCalculator
import com.health.knowledge.pregnancy.HaizCalendarCell
import com.health.knowledge.pregnancy.HaizCalendarDayType
import com.health.knowledge.pregnancy.MonthlyHaizReport
import com.health.knowledge.pregnancy.PeriodCycleEntry
import com.health.knowledge.pregnancy.PregnancyPlannerEngine
import com.health.knowledge.pregnancy.YearlyHaizMonthBar
import com.health.knowledge.ui.theme.onePxBorder
import com.health.knowledge.ui.theme.solidOverSurface
import java.util.Calendar

private val HaizCrimson = Color(0xFFE11D48)
private val HaizAmber = Color(0xFFD97706)
private val FertileEmerald = Color(0xFF059669)
private val OvulationDeepGreen = Color(0xFF047857)

@Composable
fun HaizMonthlyReportDialog(
    selectedYear: Int,
    selectedMonthZeroBased: Int,
    averageCycleDays: Int,
    periodHistory: List<PeriodCycleEntry>,
    onSelectMonth: (year: Int, monthZeroBased: Int) -> Unit,
    onStepYear: (delta: Int) -> Unit,
    onSaveEntry: (startMillis: Long, endMillis: Long, flowLevel: String, note: String, entryId: String?) -> Unit,
    onDeleteEntry: (entryId: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val nowCal = remember { Calendar.getInstance() }
    val currentYear = nowCal.get(Calendar.YEAR)
    val currentMonth = nowCal.get(Calendar.MONTH)

    val monthlyReport = remember(selectedYear, selectedMonthZeroBased, periodHistory, averageCycleDays) {
        PregnancyPlannerEngine.buildMonthlyHaizReport(
            year = selectedYear,
            monthZeroBased = selectedMonthZeroBased,
            history = periodHistory,
            averageCycleDays = averageCycleDays
        )
    }

    val yearlyTrend = remember(selectedYear, periodHistory, averageCycleDays) {
        PregnancyPlannerEngine.buildYearlyHaizTrend(
            year = selectedYear,
            history = periodHistory,
            averageCycleDays = averageCycleDays
        )
    }

    var selectedDayCell by remember(selectedYear, selectedMonthZeroBased, periodHistory) {
        mutableStateOf<HaizCalendarCell?>(
            monthlyReport.calendarCells.firstOrNull {
                it.type == HaizCalendarDayType.PERIOD_ACTUAL || it.type == HaizCalendarDayType.OVULATION
            } ?: monthlyReport.calendarCells.firstOrNull { it.isToday }
        )
    }

    var editingEntryTarget by remember { mutableStateOf<PeriodCycleEntry?>(null) }
    var showEntryEditorDialog by remember { mutableStateOf(false) }

    val monthListState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedMonthZeroBased - 1).coerceAtLeast(0)
    )
    LaunchedEffect(selectedMonthZeroBased) {
        monthListState.animateScrollToItem((selectedMonthZeroBased - 1).coerceAtLeast(0))
    }

    if (showEntryEditorDialog) {
        HaizMonthEntryEditorDialog(
            targetYear = selectedYear,
            targetMonthZeroBased = selectedMonthZeroBased,
            existingEntry = editingEntryTarget,
            onDismiss = {
                showEntryEditorDialog = false
                editingEntryTarget = null
            },
            onSave = { startM, endM, flow, note ->
                onSaveEntry(startM, endM, flow, note, editingEntryTarget?.id)
                showEntryEditorDialog = false
                editingEntryTarget = null
                Toast.makeText(context, "✅ হায়েজের তথ্য সংরক্ষিত হয়েছে", Toast.LENGTH_SHORT).show()
            },
            onDelete = editingEntryTarget?.let { target ->
                {
                    onDeleteEntry(target.id)
                    showEntryEditorDialog = false
                    editingEntryTarget = null
                    Toast.makeText(context, "🗑️ মাসিকের এন্ট্রি মুছে ফেলা হয়েছে", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .padding(horizontal = 10.dp, vertical = 12.dp)
                .testTag("haiz_monthly_report_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🩸", fontSize = 20.sp)
                            Column {
                                Text(
                                    text = "মেয়েদের হায়েজ ও মাসিক রিপোর্ট",
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "মাস ও বছর অনুযায়ী বিস্তারিত হিসাব",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    editingEntryTarget = monthlyReport.activeEntry
                                    showEntryEditorDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = Color.White.copy(alpha = 0.22f),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = if (monthlyReport.activeEntry != null) Icons.Default.Edit else Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (monthlyReport.activeEntry != null) "এডিট" else "তথ্য লগ",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("haiz_report_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "বন্ধ করুন",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Year Switcher + 12-Month Selector Strip
                    item(key = "haiz_year_month_selector") {
                        HaizYearAndMonthSelectorCard(
                            selectedYear = selectedYear,
                            selectedMonthZeroBased = selectedMonthZeroBased,
                            currentYear = currentYear,
                            currentMonthZeroBased = currentMonth,
                            yearlyTrend = yearlyTrend,
                            monthListState = monthListState,
                            onStepYear = onStepYear,
                            onSelectMonth = onSelectMonth
                        )
                    }

                    // 2. Selected Month Summary KPI Card
                    item(key = "haiz_month_summary") {
                        HaizMonthSummaryCard(
                            report = monthlyReport,
                            onEditOrLogClick = {
                                editingEntryTarget = monthlyReport.activeEntry
                                showEntryEditorDialog = true
                            }
                        )
                    }

                    // 3. Visual Calendar Grid + Selected Day Inspector
                    item(key = "haiz_month_calendar") {
                        HaizMonthCalendarCard(
                            report = monthlyReport,
                            selectedDayCell = selectedDayCell,
                            onSelectDay = { selectedDayCell = it }
                        )
                    }

                    // 4. Multi-Month Yearly Overview Bar Chart
                    item(key = "haiz_yearly_trend") {
                        HaizYearlyComparisonCard(
                            selectedYear = selectedYear,
                            selectedMonthZeroBased = selectedMonthZeroBased,
                            bars = yearlyTrend,
                            onSelectMonth = { m -> onSelectMonth(selectedYear, m) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HaizYearAndMonthSelectorCard(
    selectedYear: Int,
    selectedMonthZeroBased: Int,
    currentYear: Int,
    currentMonthZeroBased: Int,
    yearlyTrend: List<YearlyHaizMonthBar>,
    monthListState: androidx.compose.foundation.lazy.LazyListState,
    onStepYear: (Int) -> Unit,
    onSelectMonth: (Int, Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f).solidOverSurface()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = { onStepYear(-1) },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("haiz_prev_year_btn")
                        ) {
                            Icon(
                                Icons.Default.ChevronLeft,
                                contentDescription = "আগের বছর",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = HabitScheduleCalculator.toBengaliDigits(selectedYear.toString()),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        IconButton(
                            onClick = { onStepYear(1) },
                            modifier = Modifier
                                .size(32.dp)
                                .testTag("haiz_next_year_btn")
                        ) {
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = "পরের বছর",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                if (selectedYear != currentYear || selectedMonthZeroBased != currentMonthZeroBased) {
                    OutlinedButton(
                        onClick = { onSelectMonth(currentYear, currentMonthZeroBased) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 5.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("চলতি মাস", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "চলতি মাস নির্বাচিত",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            LazyRow(
                state = monthListState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(yearlyTrend) { index, bar ->
                    val isSelected = index == selectedMonthZeroBased
                    val dotColor = when {
                        bar.hasActualLog -> HaizCrimson
                        bar.isProjected -> HaizAmber
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                    Surface(
                        onClick = { onSelectMonth(selectedYear, index) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                        border = onePxBorder(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("haiz_month_chip_$index")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) Color.White else dotColor)
                            )
                            Text(
                                text = bar.shortLabelBn,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HaizMonthSummaryCard(
    report: MonthlyHaizReport,
    onEditOrLogClick: () -> Unit
) {
    val (statusLabel, statusColor) = when {
        report.hasActualLog -> "✅ রেকর্ডকৃত রিপোর্ট" to HaizCrimson
        report.isProjected -> "🔮 সম্ভাব্য পূর্বাভাস" to HaizAmber
        else -> "📅 তথ্য সংরক্ষিত নেই" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.monthYearTitleBn,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (report.hasActualLog) {
                        Text(
                            text = "মাত্রা: ${report.flowLevelBn}" +
                                if (report.noteBn.isNotBlank()) " • ${report.noteBn}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    color = statusColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50),
                    border = onePxBorder(statusColor.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // 2x2 KPI Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HaizKpiTile(
                    label = "শুরু ও শেষ তারিখ",
                    value = report.periodDateRangeBn,
                    valueColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                HaizKpiTile(
                    label = "হায়েজ স্থায়িত্বকাল",
                    value = if (report.durationDays > 0) {
                        val prefix = if (report.isProjected) "~ " else ""
                        "$prefix${HabitScheduleCalculator.toBengaliDigits(report.durationDays.toString())} দিন"
                    } else "—",
                    valueColor = if (report.hasActualLog) HaizCrimson else HaizAmber,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HaizKpiTile(
                    label = "মাসিক চক্র (ব্যবধান)",
                    value = if (report.cycleGapDays > 0) {
                        "${HabitScheduleCalculator.toBengaliDigits(report.cycleGapDays.toString())} দিন"
                    } else "—",
                    valueColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
                HaizKpiTile(
                    label = "সম্ভাব্য ওভুলেশন",
                    value = report.ovulationDateBn,
                    subtitle = if (report.fertileRangeBn != "—") "উর্বর: ${report.fertileRangeBn}" else null,
                    valueColor = FertileEmerald,
                    modifier = Modifier.weight(1f)
                )
            }

            // Health & Fiqh Notice Box
            val noticeBg = when {
                report.isWarningNotice -> HaizAmber.copy(alpha = 0.10f)
                report.hasActualLog -> FertileEmerald.copy(alpha = 0.10f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
            val noticeBorder = when {
                report.isWarningNotice -> HaizAmber.copy(alpha = 0.45f)
                report.hasActualLog -> FertileEmerald.copy(alpha = 0.45f)
                else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            }

            Surface(
                color = noticeBg,
                shape = RoundedCornerShape(12.dp),
                border = onePxBorder(noticeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(11.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = report.healthNoticeTitleBn,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = report.healthNoticeDetailBn,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }

            if (!report.hasActualLog) {
                Button(
                    onClick = onEditOrLogClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("এই মাসের মাসিকের তথ্য লগ করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HaizKpiTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
    subtitle: String? = null
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f).solidOverSurface(),
        border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HaizMonthCalendarCard(
    report: MonthlyHaizReport,
    selectedDayCell: HaizCalendarCell?,
    onSelectDay: (HaizCalendarCell) -> Unit
) {
    val weekdays = listOf("শনি", "রবি", "সোম", "মঙ্গল", "বুধ", "বৃহঃ", "শুক্র")
    val totalSlots = buildList<HaizCalendarCell?> {
        repeat(report.calendarLeadingEmptyCount) { add(null) }
        addAll(report.calendarCells)
        val remainder = size % 7
        if (remainder != 0) {
            repeat(7 - remainder) { add(null) }
        }
    }
    val weeks = totalSlots.chunked(7)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📅 মাসিক হায়েজ ও উর্বরতা ক্যালেন্ডার",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Compact Color Legend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendChip(color = HaizCrimson, label = "রেকর্ডকৃত")
                LegendChip(color = HaizAmber, label = "সম্ভাব্য")
                LegendChip(color = FertileEmerald, label = "উর্বর সময়")
                LegendChip(color = OvulationDeepGreen, label = "ওভুলেশন")
            }

            // Weekday Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                weekdays.forEach { dayName ->
                    Text(
                        text = dayName,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Calendar Weeks
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                weeks.forEach { weekRow ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        weekRow.forEach { cell ->
                            if (cell == null) {
                                Box(modifier = Modifier.weight(1f).height(42.dp))
                            } else {
                                val isSel = selectedDayCell?.dayOfMonth == cell.dayOfMonth
                                val bgColor = when (cell.type) {
                                    HaizCalendarDayType.PERIOD_ACTUAL -> HaizCrimson
                                    HaizCalendarDayType.PERIOD_PROJECTED -> HaizAmber.copy(alpha = 0.22f)
                                    HaizCalendarDayType.OVULATION -> OvulationDeepGreen
                                    HaizCalendarDayType.FERTILE -> FertileEmerald.copy(alpha = 0.16f)
                                    HaizCalendarDayType.NORMAL -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f)
                                }
                                val textColor = when (cell.type) {
                                    HaizCalendarDayType.PERIOD_ACTUAL,
                                    HaizCalendarDayType.OVULATION -> Color.White
                                    HaizCalendarDayType.PERIOD_PROJECTED -> HaizAmber
                                    HaizCalendarDayType.FERTILE -> FertileEmerald
                                    HaizCalendarDayType.NORMAL -> MaterialTheme.colorScheme.onSurface
                                }
                                val borderColor = when {
                                    isSel -> MaterialTheme.colorScheme.primary
                                    cell.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    cell.type == HaizCalendarDayType.PERIOD_PROJECTED -> HaizAmber
                                    cell.type == HaizCalendarDayType.FERTILE -> FertileEmerald.copy(alpha = 0.45f)
                                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(bgColor)
                                        .border(
                                            width = if (isSel) 2.dp else 1.dp,
                                            color = borderColor,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { onSelectDay(cell) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = HabitScheduleCalculator.toBengaliDigits(cell.dayOfMonth.toString()),
                                            fontSize = 12.sp,
                                            fontWeight = if (cell.type != HaizCalendarDayType.NORMAL || isSel) FontWeight.Bold else FontWeight.Medium,
                                            color = textColor,
                                            lineHeight = 14.sp
                                        )
                                        val emoji = when (cell.type) {
                                            HaizCalendarDayType.PERIOD_ACTUAL,
                                            HaizCalendarDayType.PERIOD_PROJECTED -> "🩸"
                                            HaizCalendarDayType.OVULATION -> "🎯"
                                            HaizCalendarDayType.FERTILE -> "🌟"
                                            HaizCalendarDayType.NORMAL -> ""
                                        }
                                        if (emoji.isNotEmpty()) {
                                            Text(text = emoji, fontSize = 9.sp, lineHeight = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Day Inspector Banner
            val inspectorText = selectedDayCell?.let { cell ->
                val dateLabel = HabitDateAndReportEngine.formatFullDateBn(cell.dateMillis)
                val statusDesc = when (cell.type) {
                    HaizCalendarDayType.PERIOD_ACTUAL ->
                        "🩸 হায়েজের ${HabitScheduleCalculator.toBengaliDigits(cell.haizDayNumber.toString())}তম দিন (রেকর্ডকৃত)"
                    HaizCalendarDayType.PERIOD_PROJECTED ->
                        "🔮 সম্ভাব্য হায়েজের ${HabitScheduleCalculator.toBengaliDigits(cell.haizDayNumber.toString())}তম দিন (পূর্বাভাস)"
                    HaizCalendarDayType.OVULATION ->
                        "🎯 সম্ভাব্য ওভুলেশন দিন (গর্ভধারণের সম্ভাবনা সর্বাধিক)"
                    HaizCalendarDayType.FERTILE ->
                        "🌟 উর্বর সময় (Fertile Window)"
                    HaizCalendarDayType.NORMAL ->
                        "🌿 স্বাভাবিক / পবিত্রতার দিন (তূহুর)"
                }
                "$dateLabel\n$statusDesc"
            } ?: "তারিখের বিস্তারিত অবস্থা দেখতে উপরের যেকোনো দিনে ট্যাপ করুন।"

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f).solidOverSurface(),
                shape = RoundedCornerShape(10.dp),
                border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = inspectorText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

@Composable
private fun LegendChip(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun HaizYearlyComparisonCard(
    selectedYear: Int,
    selectedMonthZeroBased: Int,
    bars: List<YearlyHaizMonthBar>,
    onSelectMonth: (Int) -> Unit
) {
    val loggedBars = bars.filter { it.hasActualLog }
    val avgDuration = if (loggedBars.isNotEmpty()) {
        val avg = loggedBars.map { it.durationDays }.average()
        String.format(java.util.Locale.US, "%.1f", avg)
    } else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "📊 ${HabitScheduleCalculator.toBengaliDigits(selectedYear.toString())} সালের ১২ মাসের তুলনা",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "যেকোনো মাসের বারে ট্যাপ করে সেই মাসের রিপোর্ট দেখুন",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (avgDuration != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "গড়: ${HabitScheduleCalculator.toBengaliDigits(avgDuration)} দিন",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(126.dp)
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                bars.forEach { bar ->
                    val isSelected = bar.monthZeroBased == selectedMonthZeroBased
                    val barColor = when {
                        bar.hasActualLog && bar.isWarningDuration -> HaizAmber
                        bar.hasActualLog -> HaizCrimson
                        bar.isProjected -> HaizAmber.copy(alpha = 0.45f)
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                    }
                    val fraction = if (bar.durationDays > 0) {
                        (bar.durationDays.toFloat() / 10f).coerceIn(0.18f, 1f)
                    } else {
                        0.08f
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelectMonth(bar.monthZeroBased) },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (bar.durationDays > 0) {
                            Text(
                                text = HabitScheduleCalculator.toBengaliDigits(bar.durationDays.toString()),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 11.sp
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.72f)
                                .weight(1f, fill = true),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(fraction)
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(barColor)
                                    .then(
                                        if (isSelected) Modifier.border(
                                            1.5.dp,
                                            MaterialTheme.colorScheme.primary,
                                            RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp)
                                        ) else Modifier
                                    )
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = bar.shortLabelBn.take(3),
                            fontSize = 9.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HaizMonthEntryEditorDialog(
    targetYear: Int,
    targetMonthZeroBased: Int,
    existingEntry: PeriodCycleEntry?,
    onDismiss: () -> Unit,
    onSave: (startMillis: Long, endMillis: Long, flowLevel: String, note: String) -> Unit,
    onDelete: (() -> Unit)?
) {
    val context = LocalContext.current
    val defaultMonthStartMillis = remember(targetYear, targetMonthZeroBased) {
        Calendar.getInstance().apply {
            set(Calendar.YEAR, targetYear)
            set(Calendar.MONTH, targetMonthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    var startMillis by remember(existingEntry, defaultMonthStartMillis) {
        mutableLongStateOf(
            if (existingEntry != null && existingEntry.startMillis > 0L) existingEntry.startMillis
            else defaultMonthStartMillis
        )
    }
    var endMillis by remember(existingEntry, defaultMonthStartMillis) {
        mutableLongStateOf(
            when {
                existingEntry != null && existingEntry.endMillis >= existingEntry.startMillis -> existingEntry.endMillis
                existingEntry != null -> HabitDateAndReportEngine.addDays(existingEntry.startMillis, 4)
                else -> HabitDateAndReportEngine.addDays(defaultMonthStartMillis, 4)
            }
        )
    }
    var flowLevel by remember(existingEntry) {
        mutableStateOf(existingEntry?.flowLevel?.ifBlank { "মাঝারি (স্বাভাবিক)" } ?: "মাঝারি (স্বাভাবিক)")
    }
    var note by remember(existingEntry) {
        mutableStateOf(existingEntry?.note ?: "")
    }
    var confirmDelete by remember { mutableStateOf(false) }

    val durationDays = remember(startMillis, endMillis) {
        if (startMillis > 0L && endMillis >= startMillis) {
            (((endMillis - startMillis + 43_200_000L) / 86_400_000L).toInt() + 1).coerceAtLeast(1)
        } else 1
    }

    val openStartPicker = {
        val cal = Calendar.getInstance().apply { timeInMillis = startMillis }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val picked = Calendar.getInstance().apply {
                    set(y, m, d, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val prevDur = durationDays
                startMillis = picked
                endMillis = HabitDateAndReportEngine.addDays(picked, (prevDur - 1).coerceAtLeast(0))
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    val openEndPicker = {
        val cal = Calendar.getInstance().apply { timeInMillis = endMillis }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val picked = Calendar.getInstance().apply {
                    set(y, m, d, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                if (picked >= startMillis) {
                    endMillis = picked
                } else {
                    Toast.makeText(context, "শেষ তারিখ শুরুর তারিখের পরে হতে হবে", Toast.LENGTH_SHORT).show()
                }
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp),
        title = {
            Text(
                text = "🩸 ${PregnancyPlannerEngine.fullMonthNamesBn[targetMonthZeroBased.coerceIn(0, 11)]} ${HabitScheduleCalculator.toBengaliDigits(targetYear.toString())} — হায়েজ লগ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedCard(
                    onClick = openStartPicker,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("শুরুর তারিখ", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Text(HabitDateAndReportEngine.formatFullDateBn(startMillis), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                OutlinedCard(
                    onClick = openEndPicker,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("শেষ হওয়ার তারিখ", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Text(HabitDateAndReportEngine.formatFullDateBn(endMillis), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Icon(Icons.Default.EventAvailable, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "⏱️ মোট স্থায়িত্বকাল: ${HabitScheduleCalculator.toBengaliDigits(durationDays.toString())} দিন",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                    )
                }

                Text("রক্তস্রাবের মাত্রা:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("হালকা", "মাঝারি (স্বাভাবিক)", "বেশি / ভারী").forEach { option ->
                        FilterChip(
                            selected = flowLevel == option,
                            onClick = { flowLevel = option },
                            label = { Text(option, fontSize = 11.sp, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }

                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("ব্যক্তিগত নোট (ঐচ্ছিক)", fontSize = 12.sp) },
                    placeholder = { Text("যেমন: হালকা দুর্বলতা বা কোমর ব্যথা", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                )

                if (onDelete != null) {
                    if (!confirmDelete) {
                        OutlinedButton(
                            onClick = { confirmDelete = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("এই মাসের এন্ট্রি মুছুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onDelete,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("নিশ্চিত ডিলিট করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(startMillis, endMillis, flowLevel, note) },
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("সংরক্ষণ করুন", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("বাতিল")
            }
        }
    )
}
