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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.angryscan.app.db.models.TaskState
import org.angryscan.app.resources.*
import org.angryscan.app.scan.TaskEntityViewModel
import org.angryscan.app.scan.calculateTaskScore
import org.angryscan.app.scan.common.connectors.*
import org.angryscan.app.ui.extensions.toHumanReadable
import org.angryscan.app.ui.strings.composableName
import org.angryscan.app.ui.windows.components.DescriptionTooltip
import org.angryscan.app.ui.windows.screens.main.LocalMainScreenAdaptiveTokens
import org.angryscan.common.engine.IMatcher
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.*

val ScanListFinishedColumnWidth = 92.dp
val ScanListDurationColumnWidth = 104.dp
val ScanListStatusColumnWidth = 110.dp
val ScanListObjectSizeColumnWidth = 98.dp
val ScanListPiiFoundColumnWidth = 92.dp
val ScanListPiiSizeColumnWidth = 98.dp
val ScanListPiiScoreColumnWidth = 92.dp
val ScanListAttributesColumnWidth = 232.dp
val ScanListChevronColumnWidth = 56.dp
val ScanListRowHorizontalPadding = 12.dp
val ScanListMainToMetricsGap = 12.dp
val ScanListMetricsWidth =
    ScanListFinishedColumnWidth +
        ScanListDurationColumnWidth +
        ScanListStatusColumnWidth +
        ScanListObjectSizeColumnWidth +
        ScanListPiiFoundColumnWidth +
        ScanListPiiSizeColumnWidth +
        ScanListPiiScoreColumnWidth +
        ScanListAttributesColumnWidth +
        ScanListChevronColumnWidth

@Composable
fun ScanTaskHeaderRow(
    modifier: Modifier = Modifier
) {
    ScanTaskHeaderRow(
        modifier = modifier,
        statusFilter = null,
        statusCounts = null,
        onStatusFilterChange = null
    )
}

@Composable
fun ScanTaskHeaderRow(
    modifier: Modifier = Modifier,
    statusFilter: StatusFilter? = null,
    statusCounts: Map<StatusFilter, Int>? = null,
    onStatusFilterChange: ((StatusFilter) -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val scanTokens = LocalMainScreenAdaptiveTokens.current.scanList
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = scanTokens.rowHorizontalPadding, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HeaderCell(stringResource(Res.string.ScansPage_ColumnFinishedTime), modifier = Modifier.width(scanTokens.finishedColumnWidth), centered = false)
        HeaderCell(stringResource(Res.string.ScansPage_ColumnDuration), modifier = Modifier.width(scanTokens.durationColumnWidth), centered = true)
        StatusHeaderCell(
            modifier = Modifier.width(scanTokens.statusColumnWidth),
            statusFilter = statusFilter,
            statusCounts = statusCounts,
            onStatusFilterChange = onStatusFilterChange
        )
        HeaderCell(
            stringResource(Res.string.ScansPage_ColumnPath),
            modifier = Modifier.weight(1f).padding(end = 6.dp),
            centered = true
        )
        HeaderCell(stringResource(Res.string.ScansPage_ColumnObjectSize), modifier = Modifier.width(scanTokens.objectSizeColumnWidth), centered = true)
        HeaderCell(stringResource(Res.string.ScansPage_ColumnPiiFound), modifier = Modifier.width(scanTokens.piiFoundColumnWidth), centered = true)
        HeaderCell(stringResource(Res.string.ScansPage_ColumnPiiSize), modifier = Modifier.width(scanTokens.piiSizeColumnWidth), centered = true)
        HeaderCell(stringResource(Res.string.ScansPage_ColumnPiiScore), modifier = Modifier.width(scanTokens.piiScoreColumnWidth), centered = true)
        HeaderCell(stringResource(Res.string.ScansPage_ColumnPiiAttributesFound), modifier = Modifier.width(scanTokens.attributesColumnWidth), centered = true)
        Spacer(modifier = Modifier.width(scanTokens.chevronColumnWidth))
    }
    HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.22f))
}

@Composable
private fun StatusHeaderCell(
    modifier: Modifier,
    statusFilter: StatusFilter?,
    statusCounts: Map<StatusFilter, Int>?,
    onStatusFilterChange: ((StatusFilter) -> Unit)?
) {
    val colorScheme = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    val clickable = statusFilter != null && onStatusFilterChange != null

    Box(
        modifier = modifier.heightIn(min = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!clickable) {
            Text(
                text = stringResource(Res.string.ScansPage_ColumnStatus),
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { expanded = true }
                    .pointerHoverIcon(PointerIcon.Hand)
                    .padding(horizontal = 0.dp, vertical = 0.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(Res.string.ScansPage_ColumnStatus),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Outlined.ArrowDropDown,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                    modifier = Modifier.size(16.dp)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                StatusFilter.entries.forEach { item ->
                    val count = statusCounts?.get(item)
                    DropdownMenuItem(
                        leadingIcon = {
                            if (statusFilter == item) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null
                                )
                            }
                        },
                        text = {
                            Text(
                                text = if (count != null) "${item.label()} ($count)" else item.label()
                            )
                        },
                        onClick = {
                            onStatusFilterChange?.invoke(item)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderCell(
    text: String,
    modifier: Modifier,
    centered: Boolean = false,
) {
    Box(modifier = modifier, contentAlignment = if (centered) Alignment.Center else Alignment.CenterStart) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@OptIn(ExperimentalTime::class)
@Composable
fun ScanTaskCard(
    taskEntity: TaskEntityViewModel,
    onClick: () -> Unit,
    currentTime: Instant,
    attributesExpanded: Boolean = false,
    onAttributesExpandClick: (() -> Unit)? = null,
    onRescanClick: (() -> Unit)? = null,
    onEditAndRunClick: (() -> Unit)? = null,
) {
    val state by taskEntity.state.collectAsState()
    val path by taskEntity.path.collectAsState()
    val name by taskEntity.name.collectAsState()
    val startedAt by taskEntity.startedAt.collectAsState()
    val finishedAt by taskEntity.finishedAt.collectAsState()
    val pausedAt by taskEntity.pausedAt.collectAsState()
    val foundFiles by taskEntity.foundFiles.collectAsState()
    val folderSize by taskEntity.folderSize.collectAsState()
    val foundFilesSize by taskEntity.foundFilesSize.collectAsState()
    val foundAttributes by taskEntity.foundAttributes.collectAsState()

    val pausedAtInstant = pausedAt?.toInstant(TimeZone.currentSystemDefault())
    val startedAtInstant = startedAt?.toInstant(TimeZone.currentSystemDefault())
    val deltaSeconds by taskEntity.deltaSeconds.collectAsState()

    val deltaDuration = (deltaSeconds ?: 0L).toDuration(DurationUnit.SECONDS)

    val scoreSum = remember(foundAttributes) { calculateTaskScore(foundAttributes) }

    val scanDuration: Duration = if (startedAt != null) {
        when (state) {
            TaskState.COMPLETED -> finishedAt!!.toInstant(TimeZone.currentSystemDefault()) - startedAtInstant!! - deltaDuration
            TaskState.STOPPED, TaskState.PENDING -> (pausedAtInstant ?: startedAtInstant!!) - startedAtInstant!! - deltaDuration
            else -> currentTime - startedAtInstant!! - deltaDuration
        }
    } else {
        0L.toDuration(DurationUnit.SECONDS)
    }

    val scanTokens = LocalMainScreenAdaptiveTokens.current.scanList
    val adaptiveScale = LocalMainScreenAdaptiveTokens.current.scale
    val rowShape = RoundedCornerShape(scanTokens.rowCorner)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val connector = taskEntity.dbTask.connector
    val colorScheme = MaterialTheme.colorScheme
    val databaseConnectionLabel = when (connector) {
        is ConnectorPostgres -> "${connector.host}:${connector.port}/${connector.database}"
        is ConnectorMySQL -> "${connector.host}:${connector.port}/${connector.database}"
        is ConnectorGreenPlum -> "${connector.host}:${connector.port}/${connector.database}"
        is ConnectorHive -> "${connector.host}:${connector.port}/${connector.database}"
        is ConnectorCockroachDB -> "${connector.host}:${connector.port}/${connector.database}"
        is ConnectorClickHouse -> "${connector.host}:${connector.port}/${connector.database}"
        is ConnectorRedshift -> "${connector.host}:${connector.port}/${connector.database}"
        is ConnectorSqlServer -> "${connector.host}:${connector.port}/${connector.database}"
        is ConnectorMongoDB -> "${connector.host}:${connector.port}/${connector.database}"
        is ConnectorSqlite -> connector.filePath
        else -> null
    }
    val title = run {
        val normalizedName = name?.trim().orEmpty()
        val fallbackPathTitle = path.substringAfterLast('/').substringAfterLast('\\')
        val schemaSuffix = if (path.isBlank()) "" else " ${stringResource(Res.string.Result_CardSchema)}: ${path.trim()}"
        when {
            connector is IDatabaseConnector && (normalizedName.isBlank() || normalizedName == path.trim()) ->
                (databaseConnectionLabel ?: fallbackPathTitle) + schemaSuffix
            normalizedName.isNotBlank() -> normalizedName
            else -> fallbackPathTitle
        }
    }
    val sourceTypeLabel = when (connector) {
        is ConnectorS3 -> stringResource(Res.string.MainScreen_SourceType_S3)
        is ConnectorHTTP -> stringResource(Res.string.MainScreen_SourceType_HTTP)
        is IDatabaseConnector -> stringResource(Res.string.MainScreen_SourceType_Postgres)
        else -> stringResource(Res.string.MainScreen_SourceType_FileShare)
    }
    val finishedLabel = formatFinishedLabel(finishedAt, currentTime)
    val durationLabel = formatShortDuration(scanDuration)
    val statusLabel = taskStatusLabel(state)
    val objectSizeLabel = compactSize(folderSize)
    val piiSizeLabel = if (foundFilesSize > 0) compactSize(foundFilesSize.toHumanReadable()) else "-"
    val riskLevelLabel = when {
        scoreSum >= 500L -> stringResource(Res.string.ScansPage_RiskLevel_High)
        scoreSum >= 100L -> stringResource(Res.string.ScansPage_RiskLevel_Medium)
        else -> stringResource(Res.string.ScansPage_RiskLevel_Low)
    }
    val valueTextStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
    val sortedAttributes = remember(foundAttributes) {
        foundAttributes.toList().sortedByDescending { it.second }
    }
    val topAttributes = remember(sortedAttributes, attributesExpanded) {
        when {
            attributesExpanded && sortedAttributes.size > 1 -> sortedAttributes.take(1)
            else -> sortedAttributes.take(2)
        }
    }
    val extraAttributesCount = (sortedAttributes.size - topAttributes.size).coerceAtLeast(0)
    val extraAttributes = remember(sortedAttributes, topAttributes) { sortedAttributes.drop(topAttributes.size) }
    var actionsExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .hoverable(interactionSource = interactionSource)
            .clip(rowShape)
            .background(
                if (isHovered) colorScheme.surfaceVariant.copy(alpha = 0.46f)
                else colorScheme.surfaceVariant.copy(alpha = 0.28f),
                rowShape
            )
            .border(
                adaptiveScale.dp(1.dp, min = 1.dp, max = 1.4.dp),
                if (isHovered) colorScheme.outlineVariant.copy(alpha = 0.7f) else colorScheme.outlineVariant.copy(alpha = 0.48f),
                rowShape
            )
            .clickable(onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = scanTokens.rowHorizontalPadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(scanTokens.finishedColumnWidth)
                .heightIn(min = 22.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = finishedLabel,
                style = valueTextStyle,
                color = colorScheme.onSurface,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .width(scanTokens.durationColumnWidth)
                .heightIn(min = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = durationLabel,
                style = valueTextStyle,
                color = colorScheme.onSurface,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .width(scanTokens.statusColumnWidth)
                .heightIn(min = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(end = adaptiveScale.dp(6.dp, min = 4.dp, max = 10.dp)),
            contentAlignment = Alignment.CenterStart
        ) {
            val sourceTooltip = buildString {
                append(sourceTypeLabel)
                if (connector is IDatabaseConnector && databaseConnectionLabel != null) {
                    append('\n')
                    append(databaseConnectionLabel)
                    if (path.isNotBlank()) {
                        append('\n')
                        append("${stringResource(Res.string.Result_CardSchema)}: ${path.trim()}")
                    }
                } else if (path.isNotBlank()) {
                    append('\n')
                    append(path)
                }
            }
            DescriptionTooltip(description = sourceTooltip) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    when (connector) {
                        is ConnectorS3 -> Icon(
                            painter = painterResource(Res.drawable.aws_s3),
                            contentDescription = sourceTypeLabel,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(14.dp)
                        )
                        is ConnectorHTTP -> Icon(
                            imageVector = Icons.Outlined.Language,
                            contentDescription = sourceTypeLabel,
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                            modifier = Modifier.size(14.dp)
                        )
                        is IDatabaseConnector -> Icon(
                            painter = painterResource(Res.drawable.db_default_logo),
                            contentDescription = sourceTypeLabel,
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                            modifier = Modifier.size(14.dp)
                        )
                        else -> Icon(
                            imageVector = Icons.Outlined.Folder,
                            contentDescription = sourceTypeLabel,
                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .pointerHoverIcon(PointerIcon.Hand),
                        textAlign = TextAlign.Start
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .width(scanTokens.objectSizeColumnWidth)
                .heightIn(min = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = objectSizeLabel,
                style = valueTextStyle,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .width(scanTokens.piiFoundColumnWidth)
                .heightIn(min = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            DescriptionTooltip(
                description = stringResource(Res.string.ScansPage_TooltipPiiFoundFiles, foundFiles)
            ) {
                Text(
                    text = foundFiles.toString(),
                    style = valueTextStyle,
                    color = colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }

        Box(
            modifier = Modifier
                .width(scanTokens.piiSizeColumnWidth)
                .heightIn(min = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            DescriptionTooltip(
                description = stringResource(Res.string.ScansPage_TooltipPiiSizeFilesTotal, piiSizeLabel)
            ) {
                Text(
                    text = piiSizeLabel,
                    style = valueTextStyle,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        Box(
            modifier = Modifier
                .width(scanTokens.piiScoreColumnWidth)
                .heightIn(min = 22.dp),
            contentAlignment = Alignment.Center
        ) {
            DescriptionTooltip(
                description = stringResource(
                    Res.string.ScansPage_TooltipPiiScoreLogic,
                    riskLevelLabel,
                    scoreSum
                )
            ) {
                Text(
                    text = scoreSum.toString(),
                    style = valueTextStyle,
                    color = colorScheme.onSurface,
                    maxLines = 1
                )
            }
        }

        Box(
            modifier = Modifier
                .width(scanTokens.attributesColumnWidth)
                .heightIn(min = 22.dp),
            contentAlignment = if (topAttributes.isEmpty()) Alignment.Center else Alignment.CenterStart
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 1.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (first != null) {
                            AttributeHoverText(
                                matcher = first.first,
                                count = first.second
                            )
                        }
                        if (!attributesExpanded && second != null) {
                            Text(
                                text = ", ",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                            )
                            AttributeHoverText(
                                matcher = second.first,
                                count = second.second
                            )
                        }

                        if (extraAttributesCount > 0) {
                            if (first != null && second == null) {
                                Text(
                                    text = ", ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                                )
                            } else if (second != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            val label = if (attributesExpanded) "Collapse" else "+$extraAttributesCount"
                            if (onAttributesExpandClick != null) {
                                val expandInteractionSource = remember { MutableInteractionSource() }
                                val isExpandHovered by expandInteractionSource.collectIsHoveredAsState()
                                Row(
                                    modifier = Modifier
                                        .hoverable(interactionSource = expandInteractionSource)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (isExpandHovered) colorScheme.primary.copy(alpha = 0.16f)
                                            else colorScheme.primary.copy(alpha = 0.10f),
                                            RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            width = adaptiveScale.dp(1.dp, min = 1.dp, max = 1.4.dp),
                                            color = if (isExpandHovered) colorScheme.primary.copy(alpha = 0.30f)
                                            else colorScheme.outlineVariant.copy(alpha = 0.32f),
                                            shape = RoundedCornerShape(6.dp)
                                        )
                                        .clickable(
                                            interactionSource = expandInteractionSource,
                                            indication = null,
                                            onClick = onAttributesExpandClick
                                        )
                                        .pointerHoverIcon(PointerIcon.Hand)
                                        .padding(horizontal = 4.dp, vertical = 1.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isExpandHovered) colorScheme.primary else colorScheme.primary.copy(alpha = 0.95f),
                                        maxLines = 1
                                    )
                                    if (attributesExpanded) {
                                        Icon(
                                            imageVector = Icons.Outlined.ExpandLess,
                                            contentDescription = null,
                                            tint = colorScheme.primary.copy(alpha = 0.95f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }

                    if (attributesExpanded && extraAttributes.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(0.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            extraAttributes.forEachIndexed { index, (attr, count) ->
                                if (index != 0) {
                                    Text(
                                        text = ", ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                                    )
                                }
                                AttributeHoverText(matcher = attr, count = count)
                            }
                        }
                    }
                }
            }
        }
        Box(
            modifier = Modifier.width(scanTokens.chevronColumnWidth),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onRescanClick != null || onEditAndRunClick != null) {
                    Box {
                        IconButton(
                            onClick = { actionsExpanded = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MoreVert,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = actionsExpanded,
                            onDismissRequest = { actionsExpanded = false }
                        ) {
                            onRescanClick?.let { rescan ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.ScanResult_RescanAsIs)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.RestartAlt,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        actionsExpanded = false
                                        rescan()
                                    }
                                )
                            }
                            onEditAndRunClick?.let { editAndRun ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.ScanResult_EditAndRun)) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = null
                                        )
                                    },
                                    onClick = {
                                        actionsExpanded = false
                                        editAndRun()
                                    }
                                )
                            }
                        }
                    }
                }
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun taskStatusLabel(state: TaskState): String = when (state) {
    TaskState.SCANNING, TaskState.SEARCHING -> stringResource(Res.string.TaskStateChipFilter_Active)
    TaskState.STOPPED, TaskState.PENDING -> stringResource(Res.string.TaskStateChipFilter_Paused)
    TaskState.FAILED -> stringResource(Res.string.TaskStateChipFilter_Error)
    TaskState.COMPLETED -> stringResource(Res.string.TaskStateChipFilter_Completed)
    TaskState.LOADING -> "—"
}

@Composable
fun ScanTaskAttributesSubRow(
    taskEntity: TaskEntityViewModel,
    modifier: Modifier = Modifier,
    onCollapseClick: () -> Unit
) {
    val foundAttributes by taskEntity.foundAttributes.collectAsState()
    val sortedAttributes = remember(foundAttributes) {
        foundAttributes.toList().sortedByDescending { it.second }
    }
    val colorScheme = MaterialTheme.colorScheme

    if (sortedAttributes.isEmpty()) return

    val scanTokens = LocalMainScreenAdaptiveTokens.current.scanList
    val adaptiveScale = LocalMainScreenAdaptiveTokens.current.scale
    val rowShape = RoundedCornerShape(scanTokens.rowCorner)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(colorScheme.surfaceVariant.copy(alpha = 0.10f), rowShape)
            .border(adaptiveScale.dp(1.dp, min = 1.dp, max = 1.4.dp), colorScheme.outlineVariant.copy(alpha = 0.22f), rowShape)
            .padding(horizontal = scanTokens.rowHorizontalPadding, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Attributes",
                style = MaterialTheme.typography.labelMedium,
                fontSize = 10.sp,
                color = colorScheme.onSurfaceVariant
            )
            val collapseInteractionSource = remember { MutableInteractionSource() }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .hoverable(interactionSource = collapseInteractionSource)
                    .clickable(
                        interactionSource = collapseInteractionSource,
                        indication = null,
                        onClick = onCollapseClick
                    )
                    .pointerHoverIcon(PointerIcon.Hand)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Collapse",
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 10.sp,
                    color = colorScheme.primary.copy(alpha = 0.92f)
                )
                Icon(
                    imageVector = Icons.Outlined.ExpandLess,
                    contentDescription = null,
                    tint = colorScheme.primary.copy(alpha = 0.92f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            sortedAttributes.forEachIndexed { index, (attr, count) ->
                DescriptionTooltip(description = stringResource(Res.string.ScansPage_TooltipFound, count)) {
                    Text(
                        text = attr.composableName(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    )
                }
                if (index != sortedAttributes.lastIndex) {
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.outline.copy(alpha = 0.65f),
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalTime::class)
private fun formatFinishedLabel(
    finishedAt: LocalDateTime?,
    currentTime: Instant
): String {
    if (finishedAt == null) return "-"
    val zone = TimeZone.currentSystemDefault()
    val currentDate = currentTime.toLocalDateTime(zone).date
    val yesterdayDate = (currentTime - 24.toDuration(DurationUnit.HOURS)).toLocalDateTime(zone).date
    val finishedDate = finishedAt.date
    return when {
        finishedDate == currentDate -> stringResource(Res.string.ScansPage_Finished_Today)
        finishedDate == yesterdayDate -> stringResource(Res.string.ScansPage_Finished_Yesterday)
        else -> formatDateShort(finishedAt)
    }
}

private fun formatDateShort(value: LocalDateTime): String {
    val dd = value.day.toString().padStart(2, '0')
    val mmDate = value.month.ordinal.plus(1).toString().padStart(2, '0')
    val yy = (value.year % 100).toString().padStart(2, '0')
    return "$dd.$mmDate.$yy"
}

private fun formatShortDuration(duration: Duration): String {
    val hours = duration.inWholeHours
    if (hours > 0) return "${hours}h"
    val minutes = duration.inWholeMinutes
    if (minutes > 0) return "${minutes}m"
    val seconds = duration.inWholeSeconds
    return "${seconds}s"
}

private fun compactSize(value: String?): String {
    if (value.isNullOrBlank()) return "-"
    return value
        .replace(" ", "")
        .replace("MB", "Mb")
        .replace("KB", "Kb")
        .replace("GB", "Gb")
        .replace("TB", "Tb")
}

@Composable
private fun AttributeHoverText(
    matcher: IMatcher,
    count: Int
) {
    val colorScheme = MaterialTheme.colorScheme
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()

    DescriptionTooltip(description = stringResource(Res.string.ScansPage_TooltipFound, count)) {
        Text(
            text = matcher.composableName(),
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
