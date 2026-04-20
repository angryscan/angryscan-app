package org.angryscan.app.ui

import org.angryscan.app.common.ScanSettings
import org.angryscan.app.scan.functions.CertDetectFun
import org.angryscan.app.scan.functions.CodeDetectFun
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.matchers.*
import org.angryscan.gitleaks.matcher.GitleaksMatcher
import kotlin.reflect.KClass

/**
 * Validation result for scan settings
 */
data class ValidationResult(
    val isValid: Boolean,
    val errorTitle: String? = null,
    val errorMessage: String? = null
)

private val visibleMatcherClasses: Set<KClass<out IMatcher>> = setOf(
    FullName::class, FullNameUS::class, Email::class,
    Address::class, AddressUS::class, Geo::class, CadastralNumber::class,
    Birthday::class, DeathDate::class,
    Phone::class, PhoneUS::class,
    Passport::class, PassportUS::class, IdentityDocType::class,
    DriverLicense::class, DriverLicenseUS::class, VIN::class, VehicleRegNumber::class, OSAGOPolicy::class,
    INN::class, RIN::class, EIN::class, ITIN::class,
    SNILS::class, SSN::class,
    ResidencePermit::class, VisaNumberUS::class, AlienRegistrationNumber::class, USCIS::class, SEVISID::class,
    MilitaryID::class, MilitaryRank::class, DODID::class, NSN::class, TCN::class, APOFPODPO::class, SecurityAffiliation::class,
    OMS::class, MedicareUS::class, NPI::class,
    EducationDoc::class, EducationLevel::class, EducationLicense::class,
    LegalEntityId::class, LegalEntityName::class, OGRNIP::class, OKPO::class, StateRegContract::class, ExecDocNumber::class, SocialUserId::class,
    BankAccount::class, BankAccountLE::class, RTN::class, SberBook::class,
    CardNumber::class, CVV::class,
    Login::class, Password::class, IPv4::class, IPv6::class, CodeDetectFun::class, CertDetectFun::class, Certificate::class, HashData::class,
    GitleaksMatcher::class, CryptoWallet::class, CryptoSeedPhrase::class,
)

fun hasSelectedMatchersForScan(scanSettings: ScanSettings): Boolean =
    scanSettings.matchers.any { it::class in visibleMatcherClasses }

/**
 * Validates scan settings (extensions and matchers)
 * @param scanSettings The scan settings to validate
 * @param noExtensionsTitle Title for error when no extensions selected
 * @param noExtensionsMessage Message for error when no extensions selected
 * @param noMatchersTitle Title for error when no matchers selected
 * @param noMatchersMessage Message for error when no matchers selected
 * @return ValidationResult with validation status and error messages if invalid
 */
fun validateScanSettings(
    scanSettings: ScanSettings,
    noExtensionsTitle: String,
    noExtensionsMessage: String,
    noMatchersTitle: String,
    noMatchersMessage: String
): ValidationResult {
    // Validate extensions
    if (scanSettings.extensions.isEmpty()) {
        return ValidationResult(
            isValid = false,
            errorTitle = noExtensionsTitle,
            errorMessage = noExtensionsMessage
        )
    }
    
    // Validate matchers visible in "choose data to find"
    if (!hasSelectedMatchersForScan(scanSettings)) {
        return ValidationResult(
            isValid = false,
            errorTitle = noMatchersTitle,
            errorMessage = noMatchersMessage
        )
    }
    
    return ValidationResult(isValid = true)
}

