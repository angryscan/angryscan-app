package org.angryscan.app.common

import org.angryscan.app.scan.common.files.types.*
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.matchers.*

/**
 * Defaults applied when [ScanSettings] cannot be loaded from disk (first launch / corrupt file).
 */
object ScanSettingsDefaults {

    private val archiveTypes: Set<IFileType> = setOf(ZIPType, RARType)

    fun defaultExtensions(): List<IFileType> =
        IFileType.getAll().filter {
            it !in CertFileType.entries &&
                it !in CodeFileType.entries &&
                it !in archiveTypes
        }

    fun defaultMatchers(appSettings: AppSettings): List<IMatcher> =
        when (appSettings.effectiveLocaleTag()) {
            "ru" -> russianFirstLaunchMatchers()
            else -> englishFirstLaunchMatchers()
        }

    /** RU flag: ФИО, адрес, телефон, паспорт, ИНН, СНИЛС; 🌍: email, coordinates, card, CVV. */
    private fun russianFirstLaunchMatchers(): List<IMatcher> = listOf(
        FullName,
        Address,
        Phone,
        Passport,
        INN,
        SNILS,
        Email,
        Geo,
        CardNumber(),
        CVV,
    )

    /** US flag: Full name (US) … SSN; 🌍: email, coordinates, card, CVV. */
    private fun englishFirstLaunchMatchers(): List<IMatcher> = listOf(
        FullNameUS,
        AddressUS,
        PhoneUS,
        PassportUS,
        DriverLicenseUS,
        SSN,
        Email,
        Geo,
        CardNumber(),
        CVV,
    )
}
