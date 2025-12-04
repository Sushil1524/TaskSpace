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
    @Query("SELECT * FROM tasks")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksSync(): List<Task>

    // Get tasks for a specific date (One-time for that date OR Daily OR Weekly on that day)
    @Query("""
        SELECT * FROM tasks 
        WHERE createdForDate = :date 
        OR repeat = 'Daily' 
        OR (repeat = 'Weekly' AND repeatDayOfWeek = :dayOfWeek)
        ORDER BY 
        CASE LOWER(priority)
            WHEN 'high' THEN 1
            WHEN 'medium' THEN 2
            WHEN 'low' THEN 3
            ELSE 4
        END
    """)
    fun getTasksForDate(date: String, dayOfWeek: String): Flow<List<Task>>

    // Get tasks created for a specific range (for History/Insights - mainly one-time tasks or instances)
    @Query("SELECT * FROM tasks WHERE createdForDate BETWEEN :startDate AND :endDate")
    fun getTasksForDateRange(startDate: String, endDate: String): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks WHERE id = :taskId")
    suspend fun deleteTaskById(taskId: String)
    
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 1 AND createdForDate = :date")
    fun getCompletedTaskCountForDate(date: String): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM tasks WHERE createdForDate = :date")
    fun getTotalTaskCountForDate(date: String): Flow<Int>
    
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    fun getTaskById(taskId: String): Flow<Task?>

    @Query("SELECT * FROM tasks WHERE repeat != 'None'")
    fun getRepeatingTasks(): Flow<List<Task>>
}
