package com.sg.taskspace.data

import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    fun getTasksForDate(date: String, dayOfWeek: String): Flow<List<Task>> = 
        taskDao.getTasksForDate(date, dayOfWeek)

    fun getTasksForDateRange(startDate: String, endDate: String): Flow<List<Task>> = 
        taskDao.getTasksForDateRange(startDate, endDate)

    suspend fun insertTask(task: Task) = taskDao.insertTask(task)

    suspend fun updateTask(task: Task) = taskDao.updateTask(task)

    suspend fun deleteTask(task: Task) = taskDao.deleteTask(task)
    
    suspend fun deleteTaskById(taskId: String) = taskDao.deleteTaskById(taskId)
    
    fun getCompletedCount(date: String): Flow<Int> = taskDao.getCompletedTaskCountForDate(date)
    
    fun getTotalCount(date: String): Flow<Int> = taskDao.getTotalTaskCountForDate(date)
    
    fun getTaskById(taskId: String): Flow<Task?> = taskDao.getTaskById(taskId)

    fun getRepeatingTasks(): Flow<List<Task>> = taskDao.getRepeatingTasks()
}
