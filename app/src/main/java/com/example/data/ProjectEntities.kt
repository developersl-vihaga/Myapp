package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val repoUrl: String,
    val importedAt: Long = System.currentTimeMillis(),
    val status: String = "Ready", // Ready, Compiling, Success, Failed
    val compileSdk: Int = 34,
    val targetSdk: Int = 34,
    val buildType: String = "Debug", // Debug, Release
    val apkUrl: String? = null,
    val description: String = ""
)

@Entity(tableName = "project_files")
data class ProjectFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val filePath: String,
    val content: String
)

@Entity(tableName = "build_logs")
data class BuildLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val logMessage: String,
    val isError: Boolean = false
)
