package com.sg.taskspace.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getAllHabitsSync(): List<Habit>

    @Query("SELECT * FROM habit_logs ORDER BY date DESC")
    fun getAllHabitLogs(): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs ORDER BY date DESC")
    suspend fun getAllHabitLogsSync(): List<HabitLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: String)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date LIMIT 1")
    fun getHabitLog(habitId: String, date: String): Flow<HabitLog?>

    @Query("SELECT * FROM habit_logs WHERE date = :date")
    fun getHabitLogsForDate(date: String): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY date DESC")
    fun getAllLogsForHabit(habitId: String): Flow<List<HabitLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLog(log: HabitLog)

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()

    @Query("DELETE FROM habit_logs")
    suspend fun deleteAllHabitLogs()

    // ── Soft Delete & Trash ──────────────────────────────────────────────────
    @Query("SELECT * FROM habits WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedHabits(): Flow<List<Habit>>

    @Query("UPDATE habits SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :habitId")
    suspend fun softDeleteHabit(habitId: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE habits SET isDeleted = 0, deletedAt = NULL WHERE id = :habitId")
    suspend fun restoreHabit(habitId: String)

    @Query("DELETE FROM habits WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :thresholdMillis")
    suspend fun purgeExpiredHabits(thresholdMillis: Long)

    @Query("DELETE FROM habits WHERE isDeleted = 1")
    suspend fun emptyTrashHabits()
}
