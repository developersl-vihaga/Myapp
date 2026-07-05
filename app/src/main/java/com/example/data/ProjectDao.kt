package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY importedAt DESC")
    fun getAllProjects(): Flow<List<Project>>

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectById(id: Long): Flow<Project?>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectByIdSync(id: Long): Project?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: Project): Long

    @Update
    suspend fun updateProject(project: Project)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: Long)

    // Project Files queries
    @Query("SELECT * FROM project_files WHERE projectId = :projectId ORDER BY filePath ASC")
    fun getFilesByProject(projectId: Long): Flow<List<ProjectFile>>

    @Query("SELECT * FROM project_files WHERE projectId = :projectId ORDER BY filePath ASC")
    suspend fun getFilesByProjectSync(projectId: Long): List<ProjectFile>

    @Query("SELECT * FROM project_files WHERE id = :id")
    suspend fun getFileById(id: Long): ProjectFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: ProjectFile): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<ProjectFile>)

    @Update
    suspend fun updateFile(file: ProjectFile)

    @Query("DELETE FROM project_files WHERE projectId = :projectId")
    suspend fun deleteFilesByProject(projectId: Long)

    // Build Logs queries
    @Query("SELECT * FROM build_logs WHERE projectId = :projectId ORDER BY timestamp ASC")
    fun getLogsByProject(projectId: Long): Flow<List<BuildLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BuildLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<BuildLog>)

    @Query("DELETE FROM build_logs WHERE projectId = :projectId")
    suspend fun deleteLogsByProject(projectId: Long)
}
