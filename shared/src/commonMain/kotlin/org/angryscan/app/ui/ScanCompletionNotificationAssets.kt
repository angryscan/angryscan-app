package org.angryscan.app.ui

data class ScanCompletionNotificationAssets(
    val appIconResourcePath: String,
    val completionMiniIconResourcePath: String
)

fun scanCompletionNotificationAssets(): ScanCompletionNotificationAssets {
    return ScanCompletionNotificationAssets(
        appIconResourcePath = "drawable/icon.png",
        completionMiniIconResourcePath = "drawable/scan_completed_done_all.svg"
    )
}
