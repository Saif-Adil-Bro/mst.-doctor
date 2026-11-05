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
    val startDateFormattedBn: String,
    val eddDateFormattedBn: String,
    val elapsedDays: Int,
    val remainingDays: Int,
    val currentWeek: Int,
    val extraDaysInWeek: Int,
    val trimester: Int,
    val trimesterLabelBn: String,
    val progressFraction: Float,
    val progressPercent: Int
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
    val category: String, // "PRE_CONCEPTION", "DO", "DONT", "NEWBORN_SUNNAH"
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

object PregnancyPlannerEngine {

    const val TOTAL_PREGNANCY_DAYS = 280
    private const val DAY_MILLIS = 86_400_000L

    fun calculateTimelineStatus(
        startMillis: Long,
        referenceDateMillis: Long = System.currentTimeMillis()
    ): PregnancyTimelineStatus {
        val cleanStart = HabitDateAndReportEngine.startOfDayMillis(startMillis)
        val cleanRef = HabitDateAndReportEngine.startOfDayMillis(referenceDateMillis)
        val eddMillis = HabitDateAndReportEngine.addDays(cleanStart, TOTAL_PREGNANCY_DAYS)

        val rawElapsed = ((cleanRef - cleanStart) / DAY_MILLIS).toInt()
        val elapsedDays = rawElapsed.coerceIn(1, TOTAL_PREGNANCY_DAYS)
        val remainingDays = (TOTAL_PREGNANCY_DAYS - elapsedDays).coerceAtLeast(0)

        val currentWeek = ((elapsedDays - 1) / 7 + 1).coerceIn(1, 40)
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

        val fraction = (elapsedDays.toFloat() / TOTAL_PREGNANCY_DAYS.toFloat()).coerceIn(0f, 1f)
        val percent = (fraction * 100f).roundToInt().coerceIn(0, 100)

        return PregnancyTimelineStatus(
            startMillis = cleanStart,
            eddMillis = eddMillis,
            startDateFormattedBn = HabitDateAndReportEngine.formatFullDateBn(cleanStart),
            eddDateFormattedBn = HabitDateAndReportEngine.formatFullDateBn(eddMillis),
            elapsedDays = elapsedDays,
            remainingDays = remainingDays,
            currentWeek = currentWeek,
            extraDaysInWeek = extraDaysInWeek,
            trimester = trimester,
            trimesterLabelBn = trimesterLabelBn,
            progressFraction = fraction,
            progressPercent = percent
        )
    }

    /**
     * Converts a user-selected Expected Delivery Date (EDD) into the corresponding
     * pregnancy start timestamp (EDD - 280 days).
     */
    fun startMillisFromEdd(eddMillis: Long): Long {
        val cleanEdd = HabitDateAndReportEngine.startOfDayMillis(eddMillis)
        return HabitDateAndReportEngine.addDays(cleanEdd, -TOTAL_PREGNANCY_DAYS)
    }

    fun getGrowthForWeek(week: Int): WeeklyBabyGrowth {
        val safeWeek = week.coerceIn(1, 40)
        return weeklyGrowthCatalog.firstOrNull { it.week == safeWeek }
            ?: weeklyGrowthCatalog.last()
    }

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
}
