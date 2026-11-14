package com.health.knowledge

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.health.knowledge.habit.HabitAlarmReceiver
import com.health.knowledge.sync.DailyTipNotificationWorker
import com.health.knowledge.sync.SyncWorker
import com.health.knowledge.ui.admin.AdminScreen
import com.health.knowledge.ui.detail.DetailScreen
import com.health.knowledge.ui.habit.HabitReminderScreen
import com.health.knowledge.ui.home.HomeScreen
import com.health.knowledge.ui.pregnancy.PregnancyPlannerScreen
import com.health.knowledge.ui.tibbenabawi.TibbeNabawiDetailScreen
import com.health.knowledge.ui.tibbenabawi.TibbeNabawiScreen
import com.health.knowledge.ui.theme.HealthKnowledgeTheme
import com.health.knowledge.util.HealthNotificationHelper
import com.health.knowledge.util.UserPreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var prefsManager: UserPreferencesManager

    private val targetArticleIdState = mutableStateOf<Int?>(null)
    private val targetTibbeIdState = mutableStateOf<Int?>(null)
    private val openHabitModuleState = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Notification Channels and Subscribe to "all_users" push topic
        HealthNotificationHelper.createChannels(this)
        HabitAlarmReceiver.ensureHabitChannels(this)
        HealthNotificationHelper.subscribeToAllUsersTopic()

        // Schedule background sync every 6 hours (only when internet available)
        SyncWorker.schedule(this)
        // Incremental check for new posts on fresh launch (throttled to avoid redundant work on config changes/reopens)
        if (savedInstanceState == null) {
            val syncPrefs = getSharedPreferences("sync_prefs", MODE_PRIVATE)
            val lastSync = syncPrefs.getLong("last_sync_timestamp", 0L)
            val fifteenMinutesMs = 15 * 60 * 1000L
            if (lastSync == 0L || System.currentTimeMillis() - lastSync >= fifteenMinutesMs) {
                SyncWorker.syncNow(this)
            }
        }
        // Schedule daily health tip reminder if enabled (default 8:00 AM or user-customized)
        DailyTipNotificationWorker.scheduleOrCancel(
            context = this,
            enabled = prefsManager.dailyReminderEnabled.value,
            targetHour = prefsManager.dailyReminderHour.value,
            targetMinute = prefsManager.dailyReminderMinute.value
        )

        val initialId = intent?.getIntExtra("open_article_id", -1) ?: -1
        val initialTibbeId = intent?.getIntExtra("open_tibbe_id", -1) ?: -1
        if (initialTibbeId > 0) {
            targetTibbeIdState.value = initialTibbeId
        } else if (initialId < -1) {
            targetTibbeIdState.value = -initialId
        } else if (initialId > 0) {
            targetArticleIdState.value = initialId
        }
        if (intent?.getBooleanExtra("open_habit_module", false) == true) {
            openHabitModuleState.value = true
        }

        setContent {
            val themeMode by prefsManager.themeMode.collectAsState()

            HealthKnowledgeTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    val targetId by targetArticleIdState
                    val targetTibbeId by targetTibbeIdState
                    val openHabitModule by openHabitModuleState

                    LaunchedEffect(targetId) {
                        targetId?.let { id ->
                            if (id > 0) {
                                navController.navigate("detail/$id")
                                targetArticleIdState.value = null
                            }
                        }
                    }

                    LaunchedEffect(targetTibbeId) {
                        targetTibbeId?.let { id ->
                            if (id > 0) {
                                navController.navigate("tibbe_nabawi_detail/$id")
                                targetTibbeIdState.value = null
                            }
                        }
                    }

                    LaunchedEffect(openHabitModule) {
                        if (openHabitModule) {
                            navController.navigate("habit_reminder")
                            openHabitModuleState.value = false
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "home",
                        enterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(durationMillis = 280))
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> -fullWidth / 4 },
                                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(durationMillis = 220))
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                initialOffsetX = { fullWidth -> -fullWidth / 4 },
                                animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
                            ) + fadeIn(animationSpec = tween(durationMillis = 280))
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                targetOffsetX = { fullWidth -> fullWidth },
                                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                            ) + fadeOut(animationSpec = tween(durationMillis = 260))
                        }
                    ) {
                        composable("home") {
                            HomeScreen(
                                onArticleClick = { id ->
                                    if (id < 0) {
                                        navController.navigate("tibbe_nabawi_detail/${-id}")
                                    } else {
                                        navController.navigate("detail/$id")
                                    }
                                },
                                onAdminClick = {
                                    navController.navigate("admin")
                                },
                                onHabitReminderClick = {
                                    navController.navigate("habit_reminder")
                                },
                                onPregnancyPlannerClick = {
                                    navController.navigate("pregnancy_planner")
                                },
                                onTibbeNabawiClick = {
                                    navController.navigate("tibbe_nabawi")
                                }
                            )
                        }

                        composable("tibbe_nabawi") {
                            TibbeNabawiScreen(
                                onBack = { navController.popBackStack() },
                                onPostClick = { id ->
                                    navController.navigate("tibbe_nabawi_detail/$id")
                                }
                            )
                        }

                        composable(
                            route = "tibbe_nabawi_detail/{id}",
                            arguments = listOf(navArgument("id") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val id = backStackEntry.arguments?.getInt("id") ?: 1
                            TibbeNabawiDetailScreen(
                                postId = id,
                                onBack = { navController.popBackStack() },
                                onRelatedPostClick = { relatedId ->
                                    navController.navigate("tibbe_nabawi_detail/$relatedId")
                                }
                            )
                        }

                        composable("habit_reminder") {
                            HabitReminderScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }

                        composable("pregnancy_planner") {
                            PregnancyPlannerScreen(
                                onBack = { navController.popBackStack() },
                                onOpenHabitPlanner = {
                                    navController.navigate("habit_reminder")
                                },
                                onArticleClick = { id ->
                                    navController.navigate("detail/$id")
                                },
                                onAdminClick = {
                                    navController.navigate("admin")
                                }
                            )
                        }

                        composable(
                            route = "detail/{articleId}",
                            arguments = listOf(navArgument("articleId") { type = NavType.IntType })
                        ) { backStackEntry ->
                            val articleId = backStackEntry.arguments?.getInt("articleId") ?: return@composable
                            DetailScreen(
                                articleId = articleId,
                                onBack = { navController.popBackStack() },
                                onRelatedArticleClick = { relatedId ->
                                    navController.navigate("detail/$relatedId")
                                }
                            )
                        }

                        composable("admin") {
                            AdminScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val id = intent.getIntExtra("open_article_id", -1)
        val tibbeId = intent.getIntExtra("open_tibbe_id", -1)
        if (tibbeId > 0) {
            targetTibbeIdState.value = tibbeId
        } else if (id < -1) {
            targetTibbeIdState.value = -id
        } else if (id > 0) {
            targetArticleIdState.value = id
        }
        if (intent.getBooleanExtra("open_habit_module", false)) {
            openHabitModuleState.value = true
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            prefsManager.lockAdminMode()
        }
        super.onDestroy()
    }
}
