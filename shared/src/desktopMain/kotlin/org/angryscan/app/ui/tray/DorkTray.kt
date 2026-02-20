package org.angryscan.app.ui.tray

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.ApplicationScope
import dorkbox.systemTray.MenuItem
import dorkbox.systemTray.SystemTray
import org.angryscan.app.resources.*
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource

@Composable
fun ApplicationScope.DorkTray(
    mainIsVisible: Boolean,
    mainVisibilitySet: (Boolean) -> Unit,
) {
    SystemTray.AUTO_FIX_INCONSISTENCIES = false

    val tray by remember { mutableStateOf(SystemTray.get() ?: throw Exception("Unable to load SystemTray!")) }

    // Более высокая плотность — иконка рендерится крупнее (трей сам масштабирует под слот)
    val trayImage = painterResource(Res.drawable.favicon_light_tab)
        .toAwtImage(Density(5f), LayoutDirection.Ltr)

    LaunchedEffect(true) {
        tray.setImage(
            trayImage
        )
        tray.setTooltip(getString(Res.string.appName))

        tray.menu.add(
            MenuItem(getString(Res.string.trayOpen))
        )
        tray.menu.add(
            MenuItem(getString(Res.string.trayExit)).apply {
                setCallback {
                    exitApplication()
                }
            }
        )
    }
    LaunchedEffect(mainIsVisible) {
        val menuItem = tray.menu.entries.firstOrNull {
            if (it is MenuItem) {
                it.text == getString(Res.string.trayOpen) || it.text == getString(Res.string.trayHide)
            } else false
        }
        if (menuItem is MenuItem) {
            menuItem.text =
                if (mainIsVisible) getString(Res.string.trayHide) else getString(Res.string.trayOpen)

            menuItem.setCallback {
                mainVisibilitySet(!mainIsVisible)
            }
        }
    }
}