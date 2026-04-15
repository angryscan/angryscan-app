package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.runtime.Composable
import org.angryscan.app.db.models.TaskState
import org.angryscan.app.resources.*
import org.jetbrains.compose.resources.stringResource

enum class StatusFilter(val states: Set<TaskState>) {
    ALL(emptySet()),
    ACTIVE(setOf(TaskState.SCANNING, TaskState.SEARCHING)),
    PAUSED(setOf(TaskState.STOPPED, TaskState.PENDING)),
    ERROR(setOf(TaskState.FAILED)),
    COMPLETED(setOf(TaskState.COMPLETED))
}

@Composable
fun StatusFilter.label(): String = when (this) {
    StatusFilter.ALL -> stringResource(Res.string.ScansPage_FilterAll)
    StatusFilter.ACTIVE -> stringResource(Res.string.TaskStateChipFilter_Active)
    StatusFilter.PAUSED -> stringResource(Res.string.TaskStateChipFilter_Paused)
    StatusFilter.ERROR -> stringResource(Res.string.TaskStateChipFilter_Error)
    StatusFilter.COMPLETED -> stringResource(Res.string.TaskStateChipFilter_Completed)
}

