@file:Suppress("OVERRIDE_DEPRECATION")

package org.angryscan.app.common

import org.angryscan.common.engine.IMatcher
import org.angryscan.common.extensions.Matchers
import org.angryscan.app.scan.functions.CertDetectFun
import org.angryscan.app.scan.functions.CodeDetectFun
import org.angryscan.app.scan.functions.RKNDomainDetectFun
import java.util.function.IntFunction

object MatchersRegister: List<IMatcher> by Matchers.toList() + listOf(
    RKNDomainDetectFun,
    CodeDetectFun,
    CertDetectFun
) {
    override fun <T : Any?> toArray(generator: IntFunction<Array<out T?>?>): Array<out T?>? {
        return super.toArray(generator)
    }

    override fun contains(element: IMatcher): Boolean {
        return this.any{
            it::class == element::class
        }
    }
}