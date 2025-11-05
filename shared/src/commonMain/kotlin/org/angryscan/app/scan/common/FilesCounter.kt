package org.angryscan.app.scan.common

class FilesCounter {
    var filesCount = 0L
    var filesSize= FileSize()

    fun add(incrementBytes: Long): FilesCounter {
        filesCount ++
        filesSize += incrementBytes
        return this
    }

    operator fun plus(other: FilesCounter): FilesCounter {
        filesCount += other.filesCount
        filesSize += other.filesSize
        return this
    }
}