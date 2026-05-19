package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistAddCheck
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.angryscan.app.resources.*
import org.angryscan.app.ui.extensions.toHumanReadable
import org.jetbrains.compose.resources.stringResource

@Composable
private fun StatChip(
    text: String,
    onClick: (() -> Unit)?,
    compact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isClickable = onClick != null
    val base = if (isClickable) {
        modifier
            .hoverable(interactionSource = interactionSource)
            .clickable(onClick = onClick!!)
            .pointerHoverIcon(PointerIcon.Hand)
    } else modifier
    Text(
        text = text,
        style = if (compact) MaterialTheme.typography.labelSmall else MaterialTheme.typography.bodySmall,
        color = colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = base
            .clip(shape)
            .background(
                if (isClickable && isHovered) colorScheme.surfaceVariant.copy(alpha = 0.6f)
                else colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
            .padding(
                horizontal = if (compact) 6.dp else 10.dp,
                vertical = if (compact) 4.dp else 6.dp
            )
    )
}

@OptIn(ExperimentalLayoutApi::class)
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
    onClick: (() -> Unit)? = null,
    compact: Boolean = false
) {
    val spacing = if (compact) 4.dp else 6.dp
    if (compact) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatChip(
                "${stringResource(Res.string.Task_TotalFiles)}: ${if (totalFiles > 0 && folderSize.isNotEmpty()) "$totalFiles ($folderSize)" else totalFiles}",
                onClick,
                compact
            )
            StatChip(
                "${stringResource(Res.string.Task_SelectedFiles)}: ${if (selectedFiles > 0 && selectedFilesSize > 0) "$selectedFiles (${selectedFilesSize.toHumanReadable()})" else selectedFiles}",
                onClick,
                compact
            )
            StatChip(
                "${stringResource(Res.string.Task_FoundFiles)}: ${if (foundFiles > 0 && foundFilesSize > 0) "$foundFiles (${foundFilesSize.toHumanReadable()})" else foundFiles}",
                onClick,
                compact
            )
            StatChip("${stringResource(Res.string.Task_ScanTime)}: $scanTime", onClick, compact)
            StatChip("${stringResource(Res.string.Result_ColumnScore)}: $scoreSum", onClick, compact)
        }
    } else {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            StatChip(
                "${stringResource(Res.string.Task_TotalFiles)}: ${if (totalFiles > 0 && folderSize.isNotEmpty()) "$totalFiles ($folderSize)" else totalFiles}",
                onClick,
                compact
            )
            StatChip(
                "${stringResource(Res.string.Task_SelectedFiles)}: ${if (selectedFiles > 0 && selectedFilesSize > 0) "$selectedFiles (${selectedFilesSize.toHumanReadable()})" else selectedFiles}",
                onClick,
                compact
            )
            StatChip(
                "${stringResource(Res.string.Task_FoundFiles)}: ${if (foundFiles > 0 && foundFilesSize > 0) "$foundFiles (${foundFilesSize.toHumanReadable()})" else foundFiles}",
                onClick,
                compact
            )
            StatChip("${stringResource(Res.string.Task_ScanTime)}: $scanTime", onClick, compact)
            StatChip("${stringResource(Res.string.Result_ColumnScore)}: $scoreSum", onClick, compact)
        }
    }
}
