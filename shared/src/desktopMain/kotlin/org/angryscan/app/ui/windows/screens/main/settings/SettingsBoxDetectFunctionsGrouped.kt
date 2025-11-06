package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.angryscan.app.common.ScanSettings
import org.angryscan.common.matchers.*
import org.jetbrains.compose.resources.stringResource
import org.angryscan.app.resources.*
import org.angryscan.app.scan.functions.CertDetectFun
import org.angryscan.app.scan.functions.CodeDetectFun
import org.angryscan.app.scan.functions.RKNDomainDetectFun
import org.angryscan.app.ui.windows.screens.main.settings.items.MatchersGroup
import org.angryscan.app.ui.windows.screens.main.settings.items.MinimalDetectionGroupCard
import org.angryscan.app.ui.windows.screens.main.settings.items.MinimalSelectAllButton
import org.koin.compose.koinInject


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBoxDetectFunctionsGrouped(
    scanSettings: ScanSettings
) {
    val detectFunctions = remember { scanSettings.matchers }
    var expanded by remember { scanSettings.matchersSettingsExpanded }

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
                    CarNumber,
                    SNILS,
                    Passport,
                    OMS,
                    INN
                )
            ),
            MatchersGroup(
                name = personalDataTextName,
                matchers = listOf(
                    FullName,
                    Email,
                    Address,
                    Login,
                    Password
                )
            ),
            MatchersGroup(
                name = bankingSecrecyName,
                matchers = listOf(
                    CardNumber(),
                    AccountNumber,
                    CVV
                )
            ),
            MatchersGroup(
                name = itAssetsName,
                matchers = listOf(
                    IPv4,
                    IPv6,
                    CodeDetectFun,
                    CertDetectFun,
                    RKNDomainDetectFun
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
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MinimalSelectAllButton(scanSettings = scanSettings)

            matchersGroups.forEach { group ->
                MinimalDetectionGroupCard(
                    group = group,
                    scanSettings = scanSettings
                )
            }
        }
    }
}



