package org.angryscan.app.scan.functions

import kotlinx.serialization.Serializable
import org.angryscan.common.engine.IMatcher

@Serializable
data class UnknownDetectFun(override val name: String = "Unknown") : IMatcher {
    override fun check(value: String) = false
}