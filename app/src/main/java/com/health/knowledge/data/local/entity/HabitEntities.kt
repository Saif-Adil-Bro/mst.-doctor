package com.health.knowledge.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 1. Core Habits Table (`habits`)
 */
@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "title")
    val title: String,

    /**
     * Allowed values: 'MEAL_LINKED', 'MEDICATION', 'HYDRATION', 'CUSTOM'
     */
    @ColumnInfo(name = "category")
    val category: String,

    /**
     * HH:mm in 24h format (e.g., "14:00")
     */
    @ColumnInfo(name = "anchor_time")
    val anchorTime: String,

    @ColumnInfo(name = "has_pre_reminder", defaultValue = "1")
    val hasPreReminder: Int = 1,

    @ColumnInfo(name = "pre_offset_mins", defaultValue = "30")
    val preOffsetMins: Int = 30,

    @ColumnInfo(name = "has_post_reminder", defaultValue = "1")
    val hasPostReminder: Int = 1,

    @ColumnInfo(name = "post_offset_mins", defaultValue = "45")
    val postOffsetMins: Int = 45,

    /**
     * JSON array string of ISO/Calendar day numbers 1..7 (1=Mon .. 7=Sun), e.g., "[1,2,3,4,5,6,7]"
     */
    @ColumnInfo(name = "repeat_days")
    val repeatDays: String = "[1,2,3,4,5,6,7]",

    @ColumnInfo(name = "is_active", defaultValue = "1")
    val isActive: Int = 1,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 2. Reminder & Audio Configurations (`reminder_configs`)
 */
@Entity(
    tableName = "reminder_configs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habit_id"])]
)
data class ReminderConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "habit_id")
    val habitId: String,

    /**
     * Allowed values: 'PRE_EVENT', 'EXACT_EVENT', 'POST_EVENT'
     */
    @ColumnInfo(name = "phase")
    val phase: String,

    @ColumnInfo(name = "message_template")
    val messageTemplate: String,

    /**
     * Allowed values: 'TTS_VOICE', 'PRESET_TONE', 'SYSTEM_DEFAULT'
     */
    @ColumnInfo(name = "audio_mode")
    val audioMode: String,

    /**
     * e.g., "water_drop", "soft_bell", "medication_chime"
     */
    @ColumnInfo(name = "preset_sound_key")
    val presetSoundKey: String? = null,

    @ColumnInfo(name = "tts_text")
    val ttsText: String? = null
)

/**
 * 3. Execution Logs & Tracking (`habit_execution_logs`)
 */
@Entity(
    tableName = "habit_execution_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habit_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["habit_id"])]
)
data class HabitExecutionLogEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "habit_id")
    val habitId: String,

    /**
     * Allowed values: 'PRE_EVENT', 'EXACT_EVENT', 'POST_EVENT'
     */
    @ColumnInfo(name = "phase")
    val phase: String,

    @ColumnInfo(name = "scheduled_time")
    val scheduledTime: Long,

    @ColumnInfo(name = "executed_time")
    val executedTime: Long? = null,

    /**
     * Allowed values: 'PENDING', 'COMPLETED', 'SNOOZED', 'MISSED', 'DISMISSED'
     */
    @ColumnInfo(name = "status")
    val status: String
)
