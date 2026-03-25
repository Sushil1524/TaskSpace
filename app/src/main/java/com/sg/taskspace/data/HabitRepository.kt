package com.sg.taskspace.data

import kotlinx.coroutines.flow.Flow

class HabitRepository(private val habitDao: HabitDao) {
    val allHabits: Flow<List<Habit>> = habitDao.getAllHabits()

    suspend fun insertHabit(habit: Habit) {
        habitDao.insertHabit(habit)
    }

    suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteHabit(habit)
    }

    fun getHabitLog(habitId: String, date: String): Flow<HabitLog?> {
        return habitDao.getHabitLog(habitId, date)
    }

    fun getHabitLogsForDate(date: String): Flow<List<HabitLog>> {
        return habitDao.getHabitLogsForDate(date)
    }

    suspend fun insertHabitLog(log: HabitLog) {
        habitDao.insertHabitLog(log)
    }
}
