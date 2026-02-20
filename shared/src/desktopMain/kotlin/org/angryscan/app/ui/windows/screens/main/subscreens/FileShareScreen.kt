package org.angryscan.app.ui.windows.screens.main.subscreens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.ScreenStateSettings
import org.angryscan.app.resources.*
import org.angryscan.app.scan.ScanService
import org.angryscan.app.scan.common.ScanPathHelper
import org.angryscan.app.scan.common.connectors.ConnectorFileShare
import org.angryscan.app.scan.common.createDialogSettings
import org.angryscan.app.ui.components.SelectionTypes
import org.angryscan.app.ui.windows.components.DescriptionTooltip
import org.angryscan.app.ui.windows.screens.main.components.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import java.io.File

@Composable
fun FileShareScreen(
    navController: androidx.navigation.NavController,
    expandScanState: (Int) -> Unit,
    setSidebarContent: (@Composable () -> Unit) -> Unit = {},
    setBottomBarContent: (@Composable () -> Unit) -> Unit = {}
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
            screenStateSettings.save()
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
            val hasOtherSavedState = screenStateSettings.fileShareScreenState.path.isNotEmpty()
            val hasSavedState = hasSavedDetectionSettings || hasOtherSavedState
            
            if (hasSavedState) {
                scanSettings.extensions.clear()
                scanSettings.extensions.addAll(screenStateSettings.fileShareScreenState.extensions)
                scanSettings.matchers.clear()
                scanSettings.matchers.addAll(screenStateSettings.fileShareScreenState.matchers)
                scanSettings.userSignatures.clear()
                scanSettings.userSignatures.addAll(screenStateSettings.fileShareScreenState.userSignatures)
                scanSettings.fastScan.value = screenStateSettings.fileShareScreenState.fastScan.value
                scanSettings.save()
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
                screenStateSettings.save()
            }
        }
    }
    
    LaunchedEffect(isOnFileShareScreen, scanSettings.fastScan.value, selectionType) {
        if (isOnFileShareScreen) {
            screenStateSettings.fileShareScreenState.fastScan.value = scanSettings.fastScan.value
            screenStateSettings.fileShareScreenState.selectionType.value = selectionType
            screenStateSettings.save()
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

    val folderPicker = rememberDirectoryPickerLauncher(
        dialogSettings = createDialogSettings(),
        title = stringResource(Res.string.MainScreen_FolderPickerTitle)
    ) { dir ->
        if (dir != null) {
            path = dir.path
            saveScreenState()
        }
    }

    var selectPathError by remember { mutableStateOf(false) }

    var scanNotCorrectPath by remember { mutableStateOf(false) }

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

    val focusRequested by ScanPathHelper.focusRequested.collectAsState()

    LaunchedEffect(helperPath) {
        if (helperPath.isNotEmpty()) {
            path = helperPath
            coroutineScope.launch {
                delay(100)
                screenStateSettings.fileShareScreenState.path = path
                screenStateSettings.save()
            }
            if (focusRequested)
                ScanPathHelper.resetFocus()
        }
    }

    var dropdownExpanded by remember { mutableStateOf(false) }
    val typeOptions = listOf(
        SelectionTypes.Folder to stringResource(Res.string.MainScreen_SelectTypeFolder),
        SelectionTypes.File to stringResource(Res.string.MainScreen_SelectTypeFile),
        SelectionTypes.FileWithPaths to stringResource(Res.string.MainScreen_SelectTypeFileWithPaths)
    )

    setSidebarContent { }
    setBottomBarContent {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 0.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(0.5f)
                    .height(72.dp)
                    .then(
                        if (selectPathError) Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            RoundedCornerShape(20.dp)
                        )
                        else Modifier
                    ),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = path,
                        onValueChange = { path = it; saveScreenState() },
                        modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                        placeholder = {
                            Text(
                                text = when (selectionType) {
                                    SelectionTypes.FileWithPaths -> stringResource(Res.string.MainScreen_SelectFileWithPathsPlaceholder)
                                    else -> stringResource(Res.string.MainScreen_SelectPathPlaceholder)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        },
                        textStyle = MaterialTheme.typography.bodyMedium,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        isError = selectPathError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                            errorBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                        )
                    )
                    FilledTonalButton(
                        onClick = {
                            when (selectionType) {
                                SelectionTypes.Folder -> folderPicker.launch()
                                SelectionTypes.File -> filePicker.launch()
                                SelectionTypes.FileWithPaths -> pathFilePicker.launch()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = when (selectionType) {
                                SelectionTypes.Folder -> Icons.Outlined.FolderOpen
                                SelectionTypes.File -> Icons.Outlined.FileOpen
                                SelectionTypes.FileWithPaths -> Icons.Outlined.DocumentScanner
                            },
                            contentDescription = null,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Box {
                        IconButton(
                            onClick = { dropdownExpanded = true },
                            modifier = Modifier
                                .size(40.dp)
                                .pointerHoverIcon(PointerIcon.Hand)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowDropDown,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            typeOptions.forEach { (type, label) ->
                                DropdownMenuItem(
                                    text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                                    onClick = {
                                        if (selectionType != type) path = ""
                                        selectionType = type
                                        scanSettings.save()
                                        saveScreenState()
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            if (path.isNotEmpty()) {
                Button(
                    enabled = true,
                    onClick = {
                        if (!path.split(";").map { File(it).exists() }.all { it }) {
                            scanNotCorrectPath = true
                            return@Button
                        }
                        if (!validateAndShowError()) return@Button
                        val scanPath = if (selectionType == SelectionTypes.FileWithPaths) {
                            File(path).readLines().joinToString(separator = ";")
                        } else path
                        saveScreenState()
                        scanSettings.save()
                        screenStateSettings.fileShareScreenState.matchers.clear()
                        screenStateSettings.fileShareScreenState.matchers.addAll(scanSettings.matchers)
                        screenStateSettings.save()
                        coroutineScope.launch {
                            val task = scanService.createTask(
                                name = if (selectionType == SelectionTypes.FileWithPaths) path else null,
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
                        modifier = Modifier.wrapContentWidth().height(72.dp).widthIn(min = 200.dp)
                    ).scanButtonHoverFeedback(enabled = true),
                    shape = RoundedCornerShape(20.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 10.dp,
                        disabledElevation = 2.dp
                    ),
                    colors = startScanButtonColors()
                ) {
                    StartScanButtonContent()
                }
            } else {
                DescriptionTooltip(
                    description = stringResource(Res.string.MainScreen_ScanHint_FileShare),
                    delay = 400
                ) {
                    Button(
                        enabled = false,
                        onClick = { },
                        modifier = ScanButtonModifier(
                            isReady = false,
                            modifier = Modifier.wrapContentWidth().height(72.dp).widthIn(min = 200.dp)
                        ).scanButtonHoverFeedback(enabled = false),
                        shape = RoundedCornerShape(20.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 10.dp,
                            disabledElevation = 0.dp
                        ),
                        colors = startScanButtonColors()
                    ) {
                        StartScanButtonContent()
                    }
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