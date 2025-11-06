package org.angryscan.app.scan.common

class FileSize(): Comparable<FileSize> {
    private var bytes: Long = 0
    private var kBytes: Long = 0
    private var mBytes: Long = 0
    private var gBytes: Long = 0
    private var tBytes: Long = 0

    constructor(size: Long) : this() {
        plus(size)
    }

    operator fun plus(incrementBytes: Long): FileSize {
        bytes += incrementBytes

        kBytes += bytes / 1024
        bytes %= 1024

        mBytes += kBytes / 1024
        kBytes %= 1024

        gBytes += mBytes / 1024
        mBytes %= 1024

        tBytes += gBytes / 1024
        gBytes %= 1024

        return this
    }

    operator fun plus(incrementBytes: FileSize): FileSize {
        bytes += incrementBytes.bytes

        kBytes += incrementBytes.kBytes + (bytes / 1024)
        bytes %= 1024

        mBytes += incrementBytes.mBytes + (kBytes / 1024)
        kBytes %= 1024

        gBytes += incrementBytes.gBytes + (mBytes / 1024)
        mBytes %= 1024

        tBytes += incrementBytes.tBytes + (gBytes / 1024)
        gBytes %= 1024

        return this
    }

    override fun compareTo(other: FileSize): Int {
        if(this.tBytes > other.tBytes) return 1
        if(this.tBytes < other.tBytes) return -1
        if(this.gBytes > other.gBytes) return 1
        if(this.gBytes < other.gBytes) return -1
        if(this.mBytes > other.mBytes) return 1
        if(this.mBytes < other.mBytes) return -1
        if(this.kBytes > other.kBytes) return 1
        if(this.kBytes < other.kBytes) return -1
        if(this.bytes > other.bytes) return 1
        if(this.bytes < other.bytes) return -1
        return 0
    }

    override fun toString(): String {
        return when {
            tBytes != 0L -> "${tBytes}.${(gBytes * 100) / 1024} TB"
            gBytes != 0L -> "${gBytes}.${(mBytes * 100) / 1024} GB"
            mBytes != 0L -> "${mBytes}.${(kBytes * 100) / 1024} MB"
            kBytes != 0L -> "${kBytes}.${(bytes * 100) / 1024} KB"
            else -> "$bytes B"
        }
    }

}