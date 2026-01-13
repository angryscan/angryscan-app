package org.angryscan.app.common

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.angryscan.app.scan.common.files.types.*
import org.angryscan.app.serializers.MutableStateKClassSerializer
import org.angryscan.app.serializers.MutableStateSerializer
import org.angryscan.app.serializers.PolymorphicFormatter
import org.angryscan.app.ui.components.SelectionTypes
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.engine.kotlin.KotlinEngine
import org.angryscan.common.extensions.Matchers
import org.angryscan.common.matchers.UserSignature
import org.angryscan.gitleaks.matcher.GitleaksMatcher
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import kotlin.reflect.KClass

@Serializable
class ScanSettings : KoinComponent {
    @Transient
    private val logger = KotlinLogging.logger {}

    class SettingsFile(path: String) : File(path)

    private val settingsFile: SettingsFile by inject()

    @Serializable
    val extensions: MutableList<IFileType> = mutableStateListOf()

    @Serializable(with = MutableStateSerializer::class)
    var extensionsSettingsExpanded: MutableState<Boolean> = mutableStateOf(false)

    @Serializable
    val matchers: MutableList<IMatcher> = mutableStateListOf()

    @Serializable(with = MutableStateSerializer::class)
    var matchersSettingsExpanded: MutableState<Boolean> = mutableStateOf(false)

    @Serializable
    val userSignatures: MutableList<UserSignature> = mutableStateListOf()

    @Serializable(with = MutableStateSerializer::class)
    var userSignatureSettingsExpanded: MutableState<Boolean> = mutableStateOf(false)

    @Transient
    var mainScreenSettingsExpanded: MutableState<Boolean> = mutableStateOf(false)

    @Serializable(with = MutableStateSerializer::class)
    var selectionType: MutableState<SelectionTypes> = mutableStateOf(SelectionTypes.Folder)

    @Serializable(with = MutableStateSerializer::class)
    var fastScan: MutableState<Boolean> = mutableStateOf(false)
    val sampleLength = 10_000
    val sampleCount = 100

    @Serializable(with = MutableStateKClassSerializer::class)
    var engine: MutableState<KClass<out IScanEngine>> = mutableStateOf(
        when (OS.currentOS()) {
            OS.WINDOWS -> KotlinEngine::class
            else -> HyperScanEngine::class
        }
    )

    constructor() {
        reload()
    }

    /**
     * Reload scan settings from disk, and re-bind selected user signatures to the current
     * [UserSignatureSettings] definitions by name.
     */
    fun reload() {
        val userSignatureSettings by inject<UserSignatureSettings>()
        try {
            val prop: ScanSettings = PolymorphicFormatter.decodeFromString(settingsFile.readText())

            this.extensions.clear()
            this.extensions.addAll(prop.extensions)
            this.extensionsSettingsExpanded.value = prop.extensionsSettingsExpanded.value

            this.fastScan.value = prop.fastScan.value

            this.matchers.clear()
            this.matchers.addAll(prop.matchers.distinct())
            this.matchersSettingsExpanded.value = prop.matchersSettingsExpanded.value

            val defsByName = userSignatureSettings.userSignatures.associateBy { it.name }
            this.userSignatures.clear()
            this.userSignatures.addAll(
                prop.userSignatures.mapNotNull { defsByName[it.name] }
            )
            this.userSignatureSettingsExpanded.value = prop.userSignatureSettingsExpanded.value
            this.selectionType.value = prop.selectionType.value
            this.engine.value = prop.engine.value

            GitleaksMatcher.close()
            GitleaksMatcher.init()
        } catch (_: Exception) {
            logger.error { "Failed to load ScanSettings. Loading default." }
            this.extensions.clear()
            this.extensions.addAll(
                IFileType.getAll().filter {
                    it !in CertFileType.entries +
                            CodeFileType.entries
                }
            )
            this.extensionsSettingsExpanded.value = false
            this.matchers.clear()
            this.matchers.addAll(Matchers)
            this.matchersSettingsExpanded.value = false
            this.fastScan.value = false
            this.userSignatureSettingsExpanded.value = false
            this.selectionType.value = SelectionTypes.Folder
            this.engine.value = when (OS.currentOS()) {
                OS.WINDOWS -> KotlinEngine::class
                else -> HyperScanEngine::class
            }
            this.userSignatures.clear()

            GitleaksMatcher.close()
            GitleaksMatcher.init()
        }
    }

    fun save() {
        try {
            settingsFile.writeText(PolymorphicFormatter.encodeToString(this))
        } catch (_: Exception) {
            logger.error {
                "Failed to save ScanSettings."
            }
        }
    }
}