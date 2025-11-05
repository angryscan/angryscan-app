package org.angryscan.app.ui.windows.screens.settings.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.angryscan.app.common.AppSettings
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.SettingsScreen_Theme
import org.angryscan.app.ui.icons.icon
import org.angryscan.app.ui.strings.composableName
import org.angryscan.app.ui.windows.screens.settings.SettingsRow
import org.angryscan.app.ui.windows.screens.settings.components.SettingsSelector

@Composable
fun ThemeSettings() {
    val appSettings = koinInject<AppSettings>()
    var theme by remember { appSettings.theme }

    SettingsRow(title = stringResource(Res.string.SettingsScreen_Theme)) {
        val rows =
            AppSettings.ThemeType.entries.size / 3 + if (AppSettings.ThemeType.entries.size % 3 > 0) 1 else 0

        val height = (34 * rows + (6 * (rows - 1))).dp

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
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

