package org.angryscan.app.ui.windows.screens.settings

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.koinInject
import org.angryscan.app.common.AppSettings
import org.angryscan.app.ui.windows.screens.settings.items.AboutSettings
import org.angryscan.app.ui.windows.screens.settings.items.ContextMenuSettings
import org.angryscan.app.ui.windows.screens.settings.items.EngineSettings
import org.angryscan.app.ui.windows.screens.settings.items.LanguageSettings
import org.angryscan.app.ui.windows.screens.settings.items.LoggingSettings
import org.angryscan.app.ui.windows.screens.settings.items.ThemeSettings
import org.angryscan.app.ui.windows.screens.settings.items.ThreadCountSettings

@Composable
fun SettingsScreen() {
    val appSettings = koinInject<AppSettings>()
    val language by remember { appSettings.language }
    val scrollState = rememberScrollState()

    key(language) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .width(800.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .padding(6.dp)
                            .padding(end = 20.dp)
                            .verticalScroll(scrollState)
                    ) {
                        ThreadCountSettings()
                        ContextMenuSettings()
                        LanguageSettings()
                        ThemeSettings()
                        LoggingSettings()
                        AboutSettings()
                        EngineSettings()
                    }
                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(scrollState),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .padding(end = 6.dp)
                            .width(10.dp),
                        style = LocalScrollbarStyle.current.copy(
                            hoverColor = MaterialTheme.colorScheme.primary,
                            unhoverColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }
            }
        }
    }
}