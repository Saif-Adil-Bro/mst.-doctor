package com.health.knowledge.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.health.knowledge.data.local.entity.HabitEntity
import com.health.knowledge.data.local.entity.HabitExecutionLogEntity
import com.health.knowledge.data.local.entity.ReminderConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    // ── HABITS ───────────────────────────────────────────────────────────────

    @Query("SELECT * FROM habits ORDER BY is_active DESC, anchor_time ASC")
    fun getAllHabitsFlow(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE is_active = 1 ORDER BY anchor_time ASC")
    suspend fun getActiveHabitsSnapshot(): List<HabitEntity>

    @Query("SELECT * FROM habits ORDER BY created_at ASC")
    suspend fun getAllHabitsSnapshot(): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :habitId LIMIT 1")
    suspend fun getHabitById(habitId: String): HabitEntity?

    @Query("SELECT COUNT(*) FROM habits")
    suspend fun getHabitCount(): Int

    @Query("SELECT COUNT(*) FROM habits WHERE is_active = 1")
    fun getActiveHabitCountFlow(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabits(habits: List<HabitEntity>)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("UPDATE habits SET is_active = :isActive WHERE id = :habitId")
    suspend fun setHabitActive(habitId: String, isActive: Int)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: String)

    // ── REMINDER CONFIGS ─────────────────────────────────────────────────────

    @Query("SELECT * FROM reminder_configs")
    fun getAllReminderConfigsFlow(): Flow<List<ReminderConfigEntity>>

    @Query("SELECT * FROM reminder_configs WHERE habit_id = :habitId")
    suspend fun getConfigsForHabit(habitId: String): List<ReminderConfigEntity>

    @Query("SELECT * FROM reminder_configs WHERE habit_id = :habitId AND phase = :phase LIMIT 1")
    suspend fun getConfigForHabitPhase(habitId: String, phase: String): ReminderConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminderConfigs(configs: List<ReminderConfigEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertReminderConfig(config: ReminderConfigEntity)

    @Query("DELETE FROM reminder_configs WHERE habit_id = :habitId")
    suspend fun deleteConfigsForHabit(habitId: String)

    // ── EXECUTION LOGS ───────────────────────────────────────────────────────

    @Query("SELECT * FROM habit_execution_logs ORDER BY COALESCE(executed_time, scheduled_time) DESC")
    fun getRecentExecutionLogsFlow(): Flow<List<HabitExecutionLogEntity>>

    @Query(
        """
        SELECT * FROM habit_execution_logs 
        WHERE habit_id = :habitId 
          AND status = 'COMPLETED' 
          AND COALESCE(executed_time, scheduled_time) BETWEEN :startMillis AND :endMillis
        """
    )
    suspend fun getCompletedLogsForHabitInRange(
        habitId: String,
        startMillis: Long,
        endMillis: Long
    ): List<HabitExecutionLogEntity>

    @Query("DELETE FROM habit_execution_logs WHERE id IN (:logIds)")
    suspend fun deleteLogsByIds(logIds: List<String>)

    @Query("SELECT * FROM habit_execution_logs WHERE status = 'PENDING' AND phase = 'POST_EVENT' AND scheduled_time > :nowMillis ORDER BY scheduled_time ASC")
    fun getPendingPostEventLogsFlow(nowMillis: Long): Flow<List<HabitExecutionLogEntity>>

    @Query("SELECT * FROM habit_execution_logs WHERE habit_id = :habitId AND phase = :phase AND status = 'PENDING'")
    suspend fun getPendingLogsForPhase(habitId: String, phase: String): List<HabitExecutionLogEntity>

    @Query("DELETE FROM habit_execution_logs WHERE habit_id = :habitId AND phase = :phase AND status = 'PENDING'")
    suspend fun deletePendingLogsForPhase(habitId: String, phase: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertExecutionLog(log: HabitExecutionLogEntity)

    @Query("UPDATE habit_execution_logs SET status = :status, executed_time = :executedTime WHERE id = :logId")
    suspend fun updateLogStatus(logId: String, status: String, executedTime: Long?)

    @Query("DELETE FROM habit_execution_logs WHERE status != 'PENDING'")
    suspend fun clearCompletedHistoryLogs()

    @Transaction
    suspend fun saveHabitWithConfigs(habit: HabitEntity, configs: List<ReminderConfigEntity>) {
        upsertHabit(habit)
        deleteConfigsForHabit(habit.id)
        upsertReminderConfigs(configs)
    }
}
