package org.angryscan.app.ui.windows.screens.main.subscreens

import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.path
import io.modelaudit.scanFolder
import kotlinx.coroutines.*
import org.angryscan.app.common.ScreenStateSettings
import org.angryscan.app.db.models.TaskState
import org.angryscan.app.resources.MainScreen_FolderPickerTitle
import org.angryscan.app.resources.MainScreen_ScanStartButton
import org.angryscan.app.resources.MainScreen_SelectPathPlaceholder
import org.angryscan.app.resources.Res
import org.angryscan.app.scan.ScanService
import org.angryscan.app.scan.TaskEntityViewModel
import org.angryscan.app.scan.common.connectors.ConnectorAIModels
import org.angryscan.app.scan.common.createDialogSettings
import org.angryscan.app.ui.windows.components.RadioButtonNavigation
import org.angryscan.app.ui.windows.screens.main.components.MainScreenConnector
import org.angryscan.app.ui.windows.screens.main.settings.SettingsBox
import org.angryscan.app.ui.windows.screens.main.settings.SettingsButton
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import java.io.File

private val logger = KotlinLogging.logger {}

@Composable
fun AIModelsScreen(
    navController: androidx.navigation.NavController,
    settingsExpanded: Boolean,
    expandSettings: () -> Unit,
    hideSettings: () -> Unit,
    expandScanState: (Int) -> Unit
) {
    val scanService = koinInject<ScanService>()
    val screenStateSettings = koinInject<ScreenStateSettings>()
    var path by remember { mutableStateOf(screenStateSettings.aimodelsScreenState.path) }

    val settingsButtonTransition = updateTransition(settingsExpanded)
    val settingsBoxTransition = updateTransition(settingsExpanded)

    val coroutineScope = rememberCoroutineScope()
    var saveJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    fun saveScreenState() {
        saveJob?.cancel()
        saveJob = coroutineScope.launch {
            delay(500)
            screenStateSettings.aimodelsScreenState.path = path
            screenStateSettings.save()
        }
    }

    var selectPathError by remember { mutableStateOf(false) }
    var scanNotCorrectPath by remember { mutableStateOf(false) }
    var scanInProgress by remember { mutableStateOf(false) }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val isOnAIModelsScreen = backStackEntry?.destination?.hasRoute(MainScreenConnector.AIModels::class) == true

    var hasLoadedAIModelsSettings by remember { mutableStateOf(false) }

    LaunchedEffect(isOnAIModelsScreen) {
        if (isOnAIModelsScreen && !hasLoadedAIModelsSettings) {
            if (screenStateSettings.aimodelsScreenState.path.isNotEmpty()) {
                path = screenStateSettings.aimodelsScreenState.path
            }
            hasLoadedAIModelsSettings = true
        } else if (!isOnAIModelsScreen) {
            hasLoadedAIModelsSettings = false
        }
    }

    LaunchedEffect(scanNotCorrectPath) {
        if (scanNotCorrectPath) {
            selectPathError = true
            delay(200)
            selectPathError = false
            delay(400)
            selectPathError = true
            delay(200)
            selectPathError = false
            delay(400)
            selectPathError = true
            delay(200)
            selectPathError = false
            scanNotCorrectPath = false
        }
    }

    val folderPicker = rememberDirectoryPickerLauncher(
        dialogSettings = createDialogSettings(),
        title = stringResource(Res.string.MainScreen_FolderPickerTitle)
    ) { dir ->
        if (dir != null) {
            val selectedPath = (dir as? java.io.File)?.absolutePath ?: dir.path
            path = selectedPath
            screenStateSettings.aimodelsScreenState.path = selectedPath
            screenStateSettings.save()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = if (settingsExpanded) 0.dp else 150.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            modifier = Modifier
                .height(80.dp)
                .width(700.dp),
            value = path,
            onValueChange = {
                path = it
                saveScreenState()
            },
            placeholder = {
                Text(text = stringResource(Res.string.MainScreen_SelectPathPlaceholder))
            },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            isError = selectPathError,
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .height(48.dp)
                        .width(64.dp)
                        .size(48.dp)
                        .padding(start = 8.dp, top = 4.dp, bottom = 4.dp, end = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        modifier = Modifier.fillMaxSize(),
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null
                    )
                }
            },
            trailingIcon = {
                Row {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.onBackground)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable { folderPicker.launch() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.background
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
            }
        )

        Box(
            modifier = Modifier
                .width(700.dp)
                .padding(vertical = 0.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            RadioButtonNavigation(navController = navController)
        }

        Row {
            Button(
                onClick = {
                    logger.info { "[AI Models] Scan button clicked. path=\"$path\"" }
                    if (path.isBlank()) {
                        logger.warn { "[AI Models] Scan rejected: path is blank" }
                        scanNotCorrectPath = true
                        return@Button
                    }
                    if (!File(path).exists()) {
                        logger.warn { "[AI Models] Scan rejected: path does not exist: \"$path\"" }
                        scanNotCorrectPath = true
                        return@Button
                    }
                    saveScreenState()
                    scanInProgress = true
                    val pathToScan = path
                    coroutineScope.launch {
                        try {
                            logger.info { "[AI Models] Creating task for path=\"$pathToScan\"" }
                            val task = scanService.createTask(
                                name = null,
                                path = pathToScan,
                                extensions = emptyList(),
                                matchers = emptyList(),
                                fastScan = false,
                                connector = ConnectorAIModels()
                            )
                            logger.info { "[AI Models] Task created. id=${task.id.value}, path=\"${task.path.value}\"" }
                            task.setState(TaskState.SCANNING)
                            task.id.value?.let { taskId ->
                                expandScanState(taskId)
                            }
                            scanInProgress = false
                            GlobalScope.launch(Dispatchers.Default) {
                                runAIModelScan(scanService, task, pathToScan)
                            }
                        } catch (e: Exception) {
                            logger.error(e) { "[AI Models] Failed to create task or start scan: ${e.message}" }
                            scanInProgress = false
                        }
                    }
                },
                modifier = Modifier
                    .width(268.dp)
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium.copy(
                    topEnd = CornerSize(0.dp),
                    bottomEnd = CornerSize(0.dp)
                ),
                enabled = !scanInProgress
            ) {
                Text(
                    text = if (scanInProgress) "..." else stringResource(Res.string.MainScreen_ScanStartButton),
                    fontSize = 24.sp
                )
            }
            SettingsButton(
                transition = settingsButtonTransition,
                onClick = {
                    if (!settingsExpanded) {
                        expandSettings()
                    } else {
                        hideSettings()
                    }
                }
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 16.dp)
        ) {
            SettingsBox(transition = settingsBoxTransition)
        }
    }
}

private suspend fun runAIModelScan(
    scanService: ScanService,
    task: TaskEntityViewModel,
    path: String
) {
    logger.info { "[AI Models] runAIModelScan started. taskId=${task.id.value}, path=\"$path\"" }
    try {
        logger.info { "[AI Models] Calling io.modelaudit.scanFolder(\"$path\")..." }
        val result = withContext(Dispatchers.IO) { scanFolder(path) }
        val resultStr = result.toString()
        val rawPreviewLen = 3000
        logger.info { "[AI Models] scanFolder returned. result length=${resultStr.length}, preview=${resultStr.take(200)}..." }
        logger.debug { "[AI Models] Raw output from scanFolder (first $rawPreviewLen chars): ${resultStr.take(rawPreviewLen)}${if (resultStr.length > rawPreviewLen) " ... (truncated)" else ""}" }
        withContext(Dispatchers.Default) {
            logger.info { "[AI Models] Calling completeAIModelTask for taskId=${task.id.value}" }
            scanService.completeAIModelTask(task, resultStr)
            logger.info { "[AI Models] completeAIModelTask finished successfully" }
        }
    } catch (e: Exception) {
        logger.error(e) { "[AI Models] runAIModelScan failed: ${e.message}. Setting task to FAILED." }
        withContext<Unit>(Dispatchers.Default) {
            task.setState(TaskState.FAILED)
        }
    }
}
