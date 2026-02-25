package org.angryscan.app.ui.windows.screens.scans

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

    val visibleTasks = allTasks.filter { it.state.value != TaskState.LOADING }
    val filteredTasks = visibleTasks
        .filter { task ->
            if (filterTaskStates.isEmpty()) true
            else task.state.value in filterTaskStates
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 20.dp)
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
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.SideMenu_ScanListPage),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colorScheme.onSurface
                )
                Text(
                    text = stringResource(Res.string.ScansPage_ResultCount, filteredTasks.size),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = stringResource(Res.string.ScansPage_FilterByStatus),
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 20.dp)
            ) {
                ScanFilterChipBox(
                    active = active,
                    paused = paused,
                    error = error,
                    completed = completed,
                    countActive = countActive,
                    countPaused = countPaused,
                    countError = countError,
                    countCompleted = countCompleted,
                    onAllClick = {
                        active = false
                        paused = false
                        error = false
                        completed = false
                        filterTaskStates.clear()
                    },
                    onActiveClick = {
                        active = !active
                        if (active) {
                            filterTaskStates.addAll(listOf(TaskState.SCANNING, TaskState.SEARCHING))
                        } else {
                            filterTaskStates.removeAll(listOf(TaskState.SCANNING, TaskState.SEARCHING))
                        }
                    },
                    onPausedClick = {
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
                        error = !error
                        if (error) filterTaskStates.add(TaskState.FAILED)
                        else filterTaskStates.remove(TaskState.FAILED)
                    },
                    onCompletedClick = {
                        completed = !completed
                        if (completed) filterTaskStates.add(TaskState.COMPLETED)
                        else filterTaskStates.remove(TaskState.COMPLETED)
                    }
                )
            }

            if (filteredTasks.isEmpty()) {
                val isFiltered = filterTaskStates.isNotEmpty()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
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
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    val state = rememberLazyListState()
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 28.dp),
                        state = state,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredTasks) { task ->
                            ScanTaskCard(
                                taskEntity = task,
                                onClick = { onTaskClick(task.id.value!!) },
                                currentTime = currentTime,
                            )
                        }
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(state),
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(end = 8.dp)
                            .width(10.dp)
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