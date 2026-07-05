package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.api.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface AppScreen {
    object Home : AppScreen
    data class ProjectDetails(val projectId: Long) : AppScreen
}

class ProjectViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = ProjectRepository(db.projectDao())

    // UI state for screen routing
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Home)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Observable streams from local DB
    val projects: StateFlow<List<Project>> = repository.allProjects
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedProjectId = MutableStateFlow<Long?>(null)
    val selectedProjectId: StateFlow<Long?> = _selectedProjectId.asStateFlow()

    val currentProject: StateFlow<Project?> = _selectedProjectId
        .flatMapLatest { id ->
            if (id != null) repository.getProjectById(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentFiles: StateFlow<List<ProjectFile>> = _selectedProjectId
        .flatMapLatest { id ->
            if (id != null) repository.getFilesByProject(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentLogs: StateFlow<List<BuildLog>> = _selectedProjectId
        .flatMapLatest { id ->
            if (id != null) repository.getLogsByProject(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Static Analysis states
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _analysisResults = MutableStateFlow<AIProjectAnalysis?>(null)
    val analysisResults: StateFlow<AIProjectAnalysis?> = _analysisResults.asStateFlow()

    // AI Debugger states
    private val _isDebugging = MutableStateFlow(false)
    val isDebugging: StateFlow<Boolean> = _isDebugging.asStateFlow()

    private val _debugResult = MutableStateFlow<AIDebuggerResponse?>(null)
    val debugResult: StateFlow<AIDebuggerResponse?> = _debugResult.asStateFlow()

    // Active File Editor
    private val _selectedFile = MutableStateFlow<ProjectFile?>(null)
    val selectedFile: StateFlow<ProjectFile?> = _selectedFile.asStateFlow()

    // Screen navigation
    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        if (screen is AppScreen.ProjectDetails) {
            _selectedProjectId.value = screen.projectId
            // Reset temporary screen-specific states
            _analysisResults.value = null
            _debugResult.value = null
            _selectedFile.value = null
        } else {
            _selectedProjectId.value = null
        }
    }

    fun selectFile(file: ProjectFile?) {
        _selectedFile.value = file
    }

    // Repository operations
    fun importProject(url: String, onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            val projectId = repository.importProjectFromUrl(url)
            onSuccess(projectId)
        }
    }

    fun deleteProject(projectId: Long) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            if (_selectedProjectId.value == projectId) {
                navigateTo(AppScreen.Home)
            }
        }
    }

    fun saveFileContent(fileId: Long, newContent: String) {
        viewModelScope.launch {
            val file = _selectedFile.value
            if (file != null && file.id == fileId) {
                val updatedFile = file.copy(content = newContent)
                repository.updateFile(updatedFile)
                _selectedFile.value = updatedFile
                
                // Clear obsolete static analysis since files have changed
                _analysisResults.value = null
            }
        }
    }

    // AI Analysis Trigger
    fun runStaticCodeAnalysis() {
        val projectId = _selectedProjectId.value ?: return
        val files = currentFiles.value
        if (files.isEmpty()) return

        viewModelScope.launch {
            _isAnalyzing.value = true
            _analysisResults.value = null
            
            repository.addLog(projectId, "Starting AI static analysis on ${files.size} files...")
            
            val filesList = files.map { it.filePath to it.content }
            val result = GeminiClient.analyzeProject(filesList)
            
            _analysisResults.value = result
            _isAnalyzing.value = false

            if (result.issues.isNotEmpty() && result.issues.firstOrNull()?.filePath != "System") {
                repository.addLog(projectId, "AI Static Analysis found ${result.issues.size} potential warnings/compile blockers.")
            } else if (result.issues.firstOrNull()?.filePath == "System") {
                repository.addLog(projectId, "AI Static Analysis skipped: Setup required.", isError = true)
            } else {
                repository.addLog(projectId, "AI Static Analysis complete. Code is pristine!")
            }
        }
    }

    fun applyAIFix(issue: AIIssue) {
        val files = currentFiles.value
        val targetFile = files.find { it.filePath == issue.filePath } ?: return

        viewModelScope.launch {
            val projectId = _selectedProjectId.value ?: return@launch
            
            // Perform basic search & replace
            val content = targetFile.content
            val updatedContent = if (content.contains(issue.originalCode)) {
                content.replace(issue.originalCode, issue.proposedCode)
            } else {
                // Fallback: If code blocks don't match perfectly, append or help
                content
            }

            if (updatedContent != content) {
                repository.updateFile(targetFile.copy(content = updatedContent))
                repository.addLog(projectId, "Applied AI patch to resolve '${issue.issueTitle}' in ${issue.filePath}")
                
                // Refresh editor if editing the same file
                val currentEd = _selectedFile.value
                if (currentEd?.id == targetFile.id) {
                    _selectedFile.value = targetFile.copy(content = updatedContent)
                }

                // Filter out fixed issue
                val currentAnalysis = _analysisResults.value
                if (currentAnalysis != null) {
                    _analysisResults.value = AIProjectAnalysis(
                        issues = currentAnalysis.issues.filter { it != issue }
                    )
                }
            } else {
                repository.addLog(projectId, "Could not automatically resolve '${issue.issueTitle}': original block changed. Please fix manually.", isError = true)
            }
        }
    }

    // AI Debugger Trigger
    fun runAIDebugger() {
        val projectId = _selectedProjectId.value ?: return
        val logs = currentLogs.value
        val files = currentFiles.value
        if (logs.isEmpty() || files.isEmpty()) return

        val errorLogsStr = logs.filter { it.isError }.joinToString("\n") { it.logMessage }
        if (errorLogsStr.isEmpty()) return

        viewModelScope.launch {
            _isDebugging.value = true
            _debugResult.value = null

            val filesList = files.map { it.filePath to it.content }
            val response = GeminiClient.debugBuildError(errorLogsStr, filesList)

            _debugResult.value = response
            _isDebugging.value = false
            
            repository.addLog(projectId, "AI Debugger: Proposal ready. See 'AI Fixes' sub-panel.")
        }
    }

    fun applyPatch(patch: AIPatch) {
        val files = currentFiles.value
        val targetFile = files.find { it.filePath == patch.filePath } ?: return

        viewModelScope.launch {
            val projectId = _selectedProjectId.value ?: return@launch
            val content = targetFile.content
            val updatedContent = if (content.contains(patch.originalCode)) {
                content.replace(patch.originalCode, patch.proposedCode)
            } else {
                content
            }

            if (updatedContent != content) {
                repository.updateFile(targetFile.copy(content = updatedContent))
                repository.addLog(projectId, "Patched ${patch.filePath}: ${patch.explanation}")
                
                val currentEd = _selectedFile.value
                if (currentEd?.id == targetFile.id) {
                    _selectedFile.value = targetFile.copy(content = updatedContent)
                }

                // Remove the applied patch
                val currentDebug = _debugResult.value
                if (currentDebug != null) {
                    _debugResult.value = AIDebuggerResponse(
                        explanation = currentDebug.explanation,
                        proposedChanges = currentDebug.proposedChanges?.filter { it != patch }
                    )
                }
            } else {
                repository.addLog(projectId, "Could not apply patch to ${patch.filePath}: matching code not found.", isError = true)
            }
        }
    }

    // Cloud Compile Sim Engine
    fun compileProject() {
        val projectId = _selectedProjectId.value ?: return
        val files = currentFiles.value

        viewModelScope.launch {
            val project = currentProject.value ?: return@launch
            
            // Update project status to Compiling
            repository.updateProject(project.copy(status = "Compiling", apkUrl = null))
            
            // Reset logs
            db.projectDao().deleteLogsByProject(projectId)
            
            repository.addLog(projectId, "========================================")
            repository.addLog(projectId, "🚀 CLOUD COMPILER KICKED OFF")
            repository.addLog(projectId, "Project: ${project.name}")
            repository.addLog(projectId, "Build Type: ${project.buildType}")
            repository.addLog(projectId, "Target SDK: API ${project.targetSdk}")
            repository.addLog(projectId, "========================================")
            
            delay(1200)
            
            repository.addLog(projectId, "🔨 Phase 1: Resolving workspace and environment...")
            delay(1000)
            repository.addLog(projectId, "Checking repository file tree integrity...")
            repository.addLog(projectId, "Success: Found ${files.size} active editable files.")
            
            delay(800)
            repository.addLog(projectId, "🔨 Phase 2: Running Gradle verification & dependency download...")
            repository.addLog(projectId, "Executing: gradlew checkEnv")
            delay(1500)
            
            // Check for potential deliberate build failures to showcase compiler debug loops!
            // If any file has the word "ERROR" or "throw Exception" or bad syntax like empty spaces, we fail the build!
            val mainActivity = files.find { it.filePath.contains("MainActivity.kt") }
            val gradleFile = files.find { it.filePath.contains("build.gradle") }
            
            val isBrokenSyntax = mainActivity?.content?.contains(";;") == true || 
                                 mainActivity?.content?.contains("error_here") == true ||
                                 gradleFile?.content?.contains("broken_dependency") == true
            
            repository.addLog(projectId, "Verifying maven repositories [Google, MavenCentral]...")
            repository.addLog(projectId, "Resolved dependencies: androidx.compose.ui, androidx.room, kotlinx-coroutines")
            delay(1200)

            if (isBrokenSyntax) {
                repository.addLog(projectId, "🔨 Phase 3: Compiling sources and resources...")
                repository.addLog(projectId, "Executing: gradlew compile${project.buildType}Kotlin", isError = false)
                delay(1200)
                
                repository.addLog(projectId, "❌ BUILD FAILED in 5s!", isError = true)
                repository.addLog(projectId, "File: ${mainActivity?.filePath ?: "MainActivity.kt"}", isError = true)
                repository.addLog(projectId, "e: app/src/main/java/com/developersl/brahmachari/MainActivity.kt: (28, 5): Syntax Error or unresolved reference.", isError = true)
                repository.addLog(projectId, "Please correct the file syntax or tap 'AI Debugger' for immediate resolution assistance.", isError = true)
                
                repository.updateProject(project.copy(status = "Failed"))
                return@launch
            }

            repository.addLog(projectId, "🔨 Phase 3: Compiling Kotlin, Java, and resources...")
            repository.addLog(projectId, "Processing Android resources (AAPT2)...")
            delay(1000)
            repository.addLog(projectId, "Executing: gradlew compile${project.buildType}Kotlin")
            delay(2000)
            repository.addLog(projectId, "Kotlin compilation completed successfully.")

            repository.addLog(projectId, "🔨 Phase 4: Bundling and dexing (R8 Optimization)...")
            delay(1200)
            repository.addLog(projectId, "Optimizing bytecode (Minification: ${project.buildType == "Release"})...")
            repository.addLog(projectId, "Classes dexed successfully into classes.dex.")

            repository.addLog(projectId, "🔨 Phase 5: Packaging and APK creation...")
            delay(1000)
            repository.addLog(projectId, "Packaging compiled outputs into standard package format...")
            
            repository.addLog(projectId, "🔨 Phase 6: Byte-aligning and APK signing...")
            delay(1000)
            repository.addLog(projectId, "Aligning APK using zipalign utility...")
            repository.addLog(projectId, "Signing APK with upload-keystore (SHA-256: 8D:E9:5F:C1...)...")
            
            delay(800)
            repository.addLog(projectId, "========================================")
            repository.addLog(projectId, "✅ BUILD SUCCESSFUL")
            repository.addLog(projectId, "Built APK: app-${project.buildType.lowercase()}.apk")
            repository.addLog(projectId, "Output size: 4.84 MB")
            repository.addLog(projectId, "Build Time: 11.2 seconds")
            repository.addLog(projectId, "========================================")
            
            // Random distinct generated string to simulate a real download apk endpoint
            val apkName = "${project.name.lowercase().replace(" ", "_")}_v1.0_${project.buildType.lowercase()}.apk"
            val randomString = (1..6).map { ('a'..'z').random() }.joinToString("")
            val apkDownloadUrl = "https://apkcraft-compiler.cloud/builds/$randomString/$apkName"
            
            repository.updateProject(project.copy(status = "Success", apkUrl = apkDownloadUrl))
        }
    }
}
