package org.angryscan.app.scan

import org.angryscan.app.scan.engine.toKotlinMatchers
import org.angryscan.common.engine.kotlin.KotlinEngine
import org.angryscan.common.matchers.Email
import kotlin.test.Test
import kotlin.test.assertEquals

internal class DatabaseContentScannerTest {
    @Test
    fun `scan content detects matches without files`() {
        val engine = KotlinEngine(listOf(Email).toKotlinMatchers())

        val document = DatabaseContentScanner.scanContent(
            path = "public.users",
            content = """{"email":"user@example.com"}""",
            engines = listOf(engine)
        )

        assertEquals(1, document.getDocumentFields()[Email])

        engine.close()
    }
}
