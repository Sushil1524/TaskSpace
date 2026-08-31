package com.sg.taskspace.data

import kotlinx.coroutines.flow.Flow

class HabitRepository(private val habitDao: HabitDao) {
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()
    val allHabitLogs: Flow<List<HabitLog>> = habitDao.getAllHabitLogs()

    suspend fun insertHabit(habit: Habit) {
        habitDao.insertHabit(habit)
    }

    suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteHabit(habit)
    }

    suspend fun deleteHabitById(habitId: String) {
        habitDao.deleteHabitById(habitId)
    }

    fun getHabitLog(habitId: String, date: String): Flow<HabitLog?> {
        return habitDao.getHabitLog(habitId, date)
    }

    fun getHabitLogsForDate(date: String): Flow<List<HabitLog>> {
        return habitDao.getHabitLogsForDate(date)
    }

    fun getAllLogsForHabit(habitId: String): Flow<List<HabitLog>> {
        return habitDao.getAllLogsForHabit(habitId)
    }

    suspend fun insertHabitLog(log: HabitLog) {
        habitDao.insertHabitLog(log)
    }

    suspend fun deleteAllHabits() = habitDao.deleteAllHabits()
    suspend fun deleteAllHabitLogs() = habitDao.deleteAllHabitLogs()

    suspend fun getAllHabitsSync(): List<Habit> = habitDao.getAllHabitsSync()
    suspend fun getAllHabitLogsSync(): List<HabitLog> = habitDao.getAllHabitLogsSync()

    // ── Soft Delete & Trash ──────────────────────────────────────────────────
    fun getDeletedHabits(): Flow<List<Habit>> = habitDao.getDeletedHabits()

    suspend fun softDeleteHabit(habitId: String) = habitDao.softDeleteHabit(habitId)

    suspend fun restoreHabit(habitId: String) = habitDao.restoreHabit(habitId)

    suspend fun purgeExpiredHabits(thresholdMillis: Long) = habitDao.purgeExpiredHabits(thresholdMillis)

    suspend fun emptyTrashHabits() = habitDao.emptyTrashHabits()
}
