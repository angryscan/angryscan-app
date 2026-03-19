package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Transition
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.resources.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

private val ContainerShape = RoundedCornerShape(24.dp)
private val ContentPadding = 12.dp

enum class SettingsTab { Scan, Files, Detect, Signatures }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBox(
    transition: Transition<Boolean>,
    navController: NavController,
    sqlConnectionBlinkSignal: Int = 0,
    showSnackbar: suspend (message: String, isError: Boolean) -> Unit = { _, _ -> }
) {
    val scanSettings = koinInject<ScanSettings>()
    var selectedTab by remember { mutableIntStateOf(SettingsTab.Scan.ordinal) }
    val scrollState = rememberScrollState()

    val colorScheme = MaterialTheme.colorScheme
    AnimatedVisibility(
        visible = transition.currentState,
        enter = expandVertically(),
        exit = shrinkVertically()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(ContainerShape)
                .background(colorScheme.surface.copy(alpha = 0.6f))
                .border(
                    width = 1.dp,
                    color = colorScheme.outlineVariant.copy(alpha = 0.25f),
                    shape = ContainerShape
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Шапка: табы в отдельной полоске с мягким фоном
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.35f))
                ) {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = Color.Transparent,
                        contentColor = colorScheme.primary,
                        divider = {},
                        indicator = { tabPositions ->
                            Box(Modifier.fillMaxWidth()) {
                                if (selectedTab < tabPositions.size) {
                                    val pos = tabPositions[selectedTab]
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .offset(x = pos.left)
                                            .width(pos.width)
                                            .height(3.dp)
                                            .clip(RoundedCornerShape(1.5.dp))
                                            .background(colorScheme.primary)
                                    )
                                }
                            }
                        }
                    ) {
                        Tab(
                            selected = selectedTab == SettingsTab.Scan.ordinal,
                            onClick = { selectedTab = SettingsTab.Scan.ordinal },
                            text = { Text(stringResource(Res.string.ScanSettings_TabScan), fontSize = 14.sp) }
                        )
                        Tab(
                            selected = selectedTab == SettingsTab.Files.ordinal,
                            onClick = { selectedTab = SettingsTab.Files.ordinal },
                            text = { Text(stringResource(Res.string.ScanSettings_TabFiles), fontSize = 14.sp) }
                        )
                        Tab(
                            selected = selectedTab == SettingsTab.Detect.ordinal,
                            onClick = { selectedTab = SettingsTab.Detect.ordinal },
                            text = { Text(stringResource(Res.string.ScanSettings_TabDetect), fontSize = 14.sp) }
                        )
                        Tab(
                            selected = selectedTab == SettingsTab.Signatures.ordinal,
                            onClick = { selectedTab = SettingsTab.Signatures.ordinal },
                            text = { Text(stringResource(Res.string.ScanSettings_TabSignatures), fontSize = 14.sp) }
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                    thickness = 1.dp
                )

                // Контент выбранной секции — один скролл
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = ContentPadding)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(vertical = ContentPadding),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        when (selectedTab) {
                            SettingsTab.Scan.ordinal -> SettingsBoxScan(
                                scanSettings = scanSettings,
                                navController = navController,
                                sqlConnectionBlinkSignal = sqlConnectionBlinkSignal,
                                selectedTab = selectedTab,
                                showSnackbar = showSnackbar
                            )
                            SettingsTab.Files.ordinal -> SettingsBoxExtensionsSelection(scanSettings)
                            SettingsTab.Detect.ordinal -> SettingsBoxDetectFunctionsGrouped(scanSettings)
                            SettingsTab.Signatures.ordinal -> SettingsBoxUserSignature(scanSettings)
                        }
                    }

                    VerticalScrollbar(
                        adapter = rememberScrollbarAdapter(scrollState),
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .width(8.dp),
                        style = LocalScrollbarStyle.current.copy(
                            hoverColor = MaterialTheme.colorScheme.primary,
                            unhoverColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    )
                }
            }
        }
    }
}
