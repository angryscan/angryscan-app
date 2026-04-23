package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.angryscan.app.common.AppSettings
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.*
import org.angryscan.app.scan.functions.CertDetectFun
import org.angryscan.app.scan.functions.CodeDetectFun
import org.angryscan.app.ui.strings.composableName
import org.angryscan.app.ui.strings.readableNameForLocale
import org.angryscan.app.ui.windows.components.MatcherTooltip
import org.angryscan.app.ui.windows.screens.main.LocalMainScreenAdaptiveTokens
import org.angryscan.app.ui.windows.screens.main.settings.items.*
import org.angryscan.common.engine.hyperscan.HyperScanEngine
import org.angryscan.common.matchers.*
import org.angryscan.gitleaks.matcher.GitleaksMatcher
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsBoxDetectFunctionsGrouped(
    scanSettings: ScanSettings,
    showTitle: Boolean = true,
    unifiedBlock: Boolean = false,
    errorHighlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    val detectFunctions = remember { scanSettings.matchers }
    val appSettings = koinInject<AppSettings>()
    val initialCountry = remember {
        when (appSettings.effectiveLocaleTag()) {
            "ru" -> MatcherCountry.RUSSIA
            else -> MatcherCountry.USA
        }
    }
    var selectedCountry by remember { mutableStateOf<MatcherCountry?>(initialCountry) }
    val currentEngine by scanSettings.engine

    LaunchedEffect(detectFunctions) { scanSettings.save() }
    LaunchedEffect(currentEngine) {
        if (currentEngine == HyperScanEngine::class) {
            scanSettings.matchers.removeIf { it::class == CryptoSeedPhrase::class }
            scanSettings.save()
        }
    }

    // Display grouping only (doesn't change matcher logic).
    val namesGroupName = stringResource(Res.string.ScanSettings_DetectGroup_Names)
    val addressGeoGroupName = stringResource(Res.string.ScanSettings_DetectGroup_AddressGeo)
    val datesGroupName = stringResource(Res.string.ScanSettings_DetectGroup_Dates)
    val phonesGroupName = stringResource(Res.string.ScanSettings_DetectGroup_Phones)
    val passportIdsGroupName = stringResource(Res.string.ScanSettings_DetectGroup_PassportIds)
    val vehicleDriverGroupName = stringResource(Res.string.ScanSettings_DetectGroup_VehicleDriver)
    val taxGroupName = stringResource(Res.string.ScanSettings_DetectGroup_Tax)
    val ssnPensionGroupName = stringResource(Res.string.ScanSettings_DetectGroup_SsnPension)
    val migrationDocsGroupName = stringResource(Res.string.ScanSettings_DetectGroup_MigrationDocs)
    val militaryPoliceGroupName = stringResource(Res.string.ScanSettings_DetectGroup_MilitaryPolice)
    val medicalBioGroupName = stringResource(Res.string.ScanSettings_DetectGroup_MedicalBio)
    val educationGroupName = stringResource(Res.string.ScanSettings_DetectGroup_Education)
    val legalEntityGroupName = stringResource(Res.string.ScanSettings_DetectGroup_LegalEntity)
    val bankingSecrecyGroupName = stringResource(Res.string.ScanSettings_DetectGroup_BankingSecrecy)
    val cardsGroupName = stringResource(Res.string.ScanSettings_DetectGroup_Cards)
    val itAssetsGroupName = stringResource(Res.string.ScanSettings_DetectGroup_ItAssets)

    val namesMatchers = remember { listOf(FullName, FullNameUS, Email) }
    val addressGeoMatchers = remember { listOf(Address, AddressUS, Geo, CadastralNumber) }
    val datesMatchers = remember { listOf(Birthday, DeathDate) }
    val phonesMatchers = remember { listOf(Phone, PhoneUS) }
    val passportIdsMatchers = remember { listOf(Passport, PassportUS, IdentityDocType, MaritalStatus) }
    val vehicleDriverMatchers = remember {
        listOf(DriverLicense, DriverLicenseUS, VIN, VehicleRegNumber, OSAGOPolicy)
    }
    val taxMatchers = remember { listOf(INN, RIN, EIN, ITIN) }
    val ssnPensionMatchers = remember { listOf(SNILS, SSN) }
    val migrationDocsMatchers = remember { listOf(ResidencePermit, VisaNumberUS, AlienRegistrationNumber, USCIS, SEVISID) }
    val militaryPoliceMatchers = remember {
        listOf(MilitaryID, MilitaryRank, DODID, NSN, TCN, APOFPODPO, SecurityAffiliation)
    }
    val medicalBioMatchers = remember { listOf(OMS, MedicareUS, NPI) }
    val educationMatchers = remember { listOf(EducationDoc, EducationLevel, EducationLicense) }
    val legalEntityMatchers = remember {
        listOf(LegalEntityId, LegalEntityName, OGRNIP, OKPO, StateRegContract, ExecDocNumber, SocialUserId)
    }
    val bankingSecrecyMatchers = remember { listOf(BankAccount, BankAccountLE, RTN, SberBook) }
    val cardsMatchers = remember { listOf(CardNumber(), CVV) }
    val itAssetsMatchers = remember {
        listOf(Login, Password, IPv4, IPv6, CodeDetectFun, CertDetectFun, Certificate, HashData, GitleaksMatcher, CryptoWallet, CryptoSeedPhrase)
    }

    val matchersGroups = remember(
        namesGroupName,
        addressGeoGroupName,
        datesGroupName,
        phonesGroupName,
        passportIdsGroupName,
        vehicleDriverGroupName,
        taxGroupName,
        ssnPensionGroupName,
        migrationDocsGroupName,
        militaryPoliceGroupName,
        medicalBioGroupName,
        educationGroupName,
        legalEntityGroupName,
        bankingSecrecyGroupName,
        cardsGroupName,
        itAssetsGroupName
    ) {
        listOf(
            MatchersGroup(name = namesGroupName, matchers = namesMatchers),
            MatchersGroup(name = addressGeoGroupName, matchers = addressGeoMatchers),
            MatchersGroup(name = datesGroupName, matchers = datesMatchers),
            MatchersGroup(name = phonesGroupName, matchers = phonesMatchers),
            MatchersGroup(name = passportIdsGroupName, matchers = passportIdsMatchers),
            MatchersGroup(name = vehicleDriverGroupName, matchers = vehicleDriverMatchers),
            MatchersGroup(name = taxGroupName, matchers = taxMatchers),
            MatchersGroup(name = ssnPensionGroupName, matchers = ssnPensionMatchers),
            MatchersGroup(name = migrationDocsGroupName, matchers = migrationDocsMatchers),
            MatchersGroup(name = militaryPoliceGroupName, matchers = militaryPoliceMatchers),
            MatchersGroup(name = medicalBioGroupName, matchers = medicalBioMatchers),
            MatchersGroup(name = educationGroupName, matchers = educationMatchers),
            MatchersGroup(name = legalEntityGroupName, matchers = legalEntityMatchers),
            MatchersGroup(name = bankingSecrecyGroupName, matchers = bankingSecrecyMatchers),
            MatchersGroup(name = cardsGroupName, matchers = cardsMatchers),
            MatchersGroup(name = itAssetsGroupName, matchers = itAssetsMatchers)
        )
    }

    val availableMatchers = remember(matchersGroups, currentEngine) {
        val all = matchersGroups.flatMap { it.matchers }
        if (currentEngine == HyperScanEngine::class) all.filter { it::class != CryptoSeedPhrase::class } else all
    }
    val filteredGroups = remember(matchersGroups, selectedCountry, currentEngine) {
        val groups = if (selectedCountry == null) matchersGroups else MatcherCountryMapping.filterGroups(matchersGroups, selectedCountry!!)
        if (currentEngine == HyperScanEngine::class) {
            groups
                .map { g ->
                    if (g.name == itAssetsGroupName) {
                        g.copy(matchers = g.matchers.filter { it::class != CryptoSeedPhrase::class })
                    } else {
                        g
                    }
                }
                .filter { it.matchers.isNotEmpty() }
        } else groups
    }
    val countryMatchers = remember(selectedCountry, availableMatchers) {
        if (selectedCountry == null) availableMatchers else MatcherCountryMapping.filterMatchers(availableMatchers, selectedCountry!!)
    }
    val isAllSelected = countryMatchers.all { m -> detectFunctions.any { it::class == m::class } }

    val dense = unifiedBlock
    val settingsTokens = LocalMainScreenAdaptiveTokens.current.settings
    val adaptiveScale = LocalMainScreenAdaptiveTokens.current.scale
    // Slightly wider left column to keep group names on one line.
    val groupLabelWidth = if (dense) adaptiveScale.dp(160.dp, min = 150.dp, max = 260.dp) else adaptiveScale.dp(240.dp, min = 220.dp, max = 320.dp)
    val groupRowHGap = if (dense) adaptiveScale.dp(5.dp, min = 4.dp, max = 9.dp) else adaptiveScale.dp(10.dp, min = 8.dp, max = 16.dp)
    val chipCorner = if (dense) adaptiveScale.dp(4.dp, min = 3.dp, max = 7.dp) else adaptiveScale.dp(6.dp, min = 5.dp, max = 10.dp)
    val chipPadH = if (dense) adaptiveScale.dp(1.dp, min = 1.dp, max = 2.dp) else adaptiveScale.dp(2.dp, min = 1.dp, max = 4.dp)
    val chipLabelFs = if (dense) adaptiveScale.sp(8.sp, min = 8.sp, max = 10.sp) else adaptiveScale.sp(10.sp, min = 10.sp, max = 12.sp)
    val groupLabelFs = if (dense) adaptiveScale.sp(8.5.sp, min = 8.sp, max = 10.sp) else adaptiveScale.sp(10.sp, min = 10.sp, max = 12.sp)
    // Keep group labels on a single line (no wrapping).
    val groupMaxLines = 1
    val flowHGap = if (dense) adaptiveScale.dp(7.dp, min = 6.dp, max = 12.dp) else adaptiveScale.dp(9.dp, min = 8.dp, max = 14.dp)
    val flowVGap = if (dense) adaptiveScale.dp(5.dp, min = 4.dp, max = 9.dp) else adaptiveScale.dp(6.dp, min = 5.dp, max = 10.dp)
    val innerSectionGap = if (dense) adaptiveScale.dp(1.dp, min = 1.dp, max = 3.dp) else adaptiveScale.dp(3.dp, min = 2.dp, max = 6.dp)
    // Сейчас общий левый отступ секции даёт 4.dp (внутренний padding карточки).
    // Чтобы визуально получить "в 3 раза больше" слева в первой колонке, добавляем ещё +8.dp.
    val firstColumnExtraStartPadding = adaptiveScale.dp(8.dp, min = 6.dp, max = 14.dp)
    val afterFlagsGap = (settingsTokens.contentBelowHeaderSpacing * 3) / 2f

    val content: @Composable ColumnScope.() -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(innerSectionGap)
        ) {
            if (!showTitle) {
                Text(
                    text = stringResource(Res.string.ScanSettings_DetectFunctions),
                    style = MaterialTheme.typography.labelLarge,
                    fontSize = if (dense) adaptiveScale.sp(11.sp, min = 10.sp, max = 13.sp) else adaptiveScale.sp(13.sp, min = 12.sp, max = 15.sp),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                CompactCountryFilterChips(
                    selectedCountry = selectedCountry,
                    onCountrySelected = { selectedCountry = it },
                    modifier = Modifier.weight(1f),
                    dense = dense,
                    horizontalSpacing = adaptiveScale.dp(4.dp, min = 3.dp, max = 8.dp),
                    getCountryStats = { country ->
                        val countryAvailable = availableMatchers.filter { MatcherCountryMapping.getCountry(it) == country }
                        val total = countryAvailable.size
                        val selected = countryAvailable.count { m -> detectFunctions.any { it::class == m::class } }
                        Pair(selected, total)
                    }
                )
                if (!showTitle) {
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
            }

            Spacer(Modifier.height(afterFlagsGap))

            Column(modifier = Modifier.wrapContentWidth()) {
                filteredGroups.forEachIndexed { idx, group ->
                    val groupMatchers = group.matchers
                    SettingsTableRowWithContentWidthDivider(
                        dense = dense,
                        showDividerBelow = idx != filteredGroups.lastIndex,
                    ) {
                        Row(
                            modifier = Modifier
                                .wrapContentWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(groupRowHGap),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = group.name,
                                modifier = Modifier
                                    .width(groupLabelWidth)
                                    .padding(start = firstColumnExtraStartPadding),
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = groupLabelFs,
                                lineHeight = if (dense) 10.sp else TextUnit.Unspecified,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = groupMaxLines,
                                overflow = TextOverflow.Ellipsis
                            )
                            SettingsTableColumnDivider()
                            FlowRow(
                                modifier = Modifier,
                                horizontalArrangement = Arrangement.spacedBy(flowHGap),
                                verticalArrangement = Arrangement.spacedBy(flowVGap)
                            ) {
                            groupMatchers.forEach { matcher ->
                                val selected = scanSettings.matchers.any { it::class == matcher::class }
                                val forceRu = selectedCountry == MatcherCountry.RUSSIA
                                val label = remember(forceRu, matcher::class) { mutableStateOf<String?>(null) }

                                LaunchedEffect(forceRu, matcher::class) {
                                    label.value = if (forceRu) {
                                        matcher.readableNameForLocale("ru")
                                    } else {
                                        null
                                    }
                                }

                                MatcherTooltip(matcher = matcher) {
                                    SettingsFlowToggleChip(
                                        selected = selected,
                                        onClick = {
                                            if (selected) scanSettings.matchers.removeAll { it::class == matcher::class }
                                            else scanSettings.matchers.add(matcher)
                                            scanSettings.save()
                                        },
                                        label = label.value ?: matcher.composableName(),
                                        chipCorner = chipCorner,
                                        chipPadH = chipPadH,
                                        chipLabelFs = chipLabelFs,
                                        maxLines = 1
                                    )
                                }
                            }
                            }
                        }
                    }
                }
            }
        }
    }
    if (showTitle) {
        if (unifiedBlock) {
            SettingsUnifiedSubsection(
                title = stringResource(Res.string.ScanSettings_DetectFunctions),
                modifier = modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .then(
                        if (errorHighlight) {
                            Modifier.border(
                                width = adaptiveScale.dp(1.5.dp, min = 1.2.dp, max = 2.2.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                shape = RoundedCornerShape(adaptiveScale.dp(10.dp, min = 8.dp, max = 14.dp))
                            )
                        } else {
                            Modifier
                        }
                    ),
                expandContentVertically = true,
                titleTrailingInline = true,
                titleTrailingInlineSpacing = settingsTokens.headerInlineActionSpacing,
                titleTrailing = {
                    SettingsSelectAllTextButton(
                        allSelected = isAllSelected,
                        onClick = {
                            if (isAllSelected) {
                                countryMatchers.forEach { m -> scanSettings.matchers.removeAll { it::class == m::class } }
                            } else {
                                scanSettings.matchers.addAll(
                                    countryMatchers.filter { m -> !detectFunctions.any { it::class == m::class } }
                                )
                            }
                            scanSettings.save()
                        }
                    )
                },
                content = content
            )
        } else {
            SettingsSectionCard(
                title = stringResource(Res.string.ScanSettings_DetectFunctions),
                modifier = modifier.fillMaxWidth().fillMaxHeight(),
                expandContentVertically = true,
                titleTrailingInline = true,
                titleTrailingInlineSpacing = settingsTokens.headerInlineActionSpacing,
                titleTrailing = {
                    SettingsSelectAllTextButton(
                        allSelected = isAllSelected,
                        onClick = {
                            if (isAllSelected) {
                                countryMatchers.forEach { m -> scanSettings.matchers.removeAll { it::class == m::class } }
                            } else {
                                scanSettings.matchers.addAll(
                                    countryMatchers.filter { m -> !detectFunctions.any { it::class == m::class } }
                                )
                            }
                            scanSettings.save()
                        }
                    )
                },
                content = content
            )
        }
    } else {
        Column(modifier = modifier, content = content)
    }
}

