package com.health.knowledge.ui.pregnancy

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.health.knowledge.domain.model.Article
import com.health.knowledge.habit.HabitDateAndReportEngine
import com.health.knowledge.habit.HabitScheduleCalculator
import com.health.knowledge.pregnancy.IslamicPregnancyAmal
import com.health.knowledge.pregnancy.MaleHealthTip
import com.health.knowledge.pregnancy.PregnancyChecklistItem
import com.health.knowledge.pregnancy.PregnancyPlannerEngine
import com.health.knowledge.pregnancy.PregnancyQuote
import com.health.knowledge.pregnancy.PregnancyReminderPreset
import com.health.knowledge.ui.home.ArticleCard
import com.health.knowledge.ui.theme.onePxBorder
import com.health.knowledge.ui.theme.solidOverSurface
import com.health.knowledge.util.UserPreferencesManager
import java.util.Calendar

/**
 * Step-by-Step Profile Setup / Onboarding Wizard for "প্রেগন্যান্সি — আগে ও পরে".
 * Guides users sequentially:
 * - Step 1: Gender (Male -> direct entry to Male Dashboard; Female -> advances to Step 2)
 * - Step 2: Stage (Planning vs Pregnant)
 * - Step 3: Dates, cycle length, and symptoms report
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CycleDaysSelector(
    selectedCycleDays: Int,
    onCycleDaysChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val presetDays = listOf(26, 27, 28, 29, 30)
    val isCustom = selectedCycleDays !in presetDays
    val isAbnormal = selectedCycleDays < 21 || selectedCycleDays > 35

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f).solidOverSurface()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = onePxBorder(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "মাসিক চক্রের গড় দিন:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "${HabitScheduleCalculator.toBengaliDigits(selectedCycleDays.toString())} দিন",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Presets row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                presetDays.forEach { days ->
                    val isChosen = selectedCycleDays == days
                    FilterChip(
                        selected = isChosen,
                        onClick = { onCycleDaysChanged(days) },
                        label = {
                            Text(
                                text = "${HabitScheduleCalculator.toBengaliDigits(days.toString())} দিন",
                                fontSize = 12.sp,
                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }

                FilterChip(
                    selected = isCustom,
                    onClick = { /* Keep current custom selection */ },
                    label = {
                        Text(
                            text = if (isCustom) "✏️ কাস্টম (${HabitScheduleCalculator.toBengaliDigits(selectedCycleDays.toString())})" else "✏️ কাস্টম দিন",
                            fontSize = 12.sp,
                            fontWeight = if (isCustom) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }

            // Custom Stepper (- and +)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedIconButton(
                    onClick = {
                        if (selectedCycleDays > 18) onCycleDaysChanged(selectedCycleDays - 1)
                    },
                    enabled = selectedCycleDays > 18,
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "১ দিন কমান",
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = "${HabitScheduleCalculator.toBengaliDigits(selectedCycleDays.toString())} দিন",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                OutlinedIconButton(
                    onClick = {
                        if (selectedCycleDays < 60) onCycleDaysChanged(selectedCycleDays + 1)
                    },
                    enabled = selectedCycleDays < 60,
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "১ দিন বাড়ান",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (isAbnormal) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(10.dp),
                    border = onePxBorder(MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("⚠️", fontSize = 14.sp)
                        Text(
                            text = "আপনার মাসিক চক্রটি স্বাভাবিক গড় (২১–৩৫ দিন) থেকে কিছুটা ভিন্ন। অনিয়মিত চক্রের ক্ষেত্রে প্রাকৃতিক সুষম খাবার, মেথি/কালোজিরা ও পর্যাপ্ত বিশ্রামের দিকে নজর দিন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PregnancyMedicalDisclaimerFooter(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pregnancy_medical_disclaimer_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f).solidOverSurface()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = onePxBorder(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("⚖️", fontSize = 20.sp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "চিকিৎসা সংক্রান্ত দায়মুক্তি (Medical Disclaimer)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "বিশেষ দ্রষ্টব্য: এই অ্যাপের সকল তথ্য শুধুমাত্র প্রাকৃতিক জীবনযাপন ও স্বাস্থ্য সচেতনতার উদ্দেশ্যে প্রদান করা হয়েছে। মা ও শিশুর যেকোনো শারীরিক জটিলতায় অবশ্যই বিশেষজ্ঞ চিকিৎসকের পরামর্শ নিন।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * Step-by-Step Profile Setup / Onboarding Wizard for "প্রেগন্যান্সি — আগে ও পরে".
 * Guides users sequentially:
 * - Step 1: Gender (Male -> direct entry to Male Dashboard; Female -> advances to Step 2)
 * - Step 2: Stage (Planning vs Pregnant vs Postpartum)
 * - Step 3: Dates, cycle length, and symptoms report
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PregnancyProfileOnboardingView(
    initialGender: String,
    initialStageMode: String,
    initialStartMillis: Long,
    initialPeriodEndMillis: Long = 0L,
    initialCycleDays: Int,
    initialSymptoms: Set<String>,
    initialDeliveryMillis: Long = 0L,
    initialDeliveryType: String = UserPreferencesManager.DELIVERY_TYPE_NORMAL,
    initialBabyGender: String = UserPreferencesManager.BABY_GENDER_UNSPECIFIED,
    onCompleteSetup: (
        gender: String,
        stageMode: String,
        startMillis: Long,
        cycleDays: Int,
        symptoms: Set<String>,
        periodEndMillis: Long,
        deliveryMillis: Long,
        deliveryType: String,
        babyGender: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var currentStep by remember { mutableIntStateOf(1) }
    var selectedGender by remember(initialGender) { mutableStateOf(initialGender) }
    var selectedStage by remember(initialStageMode) { mutableStateOf(initialStageMode) }
    var pickedStartMillis by remember(initialStartMillis) { mutableLongStateOf(initialStartMillis) }
    var pickedEndMillis by remember(initialPeriodEndMillis) { mutableLongStateOf(initialPeriodEndMillis) }
    var selectedCycleDays by remember(initialCycleDays) { mutableIntStateOf(initialCycleDays) }
    var selectedSymptoms by remember(initialSymptoms) { mutableStateOf(initialSymptoms) }
    var pickedDeliveryMillis by remember(initialDeliveryMillis) {
        mutableLongStateOf(if (initialDeliveryMillis > 0L) initialDeliveryMillis else System.currentTimeMillis() - 7L * 86_400_000L)
    }
    var selectedDeliveryType by remember(initialDeliveryType) { mutableStateOf(initialDeliveryType) }
    var selectedBabyGender by remember(initialBabyGender) { mutableStateOf(initialBabyGender) }

    val isMale = selectedGender == UserPreferencesManager.GENDER_MALE
    val progress = if (isMale) 1f else (currentStep.toFloat() / 3f)

    val liveTimeline = remember(pickedStartMillis, selectedCycleDays) {
        PregnancyPlannerEngine.calculateTimelineStatus(
            startMillis = pickedStartMillis,
            cycleDays = selectedCycleDays
        )
    }
    val liveFertile = remember(pickedStartMillis, pickedEndMillis, selectedCycleDays) {
        PregnancyPlannerEngine.calculateFertileWindow(
            lastPeriodStartMillis = pickedStartMillis,
            lastPeriodEndMillis = pickedEndMillis,
            cycleDays = selectedCycleDays
        )
    }
    val liveBabyAge = remember(pickedDeliveryMillis, selectedDeliveryType, selectedBabyGender) {
        PregnancyPlannerEngine.calculateBabyAge(
            deliveryMillis = pickedDeliveryMillis,
            deliveryType = selectedDeliveryType,
            babyGender = selectedBabyGender
        )
    }

    fun openStartDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = pickedStartMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                pickedStartMillis = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openEndDatePicker() {
        val baseTime = if (pickedEndMillis > 0L) pickedEndMillis else pickedStartMillis + 5L * 86_400_000L
        val cal = Calendar.getInstance().apply { timeInMillis = baseTime }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                pickedEndMillis = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openDeliveryDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = pickedDeliveryMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                pickedDeliveryMillis = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Step Indicator & Progress Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pregnancy_onboarding_progress_card"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f).solidOverSurface()
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.38f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when {
                            isMale -> "ধাপ ১ / ১ • আপনার পরিচয়"
                            currentStep == 1 -> "ধাপ ১ / ৩ • আপনার পরিচয়"
                            currentStep == 2 -> "ধাপ ২ / ৩ • বর্তমান অবস্থা"
                            else -> "ধাপ ৩ / ৩ • তারিখ ও বিবরণ"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isMale) "১০০%" else "${(progress * 100).toInt()}% সম্পন্ন",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                    drawStopIndicator = {}
                )
            }
        }

        // Animated Step Content
        AnimatedContent(
            targetState = if (isMale) 1 else currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally { width -> width / 2 } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> -width / 2 } + fadeOut()
                    )
                } else {
                    (slideInHorizontally { width -> -width / 2 } + fadeIn()).togetherWith(
                        slideOutHorizontally { width -> width / 2 } + fadeOut()
                    )
                }
            },
            label = "OnboardingStepContent"
        ) { step ->
            when (step) {
                1 -> {
                    // STEP 1: Gender Selection
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "আপনার জেন্ডার নির্বাচন করুন",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "আপনার পরিচয় অনুযায়ী সঠিক হেলথ গাইড ও টিপস সাজানো হবে।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OnboardingChoiceCard(
                                emoji = "👩",
                                titleBn = "নারী",
                                subtitleBn = "প্ল্যানিং, গর্ভাবস্থা ও নবজাতক",
                                isSelected = selectedGender == UserPreferencesManager.GENDER_FEMALE,
                                onClick = {
                                    selectedGender = UserPreferencesManager.GENDER_FEMALE
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("select_gender_female")
                            )

                            OnboardingChoiceCard(
                                emoji = "👨",
                                titleBn = "পুরুষ",
                                subtitleBn = "ফার্টিলিটি ট্রিকস ও বাবার দায়িত্ব",
                                isSelected = selectedGender == UserPreferencesManager.GENDER_MALE,
                                onClick = {
                                    selectedGender = UserPreferencesManager.GENDER_MALE
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("select_gender_male")
                            )
                        }

                        // Male Features Preview Card
                        AnimatedVisibility(
                            visible = selectedGender == UserPreferencesManager.GENDER_MALE,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.32f).solidOverSurface()
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "👨 পুরুষদের ড্যাশবোর্ডে যা যা থাকছে:",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "• টেস্টোস্টেরন ও ফার্টিলিটি বৃদ্ধির ৭টি কার্যকরী ট্রিকস\n• সুন্নাহ সুপারফুড তালিকা (কালোজিরা-মধু, ডুমুর ও জয়তুন)\n• প্রসব-পূর্ব ও প্রসব-পরবর্তী স্ত্রীর যত্ন ও বাবার দায়িত্ব\n• নেক সন্তান লাভের কুরআনি দোয়া ও সময়োপযোগী আমল",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // STEP 2: Female Stage Selection (3 Stages: Planning, Pregnant, Postpartum)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "আপনার বর্তমান অবস্থা কোনটি?",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "আপনার মাতৃত্বের বর্তমান পর্যায় নির্বাচন করুন যাতে নির্দিষ্ট ট্র্যাকার ও গাইড সক্রিয় হয়।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OnboardingStageOptionCard(
                                emoji = "🌱",
                                titleBn = "প্ল্যান করছি (Pre-conception)",
                                subtitleBn = "সন্তান নেওয়ার পূর্বপ্রস্তুতি, মাসিক ট্র্যাকিং ও ওভুলেশন উর্বর দিন",
                                isSelected = selectedStage == UserPreferencesManager.STAGE_PRE_CONCEPTION,
                                onClick = {
                                    selectedStage = UserPreferencesManager.STAGE_PRE_CONCEPTION
                                    val now = System.currentTimeMillis()
                                    if (now - pickedStartMillis > 35L * 86_400_000L) {
                                        pickedStartMillis = now - 12L * 86_400_000L
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("select_stage_planning")
                            )

                            OnboardingStageOptionCard(
                                emoji = "🤰",
                                titleBn = "গর্ভবতী (Pregnancy)",
                                subtitleBn = "মাসিক বন্ধ হয়েছে • সাপ্তাহিক বৃদ্ধি, ট্রাইমেস্টার ও সম্ভাব্য প্রসব তারিখ (EDD)",
                                isSelected = selectedStage == UserPreferencesManager.STAGE_POST_CONCEPTION,
                                onClick = {
                                    selectedStage = UserPreferencesManager.STAGE_POST_CONCEPTION
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("select_stage_pregnant")
                            )

                            OnboardingStageOptionCard(
                                emoji = "🤱",
                                titleBn = "মা ও নবজাতক (Postpartum & Baby Care)",
                                subtitleBn = "বাচ্চা ডেলিভারি হয়েছে • নবজাতকের বয়স, যত্ন ও মায়ের প্রসব-পরবর্তী সুস্থতা",
                                isSelected = selectedStage == UserPreferencesManager.STAGE_POSTPARTUM,
                                onClick = {
                                    selectedStage = UserPreferencesManager.STAGE_POSTPARTUM
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("select_stage_postpartum")
                            )
                        }
                    }
                }

                else -> {
                    // STEP 3: Dates, Menstrual Cycle & Symptoms
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = when (selectedStage) {
                                    UserPreferencesManager.STAGE_POSTPARTUM -> "সন্তান জন্মের তারিখ ও স্বাস্থ্য তথ্য"
                                    UserPreferencesManager.STAGE_POST_CONCEPTION -> "কবে মাসিক বন্ধ হয়েছে ও লক্ষণ রিপোর্ট"
                                    else -> "মাসিক শুরু-শেষের তারিখ ও লক্ষণ"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "সঠিক দিন গণনা ও তাৎক্ষণিক ঘরোয়া পরামর্শ পেতে তথ্যগুলো দিন।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        when (selectedStage) {
                            UserPreferencesManager.STAGE_POSTPARTUM -> {
                                // ── POSTPARTUM (মা ও নবজাতক) INPUTS ──
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f).solidOverSurface()
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.38f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Delivery Date Picker
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "সন্তান জন্মের তারিখ (ডেলিভারি ডেট)",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = HabitDateAndReportEngine.formatFullDateBn(pickedDeliveryMillis),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            FilledTonalButton(
                                                onClick = ::openDeliveryDatePicker,
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("তারিখ দিন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Delivery Type Selection
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "প্রসবের ধরন:",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                FilterChip(
                                                    selected = selectedDeliveryType == UserPreferencesManager.DELIVERY_TYPE_NORMAL,
                                                    onClick = { selectedDeliveryType = UserPreferencesManager.DELIVERY_TYPE_NORMAL },
                                                    label = { Text("স্বাভাবিক প্রসব (Normal)", fontSize = 12.sp) }
                                                )
                                                FilterChip(
                                                    selected = selectedDeliveryType == UserPreferencesManager.DELIVERY_TYPE_C_SECTION,
                                                    onClick = { selectedDeliveryType = UserPreferencesManager.DELIVERY_TYPE_C_SECTION },
                                                    label = { Text("সিজারিয়ান (C-Section)", fontSize = 12.sp) }
                                                )
                                            }
                                        }

                                        // Baby Gender Selection
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = "সন্তানের পরিচয়:",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                FilterChip(
                                                    selected = selectedBabyGender == UserPreferencesManager.BABY_GENDER_BOY,
                                                    onClick = { selectedBabyGender = UserPreferencesManager.BABY_GENDER_BOY },
                                                    label = { Text("ছেলে 👦", fontSize = 12.sp) }
                                                )
                                                FilterChip(
                                                    selected = selectedBabyGender == UserPreferencesManager.BABY_GENDER_GIRL,
                                                    onClick = { selectedBabyGender = UserPreferencesManager.BABY_GENDER_GIRL },
                                                    label = { Text("মেয়ে 👧", fontSize = 12.sp) }
                                                )
                                                FilterChip(
                                                    selected = selectedBabyGender == UserPreferencesManager.BABY_GENDER_UNSPECIFIED,
                                                    onClick = { selectedBabyGender = UserPreferencesManager.BABY_GENDER_UNSPECIFIED },
                                                    label = { Text("নির্দিষ্ট নয়", fontSize = 12.sp) }
                                                )
                                            }
                                        }

                                        // Live Baby Age Preview
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "👶 বাবুর বয়স: ${liveBabyAge.ageDisplayBn}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "🌟 ${liveBabyAge.milestoneTitleBn}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = liveBabyAge.milestoneDetailBn,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            UserPreferencesManager.STAGE_POST_CONCEPTION -> {
                                // ── PREGNANT INPUTS (মাসিক বন্ধ) ──
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f).solidOverSurface()
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.38f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "শেষ মাসিকের প্রথম দিন (মাসিক বন্ধ / LMP)",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = HabitDateAndReportEngine.formatFullDateBn(pickedStartMillis),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            FilledTonalButton(
                                                onClick = ::openStartDatePicker,
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("তারিখ দিন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Instant Live Calculation Preview
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "✨ আজ ${HabitScheduleCalculator.toBengaliDigits(liveTimeline.elapsedDays.toString())} দিন হলো (${HabitScheduleCalculator.toBengaliDigits(liveTimeline.currentWeek.toString())} সপ্তাহ • ${liveTimeline.trimesterLabelBn})",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "🎯 সম্ভাব্য ডেলিভারি (EDD): ${liveTimeline.eddDateFormattedBn}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                // ── PRE-CONCEPTION INPUTS (প্ল্যানিং ও মাসিক ট্র্যাকিং) ──
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f).solidOverSurface()
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                    border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.38f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Start Date
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "মাসিক শুরুর তারিখ (Start Date)",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = HabitDateAndReportEngine.formatFullDateBn(pickedStartMillis),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }

                                            FilledTonalButton(
                                                onClick = ::openStartDatePicker,
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text("তারিখ দিন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                        // End Date
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "মাসিক শেষ হওয়ার তারিখ (End Date - ঐচ্ছিক)",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                Text(
                                                    text = if (pickedEndMillis > 0L) {
                                                        HabitDateAndReportEngine.formatFullDateBn(pickedEndMillis)
                                                    } else {
                                                        "এখনও শেষ হয়নি / দেওয়া হয়নি"
                                                    },
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = if (pickedEndMillis > 0L) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (pickedEndMillis > 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            FilledTonalButton(
                                                onClick = ::openEndDatePicker,
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = if (pickedEndMillis > 0L) "পরিবর্তন" else "তারিখ দিন",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        // Period Duration and Flow Notice
                                        if (liveFertile.periodDurationDays > 0) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "🩸 মাসিকের স্থায়িত্বকাল:",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        text = "${HabitScheduleCalculator.toBengaliDigits(liveFertile.periodDurationDays.toString())} দিন",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }

                                        if (!liveFertile.periodDurationNoticeBn.isNullOrEmpty()) {
                                            Surface(
                                                color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = liveFertile.periodDurationNoticeBn ?: "",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    modifier = Modifier.padding(10.dp),
                                                    lineHeight = 17.sp
                                                )
                                            }
                                        }

                                        // Instant Live Calculation Preview
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "🌸 সম্ভাব্য উর্বর সময়: ${liveFertile.fertileStartFormattedBn} – ${liveFertile.fertileEndFormattedBn}",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "সম্ভাব্য ওভুলেশন দিন: ${liveFertile.ovulationDateFormattedBn}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "পরবর্তী মাসিকের সম্ভাব্য তারিখ: ${liveFertile.nextPeriodFormattedBn}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                // Cycle Days Selector
                                CycleDaysSelector(
                                    selectedCycleDays = selectedCycleDays,
                                    onCycleDaysChanged = { selectedCycleDays = it }
                                )
                            }
                        }

                        // Symptom Selector Chips (Exclusive 'normal' vs other symptoms)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f).solidOverSurface()
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = onePxBorder(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "বর্তমান শারীরিক লক্ষণ (প্রযোজ্যগুলো নির্বাচন করুন):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PregnancyPlannerEngine.symptomOptions.forEach { sym ->
                                        val selected = sym.id in selectedSymptoms
                                        FilterChip(
                                            selected = selected,
                                            onClick = {
                                                val next = selectedSymptoms.toMutableSet()
                                                if (sym.id == "normal") {
                                                    next.clear()
                                                    next.add("normal")
                                                } else {
                                                    next.remove("normal")
                                                    if (sym.id in next) next.remove(sym.id) else next.add(sym.id)
                                                    if (next.isEmpty()) next.add("normal")
                                                }
                                                selectedSymptoms = next
                                            },
                                            label = {
                                                Text(
                                                    text = "${sym.emoji} ${sym.labelBn}",
                                                    fontSize = 12.sp,
                                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Medical Compliance Disclaimer Footer
        PregnancyMedicalDisclaimerFooter()

        // Navigation Buttons Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isMale && currentStep > 1) {
                OutlinedButton(
                    onClick = { currentStep -= 1 },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("onboarding_prev_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "পূর্ববর্তী",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "পূর্ববর্তী",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = {
                    if (isMale) {
                        onCompleteSetup(
                            selectedGender,
                            selectedStage,
                            pickedStartMillis,
                            selectedCycleDays,
                            selectedSymptoms,
                            pickedEndMillis,
                            pickedDeliveryMillis,
                            selectedDeliveryType,
                            selectedBabyGender
                        )
                    } else {
                        if (currentStep < 3) {
                            currentStep += 1
                        } else {
                            onCompleteSetup(
                                selectedGender,
                                selectedStage,
                                pickedStartMillis,
                                selectedCycleDays,
                                selectedSymptoms,
                                pickedEndMillis,
                                pickedDeliveryMillis,
                                selectedDeliveryType,
                                selectedBabyGender
                            )
                        }
                    }
                },
                modifier = Modifier
                    .weight(if (!isMale && currentStep > 1) 1.5f else 1f)
                    .height(50.dp)
                    .testTag(
                        if (isMale) "onboarding_start_male_button"
                        else if (currentStep < 3) "onboarding_next_step_button"
                        else "complete_pregnancy_onboarding_button"
                    ),
                shape = RoundedCornerShape(14.dp)
            ) {
                val buttonText = when {
                    isMale -> "শুরু করুন 🚀"
                    currentStep < 3 -> "পরবর্তী ধাপ ➔"
                    else -> "প্রোফাইল সম্পন্ন করুন ✨"
                }
                Text(
                    text = buttonText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun OnboardingStageOptionCard(
    emoji: String,
    titleBn: String,
    subtitleBn: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f).solidOverSurface()
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f).solidOverSurface()
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = onePxBorder(borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(emoji, fontSize = 22.sp)
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = titleBn,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitleBn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }
            RadioButton(
                selected = isSelected,
                onClick = null
            )
        }
    }
}

@Composable
private fun OnboardingChoiceCard(
    emoji: String,
    titleBn: String,
    subtitleBn: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f).solidOverSurface()
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.24f).solidOverSurface()
    }
    val borderColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = onePxBorder(borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                shape = CircleShape,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(emoji, fontSize = 22.sp)
                }
            }
            Text(
                text = titleBn,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitleBn,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * ── DEDICATED MALE HEALTH & FATHERHOOD COMPANION DASHBOARD ──
 * Shown when the user selects Gender = "MALE" (👨 পুরুষ).
 */
@Composable
fun MaleHealthAndFatherhoodDashboard(
    quote: PregnancyQuote,
    completedChecks: Set<String>,
    activeHabitIds: Set<String>,
    allHabits: List<com.health.knowledge.data.local.entity.HabitEntity> = emptyList(),
    pregnancyPosts: List<Article> = emptyList(),
    isAdminMode: Boolean = false,
    onNextQuote: () -> Unit,
    isSpeaking: Boolean = false,
    onSpeakText: (String) -> Unit,
    onToggleCheck: (String) -> Unit,
    onToggleReminderPreset: (PregnancyReminderPreset) -> Unit,
    onEditReminderTime: ((PregnancyReminderPreset, String) -> Unit)? = null,
    onEditProfile: () -> Unit,
    onArticleClick: (Int) -> Unit = {},
    onBookmarkToggle: (Int, Boolean) -> Unit = { _, _ -> },
    onReactionClick: (Article) -> Unit = {},
    onVerifyClick: (Article) -> Unit = {},
    onPinToggle: (Article) -> Unit = {},
    onDeleteClick: (Article) -> Unit = {},
    onOpenAdminPostDialog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedMaleTab by remember { mutableIntStateOf(0) } // 0=ট্রিকস, 1=পুষ্টি, 2=দায়িত্ব, 3=দোয়া, 4=পোস্ট

    val fatherDuties = PregnancyPlannerEngine.fatherhoodChecklist
    val doneDutyCount = fatherDuties.count { it.id in completedChecks }

    val tricksList = remember {
        PregnancyPlannerEngine.maleHealthTipsCatalog.filter { it.category == "TRICKS" }
    }
    val sunnahFoodsList = remember {
        PregnancyPlannerEngine.maleHealthTipsCatalog.filter { it.category == "SUNNAH_FOOD" }
    }
    val fatherGuideList = remember {
        PregnancyPlannerEngine.maleHealthTipsCatalog.filter { it.category == "FATHER_GUIDE" }
    }
    val fatherDuas = remember {
        PregnancyPlannerEngine.islamicAmalsAndDuas.filter {
            it.stageTag == "PRE" || it.stageTag == "TRIMESTER_2"
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 84.dp, top = 16.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Male Hero Banner Card
        item(key = "male_hero_banner") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("male_health_hero_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.36f).solidOverSurface()
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.42f))
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("👨", fontSize = 24.sp)
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "পুরুষ স্বাস্থ্য ও পিতৃত্ব গাইড",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "ফার্টিলিটি ট্রিকস, সুন্নাহ খাবার ও হবু বাবার করণীয়",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.clickable { onEditProfile() }
                        ) {
                            Text(
                                text = "প্রোফাইল",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Fatherhood Checklist Progress Strip
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(12.dp),
                        border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🌟 আজকের পিতৃত্ব প্রস্তুতি ও অভ্যাস",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${HabitScheduleCalculator.toBengaliDigits(doneDutyCount.toString())}/${HabitScheduleCalculator.toBengaliDigits(fatherDuties.size.toString())} সম্পন্ন",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                }
            }
        }

        // 2. Quote Card
        item(key = "male_quote_card") {
            PregnancyQuoteCard(
                quote = quote,
                isSpeaking = isSpeaking,
                onNextQuote = onNextQuote,
                onSpeakQuote = { onSpeakText(quote.quoteBn) }
            )
        }

        // 3. 5 Sub-Tabs: ⚡ ট্রিকস | 🍯 পুষ্টি | 🤝 দায়িত্ব | 🤲 দোয়া | 📝 পোস্ট
        item(key = "male_sub_tabs") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    0 to "⚡ ট্রিকস",
                    1 to "🍯 পুষ্টি",
                    2 to "🤝 দায়িত্ব",
                    3 to "🤲 দোয়া",
                    4 to "📝 পোস্ট (${HabitScheduleCalculator.toBengaliDigits(pregnancyPosts.size.toString())})"
                ).forEach { (idx, label) ->
                    FilterChip(
                        selected = selectedMaleTab == idx,
                        onClick = { selectedMaleTab = idx },
                        label = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (selectedMaleTab == idx) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.testTag("male_subtab_$idx")
                    )
                }
            }
        }

        when (selectedMaleTab) {
            0 -> {
                items(tricksList, key = { it.id }) { tip ->
                    MaleHealthTipCard(
                        tip = tip,
                        onSpeakClick = { onSpeakText("${tip.titleBn}। ${tip.detailBn} ${tip.tipHighlightBn}") }
                    )
                }
            }

            1 -> {
                items(sunnahFoodsList, key = { it.id }) { tip ->
                    MaleHealthTipCard(
                        tip = tip,
                        onSpeakClick = { onSpeakText("${tip.titleBn}। ${tip.detailBn} ${tip.tipHighlightBn}") }
                    )
                }
            }

            2 -> {
                item(key = "father_checklist_title") {
                    Text(
                        text = "✅ হবু বাবার দৈনিক করণীয় চেকলিস্ট",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(fatherDuties, key = { it.id }) { item ->
                    PregnancyChecklistRowCard(
                        item = item,
                        isChecked = item.id in completedChecks,
                        isDontItem = false,
                        onToggle = { onToggleCheck(item.id) }
                    )
                }

                item(key = "father_guide_title") {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "🤝 স্ত্রীর যত্নে স্বামীর বিশেষ গাইড",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                items(fatherGuideList, key = { it.id }) { tip ->
                    MaleHealthTipCard(
                        tip = tip,
                        onSpeakClick = { onSpeakText("${tip.titleBn}। ${tip.detailBn}") }
                    )
                }
            }

            3 -> {
                items(fatherDuas, key = { it.id }) { amal ->
                    IslamicPregnancyAmalCard(
                        amal = amal,
                        onSpeakClick = {
                            onSpeakText("${amal.titleBn}। উচ্চারণ: ${amal.pronunciationBn}। অর্থ: ${amal.meaningBn}")
                        }
                    )
                }

                item(key = "male_reminders_header") {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "🔔 দোয়া ও পুষ্টি রিমাইন্ডার",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                val maleReminders = PregnancyPlannerEngine.reminderPresets.filter {
                    it.id == "preg_milk_dates" || it.id == "preg_dua_tilawat"
                }
                items(maleReminders, key = { "male_rem_${it.id}" }) { preset ->
                    val habit = allHabits.firstOrNull { it.id == preset.id }
                    val currentTime = habit?.anchorTime ?: preset.anchorTime
                    PregnancyReminderPresetCard(
                        preset = preset,
                        isActive = preset.id in activeHabitIds,
                        currentTime = currentTime,
                        onToggle = { onToggleReminderPreset(preset) },
                        onEditTime = { onEditReminderTime?.invoke(preset, currentTime) }
                    )
                }
            }

            4 -> {
                item(key = "male_posts_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "📝 প্রেগন্যান্সি ও পিতৃত্ব পোস্ট (Firestore)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "অ্যাডমিন কর্তৃক প্রকাশিত যাচাইকৃত স্বাস্থ্যতথ্য ও পরামর্শ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (isAdminMode) {
                            FilledTonalButton(
                                onClick = onOpenAdminPostDialog,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("পোস্ট করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (pregnancyPosts.isEmpty()) {
                    item(key = "male_posts_empty") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = onePxBorder(MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Text(
                                text = "এখনো কোনো প্রেগন্যান্সি পোস্ট যুক্ত করা হয়নি।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    }
                } else {
                    items(pregnancyPosts, key = { "male_preg_post_${it.id}" }) { article ->
                        Box(modifier = Modifier.offset(x = 0.dp)) {
                            ArticleCard(
                                article = article,
                                isAdminMode = isAdminMode,
                                onClick = { onArticleClick(article.id) },
                                onBookmarkToggle = { onBookmarkToggle(article.id, article.isBookmarked) },
                                onReactionClick = { onReactionClick(article) },
                                onVerifyClick = { onVerifyClick(article) },
                                onPinToggle = { onPinToggle(article) },
                                onDeleteClick = { onDeleteClick(article) }
                            )
                        }
                    }
                }
            }
        }

        item(key = "male_medical_disclaimer") {
            PregnancyMedicalDisclaimerFooter()
        }
    }
}

@Composable
fun MaleHealthTipCard(
    tip: MaleHealthTip,
    onSpeakClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f).solidOverSurface()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.38f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(tip.emoji, fontSize = 20.sp)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = tip.badgeBn,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = tip.titleBn,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Pure Icon Button for Voice
                FilledTonalIconButton(
                    onClick = onSpeakClick,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "শুনুন",
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Text(
                text = tip.detailBn,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 19.sp
            )

            Surface(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                shape = RoundedCornerShape(10.dp),
                border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "💡 ${tip.tipHighlightBn}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}
