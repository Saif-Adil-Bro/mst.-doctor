package com.health.knowledge.ui.habit

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.health.knowledge.data.local.entity.HabitEntity
import com.health.knowledge.data.local.entity.HabitExecutionLogEntity
import com.health.knowledge.habit.DateStripItem
import com.health.knowledge.habit.HabitConsistencyStat
import com.health.knowledge.habit.HabitPeriodReport
import com.health.knowledge.habit.HabitScheduleCalculator
import com.health.knowledge.habit.ReportBarBucket
import com.health.knowledge.habit.ReportBarStatus
import com.health.knowledge.habit.ReportPeriod
import com.health.knowledge.ui.theme.SolaimanLipi
import com.health.knowledge.ui.theme.onePxBorder
import com.health.knowledge.ui.theme.solidOverSurface
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Horizontal Scrollable Date Strip (-30 days to +30 days around today) with quick "আজ" jump button
 * and pure icon previous/next day buttons.
 */
@Composable
fun HorizontalDateStripBar(
    dateItems: List<DateStripItem>,
    selectedDateMillis: Long,
    onSelectDate: (Long) -> Unit,
    onJumpToToday: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = remember(dateItems, selectedDateMillis) {
        dateItems.indexOfFirst { it.isSelected }.takeIf { it >= 0 }
            ?: dateItems.indexOfFirst { it.isToday }.coerceAtLeast(0)
    }
    val selectedItem = dateItems.getOrNull(selectedIndex)
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedIndex - 2).coerceAtLeast(0)
    )

    LaunchedEffect(selectedIndex) {
        val targetScroll = (selectedIndex - 2).coerceAtLeast(0)
        listState.animateScrollToItem(targetScroll)
    }

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f).solidOverSurface(),
        border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("horizontal_date_strip")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 10.dp)
        ) {
            // Top Header: Selected Full Date + Pure Icon Prev/Next + "আজ" Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(
                        onClick = {
                            if (selectedIndex > 0) {
                                onSelectDate(dateItems[selectedIndex - 1].dayStartMillis)
                            }
                        },
                        enabled = selectedIndex > 0,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("prev_date_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "আগের দিন",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = selectedItem?.fullDateTitleBn ?: "",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    IconButton(
                        onClick = {
                            if (selectedIndex < dateItems.lastIndex) {
                                onSelectDate(dateItems[selectedIndex + 1].dayStartMillis)
                            }
                        },
                        enabled = selectedIndex < dateItems.lastIndex,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("next_date_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "পরের দিন",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (selectedItem != null && !selectedItem.isToday) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .testTag("jump_today_button")
                            .clickable { onJumpToToday() }
                    ) {
                        Text(
                            text = "আজ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "আজ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Horizontal Scrollable Date Pills
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("date_strip_lazy_row"),
                contentPadding = PaddingValues(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(
                    items = dateItems,
                    key = { _, item -> item.dayStartMillis }
                ) { _, item ->
                    DateStripPillItem(
                        item = item,
                        onClick = { onSelectDate(item.dayStartMillis) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DateStripPillItem(
    item: DateStripItem,
    onClick: () -> Unit
) {
    val containerColor = when {
        item.isSelected -> MaterialTheme.colorScheme.primary
        item.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        item.isSelected -> MaterialTheme.colorScheme.primary
        item.isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }
    val dayNameColor = when {
        item.isSelected -> MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
        item.isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val dateNumColor = when {
        item.isSelected -> MaterialTheme.colorScheme.onPrimary
        item.isToday -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val dotColor = when {
        item.scheduledCount > 0 && item.completedCount >= item.scheduledCount -> {
            if (item.isSelected) Color.White else Color(0xFF2E7D32)
        }
        item.completedCount > 0 -> {
            if (item.isSelected) Color.White.copy(alpha = 0.85f) else Color(0xFFEF6C00)
        }
        else -> {
            if (item.isSelected) Color.White.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
        }
    }

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(14.dp),
        border = onePxBorder(borderColor),
        modifier = Modifier
            .width(52.dp)
            .testTag("date_pill_${item.dayStartMillis}")
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = item.shortDayNameBn,
                fontSize = 10.sp,
                fontWeight = if (item.isSelected || item.isToday) FontWeight.Bold else FontWeight.Medium,
                color = dayNameColor,
                maxLines = 1
            )
            Text(
                text = item.dayOfMonthBn,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = dateNumColor,
                maxLines = 1
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

/**
 * 4-Mode Period Filter Chips Row: দৈনিক | সাপ্তাহিক | মাসিক | বাৎসরিক
 */
@Composable
fun PeriodReportFilterRow(
    selectedPeriod: ReportPeriod,
    onSelectPeriod: (ReportPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ReportPeriod.entries.forEach { period ->
            val selected = period == selectedPeriod
            FilterChip(
                selected = selected,
                onClick = { onSelectPeriod(period) },
                label = {
                    Text(
                        text = period.labelBn,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                modifier = Modifier.testTag("period_filter_${period.name}")
            )
        }
    }
}

/**
 * Unified Period Report Card with Progress Ring, Completed/Pending/Missed Summary,
 * Weekly/Monthly/Yearly Insights, Color-Coded Clickable Bar Chart, Breakdown Panel,
 * Yearly 12-Month Grid, and Habit Consistency Section.
 */
@Composable
fun HabitPeriodReportSummaryCard(
    report: HabitPeriodReport,
    habits: List<HabitEntity>,
    pendingPostLogsByHabit: Map<String, HabitExecutionLogEntity>,
    isViewingToday: Boolean,
    nowMillis: Long,
    showNextAlarmBanner: Boolean,
    modifier: Modifier = Modifier
) {
    val rawProgress = (report.completionPercent / 100f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = tween(durationMillis = 450),
        label = "period_progress_ring"
    )

    val activeHabits = remember(habits) { habits.filter { it.isActive == 1 } }
    val earliestPendingPost = pendingPostLogsByHabit.values.minByOrNull { it.scheduledTime }

    val nextUpcomingTriggerPair = remember(activeHabits, nowMillis, isViewingToday) {
        if (!isViewingToday) null
        else {
            activeHabits.flatMap { habit ->
                val list = mutableListOf<Triple<HabitEntity, String, Long>>()
                if (habit.hasPreReminder == 1) {
                    val preMillis = HabitScheduleCalculator.calculateNextOccurrenceMillis(
                        anchorTime = habit.anchorTime,
                        signedOffsetMins = -habit.preOffsetMins,
                        repeatDaysJson = habit.repeatDays,
                        nowMillis = nowMillis
                    )
                    list.add(Triple(habit, "আগে", preMillis))
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
    }

    val defaultSelectedIdx = remember(report.period, report.periodTitleBn, report.bars.size) {
        report.bars.indexOfFirst { it.isHighlighted }.takeIf { it >= 0 } ?: 0
    }
    var selectedBarIdx by remember(report.period, report.periodTitleBn, report.bars.size) {
        mutableIntStateOf(defaultSelectedIdx)
    }
    val safeSelectedIdx = selectedBarIdx.coerceIn(0, max(0, report.bars.lastIndex))
    val selectedBucket = report.bars.getOrNull(safeSelectedIdx)

    val cardBg = when (report.period) {
        ReportPeriod.WEEKLY -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f).solidOverSurface()
        ReportPeriod.MONTHLY -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f).solidOverSurface()
        ReportPeriod.YEARLY -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.26f).solidOverSurface()
        ReportPeriod.DAILY -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.32f).solidOverSurface()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("habit_period_report_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.38f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Progress Ring + Period Summary Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier.size(72.dp),
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
                        color = when {
                            report.completionPercent >= 80 -> Color(0xFF2E7D32)
                            report.completionPercent >= 40 -> MaterialTheme.colorScheme.primary
                            else -> Color(0xFFED8A0A)
                        },
                        strokeWidth = 7.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${HabitScheduleCalculator.toBengaliDigits(report.completionPercent.toString())}%",
                            fontFamily = SolaimanLipi,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${HabitScheduleCalculator.toBengaliDigits(report.completedCount.toString())}/${HabitScheduleCalculator.toBengaliDigits(report.totalScheduled.toString())}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${report.period.labelBn} রিপোর্ট",
                            fontFamily = SolaimanLipi,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = report.periodTitleBn,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    // 3 Stat Pills: সম্পন্ন | বাকি | মিসড
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReportStatBadge(
                            label = "সম্পন্ন",
                            count = report.completedCount,
                            accentColor = Color(0xFF2E7D32),
                            modifier = Modifier.weight(1f)
                        )
                        ReportStatBadge(
                            label = "বাকি",
                            count = report.pendingCount,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        ReportStatBadge(
                            label = "মিসড",
                            count = report.missedCount,
                            accentColor = Color(0xFFD32F2F),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Period Insight Pills for Weekly, Monthly & Yearly
            if (report.period != ReportPeriod.DAILY) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                        border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "📅 সক্রিয় দিন",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "${HabitScheduleCalculator.toBengaliDigits(report.activeDaysCount.toString())} দিন",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (report.bestBucketLabel.isNotBlank()) {
                        val bestPrefix = if (report.period == ReportPeriod.YEARLY) "🏆 সেরা মাস" else "🏆 সেরা দিন"
                        Surface(
                            color = Color(0xFF2E7D32).copy(alpha = 0.09f),
                            shape = RoundedCornerShape(10.dp),
                            border = onePxBorder(Color(0xFF2E7D32).copy(alpha = 0.35f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = bestPrefix,
                                    fontSize = 11.sp,
                                    color = Color(0xFF2E7D32),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = report.bestBucketLabel,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Next Alarm Banner when viewing Today on Dashboard
            if (showNextAlarmBanner && isViewingToday) {
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "⏱️ $habitTitle ($timeFormatted)",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "${HabitScheduleCalculator.toBengaliDigits(minsLeft.toString())}মি বাকি",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else if (nextUpcomingTriggerPair != null) {
                    val (nextHabit, _, triggerTime) = nextUpcomingTriggerPair
                    val timeFormatted = HabitScheduleCalculator.formatMillisToBengaliTime(triggerTime)

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🔔 ${nextHabit.title}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = timeFormatted,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Color Legend + Visual Clickable Bar Chart
            if (report.bars.isNotEmpty()) {
                ReportColorLegendRow()

                PeriodBarChartSection(
                    period = report.period,
                    bars = report.bars,
                    selectedIndex = safeSelectedIdx,
                    onSelectBar = { idx -> selectedBarIdx = idx }
                )

                // Clicked Bar Details: Completed vs Incomplete items
                if (selectedBucket != null) {
                    SelectedBarBreakdownPanel(
                        bucket = selectedBucket,
                        period = report.period
                    )
                }
            }

            // Yearly 12-Month Performance Grid (clickable to jump to any month bar)
            if (report.period == ReportPeriod.YEARLY && report.bars.size == 12) {
                YearlyMonthOverviewGrid(
                    bars = report.bars,
                    selectedIndex = safeSelectedIdx,
                    onSelectMonth = { idx -> selectedBarIdx = idx }
                )
            }

            // Routine-wise Consistency Progress Bars (Weekly / Monthly / Yearly)
            if (report.period != ReportPeriod.DAILY && report.habitConsistency.isNotEmpty()) {
                HabitConsistencyBreakdownSection(
                    period = report.period,
                    stats = report.habitConsistency
                )
            }
        }
    }
}

@Composable
private fun ReportColorLegendRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegendDotItem(color = Color(0xFF2E7D32), label = "সম্পন্ন")
        LegendDotItem(color = Color(0xFFED8A0A), label = "আংশিক")
        LegendDotItem(color = Color(0xFFD32F2F), label = "অসম্পন্ন")
        LegendDotItem(color = MaterialTheme.colorScheme.outlineVariant, label = "আসন্ন")
        Spacer(Modifier.weight(1f, fill = false))
        Text(
            text = "বারে ট্যাপ করে বিস্তারিত দেখুন",
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LegendDotItem(color: Color, label: String) {
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
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ReportStatBadge(
    label: String,
    count: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = accentColor.copy(alpha = 0.11f),
        shape = RoundedCornerShape(10.dp),
        border = onePxBorder(accentColor.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = HabitScheduleCalculator.toBengaliDigits(count.toString()),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun barColorForStatus(status: ReportBarStatus): Color {
    return when (status) {
        ReportBarStatus.COMPLETED -> Color(0xFF2E7D32)
        ReportBarStatus.PARTIAL -> Color(0xFFED8A0A)
        ReportBarStatus.MISSED -> Color(0xFFD32F2F)
        ReportBarStatus.PENDING -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
        ReportBarStatus.NO_SCHEDULE -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
    }
}

@Composable
private fun PeriodBarChartSection(
    period: ReportPeriod,
    bars: List<ReportBarBucket>,
    selectedIndex: Int,
    onSelectBar: (Int) -> Unit
) {
    val isScrollableChart = period == ReportPeriod.MONTHLY || period == ReportPeriod.YEARLY
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedIndex - 3).coerceAtLeast(0)
    )

    LaunchedEffect(period, selectedIndex) {
        if (isScrollableChart && bars.isNotEmpty()) {
            val target = (selectedIndex - 3).coerceIn(0, bars.lastIndex)
            listState.animateScrollToItem(target)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("period_bar_chart")
    ) {
        if (isScrollableChart) {
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentPadding = PaddingValues(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                itemsIndexed(bars) { idx, bucket ->
                    BarBucketColumn(
                        bucket = bucket,
                        isSelected = idx == selectedIndex,
                        onClick = { onSelectBar(idx) },
                        modifier = Modifier.width(if (period == ReportPeriod.MONTHLY) 32.dp else 38.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                bars.forEachIndexed { idx, bucket ->
                    BarBucketColumn(
                        bucket = bucket,
                        isSelected = idx == selectedIndex,
                        onClick = { onSelectBar(idx) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BarBucketColumn(
    bucket: ReportBarBucket,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val barColor = barColorForStatus(bucket.status)
    val fillTarget = when {
        bucket.total <= 0 -> 0.10f
        bucket.status == ReportBarStatus.MISSED -> 0.30f // Clearly visible red bar on missed days
        else -> bucket.fraction.coerceIn(0.14f, 1f)
    }
    val animatedFraction by animateFloatAsState(
        targetValue = fillTarget,
        animationSpec = tween(durationMillis = 380),
        label = "bar_${bucket.label}"
    )

    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.11f) else Color.Transparent,
        shape = RoundedCornerShape(10.dp),
        border = if (isSelected) onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)) else null,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 3.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = HabitScheduleCalculator.toBengaliDigits(bucket.completed.toString()),
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )

            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(62.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
                contentAlignment = Alignment.BottomCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(animatedFraction)
                        .clip(RoundedCornerShape(50))
                        .background(barColor)
                )
            }

            Text(
                text = bucket.label,
                fontSize = 10.sp,
                fontWeight = if (isSelected || bucket.isHighlighted) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                textAlign = TextAlign.Center,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun SelectedBarBreakdownPanel(
    bucket: ReportBarBucket,
    period: ReportPeriod
) {
    val ratioText = "${HabitScheduleCalculator.toBengaliDigits(bucket.completed.toString())}/${HabitScheduleCalculator.toBengaliDigits(bucket.total.toString())} সম্পন্ন"
    val badgeColor = barColorForStatus(bucket.status)

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("selected_bar_breakdown")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📌 ${bucket.fullTitleBn}",
                    fontFamily = SolaimanLipi,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Surface(
                    color = badgeColor.copy(alpha = 0.13f),
                    shape = RoundedCornerShape(50),
                    border = onePxBorder(badgeColor.copy(alpha = 0.35f))
                ) {
                    Text(
                        text = ratioText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }

            if (bucket.total == 0) {
                Text(
                    text = "এই সময়ে কোনো রুটিন নির্ধারিত ছিল না।",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Completed Items Section
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (period == ReportPeriod.YEARLY) "সম্পন্ন হওয়া রুটিনসমূহ:" else "যেগুলো সম্পন্ন হয়েছে:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    if (bucket.completedItems.isEmpty()) {
                        Text(
                            text = "• কোনোটিই সম্পন্ন হয়নি",
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    } else {
                        bucket.completedItems.forEach { itemText ->
                            Surface(
                                color = Color(0xFF2E7D32).copy(alpha = 0.09f),
                                shape = RoundedCornerShape(8.dp),
                                border = onePxBorder(Color(0xFF2E7D32).copy(alpha = 0.28f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "✓ $itemText",
                                    fontFamily = SolaimanLipi,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                // Incomplete Items Section
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    val isPendingOnly = bucket.status == ReportBarStatus.PENDING
                    val sectionAccent = if (isPendingOnly) MaterialTheme.colorScheme.primary else Color(0xFFD32F2F)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = if (isPendingOnly) Icons.Default.Schedule else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = sectionAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = if (isPendingOnly) "আসন্ন / বাকি রুটিনসমূহ:" else "যেগুলো সম্পন্ন হয়নি:",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = sectionAccent
                        )
                    }

                    if (bucket.incompleteItems.isEmpty()) {
                        Text(
                            text = "🎉 সব রুটিন শতভাগ সম্পন্ন হয়েছে!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    } else {
                        bucket.incompleteItems.forEach { itemText ->
                            val isUpcomingItem = itemText.contains("আসন্ন") || itemText.contains("বাকি")
                            val chipAccent = if (isUpcomingItem) Color(0xFFED8A0A) else Color(0xFFD32F2F)
                            val prefixIcon = if (isUpcomingItem) "⏳" else "✕"
                            Surface(
                                color = chipAccent.copy(alpha = 0.09f),
                                shape = RoundedCornerShape(8.dp),
                                border = onePxBorder(chipAccent.copy(alpha = 0.28f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "$prefixIcon $itemText",
                                    fontFamily = SolaimanLipi,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun YearlyMonthOverviewGrid(
    bars: List<ReportBarBucket>,
    selectedIndex: Int,
    onSelectMonth: (Int) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📆 ১২ মাসের সারাংশ গ্রিড",
                fontFamily = SolaimanLipi,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            bars.chunked(4).forEachIndexed { rowIdx, rowMonths ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowMonths.forEachIndexed { colIdx, monthBucket ->
                        val absIdx = rowIdx * 4 + colIdx
                        val selected = absIdx == selectedIndex
                        val pct = if (monthBucket.total > 0) {
                            ((monthBucket.completed * 100f) / monthBucket.total).roundToInt()
                        } else 0
                        val statusColor = barColorForStatus(monthBucket.status)

                        Surface(
                            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
                            else statusColor.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(10.dp),
                            border = onePxBorder(
                                if (selected) MaterialTheme.colorScheme.primary
                                else statusColor.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectMonth(absIdx) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = monthBucket.label,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Text(
                                    text = "${HabitScheduleCalculator.toBengaliDigits(pct.toString())}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = statusColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HabitConsistencyBreakdownSection(
    period: ReportPeriod,
    stats: List<HabitConsistencyStat>
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "📊 ${period.labelBn} রুটিন-ভিত্তিক ধারাবাহিকতা",
                fontFamily = SolaimanLipi,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            stats.forEach { item ->
                val emoji = when (item.category) {
                    HabitScheduleCalculator.CATEGORY_MEAL_LINKED -> "🍽️"
                    HabitScheduleCalculator.CATEGORY_MEDICATION -> "💊"
                    HabitScheduleCalculator.CATEGORY_HYDRATION -> "💧"
                    else -> "⏰"
                }
                val accent = when {
                    item.completionPercent >= 80 -> Color(0xFF2E7D32)
                    item.completionPercent >= 40 -> MaterialTheme.colorScheme.primary
                    else -> Color(0xFFED8A0A)
                }
                val progressFraction = (item.completionPercent / 100f).coerceIn(0f, 1f)

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$emoji ${item.title}",
                            fontFamily = SolaimanLipi,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${HabitScheduleCalculator.toBengaliDigits(item.completedDays.toString())}/${HabitScheduleCalculator.toBengaliDigits(item.scheduledDays.toString())} (${HabitScheduleCalculator.toBengaliDigits(item.completionPercent.toString())}%)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(7.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progressFraction)
                                .clip(RoundedCornerShape(50))
                                .background(accent)
                        )
                    }
                }
            }
        }
    }
}
