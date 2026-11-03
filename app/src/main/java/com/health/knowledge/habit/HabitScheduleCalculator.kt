package com.health.knowledge.habit

import java.util.Calendar
import kotlin.math.abs

/**
 * Pure mathematical calculation & state-machine rules for the Smart Habit & Routine Engine.
 *
 * Schedule Formulas:
 * 1. Pre-Event Trigger:      T_pre  = T_anchor - Δt_pre
 * 2. Anchor Event Trigger:   T_anchor (exact HH:mm)
 * 3. Post-Event Dynamic:     T_post = T_done + Δt_post (Case A: User confirms at T_done)
 * 4. Post-Event Fallback:    T_post = T_anchor + DefaultDuration(30m) + Δt_post (Case B: Unconfirmed)
 */
object HabitScheduleCalculator {

    const val PHASE_PRE_EVENT = "PRE_EVENT"
    const val PHASE_EXACT_EVENT = "EXACT_EVENT"
    const val PHASE_POST_EVENT = "POST_EVENT"

    const val CATEGORY_MEAL_LINKED = "MEAL_LINKED"
    const val CATEGORY_MEDICATION = "MEDICATION"
    const val CATEGORY_HYDRATION = "HYDRATION"
    const val CATEGORY_CUSTOM = "CUSTOM"

    const val AUDIO_TTS_VOICE = "TTS_VOICE"
    const val AUDIO_PRESET_TONE = "PRESET_TONE"
    const val AUDIO_SYSTEM_DEFAULT = "SYSTEM_DEFAULT"

    const val STATUS_PENDING = "PENDING"
    const val STATUS_COMPLETED = "COMPLETED"
    const val STATUS_SNOOZED = "SNOOZED"
    const val STATUS_MISSED = "MISSED"
    const val STATUS_DISMISSED = "DISMISSED"

    const val DEFAULT_GRACE_DURATION_MINS = 30
    const val OVERLAP_WINDOW_MS = 2 * 60 * 1000L // 2 minutes

    /**
     * Parses "HH:mm" (24h format) into Pair(hour, minute).
     */
    fun parseAnchorTime(anchorTime: String): Pair<Int, Int> {
        val parts = anchorTime.trim().split(":")
        val h = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 14
        val m = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        return h to m
    }

    /**
     * Parses JSON array string e.g. "[1,2,3,4,5,6,7]" where 1=Monday .. 7=Sunday.
     */
    fun parseRepeatDays(repeatDaysJson: String): Set<Int> {
        val cleaned = repeatDaysJson
            .replace("[", "")
            .replace("]", "")
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in 1..7 }
            .toSet()
        return if (cleaned.isEmpty()) (1..7).toSet() else cleaned
    }

    fun formatRepeatDaysJson(days: Collection<Int>): String {
        val sorted = days.filter { it in 1..7 }.distinct().sorted()
        val finalDays = if (sorted.isEmpty()) (1..7).toList() else sorted
        return finalDays.joinToString(prefix = "[", postfix = "]", separator = ",")
    }

    /**
     * Formula 1: T_pre = T_anchor - abs(preOffsetMins)
     * Supports positive or negative offset inputs gracefully (always subtracts the magnitude from anchor,
     * or if a raw signed delta is evaluated via applyMinuteOffset, shifts across midnight accurately).
     */
    fun calculatePreEventTriggerMillis(anchorTimeMillis: Long, preOffsetMins: Int): Long {
        val safeOffsetMins = abs(preOffsetMins)
        return anchorTimeMillis - (safeOffsetMins * 60_000L)
    }

    /**
     * Applies any signed minute offset (negative or positive) to a base timestamp,
     * preserving exact millisecond precision across midnight boundaries.
     */
    fun applySignedMinuteOffset(baseTimeMillis: Long, signedOffsetMins: Int): Long {
        return baseTimeMillis + (signedOffsetMins * 60_000L)
    }

    /**
     * Formula 3 (Case A): User confirms at T_done -> T_post = T_done + Δt_post
     */
    fun calculateDynamicPostEventTriggerMillis(completedTimeMillis: Long, postOffsetMins: Int): Long {
        val safeOffsetMins = abs(postOffsetMins)
        return completedTimeMillis + (safeOffsetMins * 60_000L)
    }

    /**
     * Formula 3 (Case B Fallback): User does not confirm within grace period ->
     * T_post = T_anchor + DefaultDuration (30 mins) + Δt_post
     */
    fun calculateFallbackPostEventTriggerMillis(
        anchorTimeMillis: Long,
        postOffsetMins: Int,
        defaultDurationMins: Int = DEFAULT_GRACE_DURATION_MINS
    ): Long {
        val totalMins = abs(defaultDurationMins) + abs(postOffsetMins)
        return anchorTimeMillis + (totalMins * 60_000L)
    }

    /**
     * Calculates the next future trigger timestamp in milliseconds for an anchor event and a signed minute offset
     * (e.g., -30 for pre-event, 0 for exact anchor event, +75 for fallback post-event).
     *
     * Accurately handles midnight boundary crossing:
     * Example: If anchor_time is "00:15" on Tuesday (day=2) and signedOffsetMins is -30,
     * the pre-event actually fires at "23:45" on Monday night!
     */
    fun calculateNextOccurrenceMillis(
        anchorTime: String,
        signedOffsetMins: Int,
        repeatDaysJson: String,
        nowMillis: Long = System.currentTimeMillis()
    ): Long {
        val (anchorHour, anchorMin) = parseAnchorTime(anchorTime)
        val activeDays = parseRepeatDays(repeatDaysJson)

        // Check candidate anchor days from yesterday (-1) up to +8 days ahead
        // Checking -1 ensures that if anchor was earlier or if a positive offset pushes into the future,
        // or if a negative offset crosses midnight from tomorrow into tonight, we find the earliest valid trigger > nowMillis.
        for (dayDelta in -1..8) {
            val anchorCal = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                add(Calendar.DAY_OF_YEAR, dayDelta)
                set(Calendar.HOUR_OF_DAY, anchorHour)
                set(Calendar.MINUTE, anchorMin)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val isoDayOfWeek = calendarDayToIsoDay(anchorCal.get(Calendar.DAY_OF_WEEK))
            if (isoDayOfWeek in activeDays) {
                val candidateTriggerMillis = applySignedMinuteOffset(anchorCal.timeInMillis, signedOffsetMins)
                if (candidateTriggerMillis > nowMillis + 1_000L) {
                    return candidateTriggerMillis
                }
            }
        }

        // Fallback: tomorrow at anchor + offset
        val fallbackCal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, anchorHour)
            set(Calendar.MINUTE, anchorMin)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return applySignedMinuteOffset(fallbackCal.timeInMillis, signedOffsetMins)
    }

    /**
     * Converts Calendar.DAY_OF_WEEK (SUN=1..SAT=7) to ISO-8601 (MON=1..SUN=7).
     */
    fun calendarDayToIsoDay(calendarDayOfWeek: Int): Int {
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

    /**
     * Checks whether a given timestamp falls inside the user's Do-Not-Disturb (DND) sleep window.
     * Default sleep window: 23:00 to 06:30 (crosses midnight).
     */
    fun isInsideSleepWindow(
        triggerTimeMillis: Long,
        sleepStartHour: Int = 23,
        sleepStartMinute: Int = 0,
        sleepEndHour: Int = 6,
        sleepEndMinute: Int = 30
    ): Boolean {
        val cal = Calendar.getInstance().apply {
            timeInMillis = triggerTimeMillis
        }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        return isTimeInSleepWindow(
            hour = hour,
            minute = minute,
            sleepStartHour = sleepStartHour,
            sleepStartMinute = sleepStartMinute,
            sleepEndHour = sleepEndHour,
            sleepEndMinute = sleepEndMinute
        )
    }

    fun isTimeInSleepWindow(
        hour: Int,
        minute: Int,
        sleepStartHour: Int = 23,
        sleepStartMinute: Int = 0,
        sleepEndHour: Int = 6,
        sleepEndMinute: Int = 30
    ): Boolean {
        val currentMins = hour * 60 + minute
        val startMins = sleepStartHour * 60 + sleepStartMinute
        val endMins = sleepEndHour * 60 + sleepEndMinute

        return if (startMins == endMins) {
            false
        } else if (startMins > endMins) {
            // Window crosses midnight (e.g. 23:00 [1380] to 06:30 [390])
            currentMins >= startMins || currentMins < endMins
        } else {
            // Same-day window (e.g. 01:00 to 06:00)
            currentMins in startMins until endMins
        }
    }

    /**
     * Edge Case: Overlapping Triggers
     * Returns true if two triggers fire within <= 2 minutes of each other.
     */
    fun areTriggersOverlapping(
        firstTriggerMillis: Long,
        secondTriggerMillis: Long,
        maxWindowMillis: Long = OVERLAP_WINDOW_MS
    ): Boolean {
        return abs(firstTriggerMillis - secondTriggerMillis) <= maxWindowMillis
    }

    /**
     * Formats 24h "HH:mm" or timestamp into friendly Bengali time string.
     */
    fun format24hToBengali(anchorTime: String): String {
        val (hour, minute) = parseAnchorTime(anchorTime)
        return formatHourMinuteBengali(hour, minute)
    }

    fun formatMillisToBengaliTime(timeMillis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        return formatHourMinuteBengali(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
    }

    fun formatHourMinuteBengali(hour: Int, minute: Int): String {
        val period = when (hour) {
            in 5..11 -> "সকাল"
            in 12..14 -> "দুপুর"
            in 15..17 -> "বিকাল"
            in 18..19 -> "সন্ধ্যা"
            else -> "রাত"
        }
        val h12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "$period ${toBengaliDigits("%02d".format(h12))}:${toBengaliDigits("%02d".format(minute))}"
    }

    fun toBengaliDigits(input: String): String {
        return input.map { ch ->
            when (ch) {
                '0' -> '০'
                '1' -> '১'
                '2' -> '২'
                '3' -> '৩'
                '4' -> '৪'
                '5' -> '৫'
                '6' -> '৬'
                '7' -> '৭'
                '8' -> '৮'
                '9' -> '৯'
                else -> ch
            }
        }.joinToString("")
    }
}
