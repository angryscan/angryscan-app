package org.angryscan.app.scan.functions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.angryscan.common.engine.kotlin.KotlinEngine
import org.junit.Rule
import org.koin.dsl.module
import org.koin.test.KoinTestRule
import org.angryscan.app.common.AppSettings
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.UserSignatureSettings
import org.angryscan.app.db.DatabaseSettings
import org.angryscan.app.di.scanModule
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class CertFileTypeTest {

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
                single { javaClass.getResource("common/UserSignatures.json")
                    ?.let { it1 -> UserSignatureSettings.SettingsFile(it1.path) } }
                single { UserSignatureSettings() }
                single { javaClass.getResource("common/AppSettings.json")
                    ?.let { it1 -> AppSettings.AppSettingsFile(it1.path) } }
                single { AppSettings() }
                single { javaClass.getResource("common/ScanSettings.json")
                    ?.let { it1 -> ScanSettings.SettingsFile(it1.path) } }
                single { ScanSettings() }
            },
            scanModule
        )
    }

    @Test
    fun scanFile() {
        val coroutineContext = Dispatchers.Default
        val p7sFile = javaClass.getResource("/files/certs/signature.p7s")?.file
        assertNotNull(p7sFile)
        runBlocking {
            val res = CertFileType.PKCS.scanFile(
                File(p7sFile),
                coroutineContext,
                engines = listOf(KotlinEngine(listOf(CertDetectFun))),
                false
            )
            assertEquals(3, res.getDocumentFields()[CertDetectFun])
        }

        val p7b1File = javaClass.getResource("/files/certs/1.p7b")?.file
        assertNotNull(p7b1File)
        runBlocking {
            val res = CertFileType.PKCS.scanFile(
                File(p7b1File),
                coroutineContext,
                engines = listOf(KotlinEngine(listOf(CertDetectFun))),
                false
            )
            assertEquals(1, res.getDocumentFields()[CertDetectFun])
        }

        val keyDerFile = javaClass.getResource("/files/certs/key.der")?.file
        assertNotNull(keyDerFile)
        runBlocking {
            val res = CertFileType.PKCS.scanFile(
                File(keyDerFile),
                coroutineContext,
                engines = listOf(KotlinEngine(listOf(CertDetectFun))),
                false
            )
            assertEquals(1, res.getDocumentFields()[CertDetectFun])
        }
    }

}