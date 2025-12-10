package org.angryscan.app.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.serializer
import org.angryscan.app.scan.common.files.types.CertFileType
import org.angryscan.app.scan.common.files.types.CodeFileType
import org.angryscan.app.scan.common.files.types.DOCType
import org.angryscan.app.scan.common.files.types.DOCXType
import org.angryscan.app.scan.common.files.types.FileType
import org.angryscan.app.scan.common.files.types.IFileType
import org.angryscan.app.scan.common.files.types.ODPType
import org.angryscan.app.scan.common.files.types.ODSType
import org.angryscan.app.scan.common.files.types.ODTType
import org.angryscan.app.scan.common.files.types.PDFType
import org.angryscan.app.scan.common.files.types.PPTType
import org.angryscan.app.scan.common.files.types.PPTXType
import org.angryscan.app.scan.common.files.types.RARType
import org.angryscan.app.scan.common.files.types.TextType
import org.angryscan.app.scan.common.files.types.XLSType
import org.angryscan.app.scan.common.files.types.XLSXType
import org.angryscan.app.scan.common.files.types.ZIPType

// Create a separate module for polymorphic serialization of FileType to avoid circular dependencies
private val FileTypePolymorphicModule = SerializersModule {
    polymorphic(FileType::class) {
        subclass(TextType::class)
        subclass(PDFType::class)
        subclass(DOCType::class)
        subclass(DOCXType::class)
        subclass(XLSType::class)
        subclass(XLSXType::class)
        subclass(PPTType::class)
        subclass(PPTXType::class)
        subclass(ODTType::class)
        subclass(ODSType::class)
        subclass(ODPType::class)
        subclass(ZIPType::class)
        subclass(RARType::class)
    }
}

private val FileTypeJson = Json {
    serializersModule = FileTypePolymorphicModule
}

/**
 * Custom serializer for IFileType that handles enums separately from sealed classes
 */
object IFileTypeSerializer : KSerializer<IFileType> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("IFileType") {
        element<String>("type")
        element<String>("value")
    }

    override fun serialize(encoder: Encoder, value: IFileType) {
        require(encoder is JsonEncoder)

        when (value) {
            is CertFileType -> {
                // Serialize enum directly
                encoder.encodeJsonElement(
                    buildJsonObject {
                        put("type", "CertFileType")
                        put("value", value.name)
                    }
                )
            }
            is CodeFileType -> {
                // Serialize enum directly
                encoder.encodeJsonElement(
                    buildJsonObject {
                        put("type", "CodeFileType")
                        put("value", value.name)
                    }
                )
            }
            is FileType -> {
                val jsonElement = FileTypeJson.encodeToJsonElement(serializer<FileType>(), value)
                encoder.encodeJsonElement(jsonElement)
            }
        }
    }

    override fun deserialize(decoder: Decoder): IFileType {
        require(decoder is JsonDecoder)
        val jsonElement = decoder.decodeJsonElement()
        
        // Check if this is a JsonObject (our custom format for enum or polymorphic format for FileType)
        if (jsonElement is JsonObject) {
            val jsonObject = jsonElement.jsonObject
            
            // Check if there is a "value" field - this means it's our custom format for enum
            val value = jsonObject["value"]?.jsonPrimitive?.content
            val type = jsonObject["type"]?.jsonPrimitive?.content
            
            return when {
                type == "CertFileType" && value != null -> {
                    CertFileType.valueOf(value)
                }
                type == "CodeFileType" && value != null -> {
                    CodeFileType.valueOf(value)
                }
                else -> {
                    // Use polymorphic deserialization for sealed classes through the module
                    FileTypeJson.decodeFromJsonElement(serializer<FileType>(), jsonElement)
                }
            }
        } else {
            // If this is not a JsonObject (e.g., JsonArray or JsonPrimitive), try to use polymorphic deserialization
            return FileTypeJson.decodeFromJsonElement(serializer<FileType>(), jsonElement)
        }
    }
}

