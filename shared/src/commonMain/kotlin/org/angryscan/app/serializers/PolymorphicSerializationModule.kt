package org.angryscan.app.serializers

import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.engine.IScanEngine
import org.angryscan.common.engine.custom.CustomEngine
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.engine.kotlin.KotlinEngine
import org.angryscan.common.matchers.*
import org.angryscan.app.scan.common.connectors.ConnectorFileShare
import org.angryscan.app.scan.common.connectors.ConnectorHTTP
import org.angryscan.app.scan.common.connectors.ConnectorS3
import org.angryscan.app.scan.common.connectors.IConnector
import org.angryscan.app.scan.functions.CertDetectFun
import org.angryscan.app.scan.functions.CodeDetectFun
import org.angryscan.app.scan.functions.RKNDomainDetectFun

val PolymorphicSerializationModule = SerializersModule {
    polymorphic(IMatcher::class) {
        subclass(AccountNumber::class)
        subclass(Address::class)
        subclass(CardNumber::class)
        subclass(CarNumber::class)
        subclass(CVV::class)
        subclass(Email::class)
        subclass(FullName::class)
        subclass(INN::class)
        subclass(IPv4::class)
        subclass(IPv6::class)
        subclass(Login::class)
        subclass(OMS::class)
        subclass(Passport::class)
        subclass(Password::class)
        subclass(Phone::class)
        subclass(SNILS::class)
        subclass(UserSignature::class)
        subclass(CertDetectFun::class)
        subclass(CodeDetectFun::class)
        subclass(RKNDomainDetectFun::class)
    }
    polymorphic(IConnector::class) {
        subclass(ConnectorS3::class)
        subclass(ConnectorFileShare::class)
        subclass(ConnectorHTTP::class)
    }
    polymorphic(IScanEngine::class) {
        subclass(KotlinEngine::class)
        subclass(HyperScanEngine::class)
        subclass(CustomEngine::class)
    }
}

val PolymorphicFormatter = Json {
    prettyPrint = false
    serializersModule = PolymorphicSerializationModule
}