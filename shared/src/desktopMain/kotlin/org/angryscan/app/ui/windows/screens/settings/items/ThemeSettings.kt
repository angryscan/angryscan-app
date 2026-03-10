package org.angryscan.app.ui.windows.screens.settings.items

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.angryscan.app.common.AppSettings
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.SettingsScreen_Theme
import org.angryscan.app.resources.SettingsScreen_ThemeDescription
import org.angryscan.app.ui.icons.icon
import org.angryscan.app.ui.strings.composableName
import org.angryscan.app.ui.windows.screens.settings.SettingsRow
import org.angryscan.app.ui.windows.screens.settings.components.SettingsSelector
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun ThemeSettings(modifier: Modifier = Modifier) {
    val appSettings = koinInject<AppSettings>()
    var theme by remember { appSettings.theme }

    SettingsRow(title = stringResource(Res.string.SettingsScreen_Theme), modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
                Text(
                    text = stringResource(Res.string.SettingsScreen_ThemeDescription),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val minCellWidth = 180.dp
            val columns = (maxWidth / minCellWidth).toInt().coerceAtLeast(1)
            val rows = (AppSettings.ThemeType.entries.size + columns - 1) / columns
            val height = (34 * rows + (8 * (rows - 1).coerceAtLeast(0))).dp

            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .height(height)
                    .fillMaxWidth()
            ) {
            items(AppSettings.ThemeType.entries) { th ->
                SettingsSelector(
                    text = th.composableName(),
                    icon = th.icon(),
                    selected = th == theme,
                    onClick = {
                        theme = th
                        appSettings.save()
                    }
                )
//                Box(
//                    modifier = Modifier
//                        .size(width = 150.dp, height = 34.dp)
//                        .clip(
//                            MaterialTheme.shapes.large
//                        )
//                        .border(
//                            width = 1.dp,
//                            color = MaterialTheme.colorScheme.primary,
//                            shape = MaterialTheme.shapes.large
//                        )
//                        .background(
//                            color = if (th == theme) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
//                        )
//                        .clickable(
//                            enabled = th != theme,
//                            onClick = {
//                                theme = th
//                                appSettings.save()
//                            }
//                        )
//                        .padding(horizontal = 10.dp),
//                    contentAlignment = Alignment.Center
//                ) {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically,
//                        horizontalArrangement = Arrangement.SpaceEvenly,
//                        modifier = Modifier
//                            .fillMaxSize()
//                    ) {
//                        Text(
//                            text = th.composableName(),
//                            fontSize = 14.sp,
//                            lineHeight = 14.sp
//                        )
//                        Icon(
//                            painter = th.icon(),
//                            contentDescription = null
//                        )
//                    }
//                }
            }
        }
        }
        }
    }
}

