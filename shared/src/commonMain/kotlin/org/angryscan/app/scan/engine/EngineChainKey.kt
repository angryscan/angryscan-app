package org.angryscan.app.scan.engine

import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import kotlin.reflect.KClass

/**
 * Cache key that uniquely identifies an engine chain configuration.
 * Two keys are equal when they produce the same set of scan engines.
 */
data class EngineChainKey(
    val primaryEngineClass: KClass<out IScanEngine>,
    val matcherSignature: List<String>,
    val requireKeywords: Boolean
) {
    companion object {
        fun of(
            primaryEngineClass: KClass<out IScanEngine>,
            matchers: List<IMatcher>,
            requireKeywords: Boolean
        ): EngineChainKey = EngineChainKey(
            primaryEngineClass = primaryEngineClass,
            matcherSignature = matchers
                .map { "${it::class.qualifiedName}:${it.name}" }
                .sorted(),
            requireKeywords = requireKeywords
        )
    }
}
