package ru.packetdima.datascanner.ui.strings

import androidx.compose.runtime.Composable
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.matchers.*
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.StringResource
import ru.packetdima.datascanner.resources.Res
import ru.packetdima.datascanner.scan.functions.CertDetectFun
import ru.packetdima.datascanner.scan.functions.CodeDetectFun
import ru.packetdima.datascanner.scan.functions.RKNDomainDetectFun
import org.angryscan.common.matchers.UserSignature
import ru.packetdima.datascanner.resources.*
import kotlin.reflect.KClass

private val matcherResources: Map<KClass<out IMatcher>, Pair<StringResource, StringResource>> = buildMap {
    // Base matchers
    put(AccountNumber::class, Res.string.Matcher_AccountNumber to Res.string.Matcher_Description_AccountNumber)
    put(Address::class, Res.string.Matcher_Address to Res.string.Matcher_Description_Address)
    put(BankAccount::class, Res.string.Matcher_BankAccount to Res.string.Matcher_Description_BankAccount)
    put(BankAccountLE::class, Res.string.Matcher_BankAccountLE to Res.string.Matcher_Description_BankAccountLE)
    put(BirthCert::class, Res.string.Matcher_BirthCert to Res.string.Matcher_Description_BirthCert)
    put(Birthday::class, Res.string.Matcher_Birthday to Res.string.Matcher_Description_Birthday)
    put(CadastralNumber::class, Res.string.Matcher_CadastralNumber to Res.string.Matcher_Description_CadastralNumber)
    put(CardNumber::class, Res.string.Matcher_CardNumbers to Res.string.Matcher_Description_CardNumbers)
    put(CarNumber::class, Res.string.Matcher_CarNumber to Res.string.Matcher_Description_CarNumber)
    put(CVV::class, Res.string.Matcher_CVV to Res.string.Matcher_Description_CVV)
    put(DeathDate::class, Res.string.Matcher_DeathDate to Res.string.Matcher_Description_DeathDate)
    put(DriverLicense::class, Res.string.Matcher_DriverLicense to Res.string.Matcher_Description_DriverLicense)
    put(EducationDoc::class, Res.string.Matcher_EducationDoc to Res.string.Matcher_Description_EducationDoc)
    put(EducationLevel::class, Res.string.Matcher_EducationLevel to Res.string.Matcher_Description_EducationLevel)
    put(EducationLicense::class, Res.string.Matcher_EducationLicense to Res.string.Matcher_Description_EducationLicense)
    put(Email::class, Res.string.Matcher_Emails to Res.string.Matcher_Description_Emails)
    put(EpCertificateNumber::class, Res.string.Matcher_EpCertificateNumber to Res.string.Matcher_Description_EpCertificateNumber)
    put(ExecDocNumber::class, Res.string.Matcher_ExecDocNumber to Res.string.Matcher_Description_ExecDocNumber)
    put(ForeignPassports::class, Res.string.Matcher_ForeignPassport to Res.string.Matcher_Description_ForeignPassport)
    put(ForeignTIN::class, Res.string.Matcher_ForeignTIN to Res.string.Matcher_Description_ForeignTIN)
    put(FullName::class, Res.string.Matcher_Name to Res.string.Matcher_Description_Name)
    put(Geo::class, Res.string.Matcher_Geo to Res.string.Matcher_Description_Geo)
    put(HashData::class, Res.string.Matcher_HashData to Res.string.Matcher_Description_HashData)
    put(IdentityDocType::class, Res.string.Matcher_IdentityDocType to Res.string.Matcher_Description_IdentityDocType)
    put(INN::class, Res.string.Matcher_INN to Res.string.Matcher_Description_INN)
    put(InheritanceDoc::class, Res.string.Matcher_InheritanceDoc to Res.string.Matcher_Description_InheritanceDoc)
    put(IPv4::class, Res.string.Matcher_IP to Res.string.Matcher_Description_IP)
    put(IPv6::class, Res.string.Matcher_IPv6 to Res.string.Matcher_Description_IPv6)
    put(LegalEntityId::class, Res.string.Matcher_LegalEntityId to Res.string.Matcher_Description_LegalEntityId)
    put(LegalEntityName::class, Res.string.Matcher_LegalEntityName to Res.string.Matcher_Description_LegalEntityName)
    put(Login::class, Res.string.Matcher_Login to Res.string.Matcher_Description_Login)
    put(MaritalStatus::class, Res.string.Matcher_MaritalStatus to Res.string.Matcher_Description_MaritalStatus)
    put(MarriageCert::class, Res.string.Matcher_MarriageCert to Res.string.Matcher_Description_MarriageCert)
    put(MilitaryID::class, Res.string.Matcher_MilitaryID to Res.string.Matcher_Description_MilitaryID)
    put(MilitaryRank::class, Res.string.Matcher_MilitaryRank to Res.string.Matcher_Description_MilitaryRank)
    put(OGRNIP::class, Res.string.Matcher_OGRNIP to Res.string.Matcher_Description_OGRNIP)
    put(OKPO::class, Res.string.Matcher_OKPO to Res.string.Matcher_Description_OKPO)
    put(OMS::class, Res.string.Matcher_OMS to Res.string.Matcher_Description_OMS)
    put(OSAGOPolicy::class, Res.string.Matcher_OSAGOPolicy to Res.string.Matcher_Description_OSAGOPolicy)
    put(Passport::class, Res.string.Matcher_Passport to Res.string.Matcher_Description_Passport)
    put(Password::class, Res.string.Matcher_Password to Res.string.Matcher_Description_Password)
    put(Phone::class, Res.string.Matcher_Phones to Res.string.Matcher_Description_Phones)
    put(RefugeeCert::class, Res.string.Matcher_RefugeeCert to Res.string.Matcher_Description_RefugeeCert)
    put(ResidencePermit::class, Res.string.Matcher_ResidencePermit to Res.string.Matcher_Description_ResidencePermit)
    put(SberBook::class, Res.string.Matcher_SberBook to Res.string.Matcher_Description_SberBook)
    put(SecurityAffiliation::class, Res.string.Matcher_SecurityAffiliation to Res.string.Matcher_Description_SecurityAffiliation)
    put(SNILS::class, Res.string.Matcher_SNILS to Res.string.Matcher_Description_SNILS)
    put(SocialUserId::class, Res.string.Matcher_SocialUserId to Res.string.Matcher_Description_SocialUserId)
    put(StateRegContract::class, Res.string.Matcher_StateRegContract to Res.string.Matcher_Description_StateRegContract)
    put(TemporaryID::class, Res.string.Matcher_TemporaryID to Res.string.Matcher_Description_TemporaryID)
    put(UidContractBankBki::class, Res.string.Matcher_UidContractBank to Res.string.Matcher_Description_UidContractBank)
    put(VIN::class, Res.string.Matcher_VIN to Res.string.Matcher_Description_VIN)
    put(VehicleRegNumber::class, Res.string.Matcher_VehicleRegNumber to Res.string.Matcher_Description_VehicleRegNumber)

    // Extension detectors
    put(CertDetectFun::class, Res.string.Matcher_Cert to Res.string.Matcher_Description_Cert)
    put(CodeDetectFun::class, Res.string.Matcher_Code to Res.string.Matcher_Description_Code)
    put(RKNDomainDetectFun::class, Res.string.Matcher_DetectBlockedDomains to Res.string.Matcher_Description_DetectBlockedDomains)
    
    // User signatures
    put(UserSignature::class, Res.string.Matcher_UserSignature_Title to Res.string.Matcher_UserSignature_Title)
}

suspend fun IMatcher.readableName(): String {
    return matcherResources[this::class]?.first?.let { getString(it) }
        ?: this.name
}

@Composable
fun IMatcher.composableName(): String {
    return matcherResources[this::class]?.first?.let { stringResource(it) }
        ?: this.name
}

@Composable
fun IMatcher.description(): String {
    return matcherResources[this::class]?.second?.let { stringResource(it) }
        ?: this.name
}