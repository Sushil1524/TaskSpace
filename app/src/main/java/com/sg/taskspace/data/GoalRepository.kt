package com.sg.taskspace.data

import kotlinx.coroutines.flow.Flow

class GoalRepository(private val goalDao: GoalDao) {
    val allGoals: Flow<List<Goal>> = goalDao.getAllGoals()
    val activeGoals: Flow<List<Goal>> = goalDao.getActiveGoals()

    suspend fun insertGoal(goal: Goal) {
        goalDao.insertGoal(goal)
    }

    suspend fun deleteGoal(goal: Goal) {
        goalDao.deleteGoal(goal)
    }
}
