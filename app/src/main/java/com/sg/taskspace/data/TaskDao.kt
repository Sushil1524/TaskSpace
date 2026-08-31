package com.sg.taskspace.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isDeleted = 0")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0")
    suspend fun getAllTasksSync(): List<Task>

    // Get active tasks for a specific date
    @Query("""
        SELECT * FROM tasks 
        WHERE isDeleted = 0 AND (
            createdForDate = :date 
            OR repeat = 'Daily' 
            OR (repeat = 'Weekly' AND repeatDayOfWeek = :dayOfWeek)
        )
        ORDER BY 
        CASE LOWER(priority)
            WHEN 'high' THEN 1
            WHEN 'medium' THEN 2
            WHEN 'low' THEN 3
            ELSE 4
        END
    """)
    fun getTasksForDate(date: String, dayOfWeek: String): Flow<List<Task>>

    // Get active tasks created for a specific range
    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND createdForDate BETWEEN :startDate AND :endDate")
    fun getTasksForDateRange(startDate: String, endDate: String): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
    
    @Query("SELECT COUNT(*) FROM tasks WHERE isDeleted = 0 AND isCompleted = 1 AND createdForDate = :date")
    fun getCompletedTaskCountForDate(date: String): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM tasks WHERE isDeleted = 0 AND createdForDate = :date")
    fun getTotalTaskCountForDate(date: String): Flow<Int>
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: String): Flow<Task?>

    @Query("SELECT * FROM tasks WHERE isDeleted = 0 AND repeat != 'None'")
    fun getRepeatingTasks(): Flow<List<Task>>

    // ── Soft Delete & Trash ──────────────────────────────────────────────────
    @Query("SELECT * FROM tasks WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedTasks(): Flow<List<Task>>

    @Query("UPDATE tasks SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :taskId")
    suspend fun softDeleteTask(taskId: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET isDeleted = 0, deletedAt = NULL WHERE id = :taskId")
    suspend fun restoreTask(taskId: String)

    @Query("DELETE FROM tasks WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :thresholdMillis")
    suspend fun purgeExpiredTasks(thresholdMillis: Long)

    @Query("DELETE FROM tasks WHERE isDeleted = 1")
    suspend fun emptyTrashTasks()
}
