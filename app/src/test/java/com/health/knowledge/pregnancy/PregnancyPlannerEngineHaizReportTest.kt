package com.health.knowledge.pregnancy

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class PregnancyPlannerEngineHaizReportTest {

    private fun dateMillis(year: Int, monthZeroBased: Int, dayOfMonth: Int): Long {
        return Calendar.getInstance().apply {
            set(year, monthZeroBased, dayOfMonth, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun `actual logged month report calculates duration, cycle gap, calendar cells and normal notice`() {
        val octStart = dateMillis(2026, Calendar.OCTOBER, 5)
        val octEnd = dateMillis(2026, Calendar.OCTOBER, 9) // 5 days
        val novStart = dateMillis(2026, Calendar.NOVEMBER, 3)
        val novEnd = dateMillis(2026, Calendar.NOVEMBER, 7) // 5 days, 29-day cycle gap

        val history = listOf(
            PeriodCycleEntry(id = "oct", startMillis = octStart, endMillis = octEnd, flowLevel = "মাঝারি (স্বাভাবিক)"),
            PeriodCycleEntry(id = "nov", startMillis = novStart, endMillis = novEnd, flowLevel = "বেশি / ভারী", note = "স্বাভাবিক")
        )

        val novReport = PregnancyPlannerEngine.buildMonthlyHaizReport(
            year = 2026,
            monthZeroBased = Calendar.NOVEMBER,
            history = history,
            averageCycleDays = 28,
            todayMillis = dateMillis(2026, Calendar.NOVEMBER, 14)
        )

        assertTrue(novReport.hasActualLog)
        assertFalse(novReport.isProjected)
        assertEquals(5, novReport.durationDays)
        assertEquals(29, novReport.cycleGapDays)
        assertFalse(novReport.isWarningNotice)
        assertEquals(30, novReport.calendarCells.size)

        val periodCells = novReport.calendarCells.filter { it.type == HaizCalendarDayType.PERIOD_ACTUAL }
        assertEquals(listOf(3, 4, 5, 6, 7), periodCells.map { it.dayOfMonth })
        assertEquals(listOf(1, 2, 3, 4, 5), periodCells.map { it.haizDayNumber })

        // Ovulation for Nov 3 + 29 - 14 = Nov 18
        val ovCell = novReport.calendarCells.firstOrNull { it.type == HaizCalendarDayType.OVULATION }
        assertEquals(18, ovCell?.dayOfMonth)
    }

    @Test
    fun `cross-month haiz entry displays accurately in both overlapping months`() {
        val crossStart = dateMillis(2026, Calendar.OCTOBER, 29)
        val crossEnd = dateMillis(2026, Calendar.NOVEMBER, 2) // 5 days: Oct 29,30,31 + Nov 1,2

        val history = listOf(
            PeriodCycleEntry(id = "cross", startMillis = crossStart, endMillis = crossEnd)
        )

        val octReport = PregnancyPlannerEngine.buildMonthlyHaizReport(
            year = 2026,
            monthZeroBased = Calendar.OCTOBER,
            history = history,
            averageCycleDays = 28
        )
        val novReport = PregnancyPlannerEngine.buildMonthlyHaizReport(
            year = 2026,
            monthZeroBased = Calendar.NOVEMBER,
            history = history,
            averageCycleDays = 28
        )

        assertTrue(octReport.hasActualLog)
        assertEquals(listOf(29, 30, 31), octReport.calendarCells.filter { it.type == HaizCalendarDayType.PERIOD_ACTUAL }.map { it.dayOfMonth })

        assertTrue(novReport.hasActualLog)
        assertEquals(listOf(1, 2), novReport.calendarCells.filter { it.type == HaizCalendarDayType.PERIOD_ACTUAL }.map { it.dayOfMonth })
        assertEquals(listOf(4, 5), novReport.calendarCells.filter { it.type == HaizCalendarDayType.PERIOD_ACTUAL }.map { it.haizDayNumber })
    }

    @Test
    fun `future unlogged month generates projected haiz report`() {
        val novStart = dateMillis(2026, Calendar.NOVEMBER, 10)
        val novEnd = dateMillis(2026, Calendar.NOVEMBER, 14) // 5 days
        val history = listOf(PeriodCycleEntry(id = "nov", startMillis = novStart, endMillis = novEnd))

        // 28 days after Nov 10 -> Dec 8..Dec 12
        val decReport = PregnancyPlannerEngine.buildMonthlyHaizReport(
            year = 2026,
            monthZeroBased = Calendar.DECEMBER,
            history = history,
            averageCycleDays = 28
        )

        assertFalse(decReport.hasActualLog)
        assertTrue(decReport.isProjected)
        assertEquals(5, decReport.durationDays)
        assertEquals(listOf(8, 9, 10, 11, 12), decReport.calendarCells.filter { it.type == HaizCalendarDayType.PERIOD_PROJECTED }.map { it.dayOfMonth })
    }

    @Test
    fun `short under 3 days and prolonged over 7 days trigger health and fiqh warnings`() {
        val shortEntry = PeriodCycleEntry(
            id = "short",
            startMillis = dateMillis(2026, Calendar.AUGUST, 5),
            endMillis = dateMillis(2026, Calendar.AUGUST, 6) // 2 days
        )
        val longEntry = PeriodCycleEntry(
            id = "long",
            startMillis = dateMillis(2026, Calendar.SEPTEMBER, 4),
            endMillis = dateMillis(2026, Calendar.SEPTEMBER, 14) // 11 days
        )

        val augReport = PregnancyPlannerEngine.buildMonthlyHaizReport(
            year = 2026,
            monthZeroBased = Calendar.AUGUST,
            history = listOf(shortEntry, longEntry)
        )
        val sepReport = PregnancyPlannerEngine.buildMonthlyHaizReport(
            year = 2026,
            monthZeroBased = Calendar.SEPTEMBER,
            history = listOf(shortEntry, longEntry)
        )

        assertTrue(augReport.isWarningNotice)
        assertEquals(2, augReport.durationDays)
        assertTrue(augReport.healthNoticeDetailBn.contains("ইস্তিহাযা"))

        assertTrue(sepReport.isWarningNotice)
        assertEquals(11, sepReport.durationDays)
        assertTrue(sepReport.healthNoticeDetailBn.contains("ইস্তিহাযা"))
    }

    @Test
    fun `yearly haiz trend returns 12 month bars with actual and projected statuses`() {
        val history = listOf(
            PeriodCycleEntry(
                id = "jan",
                startMillis = dateMillis(2026, Calendar.JANUARY, 10),
                endMillis = dateMillis(2026, Calendar.JANUARY, 14)
            )
        )
        val trend = PregnancyPlannerEngine.buildYearlyHaizTrend(
            year = 2026,
            history = history,
            averageCycleDays = 28
        )

        assertEquals(12, trend.size)
        assertTrue(trend[Calendar.JANUARY].hasActualLog)
        assertEquals(5, trend[Calendar.JANUARY].durationDays)
        assertFalse(trend[Calendar.FEBRUARY].hasActualLog)
        assertTrue(trend[Calendar.FEBRUARY].isProjected)
    }
}
