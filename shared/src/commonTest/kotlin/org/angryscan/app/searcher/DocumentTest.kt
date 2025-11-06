package org.angryscan.app.searcher

import org.angryscan.common.matchers.Email
import org.angryscan.common.matchers.FullName
import org.angryscan.app.scan.common.Document
import kotlin.test.Test
import kotlin.test.assertEquals
import org.angryscan.common.engine.IMatcher

internal class DocumentTest {

    @Test
    fun updateDocument() {
        val document = Document(1, "123")
        document.updateDocument(FullName, 0)
        document.updateDocument(Email, 1)
        assertEquals(1, document.length())
        assertEquals(false, document.isEmpty())
    }

    @Test
    fun getSensitivity() {
        val document = Document(1, "123")
        document + mapOf(FullName to 0)
        document + mapOf(Email to 1)
        assertEquals(1, document.funDetected())
        document + mapOf(Email to 1)
        assertEquals(1, document.funDetected())
        document + mapOf(FullName to 1)
        assertEquals(2, document.funDetected())
    }

    @Test
    fun isEmpty() {
        val document = Document(1, "123")
        document + mapOf(FullName to 0)
        assertEquals(true, document.isEmpty())
        document + mapOf(FullName to 1)
        assertEquals(false, document.isEmpty())
        document + mapOf(Email to 1)
        assertEquals(false, document.isEmpty())
    }

    @Test
    fun getLength() {
        val document = Document(1, "123")
        document + mapOf(FullName to 0)
        assertEquals(0, document.length())
        document + mapOf(Email to 1)
        assertEquals(1, document.length())
        document + mapOf(Email to 1)
        assertEquals(1, document.length())
        document + mapOf(FullName to 1)
        assertEquals(2, document.length())
    }

    @Test
    fun testToString() {
        val document = Document(1, "123")
        document + mapOf(FullName to 0)
        assertEquals("{}", document.toString())
        document + mapOf(Email to 1)
        assertEquals("{Email=1}", document.toString())
        document + mapOf(Email to 1)
        assertEquals("{Email=2}", document.toString())
        document + mapOf(FullName to 1)
        assertEquals("{Email=2, Full name=1}", document.toString())
    }

    @Test
    fun getDocumentFields() {
        val expected = mapOf<IMatcher, Int>(Email to 1)
        val document = Document(1, "123")
        document + mapOf(FullName to 0)
        assertEquals(mapOf(), document.getDocumentFields())
        document + mapOf(Email to 1)
        assertEquals(expected, document.getDocumentFields())
    }

    @Test
    fun getSize() {
        val document = Document(1, "123")
        assertEquals(1, document.size)
    }

    @Test
    fun getDocumentID() {
        val document = Document(1, "123")
        assertEquals("123", document.path)
    }
}