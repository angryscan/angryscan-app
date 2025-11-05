package org.angryscan.app.serializers

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.reflect.KClass


object MutableStateKClassSerializer : KSerializer<MutableState<KClass<*>>> {
    // Сериализатор для значения внутри MutableState
    private val valueSerializer = KClassAsStringSerializer

    override val descriptor: SerialDescriptor = valueSerializer.descriptor

    override fun serialize(encoder: Encoder, value: MutableState<KClass<*>>) {
        // Сериализуем только текущее значение состояния
        valueSerializer.serialize(encoder, value.value)
    }

    override fun deserialize(decoder: Decoder): MutableState<KClass<*>> {
        // Десериализуем значение и создаем новое состояние
        val kClass = valueSerializer.deserialize(decoder)
        return mutableStateOf(kClass)
    }
}

object KClassAsStringSerializer : KSerializer<KClass<*>> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("KClass", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: KClass<*>) {
        val className = value.qualifiedName ?:
        throw IllegalArgumentException("Cannot serialize KClass without qualified name: $value")
        encoder.encodeString(className)
    }

    override fun deserialize(decoder: Decoder): KClass<*> {
        val className = decoder.decodeString()
        return try {
            // Для JVM используем Class.forName
            Class.forName(className).kotlin
        } catch (e: ClassNotFoundException) {
            throw IllegalArgumentException("Class not found: $className", e)
        }
    }
}