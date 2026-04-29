package org.angryscan.app.scan.engine

import io.github.oshai.kotlinlogging.KotlinLogging
import org.angryscan.common.engine.IScanEngine

private val logger = KotlinLogging.logger {}

/**
 * Per-thread LRU cache for scan engine chains with idle TTL eviction.
 * NOT thread-safe — designed to be owned by a single [org.angryscan.app.scan.ScanThread].
 */
class EngineChainCache(
    private val maxIdleMs: Long = 5 * 60 * 1000L,
    private val maxEntries: Int = 4
) {
    private class CacheEntry(
        val engines: List<IScanEngine>,
        var lastAccessedMs: Long = System.currentTimeMillis()
    )

    // access-order LinkedHashMap gives LRU semantics: eldest entry is evicted first
    private val cache = object : LinkedHashMap<EngineChainKey, CacheEntry>(maxEntries + 1, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<EngineChainKey, CacheEntry>?): Boolean = false
    }

    val size: Int get() = cache.size

    fun getOrCreate(key: EngineChainKey, factory: () -> List<IScanEngine>): List<IScanEngine> {
        val existing = cache[key]
        if (existing != null) {
            existing.lastAccessedMs = System.currentTimeMillis()
            return existing.engines
        }

        evictLRU()

        val engines = factory()
        cache[key] = CacheEntry(engines)
        logger.debug { "Engine chain cached: $key (cache size: ${cache.size})" }
        return engines
    }

    /** Remove entries that have not been accessed for [maxIdleMs]. */
    fun evictStale() {
        val now = System.currentTimeMillis()
        val iter = cache.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            if (now - entry.value.lastAccessedMs >= maxIdleMs) {
                closeEngines(entry.value.engines)
                iter.remove()
                logger.debug { "Evicted stale engine chain: ${entry.key}" }
            }
        }
    }

    /** Close and remove all cached engine chains. */
    fun closeAll() {
        cache.values.forEach { closeEngines(it.engines) }
        cache.clear()
        logger.debug { "All engine chains closed" }
    }

    private fun evictLRU() {
        while (cache.size >= maxEntries) {
            val eldest = cache.entries.firstOrNull() ?: break
            closeEngines(eldest.value.engines)
            cache.remove(eldest.key)
            logger.debug { "LRU evicted engine chain: ${eldest.key}" }
        }
    }

    private fun closeEngines(engines: List<IScanEngine>) {
        engines.forEach { engine ->
            try {
                engine.close()
            } catch (e: Exception) {
                logger.warn(e) { "Error closing scan engine: ${engine::class.simpleName}" }
            }
        }
    }
}
