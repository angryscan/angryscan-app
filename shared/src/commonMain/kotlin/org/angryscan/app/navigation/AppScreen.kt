package org.angryscan.app.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class AppScreen {
    @Serializable data object Main
    @Serializable data object Scans
    @Serializable data class ScanResult(val scanId: Int)
    @Serializable data object Settings
}