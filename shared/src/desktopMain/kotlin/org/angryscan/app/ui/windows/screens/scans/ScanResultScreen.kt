package org.angryscan.app.ui.windows.screens.scans

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toInstant
import org.angryscan.common.engine.IMatcher
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.angryscan.app.common.AppFiles
import org.angryscan.app.common.AppSettings
import org.angryscan.app.db.models.TaskState
import org.angryscan.app.resources.*
import org.angryscan.app.scan.ScanService
import org.angryscan.app.scan.TaskFilesViewModel
import org.angryscan.app.scan.common.connectors.ConnectorS3
import org.angryscan.app.scan.common.createDialogSettings
import org.angryscan.app.scan.common.writer.ResultWriter
import org.angryscan.app.ui.dialogs.DesktopAlertDialog
import org.angryscan.app.ui.extensions.color
import org.angryscan.app.ui.extensions.icon
import org.angryscan.app.ui.strings.composableName
import org.angryscan.app.ui.windows.components.MatcherTooltip
import org.angryscan.app.ui.windows.screens.scans.components.*
import java.awt.datatransfer.StringSelection
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScanResultScreen(
    taskId: Int,
    onCloseClick: () -> Unit
) {
    val scanService = koinInject<ScanService>()
    val appSettings = koinInject<AppSettings>()
    val task = scanService.tasks.tasks.value.firstOrNull { it.id.value == taskId }

    if (task == null) {
        onCloseClick()
        return
    }

    val taskFilesViewModel = koinInject<TaskFilesViewModel> { parametersOf(task.dbTask) }
    val taskFiles by taskFilesViewModel.taskFiles.collectAsState()

    val scoreSum = taskFiles.sumOf { it.score }

    val clipboard = LocalClipboard.current

    val coroutineScope = rememberCoroutineScope()

    val state by task.state.collectAsState()
    val path by task.path.collectAsState()
    val name by task.name.collectAsState()
    val fastScan by task.fastScan.collectAsState()
    val startedAt by task.startedAt.collectAsState()
    val finishedAt by task.finishedAt.collectAsState()
    val pausedAt by task.pausedAt.collectAsState()

    val scanned by task.scannedFiles.collectAsState()
    val skipped by task.skippedFiles.collectAsState()
    val selectedFiles by task.selectedFiles.collectAsState()
    val foundFiles by task.foundFiles.collectAsState()
    val totalFiles by task.totalFiles.collectAsState()

    val folderSize by task.folderSize.collectAsState()
    val selectedFilesSize by task.selectedFilesSize.collectAsState()
    val foundFilesSize by task.foundFilesSize.collectAsState()

    val foundAttributes by task.foundAttributes.collectAsState()

    val busy by task.busy.collectAsState()

    val attributesOnOpen = remember { mutableStateListOf<IMatcher>() }

    val selectedAttributes = remember { mutableStateListOf<IMatcher>() }


    val pausedAtInstant = pausedAt?.toInstant(TimeZone.currentSystemDefault())
    val startedAtInstant = startedAt?.toInstant(TimeZone.currentSystemDefault())
    val deltaSeconds by task.deltaSeconds.collectAsState()
    val deltaDuration = (deltaSeconds ?: 0L).toDuration(DurationUnit.SECONDS)

    var currentTime by remember { mutableStateOf(Clock.System.now()) }

    LaunchedEffect(currentTime) {
        while (true) {
            currentTime = Clock.System.now()
            delay(1000)
        }
    }

    val scanTime = if (startedAt != null) {
        when (state) {
            TaskState.COMPLETED -> finishedAt!!.toInstant(TimeZone.currentSystemDefault()) - startedAtInstant!! - deltaDuration
            TaskState.STOPPED, TaskState.PENDING -> (pausedAtInstant
                ?: startedAtInstant!!) - startedAtInstant!! - deltaDuration

            else -> currentTime - startedAtInstant!! - deltaDuration
        }
            .toComponents { days, hours, minutes, seconds, _ ->
                if (days > 0)
                    "$days:$hours:${minutes.toString().padStart(2, '0')}" +
                            ":${seconds.toString().padStart(2, '0')}"
                else if (hours > 0)
                    "$hours:${minutes.toString().padStart(2, '0')}" +
                            ":${seconds.toString().padStart(2, '0')}"
                else
                    minutes.toString().padStart(2, '0') +
                            ":${seconds.toString().padStart(2, '0')}"
            }
    } else {
        "00:00:00"
    }

    LaunchedEffect(foundAttributes) {
        foundAttributes.filter { it !in attributesOnOpen }
            .let { attributes ->
                selectedAttributes.addAll(attributes)
                attributesOnOpen.addAll(attributes)
            }

        taskFilesViewModel.update()
    }

    val progress = if (selectedFiles > 0) 100 * (scanned + skipped) / selectedFiles else 0

    LaunchedEffect(progress) {
        taskFilesViewModel.update()
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (selectedFiles > 0) (scanned + skipped).toFloat() / selectedFiles else 0f,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
    )

    val fileDateFormat = LocalDateTime.Format {
        year()
        char('-')
        monthNumber()
        char('-')
        dayOfMonth()
        char('_')
        hour()
        char('-')
        minute()
        char('-')
        second()
    }

    val dialogSettings = createDialogSettings()

    var reportExtension by remember { appSettings.reportSaveExtension }
    var reportExtensionChooserExpanded by remember { mutableStateOf(false) }
    var errorDialogVisible by remember { mutableStateOf(false) }

    val saveLauncher = rememberFileSaverLauncher(
        dialogSettings = dialogSettings
    ) { file ->
        if (file != null) {
            coroutineScope.launch {
                ResultWriter.saveResult(
                    file.path,
                    taskFiles.sortedWith(
                        SortColumn.Score.comparator()
                    ),
                    onSaveError = {
                        errorDialogVisible = true
                    }
                )
            }
        }

    }

    if (errorDialogVisible) {
        DesktopAlertDialog(
            onCloseRequest = {
                errorDialogVisible = false
            },
            title = stringResource(Res.string.FileSave_ErrorTitle),
            message = stringResource(Res.string.FileSave_ErrorText)
        )
    }

    val shapes = MaterialTheme.shapes.medium.copy(bottomEnd = CornerSize(0.dp), bottomStart = CornerSize(0.dp))

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp)
            .clip(shape = shapes)
            .border(
                shape = shapes,
                color = state.color(),
                width = 1.dp
            )
            .padding(
                start = 15.dp,
                top = 15.dp,
                end = 15.dp
            ),
        snackbarHost = {
            SnackbarHost(snackbarHostState)
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.weight(0.8f)
                    ) {
                        IconButton(
                            onClick = onCloseClick
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ArrowBackIosNew,
                                contentDescription = null
                            )
                        }

                        when (task.dbTask.connector) {
                            is ConnectorS3 -> {
                                Icon(
                                    painter = painterResource(Res.drawable.aws_s3),
                                    contentDescription = "AWS S3",
                                    modifier = Modifier
                                        .size(32.dp)
                                )
                            }
                        }

                        Text(
                            text = name ?: path,
                            fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                            fontWeight = MaterialTheme.typography.bodyMedium.fontWeight,
                            letterSpacing = 0.1.sp
                        )

                        if (name == null) {
                            Icon(
                                imageVector = Icons.Outlined.CopyAll,
                                contentDescription = null,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .clickable {
                                        coroutineScope.launch {
                                            clipboard.setClipEntry(clipEntry = ClipEntry(StringSelection(path)))
                                            snackbarHostState.showSnackbar(getString(Res.string.ScanResultScreen_ClipboardCopiedMessage))
                                        }
                                    }
                            )
                        }

                        if (fastScan) {
                            Icon(
                                imageVector = Icons.Outlined.RocketLaunch,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(24.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        Icon(
                            imageVector = state.icon(),
                            contentDescription = null,
                            tint = state.color()
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedVisibility(
                            visible = state == TaskState.COMPLETED,
                        ) {
                            Row {


                                Box(
                                    modifier = Modifier
                                        .size(height = 40.dp, width = 90.dp)
                                        .clip(
                                            MaterialTheme.shapes.medium.copy(
                                                bottomEnd = CornerSize(0.dp),
                                                topEnd = CornerSize(0.dp)
                                            )
                                        )
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                        .clickable {
                                            saveLauncher.launch(
                                                suggestedName = "ADS_${fileDateFormat.format(finishedAt!!)}",
                                                extension = reportExtension.extension,
                                                directory = PlatformFile(AppFiles.UserDirPath)
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row {
                                        Icon(
                                            imageVector = Icons.Outlined.Download,
                                            contentDescription = null
                                        )
                                        Text(
                                            text = reportExtension.name,
                                            modifier = Modifier
                                                .padding(start = 5.dp)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(
                                            MaterialTheme.shapes.medium.copy(
                                                bottomStart = CornerSize(0.dp),
                                                topStart = CornerSize(0.dp)
                                            )
                                        )
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                        .clickable {
                                            reportExtensionChooserExpanded = true
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ArrowDropDown,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(32.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = reportExtensionChooserExpanded,
                                    onDismissRequest = {
                                        reportExtensionChooserExpanded = false
                                    }
                                ) {
                                    ResultWriter.FileExtensions.entries.forEach {
                                        DropdownMenuItem(
                                            onClick = {
                                                reportExtension = it
                                                reportExtensionChooserExpanded = false
                                                appSettings.save()
                                            },
                                            text = { Text(text = it.name) }
                                        )
                                    }
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                .clickable {
                                    onCloseClick()
                                    coroutineScope.launch {
                                        scanService.deleteTask(task)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null
                            )
                        }
                    }
                }
            }

            if (state != TaskState.COMPLETED) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (busy || state in listOf(TaskState.LOADING, TaskState.SEARCHING)) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(40.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                .clickable {
                                    when (state) {
                                        TaskState.SCANNING ->
                                            scanService.stopTask(task)

                                        TaskState.STOPPED -> scanService.resumeTask(task)
                                        TaskState.PENDING -> scanService.startTask(task)
                                        else -> scanService.rescanTask(task)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (state) {
                                    TaskState.SEARCHING, TaskState.SCANNING, TaskState.LOADING -> Icons.Outlined.Pause
                                    TaskState.STOPPED, TaskState.PENDING -> Icons.Outlined.PlayArrow
                                    else -> Icons.Outlined.RestartAlt
                                },
                                contentDescription = null
                            )
                        }
                    }
                    Column(
                        modifier = Modifier.width(800.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$progress% (${scanned + skipped} / $selectedFiles)",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        ) {
                            // Progress fill with bright gradient
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(animatedProgress)
                                    .background(state.color())
                            )

                            // Shimmer effect for active scanning
                            if (state == TaskState.SCANNING) {
                                val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
                                val shimmerOffset by infiniteTransition.animateFloat(
                                    initialValue = -1f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1500, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "shimmer"
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(animatedProgress)
                                        .background(
                                            brush = Brush.horizontalGradient(
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.White.copy(alpha = 0.4f),
                                                    Color.Transparent
                                                ),
                                                startX = shimmerOffset * 300f,
                                                endX = (shimmerOffset + 0.3f) * 300f
                                            )
                                        )
                                )
                            }
                        }
                    }
                }

            }


            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ScanTimeStatItem(
                    startedAt = startedAt,
                    finishedAt = finishedAt,
                    pausedAt = pausedAt,
                    state = state
                )

                VerticalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.primary
                )

                ScanStat(
                    totalFiles = totalFiles,
                    selectedFiles = selectedFiles,
                    foundFiles = foundFiles,
                    folderSize = folderSize,
                    selectedFilesSize = selectedFilesSize,
                    foundFilesSize = foundFilesSize,
                    scanTime = scanTime,
                    scoreSum = scoreSum
                )
            }

            if (foundAttributes.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.Task_FoundAttributes),
                        fontSize = 14.sp,
                        letterSpacing = 0.1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AttributeFilterChip(
                            text = stringResource(Res.string.SelectAll, attributesOnOpen.size),
                            selected = attributesOnOpen.size == selectedAttributes.size,
                            onClick = {
                                if (attributesOnOpen.size == selectedAttributes.size) {
                                    selectedAttributes.clear()
                                } else {
                                    selectedAttributes.addAll(attributesOnOpen.filter { it !in selectedAttributes })
                                }
                            }
                        )
                        attributesOnOpen.forEach { attr ->
                            MatcherTooltip(
                                matcher = attr
                            ) {
                                AttributeFilterChip(
                                    text = attr.composableName(),
                                    selected = attr in selectedAttributes,
                                    onClick = {
                                        if (attr in selectedAttributes) {
                                            selectedAttributes -= attr
                                        } else {
                                            selectedAttributes += attr
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            ResultTable(
                taskFilesViewModel = taskFilesViewModel,
                task = task,
                selectedAttributes = selectedAttributes
            )
        }
    }
}