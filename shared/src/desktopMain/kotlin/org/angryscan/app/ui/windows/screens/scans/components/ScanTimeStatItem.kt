package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import org.angryscan.app.db.models.TaskState
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.Task_FinishedAt
import org.angryscan.app.resources.Task_PausedAt
import org.angryscan.app.resources.Task_StartedAt
import org.angryscan.app.ui.windows.components.DateFormat
import org.jetbrains.compose.resources.stringResource

// Такой же вид, как StatChip. compact: labelSmall и меньше padding.
private val timeChipShape = RoundedCornerShape(8.dp)

@Composable
private fun TimeChip(
    text: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Text(
        text = text,
        style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
        color = colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(timeChipShape)
            .background(colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(
                horizontal = if (compact) 6.dp else 10.dp,
                vertical = if (compact) 4.dp else 6.dp
            )
    )
}

// Как ScanStatChip: иконка + Column(title labelSmall, value bodySmall), padding 8/4.
@Composable
private fun TimeChipAsScanStat(
    icon: ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(timeChipShape)
            .background(colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(14.dp),
            tint = colorScheme.primary.copy(alpha = 0.9f)
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview
@Composable
fun ScanTimeStatItem(
    startedAt: LocalDateTime?,
    finishedAt: LocalDateTime?,
    pausedAt: LocalDateTime?,
    state: TaskState,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    /** true = как ScanStatChip (иконка + title/value), только для карточки результата сканирования */
    useScanStatChipStyle: Boolean = false
) {
    val spacing = when {
        compact -> 4.dp
        useScanStatChipStyle -> 6.dp
        else -> 6.dp
    }
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (useScanStatChipStyle) {
            TimeChipAsScanStat(
                icon = Icons.Outlined.Schedule,
                title = stringResource(Res.string.Task_StartedAt),
                value = startedAt?.let { DateFormat.format(it) } ?: ""
            )
            if (finishedAt != null && state == TaskState.COMPLETED) {
                TimeChipAsScanStat(
                    icon = Icons.Outlined.CheckCircle,
                    title = stringResource(Res.string.Task_FinishedAt),
                    value = DateFormat.format(finishedAt)
                )
            } else if (pausedAt != null && (state == TaskState.STOPPED || state == TaskState.PENDING)) {
                TimeChipAsScanStat(
                    icon = Icons.Outlined.Pause,
                    title = stringResource(Res.string.Task_PausedAt),
                    value = DateFormat.format(pausedAt)
                )
            }
        } else {
            TimeChip(
                text = "${stringResource(Res.string.Task_StartedAt)} ${startedAt?.let { DateFormat.format(it) } ?: ""}",
                compact = compact
            )
            if (finishedAt != null && state == TaskState.COMPLETED) {
                TimeChip(
                    text = "${stringResource(Res.string.Task_FinishedAt)} ${DateFormat.format(finishedAt)}",
                    compact = compact
                )
            } else if (pausedAt != null && (state == TaskState.STOPPED || state == TaskState.PENDING)) {
                TimeChip(
                    text = "${stringResource(Res.string.Task_PausedAt)} ${DateFormat.format(pausedAt)}",
                    compact = compact
                )
            }
        }
    }
}