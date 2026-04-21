package org.angryscan.app.serializers

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import kotlinx.serialization.serializer
import org.angryscan.app.scan.common.connectors.*
import org.angryscan.app.scan.common.files.types.IFileType
import org.angryscan.app.scan.functions.CertDetectFun
import org.angryscan.app.scan.functions.CodeDetectFun
import org.angryscan.app.scan.functions.RKNDomainDetectFun
import org.angryscan.app.scan.functions.UnknownDetectFun
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import org.angryscan.common.engine.custom.CustomEngine
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.engine.kotlin.KotlinEngine
import org.angryscan.common.matchers.*
import org.angryscan.gitleaks.matcher.GitleaksMatcher

val PolymorphicSerializationModule = SerializersModule {
    polymorphic(IMatcher::class) {
        subclass(Address::class)
        subclass(BankAccount::class)
        subclass(BankAccountLE::class)
        subclass(Birthday::class)
        subclass(CadastralNumber::class)
        subclass(CardNumber::class)
        subclass(CVV::class)
        subclass(CryptoWallet::class)
        subclass(CryptoSeedPhrase::class)
        subclass(DeathDate::class)
        subclass(DriverLicense::class)
        subclass(EducationDoc::class)
        subclass(EducationLevel::class)
        subclass(EducationLicense::class)
        subclass(Email::class)
        subclass(ExecDocNumber::class)
        subclass(RIN::class)
        subclass(FullName::class)
        subclass(FullNameUS::class)
        subclass(Geo::class)
        subclass(HashData::class)
        subclass(IdentityDocType::class)
        subclass(INN::class)
        subclass(IPv4::class)
        subclass(IPv6::class)
        subclass(LegalEntityId::class)
        subclass(LegalEntityName::class)
        subclass(Login::class)
        subclass(MaritalStatus::class)
        subclass(Certificate::class)
        subclass(MedicareUS::class)
        subclass(MilitaryID::class)
        subclass(MilitaryRank::class)
        subclass(OGRNIP::class)
        subclass(OKPO::class)
        subclass(OMS::class)
        subclass(OSAGOPolicy::class)
        subclass(Passport::class)
        subclass(PassportUS::class)
        subclass(Password::class)
        subclass(Phone::class)
        subclass(PhoneUS::class)
        subclass(ResidencePermit::class)
        subclass(SberBook::class)
        subclass(SecurityAffiliation::class)
        subclass(SNILS::class)
        subclass(SocialUserId::class)
        subclass(SSN::class)
        subclass(StateRegContract::class)
        subclass(VIN::class)
        subclass(VehicleRegNumber::class)
        subclass(EIN::class)
        subclass(ITIN::class)
        subclass(RTN::class)
        subclass(DriverLicenseUS::class)
        subclass(VisaNumberUS::class)
        subclass(AlienRegistrationNumber::class)
        subclass(USCIS::class)
        subclass(SEVISID::class)
        subclass(DODID::class)
        subclass(APOFPODPO::class)
        subclass(NSN::class)
        subclass(TCN::class)
        subclass(NPI::class)
        subclass(AddressUS::class)
        subclass(UserSignature::class)
        subclass(CertDetectFun::class)
        subclass(CodeDetectFun::class)
        subclass(RKNDomainDetectFun::class)
        subclass(GitleaksMatcher::class)

        defaultDeserializer { _ -> serializer<UnknownDetectFun>() }
    }
    polymorphic(IConnector::class) {
        subclass(ConnectorS3::class)
        subclass(ConnectorFileShare::class)
        subclass(ConnectorHTTP::class)
        subclass(ConnectorAIModels::class)
        subclass(ConnectorPostgres::class)
        subclass(ConnectorMySQL::class)
        subclass(ConnectorSqlite::class)
        subclass(ConnectorGreenPlum::class)
        subclass(ConnectorHive::class)
        subclass(ConnectorCockroachDB::class)
        subclass(ConnectorClickHouse::class)
    }
    polymorphic(IScanEngine::class) {
        subclass(KotlinEngine::class)
        subclass(HyperScanEngine::class)
        subclass(CustomEngine::class)
    }
    contextual(IFileType::class, IFileTypeSerializer)
}

val PolymorphicFormatter = Json {
    prettyPrint = false
    serializersModule = PolymorphicSerializationModule
}