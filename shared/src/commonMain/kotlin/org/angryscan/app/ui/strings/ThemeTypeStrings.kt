package org.angryscan.app.ui.strings

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.angryscan.app.common.AppSettings
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.ThemeType_Dark
import org.angryscan.app.resources.ThemeType_Light
import org.angryscan.app.resources.ThemeType_System


/**
 * Returns a human-readable name for the theme type.
 *
 * @return A string name for the theme type.
 */
@Suppress("Unused")
suspend fun AppSettings.ThemeType.readableName(): String = when (this) {
    AppSettings.ThemeType.Dark -> getString(Res.string.ThemeType_Dark)
    AppSettings.ThemeType.Light -> getString(Res.string.ThemeType_Light)
    AppSettings.ThemeType.System -> getString(Res.string.ThemeType_System)
}

/**
 * A composable version of [readableName].
 *
 * @return A string name for the theme type.
 */
@Composable
fun AppSettings.ThemeType.composableName(): String = when (this) {
    AppSettings.ThemeType.Dark -> stringResource(Res.string.ThemeType_Dark)
    AppSettings.ThemeType.Light -> stringResource(Res.string.ThemeType_Light)
    AppSettings.ThemeType.System -> stringResource(Res.string.ThemeType_System)
}