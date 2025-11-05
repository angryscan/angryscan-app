package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.angryscan.app.resources.*
import org.angryscan.app.ui.extensions.toHumanReadable

@Composable
fun ScanStat(
    totalFiles: Long,
    selectedFiles: Long,
    foundFiles: Long,
    folderSize: String,
    selectedFilesSize: Long,
    foundFilesSize: Long,
    scanTime: String,
    scoreSum: Long
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
        color = MaterialTheme.colorScheme.primary
    )

    //Selected files count
    ScanStatItem(
        title = stringResource(Res.string.Task_SelectedFiles),
        text = if (selectedFiles > 0 && selectedFilesSize > 0) {
            "$selectedFiles (${selectedFilesSize.toHumanReadable()})"
        } else {
            selectedFiles.toString()
        }
    )

    VerticalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.primary
    )

    //Found files count
    ScanStatItem(
        title = stringResource(Res.string.Task_FoundFiles),
        text = if (foundFiles > 0 && foundFilesSize > 0) {
            "$foundFiles (${foundFilesSize.toHumanReadable()})"
        } else {
            foundFiles.toString()
        }
    )

    VerticalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.primary
    )

    //Scan time
    ScanStatItem(
        title = stringResource(Res.string.Task_ScanTime),
        text = scanTime
    )

    VerticalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.primary
    )

    //Score sum of all found files
    ScanStatItem(
        title = stringResource(Res.string.Result_ColumnScore),
        text = scoreSum.toString()
    )
}
