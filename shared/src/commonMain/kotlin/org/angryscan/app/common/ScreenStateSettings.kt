package org.angryscan.app.common

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import org.angryscan.app.scan.common.files.types.IFileType
import org.angryscan.app.serializers.MutableStateSerializer
import org.angryscan.app.serializers.PolymorphicFormatter
import org.angryscan.app.ui.components.SelectionTypes
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.matchers.UserSignature
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

@Serializable
class ScreenStateSettings : KoinComponent {
    @Transient
    private val logger = KotlinLogging.logger {}

    class SettingsFile(path: String) : File(path)

    private val settingsFile: SettingsFile by inject()

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
    data class AIModelsScreenState(
        var path: String = ""
    )

    var fileShareScreenState = FileShareScreenState()
    var s3ScreenState = S3ScreenState()
    var httpScreenState = HTTPScreenState()
    var aimodelsScreenState = AIModelsScreenState()

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

                // Restore AIModelsScreen state
                this.aimodelsScreenState.path = prop.aimodelsScreenState.path
            }
        } catch (e: Exception) {
            logger.error(e) {
                "Failed to load ScreenStateSettings. Loading defaults."
            }
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

