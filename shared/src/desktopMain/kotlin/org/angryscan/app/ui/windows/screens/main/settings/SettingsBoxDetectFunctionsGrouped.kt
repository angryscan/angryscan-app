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
import org.angryscan.app.scan.functions.RKNDomainDetectFun
import org.angryscan.app.ui.windows.screens.main.settings.items.*
import org.angryscan.common.matchers.*
import org.jetbrains.compose.resources.stringResource


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBoxDetectFunctionsGrouped(
    scanSettings: ScanSettings
) {
    val detectFunctions = remember { scanSettings.matchers }
    var expanded by remember { scanSettings.matchersSettingsExpanded }
    var selectedCountry by remember { mutableStateOf(MatcherCountry.ALL) }

    LaunchedEffect(detectFunctions, expanded) {
        scanSettings.save()
    }

    val personalDataNumbersName = stringResource(Res.string.DetectGroup_PersonalDataNumbers)
    val personalDataTextName = stringResource(Res.string.DetectGroup_PersonalDataText)
    val bankingSecrecyName = stringResource(Res.string.DetectGroup_BankingSecrecy)
    val itAssetsName = stringResource(Res.string.DetectGroup_ITAssets)

    val matchersGroups = remember(
        personalDataNumbersName,
        personalDataTextName,
        bankingSecrecyName,
        itAssetsName
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
                    TemporaryID,
                    ResidencePermit,
                    SberBook,
                    SocialUserId,
                    VIN,
                    VehicleRegNumber,
                    LegalEntityId,
                    OGRNIP,
                    OKPO,
                    StateRegContract,
                    EpCertificateNumber,
                    ExecDocNumber,
                    CadastralNumber,
                    MedicareUS,
                    OSAGOPolicy
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
                    BirthCert,
                    EducationDoc,
                    EducationLevel,
                    EducationLicense,
                    IdentityDocType,
                    InheritanceDoc,
                    MaritalStatus,
                    MarriageCert,
                    MilitaryRank,
                    SecurityAffiliation,
                    Geo,
                    LegalEntityName
                )
            ),
            MatchersGroup(
                name = bankingSecrecyName,
                matchers = listOf(
                    CardNumber(),
                    CVV,
                    BankAccount,
                    BankAccountLE,
                    UidContractBankBki
                )
            ),
            MatchersGroup(
                name = itAssetsName,
                matchers = listOf(
                    IPv4,
                    IPv6,
                    CodeDetectFun,
                    CertDetectFun,
                    RKNDomainDetectFun,
                    HashData
                )
            )
        )
    }

    SettingsBoxSpan(
        text = stringResource(Res.string.ScanSettings_DetectFunctions),
        expanded = expanded,
        onExpandClick = {
            expanded = !expanded
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
                modifier = Modifier.padding(bottom = 4.dp)
            )

            MinimalSelectAllButton(
                scanSettings = scanSettings,
                selectedCountry = selectedCountry
            )

            val filteredGroups = remember(matchersGroups, selectedCountry) {
                MatcherCountryMapping.filterGroups(matchersGroups, selectedCountry)
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



