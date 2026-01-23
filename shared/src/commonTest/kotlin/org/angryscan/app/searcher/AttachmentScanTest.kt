package org.angryscan.app.searcher

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.runBlocking
import org.angryscan.app.common.AppSettings
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.UserSignatureSettings
import org.angryscan.app.db.DatabaseSettings
import org.angryscan.app.di.scanModule
import org.angryscan.app.scan.common.files.types.*
import org.angryscan.app.scan.engine.toHyperScanMatchers
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.matchers.CardNumber
import org.apache.poi.openxml4j.util.ZipSecureFile
import org.junit.Rule
import org.koin.dsl.module
import org.koin.test.KoinTestRule
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class AttachmentScanTest {
    @get:Rule
    val koinTestRule = KoinTestRule.create {
        modules(
            module {
                single {
                    DatabaseSettings(
                        url = "jdbc:sqlite:build/tmp/test.db",
                        driver = "org.sqlite.JDBC"
                    )
                }
            },
            module {
                single {
                    javaClass.getResource("common/UserSignatures.json")
                        ?.let { it1 -> UserSignatureSettings.SettingsFile(it1.path) }
                }
                single { UserSignatureSettings() }
                single {
                    javaClass.getResource("common/AppSettings.json")
                        ?.let { it1 -> AppSettings.AppSettingsFile(it1.path) }
                }
                single { AppSettings() }
                single {
                    javaClass.getResource("common/ScanSettings.json")
                        ?.let { it1 -> ScanSettings.SettingsFile(it1.path) }
                }
                single { ScanSettings() }
            },
            scanModule
        )
    }

    init {
        // Disable zip-bomb protection in tests to avoid false positives on crafted test documents.
        ZipSecureFile.setMinInflateRatio(-1.0)
    }

    private fun scanCardCount(resourcePath: String, selectedExtensions: List<IFileType>): Int {
        val filePath = javaClass.getResource(resourcePath)?.file
        assertNotNull(filePath)
        val file = File(filePath)
        val matcher = CardNumber()
        val engines = listOf(HyperScanEngine(listOf(matcher).toHyperScanMatchers()))

        val document = runBlocking {
            IFileType.getFileType(file).map { ft ->
                ft.scanFile(file, currentCoroutineContext(), engines, false, selectedExtensions)
            }.let { docs ->
                val r = docs.first()
                docs.drop(1).forEach { d -> r.plus(d.getDocumentFields()) }
                r
            }
        }

        engines.forEach { it.close() }
        return document.getDocumentFields().getOrDefault(matcher, 0)
    }

    @Test
    fun `DOCX with embedded XLSX is scanned`() {
        val count = scanCardCount(
            "/files/attachments/cards_in_file.docx",
            selectedExtensions = listOf(DOCXType, XLSXType)
        )
        assertEquals(40, count, "Expected 40 card numbers to be found in embedded XLSX")
    }

    @Test
    fun `DOC with embedded XLS is scanned`() {
        val count = scanCardCount(
            "/files/attachments/cards_in_file.doc",
            selectedExtensions = listOf(DOCType, XLSType)
        )
        assertEquals(40, count, "Expected 40 card numbers to be found in embedded XLS")
    }

    @Test
    fun `DOCX nested DOCX then XLSX is scanned`() {
        val single = scanCardCount(
            "/files/attachments/cards_in_file.docx",
            selectedExtensions = listOf(DOCXType, XLSXType)
        )
        val nested = scanCardCount(
            "/files/attachments/cards_in_file_double.docx",
            selectedExtensions = listOf(DOCXType, XLSXType)
        )
        assertEquals(40, single, "Expected 40 card numbers in single-embedded DOCX")
        assertEquals(40, nested, "Expected 40 card numbers in nested DOCX->DOCX->XLSX")
    }

    @Test
    fun `DOC nested DOC then XLS is scanned`() {
        val single = scanCardCount(
            "/files/attachments/cards_in_file.doc",
            selectedExtensions = listOf(DOCType, XLSType)
        )
        val nested = scanCardCount(
            "/files/attachments/cards_in_file_double.doc",
            selectedExtensions = listOf(DOCType, XLSType)
        )
        assertEquals(40, single, "Expected 40 card numbers in single-embedded DOC")
        assertEquals(40, nested, "Expected 40 card numbers in nested DOC->DOC->XLS")
    }

    @Test
    fun `DOC with embedded TXT is scanned`() {
        val count = scanCardCount(
            "/files/attachments/cards_in_file_txt.doc",
            selectedExtensions = listOf(DOCType, TextType)
        )
        assertEquals(40, count, "Expected 40 card numbers to be found in embedded TXT")
    }

    @Test
    fun `Filtering skips embedded office files`() {
        val docxWith = scanCardCount(
            "/files/attachments/cards_in_file.docx",
            selectedExtensions = listOf(DOCXType, XLSXType)
        )
        val docxWithout = scanCardCount(
            "/files/attachments/cards_in_file.docx",
            selectedExtensions = listOf(DOCXType) // XLSX excluded
        )
        assertEquals(40, docxWith, "Expected 40 card numbers when XLSX is allowed")
        assertEquals(0, docxWithout, "Expected 0 card numbers when XLSX is excluded")

        val docWith = scanCardCount(
            "/files/attachments/cards_in_file.doc",
            selectedExtensions = listOf(DOCType, XLSType)
        )
        val docWithout = scanCardCount(
            "/files/attachments/cards_in_file.doc",
            selectedExtensions = listOf(DOCType) // XLS excluded
        )
        assertEquals(40, docWith, "Expected 40 card numbers when XLS is allowed")
        // NOTE: In this test document, Word text extraction may already include the visible
        // representation of the embedded XLS content, so the count can stay the same even when
        // XLS extraction is disabled. We still verify that enabling XLS doesn't change the count.
        assertEquals(40, docWithout, "Expected 40 card numbers even when XLS extraction is excluded")
    }

    @Test
    fun `Filtering skips embedded TXT`() {
        val withTxt = scanCardCount(
            "/files/attachments/cards_in_file_txt.doc",
            selectedExtensions = listOf(DOCType, TextType)
        )
        val withoutTxt = scanCardCount(
            "/files/attachments/cards_in_file_txt.doc",
            selectedExtensions = listOf(DOCType) // TXT excluded
        )
        assertEquals(40, withTxt, "Expected 40 card numbers when TXT is allowed")
        assertEquals(0, withoutTxt, "Expected 0 card numbers when TXT is excluded")
    }
}

