package org.angryscan.app.scan.engine

import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import org.angryscan.common.engine.Match
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EngineChainCacheTest {

    private class MockMatcher(override val name: String) : IMatcher {
        override fun check(value: String) = true
    }

    private class MockEngine(
        override val matchers: List<IMatcher> = emptyList(),
        val id: String = ""
    ) : IScanEngine {
        var closed = false
        override fun scan(text: String): List<Match> = emptyList()
        override fun close() { closed = true }
    }

    private val engineClass: KClass<out IScanEngine> = MockEngine::class

    @Test
    fun `reuses cached engines for same key`() {
        val cache = EngineChainCache(maxIdleMs = 60_000, maxEntries = 4)
        val matchers = listOf(MockMatcher("A"))
        val key = EngineChainKey.of(engineClass, matchers, requireKeywords = true)

        var createCount = 0
        val engines1 = cache.getOrCreate(key) {
            createCount++
            listOf(MockEngine(matchers, "e1"))
        }
        val engines2 = cache.getOrCreate(key) {
            createCount++
            listOf(MockEngine(matchers, "e2"))
        }

        assertEquals(1, createCount, "Factory should be called only once")
        assertSame(engines1, engines2, "Same list instance should be returned on cache hit")
    }

    @Test
    fun `creates new engines for different matcher sets`() {
        val cache = EngineChainCache(maxIdleMs = 60_000, maxEntries = 4)
        val key1 = EngineChainKey.of(engineClass, listOf(MockMatcher("A")), true)
        val key2 = EngineChainKey.of(engineClass, listOf(MockMatcher("B")), true)

        var createCount = 0
        val engines1 = cache.getOrCreate(key1) { createCount++; listOf(MockEngine()) }
        val engines2 = cache.getOrCreate(key2) { createCount++; listOf(MockEngine()) }

        assertEquals(2, createCount)
        assertNotSame(engines1, engines2)
        assertEquals(2, cache.size)
    }

    @Test
    fun `different requireKeywords creates separate entries`() {
        val cache = EngineChainCache(maxIdleMs = 60_000, maxEntries = 4)
        val matchers = listOf(MockMatcher("A"))
        val keyTrue = EngineChainKey.of(engineClass, matchers, requireKeywords = true)
        val keyFalse = EngineChainKey.of(engineClass, matchers, requireKeywords = false)

        var createCount = 0
        cache.getOrCreate(keyTrue) { createCount++; listOf(MockEngine()) }
        cache.getOrCreate(keyFalse) { createCount++; listOf(MockEngine()) }

        assertEquals(2, createCount)
        assertEquals(2, cache.size)
    }

    @Test
    fun `LRU eviction closes oldest engine when maxEntries exceeded`() {
        val cache = EngineChainCache(maxIdleMs = 60_000, maxEntries = 2)

        val engine1 = MockEngine(id = "oldest")
        val engine2 = MockEngine(id = "middle")
        val engine3 = MockEngine(id = "newest")

        cache.getOrCreate(EngineChainKey.of(engineClass, listOf(MockMatcher("A")), true)) { listOf(engine1) }
        cache.getOrCreate(EngineChainKey.of(engineClass, listOf(MockMatcher("B")), true)) { listOf(engine2) }
        cache.getOrCreate(EngineChainKey.of(engineClass, listOf(MockMatcher("C")), true)) { listOf(engine3) }

        assertTrue(engine1.closed, "Oldest engine must be closed on LRU eviction")
        assertEquals(2, cache.size)
    }

    @Test
    fun `evictStale closes engines past idle TTL`() {
        val cache = EngineChainCache(maxIdleMs = 0, maxEntries = 4)
        val engine = MockEngine()
        cache.getOrCreate(EngineChainKey.of(engineClass, listOf(MockMatcher("A")), true)) { listOf(engine) }

        cache.evictStale()

        assertTrue(engine.closed, "Stale engine must be closed")
        assertEquals(0, cache.size)
    }

    @Test
    fun `evictStale keeps fresh engines`() {
        val cache = EngineChainCache(maxIdleMs = 60_000, maxEntries = 4)
        val engine = MockEngine()
        cache.getOrCreate(EngineChainKey.of(engineClass, listOf(MockMatcher("A")), true)) { listOf(engine) }

        cache.evictStale()

        assertTrue(!engine.closed, "Fresh engine must not be closed")
        assertEquals(1, cache.size)
    }

    @Test
    fun `closeAll closes every cached engine and clears cache`() {
        val cache = EngineChainCache(maxIdleMs = 60_000, maxEntries = 10)
        val engines = (1..5).map { i ->
            MockEngine(id = "e$i").also { e ->
                cache.getOrCreate(EngineChainKey.of(engineClass, listOf(MockMatcher("M$i")), true)) { listOf(e) }
            }
        }

        cache.closeAll()

        engines.forEach { assertTrue(it.closed, "Engine ${it.id} must be closed") }
        assertEquals(0, cache.size)
    }

    @Test
    fun `key equality is independent of matcher list order`() {
        val m1 = MockMatcher("A")
        val m2 = MockMatcher("B")

        val key1 = EngineChainKey.of(engineClass, listOf(m1, m2), true)
        val key2 = EngineChainKey.of(engineClass, listOf(m2, m1), true)

        assertEquals(key1, key2)
        assertEquals(key1.hashCode(), key2.hashCode())
    }

    @Test
    fun `multiple engines in chain are all closed on eviction`() {
        val cache = EngineChainCache(maxIdleMs = 60_000, maxEntries = 1)
        val chain1 = listOf(MockEngine(id = "primary"), MockEngine(id = "fallback"))
        val chain2 = listOf(MockEngine(id = "new"))

        cache.getOrCreate(EngineChainKey.of(engineClass, listOf(MockMatcher("A")), true)) { chain1 }
        cache.getOrCreate(EngineChainKey.of(engineClass, listOf(MockMatcher("B")), true)) { chain2 }

        assertTrue(chain1[0].closed, "Primary engine of evicted chain must be closed")
        assertTrue(chain1[1].closed, "Fallback engine of evicted chain must be closed")
    }

    @Test
    fun `LRU promotes recently accessed entry`() {
        val cache = EngineChainCache(maxIdleMs = 60_000, maxEntries = 2)

        val engine1 = MockEngine(id = "e1")
        val engine2 = MockEngine(id = "e2")
        val engine3 = MockEngine(id = "e3")

        val key1 = EngineChainKey.of(engineClass, listOf(MockMatcher("A")), true)
        val key2 = EngineChainKey.of(engineClass, listOf(MockMatcher("B")), true)
        val key3 = EngineChainKey.of(engineClass, listOf(MockMatcher("C")), true)

        cache.getOrCreate(key1) { listOf(engine1) }
        cache.getOrCreate(key2) { listOf(engine2) }

        // Access key1 to promote it (making key2 the LRU candidate)
        cache.getOrCreate(key1) { throw IllegalStateException("Should not be called") }

        // Adding key3 should evict key2 (least recently used), not key1
        cache.getOrCreate(key3) { listOf(engine3) }

        assertTrue(!engine1.closed, "Recently accessed engine must survive eviction")
        assertTrue(engine2.closed, "Least recently used engine must be evicted")
    }
}
