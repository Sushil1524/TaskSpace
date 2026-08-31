package com.sg.taskspace.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.sg.taskspace.TaskSpaceApplication
import com.sg.taskspace.data.Project
import com.sg.taskspace.data.ProjectRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectViewModel(private val projectRepository: ProjectRepository) : ViewModel() {

    val projects: StateFlow<List<Project>> = projectRepository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addProject(
        name: String,
        description: String? = null,
        colorHex: String = "#6366F1",
        dueDate: String? = null,
        reminderTimeMillis: Long? = null
    ) {
        viewModelScope.launch {
            projectRepository.insertProject(
                Project(
                    name = name,
                    description = description,
                    colorHex = colorHex,
                    dueDate = dueDate,
                    reminderTimeMillis = reminderTimeMillis
                )
            )
        }
    }

    fun updateProject(project: Project) {
        viewModelScope.launch {
            projectRepository.updateProject(project)
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            projectRepository.softDeleteProject(project.id)
        }
    }

    fun restoreProject(projectId: String) {
        viewModelScope.launch {
            projectRepository.restoreProject(projectId)
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                val application = checkNotNull(extras[APPLICATION_KEY]) as com.sg.taskspace.TaskSpaceApplication
                return ProjectViewModel(application.container.projectRepository) as T
            }
        }
    }
}
