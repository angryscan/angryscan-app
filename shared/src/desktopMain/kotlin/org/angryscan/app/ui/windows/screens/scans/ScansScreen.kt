package org.angryscan.app.ui.windows.screens.scans

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.angryscan.app.db.models.TaskState
import org.angryscan.app.resources.*
import org.angryscan.app.scan.ScanService
import org.angryscan.app.ui.windows.screens.scans.components.ScanTaskCard
import org.angryscan.app.ui.windows.screens.scans.components.ScanTaskHeaderRow
import org.angryscan.app.ui.windows.screens.scans.components.StatusFilter
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Composable
fun ScansScreen(
    onTaskClick: (Int) -> Unit,
    onBackClick: (() -> Unit)? = null
) {
    val scanService = koinInject<ScanService>()

    var statusFilter by remember { mutableStateOf(StatusFilter.ALL) }

    val allTasks by scanService.tasks.tasks.collectAsState()

    val filterTaskStates = statusFilter.states

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
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(start = 16.dp, top = 18.dp, end = 16.dp, bottom = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (onBackClick != null) {
                TextButton(
                    onClick = onBackClick,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.Common_Back),
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(14.dp))
                .background(colorScheme.surfaceVariant.copy(alpha = 0.28f))
                .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.62f), RoundedCornerShape(14.dp))
                .padding(start = 10.dp, top = 8.dp, end = 4.dp, bottom = 8.dp)
        ) {
            if (allTasks.isNotEmpty() && visibleTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
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
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = stringResource(Res.string.MainScreen_RecentScans_Empty),
                            modifier = Modifier.size(36.dp),
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                        )
                        Text(
                            text = if (isFiltered) stringResource(Res.string.ScansPage_NoMatches)
                            else stringResource(Res.string.MainScreen_RecentScans_Empty),
                            style = MaterialTheme.typography.titleMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(Res.string.ScansPage_EmptyHint),
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            } else {
                val state = rememberLazyListState()
                Column(modifier = Modifier.fillMaxSize()) {
                    ScanTaskHeaderRow(
                        modifier = Modifier.padding(
                            start = 0.dp,
                            end = 16.dp,
                            top = 2.dp,
                            bottom = 2.dp
                        ),
                        statusFilter = statusFilter,
                        statusCounts = mapOf(
                            StatusFilter.ALL to visibleTasks.size,
                            StatusFilter.ACTIVE to countActive,
                            StatusFilter.PAUSED to countPaused,
                            StatusFilter.ERROR to countError,
                            StatusFilter.COMPLETED to countCompleted
                        ),
                        onStatusFilterChange = { statusFilter = it }
                    )
                    Box(modifier = Modifier.fillMaxSize()) {
                        var expandedTaskId by remember { mutableStateOf<Int?>(null) }
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 16.dp),
                            state = state,
                            contentPadding = PaddingValues(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(
                                items = filteredTasks,
                                key = { task -> task.id.value ?: task.hashCode() }
                            ) { task ->
                                ScanTaskCard(
                                    taskEntity = task,
                                    onClick = {
                                        focusManager.clearFocus()
                                        onTaskClick(task.id.value!!)
                                    },
                                    currentTime = currentTime,
                                    attributesExpanded = task.id.value != null && expandedTaskId == task.id.value,
                                    onAttributesExpandClick = {
                                        val id = task.id.value ?: return@ScanTaskCard
                                        expandedTaskId = if (expandedTaskId == id) null else id
                                    }
                                )
                            }
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(state),
                            modifier = Modifier
                                .fillMaxHeight()
                                .padding(end = 4.dp)
                                .width(8.dp)
                                .align(Alignment.CenterEnd),
                            style = LocalScrollbarStyle.current.copy(
                                unhoverColor = colorScheme.outlineVariant.copy(alpha = 0.55f),
                                hoverColor = colorScheme.primary
                            )
                        )
                    }
                }
            }
        }
    }
}
