package org.angryscan.app.ui.extensions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.angryscan.app.db.models.TaskState
import org.angryscan.app.resources.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun TaskState.color() = when (this) {
    TaskState.LOADING, TaskState.SCANNING, TaskState.SEARCHING -> MaterialTheme.colorScheme.primary
    TaskState.COMPLETED -> MaterialTheme.colorScheme.tertiary
    TaskState.STOPPED, TaskState.PENDING -> MaterialTheme.colorScheme.secondary
    TaskState.FAILED -> MaterialTheme.colorScheme.error
}

@Composable
fun TaskState.icon() = when (this) {
    TaskState.LOADING, TaskState.SCANNING, TaskState.SEARCHING -> Icons.Outlined.PlayArrow
    TaskState.COMPLETED -> Icons.Outlined.CheckCircle
    TaskState.STOPPED, TaskState.PENDING -> Icons.Outlined.Pause
    TaskState.FAILED -> Icons.Outlined.Warning
}

@Composable
fun TaskState.text() = stringResource(
    when (this) {
        TaskState.LOADING, TaskState.SCANNING, TaskState.SEARCHING -> Res.string.TaskState_Active
        TaskState.COMPLETED -> Res.string.TaskState_Completed
        TaskState.STOPPED, TaskState.PENDING -> Res.string.TaskState_Paused
        TaskState.FAILED -> Res.string.TaskState_Error
    }
)