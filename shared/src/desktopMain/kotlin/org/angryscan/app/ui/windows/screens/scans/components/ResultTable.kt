package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CursorDropdownMenu
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.launch
import org.angryscan.app.common.AppFiles
import org.angryscan.app.common.OS
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.*
import org.angryscan.app.scan.TaskEntityViewModel
import org.angryscan.app.scan.TaskFileResult
import org.angryscan.app.scan.TaskFilesViewModel
import org.angryscan.app.scan.common.connectors.ConnectorFileShare
import org.angryscan.app.scan.common.createDialogSettings
import org.angryscan.app.scan.common.files.LocationFinder
import org.angryscan.app.scan.common.files.extensions.requireKeywords
import org.angryscan.app.scan.common.files.types.IFileType
import org.angryscan.app.scan.engine.getEngine
import org.angryscan.app.ui.windows.components.MessageBox
import org.angryscan.common.engine.IMatcher
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import java.awt.Desktop
import java.io.File

@OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
fun ResultTable(
    taskFilesViewModel: TaskFilesViewModel,
    task: TaskEntityViewModel,
    selectedAttributes: List<IMatcher>,
    scanSettings: ScanSettings,
    modifier: Modifier = Modifier
) {

    val coroutineScope = rememberCoroutineScope()

    val taskFiles by taskFilesViewModel.taskFiles.collectAsState()

    var sortColumn by remember { mutableStateOf(SortColumn.Score) }
    var sortDescending by remember { mutableStateOf(false) }

    val sortedFiles = taskFiles.sortedWith(
        if (sortDescending)
            sortColumn.comparator().reversed()
        else
            sortColumn.comparator()
    )


    val selectedFiles = remember { mutableStateListOf<Int>() }

    val filesExists = remember { mutableStateListOf<Int>() }
    val filesDeleted = remember { mutableStateListOf<Int>() }

    val scrollState = rememberLazyListState()

    val state by task.state.collectAsState()

    var fileSelected by remember { mutableStateOf<TaskFileResult?>(null) }
    var attributeSelected by remember { mutableStateOf<IMatcher?>(null) }
    var locationWindowVisible by remember { mutableStateOf(false) }

    var longScanMessageBoxVisible by remember { mutableStateOf(false) }

    if (longScanMessageBoxVisible) {
        MessageBox(
            title = stringResource(Res.string.Location_LongScanTitle),
            message = stringResource(Res.string.Location_LongScanText),
            onConfirm = {
                locationWindowVisible = true
                longScanMessageBoxVisible = false
            },
            onDismiss = { longScanMessageBoxVisible = false }
        )
    }

    if (locationWindowVisible && attributeSelected != null && fileSelected != null) {
        AttributeLocationWindow(
            fileSelected?.path ?: "",
            attribute = attributeSelected!!,
            onClose = { allMasked ->
                if (allMasked) {
                    task.deleteFoundAttribute(fileSelected!!.id, attributeSelected!!)
                    coroutineScope.launch {
                        taskFilesViewModel.update()
                    }
                }
                locationWindowVisible = false
                attributeSelected = null
                fileSelected = null
            }
        )
    }


    LaunchedEffect(taskFiles) {
        filesExists.addAll(
            taskFiles
                .filter { it.id !in filesExists && it.id !in filesDeleted }
                .map { it.id }
        )

    }

    LaunchedEffect(state) {
        taskFilesViewModel.update()

        filesExists.clear()
        filesExists.addAll(
            taskFiles
                .filter { File(it.path).exists() }
                .map { it.id }
        )
        filesDeleted
            .addAll(
                taskFiles.filter { it.id !in filesExists }.map { it.id }
            )
    }

    val snackbarHostState = remember { SnackbarHostState() }



    var exportFile by remember { mutableStateOf<String?>(null) }
    val exportMatchers = remember { mutableStateListOf<IMatcher>() }

    val dialogSettings = createDialogSettings()

    val saveLauncher = rememberFileSaverLauncher(
        dialogSettings = dialogSettings
    ) { file ->
        coroutineScope.launch {
            try {
                if (file != null) {
                    val requireKeyword = IFileType
                        .getFileType(file.file)
                        .requireKeywords(file.file.extension)
                    val engine = scanSettings.engine.value.getEngine(exportMatchers, requireKeyword)
                    val rows = LocationFinder.exportRows(
                        inputFile = exportFile!!,
                        engine = engine,
                        outputFile = file.path
                    )
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            getString(
                                Res.string.LocationWindow_ExportRowsCount,
                                rows
                            )
                        )
                    }
                }
            } catch (_: Exception) {
                snackbarHostState.showSnackbar(
                    getString(
                        Res.string.LocationWindow_ExportError
                    )
                )
            }
        }
    }



    val colorScheme = MaterialTheme.colorScheme
    val containerShape = RoundedCornerShape(24.dp)

    Scaffold(
        containerColor = Color.Transparent,
        modifier = modifier
            .fillMaxSize()
            .clip(containerShape)
            .background(colorScheme.surfaceVariant.copy(alpha = 0.22f), containerShape)
            .border(
                width = 1.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.25f),
                shape = containerShape
            ),
        floatingActionButton = {
            if (selectedFiles.isNotEmpty()) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch {
                            val filesToDelete = selectedFiles.size
                            val fd = taskFiles.filter { it.id in selectedFiles }.map { file ->
                                File(file.path).delete()
                            }.count { it }

                            taskFiles.filter { it.id in selectedFiles }.forEach {
                                if (!File(it.path).exists()) {
                                    filesExists.remove(it.id)
                                    filesDeleted.add(it.id)
                                }
                            }

                            coroutineScope.launch {
                                if (fd == filesToDelete)
                                    snackbarHostState.showSnackbar(
                                        getString(
                                            Res.string.Result_DeletedFiles,
                                            filesDeleted
                                        )
                                    )
                                else
                                    snackbarHostState.showSnackbar(
                                        getString(
                                            Res.string.Result_NotDeletedFiles,
                                            filesToDelete - fd
                                        )
                                    )
                            }

                            selectedFiles.clear()

                        }
                    }
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.Result_DeleteFiles, selectedFiles.size),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = if (scrollState.canScrollBackward || scrollState.canScrollForward) 30.dp else 0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                ) {
                    Checkbox(
                        checked = selectedFiles.isNotEmpty() && selectedFiles.containsAll(sortedFiles.map { it.id }),
                        onCheckedChange = { checkState ->
                            if (checkState) {
                                selectedFiles.addAll(
                                    sortedFiles.map { it.id }
                                        .filter { id -> !selectedFiles.contains(id) && id in filesExists }
                                )
                            } else {
                                selectedFiles.clear()
                            }
                        },
                        modifier = Modifier.size(40.dp),
                        colors = CheckboxDefaults.colors().copy(
                            checkedBorderColor = MaterialTheme.colorScheme.primary,
                            uncheckedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Box(
                        modifier = Modifier
                            .weight(0.5f)
                            .clip(shape = MaterialTheme.shapes.small)
                            .clickable {
                                if (sortColumn == SortColumn.Path) {
                                    sortDescending = !sortDescending
                                } else {
                                    sortColumn = SortColumn.Path
                                    sortDescending = false
                                }
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.Result_ColumnFile),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (sortColumn == SortColumn.Path) {
                                Icon(
                                    imageVector = if (sortDescending) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                                    contentDescription = "Sort",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(20.dp)
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(0.5f)
                            .clip(shape = MaterialTheme.shapes.small)
                            .clickable {
                                if (sortColumn == SortColumn.Attribute) {
                                    sortDescending = !sortDescending
                                } else {
                                    sortColumn = SortColumn.Attribute
                                    sortDescending = false
                                }
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.Result_ColumnAttributes),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (sortColumn == SortColumn.Attribute) {
                                Icon(
                                    imageVector = if (sortDescending) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                                    contentDescription = "Sort",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(20.dp)
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(0.1f)
                            .widthIn(min = 56.dp)
                            .clip(shape = MaterialTheme.shapes.small)
                            .clickable {
                                if (sortColumn == SortColumn.Score) {
                                    sortDescending = !sortDescending
                                } else {
                                    sortColumn = SortColumn.Score
                                    sortDescending = false
                                }
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.Result_ColumnScore),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (sortColumn == SortColumn.Score) {
                                Icon(
                                    imageVector = if (sortDescending) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                                    contentDescription = "Sort",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(20.dp)
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(0.1f)
                            .widthIn(min = 56.dp)
                            .clip(shape = MaterialTheme.shapes.small)
                            .clickable {
                                if (sortColumn == SortColumn.Count) {
                                    sortDescending = !sortDescending
                                } else {
                                    sortColumn = SortColumn.Count
                                    sortDescending = false
                                }
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.Result_ColumnCount),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (sortColumn == SortColumn.Count) {
                                Icon(
                                    imageVector = if (sortDescending) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                                    contentDescription = "Sort",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(20.dp)
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(0.1f)
                            .widthIn(min = 56.dp)
                            .clip(shape = MaterialTheme.shapes.small)
                            .clickable {
                                if (sortColumn == SortColumn.Size) {
                                    sortDescending = !sortDescending
                                } else {
                                    sortColumn = SortColumn.Size
                                    sortDescending = false
                                }
                            }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(Res.string.Result_ColumnSize),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            if (sortColumn == SortColumn.Size) {
                                Icon(
                                    imageVector = if (sortDescending) Icons.Outlined.ArrowUpward else Icons.Outlined.ArrowDownward,
                                    contentDescription = "Sort",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .size(20.dp)
                                )
                            }
                        }
                    }
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    state = scrollState
                ) {
                    items(
                        sortedFiles.filter { f ->
                            f.foundAttributes.keys.any { attr -> attr in selectedAttributes }
                        }
                    ) { file ->
                        val fileType = IFileType.getFileType(file.path)
                        val locationSupported = fileType.any { LocationFinder.isSupported(it) } &&
                                task.dbTask.connector is ConnectorFileShare
                        val exportSupported = fileType.any { LocationFinder.isExportSupported(it) }
                        val exist = filesExists.contains(file.id)
                        var menuExpanded by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(
                                    enabled = exist
                                ) {
                                    try {
                                        Desktop.getDesktop().open(File(file.path))
                                    } catch (_: Exception) {
                                        filesExists.remove(file.id)
                                    }
                                }
                                .onPointerEvent(PointerEventType.Press) { event ->
                                    if (event.buttons.isSecondaryPressed && exist) {
                                        menuExpanded = true
                                    }
                                }
                                .heightIn(min = 48.dp)
                                .padding(8.dp)
                        ) {
                            CursorDropdownMenu(
                                expanded = menuExpanded,
                                onDismissRequest = {
                                    menuExpanded = false
                                },
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surface)
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.FileOpen,
                                            contentDescription = null
                                        )
                                    },
                                    text = {
                                        Text(
                                            stringResource(Res.string.openFile),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = {
                                        try {
                                            Desktop.getDesktop().open(File(file.path))
                                        } catch (_: Exception) {
                                            filesExists.remove(file.id)
                                        }
                                        menuExpanded = false
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = null
                                        )
                                    },
                                    text = {
                                        Text(
                                            stringResource(Res.string.deleteFile),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    },
                                    onClick = {
                                        if (File(file.path).delete()) {
                                            filesExists.remove(file.id)
                                            filesDeleted.add(file.id)
                                        }
                                        menuExpanded = false
                                    }
                                )
                                if (OS.currentOS() == OS.WINDOWS) {
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                Icons.Outlined.FolderOpen,
                                                contentDescription = null
                                            )
                                        },
                                        text = {
                                            Text(
                                                stringResource(Res.string.DropDown_OpenInExplorer),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        onClick = {
                                            val f = File(file.path)
                                            if (f.exists()) {
                                                ProcessBuilder("explorer", "/select,", file.path).start()
                                            } else {
                                                filesExists.remove(file.id)
                                                filesDeleted.add(file.id)
                                            }
                                            menuExpanded = false
                                        }
                                    )
                                }
                                if (exportSupported) {
                                    DropdownMenuItem(
                                        leadingIcon = {
                                            Icon(
                                                Icons.Outlined.SystemUpdateAlt,
                                                contentDescription = null
                                            )
                                        },
                                        text = {
                                            Text(
                                                stringResource(Res.string.LocationWindow_ExportAllRows),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        },
                                        onClick = {
                                            exportFile = file.path
                                            exportMatchers.clear()
                                            exportMatchers.addAll(file.foundAttributes.keys)
                                            saveLauncher.launch(
                                                suggestedName = "${File(file.path).name}_Rows",
                                                extension = "csv",
                                                directory = PlatformFile(AppFiles.UserDirPath)
                                            )
                                        }
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {

                                Checkbox(
                                    checked = selectedFiles.contains(file.id),
                                    onCheckedChange = { checkState ->
                                        if (checkState) {
                                            selectedFiles.add(file.id)
                                        } else {
                                            selectedFiles.remove(file.id)
                                        }
                                    },
                                    modifier = Modifier.size(40.dp),
                                    colors = CheckboxDefaults.colors().copy(
                                        checkedBorderColor = MaterialTheme.colorScheme.primary,
                                        uncheckedBorderColor = MaterialTheme.colorScheme.primary
                                    ),
                                    enabled = exist
                                )
                                Text(
                                    text = file.path
                                        .replace(task.path.value, "")
                                        .removePrefix("/")
                                        .removePrefix("\\")
                                        .ifEmpty {
                                            file.path
                                                .substringAfterLast("/")
                                                .substringAfterLast("\\")
                                        },
                                    fontSize = MaterialTheme.typography.bodySmall.fontSize,
                                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                                    letterSpacing = 0.1.sp,
                                    fontWeight = MaterialTheme.typography.bodySmall.fontWeight,
                                    modifier = Modifier.weight(0.5f),
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .weight(0.5f)
                                ) {
                                    file.foundAttributes.toList().sortedByDescending { it.second }.forEach { attr ->
                                        AttributeCard(
                                            attribute = attr.first,
                                            count = attr.second,
                                            onClick = {
                                                attributeSelected = attr.first
                                                fileSelected = file
                                                longScanMessageBoxVisible = true
                                            },
                                            enabled = locationSupported && exist
                                        )
                                    }
                                }
                                Text(
                                    text = file.score.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .weight(0.1f)
                                        .widthIn(min = 56.dp),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = file.count.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .weight(0.1f)
                                        .widthIn(min = 56.dp),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = file.size.toString(),
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier
                                        .weight(0.1f)
                                        .widthIn(min = 56.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            VerticalScrollbar(
                adapter = rememberScrollbarAdapter(scrollState),
                modifier = Modifier
                    .padding(end = 10.dp)
                    .width(10.dp)
                    .align(Alignment.CenterEnd),
                style = LocalScrollbarStyle.current.copy(
                    unhoverColor = MaterialTheme.colorScheme.secondary,
                    hoverColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

enum class SortColumn {
    Path,
    Attribute,
    Score,
    Count,
    Size
}

fun SortColumn.comparator(): Comparator<TaskFileResult> = when (this) {
    SortColumn.Path -> compareBy(TaskFileResult::path)
    SortColumn.Attribute -> compareBy<TaskFileResult> { it.foundAttributes.size }.reversed()
    SortColumn.Score -> compareByDescending(TaskFileResult::score)
    SortColumn.Count -> compareByDescending(TaskFileResult::count)
    SortColumn.Size -> compareByDescending(TaskFileResult::size)
}