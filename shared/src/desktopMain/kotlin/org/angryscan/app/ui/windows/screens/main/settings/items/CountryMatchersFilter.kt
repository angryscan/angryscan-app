package org.angryscan.app.ui.windows.screens.main.settings.items

import androidx.compose.runtime.Composable
import org.angryscan.app.resources.*
import org.angryscan.app.scan.functions.CertDetectFun
import org.angryscan.app.scan.functions.CodeDetectFun
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.matchers.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.reflect.KClass

enum class MatcherCountry(
    val stringResource: StringResource,
    val shortStringResource: StringResource? = null,
    val flag: String
) {
    ALL(Res.string.MatcherCountry_All, null, "🌐"),
    RUSSIA(Res.string.MatcherCountry_Russia, null, "🇷🇺"),
    USA(Res.string.MatcherCountry_USA, null, "🇺🇸"),
    CHINA(Res.string.MatcherCountry_China, null, "🇨🇳"),
    INTERNATIONAL(Res.string.MatcherCountry_International, Res.string.MatcherCountry_International_Short, "🌍");
}

@Composable
fun MatcherCountry.getLocalizedName(useShort: Boolean = false): String {
    val resource = if (useShort && shortStringResource != null) {
        shortStringResource
    } else {
        stringResource
    }
    return stringResource(resource)
}

object MatcherCountryMapping {
    private val countryMap: Map<KClass<out IMatcher>, MatcherCountry> = mapOf(
        // Russia
        Phone::class to MatcherCountry.RUSSIA,
        SNILS::class to MatcherCountry.RUSSIA,
        Passport::class to MatcherCountry.RUSSIA,
        OMS::class to MatcherCountry.RUSSIA,
        INN::class to MatcherCountry.RUSSIA,
        DriverLicense::class to MatcherCountry.RUSSIA,
        MilitaryID::class to MatcherCountry.RUSSIA,
        ResidencePermit::class to MatcherCountry.RUSSIA,
        SberBook::class to MatcherCountry.RUSSIA,
        OGRNIP::class to MatcherCountry.RUSSIA,
        OKPO::class to MatcherCountry.RUSSIA,
        OSAGOPolicy::class to MatcherCountry.RUSSIA,
        FullName::class to MatcherCountry.RUSSIA,
        Address::class to MatcherCountry.RUSSIA,
        EducationDoc::class to MatcherCountry.RUSSIA,
        EducationLevel::class to MatcherCountry.RUSSIA,
        EducationLicense::class to MatcherCountry.RUSSIA,
        IdentityDocType::class to MatcherCountry.RUSSIA,
        MaritalStatus::class to MatcherCountry.RUSSIA,
        Certificate::class to MatcherCountry.RUSSIA,
        MilitaryRank::class to MatcherCountry.RUSSIA,
        SecurityAffiliation::class to MatcherCountry.RUSSIA,
        VehicleRegNumber::class to MatcherCountry.RUSSIA,
        LegalEntityName::class to MatcherCountry.RUSSIA,
        StateRegContract::class to MatcherCountry.RUSSIA,
        ExecDocNumber::class to MatcherCountry.RUSSIA,
        CadastralNumber::class to MatcherCountry.RUSSIA,
        BankAccount::class to MatcherCountry.RUSSIA,
        BankAccountLE::class to MatcherCountry.RUSSIA,
//        RKNDomainDetectFun::class to MatcherCountry.RUSSIA,
        DeathDate::class to MatcherCountry.RUSSIA,

        // USA
        PhoneUS::class to MatcherCountry.USA,
        SSN::class to MatcherCountry.USA,
        PassportUS::class to MatcherCountry.USA,
        MedicareUS::class to MatcherCountry.USA,
        FullNameUS::class to MatcherCountry.USA,

        // China
        RIN::class to MatcherCountry.CHINA,

        // International
        Email::class to MatcherCountry.INTERNATIONAL,
        Login::class to MatcherCountry.INTERNATIONAL,
        Password::class to MatcherCountry.INTERNATIONAL,
        CardNumber::class to MatcherCountry.INTERNATIONAL,
        CVV::class to MatcherCountry.INTERNATIONAL,
        Birthday::class to MatcherCountry.INTERNATIONAL,
        SocialUserId::class to MatcherCountry.INTERNATIONAL,
        VIN::class to MatcherCountry.INTERNATIONAL,
        Geo::class to MatcherCountry.INTERNATIONAL,
        IPv4::class to MatcherCountry.INTERNATIONAL,
        IPv6::class to MatcherCountry.INTERNATIONAL,
        CodeDetectFun::class to MatcherCountry.INTERNATIONAL,
        CertDetectFun::class to MatcherCountry.INTERNATIONAL,
        HashData::class to MatcherCountry.INTERNATIONAL,
        LegalEntityId::class to MatcherCountry.INTERNATIONAL,
        CryptoWallet::class to MatcherCountry.INTERNATIONAL,
        CryptoSeedPhrase::class to MatcherCountry.INTERNATIONAL,
    )

    fun getCountry(matcher: IMatcher): MatcherCountry {
        return countryMap[matcher::class] ?: MatcherCountry.INTERNATIONAL
    }

    fun filterMatchers(matchers: List<IMatcher>, country: MatcherCountry): List<IMatcher> {
        if (country == MatcherCountry.ALL) return matchers
        return matchers.filter { getCountry(it) == country }
    }

    fun filterGroups(groups: List<MatchersGroup>, country: MatcherCountry): List<MatchersGroup> {
        if (country == MatcherCountry.ALL) return groups

        return groups.mapNotNull { group ->
            val filteredMatchers = filterMatchers(group.matchers, country)
            if (filteredMatchers.isNotEmpty()) {
                group.copy(matchers = filteredMatchers)
            } else {
                null
            }
        }
    }
}