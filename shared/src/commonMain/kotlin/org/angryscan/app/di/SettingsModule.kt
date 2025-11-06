package org.angryscan.app.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.angryscan.app.common.AppFiles
import org.angryscan.app.common.AppSettings
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.UserSignatureSettings

val settingsModule = module {
    single { UserSignatureSettings.SettingsFile(AppFiles.UserSignaturesFiles) }
    singleOf(::UserSignatureSettings)
    single { AppSettings.AppSettingsFile(AppFiles.AppSettingsFile) }
    singleOf(::AppSettings)
    single { ScanSettings.SettingsFile(AppFiles.ScanSettingsFile) }
    singleOf(::ScanSettings)
}