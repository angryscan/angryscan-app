package org.angryscan.app.scan.common.files.types

import kotlinx.serialization.Serializable
import org.angryscan.app.common.ScanSettings
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

@Serializable
sealed class FileType : IFileType, KoinComponent {
    val scanSettings: ScanSettings by inject()

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

    fun selectedExtension(fileName: String, selectedExtension: List<IFileType>): Boolean =
        selectedExtension.flatMap {
            it.extensions
        }.any { fileName.endsWith(it) }

    companion object {
    }

    override fun toString(): String {
        return name
    }
}