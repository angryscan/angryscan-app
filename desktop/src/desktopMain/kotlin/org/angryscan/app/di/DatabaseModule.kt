package org.angryscan.app.di

import org.angryscan.app.common.AppFiles
import org.angryscan.app.common.DesktopSqlConnectionSecretStore
import org.angryscan.app.common.SqlConnectionSecretStore
import org.angryscan.app.db.DatabaseSettings
import org.koin.dsl.module

val databaseModule = module {
    single {
        DatabaseSettings(
            url = "jdbc:sqlite:${AppFiles.WorkDir.resolve("ads.db").absolutePath}",
            driver = "org.sqlite.JDBC"
        )
    }
    single<SqlConnectionSecretStore> { DesktopSqlConnectionSecretStore() }
}