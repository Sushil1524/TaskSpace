package com.sg.taskspace.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.sg.taskspace.TaskSpaceApplication
import com.sg.taskspace.data.Task
import com.sg.taskspace.data.TaskRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields

class TaskViewModel(private val taskRepository: TaskRepository) : ViewModel() {

    // Date & Time Logic
    private val currentDate = LocalDate.now()
    val formattedDate: String = currentDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d"))
    val weekNumber: String = "Week ${currentDate.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)}"

    // Tasks for Today
    private val todayIso = currentDate.format(DateTimeFormatter.ISO_DATE)
    private val dayOfWeek = currentDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

    val homeTasks: StateFlow<List<Task>> = taskRepository.getTasksForDate(todayIso, dayOfWeek)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Motivation Logic
    val motivationText: StateFlow<String> = homeTasks.map { tasks ->
        if (tasks.isEmpty()) {
            "No tasks for today. Add some to get started!"
        } else {
            val completed = tasks.count { it.isCompleted }
            when (completed) {
                0 -> "Let's get started! You have ${tasks.size} tasks today."
                tasks.size -> "All done! Great job today! \uD83C\uDF89" // Party popper
                else -> "Keep going! $completed of ${tasks.size} completed."
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Loading..."
    )

    // Weekly Progress (Placeholder for now, or simple implementation)
    // We need a list of days (Mon-Sun) and their completion status
    // For Phase 2, let's just show the days and make them clickable (to filter? or just visual?)
    // User said: "shows mon thru sun and they are clickable and it shows total task and completed task with the list of that particular day"
    // This implies clicking a day changes the "Today's Task" list to that day.
    
    private val _selectedDate = MutableStateFlow(currentDate)
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Update homeTasks to observe selectedDate
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentDisplayTasks: StateFlow<List<Task>> = _selectedDate.flatMapLatest { date ->
        val dateIso = date.format(DateTimeFormatter.ISO_DATE)
        val dayName = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        taskRepository.getTasksForDate(dateIso, dayName)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    // Weekly Tasks Logic
    @OptIn(ExperimentalCoroutinesApi::class)
    val weeklyTasks: StateFlow<List<Task>> = _selectedDate.flatMapLatest { date ->
        // Calculate start and end of week
        val currentDayOfWeek = date.dayOfWeek.value
        val startOfWeek = date.minusDays((currentDayOfWeek - 1).toLong())
        val endOfWeek = startOfWeek.plusDays(6)
        
        val startIso = startOfWeek.format(DateTimeFormatter.ISO_DATE)
        val endIso = endOfWeek.format(DateTimeFormatter.ISO_DATE)
        
        taskRepository.getTasksForDateRange(startIso, endIso)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addTask(title: String, notes: String, priority: String, category: String, repeat: String = "None") {
        viewModelScope.launch {
            val task = Task(
                title = title,
                notes = notes,
                priority = priority,
                category = category,
                createdForDate = _selectedDate.value.format(DateTimeFormatter.ISO_DATE), // Add to selected date
                repeat = repeat
            )
            taskRepository.insertTask(task)
        }
    }
    
    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }
    
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }

    fun updateTaskDetails(task: Task) {
        viewModelScope.launch {
            taskRepository.updateTask(task)
        }
    }

    fun getTaskById(taskId: String): Flow<Task?> {
        // We need to add getTaskById to Repository and Dao first, or just filter from all tasks if list is small.
        // Better to add to Dao/Repo for correctness.
        // For now, let's assume we add it to Repo.
        return taskRepository.getTaskById(taskId)
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
