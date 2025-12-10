package org.angryscan.app.scan.common.files.types

import kotlinx.serialization.Serializable
import org.angryscan.app.scan.common.Document
import org.angryscan.app.serializers.IFileTypeSerializer
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import java.io.File
import kotlin.coroutines.CoroutineContext

@Serializable(with = IFileTypeSerializer::class)
sealed interface IFileType {
    val name: String
    val extensions: List<String>
    suspend fun scanFile(
        file: File,
        context: CoroutineContext,
        engines: List<IScanEngine>,
        fastScan: Boolean
    ): Document

    fun scan(text: String, engine: IScanEngine): Map<IMatcher, Int> {
        return engine
            .scan(text)
            .groupBy { it.matcher }
            .map { it.key to it.value.size }
            .toMap()
    }

    companion object {
        fun getAll(): List<IFileType> {
            return buildList {
                IFileType::class.sealedSubclasses.forEach { subclass ->
                    when {
                        // Enum классы (CertFileType, CodeFileType)
                        subclass.java.isEnum -> {
                            @Suppress("UNCHECKED_CAST")
                            val enumClass = subclass.java as Class<out Enum<*>>
                            val valuesMethod = enumClass.getMethod("values")
                            val enumValues = valuesMethod.invoke(null) as Array<*>
                            addAll(enumValues.filterIsInstance<IFileType>())
                        }
                        // Sealed классы (FileType) - получаем все их sealed подклассы
                        subclass.java.isSealed -> {
                            subclass.sealedSubclasses.forEach { sealedSubclass ->
                                sealedSubclass.objectInstance?.let { add(it) }
                            }
                        }
                    }
                }
            }
        }

        /**
         * Found IFileType by file extension
         */
        fun getFileType(file: File): IFileType? {
            val extension = file.extension.lowercase()
            return getAll().find { fileType ->
                fileType.extensions.any { it.lowercase() == extension }
            }
        }

        /**
         * Found IFileType by file extension
         */
        fun getFileType(filePath: String): IFileType? =
            getFileType(File(filePath))
    }
}