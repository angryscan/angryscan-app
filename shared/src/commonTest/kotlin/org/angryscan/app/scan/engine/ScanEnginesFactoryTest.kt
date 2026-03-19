package org.angryscan.app.scan.engine

import org.angryscan.app.common.MatchersRegister
import org.angryscan.common.engine.IScanEngine
import org.angryscan.common.engine.kotlin.KotlinEngine
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ScanEnginesFactoryTest {
    @Test
    fun `build includes every matcher from register using fallbacks`() {
        val engines: List<IScanEngine> = ScanEnginesFactory.build(
            preferredEngine = KotlinEngine::class,
            matchers = MatchersRegister.toList(),
            requireKeywords = false
        )

        try {
            val actualMatcherClasses = engines
                .flatMap { it.matchers }
                .map { it::class }
                .toSet()
            val expectedMatcherClasses = MatchersRegister
                .map { it::class }
                .toSet()

            assertEquals(expectedMatcherClasses, actualMatcherClasses)
        } finally {
            engines.forEach { it.close() }
        }
    }
}
