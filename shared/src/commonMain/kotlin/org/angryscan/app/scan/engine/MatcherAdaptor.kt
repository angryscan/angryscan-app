package org.angryscan.app.scan.engine

import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.custom.ICustomMatcher
import org.angryscan.common.engine.hyperscan.IHyperMatcher
import org.angryscan.common.engine.kotlin.IKotlinMatcher

fun List<IMatcher>.toHyperScanMatchers(): List<IHyperMatcher> {
    return this.filterIsInstance<IHyperMatcher>()
}

fun List<IMatcher>.notHyperScanMatchers(): List<IMatcher> {
    return this.filterNot { it in this.toHyperScanMatchers() }
}

fun List<IMatcher>.toKotlinMatchers(): List<IKotlinMatcher> {
    return this.filterIsInstance<IKotlinMatcher>()
}

fun List<IMatcher>.notKotlinMatchers(): List<IMatcher> {
    return this.filterNot { it in this.toKotlinMatchers() }
}

fun List<IMatcher>.toCustomMatchers(): List<ICustomMatcher> {
    return this.filterIsInstance<ICustomMatcher>()
}

fun List<IMatcher>.notCustomMatchers(): List<IMatcher> {
    return this.filterNot { it in this.toCustomMatchers() }
}

