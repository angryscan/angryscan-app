package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.angryscan.app.db.models.TaskState
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.Task_FoundAttributes
import org.angryscan.app.resources.aws_s3
import org.angryscan.app.scan.TaskEntityViewModel
import org.angryscan.app.scan.TaskFilesViewModel
import org.angryscan.app.scan.common.connectors.ConnectorAIModels
import org.angryscan.app.scan.common.connectors.ConnectorFileShare
import org.angryscan.app.scan.common.connectors.ConnectorHTTP
import org.angryscan.app.scan.common.connectors.ConnectorS3
import org.angryscan.app.ui.extensions.color
import org.angryscan.app.ui.extensions.icon
import org.angryscan.app.ui.extensions.text
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toDuration

@OptIn(ExperimentalLayoutApi::class, ExperimentalTime::class)
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
    val selectedFiles by taskEntity.selectedFiles.collectAsState()
    val foundFiles by taskEntity.foundFiles.collectAsState()
    val totalFiles by taskEntity.totalFiles.collectAsState()

    val foundAttributes by taskEntity.foundAttributes.collectAsState()

    val folderSize by taskEntity.folderSize.collectAsState()
    val selectedFilesSize by taskEntity.selectedFilesSize.collectAsState()
    val foundFilesSize by taskEntity.foundFilesSize.collectAsState()

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

    val cardShape = RoundedCornerShape(20.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val colorScheme = MaterialTheme.colorScheme
    val accentBarWidth = 4.dp
    val iconSize = 30.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource = interactionSource)
            .clip(cardShape)
            .background(
                color = if (isHovered) colorScheme.surfaceVariant.copy(alpha = 0.35f)
                else colorScheme.surfaceVariant.copy(alpha = 0.15f),
                shape = cardShape
            )
            .border(
                width = 1.dp,
                color = if (isHovered) colorScheme.outlineVariant.copy(alpha = 0.5f)
                else colorScheme.outlineVariant.copy(alpha = 0.35f),
                shape = cardShape
            )
    ) {
        Box(
            modifier = Modifier
                .width(accentBarWidth)
                .fillMaxHeight()
                .background(state.color().copy(alpha = 0.55f), RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp))
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (fastScan) {
                    Icon(
                        imageVector = Icons.Outlined.RocketLaunch,
                        contentDescription = "Fast scan",
                        modifier = Modifier.size(iconSize),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Box(contentAlignment = Alignment.Center) {
                    if (state == TaskState.SCANNING) {
                        val infiniteTransition = rememberInfiniteTransition(label = "rotation")
                        val rotationAngle by infiniteTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "rotation"
                        )
                        CircularProgressIndicator(
                            modifier = Modifier.size(iconSize).rotate(rotationAngle),
                            strokeWidth = 2.dp,
                            color = state.color()
                        )
                    } else {
                        Icon(
                            imageVector = state.icon(),
                            contentDescription = state.text(),
                            tint = state.color(),
                            modifier = Modifier.size(iconSize)
                        )
                    }
                }
                when (taskEntity.dbTask.connector) {
                    is ConnectorS3 -> Icon(painter = painterResource(Res.drawable.aws_s3), contentDescription = "S3", modifier = Modifier.size(iconSize))
                    is ConnectorHTTP -> Icon(imageVector = Icons.Outlined.Http, contentDescription = "HTTP", modifier = Modifier.size(iconSize))
                    is ConnectorFileShare -> Icon(imageVector = Icons.Outlined.FolderOpen, contentDescription = "File share", modifier = Modifier.size(iconSize))
                    is ConnectorAIModels -> Icon(imageVector = Icons.Outlined.RocketLaunch, contentDescription = "AI models", modifier = Modifier.size(iconSize))
                }
                val pathInteractionSource = remember { MutableInteractionSource() }
                val pathHovered by pathInteractionSource.collectIsHoveredAsState()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .hoverable(pathInteractionSource)
                        .clickable(onClick = onClick)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .padding(vertical = 2.dp)
                ) {
                    Text(
                        text = when {
                            name != null && path.isNotBlank() && path != name -> "$name · $path"
                            else -> (name ?: path)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = colorScheme.onSurface,
                        textDecoration = if (pathHovered) TextDecoration.Underline else TextDecoration.None,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(state.color().copy(alpha = 0.2f))
                        .border(1.dp, state.color().copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = state.text(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = state.color()
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ScanTimeStatItem(
                    startedAt = startedAt,
                    finishedAt = finishedAt,
                    pausedAt = pausedAt,
                    state = state,
                    compact = true
                )
                ScanStatInline(
                    totalFiles = totalFiles,
                    selectedFiles = selectedFiles,
                    foundFiles = foundFiles,
                    folderSize = folderSize,
                    selectedFilesSize = selectedFilesSize,
                    foundFilesSize = foundFilesSize,
                    scanTime = scanTime,
                    scoreSum = scoreSum,
                    onClick = onClick,
                    compact = true
                )
            }

            if (foundAttributes.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = stringResource(Res.string.Task_FoundAttributes),
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        foundAttributes.toList().sortedByDescending { it.second }.forEach { attr ->
                            AttributeCard(attr.first, attr.second)
                        }
                    }
                }
            }
        }
    }
}