package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.angryscan.app.db.models.TaskState
import org.angryscan.app.resources.*
import org.angryscan.app.scan.TaskEntityViewModel
import org.angryscan.app.scan.TaskFilesViewModel
import org.angryscan.app.scan.common.connectors.ConnectorFileShare
import org.angryscan.app.scan.common.connectors.ConnectorHTTP
import org.angryscan.app.scan.common.connectors.ConnectorS3
import org.angryscan.app.ui.extensions.color
import org.angryscan.app.ui.extensions.text
import org.angryscan.app.ui.windows.components.DescriptionTooltip
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toDuration

val ScanListStatusColumnWidth = 156.dp
val ScanListFoundColumnWidth = 118.dp
val ScanListScoreColumnWidth = 96.dp
val ScanListTimeColumnWidth = 118.dp
val ScanListAttributesColumnWidth = 256.dp
val ScanListChevronColumnWidth = 28.dp
val ScanListRowHorizontalPadding = 12.dp
val ScanListMainToMetricsGap = 12.dp
val ScanListMetricsWidth =
    ScanListStatusColumnWidth +
        ScanListFoundColumnWidth +
        ScanListScoreColumnWidth +
        ScanListTimeColumnWidth +
        ScanListChevronColumnWidth

@OptIn(ExperimentalTime::class)
@Composable
fun ScanTaskCard(
    taskEntity: TaskEntityViewModel,
    onClick: () -> Unit,
    currentTime: Instant
) {
    val state by taskEntity.state.collectAsState()
    val fastScan by taskEntity.fastScan.collectAsState()
    val path by taskEntity.path.collectAsState()
    val name by taskEntity.name.collectAsState()
    val startedAt by taskEntity.startedAt.collectAsState()
    val finishedAt by taskEntity.finishedAt.collectAsState()
    val pausedAt by taskEntity.pausedAt.collectAsState()
    val foundFiles by taskEntity.foundFiles.collectAsState()
    val totalFiles by taskEntity.totalFiles.collectAsState()
    val foundAttributes by taskEntity.foundAttributes.collectAsState()

    val pausedAtInstant = pausedAt?.toInstant(TimeZone.currentSystemDefault())
    val startedAtInstant = startedAt?.toInstant(TimeZone.currentSystemDefault())
    val deltaSeconds by taskEntity.deltaSeconds.collectAsState()

    val deltaDuration = (deltaSeconds ?: 0L).toDuration(DurationUnit.SECONDS)

    val taskFilesViewModel = koinInject<TaskFilesViewModel> { parametersOf(taskEntity.dbTask) }
    val scoreSum by taskFilesViewModel.scoreSum.collectAsState()

    val scanTime = if (startedAt != null) {
        when (state) {
            TaskState.COMPLETED -> finishedAt!!.toInstant(TimeZone.currentSystemDefault()) - startedAtInstant!! - deltaDuration
            TaskState.STOPPED, TaskState.PENDING -> (pausedAtInstant ?: startedAtInstant!!) - startedAtInstant!! - deltaDuration
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

    val rowShape = RoundedCornerShape(12.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val colorScheme = MaterialTheme.colorScheme
    val sourceLabel = when (taskEntity.dbTask.connector) {
        is ConnectorS3 -> stringResource(Res.string.MainScreen_SourceType_S3)
        is ConnectorHTTP -> stringResource(Res.string.MainScreen_SourceType_HTTP)
        is ConnectorFileShare -> stringResource(Res.string.MainScreen_SourceType_FileShare)
        else -> stringResource(Res.string.ScansPage_SourceFallback)
    }
    val title = name ?: path
    val startFull = startedAt?.let(::formatDateTimeWithSeconds) ?: "-"
    val finishFull = finishedAt?.let(::formatDateTimeWithSeconds) ?: "-"
    val startTime = startedAt?.let(::formatTimeWithSeconds) ?: "-"
    val finishTime = finishedAt?.let(::formatTimeWithSeconds) ?: "-"
    val detailsTooltip = buildString {
        append(stringResource(Res.string.ScansPage_StartLabel)).append(": ").append(startFull)
        append('\n')
        append(stringResource(Res.string.ScansPage_FinishLabel)).append(": ").append(finishFull)
    }
    val details = buildString {
        append(stringResource(Res.string.ScansPage_StartLabel)).append(' ')
        append(startTime)
        append(" · ").append(stringResource(Res.string.ScansPage_FinishLabel)).append(' ')
        append(finishTime)
        append(" · ")
        append(sourceLabel)
        if (fastScan) append(" · ").append(stringResource(Res.string.ScansPage_SourceFast))
    }
    val filesLabel = "$foundFiles / $totalFiles"
    val sortedAttributes = remember(foundAttributes) {
        foundAttributes.toList().sortedByDescending { it.second }
    }
    val topAttributes = remember(sortedAttributes) { sortedAttributes.take(2) }
    val extraAttributesCount = (sortedAttributes.size - 2).coerceAtLeast(0)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource = interactionSource)
            .clip(rowShape)
            .background(
                if (isHovered) colorScheme.surfaceVariant.copy(alpha = 0.34f)
                else colorScheme.surfaceVariant.copy(alpha = 0.16f),
                rowShape
            )
            .border(
                1.dp,
                if (isHovered) colorScheme.outlineVariant.copy(alpha = 0.55f) else colorScheme.outlineVariant.copy(alpha = 0.26f),
                rowShape
            )
            .clickable(onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = ScanListRowHorizontalPadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val titleInteractionSource = remember { MutableInteractionSource() }
            val isTitleHovered by titleInteractionSource.collectIsHoveredAsState()
            DescriptionTooltip(description = path) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isTitleHovered) colorScheme.primary else colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .hoverable(interactionSource = titleInteractionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                )
            }
            DescriptionTooltip(description = detailsTooltip) {
                Text(
                    text = details,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier.width(ScanListAttributesColumnWidth),
            contentAlignment = Alignment.Center
        ) {
            if (topAttributes.isEmpty()) {
                Text(
                    text = "-",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                val first = topAttributes.getOrNull(0)
                val second = topAttributes.getOrNull(1)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 1.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (first != null) {
                        AttributeHoverText(
                            name = first.first.name,
                            count = first.second
                        )
                    }
                    if (second != null) {
                        Text(
                            text = ", ",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                        )
                        AttributeHoverText(
                            name = second.first.name,
                            count = second.second
                        )
                    }
                    if (extraAttributesCount > 0) {
                        Text(
                            text = if (first != null || second != null) ", +$extraAttributesCount" else "+$extraAttributesCount",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(ScanListMainToMetricsGap))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.width(ScanListMetricsWidth)
        ) {
            Box(
                modifier = Modifier.width(ScanListStatusColumnWidth),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(state.color().copy(alpha = 0.18f))
                        .border(1.dp, state.color().copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = state.text(),
                        style = MaterialTheme.typography.labelMedium,
                        color = state.color(),
                        maxLines = 1
                    )
                }
            }

            Column(
                modifier = Modifier
                    .width(ScanListFoundColumnWidth),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = filesLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = stringResource(Res.string.ScansPage_MetricFoundFiles),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                )
            }

            Column(
                modifier = Modifier.width(ScanListScoreColumnWidth),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = scoreSum.toString(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = stringResource(Res.string.ScansPage_MetricScore),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                )
            }

            Column(
                modifier = Modifier.width(ScanListTimeColumnWidth),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = scanTime,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    color = colorScheme.onSurface,
                    maxLines = 1
                )
                Text(
                    text = stringResource(Res.string.ScansPage_MetricDuration),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                )
            }
            Box(
                modifier = Modifier.width(ScanListChevronColumnWidth),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatDateTimeWithSeconds(value: LocalDateTime): String {
    val dd = value.day.toString().padStart(2, '0')
    val mmDate = value.month.ordinal.plus(1).toString().padStart(2, '0')
    val yyyy = value.year.toString().padStart(4, '0')
    val hh = value.hour.toString().padStart(2, '0')
    val mmTime = value.minute.toString().padStart(2, '0')
    val ss = value.second.toString().padStart(2, '0')
    return "$dd.$mmDate.$yyyy $hh:$mmTime:$ss"
}

private fun formatTimeWithSeconds(value: LocalDateTime): String {
    val hh = value.hour.toString().padStart(2, '0')
    val mmTime = value.minute.toString().padStart(2, '0')
    val ss = value.second.toString().padStart(2, '0')
    return "$hh:$mmTime:$ss"
}

@Composable
private fun AttributeHoverText(
    name: String,
    count: Int
) {
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    DescriptionTooltip(description = stringResource(Res.string.ScansPage_TooltipFound, count)) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = if (isHovered) colorScheme.primary else colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .hoverable(interactionSource = interactionSource)
                .pointerHoverIcon(PointerIcon.Hand)
        )
    }
}
