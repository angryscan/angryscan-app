package org.angryscan.app.scan.functions

import kotlinx.serialization.Serializable
import org.angryscan.common.engine.Match
import org.angryscan.common.engine.custom.ICustomMatcher

@Serializable
object CodeDetectFun: ICustomMatcher {
    override val name: String = "Code Detect"
    override fun check(value: String) = true

    override fun scan(text: String): List<Match>  = listOf()
}