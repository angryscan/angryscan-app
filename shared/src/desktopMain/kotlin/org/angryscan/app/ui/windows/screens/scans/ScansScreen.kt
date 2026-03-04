package org.angryscan.app.ui.windows.screens.scans

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.angryscan.app.db.models.TaskState
import org.angryscan.app.resources.*
import org.angryscan.app.scan.ScanService
import org.angryscan.app.ui.windows.screens.scans.components.ScanFilterChipBox
import org.angryscan.app.ui.windows.screens.scans.components.ScanTaskCard
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun ScansScreen(onTaskClick: (Int) -> Unit) {
    val scanService = koinInject<ScanService>()

    val filterTaskStates = remember { mutableListOf<TaskState>() }
    var active by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    var completed by remember { mutableStateOf(false) }

    val allTasks by scanService.tasks.tasks.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    val visibleTasks = allTasks.filter { it.state.value != TaskState.LOADING }
    val filteredTasks = visibleTasks
        .filter { task ->
            if (filterTaskStates.isEmpty()) true
            else task.state.value in filterTaskStates
        }
        .filter { task ->
            val q = searchQuery.trim().lowercase()
            if (q.isEmpty()) true
            else (task.name.value.orEmpty().lowercase().contains(q) ||
                    task.path.value.orEmpty().lowercase().contains(q))
        }
        .sortedByDescending { it.finishedAt.value }
        .sortedByDescending { it.pausedAt.value }
        .sortedByDescending { it.startedAt.value }

    val countActive = visibleTasks.count { it.state.value == TaskState.SCANNING || it.state.value == TaskState.SEARCHING }
    val countPaused = visibleTasks.count { it.state.value == TaskState.STOPPED || it.state.value == TaskState.PENDING }
    val countError = visibleTasks.count { it.state.value == TaskState.FAILED }
    val countCompleted = visibleTasks.count { it.state.value == TaskState.COMPLETED }

    var currentTime by remember { mutableStateOf(Clock.System.now()) }

    LaunchedEffect(currentTime) {
        while (true) {
            currentTime = Clock.System.now()
            delay(1000)
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val containerShape = RoundedCornerShape(24.dp)
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(containerShape)
                .background(colorScheme.surfaceVariant.copy(alpha = 0.22f), containerShape)
                .border(
                    width = 1.dp,
                    color = colorScheme.outlineVariant.copy(alpha = 0.25f),
                    shape = containerShape
                )
                .padding(16.dp)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 36.dp),
                    placeholder = {
                        Text(
                            stringResource(Res.string.ScansPage_SearchPlaceholder),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = stringResource(Res.string.ScansPage_SearchPlaceholder),
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colorScheme.outlineVariant.copy(alpha = 0.6f),
                        unfocusedBorderColor = colorScheme.outlineVariant.copy(alpha = 0.35f),
                        focusedContainerColor = colorScheme.surface,
                        unfocusedContainerColor = colorScheme.surfaceVariant.copy(alpha = 0.2f),
                        cursorColor = colorScheme.primary,
                        focusedTextColor = colorScheme.onSurface,
                        unfocusedTextColor = colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = colorScheme.outlineVariant.copy(alpha = 0.2f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.ScansPage_FilterByStatus),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                ScanFilterChipBox(
                    active = active,
                    paused = paused,
                    error = error,
                    completed = completed,
                    totalCount = visibleTasks.size,
                    countActive = countActive,
                    countPaused = countPaused,
                    countError = countError,
                    countCompleted = countCompleted,
                    onAllClick = {
                        focusManager.clearFocus()
                        active = false
                        paused = false
                        error = false
                        completed = false
                        filterTaskStates.clear()
                    },
                    onActiveClick = {
                        focusManager.clearFocus()
                        active = !active
                        if (active) {
                            filterTaskStates.addAll(listOf(TaskState.SCANNING, TaskState.SEARCHING))
                        } else {
                            filterTaskStates.removeAll(listOf(TaskState.SCANNING, TaskState.SEARCHING))
                        }
                    },
                    onPausedClick = {
                        focusManager.clearFocus()
                        paused = !paused
                        if (paused) {
                            filterTaskStates.add(TaskState.STOPPED)
                            filterTaskStates.add(TaskState.PENDING)
                        } else {
                            filterTaskStates.remove(TaskState.STOPPED)
                            filterTaskStates.remove(TaskState.PENDING)
                        }
                    },
                    onErrorClick = {
                        focusManager.clearFocus()
                        error = !error
                        if (error) filterTaskStates.add(TaskState.FAILED)
                        else filterTaskStates.remove(TaskState.FAILED)
                    },
                    onCompletedClick = {
                        focusManager.clearFocus()
                        completed = !completed
                        if (completed) filterTaskStates.add(TaskState.COMPLETED)
                        else filterTaskStates.remove(TaskState.COMPLETED)
                    }
                )
            }

            if (allTasks.isNotEmpty() && visibleTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = stringResource(Res.string.ScansPage_Loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (filteredTasks.isEmpty()) {
                val isFiltered = filterTaskStates.isNotEmpty()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = stringResource(Res.string.MainScreen_RecentScans_Empty),
                            modifier = Modifier.size(48.dp),
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                        Text(
                            text = if (isFiltered)
                                stringResource(Res.string.ScansPage_NoMatches)
                            else
                                stringResource(Res.string.MainScreen_RecentScans_Empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(Res.string.ScansPage_EmptyHint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    val state = rememberLazyListState()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 24.dp),
                        state = state,
                        contentPadding = PaddingValues(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredTasks) { task ->
                            ScanTaskCard(
                                taskEntity = task,
                                onClick = {
                                    focusManager.clearFocus()
                                    onTaskClick(task.id.value!!)
                                },
                                currentTime = currentTime,
                            )
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(state),
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(end = 8.dp)
                            .width(8.dp)
                            .align(Alignment.CenterEnd),
                        style = LocalScrollbarStyle.current.copy(
                            unhoverColor = colorScheme.outlineVariant.copy(alpha = 0.6f),
                            hoverColor = colorScheme.primary
                        )
                    )
                }
            }
        }
    }
}