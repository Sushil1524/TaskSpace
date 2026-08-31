package com.sg.taskspace.data

import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {

    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()

    suspend fun insertProject(project: Project) = projectDao.insertProject(project)

    suspend fun updateProject(project: Project) = projectDao.updateProject(project)

    suspend fun deleteProject(project: Project) = projectDao.deleteProject(project)

    suspend fun deleteProjectById(projectId: String) = projectDao.deleteProjectById(projectId)

    fun getProjectById(projectId: String): Flow<Project?> = projectDao.getProjectById(projectId)

    suspend fun getAllProjectsSync(): List<Project> = projectDao.getAllProjectsSync()

    suspend fun deleteAllProjects() = projectDao.deleteAllProjects()

    // ── Soft Delete & Trash ──────────────────────────────────────────────────
    fun getDeletedProjects(): Flow<List<Project>> = projectDao.getDeletedProjects()

    suspend fun softDeleteProject(projectId: String) = projectDao.softDeleteProject(projectId)

    suspend fun restoreProject(projectId: String) = projectDao.restoreProject(projectId)

    suspend fun purgeExpiredProjects(thresholdMillis: Long) = projectDao.purgeExpiredProjects(thresholdMillis)

    suspend fun emptyTrashProjects() = projectDao.emptyTrashProjects()
}
