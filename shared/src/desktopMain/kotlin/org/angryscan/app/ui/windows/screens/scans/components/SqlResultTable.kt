package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.*
import org.angryscan.app.scan.SqlColumnResultCard
import org.angryscan.app.scan.SqlTableResultCard
import org.angryscan.app.scan.TaskEntityViewModel
import org.angryscan.app.scan.TaskFilesViewModel
import org.angryscan.app.scan.groupSqlResultsByTable
import org.angryscan.common.engine.IMatcher
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SqlResultTable(
    taskFilesViewModel: TaskFilesViewModel,
    task: TaskEntityViewModel,
    selectedAttributes: List<IMatcher>,
    scanSettings: ScanSettings,
    modifier: Modifier = Modifier
) {
    val taskFiles by taskFilesViewModel.taskFiles.collectAsState()
    val visibleRows = remember(taskFiles, selectedAttributes) {
        taskFiles.filter { row ->
            row.foundAttributes.keys.any { attr -> attr in selectedAttributes }
        }
    }
    val cards = remember(visibleRows) {
        groupSqlResultsByTable(visibleRows)
    }

    val colorScheme = MaterialTheme.colorScheme
    val containerShape = RoundedCornerShape(24.dp)

    Scaffold(
        containerColor = Color.Transparent,
        modifier = modifier
            .fillMaxSize()
            .clip(containerShape)
            .background(colorScheme.surfaceVariant.copy(alpha = 0.22f), containerShape)
            .border(
                width = 1.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.25f),
                shape = containerShape
            )
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(cards, key = { it.tablePath }) { card ->
                SqlTableResultCardItem(card = card)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SqlTableResultCardItem(card: SqlTableResultCard) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SqlHeaderLabelValue(
                    label = stringResource(Res.string.Result_CardSchema),
                    value = card.schemaName ?: "-",
                    modifier = Modifier.weight(1f)
                )
                SqlHeaderLabelValue(
                    label = stringResource(Res.string.Result_CardTable),
                    value = card.tableName,
                    modifier = Modifier.weight(1f)
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.widthIn(max = 360.dp)
                ) {
                    SqlSummaryChip(
                        label = stringResource(Res.string.Result_ColumnCount),
                        value = card.count.toString()
                    )
                    SqlSummaryChip(
                        label = stringResource(Res.string.Result_ColumnScore),
                        value = card.score.toString()
                    )
                    SqlSummaryChip(
                        label = stringResource(Res.string.Result_ColumnSize),
                        value = card.size.toString()
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                card.columns.forEach { column ->
                    SqlColumnResultItem(column = column)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SqlColumnResultItem(column: SqlColumnResultCard) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = column.name,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(min = 120.dp, max = 220.dp)
            )

            FlowRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                column.foundAttributes
                    .toList()
                    .sortedByDescending { it.second }
                    .forEach { (matcher, count) ->
                        AttributeChip(
                            attribute = matcher,
                            count = count
                        )
                    }
            }

            Text(
                text = "${stringResource(Res.string.Result_ColumnCount)}: ${column.count}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SqlHeaderLabelValue(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun SqlSummaryChip(
    label: String,
    value: String
) {
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = "$label: $value",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
