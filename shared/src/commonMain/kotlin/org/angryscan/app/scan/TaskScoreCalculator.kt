package org.angryscan.app.scan

import org.angryscan.app.scan.functions.CertDetectFun
import org.angryscan.app.scan.functions.CodeDetectFun
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.matchers.CardNumber
import org.angryscan.common.matchers.FullName

private fun attributeWeight(matcher: IMatcher): Float = when (matcher) {
    is FullName -> 5f
    is CardNumber -> 30f
    is CodeDetectFun -> 0.01f
    is CertDetectFun -> 100f
    else -> 1f
}

fun calculateTaskScore(foundAttributes: Map<IMatcher, Int>): Long {
    if (foundAttributes.isEmpty()) return 0L
    val hasFullName = foundAttributes.keys.any { it is FullName }
    val basePerMatcher = if (hasFullName) 20L else (foundAttributes.size - 1).coerceAtLeast(0).toLong()
    return foundAttributes.entries.sumOf { (matcher, count) ->
        basePerMatcher + (attributeWeight(matcher) * count).toLong()
    }
}
