package com.sg.taskspace.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sg.taskspace.TaskSpaceApplication
import com.sg.taskspace.data.Task
import com.sg.taskspace.data.TaskRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class TaskViewModel(private val taskRepository: TaskRepository) : ViewModel() {

    // For Phase 1, we just get all tasks or tasks for today
    // We'll assume "Today" for the home screen list for now
    private val today = LocalDate.now().format(DateTimeFormatter.ISO_DATE)
    private val dayOfWeek = LocalDate.now().dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

    val homeTasks: StateFlow<List<Task>> = taskRepository.getTasksForDate(today, dayOfWeek)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTask(title: String, notes: String, priority: String, category: String) {
        viewModelScope.launch {
            val task = Task(
                title = title,
                notes = notes,
                priority = priority,
                category = category,
                createdForDate = LocalDate.now().format(DateTimeFormatter.ISO_DATE),
                repeat = "None" // Default for now
            )
            taskRepository.insertTask(task)
        }
    }
    
    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TaskSpaceApplication)
                TaskViewModel(application.container.taskRepository)
            }
        }
    }
}
