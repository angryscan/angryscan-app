package org.angryscan.app.ui.windows.screens.settings.items

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.angryscan.app.common.AppSettings
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.SettingsScreen_Language
import org.angryscan.app.ui.windows.screens.settings.SettingsRow
import org.angryscan.app.ui.windows.screens.settings.components.SettingsSelector
import java.util.*

@Composable
fun LanguageSettings() {
    val appSettings = koinInject<AppSettings>()
    var language by remember { appSettings.language }

    LaunchedEffect(language) {
        Locale.setDefault(Locale.forLanguageTag(language.locale))
    }

    key(language) {
        SettingsRow(title = stringResource(Res.string.SettingsScreen_Language)) {
            val rows =
                AppSettings.LanguageType.entries.size / 3 + if (AppSettings.LanguageType.entries.size % 3 > 0) 1 else 0

            val height = (34 * rows + (6 * (rows - 1))).dp

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
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

