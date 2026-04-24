package org.angryscan.app.scan.engine

import io.github.oshai.kotlinlogging.KotlinLogging
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import org.angryscan.common.engine.custom.CustomEngine
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.engine.kotlin.KotlinEngine
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

private val logger = KotlinLogging.logger {}

private const val MAX_ENGINE_TYPES = 3

fun KClass<out IScanEngine>.getEngine(matchers: List<IMatcher>): IScanEngine {
    return getEngine(matchers, requireKeywords = true)
}

fun KClass<out IScanEngine>.getEngine(matchers: List<IMatcher>, requireKeywords: Boolean): IScanEngine {
    return when (this) {
        HyperScanEngine::class -> {
            val hyperMatchers = matchers.toHyperScanMatchers()
            CompiledHyperScanStore.getOrCompile(hyperMatchers, requireKeywords)
        }
        KotlinEngine::class -> KotlinEngine(matchers.toKotlinMatchers(), requireKeywords = requireKeywords)
        CustomEngine::class -> CustomEngine(matchers.toCustomMatchers())
        else -> throw IllegalArgumentException("Unknown engine")
    }
}

fun KClass<out IScanEngine>.fallback(): KClass<out IScanEngine> {
    return when (this) {
        HyperScanEngine::class -> KotlinEngine::class
        KotlinEngine::class -> CustomEngine::class
        CustomEngine::class -> HyperScanEngine::class
        else -> throw IllegalArgumentException("Unknown engine")
    }
}

fun IScanEngine.fallback(): KClass<out IScanEngine> {
    return this::class.fallback()
}

fun IScanEngine.inappropriateMatchers(matchers: List<IMatcher>): List<IMatcher> {
    return when (this) {
        is HyperScanEngine -> matchers.notHyperScanMatchers()
        is KotlinEngine -> matchers.notKotlinMatchers()
        is CustomEngine -> matchers.notCustomMatchers()
        else -> throw IllegalArgumentException("Unknown engine")
    }
}

/**
 * Builds a complete engine chain (primary + fallback engines) for the given configuration.
 * The fallback loop is bounded by the number of available engine types to prevent infinite cycles.
 */
fun buildEngineChain(
    primaryEngineClass: KClass<out IScanEngine>,
    matchers: List<IMatcher>,
    requireKeywords: Boolean
): List<IScanEngine> {
    val engines = mutableListOf<IScanEngine>()

    val primary = primaryEngineClass.getEngine(matchers, requireKeywords)
    if (primary.matchers.isNotEmpty()) {
        engines.add(primary)
    }

    val remaining = if (engines.isNotEmpty())
        engines[0].inappropriateMatchers(matchers).toMutableList()
    else
        matchers.toMutableList()

    if (remaining.isNotEmpty()) {
        var fallbackClass = primaryEngineClass.fallback()
        var iterations = 0
        while (remaining.isNotEmpty() && iterations < MAX_ENGINE_TYPES) {
            val prevSize = remaining.size
            val engine = fallbackClass.getEngine(remaining, requireKeywords)
            if (engine.matchers.isNotEmpty()) {
                engines.add(engine)
                remaining.removeAll(engine.matchers.toSet())
            }
            if (remaining.size == prevSize) {
                logger.warn {
                    "Fallback iteration $iterations made no progress. " +
                            "Remaining matchers: ${remaining.map { it.name }}"
                }
            }
            fallbackClass = fallbackClass.fallback()
            iterations++
        }
        if (remaining.isNotEmpty()) {
            logger.warn {
                "Exhausted all engine types. ${remaining.size} matchers unhandled: " +
                        remaining.map { it.name }
            }
        }
    }

    return engines
}

/**
 * Thread-safe store for serialized compiled HyperScan databases.
 * Allows fast re-creation of HyperScan engines via [HyperScanEngine.fromCompiledDatabase]
 * across threads or after cache eviction, avoiding costly pattern recompilation.
 */
object CompiledHyperScanStore {
    private val databases = ConcurrentHashMap<String, ByteArray>()

    fun getOrCompile(
        matchers: List<org.angryscan.common.engine.hyperscan.IHyperMatcher>,
        requireKeywords: Boolean
    ): HyperScanEngine {
        if (matchers.isEmpty()) return HyperScanEngine(matchers, requireKeywords = requireKeywords)

        val storeKey = storeKey(matchers, requireKeywords)
        val compiled = databases[storeKey]

        return if (compiled != null) {
            try {
                HyperScanEngine.fromCompiledDatabase(matchers, compiled, requireKeywords)
            } catch (e: Exception) {
                logger.warn(e) { "Failed to load compiled HyperScan DB, recompiling" }
                compileAndStore(matchers, requireKeywords, storeKey)
            }
        } else {
            compileAndStore(matchers, requireKeywords, storeKey)
        }
    }

    fun clear() {
        databases.clear()
        logger.debug { "Compiled HyperScan database store cleared" }
    }

    private fun compileAndStore(
        matchers: List<org.angryscan.common.engine.hyperscan.IHyperMatcher>,
        requireKeywords: Boolean,
        storeKey: String
    ): HyperScanEngine {
        val engine = HyperScanEngine(matchers, requireKeywords = requireKeywords)
        databases[storeKey] = engine.saveCompiledDatabase()
        logger.debug { "Compiled HyperScan DB saved to store (key=$storeKey)" }
        return engine
    }

    private fun storeKey(
        matchers: List<org.angryscan.common.engine.hyperscan.IHyperMatcher>,
        requireKeywords: Boolean
    ): String {
        return matchers
            .map { "${it::class.qualifiedName}:${it.name}" }
            .sorted()
            .joinToString(",") + ":rk=$requireKeywords"
    }
}
