package org.angryscan.app.ui

import org.angryscan.app.common.OS

fun shouldHideOnMinimizeToTray(hideOnMinimize: Boolean, os: OS): Boolean {
    return hideOnMinimize && os != OS.MAC && os != OS.LINUX
}
