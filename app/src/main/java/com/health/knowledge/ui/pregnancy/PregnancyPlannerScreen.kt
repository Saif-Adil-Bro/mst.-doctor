package com.health.knowledge.ui.pregnancy

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.health.knowledge.domain.model.Article
import com.health.knowledge.habit.HabitScheduleCalculator
import com.health.knowledge.pregnancy.FertileWindowStatus
import com.health.knowledge.pregnancy.IslamicPregnancyAmal
import com.health.knowledge.pregnancy.PregnancyChecklistItem
import com.health.knowledge.pregnancy.PregnancyPlannerEngine
import com.health.knowledge.pregnancy.PregnancyQuote
import com.health.knowledge.pregnancy.PregnancyReminderPreset
import com.health.knowledge.pregnancy.PregnancyTimelineStatus
import com.health.knowledge.ui.detail.VerificationConsentDialog
import com.health.knowledge.ui.home.AdminPasswordDialog
import com.health.knowledge.ui.home.ArticleCard
import com.health.knowledge.ui.theme.onePxBorder
import com.health.knowledge.ui.theme.solidOverSurface
import com.health.knowledge.util.UserPreferencesManager
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PregnancyPlannerScreen(
    onBack: () -> Unit,
    onOpenHabitPlanner: () -> Unit = {},
    onArticleClick: (Int) -> Unit = {},
    onAdminClick: () -> Unit = {},
    viewModel: PregnancyPlannerViewModel = hiltViewModel()
) {
    BackHandler { onBack() }

    val context = LocalContext.current
    val isOnboarded by viewModel.isOnboarded.collectAsState()
    val userGender by viewModel.userGender.collectAsState()
    val stageMode by viewModel.stageMode.collectAsState()
    val startMillis by viewModel.pregnancyStartMillis.collectAsState()
    val cycleDays by viewModel.cycleDays.collectAsState()
    val selectedSymptoms by viewModel.selectedSymptoms.collectAsState()
    val completedChecks by viewModel.completedChecks.collectAsState()
    val allHabits by viewModel.allHabits.collectAsState()
    val quoteIndex by viewModel.quoteIndex.collectAsState()
    val isAdminMode by viewModel.isAdminMode.collectAsState()
    val pregnancyArticles by viewModel.pregnancyArticles.collectAsState()
    val isRefreshingPosts by viewModel.isRefreshingPosts.collectAsState()

    // 5-click detector on Header Title for Hidden Admin Unlock (same as Mother Component)
    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var showPasswordDialog by remember { mutableStateOf(false) }
    var showAdminAddPostDialog by remember { mutableStateOf(false) }
    var articleToVerify by remember { mutableStateOf<Article?>(null) }
    var articleToDelete by remember { mutableStateOf<Article?>(null) }

    val timelineStatus = remember(startMillis, cycleDays) {
        PregnancyPlannerEngine.calculateTimelineStatus(
            startMillis = startMillis,
            cycleDays = cycleDays
        )
    }

    val fertileStatus = remember(startMillis, cycleDays) {
        PregnancyPlannerEngine.calculateFertileWindow(
            lastPeriodStartMillis = startMillis,
            cycleDays = cycleDays
        )
    }

    var selectedWeekForGrowth by remember(timelineStatus.currentWeek) {
        mutableIntStateOf(timelineStatus.currentWeek)
    }

    var postSubTab by remember { mutableIntStateOf(0) } // 0=বৃদ্ধি, 1=করণীয়, 2=দোয়া, 3=অ্যালার্ম, 4=পোস্ট

    val quotes = PregnancyPlannerEngine.emotionalAndMotivationalQuotes
    val currentQuote = quotes.getOrElse(quoteIndex % quotes.size.coerceAtLeast(1)) {
        quotes.first()
    }

    val activeHabitIds = remember(allHabits) {
        allHabits.filter { it.isActive == 1 }.map { it.id }.toSet()
    }

    if (showPasswordDialog) {
        AdminPasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onUnlockAsync = { password, onResult ->
                viewModel.verifyAdminGlobal(password) { ok ->
                    if (ok) {
                        showPasswordDialog = false
                        Toast.makeText(
                            context,
                            "🔓 অ্যাডমিন মোড আনলক হয়েছে! এখন প্রেগন্যান্সি মডিউলে পোস্ট করতে পারবেন।",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    onResult(ok)
                }
            },
            onResetPasswordAsync = { recoveryCode, newPassword, onResult ->
                viewModel.resetAdminPasswordGlobal(recoveryCode, newPassword) { ok, msg ->
                    if (ok) {
                        showPasswordDialog = false
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                    onResult(ok, msg)
                }
            }
        )
    }

    if (showAdminAddPostDialog && isAdminMode) {
        PregnancyAdminPostDialog(
            defaultStageTag = when {
                userGender == UserPreferencesManager.GENDER_MALE -> PregnancyPlannerViewModel.CAT_MALE_FATHERHOOD
                stageMode == UserPreferencesManager.STAGE_PRE_CONCEPTION -> PregnancyPlannerViewModel.CAT_PRE_CONCEPTION
                else -> PregnancyPlannerViewModel.CAT_POST_CONCEPTION
            },
            onDismiss = { showAdminAddPostDialog = false },
            onSubmit = { title, content, meds, stageTag, photoUrl, source, isPinned ->
                viewModel.submitAdminPregnancyPost(
                    title = title,
                    content = content,
                    medicines = meds,
                    stageTag = stageTag,
                    photoUrl = photoUrl,
                    source = source,
                    isPinned = isPinned
                ) { ok, msg ->
                    if (ok) {
                        showAdminAddPostDialog = false
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    articleToVerify?.let { target ->
        VerificationConsentDialog(
            article = target,
            onDismiss = { articleToVerify = null },
            onSubmit = { name, contact, comment ->
                viewModel.submitVerification(target, name, contact, comment) {
                    articleToVerify = null
                    Toast.makeText(
                        context,
                        "✅ আপনার সত্যায়ন ও ভেরিফিকেশন সফলভাবে প্রকাশিত হয়েছে!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onRemoveVerification = if (target.isVerified) {
                {
                    viewModel.removeVerification(target) {
                        articleToVerify = null
                        Toast.makeText(context, "ভেরিফিকেশন তুলে নেওয়া হয়েছে", Toast.LENGTH_SHORT).show()
                    }
                }
            } else null
        )
    }

    articleToDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { articleToDelete = null },
            title = { Text("পোস্ট ডিলিট নিশ্চিত করুন") },
            text = { Text("আপনি কি নিশ্চিত যে \"${target.title.take(50)}...\" পোস্টটি গ্লোবালি ডিলিট করতে চান?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteArticle(target)
                        articleToDelete = null
                        Toast.makeText(context, "পোস্টটি গ্লোবালি ডিলিট করা হয়েছে", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("ডিলিট করুন")
                }
            },
            dismissButton = {
                TextButton(onClick = { articleToDelete = null }) {
                    Text("বাতিল")
                }
            }
        )
    }

    fun openStartDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = timelineStatus.startMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val picked = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                viewModel.updateStartDateMillis(picked)
                Toast.makeText(context, "✅ তারিখ আপডেট হয়েছে", Toast.LENGTH_SHORT).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    fun openEddDatePicker() {
        val cal = Calendar.getInstance().apply { timeInMillis = timelineStatus.eddMillis }
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val pickedEdd = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                viewModel.updateFromExpectedDeliveryDateMillis(pickedEdd)
                Toast.makeText(context, "✅ সম্ভাব্য ডেলিভারি তারিখ আপডেট হয়েছে", Toast.LENGTH_SHORT).show()
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.testTag("pregnancy_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "ফিরে যান",
                                tint = Color.White
                            )
                        }
                        Column(
                            modifier = Modifier.clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                val now = System.currentTimeMillis()
                                if (now - lastTapTime > 2500L) {
                                    tapCount = 1
                                } else {
                                    tapCount++
                                }
                                lastTapTime = now
                                if (tapCount >= 5) {
                                    tapCount = 0
                                    if (!isAdminMode) {
                                        showPasswordDialog = true
                                    } else {
                                        onAdminClick()
                                    }
                                }
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "প্রেগন্যান্সি — আগে ও পরে",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (isAdminMode) {
                                    Surface(
                                        color = Color(0xFFFFD54F),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text(
                                            text = "অ্যাডমিন",
                                            color = Color(0xFF1B1B1B),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = when {
                                    !isOnboarded -> "প্রোফাইল সেটআপ"
                                    userGender == UserPreferencesManager.GENDER_MALE -> "👨 পুরুষ স্বাস্থ্য ও পিতৃত্ব গাইড"
                                    stageMode == UserPreferencesManager.STAGE_PRE_CONCEPTION -> "🌱 সন্তান নেওয়ার পরিকল্পনা • উর্বর সময় ট্র্যাকার"
                                    else -> "${timelineStatus.trimesterLabelBn} • ${HabitScheduleCalculator.toBengaliDigits(timelineStatus.currentWeek.toString())} সপ্তাহ (${HabitScheduleCalculator.toBengaliDigits(timelineStatus.elapsedDays.toString())} দিন)"
                                },
                                color = Color.White.copy(alpha = 0.88f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Pure Icon Buttons: Admin Add Post (Admin-Only), Profile Setup & Calendar Date
                    if (isOnboarded) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (isAdminMode) {
                                Surface(
                                    color = Color(0xFFFFD54F),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("pregnancy_admin_add_post_top_button")
                                        .clickable { showAdminAddPostDialog = true }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PostAdd,
                                            contentDescription = "অ্যাডমিন পোস্ট যোগ করুন",
                                            tint = Color(0xFF1B1B1B),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Surface(
                                color = Color.White.copy(alpha = 0.2f),
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(38.dp)
                                    .testTag("pregnancy_profile_edit_button")
                                    .clickable { viewModel.reopenOnboarding() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.ManageAccounts,
                                        contentDescription = "প্রোফাইল",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            if (userGender == UserPreferencesManager.GENDER_FEMALE) {
                                Surface(
                                    color = Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .testTag("pregnancy_calendar_button")
                                        .clickable { openStartDatePicker() }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = "তারিখ নির্বাচন",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            // Strictly Admin-Only FAB to publish pregnancy posts to Firestore
            if (isOnboarded && isAdminMode) {
                ExtendedFloatingActionButton(
                    onClick = { showAdminAddPostDialog = true },
                    icon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                    text = { Text("নতুন পোস্ট (অ্যাডমিন)", fontWeight = FontWeight.Bold) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("pregnancy_admin_fab")
                )
            }
        }
    ) { paddingValues ->
        when {
            !isOnboarded -> {
                PregnancyProfileOnboardingView(
                    initialGender = userGender,
                    initialStageMode = stageMode,
                    initialStartMillis = startMillis,
                    initialCycleDays = cycleDays,
                    initialSymptoms = selectedSymptoms,
                    onCompleteSetup = { gender, stage, lmpMillis, cycle, symptoms ->
                        viewModel.completeOnboarding(
                            gender = gender,
                            stageMode = stage,
                            startMillis = lmpMillis,
                            cycleDays = cycle,
                            symptoms = symptoms
                        )
                        Toast.makeText(context, "✅ আপনার প্রোফাইল প্রস্তুত হয়েছে!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.padding(paddingValues)
                )
            }

            userGender == UserPreferencesManager.GENDER_MALE -> {
                MaleHealthAndFatherhoodDashboard(
                    quote = currentQuote,
                    completedChecks = completedChecks,
                    activeHabitIds = activeHabitIds,
                    pregnancyPosts = pregnancyArticles,
                    isAdminMode = isAdminMode,
                    onNextQuote = viewModel::nextQuote,
                    onSpeakText = viewModel::speakBengaliPrompt,
                    onToggleCheck = viewModel::toggleCheckItem,
                    onToggleReminderPreset = { preset ->
                        viewModel.togglePregnancyReminderPreset(preset) { isNowActive ->
                            val msg = if (isNowActive) "✅ অ্যালার্ম চালু হয়েছে!" else "🔕 অ্যালার্ম বন্ধ করা হয়েছে"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onEditProfile = viewModel::reopenOnboarding,
                    onArticleClick = onArticleClick,
                    onBookmarkToggle = viewModel::toggleBookmark,
                    onReactionClick = viewModel::reactToArticle,
                    onVerifyClick = { articleToVerify = it },
                    onPinToggle = viewModel::togglePinArticle,
                    onDeleteClick = { articleToDelete = it },
                    onOpenAdminPostDialog = { showAdminAddPostDialog = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            stageMode == UserPreferencesManager.STAGE_PRE_CONCEPTION -> {
                PreConceptionStageContent(
                    fertileStatus = fertileStatus,
                    selectedSymptoms = selectedSymptoms,
                    quote = currentQuote,
                    completedChecks = completedChecks,
                    activeHabitIds = activeHabitIds,
                    pregnancyPosts = pregnancyArticles,
                    isAdminMode = isAdminMode,
                    isRefreshingPosts = isRefreshingPosts,
                    onPickLastPeriodDate = ::openStartDatePicker,
                    onToggleSymptom = viewModel::toggleSymptom,
                    onNextQuote = viewModel::nextQuote,
                    onSpeakText = viewModel::speakBengaliPrompt,
                    onToggleCheck = viewModel::toggleCheckItem,
                    onToggleReminderPreset = { preset ->
                        viewModel.togglePregnancyReminderPreset(preset) { isNowActive ->
                            val msg = if (isNowActive) "✅ অ্যালার্ম চালু হয়েছে!" else "🔕 অ্যালার্ম বন্ধ করা হয়েছে"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onSwitchToPostConception = {
                        viewModel.setStageMode(UserPreferencesManager.STAGE_POST_CONCEPTION)
                    },
                    onRefreshPosts = viewModel::syncPregnancyPostsFromFirestore,
                    onArticleClick = onArticleClick,
                    onBookmarkToggle = viewModel::toggleBookmark,
                    onReactionClick = viewModel::reactToArticle,
                    onVerifyClick = { articleToVerify = it },
                    onPinToggle = viewModel::togglePinArticle,
                    onDeleteClick = { articleToDelete = it },
                    onOpenAdminPostDialog = { showAdminAddPostDialog = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }

            else -> {
                PostConceptionStageContent(
                    timelineStatus = timelineStatus,
                    selectedSymptoms = selectedSymptoms,
                    selectedWeek = selectedWeekForGrowth,
                    onSelectWeek = { selectedWeekForGrowth = it.coerceIn(1, 40) },
                    postSubTab = postSubTab,
                    onSelectSubTab = { postSubTab = it },
                    quote = currentQuote,
                    completedChecks = completedChecks,
                    activeHabitIds = activeHabitIds,
                    pregnancyPosts = pregnancyArticles,
                    isAdminMode = isAdminMode,
                    isRefreshingPosts = isRefreshingPosts,
                    onPickStartDate = ::openStartDatePicker,
                    onPickEddDate = ::openEddDatePicker,
                    onToggleSymptom = viewModel::toggleSymptom,
                    onNextQuote = viewModel::nextQuote,
                    onSpeakText = viewModel::speakBengaliPrompt,
                    onToggleCheck = viewModel::toggleCheckItem,
                    onToggleReminderPreset = { preset ->
                        viewModel.togglePregnancyReminderPreset(preset) { isNowActive ->
                            val msg = if (isNowActive) "✅ অ্যালার্ম চালু হয়েছে!" else "🔕 অ্যালার্ম বন্ধ করা হয়েছে"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onOpenHabitPlanner = onOpenHabitPlanner,
                    onRefreshPosts = viewModel::syncPregnancyPostsFromFirestore,
                    onArticleClick = onArticleClick,
                    onBookmarkToggle = viewModel::toggleBookmark,
                    onReactionClick = viewModel::reactToArticle,
                    onVerifyClick = { articleToVerify = it },
                    onPinToggle = viewModel::togglePinArticle,
                    onDeleteClick = { articleToDelete = it },
                    onOpenAdminPostDialog = { showAdminAddPostDialog = true },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

/**
 * ── FEMALE MODE 1: সন্তান নেওয়ার আগে করণীয়, উর্বর সময় (Fertile Window), লক্ষণ, দোয়া ও ফায়ারস্টোর পোস্ট ──
 */
@Composable
private fun PreConceptionStageContent(
    fertileStatus: FertileWindowStatus,
    selectedSymptoms: Set<String>,
    quote: PregnancyQuote,
    completedChecks: Set<String>,
    activeHabitIds: Set<String>,
    pregnancyPosts: List<Article>,
    isAdminMode: Boolean,
    isRefreshingPosts: Boolean,
    onPickLastPeriodDate: () -> Unit,
    onToggleSymptom: (String) -> Unit,
    onNextQuote: () -> Unit,
    onSpeakText: (String) -> Unit,
    onToggleCheck: (String) -> Unit,
    onToggleReminderPreset: (PregnancyReminderPreset) -> Unit,
    onSwitchToPostConception: () -> Unit,
    onRefreshPosts: () -> Unit,
    onArticleClick: (Int) -> Unit,
    onBookmarkToggle: (Int, Boolean) -> Unit,
    onReactionClick: (Article) -> Unit,
    onVerifyClick: (Article) -> Unit,
    onPinToggle: (Article) -> Unit,
    onDeleteClick: (Article) -> Unit,
    onOpenAdminPostDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preChecklist = PregnancyPlannerEngine.preConceptionChecklist
    val preDuas = remember {
        PregnancyPlannerEngine.islamicAmalsAndDuas.filter { it.stageTag == "PRE" }
    }
    val doneCount = preChecklist.count { it.id in completedChecks }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 84.dp, top = 16.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Fertile Window & Ovulation Hero Card
        item(key = "pre_fertile_window_hero") {
            FertileWindowHeroCard(
                fertileStatus = fertileStatus,
                doneCount = doneCount,
                totalCount = preChecklist.size,
                onPickLastPeriodDate = onPickLastPeriodDate,
                onSwitchToPostConception = onSwitchToPostConception
            )
        }

        // 2. Symptom Report & Personalized Advice Card
        item(key = "pre_symptom_advice_card") {
            SymptomAdviceSectionCard(
                selectedSymptoms = selectedSymptoms,
                onToggleSymptom = onToggleSymptom,
                onSpeakAdvice = onSpeakText
            )
        }

        // 3. Emotional & Spiritual Quote Card
        item(key = "pre_quote_card") {
            PregnancyQuoteCard(
                quote = quote,
                onNextQuote = onNextQuote,
                onSpeakQuote = { onSpeakText(quote.quoteBn) }
            )
        }

        // 4. Pre-Conception Checklist Header & Items
        item(key = "pre_checklist_header") {
            Text(
                text = "✅ সন্তান নেওয়ার আগে করণীয়",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(preChecklist, key = { it.id }) { item ->
            PregnancyChecklistRowCard(
                item = item,
                isChecked = item.id in completedChecks,
                isDontItem = false,
                onToggle = { onToggleCheck(item.id) }
            )
        }

        // 5. Pre-Conception Islamic Duas
        item(key = "pre_duas_header") {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "🤲 সন্তান লাভের কুরআনি দোয়া ও আমল",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(preDuas, key = { it.id }) { amal ->
            IslamicPregnancyAmalCard(
                amal = amal,
                onSpeakClick = {
                    onSpeakText("${amal.titleBn}। উচ্চারণ: ${amal.pronunciationBn}। অর্থ: ${amal.meaningBn}")
                }
            )
        }

        // 6. Pre-Conception Reminders
        item(key = "pre_reminders_header") {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "🔔 প্রস্তুতি রিমাইন্ডার",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        items(PregnancyPlannerEngine.reminderPresets, key = { "pre_rem_${it.id}" }) { preset ->
            PregnancyReminderPresetCard(
                preset = preset,
                isActive = preset.id in activeHabitIds,
                onToggle = { onToggleReminderPreset(preset) }
            )
        }

        // 7. Firestore-Synced Pregnancy Posts Section (Admin-Only Post Creation)
        item(key = "pre_firestore_posts_header") {
            Spacer(Modifier.height(6.dp))
            PregnancyPostsHeaderBar(
                totalCount = pregnancyPosts.size,
                isAdminMode = isAdminMode,
                isRefreshing = isRefreshingPosts,
                onRefresh = onRefreshPosts,
                onOpenAdminPostDialog = onOpenAdminPostDialog
            )
        }

        if (pregnancyPosts.isEmpty()) {
            item(key = "pre_posts_empty") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    border = onePxBorder(MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = "এখনো কোনো প্রেগন্যান্সি পোস্ট যুক্ত করা হয়নি।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(pregnancyPosts, key = { "pre_post_${it.id}" }) { article ->
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

@Composable
private fun FertileWindowHeroCard(
    fertileStatus: FertileWindowStatus,
    doneCount: Int,
    totalCount: Int,
    onPickLastPeriodDate: () -> Unit,
    onSwitchToPostConception: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fertile_window_hero_card"),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "🌸 সন্তান নেওয়ার পরিকল্পনা ও উর্বর সময়",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = fertileStatus.statusHeadlineBn,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50),
                    border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${HabitScheduleCalculator.toBengaliDigits(doneCount.toString())}/${HabitScheduleCalculator.toBengaliDigits(totalCount.toString())} সম্পন্ন",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "🎯 সম্ভাব্য উর্বর সময়: ${fertileStatus.fertileStartFormattedBn} – ${fertileStatus.fertileEndFormattedBn}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "সম্ভাব্য ওভুলেশন: ${fertileStatus.ovulationDateFormattedBn}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "শেষ মাসিক: ${fertileStatus.lastPeriodFormattedBn} • চক্র: ${HabitScheduleCalculator.toBengaliDigits(fertileStatus.cycleDays.toString())} দিন (আজ ${HabitScheduleCalculator.toBengaliDigits(fertileStatus.currentCycleDay.toString())}তম দিন)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPickLastPeriodDate,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text("মাসিকের তারিখ", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onSwitchToPostConception,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text("গর্ভবতী হয়েছি", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * ── FEMALE MODE 2: গর্ভধারণের পরে (সম্ভাব্য ডেলিভারি তারিখ, আজ কত দিন হলো, লক্ষণ পরামর্শ, বৃদ্ধি, করণীয়-বর্জনীয়, দোয়া ও ফায়ারস্টোর পোস্ট) ──
 */
@Composable
private fun PostConceptionStageContent(
    timelineStatus: PregnancyTimelineStatus,
    selectedSymptoms: Set<String>,
    selectedWeek: Int,
    onSelectWeek: (Int) -> Unit,
    postSubTab: Int,
    onSelectSubTab: (Int) -> Unit,
    quote: PregnancyQuote,
    completedChecks: Set<String>,
    activeHabitIds: Set<String>,
    pregnancyPosts: List<Article>,
    isAdminMode: Boolean,
    isRefreshingPosts: Boolean,
    onPickStartDate: () -> Unit,
    onPickEddDate: () -> Unit,
    onToggleSymptom: (String) -> Unit,
    onNextQuote: () -> Unit,
    onSpeakText: (String) -> Unit,
    onToggleCheck: (String) -> Unit,
    onToggleReminderPreset: (PregnancyReminderPreset) -> Unit,
    onOpenHabitPlanner: () -> Unit,
    onRefreshPosts: () -> Unit,
    onArticleClick: (Int) -> Unit,
    onBookmarkToggle: (Int, Boolean) -> Unit,
    onReactionClick: (Article) -> Unit,
    onVerifyClick: (Article) -> Unit,
    onPinToggle: (Article) -> Unit,
    onDeleteClick: (Article) -> Unit,
    onOpenAdminPostDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    var doDontFilter by remember { mutableStateOf("DO") } // "DO", "DONT", "SUNNAH"
    var duaTrimesterFilter by remember { mutableIntStateOf(0) } // 0=সব, 1=১ম, 2=২য়, 3=৩য়
    var postStageFilter by remember { mutableStateOf("ALL") } // "ALL", "PRE", "POST", "MALE"

    val filteredPregnancyPosts = remember(pregnancyPosts, postStageFilter) {
        when (postStageFilter) {
            "PRE" -> pregnancyPosts.filter { it.categories.contains(PregnancyPlannerViewModel.CAT_PRE_CONCEPTION) }
            "POST" -> pregnancyPosts.filter {
                it.categories.contains(PregnancyPlannerViewModel.CAT_POST_CONCEPTION) ||
                    (!it.categories.contains(PregnancyPlannerViewModel.CAT_PRE_CONCEPTION) &&
                        !it.categories.contains(PregnancyPlannerViewModel.CAT_MALE_FATHERHOOD))
            }
            "MALE" -> pregnancyPosts.filter { it.categories.contains(PregnancyPlannerViewModel.CAT_MALE_FATHERHOOD) }
            else -> pregnancyPosts
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 84.dp, top = 16.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Expected Delivery Date & "Today How Many Days" Hero Card
        item(key = "post_countdown_hero") {
            PregnancyCountdownHeroCard(
                status = timelineStatus,
                onPickStartDate = onPickStartDate,
                onPickEddDate = onPickEddDate
            )
        }

        // 2. Interactive Symptom Report & Personalized Care Card
        item(key = "post_symptom_advice_card") {
            SymptomAdviceSectionCard(
                selectedSymptoms = selectedSymptoms,
                onToggleSymptom = onToggleSymptom,
                onSpeakAdvice = onSpeakText
            )
        }

        // 3. Heartwarming Emotional & Spiritual Quote Card
        item(key = "post_quote_card") {
            PregnancyQuoteCard(
                quote = quote,
                onNextQuote = onNextQuote,
                onSpeakQuote = { onSpeakText(quote.quoteBn) }
            )
        }

        // 4. 5 Sub-Tab Pills: 👶 বৃদ্ধি | ✓ করণীয় | 🤲 দোয়া | 🔔 অ্যালার্ম | 📝 পোস্ট
        item(key = "post_sub_tabs") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    0 to "👶 বৃদ্ধি",
                    1 to "✓ করণীয়",
                    2 to "🤲 দোয়া",
                    3 to "🔔 অ্যালার্ম",
                    4 to "📝 পোস্ট (${HabitScheduleCalculator.toBengaliDigits(pregnancyPosts.size.toString())})"
                ).forEach { (idx, label) ->
                    FilterChip(
                        selected = postSubTab == idx,
                        onClick = { onSelectSubTab(idx) },
                        label = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (postSubTab == idx) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.testTag("preg_subtab_$idx")
                    )
                }
            }
        }

        when (postSubTab) {
            0 -> {
                // Weekly Baby Growth (Weeks 1..40) + Today's Quick Do's & Don'ts Preview
                item(key = "weekly_growth_card") {
                    WeeklyBabyGrowthSectionCard(
                        currentWeek = timelineStatus.currentWeek,
                        selectedWeek = selectedWeek,
                        onSelectWeek = onSelectWeek,
                        onSpeakGrowth = onSpeakText
                    )
                }

                item(key = "today_quick_do_dont_header") {
                    Text(
                        text = "🌟 আজকের প্রধান করণীয় ও বর্জনীয়",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                val topDos = PregnancyPlannerEngine.pregnancyDosAndDonts.filter { it.category == "DO" }.take(3)
                items(topDos, key = { "quick_do_${it.id}" }) { item ->
                    PregnancyChecklistRowCard(
                        item = item,
                        isChecked = item.id in completedChecks,
                        isDontItem = false,
                        onToggle = { onToggleCheck(item.id) }
                    )
                }

                val topDonts = PregnancyPlannerEngine.pregnancyDosAndDonts.filter { it.category == "DONT" }.take(2)
                items(topDonts, key = { "quick_dont_${it.id}" }) { item ->
                    PregnancyChecklistRowCard(
                        item = item,
                        isChecked = item.id in completedChecks,
                        isDontItem = true,
                        onToggle = { onToggleCheck(item.id) }
                    )
                }

                // Also show latest Firestore Pregnancy Posts preview at bottom of Growth tab
                if (pregnancyPosts.isNotEmpty() || isAdminMode) {
                    item(key = "growth_tab_posts_preview_header") {
                        Spacer(Modifier.height(4.dp))
                        PregnancyPostsHeaderBar(
                            totalCount = pregnancyPosts.size,
                            isAdminMode = isAdminMode,
                            isRefreshing = isRefreshingPosts,
                            onRefresh = onRefreshPosts,
                            onOpenAdminPostDialog = onOpenAdminPostDialog
                        )
                    }

                    items(pregnancyPosts.take(5), key = { "growth_post_preview_${it.id}" }) { article ->
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

            1 -> {
                // Full Do's, Don'ts & Newborn Sunnah Checklist
                item(key = "do_dont_filter_chips") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "DO" to "✓ করণীয়",
                            "DONT" to "✕ বর্জনীয়",
                            "SUNNAH" to "👶 নবজাতক সুন্নাহ"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = doDontFilter == key,
                                onClick = { doDontFilter = key },
                                label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }
                }

                val listToShow = when (doDontFilter) {
                    "DONT" -> PregnancyPlannerEngine.pregnancyDosAndDonts.filter { it.category == "DONT" }
                    "SUNNAH" -> PregnancyPlannerEngine.newbornSunnahChecklist
                    else -> PregnancyPlannerEngine.pregnancyDosAndDonts.filter { it.category == "DO" }
                }

                items(listToShow, key = { it.id }) { item ->
                    PregnancyChecklistRowCard(
                        item = item,
                        isChecked = item.id in completedChecks,
                        isDontItem = item.category == "DONT",
                        onToggle = { onToggleCheck(item.id) }
                    )
                }
            }

            2 -> {
                // Islamic Pregnancy Duas & Surahs by Trimester
                item(key = "dua_trimester_filter") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            0 to "সব দোয়া",
                            1 to "১ম ত্রৈমাসিক",
                            2 to "২য় ত্রৈমাসিক",
                            3 to "৩য় ও প্রসব"
                        ).forEach { (idx, label) ->
                            FilterChip(
                                selected = duaTrimesterFilter == idx,
                                onClick = { duaTrimesterFilter = idx },
                                label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }
                }

                val filteredDuas = when (duaTrimesterFilter) {
                    1 -> PregnancyPlannerEngine.islamicAmalsAndDuas.filter {
                        it.stageTag == "TRIMESTER_1" || it.stageTag == "PRE"
                    }
                    2 -> PregnancyPlannerEngine.islamicAmalsAndDuas.filter {
                        it.stageTag == "TRIMESTER_2"
                    }
                    3 -> PregnancyPlannerEngine.islamicAmalsAndDuas.filter {
                        it.stageTag == "TRIMESTER_3" || it.stageTag == "POST_BIRTH"
                    }
                    else -> PregnancyPlannerEngine.islamicAmalsAndDuas
                }

                items(filteredDuas, key = { it.id }) { amal ->
                    IslamicPregnancyAmalCard(
                        amal = amal,
                        onSpeakClick = {
                            onSpeakText("${amal.titleBn}। উচ্চারণ: ${amal.pronunciationBn}। অর্থ: ${amal.meaningBn}")
                        }
                    )
                }
            }

            3 -> {
                // Pregnancy Checkup & Medicine Reminders
                item(key = "reminders_intro") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔔 গর্ভকালীন অ্যালার্ম ও চেকআপ",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = onOpenHabitPlanner) {
                            Text("কাস্টম টাইম", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(PregnancyPlannerEngine.reminderPresets, key = { it.id }) { preset ->
                    PregnancyReminderPresetCard(
                        preset = preset,
                        isActive = preset.id in activeHabitIds,
                        onToggle = { onToggleReminderPreset(preset) }
                    )
                }
            }

            4 -> {
                // Dedicated Firestore Pregnancy Posts Tab (Admin-Only Post Creation)
                item(key = "posts_tab_header") {
                    PregnancyPostsHeaderBar(
                        totalCount = filteredPregnancyPosts.size,
                        isAdminMode = isAdminMode,
                        isRefreshing = isRefreshingPosts,
                        onRefresh = onRefreshPosts,
                        onOpenAdminPostDialog = onOpenAdminPostDialog
                    )
                }

                item(key = "posts_stage_filter_row") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ALL" to "সব পোস্ট",
                            "PRE" to "🌱 সন্তান নেওয়ার আগে",
                            "POST" to "🤰 গর্ভকালীন ও প্রসব",
                            "MALE" to "👨 পিতৃত্ব গাইড"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = postStageFilter == key,
                                onClick = { postStageFilter = key },
                                label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                            )
                        }
                    }
                }

                if (filteredPregnancyPosts.isEmpty()) {
                    item(key = "posts_tab_empty") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            border = onePxBorder(MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("📝", fontSize = 32.sp)
                                Text(
                                    text = "এই বিভাগে এখনো কোনো পোস্ট যুক্ত করা হয়নি।",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                if (isAdminMode) {
                                    Button(
                                        onClick = onOpenAdminPostDialog,
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("প্রথম পোস্ট প্রকাশ করুন")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredPregnancyPosts, key = { "preg_post_${it.id}" }) { article ->
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
}

@Composable
internal fun PregnancyPostsHeaderBar(
    totalCount: Int,
    isAdminMode: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenAdminPostDialog: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f).solidOverSurface()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.38f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "📝 মাতৃত্ব ও প্রেগন্যান্সি পোস্ট (${HabitScheduleCalculator.toBengaliDigits(totalCount.toString())}টি)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isAdminMode) {
                        "🔓 অ্যাডমিন মোড চালু • নতুন পোস্ট সরাসরি Firestore-এ যুক্ত করুন"
                    } else {
                        "☁️ Firestore-সিঙ্কড যাচাইকৃত পোস্ট (শুধুমাত্র অ্যাডমিন পোস্ট করতে পারবেন)"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilledTonalIconButton(
                    onClick = onRefresh,
                    enabled = !isRefreshing,
                    modifier = Modifier.size(34.dp)
                ) {
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Sync,
                            contentDescription = "Firestore সিঙ্ক",
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }

                if (isAdminMode) {
                    Button(
                        onClick = onOpenAdminPostDialog,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("পোস্ট", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Admin-Only Dialog to create & publish a new Pregnancy post directly to Firestore.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PregnancyAdminPostDialog(
    defaultStageTag: String,
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        content: String,
        medicines: String,
        stageTag: String,
        photoUrl: String,
        source: String,
        isPinned: Boolean
    ) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var medicines by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }
    var selectedStageTag by remember { mutableStateOf(defaultStageTag) }
    var isPinned by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    val stageOptions = listOf(
        PregnancyPlannerViewModel.CAT_PRE_CONCEPTION to "🌱 সন্তান নেওয়ার আগে",
        PregnancyPlannerViewModel.CAT_POST_CONCEPTION to "🤰 গর্ভকালীন ও প্রসব",
        PregnancyPlannerViewModel.CAT_MALE_FATHERHOOD to "👨 পুরুষ ও পিতৃত্ব গাইড",
        PregnancyPlannerViewModel.CAT_PREGNANCY_ROOT to "🌸 সকল পর্যায়ে"
    )

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            border = onePxBorder(),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🤰", fontSize = 22.sp)
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                "প্রেগন্যান্সি পোস্ট যোগ করুন (অ্যাডমিন)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "শুধুমাত্র অ্যাডমিন • সরাসরি Firestore-এ পাবলিশ হবে",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, enabled = !isSubmitting) {
                        Icon(Icons.Default.Close, null)
                    }
                }

                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "কোন পর্যায়ের জন্য পোস্টটি প্রযোজ্য?",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            stageOptions.forEach { (tag, label) ->
                                FilterChip(
                                    selected = selectedStageTag == tag,
                                    onClick = { selectedStageTag = tag },
                                    label = { Text(label, fontSize = 12.sp) }
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("শিরোনাম *") },
                            placeholder = { Text("যেমন: গর্ভকালীন প্রথম ৩ মাসে খাবার ও সতর্কতা") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = content,
                            onValueChange = { content = it },
                            label = { Text("বিস্তারিত বিবরণ ও পরামর্শ *") },
                            placeholder = { Text("গর্ভবতী মা বা দম্পতির করণীয়, পুষ্টি ও সমাধান বিস্তারিত লিখুন...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 4,
                            maxLines = 9
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = medicines,
                            onValueChange = { medicines = it },
                            label = { Text("পুষ্টিকর খাবার / ভেষজ উপাদান (ঐচ্ছিক)") },
                            placeholder = { Text("কমা দিয়ে লিখুন, যেমন: খেজুর, দুধ, ডালিম, ফলিক অ্যাসিড") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = source,
                            onValueChange = { source = it },
                            label = { Text("📚 তথ্যসূত্র / সোর্স (ঐচ্ছিক)") },
                            placeholder = { Text("যেমন: স্ত্রীরোগ বিশেষজ্ঞ / বিশ্ব স্বাস্থ্য সংস্থা / কুরআন-হাদিস") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = photoUrl,
                            onValueChange = { photoUrl = it },
                            label = { Text("🖼️ কভার ফটো CDN লিংক (ঐচ্ছিক)") },
                            placeholder = { Text("https://example.com/pregnancy-care.jpg") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            border = onePxBorder(MaterialTheme.colorScheme.outlineVariant),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("📌 সবার উপরে পিন করে রাখুন", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        "পিন করলে প্রেগন্যান্সি মডিউলে সবার উপরে দেখাবে",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Switch(checked = isPinned, onCheckedChange = { isPinned = it })
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        isSubmitting = true
                        onSubmit(title, content, medicines, selectedStageTag, photoUrl, source, isPinned)
                    },
                    enabled = title.isNotBlank() && content.isNotBlank() && !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Firestore-এ আপলোড হচ্ছে...")
                    } else {
                        Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Firestore-এ প্রকাশ করুন", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * Interactive Symptom Report & Instant Personalized Care Advice Card.
 */
@Composable
private fun SymptomAdviceSectionCard(
    selectedSymptoms: Set<String>,
    onToggleSymptom: (String) -> Unit,
    onSpeakAdvice: (String) -> Unit
) {
    val activeSymptomObjects = remember(selectedSymptoms) {
        val matched = PregnancyPlannerEngine.symptomOptions.filter { it.id in selectedSymptoms }
        matched.ifEmpty { listOf(PregnancyPlannerEngine.symptomOptions.first()) }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("symptom_advice_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f).solidOverSurface()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
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
                Text(
                    text = "🩺 আজ আপনার শারীরিক অনুভূতি কেমন?",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                val firstAdvice = activeSymptomObjects.first()
                FilledTonalIconButton(
                    onClick = {
                        onSpeakAdvice("${firstAdvice.adviceTitleBn}। ${firstAdvice.adviceDetailBn}")
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "শুনুন",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PregnancyPlannerEngine.symptomOptions.forEach { sym ->
                    val isSelected = sym.id in selectedSymptoms
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggleSymptom(sym.id) },
                        label = {
                            Text(
                                text = "${sym.emoji} ${sym.labelBn}",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    )
                }
            }

            activeSymptomObjects.forEach { sym ->
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(12.dp),
                    border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "${sym.emoji} ${sym.adviceTitleBn}",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = sym.adviceDetailBn,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Hero Card displaying:
 * - Animated Progress Ring (% & week/40)
 * - আজ কত দিন হলো (Elapsed Days & Weeks+Days)
 * - সম্ভাব্য ডেলিভারির তারিখ (EDD) & কত দিন বাকি (Remaining Days)
 * - Buttons to set মাসিকের তারিখ or ডেলিভারি তারিখ
 */
@Composable
private fun PregnancyCountdownHeroCard(
    status: PregnancyTimelineStatus,
    onPickStartDate: () -> Unit,
    onPickEddDate: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = status.progressFraction,
        animationSpec = tween(durationMillis = 500),
        label = "pregnancy_progress_ring"
    )

    val elapsedDaysBn = HabitScheduleCalculator.toBengaliDigits(status.elapsedDays.toString())
    val remainingDaysBn = HabitScheduleCalculator.toBengaliDigits(status.remainingDays.toString())
    val weekBn = HabitScheduleCalculator.toBengaliDigits(status.currentWeek.toString())
    val extraDaysBn = HabitScheduleCalculator.toBengaliDigits(status.extraDaysInWeek.toString())
    val percentBn = HabitScheduleCalculator.toBengaliDigits(status.progressPercent.toString())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pregnancy_countdown_hero_card"),
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Left: Progress Ring
                Box(
                    modifier = Modifier.size(74.dp),
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
                            text = "$percentBn%",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "$weekBn/৪০ সপ্তাহ",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Right: Elapsed Days, Current Week & Remaining Days
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = status.trimesterLabelBn,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "$remainingDaysBn দিন বাকি",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    Text(
                        text = "আজ $elapsedDaysBn দিন হলো ($weekBn সপ্তাহ $extraDaysBn দিন)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "🎯 সম্ভাব্য ডেলিভারি: ${status.eddDateFormattedBn}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Date Picker Action Row (1-2 word Bengali buttons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPickStartDate,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_conception_date_button"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "মাসিকের তারিখ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onPickEddDate,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("pick_edd_date_button"),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = "ডেলিভারি তারিখ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Weekly Baby Growth Card with 1..40 Week Horizontal Selector and Quranic Creation Reflection.
 */
@Composable
private fun WeeklyBabyGrowthSectionCard(
    currentWeek: Int,
    selectedWeek: Int,
    onSelectWeek: (Int) -> Unit,
    onSpeakGrowth: (String) -> Unit
) {
    val growth = remember(selectedWeek) {
        PregnancyPlannerEngine.getGrowthForWeek(selectedWeek)
    }
    val weekListState = rememberLazyListState(
        initialFirstVisibleItemIndex = (selectedWeek - 3).coerceAtLeast(0)
    )

    LaunchedEffect(selectedWeek) {
        weekListState.animateScrollToItem((selectedWeek - 3).coerceAtLeast(0))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_baby_growth_card"),
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header with pure icon prev/next week buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onSelectWeek(selectedWeek - 1) },
                        enabled = selectedWeek > 1,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "আগের সপ্তাহ")
                    }
                    Text(
                        text = "👶 সপ্তাহ ${HabitScheduleCalculator.toBengaliDigits(selectedWeek.toString())}-এ শিশুর বৃদ্ধি",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(
                        onClick = { onSelectWeek(selectedWeek + 1) },
                        enabled = selectedWeek < 40,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "পরের সপ্তাহ")
                    }
                }

                // Pure Icon Button for TTS Voice
                FilledTonalIconButton(
                    onClick = {
                        onSpeakGrowth("${growth.milestoneTitleBn}। ${growth.developmentDetailBn} ${growth.spiritualReflectionBn}")
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "শুনুন",
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            // Horizontal Scrollable Week Selector (1..40)
            LazyRow(
                state = weekListState,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items((1..40).toList(), key = { it }) { weekNum ->
                    val isSelected = weekNum == selectedWeek
                    val isCurrent = weekNum == currentWeek
                    Surface(
                        color = when {
                            isSelected -> MaterialTheme.colorScheme.primary
                            isCurrent -> MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = onePxBorder(
                            if (isSelected || isCurrent) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.clickable { onSelectWeek(weekNum) }
                    ) {
                        Text(
                            text = HabitScheduleCalculator.toBengaliDigits(weekNum.toString()),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.onPrimary
                                isCurrent -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Baby Size, Weight & Length Row
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(14.dp),
                border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(52.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(growth.emoji, fontSize = 26.sp)
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "আকার: ${growth.sizeComparisonBn}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = growth.milestoneTitleBn,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "উচ্চতা: ${growth.lengthBn}  •  ওজন: ${growth.weightBn}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = growth.developmentDetailBn,
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
                    text = "✨ ${growth.spiritualReflectionBn}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(10.dp)
                )
            }
        }
    }
}

/**
 * Heartwarming Emotional & Spiritual Quote Card with Pure Icon Buttons for Voice & Next.
 */
@Composable
internal fun PregnancyQuoteCard(
    quote: PregnancyQuote,
    onNextQuote: () -> Unit,
    onSpeakQuote: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("pregnancy_quote_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.28f).solidOverSurface()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = onePxBorder(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
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
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "💖 ${quote.tagBn}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }

                // Pure Icon Buttons (Zero text next to common icons)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalIconButton(
                        onClick = onSpeakQuote,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.VolumeUp,
                            contentDescription = "শুনুন",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    FilledTonalIconButton(
                        onClick = onNextQuote,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "পরবর্তী বাণী",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Text(
                text = quote.quoteBn,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )

            Text(
                text = "— ${quote.sourceBn}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
internal fun PregnancyChecklistRowCard(
    item: PregnancyChecklistItem,
    isChecked: Boolean,
    isDontItem: Boolean,
    onToggle: () -> Unit
) {
    val accentColor = when {
        isDontItem -> Color(0xFFC62828)
        isChecked -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f).solidOverSurface()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = onePxBorder(accentColor.copy(alpha = if (isChecked) 0.55f else 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(checkedColor = accentColor)
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        color = accentColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (isDontItem) "বর্জনীয়" else "করণীয়",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = item.titleBn,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = item.subtitleBn,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
internal fun IslamicPregnancyAmalCard(
    amal: IslamicPregnancyAmal,
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
                Column(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = amal.stageBadgeBn,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = amal.titleBn,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Pure Icon Button for Voice
                FilledTonalIconButton(
                    onClick = onSpeakClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.VolumeUp,
                        contentDescription = "শুনুন",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
                border = onePxBorder(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = amal.arabicText,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Right,
                    lineHeight = 30.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                )
            }

            Text(
                text = "উচ্চারণ: ${amal.pronunciationBn}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )

            Text(
                text = "অর্থ: ${amal.meaningBn}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            Text(
                text = "📖 ${amal.referenceBn} • ${amal.benefitBn}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
internal fun PregnancyReminderPresetCard(
    preset: PregnancyReminderPreset,
    isActive: Boolean,
    onToggle: () -> Unit
) {
    val (h, m) = HabitScheduleCalculator.parseAnchorTime(preset.anchorTime)
    val timeBn = HabitScheduleCalculator.formatHourMinuteBengali(h, m)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f).solidOverSurface()
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = onePxBorder(
            if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(preset.emoji, fontSize = 19.sp)
                    }
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.titleBn,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = timeBn,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Switch(
                checked = isActive,
                onCheckedChange = { onToggle() }
            )
        }
    }
}
