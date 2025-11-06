package org.angryscan.app.scan.functions

import kotlinx.serialization.Serializable
import org.angryscan.common.engine.ExpressionOption
import org.angryscan.common.engine.hyperscan.IHyperMatcher
import org.angryscan.common.engine.kotlin.IKotlinMatcher
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.angryscan.app.scan.functions.rkn.DomainRepository

@Serializable
object RKNDomainDetectFun: IHyperMatcher, IKotlinMatcher, KoinComponent {
    val domainRepo by inject<DomainRepository>()

    override val name = "RKN Blocked Domain"
    override fun check(value: String) = domainRepo.checkDomain(value)

    override val hyperPatterns = listOf(
        """(?:https?\s)?([\w.-]+\.[a-z]{2,})(\s|$)"""
    )
    override val expressionOptions = setOf(
        ExpressionOption.MULTILINE,
    )
    override val javaPatterns = listOf(
        """(?<=https?\s)?([\w.-]+\.[a-z]{2,})(?=\s|$)"""
    )
    override val regexOptions = setOf(
        RegexOption.MULTILINE
    )
}