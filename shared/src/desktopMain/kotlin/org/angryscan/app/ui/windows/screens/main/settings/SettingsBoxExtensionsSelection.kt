package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.ScanSettings_FileExtensions
import org.angryscan.app.resources.ScanSettings_SelectAll
import org.angryscan.app.scan.common.files.FileType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBoxExtensionsSelection(scanSettings: ScanSettings) {
    var expanded by remember { scanSettings.extensionsSettingsExpanded }


    SettingsBoxSpan(
        text = stringResource(Res.string.ScanSettings_FileExtensions),
        expanded = expanded,
        onExpandClick = {
            expanded = !expanded
        }
    ) {
        val fileTypeEntries = FileType.entries.filter { it != FileType.CODE && it != FileType.CERT }
        val rows = fileTypeEntries.size / 5 + if (fileTypeEntries.size % 5 > 0) 1 else 0

        val height = (24 * rows + (6 * (rows - 1))).dp + 52.dp

        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            verticalArrangement = Arrangement.spacedBy(5.dp),
            modifier = Modifier
                .height(height)
                .fillMaxWidth()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(42.dp)
                ) {
                    Checkbox(
                        checked = scanSettings.extensions.containsAll(fileTypeEntries),
                        onCheckedChange = { checked ->
                            if (checked) {
                                scanSettings.extensions.addAll(fileTypeEntries.filter {
                                    !scanSettings.extensions.contains(
                                        it
                                    )
                                })
                            } else {
                                scanSettings.extensions.clear()
                            }
                            scanSettings.save()
                        }
                    )
                    CompositionLocalProvider(LocalRippleConfiguration provides null) {
                        Text(
                            text = stringResource(Res.string.ScanSettings_SelectAll),
                            fontSize = 14.sp,
                            modifier = Modifier.clickable {
                                if (!scanSettings.extensions.containsAll(fileTypeEntries))
                                    scanSettings.extensions.addAll(fileTypeEntries.filter {
                                        !scanSettings.extensions.contains(
                                            it
                                        )
                                    })
                                else
                                    scanSettings.extensions.clear()
                                scanSettings.save()
                            }
                        )
                    }
                }
            }
            items(fileTypeEntries) { extension ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .height(24.dp)
                ) {
                    Checkbox(
                        checked = scanSettings.extensions.contains(extension),
                        onCheckedChange = { checked ->
                            if (checked && !scanSettings.extensions.contains(extension))
                                scanSettings.extensions.add(extension)
                            else if (!checked)
                                scanSettings.extensions.remove(extension)
                            scanSettings.save()
                        }
                    )
                    CompositionLocalProvider(LocalRippleConfiguration provides null) {
                        Text(
                            text = extension.name,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable {
                                if (scanSettings.extensions.contains(extension))
                                    scanSettings.extensions.remove(extension)
                                else
                                    scanSettings.extensions.add(extension)
                                scanSettings.save()
                            }
                        )
                    }
                }
            }
        }
    }
}