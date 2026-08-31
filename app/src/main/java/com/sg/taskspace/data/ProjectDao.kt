package com.sg.taskspace.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project)

    @Update
    suspend fun updateProject(project: Project)

    @Delete
    suspend fun deleteProject(project: Project)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProjectById(projectId: String)

    @Query("SELECT * FROM projects WHERE isDeleted = 0 ORDER BY createdAt DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE isDeleted = 0 AND id = :projectId")
    fun getProjectById(projectId: String): Flow<Project?>

    @Query("SELECT * FROM projects WHERE isDeleted = 0 ORDER BY createdAt DESC")
    suspend fun getAllProjectsSync(): List<Project>

    @Query("DELETE FROM projects")
    suspend fun deleteAllProjects()

    // ── Soft Delete & Trash ──────────────────────────────────────────────────
    @Query("SELECT * FROM projects WHERE isDeleted = 1 ORDER BY deletedAt DESC")
    fun getDeletedProjects(): Flow<List<Project>>

    @Query("UPDATE projects SET isDeleted = 1, deletedAt = :deletedAt WHERE id = :projectId")
    suspend fun softDeleteProject(projectId: String, deletedAt: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET isDeleted = 0, deletedAt = NULL WHERE id = :projectId")
    suspend fun restoreProject(projectId: String)

    @Query("DELETE FROM projects WHERE isDeleted = 1 AND deletedAt IS NOT NULL AND deletedAt < :thresholdMillis")
    suspend fun purgeExpiredProjects(thresholdMillis: Long)

    @Query("DELETE FROM projects WHERE isDeleted = 1")
    suspend fun emptyTrashProjects()
}
