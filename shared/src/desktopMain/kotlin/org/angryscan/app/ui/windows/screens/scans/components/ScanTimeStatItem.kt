package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDateTime
import org.angryscan.app.db.models.TaskState
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.Task_FinishedAt
import org.angryscan.app.resources.Task_PausedAt
import org.angryscan.app.resources.Task_StartedAt
import org.angryscan.app.ui.windows.components.DateFormat
import org.jetbrains.compose.resources.stringResource

@Preview
@Composable
fun ScanTimeStatItem(
    startedAt: LocalDateTime?,
    finishedAt: LocalDateTime?,
    pausedAt: LocalDateTime?,
    state: TaskState
) {
    val compact = true
    if (compact) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "${stringResource(Res.string.Task_StartedAt)} ${startedAt?.let { DateFormat.format(it) } ?: ""}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (finishedAt != null && state == TaskState.COMPLETED) {
                Text(
                    text = "${stringResource(Res.string.Task_FinishedAt)} ${DateFormat.format(finishedAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (pausedAt != null && (state == TaskState.STOPPED || state == TaskState.PENDING)) {
                Text(
                    text = "${stringResource(Res.string.Task_PausedAt)} ${DateFormat.format(pausedAt)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Column {
                Text(
                    text = stringResource(resource = Res.string.Task_StartedAt),
                    fontSize = 14.sp,
                    letterSpacing = 0.1.sp,
                    maxLines = 1
                )
                if (finishedAt != null && state == TaskState.COMPLETED) {
                    Text(
                        text = stringResource(Res.string.Task_FinishedAt),
                        fontSize = 14.sp,
                        letterSpacing = 0.1.sp,
                        maxLines = 1
                    )
                } else if (pausedAt != null && (state == TaskState.STOPPED || state == TaskState.PENDING)) {
                    Text(
                        text = stringResource(resource = Res.string.Task_PausedAt),
                        fontSize = 14.sp,
                        letterSpacing = 0.1.sp,
                        maxLines = 1
                    )
                }
            }
            Column {
                Text(
                    text = startedAt?.let { DateFormat.format(it) } ?: "",
                    fontSize = 14.sp,
                    letterSpacing = 0.1.sp,
                    maxLines = 1
                )
                if (finishedAt != null && state == TaskState.COMPLETED) {
                    Text(
                        text = DateFormat.format(finishedAt),
                        fontSize = 14.sp,
                        letterSpacing = 0.1.sp,
                        maxLines = 1
                    )
                } else if (pausedAt != null && (state == TaskState.STOPPED || state == TaskState.PENDING)) {
                    Text(
                        text = DateFormat.format(pausedAt),
                        fontSize = 14.sp,
                        letterSpacing = 0.1.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}