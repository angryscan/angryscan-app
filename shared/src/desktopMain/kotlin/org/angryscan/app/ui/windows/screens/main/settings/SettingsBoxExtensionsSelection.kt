package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.ScanSettings_FileExtensions
import org.angryscan.app.scan.common.files.types.CertFileType
import org.angryscan.app.scan.common.files.types.CodeFileType
import org.angryscan.app.scan.common.files.types.IFileType
import org.angryscan.app.ui.windows.components.DescriptionTooltip
import org.angryscan.app.ui.windows.screens.main.settings.items.SettingsSelectAllTextButton
import org.jetbrains.compose.resources.stringResource

private data class ExtensionsGroup(
    val title: String,
    val typeNames: Set<String>,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsBoxExtensionsSelection(
    scanSettings: ScanSettings,
    showTitle: Boolean = true,
    unifiedBlock: Boolean = false,
    modifier: Modifier = Modifier
) {
    val fileTypeEntries = remember {
        IFileType.getAll().filter { it !in CodeFileType.entries && it !in CertFileType.entries }
    }
    val allSelected = scanSettings.extensions.containsAll(fileTypeEntries)

    val dense = unifiedBlock
    val groupLabelWidth = if (dense) 108.dp else SettingsScanTable.groupLabelWidth
    val groupRowHGap = if (dense) 5.dp else 10.dp
    val chipCorner = if (dense) 4.dp else 6.dp
    val chipPadH = if (dense) 1.dp else 2.dp
    val chipLabelFs = if (dense) 8.sp else 10.sp
    val groupLabelFs = if (dense) 8.5.sp else 10.sp
    val flowHGap = if (dense) 7.dp else 9.dp
    val flowVGap = if (dense) 5.dp else 6.dp
    // Сейчас общий левый отступ секции даёт 4.dp (внутренний padding карточки).
    // Чтобы визуально получить "в 3 раза больше" слева в первой колонке, добавляем ещё +8.dp.
    val firstColumnExtraStartPadding = 8.dp

    val content: @Composable ColumnScope.() -> Unit = {
        Column(modifier = Modifier.wrapContentWidth()) {
            val groups = remember {
                listOf(
                    ExtensionsGroup(title = "word", typeNames = setOf("DOCX", "DOC", "ODT")),
                    ExtensionsGroup(title = "text", typeNames = setOf("Text")),
                    ExtensionsGroup(title = "presentations", typeNames = setOf("PPTX", "PPT", "ODP")),
                    ExtensionsGroup(title = "tables", typeNames = setOf("XLSX", "XLS", "ODS")),
                    ExtensionsGroup(title = "archives", typeNames = setOf("ZIP", "RAR")),
                )
            }

            val visibleGroups = remember(fileTypeEntries) {
                groups.mapNotNull { group ->
                    val groupTypes = fileTypeEntries.filter { it.name in group.typeNames }
                    if (groupTypes.isEmpty()) null else group to groupTypes
                }
            }

            visibleGroups.forEachIndexed { idx, (group, groupTypes) ->
                SettingsTableRowWithContentWidthDivider(
                    dense = dense,
                    showDividerBelow = idx != visibleGroups.lastIndex,
                ) {
                    Row(
                        modifier = Modifier
                            .wrapContentWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(groupRowHGap),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = group.title,
                            modifier = Modifier
                                .width(groupLabelWidth)
                                .padding(start = firstColumnExtraStartPadding),
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = groupLabelFs,
                            lineHeight = if (dense) 10.sp else TextUnit.Unspecified,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        SettingsTableColumnDivider()
                        FlowRow(
                            modifier = Modifier,
                            horizontalArrangement = Arrangement.spacedBy(flowHGap),
                            verticalArrangement = Arrangement.spacedBy(flowVGap)
                        ) {
                            for (fileType in groupTypes) {
                                val selected = scanSettings.extensions.contains(fileType)
                                DescriptionTooltip(description = fileType.name) {
                                    SettingsFlowToggleChip(
                                        selected = selected,
                                        onClick = {
                                            if (selected) scanSettings.extensions.remove(fileType)
                                            else scanSettings.extensions.add(fileType)
                                            scanSettings.save()
                                        },
                                        label = fileType.name,
                                        chipCorner = chipCorner,
                                        chipPadH = chipPadH,
                                        chipLabelFs = chipLabelFs
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showTitle) {
        if (unifiedBlock) {
            SettingsUnifiedSubsection(
                title = stringResource(Res.string.ScanSettings_FileExtensions),
                modifier = modifier,
                contentTopPadding = (SettingsScanTable.contentBelowHeaderSpacing * 3) / 2f,
                titleTrailingInline = true,
                titleTrailingInlineSpacing = SettingsScanTable.headerInlineActionSpacing,
                titleTrailing = {
                    SettingsSelectAllTextButton(
                        allSelected = allSelected,
                        onClick = {
                            if (allSelected) scanSettings.extensions.clear()
                            else scanSettings.extensions.addAll(fileTypeEntries.filter { it !in scanSettings.extensions })
                            scanSettings.save()
                        }
                    )
                },
                content = content
            )
        } else {
            SettingsSectionCard(
                title = stringResource(Res.string.ScanSettings_FileExtensions),
                modifier = modifier,
                contentTopPadding = (SettingsScanTable.contentBelowHeaderSpacing * 3) / 2f,
                titleTrailingInline = true,
                titleTrailingInlineSpacing = SettingsScanTable.headerInlineActionSpacing,
                titleTrailing = {
                    SettingsSelectAllTextButton(
                        allSelected = allSelected,
                        onClick = {
                            if (allSelected) scanSettings.extensions.clear()
                            else scanSettings.extensions.addAll(fileTypeEntries.filter { it !in scanSettings.extensions })
                            scanSettings.save()
                        }
                    )
                },
                content = content
            )
        }
    } else {
        Column(modifier = modifier, content = content)
    }
}
