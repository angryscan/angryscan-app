package org.angryscan.app.types

import IKoinTestRule
import org.angryscan.app.scan.common.files.types.DOCXType
import kotlin.test.Test

class DOCXTypeTest: IKoinTestRule {
    @Test
    fun findLocation() {
        val fileName = "first/first.docx"

        CheckLocation.checkByMap(
            fileName,
            DOCXType::findLocation
        )
    }
}