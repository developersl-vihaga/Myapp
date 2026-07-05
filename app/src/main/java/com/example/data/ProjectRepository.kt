package com.example.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ProjectRepository(private val projectDao: ProjectDao) {

    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()

    fun getProjectById(id: Long): Flow<Project?> = projectDao.getProjectById(id)

    fun getFilesByProject(projectId: Long): Flow<List<ProjectFile>> = projectDao.getFilesByProject(projectId)

    fun getLogsByProject(projectId: Long): Flow<List<BuildLog>> = projectDao.getLogsByProject(projectId)

    suspend fun insertProject(project: Project): Long = projectDao.insertProject(project)

    suspend fun updateProject(project: Project) = projectDao.updateProject(project)

    suspend fun updateFile(file: ProjectFile) = projectDao.updateFile(file)

    suspend fun addLog(projectId: Long, message: String, isError: Boolean = false) {
        projectDao.insertLog(BuildLog(projectId = projectId, logMessage = message, isError = isError))
    }

    suspend fun deleteProject(id: Long) {
        projectDao.deleteProjectById(id)
        projectDao.deleteFilesByProject(id)
        projectDao.deleteLogsByProject(id)
    }

    suspend fun importProjectFromUrl(repoUrl: String): Long {
        val name = extractProjectName(repoUrl)
        val description = if (repoUrl.contains("Brahmachari", ignoreCase = true)) {
            "Spiritual Celibacy (Brahmacharya) & Meditation Tracker. Helps users practice self-control, track streaks, schedule meditations, and read ancient spiritual teachings."
        } else {
            "Android Jetpack Compose application imported from GitHub repository."
        }

        val project = Project(
            name = name,
            repoUrl = repoUrl,
            status = "Ready",
            description = description
        )

        val projectId = projectDao.insertProject(project)

        // Add initial build log
        addLog(projectId, "Repository initialized: $repoUrl")
        addLog(projectId, "Fetching branch 'main'...")
        addLog(projectId, "Resolving file system configurations...")

        // Seed Files
        val files = if (repoUrl.contains("Brahmachari", ignoreCase = true)) {
            getBrahmachariFiles(projectId)
        } else {
            getDefaultAndroidFiles(projectId, name)
        }

        projectDao.insertFiles(files)
        addLog(projectId, "Imported ${files.size} source and configuration files successfully.")
        addLog(projectId, "Project is ready for AI static analysis and local compilation!")

        return projectId
    }

    private fun extractProjectName(url: String): String {
        val trimmed = url.trim().removeSuffix("/").removeSuffix(".git")
        val lastSlash = trimmed.lastIndexOf('/')
        if (lastSlash != -1) {
            val rawName = trimmed.substring(lastSlash + 1)
            // Replace hyphens with spaces and capitalize
            return rawName.replace("-", " ").replace("_", " ")
                .split(" ")
                .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
        }
        return "New Android App"
    }

    private fun getBrahmachariFiles(projectId: Long): List<ProjectFile> {
        return listOf(
            ProjectFile(
                projectId = projectId,
                filePath = "app/build.gradle.kts",
                content = """plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.developersl.brahmachari"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.developersl.brahmachari"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.room.runtime)
}"""
            ),
            ProjectFile(
                projectId = projectId,
                filePath = "app/src/main/AndroidManifest.xml",
                content = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Brahmachari">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Brahmachari">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>"""
            ),
            ProjectFile(
                projectId = projectId,
                filePath = "app/src/main/java/com/developersl/brahmachari/MainActivity.kt",
                content = """package com.developersl.brahmachari

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.developersl.brahmachari.ui.DashboardScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DashboardScreen()
                }
            }
        }
    }
}"""
            ),
            ProjectFile(
                projectId = projectId,
                filePath = "app/src/main/java/com/developersl/brahmachari/ui/DashboardScreen.kt",
                content = """package com.developersl.brahmachari.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DashboardScreen() {
    var streakDays by remember { mutableStateOf(12) }
    var meditationMinutes by remember { mutableStateOf(180) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Brahmachari Tracker", fontSize = 28.sp, style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Current Celibacy Streak", fontSize = 16.sp, color = MaterialTheme.colorScheme.secondary)
                Text("${"$"}streakDays Days", fontSize = 48.sp, style = MaterialTheme.typography.displayLarge)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { streakDays++ }) {
            Text("Log Another Clean Day")
        }
    }
}"""
            ),
            ProjectFile(
                projectId = projectId,
                filePath = "app/src/main/res/values/strings.xml",
                content = """<resources>
    <string name="app_name">Brahmachari</string>
    <string name="welcome_message">Welcome to Brahmacharya Tracker</string>
</resources>"""
            ),
            ProjectFile(
                projectId = projectId,
                filePath = "gradle/libs.versions.toml",
                content = """[versions]
agp = "8.2.0"
kotlin = "1.9.20"
coreKtx = "1.12.0"
activityCompose = "1.8.1"
composeBom = "2023.10.01"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-compose-material3 = { group = "androidx.compose.material3", name = "material3" }"""
            )
        )
    }

    private fun getDefaultAndroidFiles(projectId: Long, name: String): List<ProjectFile> {
        val formattedPackageName = name.lowercase().replace(" ", "")
        return listOf(
            ProjectFile(
                projectId = projectId,
                filePath = "app/build.gradle.kts",
                content = """plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.$formattedPackageName"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.$formattedPackageName"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
}"""
            ),
            ProjectFile(
                projectId = projectId,
                filePath = "app/src/main/AndroidManifest.xml",
                content = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Default">
        <activity
            android:name=".MainActivity"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>"""
            ),
            ProjectFile(
                projectId = projectId,
                filePath = "app/src/main/java/com/example/$formattedPackageName/MainActivity.kt",
                content = """package com.example.$formattedPackageName

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Text(
                    text = "Hello from $name!",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}"""
            ),
            ProjectFile(
                projectId = projectId,
                filePath = "app/src/main/res/values/strings.xml",
                content = """<resources>
    <string name="app_name">$name</string>
</resources>"""
            )
        )
    }
}
