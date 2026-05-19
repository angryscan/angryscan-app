package org.angryscan.app

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.use
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.koin.test.KoinTestRule
import org.koin.test.get
import org.angryscan.app.common.AppSettings
import org.angryscan.app.common.DesktopS3ConnectionSecretStore
import org.angryscan.app.common.DesktopSqlConnectionSecretStore
import org.angryscan.app.common.S3ConnectionSecretStore
import org.angryscan.app.common.SqlConnectionSecretStore
import org.angryscan.app.db.DatabaseSettings
import org.angryscan.app.di.s3Module
import org.angryscan.app.di.scanModule
import org.angryscan.app.di.settingsModule
import org.angryscan.app.ui.MainWindow
import org.angryscan.app.ui.theme.AppTheme
import org.angryscan.app.ui.windows.ApplicationErrorWindow
import java.awt.EventQueue
import kotlin.test.Test
import kotlin.test.assertNotNull

internal class MainKtTest : KoinTest {
    @get:org.junit.Rule
    val koinTestRule = KoinTestRule.create {
        modules(
            module {
                single {
                    DatabaseSettings(
                        url = "jdbc:sqlite:build/tmp/test.db",
                        driver = "org.sqlite.JDBC"
                    )
                }
                single<SqlConnectionSecretStore> { DesktopSqlConnectionSecretStore() }
                single<S3ConnectionSecretStore> { DesktopS3ConnectionSecretStore() }
            },
            settingsModule,
            scanModule,
            s3Module,
        )
    }

    @Test
    fun customTheme() {
        val appSettings = get<AppSettings>()
        appSettings.theme.value = AppSettings.ThemeType.System
        ImageComposeScene(width = 1024, height = 768).use { window ->
            window.setContent {
                AppTheme { }
            }
        }
        appSettings.theme.value = AppSettings.ThemeType.Dark
        ImageComposeScene(width = 1024, height = 768).use { window ->
            window.setContent {
                AppTheme { }
            }
        }
        appSettings.theme.value = AppSettings.ThemeType.Light
        ImageComposeScene(width = 1024, height = 768).use { window ->
            window.setContent {
                AppTheme { }
            }
        }
    }

    @Test
    fun guiRunTest() {
        EventQueue.invokeAndWait {
            val uiPath = javaClass.getResource("/common/ui.json")?.file
            assertNotNull(uiPath)
            var isVisible = true

            val appSettings = get<AppSettings>()

            appSettings.theme.value = AppSettings.ThemeType.System
            ImageComposeScene(width = 1024, height = 768).use { window ->
                window.setContent {
                    MainWindow(
                        isVisible = isVisible,
                        onHideRequest = { isVisible = false },
                        onShowRequest = {

                        },
                        onCloseRequest = {

                        }
                    )
                }
            }
            appSettings.theme.value = AppSettings.ThemeType.Dark
            ImageComposeScene(width = 1024, height = 768).use { window ->
                window.setContent {
                    MainWindow(
                        isVisible = isVisible,
                        onHideRequest = { isVisible = false },
                        onShowRequest = {

                        },
                        onCloseRequest = {

                        }
                    )
                }
            }
            appSettings.theme.value = AppSettings.ThemeType.Light
            ImageComposeScene(width = 1024, height = 768).use { window ->
                window.setContent {
                    MainWindow(
                        isVisible = isVisible,
                        onHideRequest = { isVisible = false },
                        onShowRequest = {

                        },
                        onCloseRequest = {

                        }
                    )
                }
            }
        }
    }

    @Test
    fun onError() {
        ImageComposeScene(width = 1024, height = 768).use { window ->
            window.setContent {
                ApplicationErrorWindow(Exception("SomeError"))
            }
        }
    }
}
