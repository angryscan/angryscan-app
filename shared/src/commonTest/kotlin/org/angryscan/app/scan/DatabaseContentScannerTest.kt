package org.angryscan.app.scan

import org.angryscan.app.scan.engine.toKotlinMatchers
import org.angryscan.common.engine.kotlin.KotlinEngine
import org.angryscan.common.matchers.Email
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    @Test
    fun `scanContentStructured attributes matches to correct column`() {
        val engine = KotlinEngine(listOf(Email).toKotlinMatchers())

        val rows = listOf(
            mapOf("email" to "user@example.com", "name" to "John"),
            mapOf("email" to "other@test.com", "name" to "Jane")
        )

        val doc = DatabaseContentScanner.scanContentStructured(
            path = "public.users",
            rows = rows,
            engines = listOf(engine)
        )

        assertEquals(1, doc.columnFields.size)
        assertEquals(2, doc.columnFields["email"]?.get(Email))
        assertNull(doc.columnFields["name"])
        assertEquals(2, doc.getDocumentFields()[Email])

        engine.close()
    }

    @Test
    fun `scanContentStructured empty rows returns empty columnFields`() {
        val engine = KotlinEngine(listOf(Email).toKotlinMatchers())

        val doc = DatabaseContentScanner.scanContentStructured(
            path = "public.users",
            rows = emptyList(),
            engines = listOf(engine)
        )

        assertEquals(0, doc.columnFields.size)
        assertEquals(0L, doc.size)
        assertNull(doc.getDocumentFields()[Email])

        engine.close()
    }
}
