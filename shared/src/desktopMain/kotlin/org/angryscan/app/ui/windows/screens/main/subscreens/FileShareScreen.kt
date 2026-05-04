package org.angryscan.app.ui.windows.screens.main.subscreens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.ScreenStateSettings
import org.angryscan.app.resources.*
import org.angryscan.app.scan.ScanService
import org.angryscan.app.scan.common.ScanPathHelper
import org.angryscan.app.scan.common.connectors.ConnectorFileShare
import org.angryscan.app.scan.common.createDialogSettings
import org.angryscan.app.ui.components.SelectionTypes
import org.angryscan.app.ui.hasSelectedMatchersForScan
import org.angryscan.app.ui.windows.screens.main.components.*
import org.angryscan.app.ui.windows.screens.main.rememberMainSourceRowTokens
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import java.io.File

@Composable
fun FileShareScreen(
    navController: androidx.navigation.NavController,
    expandScanState: (Int) -> Unit,
    onRequireScanSettings: (missingExtensions: Boolean, missingMatchers: Boolean) -> Unit = { _, _ -> },
    onRequireSourceInputs: () -> Unit = {},
    setSidebarContent: (@Composable () -> Unit) -> Unit = {},
    setBottomBarContent: (@Composable () -> Unit) -> Unit = {},
    setUnderSourceContent: (@Composable () -> Unit) -> Unit = {}
) {
    val scanService = koinInject<ScanService>()

    val scanSettings = koinInject<ScanSettings>()
    val screenStateSettings = koinInject<ScreenStateSettings>()

    val helperPath by ScanPathHelper.path.collectAsState()
    var path by remember { mutableStateOf(screenStateSettings.fileShareScreenState.path) }

    val (validationErrorDialog, validateAndShowError, dismissValidationError) = rememberScanValidation(scanSettings)

    var selectionType by remember { scanSettings.selectionType }

    val coroutineScope = rememberCoroutineScope()
    
    var saveJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
    
    fun saveScreenState() {
        saveJob?.cancel()
        saveJob = coroutineScope.launch {
            delay(500) // Debounce 500ms
            screenStateSettings.fileShareScreenState.path = path
            screenStateSettings.fileShareScreenState.selectionType.value = selectionType
            screenStateSettings.fileShareScreenState.extensions.clear()
            screenStateSettings.fileShareScreenState.extensions.addAll(scanSettings.extensions)
            screenStateSettings.fileShareScreenState.matchers.clear()
            screenStateSettings.fileShareScreenState.matchers.addAll(scanSettings.matchers)
            screenStateSettings.fileShareScreenState.userSignatures.clear()
            screenStateSettings.fileShareScreenState.userSignatures.addAll(scanSettings.userSignatures)
            screenStateSettings.fileShareScreenState.fastScan.value = scanSettings.fastScan.value
            withContext(Dispatchers.IO) { screenStateSettings.save() }
        }
    }
    
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isOnFileShareScreen = backStackEntry?.destination?.hasRoute(MainScreenConnector.FileShare::class) == true
    
    var hasLoadedFileShareSettings by remember { mutableStateOf(false) }
    
    // Load detection settings when entering this screen
    LaunchedEffect(isOnFileShareScreen) {
        if (isOnFileShareScreen && !hasLoadedFileShareSettings) {
            selectionType = screenStateSettings.fileShareScreenState.selectionType.value
            
            val hasSavedDetectionSettings = screenStateSettings.fileShareScreenState.extensions.isNotEmpty() || 
                                           screenStateSettings.fileShareScreenState.matchers.isNotEmpty() || 
                                           screenStateSettings.fileShareScreenState.userSignatures.isNotEmpty()
            if (hasSavedDetectionSettings) {
                scanSettings.extensions.clear()
                scanSettings.extensions.addAll(screenStateSettings.fileShareScreenState.extensions)
                scanSettings.matchers.clear()
                scanSettings.matchers.addAll(screenStateSettings.fileShareScreenState.matchers)
                scanSettings.userSignatures.clear()
                scanSettings.userSignatures.addAll(screenStateSettings.fileShareScreenState.userSignatures)
                scanSettings.fastScan.value = screenStateSettings.fileShareScreenState.fastScan.value
                withContext(Dispatchers.IO) { scanSettings.save() }
            }
            hasLoadedFileShareSettings = true
        } else if (!isOnFileShareScreen) {
            hasLoadedFileShareSettings = false
        }
    }
    
    // Save detection settings using snapshotFlow to detect changes in mutableStateListOf
    LaunchedEffect(isOnFileShareScreen) {
        if (!isOnFileShareScreen) return@LaunchedEffect
        
        var saveJob: kotlinx.coroutines.Job? = null
        
        snapshotFlow { 
            Triple(
                scanSettings.extensions.size to scanSettings.extensions.joinToString(",") { it.name },
                scanSettings.matchers.size to scanSettings.matchers.joinToString(",") { it::class.simpleName ?: "" },
                scanSettings.userSignatures.size to scanSettings.userSignatures.joinToString(",") { it.name }
            )
        }.collect { (_, _, _) ->
            saveJob?.cancel()
            saveJob = coroutineScope.launch {
                delay(300)
                screenStateSettings.fileShareScreenState.extensions.clear()
                screenStateSettings.fileShareScreenState.extensions.addAll(scanSettings.extensions)
                screenStateSettings.fileShareScreenState.matchers.clear()
                screenStateSettings.fileShareScreenState.matchers.addAll(scanSettings.matchers)
                screenStateSettings.fileShareScreenState.userSignatures.clear()
                screenStateSettings.fileShareScreenState.userSignatures.addAll(scanSettings.userSignatures)
                screenStateSettings.fileShareScreenState.fastScan.value = scanSettings.fastScan.value
                screenStateSettings.fileShareScreenState.selectionType.value = selectionType
                withContext(Dispatchers.IO) { screenStateSettings.save() }
            }
        }
    }

    LaunchedEffect(isOnFileShareScreen, scanSettings.fastScan.value, selectionType) {
        if (isOnFileShareScreen) {
            screenStateSettings.fileShareScreenState.fastScan.value = scanSettings.fastScan.value
            screenStateSettings.fileShareScreenState.selectionType.value = selectionType
            withContext(Dispatchers.IO) { screenStateSettings.save() }
        }
    }

    val filePicker = rememberFilePickerLauncher(
        type = FileKitType.File(),
        mode = FileKitMode.Multiple(),
        title = stringResource(Res.string.MainScreen_FilePickerTitle),
        dialogSettings = createDialogSettings()
    ) { result ->
        if (result != null) {
            path = result.joinToString(";")
            saveScreenState()
        }

    }

    val folderPicker = rememberDirectoryPickerLauncher(
        dialogSettings = createDialogSettings(),
        title = stringResource(Res.string.MainScreen_FolderPickerTitle)
    ) { dir ->
        if (dir != null) {
            path = dir.path
            saveScreenState()
        }
    }

    val pathFilePicker = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("txt", "csv")),
        mode = FileKitMode.Single,
        title = stringResource(Res.string.MainScreen_FileWithPathsPickerTitle),
        dialogSettings = createDialogSettings()
    ) { result ->
        if (result != null) {
            path = result.path
            saveScreenState()
        }
    }

    var selectPathError by remember { mutableStateOf(false) }
    LaunchedEffect(selectPathError) {
        if (selectPathError) {
            delay(2000)
            if (selectPathError) {
                selectPathError = false
            }
        }
    }

    val focusRequested by ScanPathHelper.focusRequested.collectAsState()

    LaunchedEffect(helperPath) {
        if (helperPath.isNotEmpty()) {
            path = helperPath
            coroutineScope.launch {
                delay(100)
                screenStateSettings.fileShareScreenState.path = path
                withContext(Dispatchers.IO) { screenStateSettings.save() }
            }
            if (focusRequested)
                ScanPathHelper.resetFocus()
        }
    }

    fun detectSelectionType(rawPath: String): SelectionTypes {
        if (rawPath.isBlank()) return SelectionTypes.File
        val parts = rawPath.split(";").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size == 1) {
            val single = File(parts.first())
            if (single.isDirectory) return SelectionTypes.Folder
            if (single.isFile && single.extension.lowercase() in setOf("txt", "csv")) {
                return SelectionTypes.FileWithPaths
            }
        }
        return SelectionTypes.File
    }
    var browseMenuExpanded by remember { mutableStateOf(false) }

    setSidebarContent { }
    setUnderSourceContent { }
    setBottomBarContent {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 0.dp)
        ) {
            val sourceTokens = rememberMainSourceRowTokens(maxWidth, maxHeight)
            val controlHeight = sourceTokens.controlHeight
            val controlShape = RoundedCornerShape(sourceTokens.controlCorner)
            val scanButtonWidth = when {
                maxWidth >= 1500.dp -> sourceTokens.scanButtonWidthWide
                maxWidth < 1200.dp -> sourceTokens.scanButtonWidthCompact
                else -> sourceTokens.scanButtonWidthRegular
            }
            val controlGap = if (maxWidth < 1200.dp) sourceTokens.controlGapCompact else sourceTokens.controlGapRegular
            val pathMinWidth = if (maxWidth < 1200.dp) sourceTokens.pathMinWidthCompact else sourceTokens.pathMinWidthRegular
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(controlGap, Alignment.Start)
            ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = pathMinWidth)
                    .height(controlHeight)
                    .then(
                        if (selectPathError) Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            controlShape
                        )
                        else Modifier
                    ),
                shape = controlShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = sourceTokens.inlinePaddingHorizontal,
                            vertical = sourceTokens.inlinePaddingVertical
                        ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(sourceTokens.inlineControlGap)
            ) {
                    OutlinedTextField(
                        value = path,
                        onValueChange = { path = it; saveScreenState() },
                        modifier = Modifier.weight(1f).heightIn(min = sourceTokens.fieldMinHeight),
                        placeholder = {
                            Text(
                                text = when (detectSelectionType(path)) {
                                    SelectionTypes.FileWithPaths -> stringResource(Res.string.MainScreen_SelectFileWithPathsPlaceholder)
                                    else -> stringResource(Res.string.MainScreen_SelectPathPlaceholder)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true,
                        shape = RoundedCornerShape(sourceTokens.compactFieldCorner + 4.dp),
                        isError = selectPathError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            errorBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            errorContainerColor = Color.Transparent
                        )
                    )
                    Box {
                        val currentType = detectSelectionType(path)
                        FilledTonalButton(
                            onClick = { browseMenuExpanded = true },
                            shape = RoundedCornerShape(sourceTokens.compactFieldCorner + 4.dp),
                            modifier = Modifier.size(sourceTokens.iconButtonSize + 8.dp),
                            contentPadding = PaddingValues(0.dp),
                            colors = sourceActionFilledTonalButtonColors()
                        ) {
                            Icon(
                                imageVector = when (currentType) {
                                    SelectionTypes.Folder -> Icons.Outlined.FolderOpen
                                    SelectionTypes.File -> Icons.Outlined.FileOpen
                                    SelectionTypes.FileWithPaths -> Icons.Outlined.DocumentScanner
                                },
                                contentDescription = null,
                                modifier = Modifier.size(sourceTokens.iconSize)
                            )
                        }

                        DropdownMenu(
                            expanded = browseMenuExpanded,
                            onDismissRequest = { browseMenuExpanded = false },
                            offset = DpOffset((-92).dp, sourceTokens.inlinePaddingHorizontal),
                            shape = RoundedCornerShape(sourceTokens.controlCorner - 2.dp),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                            tonalElevation = 8.dp,
                            shadowElevation = 14.dp
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                        Text(
                                            text = stringResource(Res.string.MainScreen_SelectTypeFolder),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Select directory to scan",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.FolderOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    browseMenuExpanded = false
                                    folderPicker.launch()
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            )
                            DropdownMenuItem(
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                        Text(
                                            text = stringResource(Res.string.MainScreen_SelectTypeFile),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Select one or multiple files",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.FileOpen,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    browseMenuExpanded = false
                                    filePicker.launch()
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            )
                            DropdownMenuItem(
                                text = {
                                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                        Text(
                                            text = stringResource(Res.string.MainScreen_SelectTypeFileWithPaths),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = "Use txt/csv with paths",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.DocumentScanner,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                onClick = {
                                    browseMenuExpanded = false
                                    pathFilePicker.launch()
                                },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }
            Button(
                enabled = true,
                onClick = {
                    val pathParts = path.split(";").map { it.trim() }.filter { it.isNotEmpty() }
                    if (pathParts.isEmpty()) {
                        onRequireSourceInputs()
                        selectPathError = true
                        return@Button
                    }
                    val missingExtensions = scanSettings.extensions.isEmpty()
                    val missingMatchers = !hasSelectedMatchersForScan(scanSettings)
                    if (missingExtensions || missingMatchers) {
                        onRequireScanSettings(missingExtensions, missingMatchers)
                        return@Button
                    }
                    if (!pathParts.all { File(it).exists() }) {
                        onRequireSourceInputs()
                        selectPathError = true
                        return@Button
                    }
                    if (!validateAndShowError()) return@Button
                    val normalizedPath = pathParts.joinToString(";")
                    val detectedType = detectSelectionType(normalizedPath)
                    val scanPath = if (detectedType == SelectionTypes.FileWithPaths) {
                        File(normalizedPath).readLines().joinToString(separator = ";")
                    } else {
                        normalizedPath
                    }
                    selectionType = detectedType
                    path = normalizedPath
                    saveScreenState()
                    screenStateSettings.fileShareScreenState.matchers.clear()
                    screenStateSettings.fileShareScreenState.matchers.addAll(scanSettings.matchers)
                    coroutineScope.launch(Dispatchers.IO) {
                        scanSettings.save()
                        screenStateSettings.save()
                    }
                    coroutineScope.launch {
                        val task = scanService.createTask(
                            name = if (detectedType == SelectionTypes.FileWithPaths) normalizedPath else null,
                            path = scanPath,
                            extensions = scanSettings.extensions.toList(),
                            matchers = scanSettings.matchers.toList() + scanSettings.userSignatures.toList(),
                            fastScan = scanSettings.fastScan.value,
                            connector = ConnectorFileShare()
                        )
                        scanService.startTask(task)
                        task.id.value?.let { expandScanState(it) }
                    }
                },
                modifier = ScanButtonModifier(
                    isReady = true,
                    modifier = Modifier.width(scanButtonWidth).height(controlHeight)
                ).scanButtonHoverFeedback(enabled = true).scanButtonChipBorder(),
                shape = controlShape,
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    disabledElevation = 0.dp
                ),
                colors = startScanButtonColors()
            ) {
                StartScanButtonContent()
            }
            }
        }
    }

    // Validation error dialog
    ScanValidationErrorDialog(
        validationError = validationErrorDialog,
        onDismiss = dismissValidationError
    )
}