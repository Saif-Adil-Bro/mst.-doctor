package com.health.knowledge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.work.Configuration
import com.health.knowledge.sync.SyncWorker
import com.health.knowledge.ui.admin.AdminScreen
import com.health.knowledge.ui.detail.DetailScreen
import com.health.knowledge.ui.home.HomeScreen
import com.health.knowledge.ui.theme.HealthKnowledgeTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Schedule background sync every 6 hours (only when internet available)
        SyncWorker.schedule(this)
        // Also try immediate sync on app open
        SyncWorker.syncNow(this)

        setContent {
            HealthKnowledgeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

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
                                onBack = { navController.popBackStack() }
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
