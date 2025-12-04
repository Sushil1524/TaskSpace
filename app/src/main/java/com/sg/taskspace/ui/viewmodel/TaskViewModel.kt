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
    // Motivation Logic (Moved to UI)

    // Weekly Progress (Placeholder for now, or simple implementation)
    // We need a list of days (Mon-Sun) and their completion status
    // For Phase 2, let's just show the days and make them clickable (to filter? or just visual?)
    // User said: "shows mon thru sun and they are clickable and it shows total task and completed task with the list of that particular day"
    // This implies clicking a day changes the "Today's Task" list to that day.
    
    private val _selectedDate = MutableStateFlow(currentDate)
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Update homeTasks to observe selectedDate
    // currentDisplayTasks moved to below to use new logic

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    // Categories
    // Categories (Defined in UI/Bottom Sheet)

    data class DayStats(val date: LocalDate, val totalTasks: Int, val completedTasks: Int)

    // Helper to filter tasks for a specific day, handling repeating task exceptions
    private fun getTasksForDay(
        date: LocalDate,
        rangeTasks: List<Task>,
        repeatingTasks: List<Task>
    ): List<Task> {
        val dateIso = date.format(DateTimeFormatter.ISO_DATE)
        val dayName = date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }

        // 1. Get all specific tasks for this day (including completed instances of repeating tasks)
        val specificTasks = rangeTasks.filter { it.createdForDate == dateIso }

        // 2. Get relevant repeating tasks
        val relevantRepeating = repeatingTasks.filter {
            it.repeat == "Daily" || (it.repeat == "Weekly" && it.repeatDayOfWeek == dayName)
        }

        // 3. Filter out repeating tasks that have a completion instance (child) in specificTasks
        val effectiveRepeating = relevantRepeating.filter { repeating ->
            specificTasks.none { specific -> specific.parentId == repeating.id }
        }

        // 4. Combine
        return specificTasks + effectiveRepeating
    }

    // Weekly Stats Logic
    @OptIn(ExperimentalCoroutinesApi::class)
    val weeklyStats: StateFlow<List<DayStats>> = _selectedDate.flatMapLatest { date ->
        val currentDayOfWeek = date.dayOfWeek.value
        val startOfWeek = date.minusDays((currentDayOfWeek - 1).toLong())
        val endOfWeek = startOfWeek.plusDays(6)
        val startIso = startOfWeek.format(DateTimeFormatter.ISO_DATE)
        val endIso = endOfWeek.format(DateTimeFormatter.ISO_DATE)

        kotlinx.coroutines.flow.combine(
            taskRepository.getTasksForDateRange(startIso, endIso),
            taskRepository.getRepeatingTasks()
        ) { rangeTasks, repeatingTasks ->
            val stats = mutableListOf<DayStats>()
            for (i in 0 until 7) {
                val currentDate = startOfWeek.plusDays(i.toLong())
                val dailyTasks = getTasksForDay(currentDate, rangeTasks, repeatingTasks)
                
                stats.add(DayStats(
                    date = currentDate,
                    totalTasks = dailyTasks.size,
                    completedTasks = dailyTasks.count { it.isCompleted }
                ))
            }
            stats
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Weekly Tasks Logic (Updated to include repeating tasks)
    @OptIn(ExperimentalCoroutinesApi::class)
    val weeklyTasks: StateFlow<List<Task>> = _selectedDate.flatMapLatest { date ->
        val currentDayOfWeek = date.dayOfWeek.value
        val startOfWeek = date.minusDays((currentDayOfWeek - 1).toLong())
        val endOfWeek = startOfWeek.plusDays(6)
        val startIso = startOfWeek.format(DateTimeFormatter.ISO_DATE)
        val endIso = endOfWeek.format(DateTimeFormatter.ISO_DATE)
        
        kotlinx.coroutines.flow.combine(
            taskRepository.getTasksForDateRange(startIso, endIso),
            taskRepository.getRepeatingTasks()
        ) { rangeTasks, repeatingTasks ->
             // Return all tasks for the week, but we need to be careful about duplicates if we just list them.
             // For the "Weekly Planning" view, we probably want to see the *definitions* or the *instances*.
             // If we use this for the "Day Details Dialog", we should use getTasksForDay logic.
             // Let's keep this simple for now and let the UI filter or use a better structure.
             // Actually, let's just return everything so the UI can decide.
             (rangeTasks + repeatingTasks).distinctBy { it.id }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Update currentDisplayTasks to use the new logic
    @OptIn(ExperimentalCoroutinesApi::class)
    val currentDisplayTasks: StateFlow<List<Task>> = _selectedDate.flatMapLatest { date ->
        val dateIso = date.format(DateTimeFormatter.ISO_DATE)
        
        // We need to fetch range tasks for just this day to be efficient, 
        // but our Repo only has getTasksForDateRange or getTasksForDate.
        // getTasksForDate in Dao includes repeating logic in SQL, which is now insufficient because of the parentId check.
        // So we should fetch "specific tasks" for today and "repeating tasks" and merge them in Kotlin.
        
        kotlinx.coroutines.flow.combine(
            taskRepository.getTasksForDate(dateIso, ""), // We can use this but ignore its repeating logic? No, it returns mixed.
            // Let's use getTasksForDateRange for just today.
            taskRepository.getTasksForDateRange(dateIso, dateIso),
            taskRepository.getRepeatingTasks()
        ) { _, rangeTasks, repeatingTasks -> // Ignore the first one
            getTasksForDay(date, rangeTasks, repeatingTasks)
        }
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
            if (task.repeat != "None") {
                // It's a repeating task (Template). User wants to complete it for TODAY.
                // Create a new instance for today that is completed.
                val todayIso = _selectedDate.value.format(DateTimeFormatter.ISO_DATE)
                val completedInstance = task.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    createdForDate = todayIso,
                    isCompleted = true,
                    repeat = "None",
                    parentId = task.id
                )
                taskRepository.insertTask(completedInstance)
            } else if (task.parentId != null && task.isCompleted) {
                // It's a completed instance of a repeating task. User is un-checking it.
                // We should delete this instance so the original repeating task shows up again.
                taskRepository.deleteTask(task)
            } else {
                // Normal task or disconnected instance
                taskRepository.updateTask(task.copy(isCompleted = !task.isCompleted))
            }
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
