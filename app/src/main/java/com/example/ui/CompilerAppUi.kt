package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.*
import com.example.api.*
import kotlinx.coroutines.launch

// Color Tokens for our Developer Slate Dark Theme
val ColorSlateBackground = Color(0xFF0F1115)
val ColorSlateSurface = Color(0xFF161A22)
val ColorSlateCard = Color(0xFF1E2430)
val ColorNeonGreen = Color(0xFF00FF66)
val ColorNeonBlue = Color(0xFF00B2FF)
val ColorNeonYellow = Color(0xFFFFCC00)
val ColorNeonRed = Color(0xFFFF3B30)
val ColorTerminalText = Color(0xFFE2E8F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompilerAppUi(
    viewModel: ProjectViewModel,
    modifier: Modifier = Modifier
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ColorSlateBackground)
    ) {
        Crossfade(
            targetState = currentScreen,
            animationSpec = tween(300),
            label = "ScreenTransition"
        ) { screen ->
            when (screen) {
                is AppScreen.Home -> HomeScreen(
                    viewModel = viewModel,
                    onProjectSelected = { id -> viewModel.navigateTo(AppScreen.ProjectDetails(id)) }
                )
                is AppScreen.ProjectDetails -> ProjectDetailsScreen(
                    projectId = screen.projectId,
                    viewModel = viewModel,
                    onBack = { viewModel.navigateTo(AppScreen.Home) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ProjectViewModel,
    onProjectSelected: (Long) -> Unit
) {
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    var repoUrl by remember { mutableStateOf("") }
    var isImporting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = ColorNeonGreen,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            "Cloud APK Compiler",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorSlateSurface,
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = ColorSlateBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner image
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.img_compiler_banner),
                            contentDescription = "Cloud compiler banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Gradient Overlay for readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                "AI-Powered Android Studio Builder",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "Import, edit, and compile repositories into signed APKs offline & in the cloud.",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Quick Stats Bar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Active Node",
                        value = "SG-Node-02",
                        icon = Icons.Default.Settings,
                        color = ColorNeonBlue,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Cloud Build Engine",
                        value = "Gradle v8.5",
                        icon = Icons.Default.Build,
                        color = ColorNeonGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Repository Import Card
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ColorSlateSurface),
                    border = BorderStroke(1.dp, ColorNeonBlue.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Import Android Repository",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Enter a GitHub URL or clone our preset configurations immediately:",
                            fontSize = 12.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = repoUrl,
                            onValueChange = { repoUrl = it },
                            placeholder = { Text("https://github.com/...", color = Color.Gray) },
                            textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ColorNeonBlue,
                                unfocusedBorderColor = Color.Gray,
                                focusedContainerColor = ColorSlateBackground,
                                unfocusedContainerColor = ColorSlateBackground
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("repo_url_input"),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Done
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (repoUrl.isBlank()) {
                                        Toast.makeText(context, "Please enter a valid Git URL", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isImporting = true
                                    viewModel.importProject(repoUrl) { projectId ->
                                        isImporting = false
                                        repoUrl = ""
                                        Toast.makeText(context, "Repository loaded successfully!", Toast.LENGTH_SHORT).show()
                                        onProjectSelected(projectId)
                                    }
                                },
                                enabled = !isImporting,
                                colors = ButtonDefaults.buttonColors(containerColor = ColorNeonBlue),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("import_button")
                            ) {
                                if (isImporting) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Clone Repo", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Instant Presets", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PresetChip(
                                label = "Brahmachari- App",
                                subtitle = "celibacy & meditation",
                                onClick = {
                                    repoUrl = "https://github.com/developersl-vihaga/Brahmachari-.git"
                                }
                            )
                            PresetChip(
                                label = "Compose Template",
                                subtitle = "clean modern empty",
                                onClick = {
                                    repoUrl = "https://github.com/android/compose-samples-template.git"
                                }
                            )
                        }
                    }
                }
            }

            // Projects List Section
            item {
                Text(
                    "Active Workspaces",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            if (projects.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ColorSlateSurface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No Active Workspace Loaded",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Paste a GitHub URL or select a preset chip above to seed a working Android project.",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            } else {
                items(projects) { project ->
                    ProjectCard(
                        project = project,
                        onClick = { onProjectSelected(project.id) },
                        onDelete = { viewModel.deleteProject(project.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ColorSlateSurface),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(title, fontSize = 10.sp, color = Color.LightGray)
                Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun PresetChip(
    label: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = ColorSlateCard),
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
        modifier = Modifier
            .clickable { onClick() }
            .widthIn(max = 180.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, fontSize = 12.sp, color = ColorNeonGreen, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(subtitle, fontSize = 9.sp, color = Color.LightGray)
        }
    }
}

@Composable
fun ProjectCard(
    project: Project,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = ColorSlateSurface),
        border = BorderStroke(
            1.dp,
            when (project.status) {
                "Compiling" -> ColorNeonYellow.copy(alpha = 0.5f)
                "Success" -> ColorNeonGreen.copy(alpha = 0.5f)
                "Failed" -> ColorNeonRed.copy(alpha = 0.5f)
                else -> Color.Gray.copy(alpha = 0.2f)
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        project.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        project.repoUrl,
                        fontSize = 11.sp,
                        color = Color.LightGray,
                        maxLines = 1
                    )
                }

                // Project Status Badge
                Box(
                    modifier = Modifier
                        .background(
                            color = when (project.status) {
                                "Compiling" -> ColorNeonYellow.copy(alpha = 0.15f)
                                "Success" -> ColorNeonGreen.copy(alpha = 0.15f)
                                "Failed" -> ColorNeonRed.copy(alpha = 0.15f)
                                else -> Color.Gray.copy(alpha = 0.15f)
                            },
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        project.status.uppercase(),
                        color = when (project.status) {
                            "Compiling" -> ColorNeonYellow
                            "Success" -> ColorNeonGreen
                            "Failed" -> ColorNeonRed
                            else -> Color.LightGray
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            if (project.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    project.description,
                    fontSize = 12.sp,
                    color = Color.LightGray.copy(alpha = 0.8f),
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.Gray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Compile SDK: ${project.compileSdk} | Target: ${project.targetSdk}",
                    fontSize = 10.sp,
                    color = Color.LightGray,
                    fontFamily = FontFamily.Monospace
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete workspace",
                        tint = ColorNeonRed.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailsScreen(
    projectId: Long,
    viewModel: ProjectViewModel,
    onBack: () -> Unit
) {
    val project by viewModel.currentProject.collectAsStateWithLifecycle()
    val files by viewModel.currentFiles.collectAsStateWithLifecycle()
    val logs by viewModel.currentLogs.collectAsStateWithLifecycle()
    val selectedFile by viewModel.selectedFile.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("Files (Code)", "AI Assistant", "Build Console")

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ColorNeonBlue)
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            project!!.name,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            project!!.repoUrl,
                            fontSize = 11.sp,
                            color = Color.LightGray,
                            maxLines = 1
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Quick Action: Build
                    IconButton(
                        onClick = {
                            selectedTabIndex = 2 // Switch to Console tab
                            viewModel.compileProject()
                        },
                        enabled = project!!.status != "Compiling"
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Build & Compile APK",
                            tint = if (project!!.status == "Compiling") Color.Gray else ColorNeonGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ColorSlateSurface,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = ColorSlateBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tabs Bar
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = ColorSlateSurface,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = ColorNeonBlue
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                title,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(ColorSlateBackground)
            ) {
                when (selectedTabIndex) {
                    0 -> CodeEditorTab(
                        files = files,
                        selectedFile = selectedFile,
                        onFileSelected = { file -> viewModel.selectFile(file) },
                        onSaveFile = { id, content ->
                            viewModel.saveFileContent(id, content)
                            Toast.makeText(context, "Saved changes to local database!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    1 -> AIAssistantTab(viewModel = viewModel)
                    2 -> BuildConsoleTab(
                        project = project!!,
                        logs = logs,
                        onCompile = { viewModel.compileProject() },
                        onAIDebug = {
                            viewModel.runAIDebugger()
                            selectedTabIndex = 1 // Take them to AI Assistant tab where debug recommendations show up
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CodeEditorTab(
    files: List<ProjectFile>,
    selectedFile: ProjectFile?,
    onFileSelected: (ProjectFile) -> Unit,
    onSaveFile: (Long, String) -> Unit
) {
    var editorText by remember(selectedFile) { mutableStateOf(selectedFile?.content ?: "") }

    if (selectedFile == null) {
        // Show file explorer list
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "Source Directory Tree",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(files) { file ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ColorSlateSurface),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.15f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onFileSelected(file) }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (file.filePath.endsWith(".gradle.kts") || file.filePath.endsWith(".toml")) {
                                    Icons.Default.Settings
                                } else if (file.filePath.endsWith(".xml")) {
                                    Icons.Default.Code
                                } else {
                                    Icons.Default.Terminal
                                },
                                contentDescription = null,
                                tint = if (file.filePath.contains("MainActivity")) ColorNeonGreen else ColorNeonBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    file.filePath.substringAfterLast('/'),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    file.filePath.substringBeforeLast('/', "Root"),
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Monospace Terminal Code Editor Layout
        Column(modifier = Modifier.fillMaxSize()) {
            // Editor Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorSlateSurface)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onSaveFile(selectedFile.id, editorText) }) {
                        Icon(Icons.Default.Save, contentDescription = "Save file", tint = ColorNeonGreen)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        selectedFile.filePath,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        modifier = Modifier.widthIn(max = 200.dp),
                        maxLines = 1
                    )
                }

                Button(
                    onClick = { onSaveFile(selectedFile.id, editorText) },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorNeonGreen.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, ColorNeonGreen),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("SAVE", color = ColorNeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            // Mobile-optimized Quick-Symbol Bar
            val quickSymbols = listOf("{", "}", "[", "]", "(", ")", ";", "=", "\"", "+", "-", ".", "/")
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorSlateSurface.copy(alpha = 0.8f))
                    .padding(vertical = 4.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(quickSymbols) { symbol ->
                    Box(
                        modifier = Modifier
                            .background(ColorSlateCard, RoundedCornerShape(4.dp))
                            .clickable {
                                editorText += symbol
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = symbol,
                            color = ColorNeonBlue,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Real Code Editor Workspace
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .background(ColorSlateBackground)
            ) {
                TextField(
                    value = editorText,
                    onValueChange = { editorText = it },
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        color = ColorTerminalText,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("code_editor_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = ColorSlateBackground,
                        unfocusedContainerColor = ColorSlateBackground
                    )
                )
            }
        }
    }
}

@Composable
fun AIAssistantTab(viewModel: ProjectViewModel) {
    val isAnalyzing by viewModel.isAnalyzing.collectAsStateWithLifecycle()
    val analysisResults by viewModel.analysisResults.collectAsStateWithLifecycle()
    
    val isDebugging by viewModel.isDebugging.collectAsStateWithLifecycle()
    val debugResult by viewModel.debugResult.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = ColorSlateSurface),
                border = BorderStroke(1.dp, ColorNeonGreen.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "AI Static Compiler Guard",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorNeonGreen,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Run an automated deep scan across all project files to detect and patch syntax defects, misconfigured gradle properties, or namespace overlaps before starting your build.",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.runStaticCodeAnalysis() },
                        enabled = !isAnalyzing,
                        colors = ButtonDefaults.buttonColors(containerColor = ColorNeonGreen),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isAnalyzing) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Analyze Code & Properties", color = Color.Black, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Live Debug Panel if there's a compilation error
        if (isDebugging || debugResult != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ColorSlateSurface),
                    border = BorderStroke(1.dp, ColorNeonRed.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BugReport, contentDescription = null, tint = ColorNeonRed)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "AI Debugger Active",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorNeonRed,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        if (isDebugging) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 12.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = ColorNeonRed, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("AI Debugger is parsing logs and source files...", fontSize = 11.sp, color = Color.LightGray)
                            }
                        } else if (debugResult != null) {
                            Text(
                                debugResult!!.explanation,
                                fontSize = 12.sp,
                                color = Color.White
                            )

                            val patches = debugResult!!.proposedChanges
                            if (!patches.isNullOrEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Proposed Fixes:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ColorNeonRed, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(8.dp))

                                patches.forEach { patch ->
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = ColorSlateCard),
                                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.2f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(patch.filePath, fontSize = 11.sp, color = ColorNeonBlue, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            Text(patch.explanation, fontSize = 11.sp, color = Color.LightGray)
                                            
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("Replace:", fontSize = 9.sp, color = Color.Gray)
                                            Box(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.4f)).padding(6.dp)) {
                                                Text(patch.originalCode, fontSize = 10.sp, color = ColorNeonRed, fontFamily = FontFamily.Monospace)
                                            }
                                            
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("With:", fontSize = 9.sp, color = Color.Gray)
                                            Box(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.4f)).padding(6.dp)) {
                                                Text(patch.proposedCode, fontSize = 10.sp, color = ColorNeonGreen, fontFamily = FontFamily.Monospace)
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = { viewModel.applyPatch(patch) },
                                                colors = ButtonDefaults.buttonColors(containerColor = ColorNeonRed.copy(alpha = 0.2f)),
                                                border = BorderStroke(1.dp, ColorNeonRed),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                                modifier = Modifier.align(Alignment.End).height(28.dp)
                                            ) {
                                                Text("Apply Fix", color = ColorNeonRed, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Static Analysis Results
        if (analysisResults != null) {
            val issues = analysisResults!!.issues
            
            if (issues.isEmpty()) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ColorSlateSurface),
                        border = BorderStroke(1.dp, ColorNeonGreen.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ColorNeonGreen)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("No issues found. Your source code files are 100% compliant and ready to compile!", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            } else {
                items(issues) { issue ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = ColorSlateSurface),
                        border = BorderStroke(
                            1.dp,
                            when (issue.severity) {
                                "CRITICAL", "HIGH" -> ColorNeonRed.copy(alpha = 0.4f)
                                "MEDIUM" -> ColorNeonYellow.copy(alpha = 0.4f)
                                else -> ColorNeonBlue.copy(alpha = 0.4f)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (issue.severity == "CRITICAL" || issue.severity == "HIGH") {
                                            Icons.Default.Error
                                        } else {
                                            Icons.Default.Warning
                                        },
                                        contentDescription = null,
                                        tint = when (issue.severity) {
                                            "CRITICAL", "HIGH" -> ColorNeonRed
                                            "MEDIUM" -> ColorNeonYellow
                                            else -> ColorNeonBlue
                                        },
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        issue.issueTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Badge(
                                    containerColor = when (issue.severity) {
                                        "CRITICAL", "HIGH" -> ColorNeonRed.copy(alpha = 0.15f)
                                        "MEDIUM" -> ColorNeonYellow.copy(alpha = 0.15f)
                                        else -> ColorNeonBlue.copy(alpha = 0.15f)
                                    }
                                ) {
                                    Text(
                                        issue.severity,
                                        color = when (issue.severity) {
                                            "CRITICAL", "HIGH" -> ColorNeonRed
                                            "MEDIUM" -> ColorNeonYellow
                                            else -> ColorNeonBlue
                                        },
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text("File: ${issue.filePath}", fontSize = 11.sp, color = ColorNeonBlue, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(issue.explanation, fontSize = 12.sp, color = Color.LightGray)

                            if (issue.proposedCode.isNotEmpty() && issue.originalCode.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Original Block:", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .padding(8.dp)
                                ) {
                                    Text(issue.originalCode, fontSize = 10.sp, color = ColorNeonRed.copy(alpha = 0.8f), fontFamily = FontFamily.Monospace)
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text("Proposed Fix:", fontSize = 10.sp, color = Color.Gray, fontFamily = FontFamily.Monospace)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .padding(8.dp)
                                ) {
                                    Text(issue.proposedCode, fontSize = 10.sp, color = ColorNeonGreen, fontFamily = FontFamily.Monospace)
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel.applyAIFix(issue) },
                                    colors = ButtonDefaults.buttonColors(containerColor = ColorNeonGreen.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, ColorNeonGreen),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                    modifier = Modifier.align(Alignment.End).height(30.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Build, contentDescription = null, tint = ColorNeonGreen, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Apply AI Patch", color = ColorNeonGreen, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BuildConsoleTab(
    project: Project,
    logs: List<BuildLog>,
    onCompile: () -> Unit,
    onAIDebug: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val consoleScrollState = rememberScrollState()

    // Scroll automatically when new log lines are printed
    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            consoleScrollState.animateScrollTo(consoleScrollState.maxValue)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Build settings & triggers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Build Type: ${project.buildType}", fontSize = 12.sp, color = Color.LightGray, fontFamily = FontFamily.Monospace)
                Text("Compile SDK: ${project.compileSdk}", fontSize = 12.sp, color = Color.LightGray, fontFamily = FontFamily.Monospace)
            }

            Button(
                onClick = onCompile,
                enabled = project.status != "Compiling",
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (project.status == "Failed") ColorNeonRed else ColorNeonGreen
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("compile_action_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (project.status == "Compiling") Icons.Default.Refresh else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (project.status == "Compiling") "Compiling..." else "Build APK",
                        color = Color.Black,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Virtual Monospace Terminal Console Workspace
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black, RoundedCornerShape(8.dp))
                .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            if (logs.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Compiler is idle.", color = Color.Gray, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("Tap 'Build APK' to launch compiling sequence.", color = Color.Gray.copy(alpha = 0.7f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(consoleScrollState),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    logs.forEach { log ->
                        Text(
                            text = log.logMessage,
                            color = if (log.isError) ColorNeonRed else ColorTerminalText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Build status panel: Success / Failed
        if (project.status == "Failed") {
            Card(
                colors = CardDefaults.cardColors(containerColor = ColorSlateSurface),
                border = BorderStroke(1.dp, ColorNeonRed),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Compilation Error Detected", color = ColorNeonRed, fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                    Text("Unresolved Kotlin syntaxes or mismatched parameters broke the package building sequence.", color = Color.LightGray, fontSize = 11.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onAIDebug,
                        colors = ButtonDefaults.buttonColors(containerColor = ColorNeonRed.copy(alpha = 0.2f)),
                        border = BorderStroke(1.dp, ColorNeonRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BugReport, contentDescription = null, tint = ColorNeonRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resolve with AI Debugger", color = ColorNeonRed, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
            }
        } else if (project.status == "Success" && project.apkUrl != null) {
            val context = LocalContext.current
            Card(
                colors = CardDefaults.cardColors(containerColor = ColorSlateSurface),
                border = BorderStroke(1.dp, ColorNeonGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // QR Code Drawing on Canvas for maximum fidelity
                    Canvas(
                        modifier = Modifier
                            .size(70.dp)
                            .background(Color.White, RoundedCornerShape(4.dp))
                    ) {
                        val sizeX = 7
                        val sizeY = 7
                        val widthPx = size.width / sizeX
                        val heightPx = size.height / sizeY
                        
                        // Seed mock QR block matrices
                        val qrBlocks = arrayOf(
                            booleanArrayOf(true, true, true, false, true, true, true),
                            booleanArrayOf(true, false, true, true, false, false, true),
                            booleanArrayOf(true, true, true, false, true, false, true),
                            booleanArrayOf(false, false, false, true, false, true, false),
                            booleanArrayOf(true, true, false, true, true, true, true),
                            booleanArrayOf(true, false, false, false, true, false, true),
                            booleanArrayOf(true, true, true, false, false, true, true)
                        )

                        for (y in 0 until sizeY) {
                            for (x in 0 until sizeX) {
                                if (qrBlocks[y][x]) {
                                    drawRect(
                                        color = Color.Black,
                                        topLeft = Offset(x * widthPx, y * heightPx),
                                        size = androidx.compose.ui.geometry.Size(widthPx, heightPx)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "APK Package Ready",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = ColorNeonGreen,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            "Format: Android APK (debug)",
                            fontSize = 10.sp,
                            color = Color.LightGray
                        )
                        Text(
                            "Size: 4.84 MB | Keystore: V2",
                            fontSize = 10.sp,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(project.apkUrl))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(ColorNeonGreen.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "Download APK", tint = ColorNeonGreen, modifier = Modifier.size(16.dp))
                            }

                            IconButton(
                                onClick = {
                                    val shareIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, "Here is the compiled Android APK download link for '${project.name}': ${project.apkUrl}")
                                        type = "text/plain"
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share APK Link"))
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(ColorNeonBlue.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share Link", tint = ColorNeonBlue, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
