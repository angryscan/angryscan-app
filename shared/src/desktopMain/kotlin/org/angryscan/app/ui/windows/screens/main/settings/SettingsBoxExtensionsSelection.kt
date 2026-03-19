package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.ScanSettings_FileExtensions
import org.angryscan.app.scan.common.files.types.CertFileType
import org.angryscan.app.scan.common.files.types.CodeFileType
import org.angryscan.app.scan.common.files.types.IFileType
import org.angryscan.app.ui.windows.screens.main.settings.items.SelectAllOrDiscardAllText
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsBoxExtensionsSelection(scanSettings: ScanSettings) {
    val fileTypeEntriesOrdered = remember {
        IFileType.getAll()
            .filter { it !in CodeFileType.entries && it !in CertFileType.entries }
            .sortedBy { if (it.name == "Text") 1 else 0 }
    }
    val allSelected = scanSettings.extensions.containsAll(fileTypeEntriesOrdered)

    SettingsSectionCard(
        title = stringResource(Res.string.ScanSettings_FileExtensions),
        titleTrailing = {
            SelectAllOrDiscardAllText(
                allSelected = allSelected,
                onClick = {
                    if (allSelected) scanSettings.extensions.clear()
                    else scanSettings.extensions.addAll(fileTypeEntriesOrdered.filter { it !in scanSettings.extensions })
                    scanSettings.save()
                }
            )
        }
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (fileType in fileTypeEntriesOrdered) {
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
