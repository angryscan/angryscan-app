package org.angryscan.app.common

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.engine.kotlin.KotlinEngine
import org.angryscan.common.extensions.Matchers
import org.angryscan.common.matchers.UserSignature
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.angryscan.app.scan.common.files.FileType
import org.angryscan.app.scan.functions.RKNDomainDetectFun
import org.angryscan.app.serializers.MutableStateKClassSerializer
import org.angryscan.app.serializers.MutableStateSerializer
import org.angryscan.app.serializers.PolymorphicFormatter
import org.angryscan.app.ui.components.SelectionTypes
import java.io.File
import kotlin.reflect.KClass

@Serializable
class ScanSettings : KoinComponent {
    @Transient
    private val logger = KotlinLogging.logger {}

    class SettingsFile(path: String) : File(path)

    private val settingsFile: SettingsFile by inject()

    @Serializable
    val extensions: MutableList<FileType> = mutableStateListOf()

    @Serializable(with = MutableStateSerializer::class)
    var extensionsSettingsExpanded: MutableState<Boolean>

    @Serializable
    val matchers: MutableList<IMatcher> = mutableStateListOf()

    @Serializable(with = MutableStateSerializer::class)
    var matchersSettingsExpanded: MutableState<Boolean>

    @Serializable
    val userSignatures: MutableList<UserSignature> = mutableStateListOf()

    @Serializable(with = MutableStateSerializer::class)
    var userSignatureSettingsExpanded: MutableState<Boolean>

    @Serializable(with = MutableStateSerializer::class)
    var selectionType: MutableState<SelectionTypes>

    @Serializable(with = MutableStateSerializer::class)
    var fastScan: MutableState<Boolean>
    val sampleLength = 10_000
    val sampleCount = 100

    @Serializable(with = MutableStateKClassSerializer::class)
    var engine: MutableState<KClass<out IScanEngine>>

    constructor() {
        val userSignatureSettings by inject<UserSignatureSettings>()
        try {
            val prop: ScanSettings = PolymorphicFormatter.decodeFromString(settingsFile.readText())

            this.extensions.addAll(prop.extensions)
            this.extensionsSettingsExpanded = prop.extensionsSettingsExpanded

            this.fastScan = prop.fastScan

            this.matchers.addAll(prop.matchers.distinct())
            this.matchersSettingsExpanded = prop.matchersSettingsExpanded

            this.userSignatures.addAll(prop.userSignatures.filter { it in userSignatureSettings.userSignatures })
            this.userSignatureSettingsExpanded = prop.userSignatureSettingsExpanded
            this.selectionType = prop.selectionType
            this.engine = prop.engine
        } catch (_: Exception) {
            logger.error {
                "Failed to load ScanSettings. Loading default."
            }
            this.extensions.clear()
            this.extensions.addAll(FileType.entries.filter {
                it != FileType.ZIP &&
                        it != FileType.RAR &&
                        it != FileType.CERT &&
                        it != FileType.CODE
            })
            this.extensionsSettingsExpanded = mutableStateOf(false)
            this.matchers.clear()
            this.matchers.addAll(Matchers)
            this.matchers.add(RKNDomainDetectFun)
            this.matchersSettingsExpanded = mutableStateOf(false)
            this.fastScan = mutableStateOf(false)
            this.userSignatureSettingsExpanded = mutableStateOf(false)
            this.selectionType = mutableStateOf(SelectionTypes.Folder)
            this.engine = mutableStateOf(
                when(OS.currentOS()) {
                    OS.WINDOWS -> KotlinEngine::class
                    else -> HyperScanEngine::class
                }

            )
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