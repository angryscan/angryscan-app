package org.angryscan.app.common

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.angryscan.app.scan.common.files.types.*
import org.angryscan.app.scan.functions.CertDetectFun
import org.angryscan.app.scan.functions.CodeDetectFun
import org.angryscan.app.serializers.MutableStateSerializer
import org.angryscan.app.serializers.PolymorphicFormatter
import org.angryscan.app.ui.components.SelectionTypes
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.matchers.*
import org.angryscan.gitleaks.matcher.GitleaksMatcher
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

@Serializable
class ScreenStateSettings : KoinComponent {
    @Transient
    private val logger = KotlinLogging.logger {}

    class SettingsFile(path: String) : File(path)

    private val settingsFile: SettingsFile by inject()

    private val appSettings: AppSettings by inject()

    @Serializable
    data class FileShareScreenState(
        var path: String = "",
        @Serializable(with = MutableStateSerializer::class)
        var selectionType: MutableState<SelectionTypes> = mutableStateOf(SelectionTypes.Folder),
        @Serializable
        val extensions: MutableList<IFileType> = mutableStateListOf(),
        @Serializable
        val matchers: MutableList<IMatcher> = mutableStateListOf(),
        @Serializable
        val userSignatures: MutableList<UserSignature> = mutableStateListOf(),
        @Serializable(with = MutableStateSerializer::class)
        var fastScan: MutableState<Boolean> = mutableStateOf(false)
    )

    @Serializable
    data class S3ScreenState(
        var path: String = "",
        var endpoint: String = "",
        var accessKey: String = "",
        var secretKey: String = "",
        var bucket: String = "",
        var connectionSettingsExpanded: Boolean = false,
        @Serializable
        val extensions: MutableList<IFileType> = mutableStateListOf(),
        @Serializable
        val matchers: MutableList<IMatcher> = mutableStateListOf(),
        @Serializable
        val userSignatures: MutableList<UserSignature> = mutableStateListOf(),
        @Serializable(with = MutableStateSerializer::class)
        var fastScan: MutableState<Boolean> = mutableStateOf(false)
    )

    @Serializable
    data class HTTPScreenState(
        var path: String = "",
        @Serializable
        val extensions: MutableList<IFileType> = mutableStateListOf(),
        @Serializable
        val matchers: MutableList<IMatcher> = mutableStateListOf(),
        @Serializable
        val userSignatures: MutableList<UserSignature> = mutableStateListOf(),
        @Serializable(with = MutableStateSerializer::class)
        var fastScan: MutableState<Boolean> = mutableStateOf(false)
    )

    @Serializable
    data class SqlDatabaseScreenState(
        var databaseType: DatabaseType = DatabaseType.PostgreSQL,
        var schema: String = "",
        var host: String = "",
        var port: String = "5432",
        var database: String = "",
        var user: String = "",
        var password: String = "",
        var filePath: String = "",
        var rowLimit: String = "1000",
        @Serializable
        val extensions: MutableList<IFileType> = mutableStateListOf(),
        @Serializable
        val matchers: MutableList<IMatcher> = mutableStateListOf(),
        @Serializable
        val userSignatures: MutableList<UserSignature> = mutableStateListOf(),
        @Serializable(with = MutableStateSerializer::class)
        var fastScan: MutableState<Boolean> = mutableStateOf(false)
    )

    @Serializable
    data class ScanProfile(
        var name: String = "Default",
        var fastScan: Boolean = false,
        @Serializable
        val extensions: MutableList<IFileType> = mutableListOf(),
        @Serializable
        val matchers: MutableList<IMatcher> = mutableListOf(),
        @Serializable
        val userSignatures: MutableList<UserSignature> = mutableListOf()
    )

    var fileShareScreenState = FileShareScreenState()
    var s3ScreenState = S3ScreenState()
    var httpScreenState = HTTPScreenState()
    @Serializable(with = MutableStateSerializer::class)
    var sqlScreenState = mutableStateOf(SqlDatabaseScreenState())
    var scanProfiles: MutableList<ScanProfile> = mutableStateListOf()
    var activeScanProfileName: String = "Default"

    private fun defaultProfileExtensions(): MutableList<IFileType> =
        ScanSettingsDefaults.defaultExtensions().toMutableList()

    /** Same defaults as [ScanSettings] first launch (extensions + matchers by app language). */
    private fun seedDetectionSourcesFromDefaults() {
        val ext = ScanSettingsDefaults.defaultExtensions()
        val mat = ScanSettingsDefaults.defaultMatchers(appSettings)
        fileShareScreenState.extensions.clear()
        fileShareScreenState.extensions.addAll(ext)
        fileShareScreenState.matchers.clear()
        fileShareScreenState.matchers.addAll(mat)
        s3ScreenState.extensions.clear()
        s3ScreenState.extensions.addAll(ext)
        s3ScreenState.matchers.clear()
        s3ScreenState.matchers.addAll(mat)
        httpScreenState.extensions.clear()
        httpScreenState.extensions.addAll(ext)
        httpScreenState.matchers.clear()
        httpScreenState.matchers.addAll(mat)
    }

    private fun defaultScanProfiles(): MutableList<ScanProfile> {
        val defaultExtensions = defaultProfileExtensions()
        return mutableListOf(
            ScanProfile(
                name = "Sensitive data",
                fastScan = false,
                extensions = defaultExtensions.toMutableList(),
                matchers = mutableListOf(
                    Phone, PhoneUS, SNILS, SSN, Passport, PassportUS,
                    INN, DriverLicense, DriverLicenseUS, FullName, FullNameUS,
                    Address, AddressUS, Birthday, Email
                )
            ),
            ScanProfile(
                name = "Banking secrecy",
                fastScan = false,
                extensions = defaultExtensions.toMutableList(),
                matchers = mutableListOf(
                    CardNumber(), CVV, BankAccount, BankAccountLE, RTN, EIN, ITIN
                )
            ),
            ScanProfile(
                name = "IT assets + secrets",
                fastScan = true,
                extensions = defaultExtensions.toMutableList(),
                matchers = mutableListOf(
                    IPv4, IPv6, CodeDetectFun, CertDetectFun, HashData, GitleaksMatcher
                )
            ),
            ScanProfile(
                name = "Documents only",
                fastScan = false,
                extensions = mutableListOf(
                    DOCType, DOCXType, ODTType, PDFType,
                    XLSType, XLSXType, ODSType,
                    PPTType, PPTXType, ODPType,
                    TextType
                ),
                matchers = mutableListOf(
                    Email, Phone, PhoneUS, CardNumber(), CVV,
                    FullName, FullNameUS, Passport, PassportUS, INN, SSN
                )
            )
        )
    }

    private fun ensureSystemProfilesPresent() {
        scanProfiles.forEach { profile ->
            when (profile.name) {
                "Personal data ALL", "Персональные данные ALL", "Personal data", "Персональные данные" -> profile.name = "Sensitive data"
                "Banking secrecy ALL", "Банковская тайна ALL" -> profile.name = "Banking secrecy"
            }
        }
        val deprecatedLocalizedProfileNames = setOf(
            "Personal data RU",
            "Personal data US",
            "Banking secrecy RU",
            "Banking secrecy US",
            "Персональные данные RU",
            "Персональные данные US",
            "Банковская тайна RU",
            "Банковская тайна US"
        )
        scanProfiles.removeAll { it.name in deprecatedLocalizedProfileNames }
        val existingNames = scanProfiles.map { it.name }.toMutableSet()
        defaultScanProfiles().forEach { profile ->
            if (profile.name !in existingNames) {
                scanProfiles.add(profile)
                existingNames.add(profile.name)
            }
        }
    }

    constructor() {
        val userSignatureSettings by inject<UserSignatureSettings>()
        try {
            if (settingsFile.exists()) {
                val prop: ScreenStateSettings = PolymorphicFormatter.decodeFromString(settingsFile.readText())

                // Restore FileShareScreen state
                this.fileShareScreenState.path = prop.fileShareScreenState.path
                this.fileShareScreenState.selectionType = prop.fileShareScreenState.selectionType
                this.fileShareScreenState.extensions.clear()
                this.fileShareScreenState.extensions.addAll(prop.fileShareScreenState.extensions)
                this.fileShareScreenState.matchers.clear()
                this.fileShareScreenState.matchers.addAll(prop.fileShareScreenState.matchers.distinct())
                this.fileShareScreenState.userSignatures.clear()
                this.fileShareScreenState.userSignatures.addAll(
                    prop.fileShareScreenState.userSignatures.filter { it in userSignatureSettings.userSignatures }
                )
                this.fileShareScreenState.fastScan = prop.fileShareScreenState.fastScan

                // Restore S3Screen state
                this.s3ScreenState.path = prop.s3ScreenState.path
                this.s3ScreenState.endpoint = prop.s3ScreenState.endpoint
                this.s3ScreenState.accessKey = prop.s3ScreenState.accessKey
                this.s3ScreenState.secretKey = prop.s3ScreenState.secretKey
                this.s3ScreenState.bucket = prop.s3ScreenState.bucket
                this.s3ScreenState.connectionSettingsExpanded = prop.s3ScreenState.connectionSettingsExpanded
                this.s3ScreenState.extensions.clear()
                this.s3ScreenState.extensions.addAll(prop.s3ScreenState.extensions)
                this.s3ScreenState.matchers.clear()
                this.s3ScreenState.matchers.addAll(prop.s3ScreenState.matchers.distinct())
                this.s3ScreenState.userSignatures.clear()
                this.s3ScreenState.userSignatures.addAll(
                    prop.s3ScreenState.userSignatures.filter { it in userSignatureSettings.userSignatures }
                )
                this.s3ScreenState.fastScan = prop.s3ScreenState.fastScan

                // Restore HTTPScreen state
                this.httpScreenState.path = prop.httpScreenState.path
                this.httpScreenState.extensions.clear()
                this.httpScreenState.extensions.addAll(prop.httpScreenState.extensions)
                this.httpScreenState.matchers.clear()
                this.httpScreenState.matchers.addAll(prop.httpScreenState.matchers.distinct())
                this.httpScreenState.userSignatures.clear()
                this.httpScreenState.userSignatures.addAll(
                    prop.httpScreenState.userSignatures.filter { it in userSignatureSettings.userSignatures }
                )
                this.httpScreenState.fastScan = prop.httpScreenState.fastScan

                // Restore SqlDatabase state (migration: old PostgresScreenState → SqlDatabaseScreenState, databaseType defaults to PostgreSQL)
                this.sqlScreenState.value = prop.sqlScreenState.value
                this.sqlScreenState.value.extensions.clear()
                this.sqlScreenState.value.extensions.addAll(prop.sqlScreenState.value.extensions)
                this.sqlScreenState.value.matchers.clear()
                this.sqlScreenState.value.matchers.addAll(prop.sqlScreenState.value.matchers.distinct())
                this.sqlScreenState.value.userSignatures.clear()
                this.sqlScreenState.value.userSignatures.addAll(
                    prop.sqlScreenState.value.userSignatures.filter { it in userSignatureSettings.userSignatures }
                )
                this.sqlScreenState.value.fastScan = prop.sqlScreenState.value.fastScan

                this.scanProfiles.clear()
                this.scanProfiles.addAll(
                    prop.scanProfiles.map { profile ->
                        profile.copy(
                            userSignatures = profile.userSignatures
                                .filter { it in userSignatureSettings.userSignatures }
                                .toMutableList(),
                            matchers = profile.matchers.distinct().toMutableList(),
                            extensions = profile.extensions.toMutableList()
                        )
                    }
                )
                if (this.scanProfiles.isEmpty()) {
                    this.scanProfiles.addAll(defaultScanProfiles())
                }
                this.activeScanProfileName =
                    if (prop.activeScanProfileName.isNotBlank()) prop.activeScanProfileName
                    else this.scanProfiles.first().name
                ensureSystemProfilesPresent()
                if (this.scanProfiles.none { it.name == this.activeScanProfileName }) {
                    this.activeScanProfileName = this.scanProfiles.first().name
                }
            } else {
                seedDetectionSourcesFromDefaults()
            }
        } catch (e: Exception) {
            logger.error(e) {
                "Failed to load ScreenStateSettings. Loading defaults."
            }
            if (scanProfiles.isEmpty()) {
                scanProfiles.addAll(defaultScanProfiles())
                activeScanProfileName = scanProfiles.first().name
            }
            seedDetectionSourcesFromDefaults()
        }
        if (scanProfiles.isEmpty()) {
            scanProfiles.addAll(defaultScanProfiles())
            activeScanProfileName = scanProfiles.first().name
        }
        ensureSystemProfilesPresent()
        if (scanProfiles.none { it.name == activeScanProfileName }) {
            activeScanProfileName = scanProfiles.first().name
        }
    }

    fun save() {
        try {
            settingsFile.writeText(PolymorphicFormatter.encodeToString(this))
        } catch (e: Exception) {
            logger.error(e) {
                "Failed to save ScreenStateSettings."
            }
        }
    }
}

