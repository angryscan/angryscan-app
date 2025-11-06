package org.angryscan.app.scan.functions

import kotlinx.serialization.Serializable
import org.angryscan.common.engine.ExpressionOption
import org.angryscan.common.engine.hyperscan.IHyperMatcher
import org.angryscan.common.engine.kotlin.IKotlinMatcher

@Serializable
object CertDetectFun : IKotlinMatcher, IHyperMatcher {
    override val name: String = "Cert Detect"
    override fun check(value: String) = true

    override val javaPatterns = listOf(
        """(---BEGIN CERTIFICATE)|(---BEGIN PKCS7)|(---BEGIN.*?KEY)"""
    )
    override val regexOptions: Set<RegexOption> = setOf()
    override val hyperPatterns = listOf(
        """(---BEGIN CERTIFICATE)|(---BEGIN PKCS7)|(---BEGIN.*?KEY)"""
    )
    override val expressionOptions: Set<ExpressionOption> = setOf()
}