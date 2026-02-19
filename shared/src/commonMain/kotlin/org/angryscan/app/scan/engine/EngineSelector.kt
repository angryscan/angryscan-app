package org.angryscan.app.scan.engine

import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import org.angryscan.common.engine.custom.CustomEngine
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.engine.kotlin.KotlinEngine
import kotlin.reflect.KClass

fun KClass<out IScanEngine>.getEngine(matchers: List<IMatcher>): IScanEngine {
    return getEngine(matchers, requireKeywords = true)
}

fun KClass<out IScanEngine>.getEngine(matchers: List<IMatcher>, requireKeywords: Boolean): IScanEngine {
    return when(this) {
        HyperScanEngine::class -> {
            val hyperMatchers = matchers.toHyperScanMatchers()
            if (hyperMatchers.isEmpty()) {
                // HyperScan does not accept zero patterns; use fallback engine (e.g. Gitleaks is not IHyperMatcher)
                fallback().getEngine(matchers, requireKeywords)
            } else {
                HyperScanEngine(hyperMatchers, requireKeywords = requireKeywords)
            }
        }
        KotlinEngine::class -> KotlinEngine(matchers.toKotlinMatchers(), requireKeywords = requireKeywords)
        CustomEngine::class -> CustomEngine(matchers.toCustomMatchers())
        else -> throw IllegalArgumentException("Unknown engine")
    }
}

fun KClass<out IScanEngine>.fallback(): KClass<out IScanEngine> {
    return when(this) {
        HyperScanEngine::class -> KotlinEngine::class
        KotlinEngine::class -> CustomEngine::class
        CustomEngine::class -> HyperScanEngine::class
        else -> throw IllegalArgumentException("Unknown engine")
    }
}

fun IScanEngine.fallback() : KClass<out IScanEngine> {
    return this::class.fallback()
}

fun IScanEngine.inappropriateMatchers(matchers: List<IMatcher>): List<IMatcher> {
    return when(this) {
        is HyperScanEngine -> matchers.notHyperScanMatchers()
        is KotlinEngine -> matchers.notKotlinMatchers()
        is CustomEngine -> matchers.notCustomMatchers()
        else -> throw IllegalArgumentException("Unknown engine")
    }
}