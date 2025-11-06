package org.angryscan.app.di

import org.koin.dsl.module
import org.angryscan.app.common.AppFiles
import org.angryscan.app.db.DatabaseSettings

val databaseModule = module {
    single {
        DatabaseSettings(
            url = "jdbc:sqlite:${AppFiles.WorkDir.resolve("ads.db").absolutePath}",
            driver = "org.sqlite.JDBC"
        )
    }
}