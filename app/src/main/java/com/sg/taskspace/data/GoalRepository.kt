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

    suspend fun deleteGoalById(goalId: String) {
        goalDao.deleteGoalById(goalId)
    }

    suspend fun deleteAllGoals() = goalDao.deleteAllGoals()

    suspend fun getAllGoalsSync(): List<Goal> = goalDao.getAllGoalsSync()

    // ── Soft Delete & Trash ──────────────────────────────────────────────────
    fun getDeletedGoals(): Flow<List<Goal>> = goalDao.getDeletedGoals()

    suspend fun softDeleteGoal(goalId: String) = goalDao.softDeleteGoal(goalId)

    suspend fun restoreGoal(goalId: String) = goalDao.restoreGoal(goalId)

    suspend fun purgeExpiredGoals(thresholdMillis: Long) = goalDao.purgeExpiredGoals(thresholdMillis)

    suspend fun emptyTrashGoals() = goalDao.emptyTrashGoals()
}
