package com.health.knowledge.di

import androidx.work.WorkerFactory
import com.health.knowledge.sync.SyncWorker
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object WorkerModule
// HiltWorkerFactory is automatically provided by @HiltWorker annotation on SyncWorker
