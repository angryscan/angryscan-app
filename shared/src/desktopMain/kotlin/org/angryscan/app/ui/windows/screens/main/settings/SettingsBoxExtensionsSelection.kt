package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import org.angryscan.app.ui.windows.screens.main.settings.items.SelectAllOrDiscardAllText
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
            // NOTE: "txt" is an extension inside TextType (name = "Text"), so we include TextType in Documents.
            FileTypeGroup(Res.string.ScanSettings_FileExtensions_Group_Documents, setOf("DOCX", "DOC", "ODT", "PDF", "Text")),
            FileTypeGroup(Res.string.ScanSettings_FileExtensions_Group_Spreadsheets, setOf("XLSX", "XLS", "ODS")),
            FileTypeGroup(Res.string.ScanSettings_FileExtensions_Group_Presentations, setOf("PPTX", "PPT", "ODP")),
            FileTypeGroup(Res.string.ScanSettings_FileExtensions_Group_Archives, setOf("ZIP", "RAR"))
        )
    }

    SettingsSectionCard(
        title = stringResource(Res.string.ScanSettings_FileExtensions),
        titleTrailing = {
            SelectAllOrDiscardAllText(
                allSelected = allSelected,
                onClick = {
                    if (allSelected) scanSettings.extensions.clear()
                    else scanSettings.extensions.addAll(fileTypeEntries.filter { it !in scanSettings.extensions })
                    scanSettings.save()
                }
            )
        }
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            groups.forEach { group ->
                val groupTypes = fileTypeEntries.filter { it.name in group.typeNames }
                if (groupTypes.isEmpty()) return@forEach
                val groupAllSelected = groupTypes.all { scanSettings.extensions.contains(it) }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 0.dp, bottom = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(group.titleRes),
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SelectAllOrDiscardAllText(
                        allSelected = groupAllSelected,
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
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    for (fileType in groupTypes) {
                        val selected = scanSettings.extensions.contains(fileType)
                        FilterChip(
                            modifier = Modifier.height(28.dp),
                            selected = selected,
                            onClick = {
                                if (selected) scanSettings.extensions.remove(fileType)
                                else scanSettings.extensions.add(fileType)
                                scanSettings.save()
                            },
                            label = { Text(text = fileType.name, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }
    }
}
