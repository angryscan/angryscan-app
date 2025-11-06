package org.angryscan.app.scan.engine

import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import org.angryscan.common.engine.custom.CustomEngine
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.engine.kotlin.KotlinEngine
import kotlin.reflect.KClass

fun KClass<out IScanEngine>.getEngine(matchers: List<IMatcher>): IScanEngine {
    return when(this) {
        HyperScanEngine::class -> HyperScanEngine(matchers.toHyperScanMatchers())
        KotlinEngine::class -> KotlinEngine(matchers.toKotlinMatchers())
        CustomEngine::class -> CustomEngine(matchers.toCustomMatchers())
        else -> throw IllegalArgumentException("Unknown engine")
    }
}

fun IScanEngine.fallback() : KClass<out IScanEngine> {
    return when(this) {
        is HyperScanEngine -> KotlinEngine::class
        is KotlinEngine -> CustomEngine::class
        is CustomEngine -> HyperScanEngine::class
        else -> throw IllegalArgumentException("Unknown engine")
    }
}

fun IScanEngine.inappropriateMatchers(matchers: List<IMatcher>): List<IMatcher> {
    return when(this) {
        is HyperScanEngine -> matchers.notHyperScanMatchers()
        is KotlinEngine -> matchers.notKotlinMatchers()
        is CustomEngine -> matchers.notCustomMatchers()
        else -> throw IllegalArgumentException("Unknown engine")
    }
}