package org.angryscan.app.di

import org.angryscan.app.common.*
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
    single<S3ConnectionSecretStore> { DesktopS3ConnectionSecretStore() }
}