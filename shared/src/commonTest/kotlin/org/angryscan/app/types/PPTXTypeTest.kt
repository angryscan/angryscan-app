package org.angryscan.app.types

import IKoinTestRule
import org.angryscan.app.scan.common.files.types.PPTXType
import kotlin.test.Test

class PPTXTypeTest : IKoinTestRule {
    @Test
    fun findLocation() {
        val fileName = "first/first.pptx"

        CheckLocation.checkByMap(
            fileName,
            PPTXType::findLocation
        )
    }
}