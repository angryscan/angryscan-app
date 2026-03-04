package org.angryscan.app.ui.windows.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import org.angryscan.app.common.AppSettings
import org.angryscan.app.store.ContextMenu
import org.angryscan.app.ui.windows.screens.settings.items.*
import org.koin.compose.koinInject

private val rowSpacing = 12.dp
private val sectionSpacing = 12.dp
private val minRowHeight = 160.dp

@Composable
fun SettingsScreen() {
    val appSettings = koinInject<AppSettings>()
    val language by remember { appSettings.language }
    val colorScheme = MaterialTheme.colorScheme
    val containerShape = RoundedCornerShape(20.dp)

    key(language) {
        AnimatedContent(
            targetState = language,
            transitionSpec = { fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180)) },
            label = "settings_content"
        ) { _ ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(containerShape)
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.22f), containerShape)
                        .border(
                            width = 1.dp,
                            color = colorScheme.outlineVariant.copy(alpha = 0.25f),
                            shape = containerShape
                        )
                        .padding(20.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(sectionSpacing),
                        modifier = Modifier.fillMaxSize()
                    ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .heightIn(min = minRowHeight),
                                horizontalArrangement = Arrangement.spacedBy(rowSpacing),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    AboutSettings(Modifier.fillMaxHeight())
                                }
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    ThreadCountSettings(Modifier.fillMaxHeight())
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1.2f)
                                    .heightIn(min = minRowHeight),
                                horizontalArrangement = Arrangement.spacedBy(rowSpacing),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    LanguageSettings(Modifier.fillMaxHeight())
                                }
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    ThemeSettings(Modifier.fillMaxHeight())
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .heightIn(min = minRowHeight),
                                horizontalArrangement = Arrangement.spacedBy(rowSpacing),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    EngineSettings(Modifier.fillMaxHeight())
                                }
                                Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    LoggingSettings(Modifier.fillMaxHeight())
                                }
                            }
                            if (ContextMenu.supported()) {
                                Box(modifier = Modifier.fillMaxWidth()) { ContextMenuSettings() }
                            }
                        }
                    }
                }
            }
        }
    }