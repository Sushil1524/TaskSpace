package com.sg.taskspace.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.sg.taskspace.data.AppDatabase
import com.sg.taskspace.data.Goal
import com.sg.taskspace.data.GoalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GoalViewModel(private val goalRepository: GoalRepository) : ViewModel() {

    val goals: StateFlow<List<Goal>> = goalRepository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addGoal(title: String, deadlineDate: String, notes: String? = null) {
        viewModelScope.launch {
            goalRepository.insertGoal(Goal(title = title, deadlineDate = deadlineDate, notes = notes))
        }
    }

    fun updateGoal(goal: Goal) {
        viewModelScope.launch {
            goalRepository.insertGoal(goal)
        }
    }

    fun toggleGoalCompletion(goal: Goal) {
        viewModelScope.launch {
            goalRepository.insertGoal(goal.copy(isCompleted = !goal.isCompleted))
        }
    }

    fun deleteGoal(goal: Goal) {
        viewModelScope.launch {
            goalRepository.deleteGoal(goal)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY])
                val database = AppDatabase.getDatabase(application)
                return GoalViewModel(GoalRepository(database.goalDao())) as T
            }
        }
    }
}
