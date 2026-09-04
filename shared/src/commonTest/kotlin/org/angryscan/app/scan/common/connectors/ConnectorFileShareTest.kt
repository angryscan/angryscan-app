package org.angryscan.app.scan.common.connectors

import kotlinx.coroutines.runBlocking
import org.angryscan.app.scan.common.files.types.TextType
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ConnectorFileShareTest {
    @Test
    fun `scanDirectory skips non-existent paths`() = runBlocking {
        val connector = ConnectorFileShare()
        val found = mutableListOf<FoundedFile>()
        val counter = connector.scanDirectory(
            dir = File("/tmp/definitely-missing-angryscan-path-12345.csv").absolutePath,
            extensions = listOf(TextType),
            fileSelected = { found.add(it) }
        )
        assertTrue(found.isEmpty())
        assertEquals(0L, counter.objectCount)
    }

    @Test
    fun `scanDirectory registers existing csv as TextType`() = runBlocking {
        val dir = createTempDirectory(prefix = "fileshare-csv-").toFile()
        try {
            val csv = File(dir, "sample.csv").apply { writeText("a,b\n1,2\n") }
            val connector = ConnectorFileShare()
            val found = mutableListOf<FoundedFile>()
            val counter = connector.scanDirectory(
                dir = csv.absolutePath,
                extensions = listOf(TextType),
                fileSelected = { found.add(it) }
            )
            assertEquals(1, found.size)
            assertEquals(csv.absolutePath, found.single().path)
            assertEquals(1L, counter.objectCount)
        } finally {
            dir.deleteRecursively()
        }
    }
}
