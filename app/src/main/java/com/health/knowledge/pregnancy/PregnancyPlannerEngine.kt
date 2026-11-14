package com.health.knowledge.pregnancy

import androidx.compose.runtime.Immutable
import com.health.knowledge.habit.HabitDateAndReportEngine
import com.health.knowledge.habit.HabitScheduleCalculator
import java.util.Calendar
import kotlin.math.roundToInt

@Immutable
data class PregnancyTimelineStatus(
    val startMillis: Long,
    val eddMillis: Long,
    val cycleDays: Int,
    val totalCycleAdjustedDays: Int,
    val startDateFormattedBn: String,
    val eddDateFormattedBn: String,
    val elapsedDays: Int,
    val remainingDays: Int,
    val currentWeek: Int,
    val extraDaysInWeek: Int,
    val trimester: Int,
    val trimesterLabelBn: String,
    val progressFraction: Float,
    val progressPercent: Int,
    val isFullTerm: Boolean = false
)

@Immutable
data class FertileWindowStatus(
    val lastPeriodStartMillis: Long,
    val lastPeriodEndMillis: Long = 0L,
    val periodDurationDays: Int = 0,
    val periodDurationNoticeBn: String? = null,
    val cycleDays: Int,
    val lastPeriodFormattedBn: String,
    val lastPeriodEndFormattedBn: String = "",
    val fertileStartFormattedBn: String,
    val fertileEndFormattedBn: String,
    val ovulationDateFormattedBn: String,
    val nextPeriodFormattedBn: String,
    val ovulationDateMillis: Long = 0L,
    val nextPeriodDateMillis: Long = 0L,
    val currentCycleDay: Int,
    val isCurrentlyFertile: Boolean,
    val isNextPeriodDueOrMissed: Boolean = false,
    val statusHeadlineBn: String
)

@Immutable
data class BabyAgeStatus(
    val deliveryMillis: Long,
    val deliveryType: String, // "NORMAL", "C_SECTION"
    val babyGender: String,   // "BOY", "GIRL", "UNSPECIFIED"
    val deliveryFormattedBn: String,
    val ageDaysTotal: Int,
    val ageWeeksTotal: Int,
    val ageMonthsTotal: Int,
    val ageDisplayBn: String,
    val milestoneTitleBn: String,
    val milestoneDetailBn: String
)

@Immutable
data class PostpartumBabyCareTip(
    val id: String,
    val category: String, // "BREASTFEEDING", "CORD_CARE", "MASSAGE", "SLEEP", "COLIC", "MILESTONES"
    val emoji: String,
    val titleBn: String,
    val detailBn: String,
    val highlightBn: String
)

@Immutable
data class PostpartumMotherCareTip(
    val id: String,
    val deliveryTypeTarget: String, // "ALL", "NORMAL", "C_SECTION"
    val emoji: String,
    val titleBn: String,
    val detailBn: String,
    val highlightBn: String
)

@Immutable
data class StageFatherTip(
    val id: String,
    val stage: String, // "PRE", "PREGNANT", "POSTPARTUM"
    val emoji: String,
    val badgeBn: String,
    val titleBn: String,
    val detailBn: String,
    val actionBn: String
)

@Immutable
data class StageSafetyWarning(
    val id: String,
    val stage: String, // "PRE", "PREGNANT", "POSTPARTUM"
    val isRedFlag: Boolean, // true = danger sign, false = foods/herbs to avoid
    val emoji: String,
    val titleBn: String,
    val detailBn: String
)

@Immutable
data class InstantHomeRemedy(
    val id: String,
    val symptomId: String,
    val emoji: String,
    val titleBn: String,
    val remedyListBn: List<String>,
    val cautionBn: String
)

@Immutable
data class PregnancySymptomOption(
    val id: String,
    val emoji: String,
    val labelBn: String,
    val adviceTitleBn: String,
    val adviceDetailBn: String
)

@Immutable
data class MaleHealthTip(
    val id: String,
    val category: String, // "TRICKS", "SUNNAH_FOOD", "FATHER_GUIDE"
    val emoji: String,
    val badgeBn: String,
    val titleBn: String,
    val detailBn: String,
    val tipHighlightBn: String
)

@Immutable
data class WeeklyBabyGrowth(
    val week: Int,
    val emoji: String,
    val sizeComparisonBn: String,
    val lengthBn: String,
    val weightBn: String,
    val milestoneTitleBn: String,
    val developmentDetailBn: String,
    val spiritualReflectionBn: String
)

@Immutable
data class PregnancyChecklistItem(
    val id: String,
    val category: String, // "PRE_CONCEPTION", "DO", "DONT", "NEWBORN_SUNNAH", "FATHER_DUTY"
    val trimester: Int,   // 0 = all trimesters, 1, 2, 3
    val titleBn: String,
    val subtitleBn: String
)

@Immutable
data class IslamicPregnancyAmal(
    val id: String,
    val stageTag: String, // "PRE", "TRIMESTER_1", "TRIMESTER_2", "TRIMESTER_3", "POST_BIRTH"
    val stageBadgeBn: String,
    val titleBn: String,
    val arabicText: String,
    val pronunciationBn: String,
    val meaningBn: String,
    val referenceBn: String,
    val benefitBn: String
)

@Immutable
data class PregnancyQuote(
    val id: Int,
    val tagBn: String,
    val quoteBn: String,
    val sourceBn: String
)

@Immutable
data class PregnancyReminderPreset(
    val id: String,
    val stage: String, // "PRE" or "POST"
    val emoji: String,
    val titleBn: String,
    val category: String,
    val anchorTime: String,
    val preOffsetMins: Int,
    val postOffsetMins: Int,
    val messageBn: String
)

@Immutable
data class PeriodCycleEntry(
    val id: String,
    val startMillis: Long,
    val endMillis: Long = 0L,
    val flowLevel: String = "মাঝারি (স্বাভাবিক)",
    val note: String = "",
    val updatedAtMillis: Long = 0L
)

enum class HaizCalendarDayType {
    NORMAL,
    PERIOD_ACTUAL,
    PERIOD_PROJECTED,
    FERTILE,
    OVULATION
}

@Immutable
data class HaizCalendarCell(
    val dayOfMonth: Int,
    val dateMillis: Long,
    val type: HaizCalendarDayType,
    val haizDayNumber: Int = 0,
    val isToday: Boolean = false
)

@Immutable
data class MonthlyHaizReport(
    val year: Int,
    val monthZeroBased: Int,
    val monthYearTitleBn: String,
    val shortMonthLabelBn: String,
    val hasActualLog: Boolean,
    val isProjected: Boolean,
    val activeEntry: PeriodCycleEntry? = null,
    val entriesInMonth: List<PeriodCycleEntry> = emptyList(),
    val periodStartMillis: Long = 0L,
    val periodEndMillis: Long = 0L,
    val periodDateRangeBn: String = "লগ করা হয়নি",
    val durationDays: Int = 0,
    val cycleGapDays: Int = 0,
    val fertileStartMillis: Long = 0L,
    val fertileEndMillis: Long = 0L,
    val fertileRangeBn: String = "—",
    val ovulationMillis: Long = 0L,
    val ovulationDateBn: String = "—",
    val flowLevelBn: String = "—",
    val noteBn: String = "",
    val healthNoticeTitleBn: String = "",
    val healthNoticeDetailBn: String = "",
    val isWarningNotice: Boolean = false,
    val calendarLeadingEmptyCount: Int = 0,
    val calendarCells: List<HaizCalendarCell> = emptyList()
)

@Immutable
data class YearlyHaizMonthBar(
    val monthZeroBased: Int,
    val shortLabelBn: String,
    val fullLabelBn: String,
    val durationDays: Int,
    val cycleGapDays: Int,
    val hasActualLog: Boolean,
    val isProjected: Boolean,
    val isWarningDuration: Boolean
)

object PregnancyPlannerEngine {

    const val TOTAL_PREGNANCY_DAYS = 280
    private const val DAY_MILLIS = 86_400_000L

    fun calculateTimelineStatus(
        startMillis: Long,
        cycleDays: Int = 28,
        referenceDateMillis: Long = System.currentTimeMillis()
    ): PregnancyTimelineStatus {
        val safeCycle = cycleDays.coerceIn(18, 60)
        val cycleOffset = safeCycle - 28
        val totalAdjustedDays = (TOTAL_PREGNANCY_DAYS + cycleOffset).coerceIn(260, 312)

        val cleanStart = HabitDateAndReportEngine.startOfDayMillis(startMillis)
        val cleanRef = HabitDateAndReportEngine.startOfDayMillis(referenceDateMillis)
        val eddMillis = HabitDateAndReportEngine.addDays(cleanStart, totalAdjustedDays)

        val rawElapsed = ((cleanRef - cleanStart) / DAY_MILLIS).toInt()
        val elapsedDays = rawElapsed.coerceIn(1, totalAdjustedDays)
        val remainingDays = (totalAdjustedDays - elapsedDays).coerceAtLeast(0)

        val currentWeek = ((elapsedDays - 1) / 7 + 1).coerceIn(1, 42)
        val extraDaysInWeek = (elapsedDays - 1) % 7

        val trimester = when (currentWeek) {
            in 1..12 -> 1
            in 13..27 -> 2
            else -> 3
        }
        val trimesterLabelBn = when (trimester) {
            1 -> "১ম ত্রৈমাসিক"
            2 -> "২য় ত্রৈমাসিক"
            else -> "৩য় ত্রৈমাসিক"
        }

        val fraction = (elapsedDays.toFloat() / totalAdjustedDays.toFloat()).coerceIn(0f, 1f)
        val percent = (fraction * 100f).roundToInt().coerceIn(0, 100)
        val isFullTerm = currentWeek >= 37 || cleanRef >= eddMillis

        return PregnancyTimelineStatus(
            startMillis = cleanStart,
            eddMillis = eddMillis,
            cycleDays = safeCycle,
            totalCycleAdjustedDays = totalAdjustedDays,
            startDateFormattedBn = HabitDateAndReportEngine.formatFullDateBn(cleanStart),
            eddDateFormattedBn = HabitDateAndReportEngine.formatFullDateBn(eddMillis),
            elapsedDays = elapsedDays,
            remainingDays = remainingDays,
            currentWeek = currentWeek,
            extraDaysInWeek = extraDaysInWeek,
            trimester = trimester,
            trimesterLabelBn = trimesterLabelBn,
            progressFraction = fraction,
            progressPercent = percent,
            isFullTerm = isFullTerm
        )
    }

    /**
     * Calculates ovulation & fertile window for women planning to conceive based on
     * last period start & end dates and menstrual cycle length (18..60 days).
     */
    fun calculateFertileWindow(
        lastPeriodStartMillis: Long,
        lastPeriodEndMillis: Long = 0L,
        cycleDays: Int = 28,
        referenceDateMillis: Long = System.currentTimeMillis()
    ): FertileWindowStatus {
        val safeCycle = cycleDays.coerceIn(18, 60)
        val cleanRef = HabitDateAndReportEngine.startOfDayMillis(referenceDateMillis)
        var cycleStart = HabitDateAndReportEngine.startOfDayMillis(lastPeriodStartMillis)

        // Calculate Period Duration (স্থায়িত্বকাল: End Date - Start Date + 1)
        var periodDuration = 0
        var periodNotice: String? = null
        if (lastPeriodEndMillis > 0L && lastPeriodEndMillis >= cycleStart) {
            periodDuration = (((lastPeriodEndMillis - cycleStart) / DAY_MILLIS).toInt() + 1).coerceAtLeast(1)
            periodNotice = when {
                periodDuration < 3 -> "আপনার মাসিকের স্থায়িত্ব ৩ দিনের কম (স্বল্প রক্তক্ষরণ)। পুষ্টিহীনতা বা রক্তস্বল্পতা রোধে খাদ্যতালিকায় ডালিম, খেজুর, দুধ, ডিম এবং পর্যাপ্ত পানি গ্রহণ করুন।"
                periodDuration > 7 -> "আপনার মাসিকের স্থায়িত্ব ৭ দিনের বেশি (অতিরিক্ত রক্তক্ষরণ)। শরীর দুর্বল লাগলে মধু, কালোজিরা, ভিটামিন-সি ও আয়রনসমৃদ্ধ খাবার গ্রহণ করুন এবং চিকিৎসকের পরামর্শ নিন।"
                else -> null
            }
        }

        val nextPeriodMillis = HabitDateAndReportEngine.addDays(cycleStart, safeCycle)
        val ovulationMillis = HabitDateAndReportEngine.addDays(nextPeriodMillis, -14)
        val fertileStartMillis = HabitDateAndReportEngine.addDays(ovulationMillis, -5)
        val fertileEndMillis = HabitDateAndReportEngine.addDays(ovulationMillis, 1)

        val currentCycleDay = (((cleanRef - cycleStart) / DAY_MILLIS).toInt() + 1).coerceAtLeast(1)
        val isFertileNow = cleanRef in fertileStartMillis..fertileEndMillis
        val isDueOrMissed = cleanRef >= nextPeriodMillis

        val statusHeadline = when {
            isDueOrMissed -> "⚠️ আপনার মাসিকের সম্ভাব্য সময় পার হয়েছে / চলে এসেছে!"
            isFertileNow -> "🌟 এখন আপনার সর্বোচ্চ উর্বর সময় (Fertile Window) চলছে!"
            cleanRef < fertileStartMillis -> {
                val daysLeft = ((fertileStartMillis - cleanRef) / DAY_MILLIS).toInt().coerceAtLeast(1)
                "উর্বর সময় শুরু হতে ${HabitScheduleCalculator.toBengaliDigits(daysLeft.toString())} দিন বাকি"
            }
            else -> {
                val daysToNext = ((nextPeriodMillis - cleanRef) / DAY_MILLIS).toInt().coerceAtLeast(1)
                "পরবর্তী মাসিকের সম্ভাব্য তারিখ ${HabitScheduleCalculator.toBengaliDigits(daysToNext.toString())} দিন পর"
            }
        }

        return FertileWindowStatus(
            lastPeriodStartMillis = cycleStart,
            lastPeriodEndMillis = lastPeriodEndMillis,
            periodDurationDays = periodDuration,
            periodDurationNoticeBn = periodNotice,
            cycleDays = safeCycle,
            lastPeriodFormattedBn = HabitDateAndReportEngine.formatShortDateBn(cycleStart),
            lastPeriodEndFormattedBn = if (lastPeriodEndMillis > 0L) HabitDateAndReportEngine.formatShortDateBn(lastPeriodEndMillis) else "",
            fertileStartFormattedBn = HabitDateAndReportEngine.formatShortDateBn(fertileStartMillis),
            fertileEndFormattedBn = HabitDateAndReportEngine.formatShortDateBn(fertileEndMillis),
            ovulationDateFormattedBn = HabitDateAndReportEngine.formatFullDateBn(ovulationMillis),
            nextPeriodFormattedBn = HabitDateAndReportEngine.formatFullDateBn(nextPeriodMillis),
            ovulationDateMillis = ovulationMillis,
            nextPeriodDateMillis = nextPeriodMillis,
            currentCycleDay = currentCycleDay,
            isCurrentlyFertile = isFertileNow,
            isNextPeriodDueOrMissed = isDueOrMissed,
            statusHeadlineBn = statusHeadline
        )
    }

    fun startMillisFromEdd(eddMillis: Long, cycleDays: Int = 28): Long {
        val safeCycle = cycleDays.coerceIn(18, 60)
        val totalAdjustedDays = (TOTAL_PREGNANCY_DAYS + (safeCycle - 28)).coerceIn(260, 312)
        val cleanEdd = HabitDateAndReportEngine.startOfDayMillis(eddMillis)
        return HabitDateAndReportEngine.addDays(cleanEdd, -totalAdjustedDays)
    }

    fun calculateBabyAge(
        deliveryMillis: Long,
        deliveryType: String = "NORMAL",
        babyGender: String = "UNSPECIFIED",
        referenceDateMillis: Long = System.currentTimeMillis()
    ): BabyAgeStatus {
        val cleanDelivery = HabitDateAndReportEngine.startOfDayMillis(deliveryMillis)
        val cleanRef = HabitDateAndReportEngine.startOfDayMillis(referenceDateMillis)
        val rawDays = ((cleanRef - cleanDelivery) / DAY_MILLIS).toInt().coerceAtLeast(0)
        val ageWeeks = rawDays / 7
        val ageMonths = rawDays / 30
        val daysRemainder = rawDays % 30

        val ageDisplay = when {
            rawDays == 0 -> "আজই জন্ম নিয়েছে 👶"
            rawDays < 7 -> "${HabitScheduleCalculator.toBengaliDigits(rawDays.toString())} দিন"
            rawDays < 30 -> "${HabitScheduleCalculator.toBengaliDigits(ageWeeks.toString())} সপ্তাহ (${HabitScheduleCalculator.toBengaliDigits(rawDays.toString())} দিন)"
            ageMonths < 12 -> {
                if (daysRemainder == 0) {
                    "${HabitScheduleCalculator.toBengaliDigits(ageMonths.toString())} মাস"
                } else {
                    "${HabitScheduleCalculator.toBengaliDigits(ageMonths.toString())} মাস, ${HabitScheduleCalculator.toBengaliDigits(daysRemainder.toString())} দিন"
                }
            }
            else -> {
                val years = ageMonths / 12
                val remMonths = ageMonths % 12
                if (remMonths == 0) {
                    "${HabitScheduleCalculator.toBengaliDigits(years.toString())} বছর"
                } else {
                    "${HabitScheduleCalculator.toBengaliDigits(years.toString())} বছর, ${HabitScheduleCalculator.toBengaliDigits(remMonths.toString())} মাস"
                }
            }
        }

        val (milestoneTitle, milestoneDetail) = when {
            rawDays <= 7 -> "প্রথম সপ্তাহ • নবজাতকের অভিযোজন" to "মায়ের শালদুধই শিশুর প্রথম টিকা ও রোগ প্রতিরোধকারী খাবার। নাভির গোড়া সবসময় শুকনো ও পরিষ্কার রাখুন। দিনে ১৬–১৮ ঘণ্টা ঘুম স্বাভাবিক।"
            rawDays <= 14 -> "২য় সপ্তাহ • নাভির যত্ন ও ওজন পর্যবেক্ষণ" to "নাভির কর্ড সাধারণত ১–২ সপ্তাহের মধ্যে স্বাভাবিকভাবে শুকিয়ে খসে পড়ে। হালকা কুসুম রোদ ও তেল মালিশ শিশুর ত্বকের জন্য উপকারি।"
            rawDays <= 30 -> "১ম মাস • দৃষ্টি ও শব্দের পরিচিতি" to "বাচ্চা ৮–১২ ইঞ্চি দূরত্বের মুখমণ্ডল ফোকাস করতে পারে। মা-বাবার কণ্ঠস্বর চেনে এবং হঠাৎ শব্দে হালকা চমকে ওঠা স্বাভাবিক।"
            rawDays <= 60 -> "২য় মাস • সামাজিক হাসি (Social Smile)" to "বাচ্চা মা-বাবার চোখের দিকে তাকিয়ে হাসতে শুরু করে এবং উপুড় অবস্থায় মাথা সামান্য উঁচু করার চেষ্টা করে।"
            rawDays <= 90 -> "৩য় মাস • হাত-চোখের সমন্বয়" to "হাতের মুঠি খুলে চারপাশের জিনিসে হাত ছোঁয়ায় এবং বিভিন্ন মিষ্টি শব্দ (Cooing) করে আনন্দ প্রকাশ করে। পেটে গ্যাসের জন্য সঠিক বার্পিং দিন।"
            rawDays <= 180 -> "৪–৬ মাস • নিজে নিজে পাশ ফেরা ও শক্ত ঘাড়" to "বাচ্চা নিজে নিজে পাশ ফিরতে পারে এবং ঘাড় সম্পূর্ণ শক্ত হয়। ৬ মাস পূর্ণ না হওয়া পর্যন্ত শুধুমাত্র মায়ের বুকের দুধই একমাত্র খাদ্য ও পানীয়।"
            else -> "৬+ মাস • পারিবারিক নরম পুষ্টিকর খাদ্য শুরু" to "বুকের দুধের পাশাপাশি ঘরে তৈরি নরম সুষম খাবার (খিচুড়ি, সুজি, ডিমের কুসুম, চটকানো ফল) অল্প অল্প করে শুরু করুন।"
        }

        return BabyAgeStatus(
            deliveryMillis = cleanDelivery,
            deliveryType = deliveryType,
            babyGender = babyGender,
            deliveryFormattedBn = HabitDateAndReportEngine.formatFullDateBn(cleanDelivery),
            ageDaysTotal = rawDays,
            ageWeeksTotal = ageWeeks,
            ageMonthsTotal = ageMonths,
            ageDisplayBn = ageDisplay,
            milestoneTitleBn = milestoneTitle,
            milestoneDetailBn = milestoneDetail
        )
    }

    fun getGrowthForWeek(week: Int): WeeklyBabyGrowth {
        val safeWeek = week.coerceIn(1, 40)
        return weeklyGrowthCatalog.firstOrNull { it.week == safeWeek }
            ?: weeklyGrowthCatalog.last()
    }

    val symptomOptions: List<PregnancySymptomOption> = listOf(
        PregnancySymptomOption(
            id = "normal",
            emoji = "😊",
            labelBn = "স্বাভাবিক",
            adviceTitleBn = "আলহামদুলিল্লাহ, আপনার শরীর স্বাভাবিক আছে",
            adviceDetailBn = "প্রতিদিনের সুষম খাবার, ৮–১০ গ্লাস পানি এবং নিয়মিত বিশ্রাম ও কুরআন তিলাওয়াত অব্যাহত রাখুন।"
        ),
        PregnancySymptomOption(
            id = "nausea",
            emoji = "🤢",
            labelBn = "বমিভাব",
            adviceTitleBn = "সকালের বমিভাব ও অরুচি কমানোর উপায়",
            adviceDetailBn = "সকালে ঘুম থেকে উঠেই বিস্কুট বা মুড়ি খান। একবারে বেশি না খেয়ে অল্প অল্প করে বারবার খান এবং আদা-লেবুর ঘ্রাণ বা হালকা আদা পানি পান করুন।"
        ),
        PregnancySymptomOption(
            id = "fatigue",
            emoji = "😴",
            labelBn = "ক্লান্তি",
            adviceTitleBn = "অতিরিক্ত ক্লান্তি ও দুর্বলতায় করণীয়",
            adviceDetailBn = "দুপুরে অন্তত ১ ঘণ্টা বাম কাত হয়ে বিশ্রাম নিন। খাদ্যতালিকায় খেজুর, দুধ, ডিম ও আয়রনসমৃদ্ধ খাবার রাখুন এবং হিমোগ্লোবিন পরীক্ষা করুন।"
        ),
        PregnancySymptomOption(
            id = "dizziness",
            emoji = "💫",
            labelBn = "মাথা ঘোরা",
            adviceTitleBn = "মাথা ঘোরা বা দুর্বল লাগলে সতর্কতা",
            adviceDetailBn = "হঠাৎ শোয়া বা বসা থেকে দ্রুত উঠবেন না। পর্যাপ্ত পানি ও ডাবের পানি পান করুন এবং রক্তচাপ (Blood Pressure) নিয়মিত চেক করুন।"
        ),
        PregnancySymptomOption(
            id = "back_pain",
            emoji = "🦴",
            labelBn = "কোমর ব্যথা",
            adviceTitleBn = "কোমর ও পিঠের ব্যথায় আরাম পাওয়ার কৌশল",
            adviceDetailBn = "সোজা হয়ে বসুন, নিচু হয়ে কিছু তুলবেন না এবং ঘুমানোর সময় দুই হাঁটুর মাঝখানে নরম বালিশ ব্যবহার করুন। ক্যালসিয়াম ও ভিটামিন-ডি নিয়মিত নিন।"
        ),
        PregnancySymptomOption(
            id = "heartburn",
            emoji = "🔥",
            labelBn = "বুক জ্বালা",
            adviceTitleBn = "অ্যাসিডিটি ও বুক জ্বালাপোড়া রোধে করণীয়",
            adviceDetailBn = "অতিরিক্ত তেল-মসলা ও ভাজাপোড়া খাবার এড়িয়ে চলুন। রাতের খাবার ঘুমানোর অন্তত ২ ঘণ্টা আগে সম্পন্ন করুন এবং মাথা সামান্য উঁচু রেখে ঘুমান।"
        ),
        PregnancySymptomOption(
            id = "cramps",
            emoji = "⚡",
            labelBn = "তলপেটে টান",
            adviceTitleBn = "তলপেটে হালকা টান বা পায়ে খিল ধরা",
            adviceDetailBn = "ভারী কাজ সম্পূর্ণ বন্ধ রাখুন ও পর্যাপ্ত পানি পান করুন। তবে তীব্র ব্যথা বা রক্তপাত দেখা দিলে দেরি না করে দ্রুত চিকিৎসকের পরামর্শ নিন।"
        )
    )

    val maleHealthTipsCatalog: List<MaleHealthTip> = listOf(
        // 1. TRICKS (পুরুষের ফার্টিলিটি ও স্বাস্থ্য ট্রিকস)
        MaleHealthTip(
            id = "male_trick_sleep_testosterone",
            category = "TRICKS",
            emoji = "🌙",
            badgeBn = "হরমোন ও শক্তি",
            titleBn = "রাত ১১টার মধ্যে ঘুম ও গভীর বিশ্রাম",
            detailBn = "গবেষণায় দেখা গেছে, রাতে ৭–৮ ঘণ্টা গভীর ঘুম পুরুষের টেস্টোস্টেরন হরমোন ও শুক্রাণুর গুণগত মান ৩০% পর্যন্ত উন্নত রাখে।",
            tipHighlightBn = "রাত জাগা ও অতিরিক্ত মোবাইল ব্যবহার হরমোন কমিয়ে দেয়।"
        ),
        MaleHealthTip(
            id = "male_trick_zinc_selenium",
            category = "TRICKS",
            emoji = "🥜",
            badgeBn = "ফার্টিলিটি বুস্ট",
            titleBn = "জিঙ্ক, সেলেনিয়াম ও অ্যান্টিঅক্সিডেন্ট গ্রহণ",
            detailBn = "কুমড়োর বিচি, কাঠবাদাম, আখরোট, ডিম ও সামুদ্রিক মাছে থাকা জিঙ্ক ও ওমেগা-৩ পুরুষের প্রজনন সক্ষমতা ও গতিশীলতা বৃদ্ধি করে।",
            tipHighlightBn = "প্রতিদিন সকালে ৫–৭টি ভেজানো কাঠবাদাম ও ২টি আখরোট খান।"
        ),
        MaleHealthTip(
            id = "male_trick_heat_protection",
            category = "TRICKS",
            emoji = "🌡️",
            badgeBn = "গুরুত্বপূর্ণ সতর্কতা",
            titleBn = "অতিরিক্ত তাপ ও টাইট পোশাক পরিহার",
            detailBn = "কোলের ওপর ল্যাপটপ রেখে দীর্ঘক্ষণ কাজ করা, অতিরিক্ত গরম পানিতে গোসল বা খুব টাইট পোশাক শুক্রাণু উৎপাদনে বাধা সৃষ্টি করে।",
            tipHighlightBn = "ঢিলেঢালা সুতি পোশাক পরুন এবং একটানা বসে থাকলে প্রতি ঘণ্টায় হাঁটুন।"
        ),
        MaleHealthTip(
            id = "male_trick_smoke_stress",
            category = "TRICKS",
            emoji = "🚭",
            badgeBn = "সুস্থ শুক্রাণু",
            titleBn = "ধূমপান ত্যাগ ও মানসিক চাপ নিয়ন্ত্রণ",
            detailBn = "ধূমপান ও তামাকজাত দ্রব্য শুক্রাণুর ডিএনএ ক্ষতিগ্রস্ত করে। সুস্থ সন্তানের পরিকল্পনার অন্তত ৩ মাস আগে থেকেই ধূমপান সম্পূর্ণ বর্জন করুন।",
            tipHighlightBn = "প্রতিদিন ৩০ মিনিট দ্রুত হাঁটা বা ব্যায়াম রক্ত সঞ্চালন বাড়ায়।"
        ),

        // 2. SUNNAH_FOOD (সুন্নাহ পুষ্টি ও খাবার)
        MaleHealthTip(
            id = "male_sunnah_kalojira_honey",
            category = "SUNNAH_FOOD",
            emoji = "🍯",
            badgeBn = "সুন্নাহ সুপারফুড",
            titleBn = "কালোজিরা ও খাঁটি মধুর জাদুকরী মিশ্রণ",
            detailBn = "রাসূলুল্লাহ (সা.) বলেছেন, মৃত্যু ছাড়া সর্বরোগের মহৌষধ কালোজিরা। আধুনিক গবেষণায় কালোজিরা ও মধু পুরুষের ফার্টিলিটি ও স্ট্যামিনা বৃদ্ধিতে অত্যন্ত কার্যকর প্রমাণিত।",
            tipHighlightBn = "সকালে খালি পেটে আধা চামচ কালোজিরা ও ১ চামচ মধু চিবিয়ে খান।"
        ),
        MaleHealthTip(
            id = "male_sunnah_dates_milk",
            category = "SUNNAH_FOOD",
            emoji = "🥛",
            badgeBn = "শারীরিক শক্তি",
            titleBn = "হালকা গরম দুধে খেজুর ও কিসমিস",
            detailBn = "খেজুরে রয়েছে প্রাকৃতিক আয়রন, পটাশিয়াম, ম্যাগনেসিয়াম ও অ্যামিনো অ্যাসিড, যা স্নায়বিক ক্লান্তি দূর করে এবং প্রজনন শক্তি বৃদ্ধি করে।",
            tipHighlightBn = "রাতে ঘুমানোর ১ ঘণ্টা আগে ১ গ্লাস কুসুম গরম দুধ ও ৩–৫টি খেজুর খান।"
        ),
        MaleHealthTip(
            id = "male_sunnah_fig_olive",
            category = "SUNNAH_FOOD",
            emoji = "🫒",
            badgeBn = "কুরআনি ফল",
            titleBn = "ত্বীন (ডুমুর), জয়তুন ও ডালিম",
            detailBn = "পবিত্র কুরআনে বর্ণিত ত্বীন (ডুমুর) ও জয়তুন (অলিভ অয়েল) এবং ডালিম রক্ত সঞ্চালন বাড়ায়, হৃদযন্ত্র সুস্থ রাখে এবং হরমোনের ভারসাম্য রক্ষা করে।",
            tipHighlightBn = "সালাদে এক্সট্রা ভার্জিন অলিভ অয়েল ও মৌসুমি ডালিম রাখুন।"
        ),

        // 3. FATHER_GUIDE (হবু বাবার করণীয় গাইড)
        MaleHealthTip(
            id = "male_father_emotional_support",
            category = "FATHER_GUIDE",
            emoji = "💝",
            badgeBn = "মানসিক যত্ন",
            titleBn = "স্ত্রীর মেজাজের পরিবর্তন ও মানসিক সাপোর্ট",
            detailBn = "গর্ভকালে হরমোনের কারণে স্ত্রীর বমিভাব, ক্লান্তি ও আবেগপ্রবণতা বাড়ে। এই সময়ে স্বামীর একটু সহানুভূতি, হাসিমুখ ও সান্ত্বনা মা ও গর্ভস্থ সন্তানের জন্য সবচেয়ে বড় ওষুধ।",
            tipHighlightBn = "স্ত্রীকে কখনোই মানসিক চাপ বা কটু কথা বলবেন না।"
        ),
        MaleHealthTip(
            id = "male_father_heavy_work_nutrition",
            category = "FATHER_GUIDE",
            emoji = "🏡",
            badgeBn = "দৈনন্দিন দায়িত্ব",
            titleBn = "ভারী কাজে সাহায্য ও পুষ্টি নিশ্চিত করা",
            detailBn = "স্ত্রীর ভারী কাজগুলো নিজে করুন এবং তিনি সময়মতো আয়রন, ক্যালসিয়াম, দুধ, ডিম ও পানি খাচ্ছেন কি না তা যত্নসহকারে খেয়াল রাখুন।",
            tipHighlightBn = "ডাক্তারের চেকআপ ও আল্ট্রাসাউন্ডের দিন স্ত্রীর সাথে নিজে যান।"
        ),
        MaleHealthTip(
            id = "male_father_spiritual_bond",
            category = "FATHER_GUIDE",
            emoji = "🤲",
            badgeBn = "ইসলামি পিতৃত্ব",
            titleBn = "একসাথে কুরআন তিলাওয়াত ও নেক সন্তানের দোয়া",
            detailBn = "সন্তান গর্ভে আসার পর থেকেই বাবার দোয়া সন্তানের তাকদীর ও চরিত্র গঠনে ভূমিকা রাখে। প্রতিদিন স্ত্রীর পাশে বসে কুরআন তিলাওয়াত ও দোয়া করুন।",
            tipHighlightBn = "হালাল উপার্জন নিশ্চিত করুন—এটি নেক সন্তানের প্রধান ভিত্তি।"
        )
    )

    val fatherhoodChecklist: List<PregnancyChecklistItem> = listOf(
        PregnancyChecklistItem(
            id = "father_check_sunnah_food",
            category = "FATHER_DUTY",
            trimester = 0,
            titleBn = "কালোজিরা, মধু, খেজুর ও বাদাম গ্রহণ",
            subtitleBn = "প্রজনন স্বাস্থ্য ও স্ট্যামিনা অটুট রাখতে প্রতিদিনের সুন্নাহ খাবার গ্রহণ করুন।"
        ),
        PregnancyChecklistItem(
            id = "father_check_wife_medicine",
            category = "FATHER_DUTY",
            trimester = 0,
            titleBn = "স্ত্রীর ভিটামিন, ফলিক অ্যাসিড ও পুষ্টির খোঁজ নেওয়া",
            subtitleBn = "স্ত্রী সময়মতো ওষুধ ও পুষ্টিকর খাবার খেয়েছেন কি না প্রতিদিন খোঁজ নিন।"
        ),
        PregnancyChecklistItem(
            id = "father_check_no_smoke_sleep",
            category = "FATHER_DUTY",
            trimester = 0,
            titleBn = "ধূমপানমুক্ত পরিবেশ ও ৭–৮ ঘণ্টা ঘুম",
            subtitleBn = "গর্ভবতী স্ত্রী ও অনাগত সন্তানের সুরক্ষায় ঘরের পরিবেশ ধোঁয়ামুক্ত ও প্রশান্ত রাখুন।"
        ),
        PregnancyChecklistItem(
            id = "father_check_dua_halal",
            category = "FATHER_DUTY",
            trimester = 0,
            titleBn = "ফরয নামাযের পর নেক সন্তানের জন্য দোয়া",
            subtitleBn = "রাব্বি হাবলী মিনাস সালিহীন ও রাব্বানা হাবলানা দোয়া নিয়মিত পাঠ করুন।"
        )
    )

    val preConceptionChecklist: List<PregnancyChecklistItem> = listOf(
        PregnancyChecklistItem(
            id = "pre_folic_acid",
            category = "PRE_CONCEPTION",
            trimester = 0,
            titleBn = "ফলিক অ্যাসিড গ্রহণ শুরু করুন",
            subtitleBn = "গর্ভধারণের অন্তত ১–৩ মাস আগে থেকে প্রতিদিন ৪০০ মাইক্রোগ্রাম ফলিক অ্যাসিড শিশুর মস্তিষ্ক ও স্নায়ুর সুরক্ষায় অপরিহার্য।"
        ),
        PregnancyChecklistItem(
            id = "pre_blood_thyroid",
            category = "PRE_CONCEPTION",
            trimester = 0,
            titleBn = "রক্ত, হিমোগ্লোবিন ও থাইরয়েড পরীক্ষা",
            subtitleBn = "রক্তস্বল্পতা, থাইরয়েড হরমোন, ব্লাড সুগার ও রক্তের গ্রুপ (Rh ফ্যাক্টর) আগেভাগেই চেক করে নিন।"
        ),
        PregnancyChecklistItem(
            id = "pre_sunnah_nutrition",
            category = "PRE_CONCEPTION",
            trimester = 0,
            titleBn = "পুষ্টিকর ও সুন্নাহ খাবার গ্রহণ",
            subtitleBn = "প্রতিদিন খেজুর, দুধ, ডিম, কালোজিরা, মধু, ডালিম ও সবুজ শাকসবজি খাদ্যতালিকায় রাখুন।"
        ),
        PregnancyChecklistItem(
            id = "pre_healthy_weight",
            category = "PRE_CONCEPTION",
            trimester = 0,
            titleBn = "সুষম ওজন ও পর্যাপ্ত ঘুম নিশ্চিত করুন",
            subtitleBn = "রাত জাগা পরিহার করুন, প্রতিদিন ৩০ মিনিট হাঁটুন এবং মানসিক চাপমুক্ত থাকুন।"
        ),
        PregnancyChecklistItem(
            id = "pre_avoid_harmful",
            category = "PRE_CONCEPTION",
            trimester = 0,
            titleBn = "অতিরিক্ত ক্যাফেইন ও ক্ষতিকর ওষুধ বর্জন",
            subtitleBn = "চিকিৎসকের পরামর্শ ছাড়া কোনো ব্যথানাশক বা অ্যান্টিবায়োটিক সেবন করবেন না।"
        ),
        PregnancyChecklistItem(
            id = "pre_dua_istighfar",
            category = "PRE_CONCEPTION",
            trimester = 0,
            titleBn = "বেশি বেশি ইস্তিগফার ও নেক সন্তানের দোয়া",
            subtitleBn = "সূরা নূহ (১০–১২ আয়াত) অনুযায়ী নিয়মিত ইস্তিগফার ও কুরআনি দোয়া পাঠ করুন।"
        )
    )

    val pregnancyDosAndDonts: List<PregnancyChecklistItem> = listOf(
        // DOs (করণীয়)
        PregnancyChecklistItem(
            id = "do_supplements",
            category = "DO",
            trimester = 0,
            titleBn = "সময়মতো ভিটামিন, আয়রন ও ক্যালসিয়াম গ্রহণ",
            subtitleBn = "ডাক্তারের পরামর্শ অনুযায়ী প্রতিদিনের ফলিক অ্যাসিড, আয়রন ও ক্যালসিয়াম মিস করবেন না।"
        ),
        PregnancyChecklistItem(
            id = "do_hydration",
            category = "DO",
            trimester = 0,
            titleBn = "দিনে ৮–১০ গ্লাস পানি ও তরল খাবার পান",
            subtitleBn = "অ্যামনিওটিক ফ্লুইড ঠিক রাখতে ও পানিশূন্যতা রোধে পর্যাপ্ত পানি ও ডাবের পানি পান করুন।"
        ),
        PregnancyChecklistItem(
            id = "do_left_side_sleep",
            category = "DO",
            trimester = 0,
            titleBn = "বাম কাত হয়ে বিশ্রাম ও পর্যাপ্ত ঘুম",
            subtitleBn = "বাম কাত হয়ে শুলে গর্ভস্থ শিশুর কাছে রক্ত ও অক্সিজেন প্রবাহ সবচেয়ে ভালো থাকে।"
        ),
        PregnancyChecklistItem(
            id = "do_quran_adhkar",
            category = "DO",
            trimester = 0,
            titleBn = "কুরআন তিলাওয়াত ও সকাল-সন্ধ্যার যিকির",
            subtitleBn = "মায়ের প্রশান্তি ও গর্ভস্থ সন্তানের আত্মিক বিকাশের জন্য প্রতিদিন তিলাওয়াত শুনুন ও দোয়া পড়ুন।"
        ),
        PregnancyChecklistItem(
            id = "do_protein_dates",
            category = "DO",
            trimester = 0,
            titleBn = "প্রোটিন, দুধ, ডিম ও খেজুর গ্রহণ",
            subtitleBn = "শিশুর হাড় ও মস্তিষ্ক গঠনে প্রতিদিন দুধ, ডিম, মাছ/ডাল ও বাদাম খান।"
        ),
        PregnancyChecklistItem(
            id = "do_kick_count",
            category = "DO",
            trimester = 3,
            titleBn = "শিশুর নড়াচড়া (Kick Count) খেয়াল রাখুন",
            subtitleBn = "খাবারের পর শিশুর স্বাভাবিক নড়াচড়া লক্ষ্য করুন ও নিয়মিত চেকআপ করুন।"
        ),

        // DONTs (বর্জনীয়)
        PregnancyChecklistItem(
            id = "dont_heavy_lifting",
            category = "DONT",
            trimester = 0,
            titleBn = "ভারী জিনিস তোলা ও ঝুঁকে কাজ করা নিষেধ",
            subtitleBn = "পেটে চাপ পড়ে এমন কোনো ভারী বালতি বা আসবাবপত্র তুলবেন না।"
        ),
        PregnancyChecklistItem(
            id = "dont_raw_papaya_pineapple",
            category = "DONT",
            trimester = 0,
            titleBn = "কাঁচা পেঁপে, আনারস ও বাসি খাবার বর্জন",
            subtitleBn = "অতিরিক্ত ক্যাফেইন, কাঁচা পেঁপে, আধা-সেদ্ধ মাংস ও বাইরের খোলা খাবার এড়িয়ে চলুন।"
        ),
        PregnancyChecklistItem(
            id = "dont_self_medication",
            category = "DONT",
            trimester = 0,
            titleBn = "ডাক্তারের পরামর্শ ছাড়া ওষুধ খাওয়া নিষেধ",
            subtitleBn = "সাধারণ জ্বর বা ব্যথাতেও নিজের ইচ্ছায় কোনো ওষুধ বা এক্স-রে করাবেন না।"
        ),
        PregnancyChecklistItem(
            id = "dont_stress_late_night",
            category = "DONT",
            trimester = 0,
            titleBn = "অতিরিক্ত দুশ্চিন্তা ও রাত জাগা পরিহার",
            subtitleBn = "মায়ের মানসিক চাপ সরাসরি গর্ভস্থ শিশুর ওপর প্রভাব ফেলে; সবসময় আল্লাহর ওপর ভরসা রাখুন।"
        ),
        PregnancyChecklistItem(
            id = "dont_long_standing",
            category = "DONT",
            trimester = 0,
            titleBn = "দীর্ঘক্ষণ দাঁড়িয়ে থাকা ও ঝাঁকুনিযুক্ত ভ্রমণ নিষেধ",
            subtitleBn = "একটানা দাঁড়িয়ে কাজ না করে মাঝে মাঝে পা উঁচু করে বিশ্রাম নিন।"
        )
    )

    val newbornSunnahChecklist: List<PregnancyChecklistItem> = listOf(
        PregnancyChecklistItem(
            id = "sunnah_adhan",
            category = "NEWBORN_SUNNAH",
            trimester = 3,
            titleBn = "জন্মের পর নবজাতকের কানে আযান",
            subtitleBn = "শিশুর পৃথিবীতে প্রথম শ্রবণ যেন আল্লাহর মহত্ত্ব ও তাওহীদের বাণী হয়।"
        ),
        PregnancyChecklistItem(
            id = "sunnah_tahneek",
            category = "NEWBORN_SUNNAH",
            trimester = 3,
            titleBn = "তাহনীক করা (খেজুর চিবিয়ে মুখে ছোঁয়ানো)",
            subtitleBn = "নরম খেজুর সামান্য চিবিয়ে নবজাতকের তালুতে আলতো করে লাগিয়ে দেওয়া সুন্নাহ।"
        ),
        PregnancyChecklistItem(
            id = "sunnah_good_name",
            category = "NEWBORN_SUNNAH",
            trimester = 3,
            titleBn = "সুন্দর ও অর্থবহ ইসলামি নাম রাখা",
            subtitleBn = "সন্তানের জন্য নবী, সাহাবী বা আল্লাহর গুণবাচক নামের সাথে মিলিয়ে সুন্দর নাম নির্বাচন।"
        ),
        PregnancyChecklistItem(
            id = "sunnah_aqiqah",
            category = "NEWBORN_SUNNAH",
            trimester = 3,
            titleBn = "৭ম দিনে আকীকা ও চুল মুণ্ডন করে সদকা",
            subtitleBn = "জন্মের ৭ম দিনে আকীকা করা এবং মাথার চুলের সমপরিমাণ রুপার মূল্য সদকা করা সুন্নাহ।"
        )
    )

    val islamicAmalsAndDuas: List<IslamicPregnancyAmal> = listOf(
        IslamicPregnancyAmal(
            id = "dua_zakariya_imran",
            stageTag = "PRE",
            stageBadgeBn = "সন্তান লাভের দোয়া",
            titleBn = "হযরত যাকারিয়া (আ.)-এর উত্তম সন্তানের দোয়া",
            arabicText = "رَبِّ هَبْ لِي مِن لَّدُنكَ ذُرِّيَّةً طَيِّبَةً ۖ إِنَّكَ سَمِيعُ الدُّعَاءِ",
            pronunciationBn = "রাব্বি হাবলী মিল্লাদুনকা যুররিয়্যাতান ত্বায়্যিবাহ, ইন্নাকা সামীউদ দুআ।",
            meaningBn = "হে আমার রব! আমাকে আপনার পক্ষ থেকে পবিত্র ও উত্তম সন্তান দান করুন। নিশ্চয়ই আপনি প্রার্থনা শ্রবণকারী।",
            referenceBn = "সূরা আলে ইমরান: আয়াত ৩৮",
            benefitBn = "নেক, সুস্থ ও চরিত্রবান সন্তান লাভের জন্য স্বামী-স্ত্রী উভয়ে ফরয নামাযের পর ও সিজদায় এই দোয়া বেশি বেশি পাঠ করবেন।"
        ),
        IslamicPregnancyAmal(
            id = "dua_zakariya_anbiya",
            stageTag = "PRE",
            stageBadgeBn = "সন্তান লাভের দোয়া",
            titleBn = "নিঃসন্তান অবস্থায় হযরত যাকারিয়া (আ.)-এর দোয়া",
            arabicText = "رَبِّ لَا تَذَرْنِي فَرْدًا وَأَنتَ خَيْرُ الْوَارِثِينَ",
            pronunciationBn = "রাব্বি লা তাযারনী ফারদাও ওয়া আনতা খাইরুল ওয়ারিসীন।",
            meaningBn = "হে আমার রব! আমাকে একা (নিঃসন্তান) রেখো না, আর তুমিই তো সর্বোত্তম উত্তরাধিকারী।",
            referenceBn = "সূরা আল-আম্বিয়া: আয়াত ৮৯",
            benefitBn = "এই দোয়ার বরকতে আল্লাহ তাআলা বৃদ্ধ বয়সেও হযরত যাকারিয়া (আ.)-কে হযরত ইয়াহইয়া (আ.)-এর সুসংবাদ দিয়েছিলেন।"
        ),
        IslamicPregnancyAmal(
            id = "dua_ibrahim_saliheen",
            stageTag = "PRE",
            stageBadgeBn = "নেক সন্তানের দোয়া",
            titleBn = "হযরত ইবরাহীম (আ.)-এর দোয়া",
            arabicText = "رَبِّ هَبْ لِي مِنَ الصَّالِحِينَ",
            pronunciationBn = "রাব্বি হাবলী মিনাস সালিহীন।",
            meaningBn = "হে আমার রব! আমাকে নেককার ও সৎকর্মশীল সন্তান দান করুন।",
            referenceBn = "সূরা আস-সাফফাত: আয়াত ১০০",
            benefitBn = "সন্তান নেওয়ার পরিকল্পনার শুরু থেকেই নেক সন্তানের নিয়ত ও এই সংক্ষিপ্ত কুরআনি দোয়া পাঠ করা সুন্নাহ।"
        ),
        IslamicPregnancyAmal(
            id = "dua_maryam_mother",
            stageTag = "TRIMESTER_1",
            stageBadgeBn = "১ম ত্রৈমাসিক",
            titleBn = "গর্ভস্থ সন্তানের সুরক্ষা ও কবুলিয়াতের দোয়া",
            arabicText = "رَبِّ إِنِّي نَذَرْتُ لَكَ مَا فِي بَطْنِي مُحَرَّرًا فَتَقَبَّلْ مِنِّي ۖ إِنَّكَ أَنتَ السَّمِيعُ الْعَلِيمُ",
            pronunciationBn = "রাব্বি ইন্নী নাযারতু লাকা মা ফী বাতনী মুহাররারান ফাতাকাব্বাল মিন্নী, ইন্নাকা আনতাস সামীউল আলীম।",
            meaningBn = "হে আমার রব! আমার গর্ভে যা রয়েছে আমি তা একান্তভাবে আপনার জন্য উৎসর্গ করলাম; অতএব আমার পক্ষ থেকে তা কবুল করুন। নিশ্চয়ই আপনি সর্বশ্রোতা, সর্বজ্ঞ।",
            referenceBn = "সূরা আলে ইমরান: আয়াত ৩৫",
            benefitBn = "গর্ভধারণের প্রথম ৩ মাসে ভ্রূণের নিরাপদ গঠন এবং শয়তানের অনিষ্ট থেকে হিফাযতের জন্য সূরা ফাতিহা, আয়াতুল কুরসি ও এই আয়াত পাঠ করুন।"
        ),
        IslamicPregnancyAmal(
            id = "dua_furqan_qurrata_ayun",
            stageTag = "TRIMESTER_2",
            stageBadgeBn = "২য় ত্রৈমাসিক",
            titleBn = "চক্ষুশীতলকারী ও মুত্তাকী সন্তানের দোয়া",
            arabicText = "رَبَّنَا هَبْ لَنَا مِنْ أَزْوَاجِنَا وَذُرِّيَّاتِنَا قُرَّةَ أَعْيُنٍ وَاجْعَلْنَا لِلْمُتَّقِينَ إِمَامًا",
            pronunciationBn = "রাব্বানা হাবলানা মিন আযওয়াজিনা ওয়া যুররিয়্যাতিনা কুররাতা আইয়ুনিও ওয়াজআলনা লিল মুত্তাকীনা ইমামা।",
            meaningBn = "হে আমাদের রব! আমাদের জীবনসঙ্গী ও সন্তানদের আমাদের জন্য চক্ষুশীতলকারী বানিয়ে দিন এবং আমাদের মুত্তাকীদের আদর্শ করুন।",
            referenceBn = "সূরা আল-ফুরকান: আয়াত ৭৪",
            benefitBn = "২য় ত্রৈমাসিকে শিশুর শ্রবণশক্তি গঠিত হয়। এই সময়ে বেশি বেশি সূরা মারইয়াম, সূরা ইউসুফ ও এই দোয়া তিলাওয়াত করুন।"
        ),
        IslamicPregnancyAmal(
            id = "dua_ibrahim_namaz",
            stageTag = "TRIMESTER_2",
            stageBadgeBn = "২য় ত্রৈমাসিক",
            titleBn = "নামাযী ও অনুগত সন্তানের জন্য দোয়া",
            arabicText = "رَبِّ اجْعَلْنِي مُقِيمَ الصَّلَاةِ وَمِن ذُرِّيَّتِي ۚ رَبَّنَا وَتَقَبَّلْ دُعَاءِ",
            pronunciationBn = "রাব্বিজআলনী মুকীমাস সালাতি ওয়া মিন যুররিয়্যাতী রাব্বানা ওয়া তাকাব্বাল দুআ।",
            meaningBn = "হে আমার রব! আমাকে এবং আমার সন্তান-সন্ততিকে নামায কায়েমকারী বানিয়ে দিন; হে আমাদের রব, আমার দোয়া কবুল করুন।",
            referenceBn = "সূরা ইবরাহীম: আয়াত ৪০",
            benefitBn = "সন্তান যেন জন্মগতভাবেই দ্বীনদার ও নামাযের প্রতি অনুরাগী হয় সেজন্য গর্ভকালে এই দোয়া নিয়মিত পাঠ করুন।"
        ),
        IslamicPregnancyAmal(
            id = "dua_easy_delivery",
            stageTag = "TRIMESTER_3",
            stageBadgeBn = "৩য় ত্রৈমাসিক ও প্রসব",
            titleBn = "সহজ প্রসব ও কষ্ট লাঘবের দোয়া",
            arabicText = "اللَّهُمَّ لَا سَهْلَ إِلَّا مَا جَعَلْتَهُ سَهْلًا وَأَنْتَ تَجْعَلُ الْحَزْنَ إِذَا شِئْتَ سَهْلًا",
            pronunciationBn = "আল্লাহুম্মা লা সাহলা ইল্লা মা জাআলতাহু সাহলা, ওয়া আনতা তাজআলুল হাযনা ইযা শি’তা সাহলা।",
            meaningBn = "হে আল্লাহ! আপনি যা সহজ করে দেন তা ছাড়া কোনো কিছুই সহজ নয়, আর আপনি চাইলে কঠিন কাজকেও সহজ করে দেন।",
            referenceBn = "সহীহ ইবনে হিব্বান: ৯৭৪ (এছাড়া সূরা ইনশিকাক ১–৪ ও সূরা যিলযাল তিলাওয়াত)",
            benefitBn = "৩য় ত্রৈমাসিকে ও প্রসব বেদনার সময় ভয় দূর করতে এবং সহজ ডেলিভারির জন্য এই দোয়া ও সূরা ইনশিকাক পাঠ অত্যন্ত ফলপ্রসূ।"
        ),
        IslamicPregnancyAmal(
            id = "dua_newborn_protection",
            stageTag = "POST_BIRTH",
            stageBadgeBn = "জন্মের পর হিফাযত",
            titleBn = "নবজাতককে বদনজর থেকে রক্ষার সুন্নাহ দোয়া",
            arabicText = "أُعِيذُكَ بِكَلِمَاتِ اللَّهِ التَّامَّةِ مِنْ كُلِّ شَيْطَانٍ وَهَامَّةٍ وَمِنْ كُلِّ عَيْنٍ لَامَّةٍ",
            pronunciationBn = "উঈযুকা বিকালিমাতিল্লাহিত তাম্মাতি মিন কুল্লি শাইতানিও ওয়া হাম্মাহ, ওয়া মিন কুল্লি আইনিল লাম্মাহ।",
            meaningBn = "আমি আল্লাহর পরিপূর্ণ বাণীসমূহের মাধ্যমে তোমাকে সকল শয়তান, বিষাক্ত প্রাণী ও প্রত্যেক ক্ষতিকর বদনজর থেকে আশ্রয় দিচ্ছি।",
            referenceBn = "সহীহ বুখারী: ৩৩৭১",
            benefitBn = "রাসূলুল্লাহ (সা.) হাসান ও হুসাইন (রা.)-এর সুরক্ষার জন্য এই দোয়া পাঠ করতেন।"
        )
    )

    val emotionalAndMotivationalQuotes: List<PregnancyQuote> = listOf(
        PregnancyQuote(
            id = 1,
            tagBn = "হাদিসের সুসংবাদ",
            quoteBn = "“গর্ভধারণ থেকে শুরু করে সন্তান প্রসব ও দুধ পান করানো পর্যন্ত মায়ের প্রতিটি কষ্টের বিনিময়ে আল্লাহ তাআলা সীমান্ত পাহারাদার মুজাহিদের সমান নেকি দান করেন।”",
            sourceBn = "আল-মুজামুল আওসাত (তাবারানী)"
        ),
        PregnancyQuote(
            id = 2,
            tagBn = "আত্মিক প্রশান্তি",
            quoteBn = "“তোমার গর্ভে যে ছোট্ট প্রাণটি বেড়ে উঠছে, আল্লাহ নিজেই তার প্রতিটি হৃদস্পন্দন ও গঠন নিপুণভাবে তদারকি করছেন। দুশ্চিন্তা ছেড়ে তাঁর ওপর ভরসা রাখো।”",
            sourceBn = "সূরা আলে ইমরান: আয়াত ৬-এর অনুপ্রেরণা"
        ),
        PregnancyQuote(
            id = 3,
            tagBn = "মায়ের মর্যাদা",
            quoteBn = "“তার মা তাকে কষ্টের পর কষ্ট সহ্য করে গর্ভে ধারণ করেছে—তাই আল্লাহ স্বয়ং পিতা-মাতার কৃতজ্ঞতা আদায়ের নির্দেশ দিয়েছেন।”",
            sourceBn = "সূরা লুকমান: আয়াত ১৪"
        ),
        PregnancyQuote(
            id = 4,
            tagBn = "মানসিক শক্তি",
            quoteBn = "“গর্ভকালীন প্রতিটি ক্লান্তি, বমিভাব কিংবা নির্ঘুম রাত বৃথা যায় না; প্রতিটি মুহূর্তেই আপনার গুনাহ মাফ হচ্ছে এবং মর্যাদা বৃদ্ধি পাচ্ছে।”",
            sourceBn = "মাতৃত্বের অনুপ্রেরণা"
        ),
        PregnancyQuote(
            id = 5,
            tagBn = "ধৈর্য ও তাওয়াক্কুল",
            quoteBn = "“নিশ্চয়ই প্রতিটি কষ্টের সাথেই রয়েছে স্বস্তি। আজকের এই অপেক্ষা খুব শীঘ্রই একটি ফুটফুটে হাসিমুখে পূর্ণতা পাবে ইনশাআল্লাহ।”",
            sourceBn = "সূরা আল-ইনশিরাহ: আয়াত ৫–৬"
        ),
        PregnancyQuote(
            id = 6,
            tagBn = "ভ্রূণ সৃষ্টির বিস্ময়",
            quoteBn = "“অতঃপর আমি শুক্রবিন্দুকে জমাট রক্তে, তারপর গোশতপিণ্ডে ও অস্থিপিণ্ডে রূপান্তরিত করি এবং তাতে মাংস পরিয়ে নিপুণতম সৃষ্টিতে পূর্ণতা দেই।”",
            sourceBn = "সূরা আল-মুমিনুন: আয়াত ১৪"
        )
    )

    val reminderPresets: List<PregnancyReminderPreset> = listOf(
        PregnancyReminderPreset(
            id = "preg_folic_acid",
            stage = "PRE",
            emoji = "💊",
            titleBn = "ফলিক অ্যাসিড ও ভিটামিন",
            category = HabitScheduleCalculator.CATEGORY_MEDICATION,
            anchorTime = "21:00",
            preOffsetMins = 15,
            postOffsetMins = 30,
            messageBn = "রাতের খাবারের পর ফলিক অ্যাসিড ও ভিটামিন নেওয়ার সময় হয়েছে।"
        ),
        PregnancyReminderPreset(
            id = "preg_iron_calcium",
            stage = "POST",
            emoji = "💊",
            titleBn = "আয়রন ও ক্যালসিয়াম",
            category = HabitScheduleCalculator.CATEGORY_MEDICATION,
            anchorTime = "14:30",
            preOffsetMins = 15,
            postOffsetMins = 45,
            messageBn = "মা ও শিশুর সুস্থতায় আয়রন ও ক্যালসিয়াম গ্রহণ করুন।"
        ),
        PregnancyReminderPreset(
            id = "preg_milk_dates",
            stage = "POST",
            emoji = "🥛",
            titleBn = "দুধ, খেজুর ও পুষ্টি",
            category = HabitScheduleCalculator.CATEGORY_MEAL_LINKED,
            anchorTime = "08:30",
            preOffsetMins = 20,
            postOffsetMins = 40,
            messageBn = "সকালের নাস্তায় দুধ, খেজুর ও প্রোটিনসমৃদ্ধ খাবার গ্রহণ করুন।"
        ),
        PregnancyReminderPreset(
            id = "preg_dua_tilawat",
            stage = "POST",
            emoji = "🤲",
            titleBn = "মাতৃত্বের দোয়া ও তিলাওয়াত",
            category = HabitScheduleCalculator.CATEGORY_CUSTOM,
            anchorTime = "17:00",
            preOffsetMins = 10,
            postOffsetMins = 30,
            messageBn = "নীরবে বসে কুরআন তিলাওয়াত শুনুন এবং নেক সন্তানের দোয়া পড়ুন।"
        ),
        PregnancyReminderPreset(
            id = "preg_water_hydration",
            stage = "POST",
            emoji = "💧",
            titleBn = "গর্ভকালীন পানি পান",
            category = HabitScheduleCalculator.CATEGORY_HYDRATION,
            anchorTime = "11:30",
            preOffsetMins = 15,
            postOffsetMins = 60,
            messageBn = "পানিশূন্যতা রোধে এক গ্লাস বিশুদ্ধ পানি বা ডাবের পানি পান করুন।"
        )
    )

    val weeklyGrowthCatalog: List<WeeklyBabyGrowth> = (1..40).map { w ->
        when (w) {
            in 1..4 -> WeeklyBabyGrowth(
                week = w,
                emoji = "🌱",
                sizeComparisonBn = "পোস্ত দানা",
                lengthBn = "০.১ সে.মি.",
                weightBn = "১ গ্রামের কম",
                milestoneTitleBn = "প্রাণের প্রথম স্পন্দন ও রোপণ",
                developmentDetailBn = "নিষিক্ত ডিম্বাণু জরায়ুর দেয়ালে নিরাপদে প্রতিস্থাপিত হচ্ছে এবং প্লাসেন্টা ও অ্যামনিওটিক থলি গঠিত হচ্ছে।",
                spiritualReflectionBn = "আল্লাহ তাআলা একটি ক্ষুদ্র বিন্দু থেকে জরায়ুর নিরাপদ আশ্রয়ে নতুন প্রাণের সূচনা করেছেন (সূরা মুমিনুন: ১৩)।"
            )
            in 5..6 -> WeeklyBabyGrowth(
                week = w,
                emoji = "🫘",
                sizeComparisonBn = "তিল ও ডালিম দানা",
                lengthBn = "০.৬ সে.মি.",
                weightBn = "১ গ্রাম",
                milestoneTitleBn = "হৃদস্পন্দনের সূচনা ও স্নায়ুনালী",
                developmentDetailBn = "এই সপ্তাহে শিশুর ক্ষুদ্র হৃদপিণ্ড স্পন্দিত হতে শুরু করে এবং মস্তিষ্ক ও মেরুদণ্ডের প্রাথমিক গঠন শুরু হয়।",
                spiritualReflectionBn = "মায়ের অজান্তেই রবের হুকুমে ভ্রূণের বুকে প্রথম হৃদস্পন্দন শুরু হয়েছে—সুবহানাল্লাহ!"
            )
            in 7..8 -> WeeklyBabyGrowth(
                week = w,
                emoji = "🫐",
                sizeComparisonBn = "ছোট খেজুর",
                lengthBn = "১.৬ সে.মি.",
                weightBn = "২ গ্রাম",
                milestoneTitleBn = "হাত-পায়ের কুঁড়ি ও মুখাবয়ব",
                developmentDetailBn = "শিশুর ছোট হাত ও পায়ের আঙুলের কুঁড়ি, চোখ, নাক ও কানের প্রাথমিক আকৃতি স্পষ্ট হতে থাকে।",
                spiritualReflectionBn = "তিনিই তোমাকে সৃষ্টি করেছেন, অতঃপর সুঠাম ও সুসামঞ্জস্য করেছেন (সূরা ইনফিতার: ৭)।"
            )
            in 9..10 -> WeeklyBabyGrowth(
                week = w,
                emoji = "🫒",
                sizeComparisonBn = "জলপাই",
                lengthBn = "৩.১ সে.মি.",
                weightBn = "৪ গ্রাম",
                milestoneTitleBn = "প্রধান অঙ্গ-প্রত্যঙ্গের ভিত্তি",
                developmentDetailBn = "হৃদপিণ্ডের ৪টি প্রকোষ্ঠ গঠিত হয়েছে এবং কিডনি, লিভার ও মস্তিষ্ক দ্রুত কাজ শুরু করছে।",
                spiritualReflectionBn = "জরায়ুর অন্ধকারে আল্লাহ প্রতিটি অঙ্গকে নিখুঁত পরিমাণে বিন্যস্ত করছেন।"
            )
            in 11..12 -> WeeklyBabyGrowth(
                week = w,
                emoji = "🍋",
                sizeComparisonBn = "কাগজি লেবু",
                lengthBn = "৫.৪ সে.মি.",
                weightBn = "১৪ গ্রাম",
                milestoneTitleBn = "১ম ত্রৈমাসিক পূর্ণ ও হাত-পা নড়াচড়া",
                developmentDetailBn = "শিশু এখন হাত-পা ভাঁজ করতে পারে, আঙুলে নখ গজাচ্ছে এবং হাড়গুলো শক্ত হতে শুরু করেছে।",
                spiritualReflectionBn = "প্রথম ১২ সপ্তাহের গুরুত্বপূর্ণ ধাপ পূর্ণ হলো—আলহামদুলিল্লাহ।"
            )
            in 13..15 -> WeeklyBabyGrowth(
                week = w,
                emoji = "🍑",
                sizeComparisonBn = "পাকা ডুমুর / মাল্টা",
                lengthBn = "১০.১ সে.মি.",
                weightBn = "৭০ গ্রাম",
                milestoneTitleBn = "২য় ত্রৈমাসিক ও আঙুলের ছাপ গঠন",
                developmentDetailBn = "শিশুর আঙুলে অনন্য ফিঙ্গারপ্রিন্ট তৈরি হয়েছে, মুখের অভিব্যক্তি করতে পারে এবং আলোর সংবেদন বুঝতে শুরু করে।",
                spiritualReflectionBn = "আল্লাহ মানুষের আঙুলের অগ্রভাগও নিখুঁতভাবে বিন্যস্ত করতে সক্ষম (সূরা কিয়ামাহ: ৪)।"
            )
            in 16..18 -> WeeklyBabyGrowth(
                week = w,
                emoji = "🍐",
                sizeComparisonBn = "পেয়ারা / আপেল",
                lengthBn = "১৪.২ সে.মি.",
                weightBn = "১৯০ গ্রাম",
                milestoneTitleBn = "শ্রবণশক্তি ও প্রথম নড়াচড়ার অনুভূতি",
                developmentDetailBn = "শিশুর কানের হাড় গঠিত হওয়ায় সে এখন মায়ের হৃদস্পন্দন, কণ্ঠস্বর ও কুরআন তিলাওয়াত শুনতে পায়!",
                spiritualReflectionBn = "তিনিই তোমাদের জন্য শ্রবণশক্তি, দৃষ্টিশক্তি ও অন্তঃকরণ সৃষ্টি করেছেন (সূরা মুলক: ২৩)।"
            )
            in 19..21 -> WeeklyBabyGrowth(
                week = w,
                emoji = "🥭",
                sizeComparisonBn = "পাকা আম / কলা",
                lengthBn = "২৫.৬ সে.মি.",
                weightBn = "৩৬০ গ্রাম",
                milestoneTitleBn = "অর্ধেক পথ পূর্ণ ও রুহ সঞ্চারের বরকতময় সময়",
                developmentDetailBn = "গর্ভকালের অর্ধেক পথ পূর্ণ! শিশুর ঘুমানো ও জেগে ওঠার নির্দিষ্ট রুটিন তৈরি হচ্ছে এবং নড়াচড়া স্পষ্ট বোঝা যায়।",
                spiritualReflectionBn = "হাদিস অনুযায়ী ১২০ দিন (১৭–১৮ সপ্তাহ) পূর্ণ হলে ফেরেশতার মাধ্যমে রুহ ফুঁকে দেওয়া হয় ও তাকদীর লেখা হয়।"
            )
            in 22..24 -> WeeklyBabyGrowth(
                week = w,
                emoji = "🌽",
                sizeComparisonBn = "ভুট্টা / ডালিম",
                lengthBn = "৩০ সে.মি.",
                weightBn = "৬০০ গ্রাম",
                milestoneTitleBn = "ফুসফুসের বিকাশ ও কণ্ঠস্বর চেনা",
                developmentDetailBn = "শিশুর ফুসফুসের বায়ুথলি তৈরি হচ্ছে, চোখের পাতা ও ভ্রু গঠিত হয়েছে এবং মায়ের কণ্ঠে সাড়া দেয়।",
                spiritualReflectionBn = "মায়ের প্রতিটি দোয়া ও তিলাওয়াত শিশুর অন্তরে প্রশান্তির ছাপ ফেলে।"
            )
            in 25..27 -> WeeklyBabyGrowth(
                week = w,
                emoji = "🍈",
                sizeComparisonBn = "বেল / ফুলকপি",
                lengthBn = "৩৬.৬ সে.মি.",
                weightBn = "৯০০ গ্রাম",
                milestoneTitleBn = "চোখ খোলা ও মস্তিষ্কের দ্রুত বৃদ্ধি",
                developmentDetailBn = "শিশু এখন চোখের পাতা খুলতে ও বন্ধ করতে পারে এবং তার মস্তিষ্কের স্নায়ুকোষ দ্রুত সক্রিয় হচ্ছে।",
                spiritualReflectionBn = "২য় ত্রৈমাসিক সমাপ্তির পথে—আল্লাহ মা ও সন্তান উভয়কে নিরাপদ রাখুন।"
            )
            in 28..31 -> WeeklyBabyGrowth(
                week = w,
                emoji = "🥥",
                sizeComparisonBn = "ডাব",
                lengthBn = "৪১ সে.মি.",
                weightBn = "১.৫ কেজি",
                milestoneTitleBn = "৩য় ত্রৈমাসিক শুরু ও হাড় মজবুত হওয়া",
                developmentDetailBn = "শিশুর ওজন দ্রুত বাড়ছে, শরীরের তাপমাত্রা নিয়ন্ত্রণ ক্ষমতা তৈরি হচ্ছে এবং রোগ প্রতিরোধ ক্ষমতা বাড়ছে।",
                spiritualReflectionBn = "৩য় ত্রৈমাসিকে বেশি বেশি সূরা ইনশিকাক ও সহজ প্রসবের দোয়া পাঠ করুন।"
            )
            in 32..35 -> WeeklyBabyGrowth(
                week = w,
                emoji = "🍍",
                sizeComparisonBn = "আনারস / পাকা পেঁপে",
                lengthBn = "৪৬ সে.মি.",
                weightBn = "২.৪ কেজি",
                milestoneTitleBn = "ফুসফুস ও অঙ্গসমূহ প্রায় পরিপূর্ণ",
                developmentDetailBn = "শিশুর ত্বক মসৃণ হচ্ছে, নখ পূর্ণাঙ্গ হয়েছে এবং প্রসবের প্রস্তুতির জন্য মাথা নিচের দিকে ঘুরতে শুরু করে।",
                spiritualReflectionBn = "অপেক্ষার প্রহর ফুরিয়ে আসছে; ধৈর্য ও শুকরিয়ার সাথে প্রস্তুতি নিন।"
            )
            else -> WeeklyBabyGrowth(
                week = w,
                emoji = "👶",
                sizeComparisonBn = "পূর্ণাঙ্গ নবজাতক (তরমুজ)",
                lengthBn = "৫০ সে.মি.",
                weightBn = "৩.০ – ৩.৪ কেজি",
                milestoneTitleBn = "পৃথিবীতে আসার পূর্ণ প্রস্তুতি",
                developmentDetailBn = "শিশুর ফুসফুস ও সকল অঙ্গ পৃথিবীর আলো-বাতাসে শ্বাস নেওয়ার জন্য সম্পূর্ণ প্রস্তুত। যেকোনো দিন সুসংবাদ আসবে ইনশাআল্লাহ!",
                spiritualReflectionBn = "হে আল্লাহ! প্রসব সহজ করে দিন এবং সুস্থ ও চক্ষুশীতলকারী সন্তান দান করুন—আমীন।"
            )
        }
    }

    val postpartumBabyCareCatalog: List<PostpartumBabyCareTip> = listOf(
        PostpartumBabyCareTip(
            id = "baby_care_breastfeeding",
            category = "BREASTFEEDING",
            emoji = "🤱",
            titleBn = "শালদুধ ও শুধুমাত্র বুকের দুধ (Exclusive Breastfeeding)",
            detailBn = "জন্মের ১ ঘণ্টার মধ্যে শিশুকে মায়ের প্রথম শালদুধ (Colostrum) পান করান। প্রথম ৬ মাস শিশুকে এক ফোঁটা পানিও দেওয়া যাবে না; বুকের দুধেই শিশুর ক্ষুধা, তৃষ্ণা ও অ্যান্টিবডি রয়েছে।",
            highlightBn = "প্রতি ২-৩ ঘণ্টা পর পর অথবা বাচ্চার চাহিদামতো (On demand) দুধ পান করান।"
        ),
        PostpartumBabyCareTip(
            id = "baby_care_cord",
            category = "CORD_CARE",
            emoji = "🩹",
            titleBn = "নাভির যত্ন (Umbilical Cord Care)",
            detailBn = "নাভির গোড়ায় কোনো প্রকার তেল, হলুদ বা পাউডার লাগাবেন না। নাভি সবসময় শুকনো ও খোলা রাখুন যাতে বাতাস লাগে। ডায়াপার পরানোর সময় নাভির নিচে ভাজ করে রাখুন।",
            highlightBn = "নাভি থেকে দুর্গন্ধযুক্ত পানি বা রক্ত বের হলে দ্রুত শিশু বিশেষজ্ঞ দেখান।"
        ),
        PostpartumBabyCareTip(
            id = "baby_care_massage",
            category = "MASSAGE",
            emoji = "🧴",
            titleBn = "তেল মালিশ ও রোদ পোহানো (Massage & Sunlight)",
            detailBn = "সকাল ৮টা থেকে ৯টার হালকা রোদে শিশুকে ৫–১০ মিনিট রাখুন। খাঁটি সরিষার তেল বা অলিভ অয়েল দিয়ে হাত-পা ও পিঠে হালকা মালিশ করুন। চোখ ও মুখে তেল প্রবেশ করতে দেবেন না।",
            highlightBn = "তেল মালিশের ফলে হাড়ের গঠন শক্ত হয় ও শিশুর রক্ত সঞ্চালন বৃদ্ধি পায়।"
        ),
        PostpartumBabyCareTip(
            id = "baby_care_sleep",
            category = "SLEEP",
            emoji = "😴",
            titleBn = "ঘুমের রুটিন ও শোয়ানোর নিয়ম (Sleep Routine)",
            detailBn = "নবজাতকের দিনে ১৬ থেকে ১৮ ঘণ্টা ঘুমানো স্বাভাবিক। সবসময় শিশুকে চিত করে (পিঠের ওপর) সমতল নরম বিছানায় শোয়ান। মুখের কাছে ভারী কম্বল বা খেলনা রাখবেন না।",
            highlightBn = "দিনের বেলা মৃদু আলো ও স্বাভাবিক শব্দ রাখুন, রাতে অন্ধকার ও শান্ত পরিবেশ তৈরি করুন।"
        ),
        PostpartumBabyCareTip(
            id = "baby_care_colic",
            category = "COLIC",
            emoji = "💨",
            titleBn = "পেটে গ্যাস ও কান্না উপশম (Colic & Burping)",
            detailBn = "প্রতিবার দুধ খাওয়ানোর পর শিশুকে কাঁধের ওপর সোজা রেখে পিঠে নিচ থেকে উপরে আলতো চাপড় দিয়ে ঢেকুর (Burp) তোলান। পেটে গ্যাস হলে শিশুর পা সাইকেলের মতো আলতো নাড়াচাড়া করুন।",
            highlightBn = "দুধ পানের পর সাথে সাথে শোয়াবেন না; অন্তত ১০-১৫ মিনিট সোজা রাখুন।"
        ),
        PostpartumBabyCareTip(
            id = "baby_care_milestones",
            category = "MILESTONES",
            emoji = "🌟",
            titleBn = "নবজাতকের বিকাশ ও ইসলামিক সুন্নাহ (Sunnah & Milestones)",
            detailBn = "জন্মের পর ডান কানে আযান ও বাম কানে ইকামত দিন, মিষ্টি কিছু দিয়ে তাহনীক করুন এবং সপ্তম দিনে আক্বীকা ও মাথার চুল ওজনের সমপরিমাণ রুপা সদকা করুন।",
            highlightBn = "বাচ্চার সাথে মিষ্টি সুরে কথা বলুন ও কুরআন তিলাওয়াত শোনান।"
        )
    )

    val postpartumMotherCareCatalog: List<PostpartumMotherCareTip> = listOf(
        PostpartumMotherCareTip(
            id = "mother_recovery_diet",
            deliveryTypeTarget = "ALL",
            emoji = "🍲",
            titleBn = "প্রসব-পরবর্তী পুষ্টিকর রিকভারি খাদ্য",
            detailBn = "গরম ভাতের সাথে কালোজিরা ভর্তা, লাউ, কাঁচকলা, দেশি মুরগির পাতলা ঝোল ও পেঁপে খান। প্রতিদিন অন্তত ১ গ্লাস খাঁটি দুধ, ১টি ডিম ও প্রচুর পুষ্টিকর তরল স্যুপ গ্রহণ করুন।",
            highlightBn = "কালোজিরা ও রসুন জরায়ু দ্রুত সংকুচিত হতে ও দুধ বাড়াতে সাহায্য করে।"
        ),
        PostpartumMotherCareTip(
            id = "mother_galactagogues",
            deliveryTypeTarget = "ALL",
            emoji = "🥛",
            titleBn = "বুকের দুধ বাড়ানোর প্রাকৃতিক গ্যালাক্টাগগ খাবার",
            detailBn = "মেথি ভেজানো কুসুম গরম পানি, মৌরি বা জিরা চা, লাউয়ের তরকারি, রসুন ও মিষ্টি কুমড়া মায়ের বুকের দুধের প্রবাহ (Milk Supply) দ্বিগুণ করতে প্রমাণিত ভূমিকা রাখে।",
            highlightBn = "মায়ের শরীর হাইড্রেটেড রাখা সবচেয়ে জরুরি; প্রতিদিন ৮–১০ গ্লাস কুসুম গরম পানি পান করুন।"
        ),
        PostpartumMotherCareTip(
            id = "mother_normal_healing",
            deliveryTypeTarget = "NORMAL",
            emoji = "🚶‍♀️",
            titleBn = "নরমাল ডেলিভারির রিকভারি ও পেরিনিয়াল কেয়ার",
            detailBn = "প্রতিবার ওয়াশরুম ব্যবহারের পর কুসুম গরম পানি দিয়ে পরিষ্কার করুন। হালকা কুসুম গরম পানিতে লবণ দিয়ে সিটজ বাথ (Sitz bath) নিলে ব্যথা কমে ও জখম দ্রুত শুকায়।",
            highlightBn = "ভারী জিনিস তোলা এড়িয়ে চলুন এবং দিনে অন্তত ২ বার শুয়ে বিশ্রাম নিন।"
        ),
        PostpartumMotherCareTip(
            id = "mother_csection_care",
            deliveryTypeTarget = "C_SECTION",
            emoji = "🩹",
            titleBn = "সিজারিয়ান কাটার যত্ন ও সতর্কতা",
            detailBn = "সেলাইয়ের জায়গা সম্পূর্ণ শুকনো ও পরিষ্কার রাখুন। ডাক্তার অনুমতি না দেওয়া পর্যন্ত পেটে পানি লাগাবেন না। কাশি বা হাঁচির সময় পেটে বালিশ চেপে ধরুন যাতে টান না লাগে।",
            highlightBn = "প্রোটিন সমৃদ্ধ খাবার (ডিম, মুরগি, ডাল) সেলাইয়ের স্থান দ্রুত নিরাময়ে সাহায্য করে।"
        ),
        PostpartumMotherCareTip(
            id = "mother_mental_blues",
            deliveryTypeTarget = "ALL",
            emoji = "🌸",
            titleBn = "মানসিক স্বাস্থ্য ও পোস্টপার্টাম ব্লুজ (Mental Wellbeing)",
            detailBn = "হরমোনের পরিবর্তনের কারণে প্রসবের পর মন খারাপ, কান্না পাওয়া বা ক্লান্তি লাগা স্বাভাবিক। নিজের অনুভূতি স্বামীর সাথে ভাগ করুন এবং একাকী থাকবেন না।",
            highlightBn = "পর্যাপ্ত ঘুম নিশ্চিত করতে বাচ্চা ঘুমালেই নিজেও বিশ্রাম নেওয়ার চেষ্টা করুন।"
        )
    )

    val stageFatherTipsCatalog: List<StageFatherTip> = listOf(
        StageFatherTip(
            id = "father_pre_conception",
            stage = "PRE",
            emoji = "🍯",
            badgeBn = "সন্তান নেওয়ার প্রস্তুতি",
            titleBn = "স্বামীর পুষ্টি, ফার্টিলিটি ও মানসিক সঙ্গ",
            detailBn = "পুরুষের শুক্রাণুর মান উন্নত রাখতে নিয়মিত মধু, কালোজিরা, কাঠবাদাম ও দুধ খান। ধূমপান ও অতিরিক্ত রাত জাগা পরিহার করুন। স্ত্রীর উর্বর দিনগুলোতে তাকে মানসিকভাবে শান্ত ও প্রফুল্ল রাখুন।",
            actionBn = "স্ত্রীর সাইকেল ও উর্বর দিন মনে রেখে চিকিৎসকের পরামর্শে ফলিক অ্যাসিড গ্রহণে উৎসাহ দিন।"
        ),
        StageFatherTip(
            id = "father_pregnancy_support",
            stage = "PREGNANT",
            emoji = "🤰",
            badgeBn = "গর্ভকালীন সহযোগিতা",
            titleBn = "স্ত্রীর প্রতি সহানুভূতি, সেবা ও দায়িত্বশীলতা",
            detailBn = "বমিভাব ও ক্লান্তির সময়ে গৃহস্থালির ভারী কাজে হাত লাগান। স্ত্রীর মুড সুইংয়ে ধৈর্যশীল হোন। প্রতিটি ডক্টর ভিজিটে সাথে যান এবং চিকিৎসকের নির্দেশিকা নোট করে রাখুন।",
            actionBn = "প্রতিদিন স্ত্রীকে নিয়ে কিছুক্ষণ সান্ধ্যকালীন হাঁটাচলা করুন ও সূরা মারিয়াম তিলাওয়াত করুন।"
        ),
        StageFatherTip(
            id = "father_postpartum_baby",
            stage = "POSTPARTUM",
            emoji = "👶",
            badgeBn = "নতুন বাবার ভূমিকা",
            titleBn = "মায়ের রিকভারিতে সহযোগিতা ও নবজাতকের বন্ধন",
            detailBn = "মায়ের ঘুমানোর জন্য রাতের বেলা বাচ্চার ডায়াপার পরিবর্তন ও কান্না থামাতে কোলে নিন। সিজারিয়ান বা নরমাল ডেলিভারির পর স্ত্রীর ব্যথার সময় ওষুধ ও গরম স্যুপ এগিয়ে দিন।",
            actionBn = "শিশুর চোখে চোখ রেখে কথা বলুন ও কোলে নিয়ে গায়ে হাত বুলিয়ে বাবার গভীর বন্ধন গড়ে তুলুন।"
        )
    )

    val stageSafetyWarningsCatalog: List<StageSafetyWarning> = listOf(
        StageSafetyWarning(
            id = "safety_pre_foods",
            stage = "PRE",
            isRedFlag = false,
            emoji = "🚫",
            titleBn = "গর্ভধারণের আগে বর্জনীয় খাবার ও অভ্যাস",
            detailBn = "অতিরিক্ত চা-কফি (ক্যাফেইন), কোমল পানীয়, কাঁচা পেঁপে ও ধূমপান জরায়ুর পরিবেশ ও ডিম্বাণুর কার্যক্ষমতা নষ্ট করতে পারে।"
        ),
        StageSafetyWarning(
            id = "safety_preg_foods",
            stage = "PREGNANT",
            isRedFlag = false,
            emoji = "⚠️",
            titleBn = "গর্ভাবস্থায় এড়িয়ে চলার মতো খাবার",
            detailBn = "কাঁচা বা আধপাকা পেঁপে, অতিরিক্ত আনারস, অপাস্তুরিত কাঁচা দুধ ও পনির, অতিরিক্ত সাজনা পাতা এবং চিকিৎসকের পরামর্শ ছাড়া যেকোনো ওষুধ সেবন বর্জন করুন।"
        ),
        StageSafetyWarning(
            id = "safety_preg_redflags",
            stage = "PREGNANT",
            isRedFlag = true,
            emoji = "🚨",
            titleBn = "গর্ভকালীন ডেঞ্জার সাইন (জরুরি চিকিৎসকের শরণাপন্ন হোন)",
            detailBn = "যোনিপথে যেকোনো রক্তপাত, তীব্র তলপেট বা পিঠ ব্যথা, হঠাৎ পানি ভাঙা, প্রচণ্ড মাথাব্যথা ও চোখে ঝাপসা দেখা, বাচ্চার নড়াচড়া কমে যাওয়া বা তীব্র জ্বর হলে দেরি না করে দ্রুত হাসপাতালে যান।"
        ),
        StageSafetyWarning(
            id = "safety_post_cautions",
            stage = "POSTPARTUM",
            isRedFlag = true,
            emoji = "🚨",
            titleBn = "প্রসব-পরবর্তী বিপজ্জনক লক্ষণ",
            detailBn = "অতিরিক্ত রক্তক্ষরণ (প্রতি ঘণ্টায় ১টির বেশি প্যাড ভেজা), তীব্র দুর্গন্ধযুক্ত স্রাব, উচ্চ রক্তচাপ, তীব্র শ্বাসকষ্ট বা বাচ্চার দুধ না টানা ও শরীর নীল হয়ে যাওয়ার লক্ষণ দেখামাত্র ডাক্তার দেখান।"
        )
    )

    val instantHomeRemediesCatalog: List<InstantHomeRemedy> = listOf(
        InstantHomeRemedy(
            id = "remedy_nausea",
            symptomId = "nausea",
            emoji = "🤢",
            titleBn = "বমিভাবের তাৎক্ষণিক ঘরোয়া সমাধান",
            remedyListBn = listOf(
                "সকালে ওঠার পর মুখে এক টুকরো আদা বা হালকা আদা-লেবু পানি দিয়ে মুখ কুলি করুন।",
                "খালি পেটে না থেকে শুকনা মুড়ি বা টোস্ট বিস্কুট অল্প অল্প করে চিবিয়ে খান।",
                "তাজা লেবুর খোসার গন্ধ শুকলে বমিভাব দ্রুত কমে যায়।"
            ),
            cautionBn = "দিনে ৩ বারের বেশি বমি হলে বা পানিশূন্যতা দেখা দিলে চিকিৎসকের পরামর্শ নিন।"
        ),
        InstantHomeRemedy(
            id = "remedy_fatigue",
            symptomId = "fatigue",
            emoji = "😴",
            titleBn = "ক্লান্তি ও দুর্বলতা দূর করার প্রাকৃতিক উপায়",
            remedyListBn = listOf(
                "২টি খোরমা বা আজওয়া খেজুর চিবিয়ে এক গ্লাস কুসুম গরম দুধ পান করুন।",
                "এক গ্লাস ডাবের পানি শরীরে প্রাকৃতিক ইলেক্ট্রোলাইট ভারসাম্য বজায় রাখে।",
                "দিনের বেলা বাম কাত হয়ে অন্তত ৪০–৫০ মিনিট বিশ্রাম নিন।"
            ),
            cautionBn = "অতিরিক্ত ফ্যাকাশে ভাব হলে রক্তে হিমোগ্লোবিন পরীক্ষা করুন।"
        ),
        InstantHomeRemedy(
            id = "remedy_dizziness",
            symptomId = "dizziness",
            emoji = "💫",
            titleBn = "মাথা ঘোরা ও রক্তচাপ নিয়ন্ত্রণের উপায়",
            remedyListBn = listOf(
                "বসা বা শোয়া থেকে ওঠার সময় ধীরে ধীরে সময় নিয়ে উঠুন।",
                "এক গ্লাস পানিতে আধা চা চামচ মধু ও এক চিমটি লবণ মিশিয়ে পান করুন।",
                "পর্যাপ্ত পানি পান করুন (প্রতিদিন অন্তত আড়াই থেকে তিন লিটার)।"
            ),
            cautionBn = "মাথা ঘোরার সাথে চোখে ঝাপসা দেখলে প্রেসার মেপে ডাক্তার দেখান।"
        ),
        InstantHomeRemedy(
            id = "remedy_back_pain",
            symptomId = "back_pain",
            emoji = "🦴",
            titleBn = "কোমর ও পিঠের ব্যথায় আরাম পাওয়ার কৌশল",
            remedyListBn = listOf(
                "ঘুমানোর সময় দুই হাঁটুর মাঝে একটি নরম বালিশ রেখে বাম কাতে ঘুমান।",
                "হালকা কুসুম গরম সরিষার তেল পিঠের নিচের দিকে মালিশ করুন।",
                "নিচু হয়ে ভারী জিনিস তুলবেন না; হাঁটু ভেঙে সোজা হয়ে বসুন।"
            ),
            cautionBn = "ব্যথা তলপেট বা উরুর দিকে ছড়িয়ে পড়লে পর্যবেক্ষণ করুন।"
        ),
        InstantHomeRemedy(
            id = "remedy_heartburn",
            symptomId = "heartburn",
            emoji = "🔥",
            titleBn = "বুক জ্বালাপোড়া ও অ্যাসিডিটির সমাধান",
            remedyListBn = listOf(
                "১ গ্লাস ঠান্ডা দুধ ধীরে ধীরে চুমুক দিয়ে পান করুন; এটি পাকস্থলীর এসিড শান্ত করে।",
                "খাবারের পর এক চিমটি মৌরি বা সবুজ এলাচ মুখে রেখে চিবান।",
                "রাতে খাওয়ার অন্তত ২ ঘণ্টা পর মাথা ও বুক সামান্য উঁচু করে ঘুমান।"
            ),
            cautionBn = "ভাজাপোড়া, অতিরিক্ত ঝাল ও টক খাবার রাতের বেলা সম্পূর্ণ এড়িয়ে চলুন।"
        ),
        InstantHomeRemedy(
            id = "remedy_cramps",
            symptomId = "cramps",
            emoji = "⚡",
            titleBn = "তলপেটে টান ও পেশির খিঁচুনি দূরীকরণ",
            remedyListBn = listOf(
                "কুসুম গরম পানি দিয়ে গোসল করুন বা পায়ে হালকা গরম সেঁক দিন।",
                "কলা ও বাদাম খান—এতে থাকা পটাশিয়াম ও ম্যাগনেশিয়াম পেশির টান কমায়।",
                "পায়ের পাতা সামনের দিকে টেনে স্ট্রেচিং করুন।"
            ),
            cautionBn = "তলপেটে নিয়মিত বিরতিতে তীব্র সংকোচন বা রক্তপাত দেখা দিলে দ্রুত হাসপাতালে যান।"
        )
    )

    val fullMonthNamesBn = listOf(
        "জানুয়ারি", "ফেব্রুয়ারি", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টেম্বর", "অক্টোবর", "নভেম্বর", "ডিসেম্বর"
    )

    val shortMonthNamesBn = listOf(
        "জানু", "ফেব্রু", "মার্চ", "এপ্রিল", "মে", "জুন",
        "জুলাই", "আগস্ট", "সেপ্টে", "অক্টো", "নভে", "ডিসে"
    )

    private fun daysBetweenClean(startMillis: Long, endMillis: Long): Int {
        val cleanA = HabitDateAndReportEngine.startOfDayMillis(startMillis)
        val cleanB = HabitDateAndReportEngine.startOfDayMillis(endMillis)
        return ((cleanB - cleanA + 43_200_000L) / DAY_MILLIS).toInt()
    }

    /**
     * Builds a comprehensive Menstruation (Haiz) Monthly Report for any user-selected
     * [year] and [monthZeroBased] (0 = Jan .. 11 = Dec).
     */
    fun buildMonthlyHaizReport(
        year: Int,
        monthZeroBased: Int,
        history: List<PeriodCycleEntry>,
        averageCycleDays: Int = 28,
        todayMillis: Long = System.currentTimeMillis()
    ): MonthlyHaizReport {
        val safeMonth = monthZeroBased.coerceIn(0, 11)
        val safeCycle = averageCycleDays.coerceIn(18, 60)
        val cleanToday = HabitDateAndReportEngine.startOfDayMillis(todayMillis)

        val monthCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, safeMonth)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val monthStartMillis = monthCal.timeInMillis
        val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val monthEndMillis = HabitDateAndReportEngine.addDays(monthStartMillis, daysInMonth - 1)
        // Sat(7)%7=0, Sun(1)%7=1, Mon(2)%7=2, Tue(3)%7=3, Wed(4)%7=4, Thu(5)%7=5, Fri(6)%7=6
        val leadingEmptyCount = monthCal.get(Calendar.DAY_OF_WEEK) % 7

        val monthYearTitleBn = "${fullMonthNamesBn[safeMonth]} ${HabitScheduleCalculator.toBengaliDigits(year.toString())}"
        val shortMonthLabelBn = shortMonthNamesBn[safeMonth]

        val sortedHistory = history
            .filter { it.startMillis > 0L }
            .map { entry ->
                val cleanStart = HabitDateAndReportEngine.startOfDayMillis(entry.startMillis)
                val cleanEnd = if (entry.endMillis >= cleanStart) {
                    HabitDateAndReportEngine.startOfDayMillis(entry.endMillis)
                } else {
                    0L
                }
                entry.copy(startMillis = cleanStart, endMillis = cleanEnd)
            }
            .sortedBy { it.startMillis }

        fun effectiveEndOf(entry: PeriodCycleEntry): Long {
            return if (entry.endMillis >= entry.startMillis) entry.endMillis else entry.startMillis
        }

        val entriesInMonth = sortedHistory.filter { entry ->
            val effEnd = effectiveEndOf(entry)
            entry.startMillis <= monthEndMillis && effEnd >= monthStartMillis
        }

        val primaryEntry = entriesInMonth.lastOrNull { it.startMillis in monthStartMillis..monthEndMillis }
            ?: entriesInMonth.lastOrNull()

        val actualDayMap = mutableMapOf<Long, Int>() // dayStartMillis -> haiz day index (1..N)
        val projectedDayMap = mutableMapOf<Long, Int>()
        val fertileSet = mutableSetOf<Long>()
        val ovulationSet = mutableSetOf<Long>()

        // Populate actual period days overlapping this month
        entriesInMonth.forEach { entry ->
            val effEnd = effectiveEndOf(entry)
            val loopStart = maxOf(entry.startMillis, monthStartMillis)
            val loopEnd = minOf(effEnd, monthEndMillis)
            var cursor = loopStart
            while (cursor <= loopEnd) {
                val haizDayNum = (daysBetweenClean(entry.startMillis, cursor) + 1).coerceAtLeast(1)
                actualDayMap[cursor] = haizDayNum
                cursor = HabitDateAndReportEngine.addDays(cursor, 1)
            }
        }

        // Compute cycle gap & fertile/ovulation windows for logged entries touching this month or preceding month
        var cycleGapDays = safeCycle
        var fertileStartMillis = 0L
        var fertileEndMillis = 0L
        var ovulationMillis = 0L

        if (primaryEntry != null) {
            val idx = sortedHistory.indexOf(primaryEntry)
            if (idx > 0) {
                val prev = sortedHistory[idx - 1]
                val observedGap = daysBetweenClean(prev.startMillis, primaryEntry.startMillis)
                if (observedGap in 18..60) {
                    cycleGapDays = observedGap
                }
            }
            val nextPeriod = HabitDateAndReportEngine.addDays(primaryEntry.startMillis, cycleGapDays)
            ovulationMillis = HabitDateAndReportEngine.addDays(nextPeriod, -14)
            fertileStartMillis = HabitDateAndReportEngine.addDays(ovulationMillis, -5)
            fertileEndMillis = HabitDateAndReportEngine.addDays(ovulationMillis, 1)
        }

        // Mark fertile & ovulation days for all actual entries that might project into this month
        sortedHistory.forEachIndexed { idx, entry ->
            val gap = if (idx > 0) {
                val g = daysBetweenClean(sortedHistory[idx - 1].startMillis, entry.startMillis)
                if (g in 18..60) g else safeCycle
            } else {
                safeCycle
            }
            val nextP = HabitDateAndReportEngine.addDays(entry.startMillis, gap)
            val ov = HabitDateAndReportEngine.addDays(nextP, -14)
            val fertS = HabitDateAndReportEngine.addDays(ov, -5)
            val fertE = HabitDateAndReportEngine.addDays(ov, 1)
            if (ov in monthStartMillis..monthEndMillis) {
                ovulationSet.add(ov)
            }
            var fc = maxOf(fertS, monthStartMillis)
            val fEnd = minOf(fertE, monthEndMillis)
            while (fc <= fEnd) {
                fertileSet.add(fc)
                fc = HabitDateAndReportEngine.addDays(fc, 1)
            }
        }

        // Projection logic when no actual entry exists in the selected month
        var isProjected = false
        var projectedStart = 0L
        var projectedEnd = 0L
        var projectedDuration = 0

        if (primaryEntry == null) {
            val latestBefore = sortedHistory.lastOrNull { it.startMillis < monthStartMillis }
            if (latestBefore != null) {
                val baseDuration = if (latestBefore.endMillis >= latestBefore.startMillis) {
                    (daysBetweenClean(latestBefore.startMillis, latestBefore.endMillis) + 1).coerceIn(3, 7)
                } else {
                    5
                }
                var cursorStart = HabitDateAndReportEngine.addDays(latestBefore.startMillis, safeCycle)
                var guard = 0
                while (cursorStart <= monthEndMillis && guard < 120) {
                    val cursorEnd = HabitDateAndReportEngine.addDays(cursorStart, baseDuration - 1)
                    val nextP = HabitDateAndReportEngine.addDays(cursorStart, safeCycle)
                    val ov = HabitDateAndReportEngine.addDays(nextP, -14)
                    val fertS = HabitDateAndReportEngine.addDays(ov, -5)
                    val fertE = HabitDateAndReportEngine.addDays(ov, 1)

                    if (ov in monthStartMillis..monthEndMillis) {
                        ovulationSet.add(ov)
                    }
                    var fc = maxOf(fertS, monthStartMillis)
                    val fEnd = minOf(fertE, monthEndMillis)
                    while (fc <= fEnd) {
                        fertileSet.add(fc)
                        fc = HabitDateAndReportEngine.addDays(fc, 1)
                    }

                    if (cursorStart <= monthEndMillis && cursorEnd >= monthStartMillis) {
                        isProjected = true
                        projectedStart = cursorStart
                        projectedEnd = cursorEnd
                        projectedDuration = baseDuration
                        if (ovulationMillis == 0L) {
                            ovulationMillis = ov
                            fertileStartMillis = fertS
                            fertileEndMillis = fertE
                        }
                        var pc = maxOf(cursorStart, monthStartMillis)
                        val pEnd = minOf(cursorEnd, monthEndMillis)
                        while (pc <= pEnd) {
                            projectedDayMap[pc] = (daysBetweenClean(cursorStart, pc) + 1).coerceAtLeast(1)
                            pc = HabitDateAndReportEngine.addDays(pc, 1)
                        }
                    }

                    cursorStart = nextP
                    guard++
                }
            }
        }

        // Build calendar cells
        val calendarCells = (1..daysInMonth).map { day ->
            val dayMillis = HabitDateAndReportEngine.addDays(monthStartMillis, day - 1)
            val (type, haizDayNum) = when {
                dayMillis in actualDayMap -> HaizCalendarDayType.PERIOD_ACTUAL to (actualDayMap[dayMillis] ?: 1)
                dayMillis in projectedDayMap -> HaizCalendarDayType.PERIOD_PROJECTED to (projectedDayMap[dayMillis] ?: 1)
                dayMillis in ovulationSet -> HaizCalendarDayType.OVULATION to 0
                dayMillis in fertileSet -> HaizCalendarDayType.FERTILE to 0
                else -> HaizCalendarDayType.NORMAL to 0
            }
            HaizCalendarCell(
                dayOfMonth = day,
                dateMillis = dayMillis,
                type = type,
                haizDayNumber = haizDayNum,
                isToday = dayMillis == cleanToday
            )
        }

        val hasActualLog = primaryEntry != null
        val reportStart = primaryEntry?.startMillis ?: projectedStart
        val reportEnd = if (primaryEntry != null) effectiveEndOf(primaryEntry) else projectedEnd
        val durationDays = when {
            primaryEntry != null -> (daysBetweenClean(primaryEntry.startMillis, effectiveEndOf(primaryEntry)) + 1).coerceAtLeast(1)
            isProjected -> projectedDuration
            else -> 0
        }

        val periodDateRangeBn = when {
            reportStart > 0L && reportEnd > reportStart ->
                "${HabitDateAndReportEngine.formatShortDateBn(reportStart)} – ${HabitDateAndReportEngine.formatShortDateBn(reportEnd)}"
            reportStart > 0L -> HabitDateAndReportEngine.formatShortDateBn(reportStart)
            else -> "লগ করা হয়নি"
        }

        val fertileRangeBn = if (fertileStartMillis > 0L && fertileEndMillis >= fertileStartMillis) {
            "${HabitDateAndReportEngine.formatShortDateBn(fertileStartMillis)} – ${HabitDateAndReportEngine.formatShortDateBn(fertileEndMillis)}"
        } else {
            "—"
        }

        val ovulationDateBn = if (ovulationMillis > 0L) {
            HabitDateAndReportEngine.formatShortDateBn(ovulationMillis)
        } else {
            "—"
        }

        val (noticeTitle, noticeDetail, isWarning) = when {
            hasActualLog && durationDays < 3 -> Triple(
                "💡 স্বল্প স্থায়িত্ব ও ফেকাহ সচেতনতা",
                "এই মাসে স্থায়িত্বকাল ৩ দিনের কম (${HabitScheduleCalculator.toBengaliDigits(durationDays.toString())} দিন)। পুষ্টিহীনতা এড়াতে ডালিম, খেজুর ও আয়রনসমৃদ্ধ খাবার নিন। ইসলামী ফেকাহ মতে সর্বনিম্ন হায়েজ ৩ দিন (৭২ ঘণ্টা); ৩ দিনের কম রক্তস্রাব সাধারণত ইস্তিহাযা হিসেবে গণ্য হতে পারে।",
                true
            )
            hasActualLog && durationDays > 10 -> Triple(
                "⚠️ অতিরিক্ত রক্তক্ষরণ ও জরুরি পরামর্শ",
                "স্থায়িত্বকাল ১০ দিনের বেশি (${HabitScheduleCalculator.toBengaliDigits(durationDays.toString())} দিন)। দ্রুত গাইনি চিকিৎসকের পরামর্শ নিন ও পর্যাপ্ত বিশ্রাম-আয়রন গ্রহণ করুন। ইসলামী ফেকাহ অনুযায়ী ১০ দিনের অতিরিক্ত সময় সাধারণত ইস্তিহাযা হিসেবে গণ্য হয়।",
                true
            )
            hasActualLog && durationDays > 7 -> Triple(
                "⚠️ ৭ দিনের বেশি স্থায়িত্ব পর্যবেক্ষণ",
                "মাসিক ৭ দিনের বেশি (${HabitScheduleCalculator.toBengaliDigits(durationDays.toString())} দিন) চলেছে। শরীর দুর্বল লাগলে আয়রনসমৃদ্ধ খাবার নিন ও চিকিৎসকের পরামর্শ বিবেচনা করুন (ফেকাহ মতে সর্বোচ্চ হায়েজ ১০ দিন পর্যন্ত হতে পারে)।",
                true
            )
            hasActualLog -> Triple(
                "🌿 স্বাভাবিক হায়েজ ও স্বাস্থ্য রিপোর্ট",
                "এই মাসে আপনার মাসিকের স্থায়িত্বকাল (${HabitScheduleCalculator.toBengaliDigits(durationDays.toString())} দিন) সম্পূর্ণ স্বাভাবিক ও স্বাস্থ্যসম্মত সীমার (৩–৭ দিন) মধ্যে রয়েছে।",
                false
            )
            isProjected -> Triple(
                "🔮 পূর্ববর্তী চক্র অনুযায়ী সম্ভাব্য পূর্বাভাস",
                "আপনার গড় ${HabitScheduleCalculator.toBengaliDigits(safeCycle.toString())} দিনের চক্র অনুযায়ী এই মাসের সম্ভাব্য তারিখ দেখানো হয়েছে। আসল মাসিক শুরু হলে তারিখ লগ করে রাখুন।",
                false
            )
            else -> Triple(
                "ℹ️ এখনো কোনো তথ্য লগ করা হয়নি",
                "এই মাসের মাসিক রিপোর্ট সংরক্ষণ করতে নিচের \"এই মাসের তথ্য লগ/হালনাগাদ\" বাটনে ট্যাপ করুন।",
                false
            )
        }

        return MonthlyHaizReport(
            year = year,
            monthZeroBased = safeMonth,
            monthYearTitleBn = monthYearTitleBn,
            shortMonthLabelBn = shortMonthLabelBn,
            hasActualLog = hasActualLog,
            isProjected = isProjected,
            activeEntry = primaryEntry,
            entriesInMonth = entriesInMonth,
            periodStartMillis = reportStart,
            periodEndMillis = reportEnd,
            periodDateRangeBn = periodDateRangeBn,
            durationDays = durationDays,
            cycleGapDays = if (hasActualLog || isProjected) cycleGapDays else 0,
            fertileStartMillis = fertileStartMillis,
            fertileEndMillis = fertileEndMillis,
            fertileRangeBn = fertileRangeBn,
            ovulationMillis = ovulationMillis,
            ovulationDateBn = ovulationDateBn,
            flowLevelBn = primaryEntry?.flowLevel?.ifBlank { "মাঝারি (স্বাভাবিক)" }
                ?: if (isProjected) "সম্ভাব্য (পূর্বাভাস)" else "—",
            noteBn = primaryEntry?.note ?: "",
            healthNoticeTitleBn = noticeTitle,
            healthNoticeDetailBn = noticeDetail,
            isWarningNotice = isWarning,
            calendarLeadingEmptyCount = leadingEmptyCount,
            calendarCells = calendarCells
        )
    }

    /**
     * Generates 12 monthly summary bars for a selected [year] so users can visually compare
     * period duration and cycle regularity across all months of the year.
     */
    fun buildYearlyHaizTrend(
        year: Int,
        history: List<PeriodCycleEntry>,
        averageCycleDays: Int = 28,
        todayMillis: Long = System.currentTimeMillis()
    ): List<YearlyHaizMonthBar> {
        return (0..11).map { m ->
            val monthReport = buildMonthlyHaizReport(
                year = year,
                monthZeroBased = m,
                history = history,
                averageCycleDays = averageCycleDays,
                todayMillis = todayMillis
            )
            YearlyHaizMonthBar(
                monthZeroBased = m,
                shortLabelBn = shortMonthNamesBn[m],
                fullLabelBn = fullMonthNamesBn[m],
                durationDays = monthReport.durationDays,
                cycleGapDays = monthReport.cycleGapDays,
                hasActualLog = monthReport.hasActualLog,
                isProjected = monthReport.isProjected,
                isWarningDuration = monthReport.hasActualLog && (monthReport.durationDays < 3 || monthReport.durationDays > 7)
            )
        }
    }
}
