package org.angryscan.app.common

import androidx.compose.runtime.mutableStateListOf
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.angryscan.common.matchers.UserSignature
import java.io.File

@Serializable
class UserSignatureSettings : KoinComponent {
    @Transient
    private val logger = KotlinLogging.logger { }

    class SettingsFile(path: String) : File(path)

    private val settingsFile: SettingsFile by inject()

    @Serializable
    val userSignatures: MutableList<UserSignature> = mutableStateListOf()

    constructor() {
        try {
            val prop: UserSignatureSettings = Json.decodeFromString(settingsFile.readText())
            this.userSignatures.addAll(prop.userSignatures)
        } catch (_: Exception) {
            logger.error {
                "Failed to load User signatures. Save example."
            }
            userSignatures.clear()
            userSignatures.addAll(
                listOf(
                    UserSignature(
                        name = "Valuable Info",
                        searchSignatures = mutableListOf(
                            "СЕКРЕТ",
                            "КОНФИДЕНЦИАЛЬН",
                            "КОМПЕНСАЦ",
                            "КОММЕРЧ",
                            "ТАЙНА",
                            "КЛЮЧ",
                            "ШИФР",
                            "PIN",
                            "SECRET",
                            "PRIVACY",
                            "ДЕТАЛИ ПЛАТЕЖА",
                            "НАЗНАЧЕНИЕ ПЛАТЕЖА",
                            "DETAILS OF PAYMENT",
                            "PAYMENT DETAILS",
                            "БЕЗОПАСНОСТ",
                            "ВНУТРИБАНК",
                            "ФСБ",
                            "ФЕДЕРАЛ",
                            "ФСО",
                            "РАЗВЕДК",
                            "НАЦИОНАЛЬН",
                            "ГВАРДИ",
                            "МИНИСТЕРСТВО",
                            "МВД",
                            "ОБОРОН",
                            "МЧС",
                            "ПРЕМЬЕР",
                            "VIP",
                            "МВС",
                            "МВК",
                            "СКУД",
                            "ИНКАССАЦИЯ",
                            "ГОСУДАРСТВ"
                        )
                    )
                )
            )
            save()
        }
    }

    fun save() {
        settingsFile.writeText(Json.encodeToString(this))
    }
}