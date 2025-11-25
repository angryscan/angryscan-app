package org.angryscan.app.di

import org.angryscan.app.common.*
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val settingsModule = module {
    single { UserSignatureSettings.SettingsFile(AppFiles.UserSignaturesFiles) }
    singleOf(::UserSignatureSettings)
    single { AppSettings.AppSettingsFile(AppFiles.AppSettingsFile) }
    singleOf(::AppSettings)
    single { ScanSettings.SettingsFile(AppFiles.ScanSettingsFile) }
    singleOf(::ScanSettings)
    single { ScreenStateSettings.SettingsFile(AppFiles.ScreenStateSettingsFile) }
    singleOf(::ScreenStateSettings)
}