package com.health.knowledge.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.health.knowledge.data.local.dao.ArticleDao
import com.health.knowledge.data.local.entity.ArticleEntity

@Database(
    entities = [ArticleEntity::class],
    version = 1,
    exportSchema = false
)
abstract class HealthDatabase : RoomDatabase() {

    abstract fun articleDao(): ArticleDao

    companion object {
        const val DATABASE_NAME = "health_knowledge.db"

        // Seeding of bundled data is done by DatabaseSeeder from HealthKnowledgeApp.onCreate()
        // (kept out of here to avoid a Hilt dependency cycle: Database -> Seeder -> Dao -> Database)
        fun create(context: Context): HealthDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                HealthDatabase::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}
