package com.health.knowledge.habit

import androidx.compose.runtime.Immutable
import com.health.knowledge.data.local.entity.HabitEntity
import com.health.knowledge.data.local.entity.HabitExecutionLogEntity
import java.util.Calendar
import kotlin.math.max
import kotlin.math.roundToInt

enum class ReportPeriod(val labelBn: String) {
    DAILY("দৈনিক"),
    WEEKLY("সাপ্তাহিক"),
    MONTHLY("মাসিক"),
    YEARLY("বাৎসরিক")
}

@Immutable
data class DateStripItem(
    val dayStartMillis: Long,
    val dayEndMillis: Long,
    val dayOfWeekIso: Int,
    val shortDayNameBn: String,
    val dayOfMonthBn: String,
    val fullDateTitleBn: String,
    val monthYearTitleBn: String,
    val isToday: Boolean,
    val isSelected: Boolean,
    val isPast: Boolean,
    val isFuture: Boolean,
    val scheduledCount: Int,
    val completedCount: Int
)

@Immutable
data class ReportBarBucket(
    val label: String,
    val completed: Int,
    val total: Int,
    val fraction: Float,
    val isHighlighted: Boolean = false
)

@Immutable
data class HabitPeriodReport(
    val period: ReportPeriod,
    val periodTitleBn: String,
    val periodStartMillis: Long,
    val periodEndMillis: Long,
    val totalScheduled: Int,
    val completedCount: Int,
    val pendingCount: Int,
    val missedCount: Int,
    val completionPercent: Int,
    val bars: List<ReportBarBucket>,
    val filteredLogs: List<HabitExecutionLogEntity>
)

object HabitDateAndReportEngine {

    const val DAYS_BEFORE_TODAY = 30
    const val DAYS_AFTER_TODAY = 30
    const val TODAY_INDEX = DAYS_BEFORE_TODAY

    private val shortWeekdaysBn = mapOf(
        1 to "সোম",
        2 to "মঙ্গল",
        3 to "বুধ",
        4 to "বৃহঃ",
        5 to "শুক্র",
        6 to "শনি",
        7 to "রবি"
    )

    private val fullWeekdaysBn = mapOf(
        1 to "সোমবার",
        2 to "মঙ্গলবার",
        3 to "বুধবার",
        4 to "বৃহস্পতিবার",
        5 to "শুক্রবার",
        6 to "শনিবার",
        7 to "রবিবার"
    )

    private val fullMonthsBn = arrayOf(
        "জানুয়ারি",
        "ফেব্রুয়ারি",
        "মার্চ",
        "এপ্রিল",
        "মে",
        "জুন",
        "জুলাই",
        "আগস্ট",
        "সেপ্টেম্বর",
        "অক্টোবর",
        "নভেম্বর",
        "ডিসেম্বর"
    )

    private val shortMonthsBn = arrayOf(
        "জানু",
        "ফেব্রু",
        "মার্চ",
        "এপ্রিল",
        "মে",
        "জুন",
        "জুলাই",
        "আগস্ট",
        "সেপ্টে",
        "অক্টো",
        "নভে",
        "ডিসে"
    )

    fun startOfDayMillis(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun endOfDayMillis(timeMillis: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timeMillis
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    fun isSameDay(millisA: Long, millisB: Long): Boolean {
        return startOfDayMillis(millisA) == startOfDayMillis(millisB)
    }

    fun addDays(baseMillis: Long, deltaDays: Int): Long {
        return Calendar.getInstance().apply {
            timeInMillis = startOfDayMillis(baseMillis)
            add(Calendar.DAY_OF_YEAR, deltaDays)
        }.timeInMillis
    }

    fun calendarDayToIso(calendarDayOfWeek: Int): Int {
        return when (calendarDayOfWeek) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            Calendar.SUNDAY -> 7
            else -> 1
        }
    }

    fun isoDayOfWeek(timeMillis: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        return calendarDayToIso(cal.get(Calendar.DAY_OF_WEEK))
    }

    fun anchorTimestampForDate(dateMillis: Long, anchorTime: String): Long {
        val (h, m) = HabitScheduleCalculator.parseAnchorTime(anchorTime)
        return Calendar.getInstance().apply {
            timeInMillis = startOfDayMillis(dateMillis)
            set(Calendar.HOUR_OF_DAY, h)
            set(Calendar.MINUTE, m)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    fun formatFullDateBn(timeMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val isoDay = calendarDayToIso(cal.get(Calendar.DAY_OF_WEEK))
        val dayName = fullWeekdaysBn[isoDay] ?: ""
        val dayNum = HabitScheduleCalculator.toBengaliDigits(
            String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
        )
        val monthName = fullMonthsBn[cal.get(Calendar.MONTH)]
        val year = HabitScheduleCalculator.toBengaliDigits(cal.get(Calendar.YEAR).toString())
        return "$dayName, $dayNum $monthName $year"
    }

    fun formatShortDateBn(timeMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val dayNum = HabitScheduleCalculator.toBengaliDigits(
            String.format("%02d", cal.get(Calendar.DAY_OF_MONTH))
        )
        val monthName = shortMonthsBn[cal.get(Calendar.MONTH)]
        return "$dayNum $monthName"
    }

    fun formatMonthYearBn(timeMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        val monthName = fullMonthsBn[cal.get(Calendar.MONTH)]
        val year = HabitScheduleCalculator.toBengaliDigits(cal.get(Calendar.YEAR).toString())
        return "$monthName $year"
    }

    fun isHabitScheduledOnDate(habit: HabitEntity, dateMillis: Long): Boolean {
        val repeatDays = HabitScheduleCalculator.parseRepeatDays(habit.repeatDays)
        val isoDay = isoDayOfWeek(dateMillis)
        return isoDay in repeatDays
    }

    fun getHabitsForDate(habits: List<HabitEntity>, dateMillis: Long): List<HabitEntity> {
        val isoDay = isoDayOfWeek(dateMillis)
        return habits.filter { habit ->
            isoDay in HabitScheduleCalculator.parseRepeatDays(habit.repeatDays)
        }
    }

    fun getCompletedHabitIdsForDate(
        logs: List<HabitExecutionLogEntity>,
        dateMillis: Long
    ): Set<String> {
        val dayStart = startOfDayMillis(dateMillis)
        val dayEnd = endOfDayMillis(dateMillis)
        return logs.asSequence()
            .filter { log ->
                val ts = log.executedTime ?: log.scheduledTime
                log.status == HabitScheduleCalculator.STATUS_COMPLETED && ts in dayStart..dayEnd
            }
            .map { it.habitId }
            .toSet()
    }

    /**
     * Generates the 61-day horizontal date strip (-30 days to +30 days around today).
     */
    fun buildDateStripItems(
        habits: List<HabitEntity>,
        logs: List<HabitExecutionLogEntity>,
        selectedDateMillis: Long,
        nowMillis: Long
    ): List<DateStripItem> {
        val todayStart = startOfDayMillis(nowMillis)
        val selectedStart = startOfDayMillis(selectedDateMillis)
        val activeHabits = habits.filter { it.isActive == 1 }

        // Group completed habit IDs by dayStartMillis for O(1) lookup per date
        val completedByDayStart = mutableMapOf<Long, MutableSet<String>>()
        for (log in logs) {
            if (log.status == HabitScheduleCalculator.STATUS_COMPLETED) {
                val ts = log.executedTime ?: log.scheduledTime
                val dStart = startOfDayMillis(ts)
                completedByDayStart.getOrPut(dStart) { mutableSetOf() }.add(log.habitId)
            }
        }

        val items = ArrayList<DateStripItem>(DAYS_BEFORE_TODAY + DAYS_AFTER_TODAY + 1)
        val cal = Calendar.getInstance()

        for (offset in -DAYS_BEFORE_TODAY..DAYS_AFTER_TODAY) {
            cal.timeInMillis = todayStart
            cal.add(Calendar.DAY_OF_YEAR, offset)
            val dStart = cal.timeInMillis
            val dEnd = endOfDayMillis(dStart)
            val isoDay = calendarDayToIso(cal.get(Calendar.DAY_OF_WEEK))
            val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)

            val scheduledActive = activeHabits.filter {
                isoDay in HabitScheduleCalculator.parseRepeatDays(it.repeatDays)
            }
            val completedIds = completedByDayStart[dStart].orEmpty()
            val completedCount = scheduledActive.count { it.id in completedIds }

            items.add(
                DateStripItem(
                    dayStartMillis = dStart,
                    dayEndMillis = dEnd,
                    dayOfWeekIso = isoDay,
                    shortDayNameBn = shortWeekdaysBn[isoDay] ?: "",
                    dayOfMonthBn = HabitScheduleCalculator.toBengaliDigits(
                        String.format("%02d", dayOfMonth)
                    ),
                    fullDateTitleBn = formatFullDateBn(dStart),
                    monthYearTitleBn = formatMonthYearBn(dStart),
                    isToday = dStart == todayStart,
                    isSelected = dStart == selectedStart,
                    isPast = dStart < todayStart,
                    isFuture = dStart > todayStart,
                    scheduledCount = scheduledActive.size,
                    completedCount = completedCount
                )
            )
        }
        return items
    }

    /**
     * Computes Daily, Weekly, Monthly, or Yearly report relative to [selectedDateMillis].
     */
    fun computePeriodReport(
        habits: List<HabitEntity>,
        logs: List<HabitExecutionLogEntity>,
        selectedDateMillis: Long,
        nowMillis: Long,
        period: ReportPeriod
    ): HabitPeriodReport {
        val activeHabits = habits.filter { it.isActive == 1 }
        val selectedStart = startOfDayMillis(selectedDateMillis)
        val todayStart = startOfDayMillis(nowMillis)

        // Pre-index completed habit IDs by dayStartMillis
        val completedByDay = mutableMapOf<Long, MutableSet<String>>()
        for (log in logs) {
            if (log.status == HabitScheduleCalculator.STATUS_COMPLETED) {
                val ts = log.executedTime ?: log.scheduledTime
                val dStart = startOfDayMillis(ts)
                completedByDay.getOrPut(dStart) { mutableSetOf() }.add(log.habitId)
            }
        }

        return when (period) {
            ReportPeriod.DAILY -> computeDailyReport(
                activeHabits = activeHabits,
                logs = logs,
                completedByDay = completedByDay,
                selectedStart = selectedStart,
                todayStart = todayStart,
                nowMillis = nowMillis
            )

            ReportPeriod.WEEKLY -> computeWeeklyReport(
                activeHabits = activeHabits,
                logs = logs,
                completedByDay = completedByDay,
                selectedStart = selectedStart,
                todayStart = todayStart,
                nowMillis = nowMillis
            )

            ReportPeriod.MONTHLY -> computeMonthlyReport(
                activeHabits = activeHabits,
                logs = logs,
                completedByDay = completedByDay,
                selectedStart = selectedStart,
                todayStart = todayStart,
                nowMillis = nowMillis
            )

            ReportPeriod.YEARLY -> computeYearlyReport(
                activeHabits = activeHabits,
                logs = logs,
                completedByDay = completedByDay,
                selectedStart = selectedStart,
                todayStart = todayStart,
                nowMillis = nowMillis
            )
        }
    }

    private fun computeDailyReport(
        activeHabits: List<HabitEntity>,
        logs: List<HabitExecutionLogEntity>,
        completedByDay: Map<Long, Set<String>>,
        selectedStart: Long,
        todayStart: Long,
        nowMillis: Long
    ): HabitPeriodReport {
        val selectedEnd = endOfDayMillis(selectedStart)
        val isoDay = isoDayOfWeek(selectedStart)
        val scheduledToday = activeHabits.filter {
            isoDay in HabitScheduleCalculator.parseRepeatDays(it.repeatDays)
        }
        val completedIds = completedByDay[selectedStart].orEmpty()

        var completedCount = 0
        var missedCount = 0
        var pendingCount = 0

        // 4 time-of-day buckets for Daily chart: সকাল (<12), দুপুর (12..15), বিকাল (16..18), রাত (19..23)
        val bucketRanges = listOf(
            "সকাল" to (0..11),
            "দুপুর" to (12..15),
            "বিকাল" to (16..18),
            "রাত" to (19..23)
        )
        val bucketTotal = IntArray(4)
        val bucketDone = IntArray(4)

        for (habit in scheduledToday) {
            val (h, _) = HabitScheduleCalculator.parseAnchorTime(habit.anchorTime)
            val bIdx = when (h) {
                in 0..11 -> 0
                in 12..15 -> 1
                in 16..18 -> 2
                else -> 3
            }
            bucketTotal[bIdx]++

            if (habit.id in completedIds) {
                completedCount++
                bucketDone[bIdx]++
            } else {
                val anchorTs = anchorTimestampForDate(selectedStart, habit.anchorTime)
                when {
                    selectedStart < todayStart -> missedCount++
                    selectedStart == todayStart && anchorTs + 60 * 60_000L < nowMillis -> missedCount++
                    else -> pendingCount++
                }
            }
        }

        val total = scheduledToday.size
        val percent = if (total > 0) ((completedCount * 100f) / total).roundToInt() else 0

        val bars = bucketRanges.mapIndexed { idx, (label, _) ->
            val bTot = bucketTotal[idx]
            val bDon = bucketDone[idx]
            ReportBarBucket(
                label = label,
                completed = bDon,
                total = bTot,
                fraction = if (bTot > 0) (bDon.toFloat() / bTot.toFloat()).coerceIn(0f, 1f) else 0f,
                isHighlighted = bDon > 0 && bDon == bTot
            )
        }

        val periodLogs = logs.filter {
            val ts = it.executedTime ?: it.scheduledTime
            ts in selectedStart..selectedEnd
        }

        return HabitPeriodReport(
            period = ReportPeriod.DAILY,
            periodTitleBn = formatFullDateBn(selectedStart),
            periodStartMillis = selectedStart,
            periodEndMillis = selectedEnd,
            totalScheduled = total,
            completedCount = completedCount,
            pendingCount = pendingCount,
            missedCount = missedCount,
            completionPercent = percent,
            bars = bars,
            filteredLogs = periodLogs
        )
    }

    private fun computeWeeklyReport(
        activeHabits: List<HabitEntity>,
        logs: List<HabitExecutionLogEntity>,
        completedByDay: Map<Long, Set<String>>,
        selectedStart: Long,
        todayStart: Long,
        nowMillis: Long
    ): HabitPeriodReport {
        // Compute Monday..Sunday of the week containing selectedStart
        val isoSelected = isoDayOfWeek(selectedStart) // 1 = Mon .. 7 = Sun
        val weekStart = addDays(selectedStart, -(isoSelected - 1))
        val weekEnd = endOfDayMillis(addDays(weekStart, 6))

        var totalScheduled = 0
        var completedCount = 0
        var pendingCount = 0
        var missedCount = 0

        val bars = ArrayList<ReportBarBucket>(7)

        for (dayIdx in 0..6) {
            val dStart = addDays(weekStart, dayIdx)
            val isoDay = dayIdx + 1
            val dayHabits = activeHabits.filter {
                isoDay in HabitScheduleCalculator.parseRepeatDays(it.repeatDays)
            }
            val doneIds = completedByDay[dStart].orEmpty()
            var dDone = 0

            for (habit in dayHabits) {
                totalScheduled++
                if (habit.id in doneIds) {
                    completedCount++
                    dDone++
                } else {
                    val anchorTs = anchorTimestampForDate(dStart, habit.anchorTime)
                    when {
                        dStart < todayStart -> missedCount++
                        dStart == todayStart && anchorTs + 60 * 60_000L < nowMillis -> missedCount++
                        else -> pendingCount++
                    }
                }
            }

            val dTot = dayHabits.size
            bars.add(
                ReportBarBucket(
                    label = shortWeekdaysBn[isoDay] ?: "",
                    completed = dDone,
                    total = dTot,
                    fraction = if (dTot > 0) (dDone.toFloat() / dTot.toFloat()).coerceIn(0f, 1f) else 0f,
                    isHighlighted = dStart == selectedStart
                )
            )
        }

        val percent = if (totalScheduled > 0) {
            ((completedCount * 100f) / totalScheduled).roundToInt()
        } else 0

        val periodLogs = logs.filter {
            val ts = it.executedTime ?: it.scheduledTime
            ts in weekStart..weekEnd
        }

        val titleBn = "${formatShortDateBn(weekStart)} – ${formatShortDateBn(weekEnd)}"

        return HabitPeriodReport(
            period = ReportPeriod.WEEKLY,
            periodTitleBn = titleBn,
            periodStartMillis = weekStart,
            periodEndMillis = weekEnd,
            totalScheduled = totalScheduled,
            completedCount = completedCount,
            pendingCount = pendingCount,
            missedCount = missedCount,
            completionPercent = percent,
            bars = bars,
            filteredLogs = periodLogs
        )
    }

    private fun computeMonthlyReport(
        activeHabits: List<HabitEntity>,
        logs: List<HabitExecutionLogEntity>,
        completedByDay: Map<Long, Set<String>>,
        selectedStart: Long,
        todayStart: Long,
        nowMillis: Long
    ): HabitPeriodReport {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedStart }
        val selectedDayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val monthStart = startOfDayMillis(cal.timeInMillis)
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, maxDay)
        val monthEnd = endOfDayMillis(cal.timeInMillis)

        var totalScheduled = 0
        var completedCount = 0
        var pendingCount = 0
        var missedCount = 0

        // Group into 5 weekly buckets of the month: ১-৭, ৮-১৪, ১৫-২১, ২২-২৮, ২৯+
        val weekBuckets = listOf(
            "সপ্তাহ ১" to (1..7),
            "সপ্তাহ ২" to (8..14),
            "সপ্তাহ ৩" to (15..21),
            "সপ্তাহ ৪" to (22..28),
            "সপ্তাহ ৫" to (29..31)
        )
        val wTot = IntArray(5)
        val wDone = IntArray(5)

        val dayCal = Calendar.getInstance()
        for (dayNum in 1..maxDay) {
            dayCal.timeInMillis = monthStart
            dayCal.set(Calendar.DAY_OF_MONTH, dayNum)
            val dStart = startOfDayMillis(dayCal.timeInMillis)
            val isoDay = calendarDayToIso(dayCal.get(Calendar.DAY_OF_WEEK))
            val wIdx = ((dayNum - 1) / 7).coerceIn(0, 4)

            val dayHabits = activeHabits.filter {
                isoDay in HabitScheduleCalculator.parseRepeatDays(it.repeatDays)
            }
            val doneIds = completedByDay[dStart].orEmpty()

            for (habit in dayHabits) {
                totalScheduled++
                wTot[wIdx]++
                if (habit.id in doneIds) {
                    completedCount++
                    wDone[wIdx]++
                } else {
                    val anchorTs = anchorTimestampForDate(dStart, habit.anchorTime)
                    when {
                        dStart < todayStart -> missedCount++
                        dStart == todayStart && anchorTs + 60 * 60_000L < nowMillis -> missedCount++
                        else -> pendingCount++
                    }
                }
            }
        }

        val selectedWeekIdx = ((selectedDayOfMonth - 1) / 7).coerceIn(0, 4)
        val activeBucketCount = if (maxDay > 28) 5 else 4
        val bars = (0 until activeBucketCount).map { idx ->
            val tot = wTot[idx]
            val don = wDone[idx]
            ReportBarBucket(
                label = weekBuckets[idx].first,
                completed = don,
                total = tot,
                fraction = if (tot > 0) (don.toFloat() / tot.toFloat()).coerceIn(0f, 1f) else 0f,
                isHighlighted = idx == selectedWeekIdx
            )
        }

        val percent = if (totalScheduled > 0) {
            ((completedCount * 100f) / totalScheduled).roundToInt()
        } else 0

        val periodLogs = logs.filter {
            val ts = it.executedTime ?: it.scheduledTime
            ts in monthStart..monthEnd
        }

        return HabitPeriodReport(
            period = ReportPeriod.MONTHLY,
            periodTitleBn = formatMonthYearBn(selectedStart),
            periodStartMillis = monthStart,
            periodEndMillis = monthEnd,
            totalScheduled = totalScheduled,
            completedCount = completedCount,
            pendingCount = pendingCount,
            missedCount = missedCount,
            completionPercent = percent,
            bars = bars,
            filteredLogs = periodLogs
        )
    }

    private fun computeYearlyReport(
        activeHabits: List<HabitEntity>,
        logs: List<HabitExecutionLogEntity>,
        completedByDay: Map<Long, Set<String>>,
        selectedStart: Long,
        todayStart: Long,
        nowMillis: Long
    ): HabitPeriodReport {
        val cal = Calendar.getInstance().apply { timeInMillis = selectedStart }
        val year = cal.get(Calendar.YEAR)
        val selectedMonth = cal.get(Calendar.MONTH)

        cal.set(Calendar.MONTH, Calendar.JANUARY)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val yearStart = startOfDayMillis(cal.timeInMillis)

        cal.set(Calendar.MONTH, Calendar.DECEMBER)
        cal.set(Calendar.DAY_OF_MONTH, 31)
        val yearEnd = endOfDayMillis(cal.timeInMillis)

        var totalScheduled = 0
        var completedCount = 0
        var pendingCount = 0
        var missedCount = 0

        val mTot = IntArray(12)
        val mDone = IntArray(12)

        val iterCal = Calendar.getInstance().apply { timeInMillis = yearStart }
        while (iterCal.get(Calendar.YEAR) == year) {
            val dStart = startOfDayMillis(iterCal.timeInMillis)
            val mIdx = iterCal.get(Calendar.MONTH)
            val isoDay = calendarDayToIso(iterCal.get(Calendar.DAY_OF_WEEK))
            val dayHabits = activeHabits.filter {
                isoDay in HabitScheduleCalculator.parseRepeatDays(it.repeatDays)
            }
            val doneIds = completedByDay[dStart].orEmpty()

            for (habit in dayHabits) {
                totalScheduled++
                mTot[mIdx]++
                if (habit.id in doneIds) {
                    completedCount++
                    mDone[mIdx]++
                } else {
                    val anchorTs = anchorTimestampForDate(dStart, habit.anchorTime)
                    when {
                        dStart < todayStart -> missedCount++
                        dStart == todayStart && anchorTs + 60 * 60_000L < nowMillis -> missedCount++
                        else -> pendingCount++
                    }
                }
            }
            iterCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val bars = (0..11).map { mIdx ->
            val tot = mTot[mIdx]
            val don = mDone[mIdx]
            ReportBarBucket(
                label = shortMonthsBn[mIdx],
                completed = don,
                total = tot,
                fraction = if (tot > 0) (don.toFloat() / tot.toFloat()).coerceIn(0f, 1f) else 0f,
                isHighlighted = mIdx == selectedMonth
            )
        }

        val percent = if (totalScheduled > 0) {
            ((completedCount * 100f) / totalScheduled).roundToInt()
        } else 0

        val periodLogs = logs.filter {
            val ts = it.executedTime ?: it.scheduledTime
            ts in yearStart..yearEnd
        }

        val yearBn = HabitScheduleCalculator.toBengaliDigits(year.toString())

        return HabitPeriodReport(
            period = ReportPeriod.YEARLY,
            periodTitleBn = "$yearBn সাল",
            periodStartMillis = yearStart,
            periodEndMillis = yearEnd,
            totalScheduled = totalScheduled,
            completedCount = completedCount,
            pendingCount = pendingCount,
            missedCount = missedCount,
            completionPercent = percent,
            bars = bars,
            filteredLogs = periodLogs
        )
    }
}
