package com.health.knowledge.di

import android.content.Context
import com.health.knowledge.data.local.dao.ArticleDao
import com.health.knowledge.data.local.database.HealthDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): HealthDatabase = HealthDatabase.create(context)

    @Provides
    @Singleton
    fun provideArticleDao(database: HealthDatabase): ArticleDao = database.articleDao()
}
