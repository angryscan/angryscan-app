package org.angryscan.app.scan.common

class ObjectCounter {
    var objectCount = 0L
    var objectSize= FileSize()

    fun add(incrementBytes: Long): ObjectCounter {
        objectCount ++
        objectSize += incrementBytes
        return this
    }

    operator fun plus(other: ObjectCounter): ObjectCounter {
        objectCount += other.objectCount
        objectSize += other.objectSize
        return this
    }
}