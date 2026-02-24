package org.angryscan.app.ui

import org.angryscan.app.common.OS

fun shouldUseNativeScanCompletionNotification(
    os: OS,
    javaHome: String?,
    jpackageAppPath: String?
): Boolean {
    if (os != OS.MAC) return true

    // On macOS, native notifications may crash when app is started
    // from a plain JDK location instead of an app bundle runtime.
    if (!jpackageAppPath.isNullOrBlank()) return true

    val normalizedJavaHome = javaHome.orEmpty().replace('\\', '/')
    return normalizedJavaHome.contains(".app/Contents/")
}
