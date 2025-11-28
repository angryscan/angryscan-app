package org.angryscan.app.scan.common.files.types

import kotlinx.serialization.Serializable
import org.angryscan.app.common.ScanSettings
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

@Serializable
sealed class FileType : IFileType, KoinComponent {
    val scanSettings: ScanSettings by inject()

    init {
        values += this
    }

    fun isSampleOverload(sample: Int, fastScan: Boolean): Boolean {
        return (fastScan && sample >= scanSettings.sampleCount)
    }

    fun isSampleOverload(sample: Int, fastScan: Boolean, isActive: Boolean): Boolean {
        if (!isActive) return true
        return isSampleOverload(sample, fastScan)
    }

    fun isLengthOverload(length: Int): Boolean {
        return (length >= scanSettings.sampleLength)
    }

    fun isLengthOverload(length: Int, isActive: Boolean): Boolean {
        if (!isActive) return true
        return (isLengthOverload(length))
    }

    fun selectedExtension(fileName: String): Boolean =
        values
            .filter {
                scanSettings.extensions.contains(it) // TODO: Заменить на загруженные из задачи, а не из текущих настроек
            }.flatMap {
                it.extensions
            }.any { fileName.endsWith(it) }

    companion object {
        var values = listOf<FileType>()
            private set
    }
}