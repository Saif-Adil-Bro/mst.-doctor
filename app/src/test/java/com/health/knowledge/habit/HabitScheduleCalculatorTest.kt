package com.health.knowledge.habit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class HabitScheduleCalculatorTest {

    @Test
    fun `negative offset calculates exact pre-event timestamp`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 3, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val anchorMillis = cal.timeInMillis

        // Pre-event 30 mins before 14:00 -> 13:30
        val preMillis = HabitScheduleCalculator.calculatePreEventTriggerMillis(anchorMillis, 30)
        val resultCal = Calendar.getInstance().apply { timeInMillis = preMillis }

        assertEquals(13, resultCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, resultCal.get(Calendar.MINUTE))

        // Also verify applySignedMinuteOffset with explicit negative offset (-45 mins)
        val signedPreMillis = HabitScheduleCalculator.applySignedMinuteOffset(anchorMillis, -45)
        val signedCal = Calendar.getInstance().apply { timeInMillis = signedPreMillis }
        assertEquals(13, signedCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, signedCal.get(Calendar.MINUTE))
    }

    @Test
    fun `midnight boundary crossing backward for pre-event and forward for post-event`() {
        // Case 1: Anchor at 00:15 AM on Nov 4, 2026 with -30 mins pre-offset -> 23:45 PM on Nov 3, 2026
        val nowCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 3, 22, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nextPreTrigger = HabitScheduleCalculator.calculateNextOccurrenceMillis(
            anchorTime = "00:15",
            signedOffsetMins = -30,
            repeatDaysJson = "[1,2,3,4,5,6,7]",
            nowMillis = nowCal.timeInMillis
        )
        val preCal = Calendar.getInstance().apply { timeInMillis = nextPreTrigger }
        assertEquals(3, preCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, preCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(45, preCal.get(Calendar.MINUTE))

        // Case 2: Meal completed at 23:30 PM on Nov 3, 2026 with +45 mins post-offset -> 00:15 AM on Nov 4, 2026
        val doneCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 3, 23, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val postTrigger = HabitScheduleCalculator.calculateDynamicPostEventTriggerMillis(
            completedTimeMillis = doneCal.timeInMillis,
            postOffsetMins = 45
        )
        val postCal = Calendar.getInstance().apply { timeInMillis = postTrigger }
        assertEquals(4, postCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, postCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, postCal.get(Calendar.MINUTE))
    }

    @Test
    fun `sleep window suppression detects overnight DND boundary 23_00 to 06_30`() {
        // Default sleep window: 23:00 to 06:30
        assertTrue(HabitScheduleCalculator.isTimeInSleepWindow(23, 0, 23, 0, 6, 30))
        assertTrue(HabitScheduleCalculator.isTimeInSleepWindow(23, 45, 23, 0, 6, 30))
        assertTrue(HabitScheduleCalculator.isTimeInSleepWindow(0, 15, 23, 0, 6, 30))
        assertTrue(HabitScheduleCalculator.isTimeInSleepWindow(6, 29, 23, 0, 6, 30))

        // Outside sleep window
        assertFalse(HabitScheduleCalculator.isTimeInSleepWindow(6, 30, 23, 0, 6, 30))
        assertFalse(HabitScheduleCalculator.isTimeInSleepWindow(14, 45, 23, 0, 6, 30))
        assertFalse(HabitScheduleCalculator.isTimeInSleepWindow(22, 59, 23, 0, 6, 30))
    }

    @Test
    fun `fallback post-event and overlapping triggers follow specification`() {
        val anchorCal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 3, 14, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Fallback T_post = T_anchor (14:00) + 30m grace + 45m post_offset = 15:15
        val fallbackMillis = HabitScheduleCalculator.calculateFallbackPostEventTriggerMillis(
            anchorTimeMillis = anchorCal.timeInMillis,
            postOffsetMins = 45,
            defaultDurationMins = 30
        )
        val fallbackCal = Calendar.getInstance().apply { timeInMillis = fallbackMillis }
        assertEquals(15, fallbackCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(15, fallbackCal.get(Calendar.MINUTE))

        // Overlapping triggers within <= 2 minutes
        val t1 = anchorCal.timeInMillis
        val t2Within90Sec = t1 + 90_000L
        val t3After5Min = t1 + 5 * 60_000L
        assertTrue(HabitScheduleCalculator.areTriggersOverlapping(t1, t2Within90Sec))
        assertFalse(HabitScheduleCalculator.areTriggersOverlapping(t1, t3After5Min))
    }
}
