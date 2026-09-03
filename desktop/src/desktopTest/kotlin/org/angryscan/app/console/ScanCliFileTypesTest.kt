package org.angryscan.app.console

import org.angryscan.app.console.commands.ScanCliFileTypes
import org.angryscan.app.scan.common.files.types.CertFileType
import org.angryscan.app.scan.common.files.types.CodeFileType
import org.angryscan.app.scan.common.files.types.IFileType
import org.angryscan.app.scan.common.files.types.PDFType
import org.angryscan.app.scan.common.files.types.TextType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScanCliFileTypesTest {
    @Test
    fun `selectable types exclude Code and Cert entries`() {
        val selectable = ScanCliFileTypes.selectableFileTypes()
        val excluded = CertFileType.entries + CodeFileType.entries

        assertTrue(selectable.isNotEmpty())
        assertTrue(selectable.none { it in excluded })
        assertTrue(IFileType.getAll().filter { it in excluded }.isNotEmpty())
    }

    @Test
    fun `resolveExtension accepts regular document type names`() {
        assertEquals(PDFType, ScanCliFileTypes.resolveExtension("PDF"))
        assertEquals(TextType, ScanCliFileTypes.resolveExtension("Text"))
    }

    @Test
    fun `resolveExtension is case insensitive`() {
        assertEquals(PDFType, ScanCliFileTypes.resolveExtension("pdf"))
        assertEquals(PDFType, ScanCliFileTypes.resolveExtension("Pdf"))
        assertEquals(TextType, ScanCliFileTypes.resolveExtension("text"))
    }

    @Test
    fun `resolveExtension accepts names with underscores for spaces`() {
        val withSpaces = ScanCliFileTypes.selectableFileTypes()
            .firstOrNull { it.name.contains(' ') }
            ?: return

        val arg = withSpaces.name.replace(' ', '_')
        assertEquals(withSpaces, ScanCliFileTypes.resolveExtension(arg))
        assertEquals(withSpaces, ScanCliFileTypes.resolveExtension(arg.lowercase()))
    }

    @Test
    fun `resolveExtension rejects Code and Cert type names`() {
        val codeName = CodeFileType.entries.first().name.replace(' ', '_')
        val certName = CertFileType.entries.first().name.replace(' ', '_')

        assertFailsWith<IllegalArgumentException> {
            ScanCliFileTypes.resolveExtension(codeName)
        }
        assertFailsWith<IllegalArgumentException> {
            ScanCliFileTypes.resolveExtension(certName)
        }
    }

    @Test
    fun `resolveExtension rejects unknown names`() {
        assertFailsWith<IllegalArgumentException> {
            ScanCliFileTypes.resolveExtension("Definitely_Not_A_Type")
        }
    }

    @Test
    fun `buggy inverted filter must not be used for CLI resolution`() {
        // Documents the historical bug: filterNot { it !in Cert+Code } kept only Code/Cert.
        val inverted = IFileType.getAll().filterNot {
            it !in (CertFileType.entries + CodeFileType.entries)
        }
        assertTrue(inverted.isNotEmpty())
        assertTrue(inverted.all { it is CodeFileType || it is CertFileType })
        assertFalse(ScanCliFileTypes.selectableFileTypes().any { it in inverted })
    }
}
