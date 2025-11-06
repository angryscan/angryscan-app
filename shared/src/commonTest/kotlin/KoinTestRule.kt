import org.junit.Rule
import org.koin.dsl.module
import org.koin.test.KoinTestRule
import org.angryscan.app.common.AppSettings
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.UserSignatureSettings
import org.angryscan.app.db.DatabaseSettings
import org.angryscan.app.di.scanModule

interface IKoinTestRule {
    @get:Rule
    val koinTestRule: KoinTestRule
        get() = KoinTestRule.create {
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
}