package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.*
import org.angryscan.app.scan.common.files.types.CertFileType
import org.angryscan.app.scan.common.files.types.CodeFileType
import org.angryscan.app.scan.common.files.types.IFileType
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private data class FileTypeGroup(
    val titleRes: StringResource,
    val typeNames: Set<String>
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsBoxExtensionsSelection(scanSettings: ScanSettings) {
    val fileTypeEntries = remember {
        IFileType.getAll().filter { it !in CodeFileType.entries && it !in CertFileType.entries }
    }
    val allSelected = scanSettings.extensions.containsAll(fileTypeEntries)

    val groups = remember {
        listOf(
            FileTypeGroup(Res.string.ScanSettings_FileExtensions_Group_Documents, setOf("DOCX", "DOC", "ODT", "PDF")),
            FileTypeGroup(Res.string.ScanSettings_FileExtensions_Group_Spreadsheets, setOf("XLSX", "XLS", "ODS")),
            FileTypeGroup(Res.string.ScanSettings_FileExtensions_Group_Presentations, setOf("PPTX", "PPT", "ODP")),
            FileTypeGroup(Res.string.ScanSettings_FileExtensions_Group_Text, setOf("Text")),
            FileTypeGroup(Res.string.ScanSettings_FileExtensions_Group_Archives, setOf("ZIP", "RAR"))
        )
    }

    SettingsSectionCard(
        title = stringResource(Res.string.ScanSettings_FileExtensions),
        titleTrailing = {
            val interactionSource = remember { MutableInteractionSource() }
            val isHovered by interactionSource.collectIsHoveredAsState()
            Text(
                text = if (allSelected) stringResource(Res.string.ScanSettings_DeselectAll) else stringResource(Res.string.ScanSettings_SelectAll),
                style = MaterialTheme.typography.labelLarge,
                fontSize = 13.sp,
                color = if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            if (allSelected) scanSettings.extensions.clear()
                            else scanSettings.extensions.addAll(fileTypeEntries.filter { it !in scanSettings.extensions })
                            scanSettings.save()
                        }
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }
    ) {
        groups.forEach { group ->
            val groupTypes = fileTypeEntries.filter { it.name in group.typeNames }
            if (groupTypes.isEmpty()) return@forEach
            val groupAllSelected = groupTypes.all { scanSettings.extensions.contains(it) }
            val groupInteractionSource = remember { MutableInteractionSource() }
            val groupHovered by groupInteractionSource.collectIsHoveredAsState()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(group.titleRes),
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (groupAllSelected) stringResource(Res.string.ScanSettings_DeselectAll) else stringResource(Res.string.ScanSettings_SelectAll),
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 13.sp,
                    color = if (groupHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .clickable(
                            interactionSource = groupInteractionSource,
                            indication = null,
                            onClick = {
                                if (groupAllSelected) {
                                    groupTypes.forEach { scanSettings.extensions.remove(it) }
                                } else {
                                    groupTypes.forEach { ft ->
                                        if (ft !in scanSettings.extensions) scanSettings.extensions.add(ft)
                                    }
                                }
                                scanSettings.save()
                            }
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (fileType in groupTypes) {
                    val selected = scanSettings.extensions.contains(fileType)
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) scanSettings.extensions.remove(fileType)
                            else scanSettings.extensions.add(fileType)
                            scanSettings.save()
                        },
                        label = { Text(text = fileType.name, fontSize = 13.sp) }
                    )
                }
            }
        }
    }
}
