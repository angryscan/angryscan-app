package org.angryscan.app.ui.windows.screens.main.subscreens

import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.*
import org.angryscan.app.scan.ScanService
import org.angryscan.app.scan.common.ScanPathHelper
import org.angryscan.app.scan.common.connectors.ConnectorFileShare
import org.angryscan.app.scan.common.createDialogSettings
import org.angryscan.app.ui.components.SelectionTypes
import org.angryscan.app.ui.windows.components.RadioButtonNavigation
import org.angryscan.app.ui.windows.screens.main.settings.SettingsBox
import org.angryscan.app.ui.windows.screens.main.settings.SettingsButton
import java.io.File

@Composable
fun FileShareScreen(
    navController: androidx.navigation.NavController,
    settingsExpanded: Boolean,
    expandSettings: () -> Unit,
    hideSettings: () -> Unit,
    expandScanState: (Int) -> Unit
) {
    val scanService = koinInject<ScanService>()

    val scanSettings = koinInject<ScanSettings>()

    val helperPath by ScanPathHelper.path.collectAsState()
    var path by remember { mutableStateOf(helperPath) }


    val settingsButtonTransition = updateTransition(settingsExpanded)

    val settingsBoxTransition = updateTransition(settingsExpanded)

    var selectionType by remember { scanSettings.selectionType }
    var selectionTypeChooserExpanded by remember { mutableStateOf(false) }


    val coroutineScope = rememberCoroutineScope()

    val filePicker = rememberFilePickerLauncher(
        type = FileKitType.File(),
        mode = FileKitMode.Multiple(),
        title = stringResource(Res.string.MainScreen_FilePickerTitle),
        dialogSettings = createDialogSettings()
    ) { result ->
        if (result != null) {
            path = result.joinToString(";")
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
        }

    }

    val folderPicker = rememberDirectoryPickerLauncher(
        dialogSettings = createDialogSettings(),
        title = stringResource(Res.string.MainScreen_FolderPickerTitle)
    ) { dir ->
        if (dir != null) {
            path = dir.path
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
            if (focusRequested)
                ScanPathHelper.resetFocus()
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
            onValueChange = { path = it },
            placeholder = {
                Text(
                    text = when (selectionType) {
                        SelectionTypes.FileWithPaths -> stringResource(Res.string.MainScreen_SelectFileWithPathsPlaceholder)
                        else -> stringResource(Res.string.MainScreen_SelectPathPlaceholder)
                    }
                )
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
                        modifier = Modifier
                            .fillMaxSize(),
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
                            .clip(
                                MaterialTheme.shapes.large.copy(
                                    topEnd = CornerSize(0.dp),
                                    bottomEnd = CornerSize(0.dp)
                                )
                            )
                            .background(MaterialTheme.colorScheme.onBackground)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable {
                                when (selectionType) {
                                    SelectionTypes.Folder -> folderPicker.launch()
                                    SelectionTypes.File -> filePicker.launch()
                                    SelectionTypes.FileWithPaths -> pathFilePicker.launch()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (selectionType) {
                                SelectionTypes.Folder -> Icons.Outlined.FolderOpen
                                SelectionTypes.File -> Icons.Outlined.FileOpen
                                SelectionTypes.FileWithPaths -> Icons.Outlined.DocumentScanner
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.background
                        )
                    }
                    Box(
                        modifier = Modifier
                            .height(56.dp)
                            .width(28.dp)
                            .clip(
                                MaterialTheme.shapes.large.copy(
                                    topStart = CornerSize(0.dp),
                                    bottomStart = CornerSize(0.dp)
                                )
                            )
                            .background(MaterialTheme.colorScheme.onBackground)
                            .pointerHoverIcon(PointerIcon.Hand)
                            .clickable {
                                selectionTypeChooserExpanded = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ArrowDropDown,
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp),
                            tint = MaterialTheme.colorScheme.background
                        )
                    }
                    DropdownMenu(
                        expanded = selectionTypeChooserExpanded,
                        onDismissRequest = {
                            selectionTypeChooserExpanded = false
                        }
                    ) {
                        DropdownMenuItem(
                            onClick = {
                                if (selectionType != SelectionTypes.Folder)
                                    path = ""
                                selectionType = SelectionTypes.Folder
                                selectionTypeChooserExpanded = false
                                scanSettings.save()
                                folderPicker.launch()
                            },
                            text = { Text(text = stringResource(Res.string.MainScreen_SelectTypeFolder)) }
                        )
                        DropdownMenuItem(
                            onClick = {
                                if (selectionType != SelectionTypes.File)
                                    path = ""
                                selectionType = SelectionTypes.File
                                selectionTypeChooserExpanded = false
                                scanSettings.save()
                                filePicker.launch()
                            },
                            text = { Text(text = stringResource(Res.string.MainScreen_SelectTypeFile)) }
                        )
                        DropdownMenuItem(
                            onClick = {
                                if (selectionType != SelectionTypes.FileWithPaths)
                                    path = ""
                                selectionType = SelectionTypes.FileWithPaths
                                selectionTypeChooserExpanded = false
                                scanSettings.save()
                                pathFilePicker.launch()
                            },
                            text = { Text(text = stringResource(Res.string.MainScreen_SelectTypeFileWithPaths)) }
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
            RadioButtonNavigation(
                navController = navController
            )
        }

        Row {
                Button(
                    onClick = {
                        if (path
                                .split(";").map {
                                    File(it).exists()
                                }
                                .all { it }
                        ) {
                            val scanPath = if (selectionType == SelectionTypes.FileWithPaths) {
                                val file = File(path)
                                file.readLines().joinToString(separator = ";")
                            } else {
                                path
                            }
                            coroutineScope.launch {
                                val task = scanService.createTask(
                                    name = if (selectionType == SelectionTypes.FileWithPaths) path else null,
                                    path = scanPath,
                                    extensions = scanSettings.extensions,
                                    matchers = scanSettings.matchers + scanSettings.userSignatures,
                                    fastScan = scanSettings.fastScan.value,
                                    connector = ConnectorFileShare()
                                )
                                scanService.startTask(task)
                                task.id.value?.let { taskId ->
                                    expandScanState(taskId)
                                }

                            }
                        } else {
                            scanNotCorrectPath = true
                        }
                    },
                    modifier = Modifier
                        .width(268.dp)
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium.copy(
                        topEnd = CornerSize(0.dp),
                        bottomEnd = CornerSize(0.dp)
                    )
                ) {
                    Text(
                        text = stringResource(Res.string.MainScreen_ScanStartButton),
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
            SettingsBox(
                transition = settingsBoxTransition
            )
        }
    }
}