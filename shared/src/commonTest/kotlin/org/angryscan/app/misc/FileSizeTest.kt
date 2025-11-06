package org.angryscan.app.misc

import org.angryscan.app.scan.common.FileSize
import kotlin.test.Test
import kotlin.test.assertEquals

internal class FileSizeTest {

    @Test
    fun fileSizeTest() {
        val fs = FileSize()
        fs.plus(24)
        assertEquals("24 B", fs.toString())
        fs.plus(1000)
        assertEquals("1.0 KB", fs.toString())
        for (i in 1..1024)
            fs.plus(1024)
        assertEquals("1.0 MB", fs.toString())
        for (i in 1..1024)
            for (j in 1..1024)
                fs.plus(1024)
        assertEquals("1.0 GB", fs.toString())
    }
}