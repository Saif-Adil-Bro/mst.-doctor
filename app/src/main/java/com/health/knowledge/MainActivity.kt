package com.health.knowledge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.health.knowledge.sync.DailyTipNotificationWorker
import com.health.knowledge.sync.SyncWorker
import com.health.knowledge.ui.admin.AdminScreen
import com.health.knowledge.ui.detail.DetailScreen
import com.health.knowledge.ui.home.HomeScreen
import com.health.knowledge.ui.theme.HealthKnowledgeTheme
import com.health.knowledge.util.UserPreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var prefsManager: UserPreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule background sync every 6 hours (only when internet available)
        SyncWorker.schedule(this)
        // Also try immediate sync on app open
        SyncWorker.syncNow(this)
        // Schedule daily health tip reminder if enabled
        DailyTipNotificationWorker.scheduleOrCancel(this, prefsManager.dailyReminderEnabled.value)

        val initialArticleId = intent?.getIntExtra("open_article_id", -1) ?: -1

        setContent {
            val themeMode by prefsManager.themeMode.collectAsState()

            HealthKnowledgeTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    LaunchedEffect(initialArticleId) {
                        if (initialArticleId > 0) {
                            navController.navigate("detail/$initialArticleId")
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "home"
                    ) {
                        composable("home") {
                            HomeScreen(
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
}
