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
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
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
import com.health.knowledge.habit.HabitPeriodReport
import com.health.knowledge.habit.HabitScheduleCalculator
import com.health.knowledge.habit.ReportBarBucket
import com.health.knowledge.habit.ReportPeriod
import com.health.knowledge.ui.theme.onePxBorder
import com.health.knowledge.ui.theme.solidOverSurface
import kotlin.math.max

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
 * Next Alarm Banner (when viewing Today), and Visual Bar Chart.
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("habit_period_report_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.34f).solidOverSurface()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.40f))
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
                    modifier = Modifier.size(68.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { 1f },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                        strokeWidth = 6.dp,
                        strokeCap = StrokeCap.Round
                    )
                    CircularProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 6.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${HabitScheduleCalculator.toBengaliDigits(report.completionPercent.toString())}%",
                            style = MaterialTheme.typography.labelLarge,
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
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = report.periodTitleBn,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
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
                            accentColor = Color(0xFFC62828),
                            modifier = Modifier.weight(1f)
                        )
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

            // Visual Period Bar Chart (Daily 4 slots / Weekly 7 days / Monthly 4-5 weeks / Yearly 12 months)
            if (report.bars.isNotEmpty()) {
                PeriodBarChartSection(bars = report.bars)
            }
        }
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
private fun PeriodBarChartSection(
    bars: List<ReportBarBucket>
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            bars.forEach { bucket ->
                val animatedFraction by animateFloatAsState(
                    targetValue = bucket.fraction.coerceIn(0.08f, 1f),
                    animationSpec = tween(durationMillis = 400),
                    label = "bar_${bucket.label}"
                )
                val barColor = when {
                    bucket.completed > 0 && bucket.completed >= bucket.total -> Color(0xFF2E7D32)
                    bucket.isHighlighted -> MaterialTheme.colorScheme.primary
                    bucket.completed > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = HabitScheduleCalculator.toBengaliDigits(bucket.completed.toString()),
                        fontSize = 9.sp,
                        fontWeight = if (bucket.isHighlighted) FontWeight.Bold else FontWeight.Normal,
                        color = if (bucket.isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Box(
                        modifier = Modifier
                            .width(12.dp)
                            .height(44.dp)
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
                        fontSize = 9.sp,
                        fontWeight = if (bucket.isHighlighted) FontWeight.Bold else FontWeight.Medium,
                        color = if (bucket.isHighlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}
