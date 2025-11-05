package org.angryscan.app.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.painterResource
import org.angryscan.app.common.AppSettings
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.Theme_Dark
import org.angryscan.app.resources.Theme_Light
import org.angryscan.app.resources.Theme_System

@Composable
fun AppSettings.ThemeType.icon(): Painter = when (this) {
    AppSettings.ThemeType.Dark -> painterResource(Res.drawable.Theme_Dark)
    AppSettings.ThemeType.Light -> painterResource(Res.drawable.Theme_Light)
    AppSettings.ThemeType.System -> painterResource(Res.drawable.Theme_System)
}