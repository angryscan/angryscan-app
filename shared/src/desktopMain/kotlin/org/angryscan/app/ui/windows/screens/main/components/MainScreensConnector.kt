package org.angryscan.app.ui.windows.screens.main.components

import kotlinx.serialization.Serializable

@Serializable
sealed class MainScreenConnector{
    @Serializable object FileShare
    @Serializable object S3
    @Serializable object HTTP
}