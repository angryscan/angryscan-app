package org.angryscan.app.types

import IKoinTestRule
import org.angryscan.app.scan.common.files.types.DOCType
import kotlin.test.Test

class DOCTypeTest : IKoinTestRule {
    @Test
    fun findLocation() {
        val fileName = "first/first.doc"

        CheckLocation.checkByMap(
            fileName = fileName,
            DOCType::findLocation
        )
    }
}