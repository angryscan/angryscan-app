package org.angryscan.app.ui.windows.screens.main

import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import org.angryscan.app.db.models.TaskState
import org.angryscan.app.resources.MainScreen_RecentScans_Empty
import org.angryscan.app.resources.MainScreen_RecentScans_Title
import org.angryscan.app.resources.MainScreen_RecentScans_ViewFullHistory
import org.angryscan.app.resources.Res
import org.angryscan.app.scan.ScanService
import org.angryscan.app.ui.DesktopMainLayout
import org.angryscan.app.ui.windows.components.DescriptionTooltip
import org.angryscan.app.ui.windows.screens.main.components.MainScreenConnector
import org.angryscan.app.ui.windows.screens.main.settings.SettingsBox
import org.angryscan.app.ui.windows.screens.main.subscreens.DatabaseScreen
import org.angryscan.app.ui.windows.screens.main.subscreens.FileShareScreen
import org.angryscan.app.ui.windows.screens.main.subscreens.HTTPScreen
import org.angryscan.app.ui.windows.screens.main.subscreens.S3Screen
import org.angryscan.app.ui.windows.screens.scans.components.ScanTaskCard
import org.angryscan.app.ui.windows.screens.scans.components.ScanTaskHeaderRow
import org.angryscan.app.ui.windows.screens.scans.components.StatusFilter
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** Top inset for settings / recent scans panel: clears path row, source radios, and optional S3 connection row. */
private val MainCentralPanelTopInset = 200.dp

/** Extra vertical space between the path / action block and the source radio row. */
private val MainPathBlockToRadioRowSpacing = 0.dp

/**
 * Снятый за счёт ужатия отступов у радиостроки/колонки зазор переносим вверх: панель настроек и recent scans начинаются выше.
 */
private val MainCentralPanelTopLiftFromHeaderTightening = 20.dp

/**
 * When scan settings are open (File Share / HTTP only): panel starts just under the source radios for more height.
 * S3 keeps [MainCentralPanelTopInset] — extra row under radios must stay clear.
 */
private val MainScanSettingsPanelTopBelowRadios = 168.dp

/** То же, что [DesktopMainLayout.mainContentColumnMaxWidth]: путь и панель scan settings. */
private val MainContentColumnMaxWidth = DesktopMainLayout.mainContentColumnMaxWidth
private val MainContentOuterPaddingH = 24.dp
private val MainContentInnerPaddingH = 18.dp

@Composable
fun MainScreen(
    showScan: (taskId: Int) -> Unit,
    showScansHistory: () -> Unit
) {
    val navController = rememberNavController()
    var bottomBarContent by remember { mutableStateOf<@Composable () -> Unit>({}) }
    var underSourceContent by remember { mutableStateOf<@Composable () -> Unit>({}) }
    var settingsPanelOpened by remember { mutableStateOf(false) }
    val scanService = koinInject<ScanService>()

    val colorScheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current

    val headerRouteEntry by navController.currentBackStackEntryAsState()
    val s3SourceActive = headerRouteEntry?.destination?.hasRoute(MainScreenConnector.S3::class) == true
    val centralPanelTopPadding = when {
        // When scan settings are open we don't show extra under-radio rows (incl. S3 params),
        // so the panel can start right under the radios for all sources.
        settingsPanelOpened ->
            MainScanSettingsPanelTopBelowRadios - MainCentralPanelTopLiftFromHeaderTightening
        else ->
            MainCentralPanelTopInset + MainPathBlockToRadioRowSpacing - MainCentralPanelTopLiftFromHeaderTightening
    }
    val centralPanelMaxWidth = if (settingsPanelOpened) {
        MainContentColumnMaxWidth
    } else {
        // Latest scans table can use a bit more horizontal space.
        1160.dp
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Tap outside interactive controls clears path field / any focused editor (children are above this layer).
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { focusManager.clearFocus() }
        )
        // Primary action block is centered in the full window.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = MainContentOuterPaddingH)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 18.dp)
                    .widthIn(max = MainContentColumnMaxWidth)
                    .fillMaxWidth()
                    .padding(horizontal = MainContentInnerPaddingH, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(MainPathBlockToRadioRowSpacing),
                        horizontalAlignment = Alignment.Start,
                    ) {
                        bottomBarContent()
                        SourceRadioRow(
                            navController = navController,
                            settingsOpen = settingsPanelOpened,
                            onSelectedLabelClick = { settingsPanelOpened = !settingsPanelOpened },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (!settingsPanelOpened) {
                        underSourceContent()
                    }
                }
            }
        }

        Surface(
            modifier = (if (settingsPanelOpened) {
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(
                        start = MainContentOuterPaddingH,
                        end = MainContentOuterPaddingH,
                        top = centralPanelTopPadding,
                        bottom = 8.dp
                    )
                    .widthIn(max = centralPanelMaxWidth)
                    .fillMaxWidth()
                    .fillMaxHeight()
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = MainContentOuterPaddingH,
                        end = MainContentOuterPaddingH,
                        bottom = 12.dp
                    )
                    .widthIn(max = centralPanelMaxWidth)
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = colorScheme.outlineVariant.copy(alpha = 0.62f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .wrapContentHeight()
            }).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() },
            shape = RoundedCornerShape(16.dp),
            color = if (settingsPanelOpened) Color.Transparent else colorScheme.surfaceVariant.copy(alpha = 0.28f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            // Ensure identical inner content frame for settings/recent scans
            Box(
                modifier = if (settingsPanelOpened) {
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = MainContentInnerPaddingH, vertical = 8.dp)
                } else {
                    Modifier
                        .wrapContentHeight()
                        .padding(horizontal = MainContentInnerPaddingH, vertical = 8.dp)
                }
            ) {
                if (settingsPanelOpened) {
                    SettingsBox(
                        transition = updateTransition(targetState = true, label = "settings-inline")
                    )
                } else {
                    RecentScansPreview(
                        scanService = scanService,
                        onTaskClick = showScan,
                        onViewAllClick = showScansHistory
                    )
                }
            }
        }

        // Source selection moved under the path block (radio row).
    }

    // Keep subscreen-specific state and bottom bar logic active.
    Box(modifier = Modifier.size(0.dp)) {
        NavHost(
            navController = navController,
            startDestination = MainScreenConnector.FileShare
        ) {
            composable<MainScreenConnector.FileShare> {
                FileShareScreen(
                    navController = navController,
                    expandScanState = { taskId -> showScan(taskId) },
                    setSidebarContent = { },
                    setBottomBarContent = { content -> bottomBarContent = content },
                    setUnderSourceContent = { content -> underSourceContent = content }
                )
            }
            composable<MainScreenConnector.S3> {
                S3Screen(
                    navController = navController,
                    expandScanState = { taskId -> showScan(taskId) },
                    setSidebarContent = { },
                    setBottomBarContent = { content -> bottomBarContent = content },
                    setUnderSourceContent = { content -> underSourceContent = content }
                )
            }
            composable<MainScreenConnector.HTTP> {
                HTTPScreen(
                    navController = navController,
                    expandScanState = { taskId -> showScan(taskId) },
                    setSidebarContent = { },
                    setBottomBarContent = { content -> bottomBarContent = content },
                    setUnderSourceContent = { content -> underSourceContent = content }
                )
            }
            composable<MainScreenConnector.Postgres> {
                // Database screen doesn't provide under-radio content (unlike S3),
                // so clear whatever previous source left there.
                SideEffect {
                    underSourceContent = { }
                }
                DatabaseScreen(
                    expandScanState = { taskId -> showScan(taskId) },
                    setSidebarContent = { },
                    setBottomBarContent = { content -> bottomBarContent = content }
                )
            }
        }
    }
}

@Composable
private fun SourceRadioRow(
    navController: androidx.navigation.NavController,
    settingsOpen: Boolean,
    modifier: Modifier = Modifier,
    onSelectedLabelClick: () -> Unit,
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val selectedRoute = when {
        destination?.hasRoute(MainScreenConnector.FileShare::class) == true -> MainScreenConnector.FileShare
        destination?.hasRoute(MainScreenConnector.S3::class) == true -> MainScreenConnector.S3
        destination?.hasRoute(MainScreenConnector.HTTP::class) == true -> MainScreenConnector.HTTP
        destination?.hasRoute(MainScreenConnector.Postgres::class) == true -> MainScreenConnector.Postgres
        else -> MainScreenConnector.FileShare
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SourceRadioItem(
            selected = selectedRoute == MainScreenConnector.FileShare,
            label = "File Share",
            settingsOpen = settingsOpen,
            onSelect = { if (selectedRoute != MainScreenConnector.FileShare) navController.navigate(MainScreenConnector.FileShare) },
            onSelectedLabelClick = onSelectedLabelClick
        )
        SourceRadioItem(
            selected = selectedRoute == MainScreenConnector.S3,
            label = "AWS S3",
            settingsOpen = settingsOpen,
            onSelect = { if (selectedRoute != MainScreenConnector.S3) navController.navigate(MainScreenConnector.S3) },
            onSelectedLabelClick = onSelectedLabelClick
        )
        SourceRadioItem(
            selected = selectedRoute == MainScreenConnector.HTTP,
            label = "HTTP",
            settingsOpen = settingsOpen,
            onSelect = { if (selectedRoute != MainScreenConnector.HTTP) navController.navigate(MainScreenConnector.HTTP) },
            onSelectedLabelClick = onSelectedLabelClick
        )
        SourceRadioItem(
            selected = selectedRoute == MainScreenConnector.Postgres,
            label = "SQL Database",
            settingsOpen = settingsOpen,
            onSelect = {
                if (selectedRoute != MainScreenConnector.Postgres) {
                    navController.navigate(MainScreenConnector.Postgres)
                }
            },
            onSelectedLabelClick = onSelectedLabelClick
        )
    }
}

@Composable
private fun SourceRadioItem(
    selected: Boolean,
    label: String,
    settingsOpen: Boolean,
    onSelect: () -> Unit,
    onSelectedLabelClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    // Standard underline: hover -> "clickable", selected+settingsOpen -> "mode".
    val underline = hovered || (selected && settingsOpen)
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
        alpha = when {
            selected -> 0.95f
            hovered -> 0.88f
            else -> 0.72f
        }
    )
    val radioColors = RadioButtonDefaults.colors(
        selectedColor = color,
        unselectedColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
    )

    @Composable
    fun MainRow() {
        Row(
            modifier = Modifier
                .heightIn(min = 32.dp)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    if (selected) onSelectedLabelClick() else onSelect()
                }
                .hoverable(interactionSource)
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = 2.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            RadioButton(
                selected = selected,
                onClick = null,
                modifier = Modifier.scale(0.82f),
                colors = radioColors
            )
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = color,
                    textDecoration = if (underline) TextDecoration.Underline else TextDecoration.None,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    softWrap = false
                )
            }
        }
    }

    if (selected && settingsOpen) {
        DescriptionTooltip(
            description = "Click to close scan settings",
            delay = 350
        ) {
            MainRow()
        }
    } else {
        MainRow()
    }
}

@OptIn(ExperimentalTime::class)
@Composable
private fun RecentScansPreview(
    scanService: ScanService,
    onTaskClick: (Int) -> Unit,
    onViewAllClick: () -> Unit,
) {
    val allTasks by scanService.tasks.tasks.collectAsState()
    var statusFilter by remember { mutableStateOf(StatusFilter.ALL) }

    val visibleTasks = allTasks
        .filter { it.state.value != TaskState.LOADING }
        .sortedByDescending { it.finishedAt.value }
        .sortedByDescending { it.pausedAt.value }
        .sortedByDescending { it.startedAt.value }
    val filteredTasks = visibleTasks
        .filter { task ->
            val states = statusFilter.states
            if (states.isEmpty()) true else task.state.value in states
        }
        .take(5)

    var expandedTaskId by remember { mutableStateOf<Int?>(null) }

    var currentTime by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Clock.System.now()
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(Res.string.MainScreen_RecentScans_Title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (visibleTasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.MainScreen_RecentScans_Empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val countActive = visibleTasks.count { it.state.value == TaskState.SCANNING || it.state.value == TaskState.SEARCHING }
            val countPaused = visibleTasks.count { it.state.value == TaskState.STOPPED || it.state.value == TaskState.PENDING }
            val countError = visibleTasks.count { it.state.value == TaskState.FAILED }
            val countCompleted = visibleTasks.count { it.state.value == TaskState.COMPLETED }

            ScanTaskHeaderRow(
                statusFilter = statusFilter,
                statusCounts = mapOf(
                    StatusFilter.ALL to visibleTasks.size,
                    StatusFilter.ACTIVE to countActive,
                    StatusFilter.PAUSED to countPaused,
                    StatusFilter.ERROR to countError,
                    StatusFilter.COMPLETED to countCompleted
                ),
                onStatusFilterChange = { statusFilter = it }
            )
            val recentScansListHeight = 280.dp
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = recentScansListHeight),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(filteredTasks, key = { it.id.value ?: it.hashCode() }) { task ->
                    ScanTaskCard(
                        taskEntity = task,
                        onClick = {
                            task.id.value?.let { onTaskClick(it) }
                        },
                        currentTime = currentTime,
                        attributesExpanded = task.id.value != null && expandedTaskId == task.id.value,
                        onAttributesExpandClick = {
                            val id = task.id.value ?: return@ScanTaskCard
                            expandedTaskId = if (expandedTaskId == id) null else id
                        }
                    )
                }
            }
            TextButton(
                onClick = onViewAllClick,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(Res.string.MainScreen_RecentScans_ViewFullHistory),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
