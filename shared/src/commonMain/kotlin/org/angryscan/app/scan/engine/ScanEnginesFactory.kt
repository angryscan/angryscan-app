package org.angryscan.app.scan.engine

import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import kotlin.reflect.KClass

object ScanEnginesFactory {
    fun build(
        preferredEngine: KClass<out IScanEngine>,
        matchers: List<IMatcher>,
        requireKeywords: Boolean
    ): List<IScanEngine> {
        val engines = mutableListOf<IScanEngine>()

        preferredEngine
            .getEngine(matchers, requireKeywords = requireKeywords)
            .let { engine ->
                if (engine.matchers.isNotEmpty()) {
                    engines.add(engine)
                }
            }

        val remainingMatchers = if (engines.isNotEmpty()) {
            engines.first().inappropriateMatchers(matchers).toMutableList()
        } else {
            matchers.toMutableList()
        }

        var fallbackEngine = preferredEngine.fallback()
        while (remainingMatchers.isNotEmpty() && fallbackEngine != preferredEngine) {
            val engine = fallbackEngine.getEngine(remainingMatchers, requireKeywords = requireKeywords)
            if (engine.matchers.isNotEmpty()) {
                engines.add(engine)
                remainingMatchers.removeAll(engine.matchers)
            }
            fallbackEngine = fallbackEngine.fallback()
        }

        return engines
    }
}
