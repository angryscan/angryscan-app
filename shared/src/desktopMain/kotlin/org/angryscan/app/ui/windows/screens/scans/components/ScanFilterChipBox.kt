package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.angryscan.app.db.models.TaskState
import org.angryscan.app.resources.*
import org.angryscan.app.ui.extensions.color
import org.angryscan.app.ui.extensions.icon
import org.jetbrains.compose.resources.stringResource

@Composable
fun ScanFilterChipBox(
    active: Boolean,
    paused: Boolean,
    error: Boolean,
    completed: Boolean,
    countActive: Int = 0,
    countPaused: Int = 0,
    countError: Int = 0,
    countCompleted: Int = 0,
    onAllClick: () -> Unit,
    onActiveClick: () -> Unit,
    onPausedClick: () -> Unit,
    onErrorClick: () -> Unit,
    onCompletedClick: () -> Unit
) {
    val anyFilter = active || paused || error || completed
    val colorScheme = MaterialTheme.colorScheme
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScanFilterChip(
            text = stringResource(Res.string.ScansPage_FilterAll),
            selected = !anyFilter,
            onClick = onAllClick,
            icon = Icons.Outlined.FilterList,
            tint = colorScheme.onSurfaceVariant
        )
        ScanFilterChip(
            text = formatChipLabel(stringResource(Res.string.TaskStateChipFilter_Active), countActive),
            selected = active,
            onClick = onActiveClick,
            icon = TaskState.SCANNING.icon(),
            tint = TaskState.SCANNING.color()
        )
        ScanFilterChip(
            text = formatChipLabel(stringResource(Res.string.TaskStateChipFilter_Paused), countPaused),
            selected = paused,
            onClick = onPausedClick,
            icon = TaskState.STOPPED.icon(),
            tint = TaskState.STOPPED.color()
        )
        ScanFilterChip(
            text = formatChipLabel(stringResource(Res.string.TaskStateChipFilter_Error), countError),
            selected = error,
            onClick = onErrorClick,
            icon = TaskState.FAILED.icon(),
            tint = TaskState.FAILED.color()
        )
        ScanFilterChip(
            text = formatChipLabel(stringResource(Res.string.TaskStateChipFilter_Completed), countCompleted),
            selected = completed,
            onClick = onCompletedClick,
            icon = TaskState.COMPLETED.icon(),
            tint = TaskState.COMPLETED.color()
        )
    }
}

private fun formatChipLabel(label: String, count: Int): String =
    if (count > 0) "$label ($count)" else label