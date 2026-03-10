package org.angryscan.app.ui.windows.screens.settings.items

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.angryscan.app.common.AppSettings
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.SettingsScreen_Language
import org.angryscan.app.resources.SettingsScreen_LanguageDescription
import org.angryscan.app.ui.windows.screens.settings.SettingsRow
import org.angryscan.app.ui.windows.screens.settings.components.SettingsSelector
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import java.util.*

@Composable
fun LanguageSettings(modifier: Modifier = Modifier) {
    val appSettings = koinInject<AppSettings>()
    var language by remember { appSettings.language }

    LaunchedEffect(language) {
        Locale.setDefault(Locale.forLanguageTag(language.locale))
    }

    key(language) {
        SettingsRow(title = stringResource(Res.string.SettingsScreen_Language), modifier = modifier) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = stringResource(Res.string.SettingsScreen_LanguageDescription),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val minCellWidth = 180.dp
                val columns = (maxWidth / minCellWidth).toInt().coerceAtLeast(1)
                val rows = (AppSettings.LanguageType.entries.size + columns - 1) / columns
                val height = (34 * rows + (8 * (rows - 1).coerceAtLeast(0))).dp

                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .height(height)
                        .fillMaxWidth()
                ) {
                items(AppSettings.LanguageType.entries) { lang ->
                    SettingsSelector(
                        selected = lang == language,
                        onClick = {
                            language = lang
                            appSettings.save()
                            Locale.setDefault(Locale.forLanguageTag(lang.locale))
                        },
                        text = lang.text
                    )
                }
            }
            }
            }
        }
    }
}

