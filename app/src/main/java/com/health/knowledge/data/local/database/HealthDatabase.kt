package com.health.knowledge.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.health.knowledge.data.local.dao.ArticleDao
import com.health.knowledge.data.local.dao.HabitDao
import com.health.knowledge.data.local.dao.TibbeNabawiDao
import com.health.knowledge.data.local.entity.ArticleEntity
import com.health.knowledge.data.local.entity.HabitEntity
import com.health.knowledge.data.local.entity.HabitExecutionLogEntity
import com.health.knowledge.data.local.entity.ReminderConfigEntity
import com.health.knowledge.data.local.entity.TibbeNabawiEntity

@Database(
    entities = [
        ArticleEntity::class,
        HabitEntity::class,
        ReminderConfigEntity::class,
        HabitExecutionLogEntity::class,
        TibbeNabawiEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class HealthDatabase : RoomDatabase() {

    abstract fun articleDao(): ArticleDao
    abstract fun habitDao(): HabitDao
    abstract fun tibbeNabawiDao(): TibbeNabawiDao

    companion object {
        const val DATABASE_NAME = "health_knowledge.db"

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE articles ADD COLUMN isDeleted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE articles ADD COLUMN isReacted INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE articles ADD COLUMN isReviewed INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN lastReadAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE articles ADD COLUMN userNote TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE articles ADD COLUMN isDraft INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN isMine INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE articles ADD COLUMN authorDeviceId TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE articles ADD COLUMN source TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE articles ADD COLUMN verifiedCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE articles ADD COLUMN isVerified INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habits` (
                        `id` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `anchor_time` TEXT NOT NULL,
                        `has_pre_reminder` INTEGER NOT NULL DEFAULT 1,
                        `pre_offset_mins` INTEGER NOT NULL DEFAULT 30,
                        `has_post_reminder` INTEGER NOT NULL DEFAULT 1,
                        `post_offset_mins` INTEGER NOT NULL DEFAULT 45,
                        `repeat_days` TEXT NOT NULL,
                        `is_active` INTEGER NOT NULL DEFAULT 1,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `reminder_configs` (
                        `id` TEXT NOT NULL,
                        `habit_id` TEXT NOT NULL,
                        `phase` TEXT NOT NULL,
                        `message_template` TEXT NOT NULL,
                        `audio_mode` TEXT NOT NULL,
                        `preset_sound_key` TEXT,
                        `tts_text` TEXT,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`habit_id`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_configs_habit_id` ON `reminder_configs` (`habit_id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habit_execution_logs` (
                        `id` TEXT NOT NULL,
                        `habit_id` TEXT NOT NULL,
                        `phase` TEXT NOT NULL,
                        `scheduled_time` INTEGER NOT NULL,
                        `executed_time` INTEGER,
                        `status` TEXT NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`habit_id`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_execution_logs_habit_id` ON `habit_execution_logs` (`habit_id`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tibbe_nabawi_posts` (
                        `id` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `food_name` TEXT NOT NULL DEFAULT '',
                        `arabic_name` TEXT,
                        `category` TEXT NOT NULL DEFAULT '',
                        `diseases` TEXT NOT NULL DEFAULT '',
                        `benefits` TEXT,
                        `nature` TEXT,
                        `taste` TEXT,
                        `usage_instructions` TEXT,
                        `details` TEXT NOT NULL DEFAULT '',
                        `source_pages` TEXT NOT NULL DEFAULT '',
                        `is_bookmarked` INTEGER NOT NULL DEFAULT 0,
                        `search_keywords` TEXT NOT NULL DEFAULT '',
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_tibbe_nabawi_category` ON `tibbe_nabawi_posts` (`category`)")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tibbe_nabawi_posts ADD COLUMN quotes TEXT")
            }
        }

        fun create(context: Context): HealthDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                HealthDatabase::class.java,
                DATABASE_NAME
            )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
            .fallbackToDestructiveMigration()
            .build()
        }
    }
}
