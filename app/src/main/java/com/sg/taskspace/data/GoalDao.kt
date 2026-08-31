package com.sg.taskspace.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE isDeleted = 0 ORDER BY deadlineDate ASC")
    fun getAllGoals(): Flow<List<Goal>>

    @Query("SELECT * FROM goals WHERE isDeleted = 0 ORDER BY deadlineDate ASC")
    suspend fun getAllGoalsSync(): List<Goal>

    @Query("SELECT * FROM goals WHERE isDeleted = 0 AND isCompleted = 0 ORDER BY deadlineDate ASC")
    fun getActiveGoals(): Flow<List<Goal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Delete
    suspend fun deleteGoal(goal: Goal)

    @Query("DELETE FROM goals WHERE id = :goalId")
    suspend fun deleteGoalById(goalId: String)

    @Query("DELETE FROM goals")
    suspend fun deleteAllGoals()

    // ── Soft Delete & Trash ──────────────────────────────────────────────────
    @Query("SELECT * FROM goals WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedGoals(): Flow<List<Goal>>

    @Query("UPDATE goals SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :goalId")
    suspend fun softDeleteGoal(goalId: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE goals SET isDeleted = 0, deletedAt = NULL WHERE id = :goalId")
    suspend fun restoreGoal(goalId: String)

    @Query("DELETE FROM goals WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :thresholdMillis")
    suspend fun purgeExpiredGoals(thresholdMillis: Long)

    @Query("DELETE FROM goals WHERE isDeleted = 1")
    suspend fun emptyTrashGoals()
}
