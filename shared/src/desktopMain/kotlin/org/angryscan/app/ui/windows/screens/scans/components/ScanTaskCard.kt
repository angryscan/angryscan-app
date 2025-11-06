package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Http
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import org.angryscan.app.db.models.TaskState
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.Task_FoundAttributes
import org.angryscan.app.resources.aws_s3
import org.angryscan.app.scan.TaskEntityViewModel
import org.angryscan.app.scan.TaskFilesViewModel
import org.angryscan.app.scan.common.connectors.ConnectorFileShare
import org.angryscan.app.scan.common.connectors.ConnectorHTTP
import org.angryscan.app.scan.common.connectors.ConnectorS3
import org.angryscan.app.ui.extensions.color
import org.angryscan.app.ui.extensions.icon
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@OptIn(ExperimentalLayoutApi::class)
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(
                width = 1.dp,
                color = state.color(),
                shape = MaterialTheme.shapes.medium
            )
            .clickable(
                onClick = onClick
            )
            .padding(14.dp),

        ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .weight(1f)
                ) {
                    if (fastScan) {
                        Icon(
                            imageVector = Icons.Outlined.RocketLaunch,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center
                    ) {
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
                                modifier = Modifier
                                    .size(32.dp)
                                    .rotate(rotationAngle),
                                strokeWidth = 2.dp,
                                color = state.color()
                            )
                        }
                        
                        Icon(
                            imageVector = state.icon(),
                            contentDescription = null,
                            tint = state.color()
                        )
                    }

                    when(taskEntity.dbTask.connector) {
                        is ConnectorS3 -> {
                            Icon(
                                painter = painterResource(Res.drawable.aws_s3),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                            )
                        }
                        is ConnectorHTTP -> {
                            Icon(
                                imageVector = Icons.Outlined.Http,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                            )
                        }
                        is ConnectorFileShare -> {
                            Icon(
                                imageVector = Icons.Outlined.FolderOpen,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(32.dp)
                            )
                        }
                    }

                    Text(
                        text = name ?: path,
                        fontSize = MaterialTheme.typography.bodyMedium.fontSize,
                        lineHeight = MaterialTheme.typography.bodyMedium.lineHeight,
                        fontWeight = MaterialTheme.typography.bodyMedium.fontWeight,
                        letterSpacing = 0.1.sp,
                        style = TextStyle.Default.copy(
                            lineBreak = LineBreak.Heading
                        )
                    )
                }

            }

            Row(
                modifier = Modifier
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                ScanTimeStatItem(
                    startedAt = startedAt,
                    finishedAt = finishedAt,
                    pausedAt = pausedAt,
                    state = state
                )

                VerticalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.primary
                )

                ScanStat(
                    totalFiles = totalFiles,
                    selectedFiles = selectedFiles,
                    foundFiles = foundFiles,
                    folderSize = folderSize,
                    selectedFilesSize = selectedFilesSize,
                    foundFilesSize = foundFilesSize,
                    scanTime = scanTime,
                    scoreSum = scoreSum
                )
            }

            if (foundAttributes.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.Task_FoundAttributes),
                        fontSize = 14.sp,
                        letterSpacing = 0.1.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        foundAttributes.forEach { attr ->
                            AttributeCard(attr)
                        }
                    }
                }
            }
        }
    }
}