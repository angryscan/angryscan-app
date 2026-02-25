package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import org.angryscan.app.resources.*
import org.angryscan.app.ui.extensions.toHumanReadable
import org.jetbrains.compose.resources.stringResource

@Composable
fun ScanStat(
    totalFiles: Long,
    selectedFiles: Long,
    foundFiles: Long,
    folderSize: String,
    selectedFilesSize: Long,
    foundFilesSize: Long,
    scanTime: String,
    scoreSum: Long,
    onClick: (() -> Unit)? = null
) {
    // Total files count
    ScanStatItem(
        title = stringResource(Res.string.Task_TotalFiles),
        text = if (totalFiles > 0 && folderSize.isNotEmpty()) {
            "$totalFiles (${folderSize})"
        } else {
            totalFiles.toString()
        }
    )

    VerticalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )

    //Selected files count
    ScanStatItem(
        title = stringResource(Res.string.Task_SelectedFiles),
        text = if (selectedFiles > 0 && selectedFilesSize > 0) {
            "$selectedFiles (${selectedFilesSize.toHumanReadable()})"
        } else {
            selectedFiles.toString()
        },
        onClick = onClick
    )

    VerticalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )

    //Found files count
    ScanStatItem(
        title = stringResource(Res.string.Task_FoundFiles),
        text = if (foundFiles > 0 && foundFilesSize > 0) {
            "$foundFiles (${foundFilesSize.toHumanReadable()})"
        } else {
            foundFiles.toString()
        },
        onClick = onClick
    )

    VerticalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )

    //Scan time
    ScanStatItem(
        title = stringResource(Res.string.Task_ScanTime),
        text = scanTime
    )

    VerticalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )

    //Score sum of all found files
    ScanStatItem(
        title = stringResource(Res.string.Result_ColumnScore),
        text = scoreSum.toString()
    )
}

@Composable
fun ScanStatInline(
    totalFiles: Long,
    selectedFiles: Long,
    foundFiles: Long,
    folderSize: String,
    selectedFilesSize: Long,
    foundFilesSize: Long,
    scanTime: String,
    scoreSum: Long,
    onClick: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val modifier = if (onClick != null) Modifier.clickable(onClick = onClick).pointerHoverIcon(PointerIcon.Hand) else Modifier
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("${stringResource(Res.string.Task_TotalFiles)} ${if (totalFiles > 0 && folderSize.isNotEmpty()) "$totalFiles ($folderSize)" else totalFiles}", style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
        Text(" · ", style = MaterialTheme.typography.bodyMedium, color = colorScheme.outline)
        Text("${stringResource(Res.string.Task_SelectedFiles)} ${if (selectedFiles > 0 && selectedFilesSize > 0) "$selectedFiles (${selectedFilesSize.toHumanReadable()})" else selectedFiles}", style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
        Text(" · ", style = MaterialTheme.typography.bodyMedium, color = colorScheme.outline)
        Text("${stringResource(Res.string.Task_FoundFiles)} ${if (foundFiles > 0 && foundFilesSize > 0) "$foundFiles (${foundFilesSize.toHumanReadable()})" else foundFiles}", style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
        Text(" · ", style = MaterialTheme.typography.bodyMedium, color = colorScheme.outline)
        Text("${stringResource(Res.string.Task_ScanTime)} $scanTime", style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
        Text(" · ", style = MaterialTheme.typography.bodyMedium, color = colorScheme.outline)
        Text("${stringResource(Res.string.Result_ColumnScore)} $scoreSum", style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
    }
}
