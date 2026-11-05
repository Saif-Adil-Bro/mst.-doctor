package com.health.knowledge.ui.pregnancy

import android.app.DatePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import com.health.knowledge.habit.HabitDateAndReportEngine
import com.health.knowledge.habit.HabitScheduleCalculator
import com.health.knowledge.pregnancy.IslamicPregnancyAmal
import com.health.knowledge.pregnancy.MaleHealthTip
import com.health.knowledge.pregnancy.PregnancyChecklistItem
import com.health.knowledge.pregnancy.PregnancyPlannerEngine
import com.health.knowledge.pregnancy.PregnancyQuote
import com.health.knowledge.pregnancy.PregnancyReminderPreset
import com.health.knowledge.ui.theme.onePxBorder
import com.health.knowledge.ui.theme.solidOverSurface
import com.health.knowledge.util.UserPreferencesManager
import java.util.Calendar

/**
 * Dynamic, visual Profile Setup / Onboarding Wizard shown when a user first enters
 * "প্রেগন্যান্সি — আগে ও পরে" or taps the profile edit icon.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PregnancyProfileOnboardingView(
    initialGender: String,
    initialStageMode: String,
    initialStartMillis: Long,
    initialCycleDays: Int,
    initialSymptoms: Set<String>,
    onCompleteSetup: (
        gender: String,
        stageMode: String,
        startMillis: Long,
        cycleDays: Int,
        symptoms: Set<String>
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var selectedGender by remember(initialGender) { mutableStateOf(initialGender) }
    var selectedStage by remember(initialStageMode) { mutableStateOf(initialStageMode) }
    var pickedDateMillis by remember(initialStartMillis) { mutableLongStateOf(initialStartMillis) }
    var selectedCycleDays by remember(initialCycleDays) { mutableIntStateOf(initialCycleDays) }
    var selectedSymptoms by remember(initialSymptoms) { mutableStateOf(initialSymptoms) }

    val liveTimeline = remember(pickedDateMillis, selectedCycleDays) {
        PregnancyPlannerEngine.calculateTimelineStatus(
            startMillis = pickedDateMillis,
            cycleDays = selectedCycleDays
        )
    }
    val liveFertile = remember(pickedDateMillis, selectedCycleDays) {
        PregnancyPlannerEngine.calculateFertileWindow(
            lastPeriodStartMillis = pickedDateMillis,
            cycleDays = selectedCycleDays
        )
    }

    fun openLmpDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = pickedDateMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                pickedDateMillis = Calendar.getInstance().apply {
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Welcome Header Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("pregnancy_onboarding_banner"),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.38f).solidOverSurface()
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.42f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("🤱", fontSize = 26.sp)
                    }
                }
                Text(
                    text = "আপনার প্রোফাইল সাজিয়ে নিন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "সঠিক স্বাস্থ্য পরামর্শ ও ডেলিভারি হিসাব পেতে নিচের তথ্যগুলো নির্বাচন করুন",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }

        // STEP 1: Gender Selection (👨 পুরুষ vs 👩 নারী)
        Text(
            text = "১. আপনার জেন্ডার নির্বাচন করুন",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OnboardingChoiceCard(
                emoji = "👩",
                titleBn = "নারী",
                subtitleBn = "প্ল্যানিং ও গর্ভকালীন গাইড",
                isSelected = selectedGender == UserPreferencesManager.GENDER_FEMALE,
                onClick = { selectedGender = UserPreferencesManager.GENDER_FEMALE },
                modifier = Modifier
                    .weight(1f)
                    .testTag("select_gender_female")
            )

            OnboardingChoiceCard(
                emoji = "👨",
                titleBn = "পুরুষ",
                subtitleBn = "স্বাস্থ্য ট্রিকস ও বাবার করণীয়",
                isSelected = selectedGender == UserPreferencesManager.GENDER_MALE,
                onClick = { selectedGender = UserPreferencesManager.GENDER_MALE },
                modifier = Modifier
                    .weight(1f)
                    .testTag("select_gender_male")
            )
        }

        // If MALE is selected -> Show Male Highlights Card
        AnimatedVisibility(
            visible = selectedGender == UserPreferencesManager.GENDER_MALE,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
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
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "👨 পুরুষদের ড্যাশবোর্ডে যা থাকছে:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "• পুরুষের ফার্টিলিটি ও হরমোন বৃদ্ধির প্রাকৃতিক হেলথ ট্রিকস\n• সুন্নাহ সুপারফুড (কালোজিরা, মধু, খেজুর, ডুমুর ও জয়তুন)\n• সন্তান নেওয়ার আগে ও স্ত্রীর গর্ভকালে হবু বাবার করণীয়\n• নেক সন্তান লাভের কুরআনি দোয়া ও প্রতিদিনের রিমাইন্ডার",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }
            }
        }

        // If FEMALE is selected -> Step 2 (Planning vs Pregnant) + Step 3 (LMP Date, Cycle & Symptoms)
        AnimatedVisibility(
            visible = selectedGender == UserPreferencesManager.GENDER_FEMALE,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "২. আপনার বর্তমান অবস্থা কোনটি?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OnboardingChoiceCard(
                        emoji = "🌱",
                        titleBn = "প্ল্যান করছি",
                        subtitleBn = "সন্তান নেওয়ার প্রস্তুতি",
                        isSelected = selectedStage == UserPreferencesManager.STAGE_PRE_CONCEPTION,
                        onClick = {
                            selectedStage = UserPreferencesManager.STAGE_PRE_CONCEPTION
                            // Default last period to 12 days ago if previous date was far in the past
                            val now = System.currentTimeMillis()
                            if (now - pickedDateMillis > 35L * 86_400_000L) {
                                pickedDateMillis = now - 12L * 86_400_000L
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_stage_planning")
                    )

                    OnboardingChoiceCard(
                        emoji = "🤰",
                        titleBn = "গর্ভবতী",
                        subtitleBn = "মাসিক বন্ধ হয়েছে",
                        isSelected = selectedStage == UserPreferencesManager.STAGE_POST_CONCEPTION,
                        onClick = { selectedStage = UserPreferencesManager.STAGE_POST_CONCEPTION },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("select_stage_pregnant")
                    )
                }

                // STEP 3: Menstrual Report & Symptoms
                Text(
                    text = if (selectedStage == UserPreferencesManager.STAGE_POST_CONCEPTION) {
                        "৩. কবে মাসিক বন্ধ হয়েছে ও লক্ষণ রিপোর্ট"
                    } else {
                        "৩. শেষ মাসিকের তারিখ ও চক্রের তথ্য"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Date Picker Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (selectedStage == UserPreferencesManager.STAGE_POST_CONCEPTION) {
                                        "শেষ মাসিকের প্রথম দিন (মাসিক বন্ধ)"
                                    } else {
                                        "সর্বশেষ মাসিক শুরুর তারিখ"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = HabitDateAndReportEngine.formatFullDateBn(pickedDateMillis),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            FilledTonalButton(
                                onClick = ::openLmpDatePicker,
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text("তারিখ দিন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Cycle Days Selector (26..35 days)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "মাসিক চক্রের গড় দিন: ${HabitScheduleCalculator.toBengaliDigits(selectedCycleDays.toString())} দিন",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(26, 27, 28, 29, 30, 32, 35).forEach { days ->
                                    val isChosen = selectedCycleDays == days
                                    FilterChip(
                                        selected = isChosen,
                                        onClick = { selectedCycleDays = days },
                                        label = {
                                            Text(
                                                text = "${HabitScheduleCalculator.toBengaliDigits(days.toString())} দিন",
                                                fontSize = 12.sp,
                                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // Instant Live Calculation Preview inside Onboarding!
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (selectedStage == UserPreferencesManager.STAGE_POST_CONCEPTION) {
                                    Text(
                                        text = "✨ আজ ${HabitScheduleCalculator.toBengaliDigits(liveTimeline.elapsedDays.toString())} দিন হলো (${HabitScheduleCalculator.toBengaliDigits(liveTimeline.currentWeek.toString())} সপ্তাহ)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "🎯 সম্ভাব্য ডেলিভারি: ${liveTimeline.eddDateFormattedBn}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                } else {
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
                                }
                            }
                        }

                        // Symptom Selector Chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "বর্তমান শারীরিক লক্ষণ (ট্যাপ করুন):",
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

        // Submit Button
        Button(
            onClick = {
                onCompleteSetup(
                    selectedGender,
                    selectedStage,
                    pickedDateMillis,
                    selectedCycleDays,
                    selectedSymptoms
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("complete_pregnancy_onboarding_button"),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = "ড্যাশবোর্ডে যান",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
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
    onNextQuote: () -> Unit,
    onSpeakText: (String) -> Unit,
    onToggleCheck: (String) -> Unit,
    onToggleReminderPreset: (PregnancyReminderPreset) -> Unit,
    onEditProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMaleTab by remember { mutableIntStateOf(0) } // 0=ট্রিকস, 1=পুষ্টি, 2=দায়িত্ব, 3=দোয়া

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
        contentPadding = PaddingValues(16.dp),
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
                onNextQuote = onNextQuote,
                onSpeakQuote = { onSpeakText(quote.quoteBn) }
            )
        }

        // 3. 4 Sub-Tabs: ⚡ ট্রিকস | 🍯 পুষ্টি | 🤝 দায়িত্ব | 🤲 দোয়া
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
                    3 to "🤲 দোয়া"
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
                    PregnancyReminderPresetCard(
                        preset = preset,
                        isActive = preset.id in activeHabitIds,
                        onToggle = { onToggleReminderPreset(preset) }
                    )
                }
            }
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
