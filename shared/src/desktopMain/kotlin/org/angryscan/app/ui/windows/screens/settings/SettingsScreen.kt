package org.angryscan.app.ui.windows.screens.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.angryscan.app.common.AppSettings
import org.angryscan.app.ui.windows.screens.settings.items.*
import org.koin.compose.koinInject

private val spacingUnit = 8.dp

@Composable
fun SettingsScreen() {
    val appSettings = koinInject<AppSettings>()
    val language by remember { appSettings.language }
    val scrollState = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme
    val containerShape = RoundedCornerShape(24.dp)
    var scrollbarTarget by remember { mutableFloatStateOf(0.4f) }
    LaunchedEffect(scrollState.value) {
        scrollbarTarget = 1f
        delay(800)
        scrollbarTarget = 0.4f
    }
    val scrollbarAlpha by animateFloatAsState(
        targetValue = scrollbarTarget,
        animationSpec = tween(400),
        label = "scrollbar_alpha"
    )

    key(language) {
        AnimatedContent(
            targetState = language,
            transitionSpec = { fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(180)) },
            label = "settings_content"
        ) { _ ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .widthIn(max = 720.dp)
                        .clip(containerShape)
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.22f), containerShape)
                        .border(
                            width = 1.dp,
                            color = colorScheme.outlineVariant.copy(alpha = 0.25f),
                            shape = containerShape
                        )
                        .padding(24.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(spacingUnit * 2),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(end = spacingUnit * 4)
                                .verticalScroll(scrollState)
                        ) {
                            AboutSettings()
                            ThreadCountSettings()
                            ContextMenuSettings()
                            LanguageSettings()
                            ThemeSettings()
                            EngineSettings()
                            LoggingSettings()
                        }
                        VerticalScrollbar(
                            adapter = rememberScrollbarAdapter(scrollState),
                            modifier = Modifier
                                .alpha(scrollbarAlpha)
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(start = spacingUnit * 2, end = spacingUnit)
                                .width(10.dp),
                            style = LocalScrollbarStyle.current.copy(
                                hoverColor = colorScheme.primary,
                                unhoverColor = colorScheme.secondary
                            )
                        )
                    }
                }
            }
        }
    }
}