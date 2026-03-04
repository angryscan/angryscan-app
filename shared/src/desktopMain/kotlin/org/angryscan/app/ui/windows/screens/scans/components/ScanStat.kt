package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

private val statChipShape = RoundedCornerShape(8.dp)

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
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ScanStatChip(
            icon = Icons.Outlined.Folder,
            title = stringResource(Res.string.Task_TotalFiles),
            value = if (totalFiles > 0 && folderSize.isNotEmpty()) "$totalFiles ($folderSize)" else totalFiles.toString(),
            onClick = null
        )
        ScanStatChip(
            icon = Icons.Outlined.PlaylistAddCheck,
            title = stringResource(Res.string.Task_SelectedFiles),
            value = if (selectedFiles > 0 && selectedFilesSize > 0) "$selectedFiles (${selectedFilesSize.toHumanReadable()})" else selectedFiles.toString(),
            onClick = onClick
        )
        ScanStatChip(
            icon = Icons.Outlined.Search,
            title = stringResource(Res.string.Task_FoundFiles),
            value = if (foundFiles > 0 && foundFilesSize > 0) "$foundFiles (${foundFilesSize.toHumanReadable()})" else foundFiles.toString(),
            onClick = onClick
        )
        ScanStatChip(
            icon = Icons.Outlined.Schedule,
            title = stringResource(Res.string.Task_ScanTime),
            value = scanTime,
            onClick = null
        )
        ScanStatChip(
            icon = Icons.Outlined.Star,
            title = stringResource(Res.string.Result_ColumnScore),
            value = scoreSum.toString(),
            onClick = null
        )
    }
}

@Composable
private fun ScanStatChip(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val baseModifier = if (onClick != null) {
        modifier.clickable(onClick = onClick).pointerHoverIcon(PointerIcon.Hand)
    } else modifier

    Row(
        modifier = baseModifier
            .clip(statChipShape)
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

@Composable
private fun StatChip(
    text: String,
    onClick: (() -> Unit)?,
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
        style = MaterialTheme.typography.bodyMedium,
        color = colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = base
            .clip(shape)
            .background(
                if (isClickable && isHovered) colorScheme.surfaceVariant.copy(alpha = 0.6f)
                else colorScheme.surfaceVariant.copy(alpha = 0.35f)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
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
    onClick: (() -> Unit)? = null
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StatChip(
            "${stringResource(Res.string.Task_TotalFiles)}: ${if (totalFiles > 0 && folderSize.isNotEmpty()) "$totalFiles ($folderSize)" else totalFiles}",
            onClick
        )
        StatChip(
            "${stringResource(Res.string.Task_SelectedFiles)}: ${if (selectedFiles > 0 && selectedFilesSize > 0) "$selectedFiles (${selectedFilesSize.toHumanReadable()})" else selectedFiles}",
            onClick
        )
        StatChip(
            "${stringResource(Res.string.Task_FoundFiles)}: ${if (foundFiles > 0 && foundFilesSize > 0) "$foundFiles (${foundFilesSize.toHumanReadable()})" else foundFiles}",
            onClick
        )
        StatChip("${stringResource(Res.string.Task_ScanTime)}: $scanTime", onClick)
        StatChip("${stringResource(Res.string.Result_ColumnScore)}: $scoreSum", onClick)
    }
}
