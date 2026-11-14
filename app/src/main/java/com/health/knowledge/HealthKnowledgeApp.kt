package com.health.knowledge

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.health.knowledge.util.DatabaseSeeder
import com.health.knowledge.util.HealthNotificationHelper
import com.health.knowledge.util.UserPreferencesManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class HealthKnowledgeApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var databaseSeeder: DatabaseSeeder

    @Inject
    lateinit var prefsManager: UserPreferencesManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Always start a fresh app process with Admin Mode locked
        prefsManager.lockAdminMode()
        HealthNotificationHelper.createChannels(this)
        HealthNotificationHelper.subscribeToAllUsersTopic()
        // Load bundled offline articles into Room on first launch (no-op if already seeded)
        appScope.launch { databaseSeeder.seedIfEmpty() }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
