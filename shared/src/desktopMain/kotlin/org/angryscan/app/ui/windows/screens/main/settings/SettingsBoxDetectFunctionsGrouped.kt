package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.*
import org.angryscan.app.scan.functions.CertDetectFun
import org.angryscan.app.scan.functions.CodeDetectFun
import org.angryscan.app.ui.strings.composableName
import org.angryscan.app.ui.windows.screens.main.settings.items.*
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.matchers.*
import org.angryscan.gitleaks.matcher.GitleaksMatcher
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsBoxDetectFunctionsGrouped(scanSettings: ScanSettings) {
    val detectFunctions = remember { scanSettings.matchers }
    var selectedCountry by remember { mutableStateOf(MatcherCountry.ALL) }
    val currentEngine by scanSettings.engine

    LaunchedEffect(detectFunctions) { scanSettings.save() }
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
        personalDataNumbersName, personalDataTextName, bankingSecrecyName, itAssetsName, cryptoName
    ) {
        listOf(
            MatchersGroup(name = personalDataNumbersName, matchers = listOf(
                Phone, PhoneUS, SNILS, SSN, Passport, PassportUS, OMS, INN, Birthday, DeathDate,
                DriverLicense, RIN, MilitaryID, ResidencePermit, SberBook, SocialUserId, VIN,
                VehicleRegNumber, LegalEntityId, OGRNIP, OKPO, StateRegContract, ExecDocNumber,
                CadastralNumber, MedicareUS, OSAGOPolicy, EIN, ITIN, DriverLicenseUS, VisaNumberUS,
                AlienRegistrationNumber, USCIS, SEVISID, DODID, NSN, TCN, NPI, APOFPODPO
            )),
            MatchersGroup(name = personalDataTextName, matchers = listOf(
                FullName, FullNameUS, Email, Address, Login, Password, Certificate, EducationDoc,
                EducationLevel, EducationLicense, IdentityDocType, MaritalStatus, MilitaryRank,
                SecurityAffiliation, Geo, LegalEntityName, AddressUS
            )),
            MatchersGroup(name = bankingSecrecyName, matchers = listOf(CardNumber(), CVV, BankAccount, BankAccountLE, RTN)),
            MatchersGroup(name = itAssetsName, matchers = listOf(IPv4, IPv6, CodeDetectFun, CertDetectFun, HashData, GitleaksMatcher)),
            MatchersGroup(name = cryptoName, matchers = listOf(CryptoWallet, CryptoSeedPhrase))
        )
    }

    val availableMatchers = remember(matchersGroups, currentEngine) {
        val all = matchersGroups.flatMap { it.matchers }
        if (currentEngine == HyperScanEngine::class) all.filter { it::class != CryptoSeedPhrase::class } else all
    }
    val filteredGroups = remember(matchersGroups, selectedCountry, currentEngine) {
        val groups = MatcherCountryMapping.filterGroups(matchersGroups, selectedCountry)
        if (currentEngine == HyperScanEngine::class) {
            groups.map { g -> if (g.name == cryptoName) g.copy(matchers = g.matchers.filter { it::class != CryptoSeedPhrase::class }) else g }.filter { it.matchers.isNotEmpty() }
        } else groups
    }
    val countryMatchers = remember(selectedCountry, availableMatchers) {
        MatcherCountryMapping.filterMatchers(availableMatchers, selectedCountry)
    }
    val isAllSelected = countryMatchers.all { m -> detectFunctions.any { it::class == m::class } }

    SettingsSectionCard(title = stringResource(Res.string.ScanSettings_DetectFunctions)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CountryFilterChips(
                selectedCountry = selectedCountry,
                onCountrySelected = { selectedCountry = it },
                modifier = Modifier.weight(1f),
                getCountryStats = { country ->
                    val total = if (country == MatcherCountry.ALL) {
                        availableMatchers.size
                    } else {
                        availableMatchers.count { MatcherCountryMapping.getCountry(it) == country }
                    }
                    val selected = if (country == MatcherCountry.ALL) {
                        detectFunctions.size
                    } else {
                        detectFunctions.count { MatcherCountryMapping.getCountry(it) == country }
                    }
                    Pair(selected, total)
                }
            )
            SelectAllOrDiscardAllText(
                allSelected = isAllSelected,
                onClick = {
                    if (isAllSelected) {
                        countryMatchers.forEach { m -> scanSettings.matchers.removeAll { it::class == m::class } }
                    } else {
                        scanSettings.matchers.addAll(countryMatchers.filter { m -> !detectFunctions.any { it::class == m::class } })
                    }
                    scanSettings.save()
                }
            )
        }

        filteredGroups.forEach { group ->
            val groupMatchers = group.matchers
            val groupAllSelected = groupMatchers.all { m -> scanSettings.matchers.any { it::class == m::class } }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SelectAllOrDiscardAllText(
                    allSelected = groupAllSelected,
                    onClick = {
                        if (groupAllSelected) {
                            groupMatchers.forEach { m -> scanSettings.matchers.removeAll { it::class == m::class } }
                        } else {
                            scanSettings.matchers.addAll(groupMatchers.filter { m -> !scanSettings.matchers.any { it::class == m::class } })
                        }
                        scanSettings.save()
                    }
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (matcher in group.matchers) {
                    val selected = scanSettings.matchers.any { it::class == matcher::class }
                    FilterChip(
                        selected = selected,
                        onClick = {
                            if (selected) scanSettings.matchers.removeAll { it::class == matcher::class }
                            else scanSettings.matchers.add(matcher)
                            scanSettings.save()
                        },
                        label = { Text(text = matcher.composableName(), fontSize = 13.sp) }
                    )
                }
            }
        }
    }
}
