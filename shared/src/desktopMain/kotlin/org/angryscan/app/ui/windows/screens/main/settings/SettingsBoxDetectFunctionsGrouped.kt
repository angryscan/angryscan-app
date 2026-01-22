package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.*
import org.angryscan.app.scan.functions.CertDetectFun
import org.angryscan.app.scan.functions.CodeDetectFun
import org.angryscan.app.ui.windows.screens.main.settings.items.*
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.matchers.*
import org.angryscan.gitleaks.matcher.GitleaksMatcher
import org.jetbrains.compose.resources.stringResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBoxDetectFunctionsGrouped(
    scanSettings: ScanSettings
) {
    val detectFunctions = remember { scanSettings.matchers }
    val expandedState = scanSettings.matchersSettingsExpanded
    val expanded by expandedState
    var selectedCountry by remember { mutableStateOf(MatcherCountry.ALL) }
    val currentEngine by scanSettings.engine

    LaunchedEffect(detectFunctions, expanded) {
        scanSettings.save()
    }

    // Remove CryptoSeedPhrase from selected matchers if HyperScan engine is selected
    LaunchedEffect(currentEngine) {
        if (currentEngine == HyperScanEngine::class) {
            scanSettings.matchers.removeIf { it::class == CryptoSeedPhrase::class }
            scanSettings.save()
        }
    }

    val personalDataNumbersName = stringResource(Res.string.DetectGroup_PersonalDataNumbers)
    val personalDataTextName = stringResource(Res.string.DetectGroup_PersonalDataText)
    val bankingSecrecyName = stringResource(Res.string.DetectGroup_BankingSecrecy)
    val itAssetsName = stringResource(Res.string.DetectGroup_ITAssets)
    val cryptoName = stringResource(Res.string.DetectGroup_Crypto)

    val matchersGroups = remember(
        personalDataNumbersName,
        personalDataTextName,
        bankingSecrecyName,
        itAssetsName,
        cryptoName
    ) {
        listOf(
            MatchersGroup(
                name = personalDataNumbersName,
                matchers = listOf(
                    Phone,
                    PhoneUS,
                    SNILS,
                    SSN,
                    Passport,
                    PassportUS,
                    OMS,
                    INN,
                    Birthday,
                    DeathDate,
                    DriverLicense,
                    RIN,
                    MilitaryID,
                    ResidencePermit,
                    SberBook,
                    SocialUserId,
                    VIN,
                    VehicleRegNumber,
                    LegalEntityId,
                    OGRNIP,
                    OKPO,
                    StateRegContract,
                    ExecDocNumber,
                    CadastralNumber,
                    MedicareUS,
                    OSAGOPolicy,
                    EIN,
                    ITIN,
                    DriverLicenseUS,
                    VisaNumberUS,
                    AlienRegistrationNumber,
                    USCIS,
                    SEVISID,
                    DODID,
                    NSN,
                    TCN,
                    NPI,
                    APOFPODPO
                )
            ),
            MatchersGroup(
                name = personalDataTextName,
                matchers = listOf(
                    FullName,
                    FullNameUS,
                    Email,
                    Address,
                    Login,
                    Password,
                    Certificate,
                    EducationDoc,
                    EducationLevel,
                    EducationLicense,
                    IdentityDocType,
                    MaritalStatus,
                    MilitaryRank,
                    SecurityAffiliation,
                    Geo,
                    LegalEntityName,
                    AddressUS
                )
            ),
            MatchersGroup(
                name = bankingSecrecyName,
                matchers = listOf(
                    CardNumber(),
                    CVV,
                    BankAccount,
                    BankAccountLE,
                    RTN
                )
            ),
            MatchersGroup(
                name = itAssetsName,
                matchers = listOf(
                    IPv4,
                    IPv6,
                    CodeDetectFun,
                    CertDetectFun,
//                    RKNDomainDetectFun,
                    HashData,
                    GitleaksMatcher,
                )
            ),
            MatchersGroup(
                name = cryptoName,
                matchers = listOf(
                    CryptoWallet,
                    CryptoSeedPhrase
                )
            )
        )
    }

    SettingsBoxSpan(
        text = stringResource(Res.string.ScanSettings_DetectFunctions),
        expanded = expanded,
        onExpandClick = {
            expandedState.value = !expandedState.value
            scanSettings.save()
        }
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            CountryFilterChips(
                selectedCountry = selectedCountry,
                onCountrySelected = { country ->
                    selectedCountry = country
                },
                modifier = Modifier.padding(bottom = 4.dp),
                getCountryStats = { country ->
                    val allMatchers = matchersGroups.flatMap { it.matchers }
                    // Filter out CryptoSeedPhrase if HyperScan engine is selected
                    val filteredMatchers = if (currentEngine == HyperScanEngine::class) {
                        allMatchers.filter { it::class != CryptoSeedPhrase::class }
                    } else {
                        allMatchers
                    }
                    val countryMatchers = MatcherCountryMapping.filterMatchers(
                        filteredMatchers,
                        country
                    )
                    val selectedInCountry = countryMatchers.count { matcher ->
                        detectFunctions.any { it::class == matcher::class }
                    }
                    selectedInCountry to countryMatchers.size
                }
            )

            // Get all available matchers from groups (before country filtering) for Select All button
            val availableMatchers = remember(matchersGroups, currentEngine) {
                val allMatchers = matchersGroups.flatMap { it.matchers }
                // Filter out CryptoSeedPhrase if HyperScan engine is selected
                if (currentEngine == HyperScanEngine::class) {
                    allMatchers.filter { it::class != CryptoSeedPhrase::class }
                } else {
                    allMatchers
                }
            }

            MinimalSelectAllButton(
                scanSettings = scanSettings,
                selectedCountry = selectedCountry,
                availableMatchers = availableMatchers
            )

            val filteredGroups = remember(matchersGroups, selectedCountry, currentEngine) {
                val groups = MatcherCountryMapping.filterGroups(matchersGroups, selectedCountry)
                // Filter out CryptoSeedPhrase if HyperScan engine is selected
                if (currentEngine == HyperScanEngine::class) {
                    groups.map { group ->
                        if (group.name == cryptoName) {
                            group.copy(
                                matchers = group.matchers.filter { it::class != CryptoSeedPhrase::class }
                            )
                        } else {
                            group
                        }
                    }.filter { it.matchers.isNotEmpty() }
                } else {
                    groups
                }
            }

            filteredGroups.forEach { group ->
                MinimalDetectionGroupCard(
                    group = group,
                    scanSettings = scanSettings
                )
            }
        }
    }
}



