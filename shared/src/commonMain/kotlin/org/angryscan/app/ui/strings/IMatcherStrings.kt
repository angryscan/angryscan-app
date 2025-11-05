package org.angryscan.app.ui.strings

import androidx.compose.runtime.Composable
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.matchers.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.angryscan.app.resources.*
import org.angryscan.app.scan.functions.CertDetectFun
import org.angryscan.app.scan.functions.CodeDetectFun
import org.angryscan.app.scan.functions.RKNDomainDetectFun

fun getMatcherNameResource(matcher: IMatcher): StringResource? {
    return when (matcher) {
        is FullName -> Res.string.Matcher_Name
        is Email -> Res.string.Matcher_Emails
        is Phone -> Res.string.Matcher_Phones
        is CardNumber -> Res.string.Matcher_CardNumbers
        is CarNumber -> Res.string.Matcher_CarNumber
        is SNILS -> Res.string.Matcher_SNILS
        is Passport -> Res.string.Matcher_Passport
        is OMS -> Res.string.Matcher_OMS
        is INN -> Res.string.Matcher_INN
        is AccountNumber -> Res.string.Matcher_AccountNumber
        is Address -> Res.string.Matcher_Address
        is Login -> Res.string.Matcher_Login
        is Password -> Res.string.Matcher_Password
        is CVV -> Res.string.Matcher_CVV
        is IPv4 -> Res.string.Matcher_IP
        is IPv6 -> Res.string.Matcher_IPv6
        is CodeDetectFun -> Res.string.Matcher_Code
        is CertDetectFun -> Res.string.Matcher_Cert
        is RKNDomainDetectFun -> Res.string.Matcher_DetectBlockedDomains
        else -> null
    }
}

fun getMatcherDescriptionResource(matcher: IMatcher): StringResource? {
    return when (matcher) {
        is FullName -> Res.string.Matcher_Description_Name
        is Email -> Res.string.Matcher_Description_Emails
        is Phone -> Res.string.Matcher_Description_Phones
        is CardNumber -> Res.string.Matcher_Description_CardNumbers
        is CarNumber -> Res.string.Matcher_Description_CarNumber
        is SNILS -> Res.string.Matcher_Description_SNILS
        is Passport -> Res.string.Matcher_Description_Passport
        is OMS -> Res.string.Matcher_Description_OMS
        is INN -> Res.string.Matcher_Description_INN
        is AccountNumber -> Res.string.Matcher_Description_AccountNumber
        is Address -> Res.string.Matcher_Description_Address
        is Login -> Res.string.Matcher_Description_Login
        is Password -> Res.string.Matcher_Description_Password
        is CVV -> Res.string.Matcher_Description_CVV
        is IPv4 -> Res.string.Matcher_Description_IP
        is IPv6 -> Res.string.Matcher_Description_IPv6
        is CodeDetectFun -> Res.string.Matcher_Description_Code
        is CertDetectFun -> Res.string.Matcher_Description_Cert
        is RKNDomainDetectFun -> Res.string.Matcher_Description_DetectBlockedDomains
        else -> null
    }
}

@Composable
fun IMatcher.composableName(): String {
    return getMatcherNameResource(this)?.let { stringResource(it) } ?: this.name
}

@Composable
fun IMatcher.description(): String {
    return getMatcherDescriptionResource(this)?.let { stringResource(it) } ?: this.name
}

suspend fun IMatcher.readableName(): String {
    return getMatcherNameResource(this)?.let { getString(it) } ?: this.name
}