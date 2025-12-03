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

    // Get tasks for a specific date (One-time for that date OR Daily OR Weekly on that day)
    @Query("""
        SELECT * FROM tasks 
        WHERE createdForDate = :date 
        OR repeat = 'Daily' 
        OR (repeat = 'Weekly' AND repeatDayOfWeek = :dayOfWeek)
        ORDER BY 
        CASE priority
            WHEN 'High' THEN 1
            WHEN 'Medium' THEN 2
            WHEN 'Low' THEN 3
            ELSE 4
        END
    """)
    fun getTasksForDate(date: String, dayOfWeek: String): Flow<List<Task>>

    // Get tasks created for a specific range (for History/Insights - mainly one-time tasks or instances)
    // Note: Handling recurring tasks in history is tricky. 
    // For now, we'll just query tasks that have a specific createdForDate in the range.
    // A more complex app would generate "instances" of recurring tasks. 
    // Given the requirements, we might need to rely on the UI to expand recurring tasks or just show what was "completed" on that day if we track completions separately.
    // For simplicity in Phase 1, we will assume "History" shows tasks that were *completed* in that range, or *created* for that range.
    // Let's stick to simple date filtering for now.
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
}
