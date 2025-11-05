package org.angryscan.app.searcher

import org.apache.poi.openxml4j.util.ZipSecureFile
import kotlinx.coroutines.*
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.engine.kotlin.KotlinEngine
import org.angryscan.common.extensions.Matchers
import org.junit.Rule
import org.koin.dsl.module
import org.koin.test.KoinTestRule
import org.angryscan.app.common.AppSettings
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.UserSignatureSettings
import org.angryscan.app.db.DatabaseSettings
import org.angryscan.app.di.scanModule
import org.angryscan.app.scan.common.files.FileType
import org.angryscan.app.scan.engine.toHyperScanMatchers
import org.angryscan.app.scan.engine.toKotlinMatchers
import java.io.File
import java.io.FileWriter
import kotlin.test.*

internal class FileTypeTest() {
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
        ZipSecureFile.setMinInflateRatio(-1.0) // отключение срабатывания исключения для zip-бомбы
    }

    @Test
    fun `Check file types`() {
        listOf(
            "1.docx",
            "emails_result.xlsx",
            "third.xlsx",
            "TestText.txt",
            "5.csv",
            "small.xls",
            "first/first.doc",
            "first/first.xls",
            "first/first.docx",
            "first/first.xlsx",
            "first/first.odt",
            "first/first.odp",
            "first/first.otp",
            "first/first.pptx",
            "first/first.potx",
            "first/first.ppsx",
            "first/first.pptm",
            "first/first.ppt",
            "first/first.pps",
            "first/first.pot",
            "first/first.ods",
            "first/first.pdf",
            "very_short.xlsx",
            "ipv6.txt"
        )
            .forEach { filename ->
                runBlocking {
                    try {
                        print("Scanning file: $filename")
                        val millis = System.currentTimeMillis()
                        val path = javaClass.getResource("/files/$filename")
                        assertNotNull(path)
                        val f = File(path.file)
                        val enumType: FileType? = f.let { FileType.getFileType(it) }
                        enumType?.scanFile(
                            f,
                            currentCoroutineContext(),
                            listOf(HyperScanEngine(Matchers.toHyperScanMatchers())),
                            false
                        ).let { doc ->
                            Matrix.getMap(filename)
                                ?.let { m -> assertEquals(m, doc?.getDocumentFields(), "File: $filename") }
                                ?: println("Нет данных для $filename")
                        }
                        println("; OK; time: ${System.currentTimeMillis() - millis}")
                    } catch (e: Exception) {
                        fail(e.message)
                    }
                }
            }
    }

    // проверить на очень длинном файле
    @Test
    fun `Check fast and full scan`() {
        val filelist = listOf(
            "veryLong/very_long.log",
            "veryLong/very_long.xlsx",
            "veryLong/very_long.docx",
            "veryLong/very_long.txt",
            "veryLong/very_long.csv",
            "veryLong/very_long.xml",
            "veryLong/very_long.json",
            "veryLong/very_long.doc",
            "veryLong/very_long.xls",
            "veryLong/very_long.pdf"
        )

        fun checkScan(filename: String, map: Map<IMatcher, Int>?, isFastScan: Boolean = false) {

            val path = javaClass.getResource("/files/$filename")
            assertNotNull(path)
            val f = File(path.file)
            val enumType: FileType? = f.let { FileType.getFileType(it) }

            val engines = listOf(
                KotlinEngine(Matchers.toKotlinMatchers())
            )

            runBlocking {
                enumType?.scanFile(f, currentCoroutineContext(), engines, isFastScan).let {
                    assertNotNull(it)
                    assertEquals(map, it.getDocumentFields())
                }
            }
        }

        println("Checking fast scan")
        filelist.forEach { filename ->
            println(filename)
            val map = Matrix.getMap(filename, true)
            checkScan(filename, map, true)
        }
        println("Checking full scan")
        filelist.forEach { filename ->
            println(filename)
            val map = Matrix.getMap(filename, false)
            checkScan(filename, map, false)
        }
    }

    @Test
    fun `Check FileNotFoundException`() {
        val f = File("notExist.txt")
        assertFalse(f.exists())
        val engines = listOf(
            KotlinEngine(Matchers.toKotlinMatchers())
        )
        runBlocking {
            try {
                val enumType: FileType? = FileType.getFileType(f)
                enumType?.scanFile(f, currentCoroutineContext(), engines, false).let {
                    assertEquals(mapOf(), it?.getDocumentFields())
                    assertTrue(it?.skipped() == true)
                }
            } catch (e: Exception) {
                fail(e.message)
            }
        }
    }

    @Test
    fun `Check empty doc file exception`() {
        val path = javaClass.getResource("/files/empty.doc")
        assertNotNull(path)
        val f = File(path.file)

        val writer = FileWriter(f)
        writer.write("content")
        writer.close()
        assertTrue(f.exists())

        val engines = listOf(
            KotlinEngine(Matchers.toKotlinMatchers())
        )

        runBlocking {
            try {
                FileType.DOC.scanFile(f, currentCoroutineContext(), engines, false).let {
                    assertEquals(0, it.length())
                    assertEquals(mapOf(), it.getDocumentFields())
                }
            } catch (e: Exception) {
                fail(e.message)
            }
        }
    }
}