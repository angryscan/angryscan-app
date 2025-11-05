package org.angryscan.app.common

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.angryscan.app.scan.common.writer.ResultWriter
import org.angryscan.app.serializers.MutableStateSerializer
import java.io.File
import java.util.*
import kotlin.math.max

@Serializable
class AppSettings : KoinComponent {
    @Transient
    private val logger = KotlinLogging.logger {  }
    class AppSettingsFile(path: String) : File(path)

    enum class ThemeType {
        System,
        Dark,
        Light,
    }

    enum class LanguageType(val text: String, val locale: String) {
        Default(
            text = when (Locale.getDefault().language) {
                "ru" -> "По умолчанию (RU)"
                "en" -> "Default (EN)"
                else -> "По умолчанию (RU)"
            },
            locale = when (Locale.getDefault().language) {
                "ru" -> "ru"
                "en" -> "en"
                else -> "ru"
            }
        ),
        RU(
            text = "Русский",
            locale = "ru"
        ),
        EN(
            text = "English",
            locale = "en"
        ),
    }

    private val settingsFile: AppSettingsFile by inject()

    @Serializable(with = MutableStateSerializer::class)
    var threadCount: MutableState<Int> = mutableStateOf(max(
        Runtime.getRuntime().availableProcessors() /2,
        1
    ))

    @Serializable(with = MutableStateSerializer::class)
    var theme: MutableState<ThemeType> = mutableStateOf(ThemeType.System)

    @Serializable(with = MutableStateSerializer::class)
    var language: MutableState<LanguageType> = mutableStateOf(LanguageType.Default)

    @Serializable(with = MutableStateSerializer::class)
    var reportSaveExtension: MutableState<ResultWriter.FileExtensions> = mutableStateOf(ResultWriter.FileExtensions.XLSX)

    @Serializable(with = MutableStateSerializer::class)
    var hideOnMinimize: MutableState<Boolean> = mutableStateOf(true)

    @Serializable(with = MutableStateSerializer::class)
    var debugMode: MutableState<Boolean> = mutableStateOf(false)

    @Serializable(with = MutableStateSerializer::class)
    var firstMigration = mutableStateOf(true)

    @Serializable(with = MutableStateSerializer::class)
    var eulaAgreedVersion = mutableStateOf(0)


    constructor() {
        try {
            val prop: AppSettings = Json.decodeFromString(settingsFile.readText())

            this.threadCount = mutableStateOf(
                if (prop.threadCount.value > Runtime.getRuntime().availableProcessors())
                    max(
                        Runtime.getRuntime().availableProcessors(),
                        1
                    )
                else
                    prop.threadCount.value
            )

            this.theme = prop.theme
            this.language = prop.language
            this.hideOnMinimize = prop.hideOnMinimize
            this.reportSaveExtension = prop.reportSaveExtension
            this.debugMode = prop.debugMode
            this.firstMigration = prop.firstMigration
            this.eulaAgreedVersion = prop.eulaAgreedVersion
        } catch (_: Exception) {
            logger.error {
                "Failed to load app settings. Loading defaults."
            }
        }
    }

    fun save () {
        settingsFile.writeText(Json.encodeToString(this))
    }
}