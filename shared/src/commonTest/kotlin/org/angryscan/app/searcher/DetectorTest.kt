package org.angryscan.app.searcher

import IKoinTestRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.engine.kotlin.KotlinEngine
import org.angryscan.common.extensions.Matchers
import org.angryscan.common.matchers.CardNumber
import org.angryscan.common.matchers.INN
import org.angryscan.common.matchers.Passport
import org.angryscan.common.matchers.SNILS
import org.angryscan.app.scan.common.Document
import org.angryscan.app.scan.common.files.FileType
import org.angryscan.app.scan.engine.toHyperScanMatchers
import org.angryscan.app.scan.engine.toKotlinMatchers
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class DetectorTest : IKoinTestRule {
    @Test
    fun scan() {
        val sampleText = """Sample text, 4279432112344321 199-510-399 13  
583410778676
омс 7755320882002755"""
        val doc = Document(1, "123")
        val engine = KotlinEngine(Matchers.toKotlinMatchers())
        engine
            .scan(sampleText)
            .groupBy { it.matcher }
            .map { it.key to it.value.count() }
            .forEach { (t, u) ->
                doc.updateDocument(t, u)
            }
        
        assertEquals(4, doc.length())
    }

    @Test
    fun testText() {
        val file = javaClass.getResource("/files/TestText.txt")?.file
        assertNotNull(file)
        
        for (attribute in Matchers) {
            assertEquals(1, getCountOfAttribute(file, attribute))
        }
    }

    @Test
    fun testCardEdge() {
        val file = javaClass.getResource("/files/cardNumber/edge.txt")?.file
        assertNotNull(file)
        assertEquals(5, getCountOfAttribute(file, CardNumber()))
    }

    @Test
    fun testCardWithBrace() {
        val file = javaClass.getResource("/files/cardNumber/braces.txt")?.file
        assertNotNull(file)
        assertEquals(3, getCountOfAttribute(file, CardNumber()))
    }

    @Test
    fun testCardWithSmth() {
        val file = javaClass.getResource("/files/cardNumber/smth.txt")?.file
        assertNotNull(file)
        assertEquals(8, getCountOfAttribute(file, CardNumber()))
    }

    @Test
    fun testCardWithStar() {
        val file = javaClass.getResource("/files/cardNumber/star.txt")?.file
        assertNotNull(file)
        assertEquals(1, getCountOfAttribute(file, CardNumber()))
    }

    @Test
    fun testCardNotValid() {
        val file = javaClass.getResource("/files/cardNumber/notValid.txt")?.file
        assertNotNull(file)
        assertEquals(0, getCountOfAttribute(file, CardNumber()))
    }

    @Test
    fun testSnilsEdge() {
        val file = javaClass.getResource("/files/snils/edge.txt")?.file
        assertNotNull(file)
        assertEquals(6, getCountOfAttribute(file, SNILS))
    }

    @Test
    fun testSnilsWithBrace() {
        val file = javaClass.getResource("/files/snils/braces.txt")?.file
        assertNotNull(file)
        assertEquals(5, getCountOfAttribute(file, SNILS))
    }

    @Test
    fun testSnilsWithSmth() {
        val file = javaClass.getResource("/files/snils/smth.txt")?.file
        assertNotNull(file)
        assertEquals(6, getCountOfAttribute(file, SNILS))
    }

    @Test
    fun testSnilsWithStar() {
        val file = javaClass.getResource("/files/snils/star.txt")?.file
        assertNotNull(file)
        assertEquals(1, getCountOfAttribute(file, SNILS))
    }

    @Test
    fun testSnilsNotValid() {
        val file = javaClass.getResource("/files/snils/notValid.txt")?.file
        assertNotNull(file)
        assertEquals(0, getCountOfAttribute(file, SNILS))
    }

    @Test
    fun testInnEdge() {
        val file = javaClass.getResource("/files/inns/edge.txt")?.file
        assertNotNull(file)
        assertEquals(5, getCountOfAttribute(file, INN))
    }

    @Test
    fun testInnWithBrace() {
        val file = javaClass.getResource("/files/inns/braces.txt")?.file
        assertNotNull(file)
        assertEquals(2, getCountOfAttribute(file, INN))
    }

    @Test
    fun testInnWithSmth() {
        val file = javaClass.getResource("/files/inns/smth.txt")?.file
        assertNotNull(file)
        assertEquals(7, getCountOfAttribute(file, INN))
    }

    @Test
    fun testInnWithStar() {
        val file = javaClass.getResource("/files/inns/star.txt")?.file
        assertNotNull(file)
        assertEquals(1, getCountOfAttribute(file, INN))
    }

    @Test
    fun testInnNotValid() {
        val file = javaClass.getResource("/files/inns/notValid.txt")?.file
        assertNotNull(file)
        assertEquals(0, getCountOfAttribute(file, INN))
    }

    @Test
    fun testPassports() {
        val file = javaClass.getResource("/files/passport/passport.txt")?.file
        assertNotNull(file)
        assertEquals(1, getCountOfAttribute(file, Passport))
    }

    private fun getCountOfAttribute(filePath: String, matcher: IMatcher): Int {

        val file = File(filePath)

        assertEquals(true, file.exists())

        val coroutineContext = Dispatchers.Default
        val engines = listOf(HyperScanEngine(listOf(matcher).toHyperScanMatchers()))

        val document = runBlocking(coroutineContext) {
            FileType
                .getFileType(file)?.scanFile(file, coroutineContext, engines, false).let {
                assertNotNull(it)
            }
        }
        return document.getDocumentFields().getOrDefault(matcher, 0)
    }
}
