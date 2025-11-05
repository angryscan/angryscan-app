package org.angryscan.app.ui.strings

import androidx.compose.runtime.Composable
import org.angryscan.common.engine.IScanEngine
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.engine.kotlin.KotlinEngine
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.ScanEngine_HyperScan
import org.angryscan.app.resources.ScanEngine_Kotlin
import kotlin.reflect.KClass

@Composable
fun KClass<out IScanEngine>.composableName(): String {
    return when (this) {
        HyperScanEngine::class -> stringResource(Res.string.ScanEngine_HyperScan)
        KotlinEngine::class -> stringResource(Res.string.ScanEngine_Kotlin)
        else -> throw IllegalArgumentException("Unknown scan engine: ${this::class}")
    }
}

suspend fun KClass<out IScanEngine>.readableName(): String {
    return when (this) {
        HyperScanEngine::class -> getString(Res.string.ScanEngine_HyperScan)
        KotlinEngine::class -> getString(Res.string.ScanEngine_Kotlin)
        else -> throw IllegalArgumentException("Unknown scan engine: ${this::class}")
    }
}