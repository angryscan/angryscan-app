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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.angryscan.app.common.DatabaseType
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.ScreenStateSettings
import org.angryscan.app.db.models.TaskState
import org.angryscan.app.resources.*
import org.angryscan.app.scan.ScanService
import org.angryscan.app.scan.TaskReplayHelper
import org.angryscan.app.scan.common.connectors.*
import org.angryscan.app.scan.common.files.types.IFileType
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
import org.angryscan.common.engine.IMatcher
import org.angryscan.common.matchers.UserSignature
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
private val MainLatestScansBlockMaxWidth = 1160.dp
private val MainContentOuterPaddingH = 24.dp
private val MainContentInnerPaddingH = 18.dp

private fun connectorToStorageValue(connector: Any): String = when (connector) {
    MainScreenConnector.FileShare -> "fileshare"
    MainScreenConnector.S3 -> "s3"
    MainScreenConnector.HTTP -> "http"
    MainScreenConnector.Postgres -> "database"
    else -> "fileshare"
}

private fun storageValueToConnector(value: String): Any = when (value.lowercase()) {
    "s3" -> MainScreenConnector.S3
    "http" -> MainScreenConnector.HTTP
    "database", "postgres", "sql" -> MainScreenConnector.Postgres
    else -> MainScreenConnector.FileShare
}

@Composable
fun MainScreen(
    showScan: (taskId: Int) -> Unit,
    showScansHistory: () -> Unit
) {
    val navController = rememberNavController()
    var bottomBarContent by remember { mutableStateOf<@Composable () -> Unit>({}) }
    var underSourceContent by remember { mutableStateOf<@Composable () -> Unit>({}) }
    var settingsPanelOpened by remember { mutableStateOf(false) }
    var highlightMissingExtensions by remember { mutableStateOf(false) }
    var highlightMissingMatchers by remember { mutableStateOf(false) }
    var s3RadioBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var s3UnderBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var databaseRadioBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var databaseUnderBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    var databaseContourContainerBounds by remember { mutableStateOf<androidx.compose.ui.geometry.Rect?>(null) }
    val scanService = koinInject<ScanService>()
    val scanSettings = koinInject<ScanSettings>()
    val screenStateSettings = koinInject<ScreenStateSettings>()

    LaunchedEffect(highlightMissingExtensions, highlightMissingMatchers) {
        if (highlightMissingExtensions || highlightMissingMatchers) {
            delay(2000)
            highlightMissingExtensions = false
            highlightMissingMatchers = false
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val databaseContourColor = colorScheme.outlineVariant.copy(alpha = 0.62f)
    val focusManager = LocalFocusManager.current

    val headerRouteEntry by navController.currentBackStackEntryAsState()
    val pendingReplay by TaskReplayHelper.pending.collectAsState()
    val fileShareSourceActive = headerRouteEntry?.destination?.hasRoute(MainScreenConnector.FileShare::class) == true
    val s3SourceActive = headerRouteEntry?.destination?.hasRoute(MainScreenConnector.S3::class) == true
    val httpSourceActive = headerRouteEntry?.destination?.hasRoute(MainScreenConnector.HTTP::class) == true
    val sqlSourceActive = headerRouteEntry?.destination?.hasRoute(MainScreenConnector.Postgres::class) == true
    LaunchedEffect(sqlSourceActive, s3SourceActive, settingsPanelOpened) {
        if ((!sqlSourceActive && !s3SourceActive) || settingsPanelOpened) {
            s3RadioBounds = null
            s3UnderBounds = null
            databaseRadioBounds = null
            databaseUnderBounds = null
            databaseContourContainerBounds = null
        }
    }
    fun applyDetectionSettings(
        extensionsTarget: MutableList<IFileType>,
        matchersTarget: MutableList<IMatcher>,
        userSignaturesTarget: MutableList<UserSignature>,
        fastScanTarget: MutableState<Boolean>,
        extensions: List<IFileType>,
        matchers: List<IMatcher>,
        userSignatures: List<UserSignature>,
        fastScan: Boolean
    ) {
        extensionsTarget.clear()
        extensionsTarget.addAll(extensions)
        matchersTarget.clear()
        matchersTarget.addAll(matchers)
        userSignaturesTarget.clear()
        userSignaturesTarget.addAll(userSignatures)
        fastScanTarget.value = fastScan
    }

    LaunchedEffect(pendingReplay) {
        val replay = pendingReplay ?: return@LaunchedEffect
        val replayUserSignatures = replay.matchers.filterIsInstance<UserSignature>()
        val replayMatchers = replay.matchers.filterNot { it is UserSignature }

        applyDetectionSettings(
            extensionsTarget = scanSettings.extensions,
            matchersTarget = scanSettings.matchers,
            userSignaturesTarget = scanSettings.userSignatures,
            fastScanTarget = scanSettings.fastScan,
            extensions = replay.extensions,
            matchers = replayMatchers,
            userSignatures = replayUserSignatures,
            fastScan = replay.fastScan
        )
        scanSettings.save()

        val destination = when (val connector = replay.connector) {
            is ConnectorS3 -> {
                screenStateSettings.s3ScreenState.path = replay.path
                screenStateSettings.s3ScreenState.endpoint = connector.endpointStr
                screenStateSettings.s3ScreenState.accessKey = connector.accessKey
                screenStateSettings.s3ScreenState.secretKey = connector.secretKey
                screenStateSettings.s3ScreenState.bucket = connector.bucketStr
                applyDetectionSettings(
                    extensionsTarget = screenStateSettings.s3ScreenState.extensions,
                    matchersTarget = screenStateSettings.s3ScreenState.matchers,
                    userSignaturesTarget = screenStateSettings.s3ScreenState.userSignatures,
                    fastScanTarget = screenStateSettings.s3ScreenState.fastScan,
                    extensions = replay.extensions,
                    matchers = replayMatchers,
                    userSignatures = replayUserSignatures,
                    fastScan = replay.fastScan
                )
                MainScreenConnector.S3
            }

            is ConnectorHTTP -> {
                screenStateSettings.httpScreenState.path = replay.path
                applyDetectionSettings(
                    extensionsTarget = screenStateSettings.httpScreenState.extensions,
                    matchersTarget = screenStateSettings.httpScreenState.matchers,
                    userSignaturesTarget = screenStateSettings.httpScreenState.userSignatures,
                    fastScanTarget = screenStateSettings.httpScreenState.fastScan,
                    extensions = replay.extensions,
                    matchers = replayMatchers,
                    userSignatures = replayUserSignatures,
                    fastScan = replay.fastScan
                )
                MainScreenConnector.HTTP
            }

            is IDatabaseConnector -> {
                val currentSql = screenStateSettings.sqlScreenState.value
                screenStateSettings.sqlScreenState.value = when (connector) {
                    is ConnectorPostgres -> currentSql.copy(
                        databaseType = DatabaseType.PostgreSQL,
                        host = connector.host,
                        port = connector.port.toString(),
                        database = connector.database,
                        schema = replay.path,
                        user = connector.user,
                        password = connector.password,
                        filePath = ""
                    )
                    is ConnectorMySQL -> currentSql.copy(
                        databaseType = DatabaseType.MySQL,
                        host = connector.host,
                        port = connector.port.toString(),
                        database = connector.database,
                        schema = replay.path,
                        user = connector.user,
                        password = connector.password,
                        filePath = ""
                    )
                    is ConnectorGreenPlum -> currentSql.copy(
                        databaseType = DatabaseType.GreenPlum,
                        host = connector.host,
                        port = connector.port.toString(),
                        database = connector.database,
                        schema = replay.path,
                        user = connector.user,
                        password = connector.password,
                        filePath = ""
                    )
                    is ConnectorHive -> currentSql.copy(
                        databaseType = DatabaseType.Hive,
                        host = connector.host,
                        port = connector.port.toString(),
                        database = connector.database,
                        schema = replay.path,
                        user = connector.user,
                        password = connector.password,
                        filePath = ""
                    )
                    is ConnectorCockroachDB -> currentSql.copy(
                        databaseType = DatabaseType.CockroachDB,
                        host = connector.host,
                        port = connector.port.toString(),
                        database = connector.database,
                        schema = replay.path,
                        user = connector.user,
                        password = connector.password,
                        filePath = ""
                    )
                    is ConnectorClickHouse -> currentSql.copy(
                        databaseType = DatabaseType.ClickHouse,
                        host = connector.host,
                        port = connector.port.toString(),
                        database = connector.database,
                        schema = replay.path,
                        user = connector.user,
                        password = connector.password,
                        filePath = ""
                    )
                    is ConnectorRedshift -> currentSql.copy(
                        databaseType = DatabaseType.Redshift,
                        host = connector.host,
                        port = connector.port.toString(),
                        database = connector.database,
                        schema = replay.path,
                        user = connector.user,
                        password = connector.password,
                        filePath = ""
                    )
                    is ConnectorSqlServer -> currentSql.copy(
                        databaseType = DatabaseType.SqlServer,
                        host = connector.host,
                        port = connector.port.toString(),
                        database = connector.database,
                        schema = replay.path,
                        user = connector.user,
                        password = connector.password,
                        filePath = ""
                    )
                    is ConnectorMongoDB -> currentSql.copy(
                        databaseType = DatabaseType.MongoDB,
                        host = connector.host,
                        port = connector.port.toString(),
                        database = connector.database,
                        schema = replay.path,
                        user = connector.user,
                        password = connector.password,
                        filePath = ""
                    )
                    is ConnectorSqlite -> currentSql.copy(
                        databaseType = DatabaseType.SQLite,
                        filePath = connector.filePath,
                        schema = "",
                        host = "",
                        port = "5432",
                        database = "",
                        user = "",
                        password = ""
                    )
                    else -> currentSql
                }
                val sqlState = screenStateSettings.sqlScreenState.value
                applyDetectionSettings(
                    extensionsTarget = sqlState.extensions,
                    matchersTarget = sqlState.matchers,
                    userSignaturesTarget = sqlState.userSignatures,
                    fastScanTarget = sqlState.fastScan,
                    extensions = replay.extensions,
                    matchers = replayMatchers,
                    userSignatures = replayUserSignatures,
                    fastScan = replay.fastScan
                )
                MainScreenConnector.Postgres
            }

            else -> {
                screenStateSettings.fileShareScreenState.path = replay.path
                applyDetectionSettings(
                    extensionsTarget = screenStateSettings.fileShareScreenState.extensions,
                    matchersTarget = screenStateSettings.fileShareScreenState.matchers,
                    userSignaturesTarget = screenStateSettings.fileShareScreenState.userSignatures,
                    fastScanTarget = screenStateSettings.fileShareScreenState.fastScan,
                    extensions = replay.extensions,
                    matchers = replayMatchers,
                    userSignatures = replayUserSignatures,
                    fastScan = replay.fastScan
                )
                MainScreenConnector.FileShare
            }
        }

        settingsPanelOpened = false
        screenStateSettings.mainScreenSource = connectorToStorageValue(destination)
        screenStateSettings.save()
        navController.navigate(destination)
        TaskReplayHelper.clear()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val adaptiveTokens = rememberMainScreenAdaptiveTokens(maxWidth, maxHeight)
        val adaptiveScale = adaptiveTokens.scale
        val verticalScale = adaptiveTokens.verticalScale
        val mainCentralPanelTopInset = adaptiveScale.dp(MainCentralPanelTopInset, min = 186.dp, max = 272.dp)
        val mainPathBlockToRadioRowSpacing = adaptiveScale.dp(MainPathBlockToRadioRowSpacing, min = 0.dp, max = 8.dp)
        val mainCentralPanelTopLift = adaptiveScale.dp(MainCentralPanelTopLiftFromHeaderTightening, min = 16.dp, max = 30.dp)
        val mainScanSettingsPanelTopBelowRadios = adaptiveScale.dp(MainScanSettingsPanelTopBelowRadios, min = 154.dp, max = 230.dp)
        val mainLatestScansBlockMaxWidth = adaptiveScale.dp(MainLatestScansBlockMaxWidth, min = 1160.dp, max = 1620.dp)
        val mainContentOuterPaddingH = adaptiveScale.dp(MainContentOuterPaddingH, min = 20.dp, max = 36.dp)
        val mainContentInnerPaddingH = adaptiveScale.dp(MainContentInnerPaddingH, min = 16.dp, max = 28.dp)
        val contourStrokeWidth = adaptiveScale.dp(1.dp, min = 1.dp, max = 1.4.dp)
        val contourTopCompensation = adaptiveScale.dp(1.dp, min = 1.dp, max = 1.8.dp)
        val contourBranchDrop = adaptiveScale.dp(2.dp, min = 1.5.dp, max = 3.2.dp)
        val contourRightOpticalCompensation = adaptiveScale.dp(2.dp, min = 1.dp, max = 3.dp)
        val contourCornerRadius = adaptiveScale.dp(10.dp, min = 9.dp, max = 14.dp)
        val radioRowStartPadding = adaptiveScale.dp(12.dp, min = 10.dp, max = 20.dp)
        val sourceRowGap = adaptiveScale.dp(4.dp, min = 3.dp, max = 8.dp)

        val centralPanelTopPadding = when {
            settingsPanelOpened ->
                mainScanSettingsPanelTopBelowRadios - mainCentralPanelTopLift
            else ->
                mainCentralPanelTopInset + mainPathBlockToRadioRowSpacing - mainCentralPanelTopLift
        }
        val topActionBlockMaxWidth = mainLatestScansBlockMaxWidth + (mainContentInnerPaddingH * 2)
        val centralPanelMaxWidth = if (settingsPanelOpened) {
            topActionBlockMaxWidth
        } else {
            mainLatestScansBlockMaxWidth
        }

        CompositionLocalProvider(LocalMainScreenAdaptiveTokens provides adaptiveTokens) {
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
                .padding(horizontal = mainContentOuterPaddingH)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = adaptiveScale.dp(18.dp, min = 16.dp, max = 28.dp))
                    .widthIn(max = topActionBlockMaxWidth)
                    .fillMaxWidth()
                    .padding(
                        horizontal = mainContentInnerPaddingH,
                        vertical = adaptiveScale.dp(12.dp, min = 10.dp, max = 18.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            databaseContourContainerBounds = coordinates.boundsInRoot()
                        }
                        .drawBehind {
                            val containerBounds = databaseContourContainerBounds
                            val isSqlContour = sqlSourceActive && !settingsPanelOpened
                            val isS3Contour = s3SourceActive && !settingsPanelOpened
                            val radioRootBounds = when {
                                isSqlContour -> databaseRadioBounds
                                isS3Contour -> s3RadioBounds
                                else -> null
                            }
                            val underRootBounds = when {
                                isSqlContour -> databaseUnderBounds
                                isS3Contour -> s3UnderBounds
                                else -> null
                            }
                            if (
                                (isSqlContour || isS3Contour) &&
                                containerBounds != null &&
                                radioRootBounds != null &&
                                underRootBounds != null
                            ) {
                                val radioBounds = androidx.compose.ui.geometry.Rect(
                                    left = radioRootBounds.left - containerBounds.left,
                                    top = radioRootBounds.top - containerBounds.top,
                                    right = radioRootBounds.right - containerBounds.left,
                                    bottom = radioRootBounds.bottom - containerBounds.top
                                )
                                val underBounds = androidx.compose.ui.geometry.Rect(
                                    left = underRootBounds.left - containerBounds.left,
                                    top = underRootBounds.top - containerBounds.top,
                                    right = underRootBounds.right - containerBounds.left,
                                    bottom = underRootBounds.bottom - containerBounds.top
                                )
                                val strokeWidth = contourStrokeWidth.toPx()
                                val half = strokeWidth / 2f
                                val radioTopY = radioBounds.top + contourTopCompensation.toPx() + half
                                val minBranchY = radioBounds.bottom - half
                                val maxBranchY = (underBounds.top - half).coerceAtLeast(minBranchY)
                                val branchY = (radioBounds.bottom + contourBranchDrop.toPx()).coerceIn(minBranchY, maxBranchY)
                                val bridgeInset = 0.dp.toPx()
                                val radioCenterX = (radioBounds.left + radioBounds.right) / 2f
                                val branchHalfWidth = (radioBounds.width / 2f) + bridgeInset
                                val branchLeft = radioCenterX - branchHalfWidth
                                val rightOpticalCompensation = contourRightOpticalCompensation.toPx()
                                val branchRight = radioCenterX + branchHalfWidth + rightOpticalCompensation
                                val cornerRadius = contourCornerRadius.toPx()
                                val path = Path().apply {
                                    moveTo(branchLeft + half, radioTopY)
                                    lineTo(branchRight - half, radioTopY)
                                    lineTo(branchRight - half, branchY)
                                    lineTo(underBounds.right - half, branchY)
                                    lineTo(underBounds.right - half, underBounds.bottom - half)
                                    lineTo(underBounds.left + half, underBounds.bottom - half)
                                    lineTo(underBounds.left + half, branchY)
                                    lineTo(branchLeft + half, branchY)
                                    lineTo(branchLeft + half, radioTopY)
                                    close()
                                }
                                drawPath(
                                    path = path,
                                    color = databaseContourColor,
                                    style = Stroke(
                                        width = strokeWidth,
                                        join = StrokeJoin.Round,
                                        pathEffect = PathEffect.cornerPathEffect(cornerRadius)
                                    )
                                )
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(adaptiveScale.dp(6.dp, min = 5.dp, max = 10.dp))
                    ) {
                        bottomBarContent()
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(mainPathBlockToRadioRowSpacing),
                            horizontalAlignment = Alignment.Start,
                        ) {
                            SourceRadioRow(
                                navController = navController,
                                settingsOpen = settingsPanelOpened,
                                onSelectedLabelClick = {
                                    val nextOpened = !settingsPanelOpened
                                    settingsPanelOpened = nextOpened
                                    if (!nextOpened) {
                                        highlightMissingExtensions = false
                                        highlightMissingMatchers = false
                                    }
                                },
                                onSourceSwitch = { sourceToken ->
                                    settingsPanelOpened = false
                                    highlightMissingExtensions = false
                                    highlightMissingMatchers = false
                                    screenStateSettings.mainScreenSource = sourceToken
                                    screenStateSettings.save()
                                },
                                highlightDatabase = false,
                                highlightColor = databaseContourColor,
                                onS3ItemBoundsChanged = { s3RadioBounds = it },
                                onDatabaseItemBoundsChanged = { databaseRadioBounds = it },
                                sourceGap = sourceRowGap,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = radioRowStartPadding)
                            )
                        }
                        if (!settingsPanelOpened) {
                            Box(
                                modifier = if (sqlSourceActive || s3SourceActive) {
                                    Modifier
                                        .fillMaxWidth()
                                        .onGloballyPositioned { coordinates ->
                                            if (sqlSourceActive) {
                                                databaseUnderBounds = coordinates.boundsInRoot()
                                            } else if (s3SourceActive) {
                                                s3UnderBounds = coordinates.boundsInRoot()
                                            }
                                        }
                                        .padding(
                                            start = adaptiveScale.dp(1.dp, min = 1.dp, max = 2.dp),
                                            end = adaptiveScale.dp(1.dp, min = 1.dp, max = 2.dp),
                                            top = adaptiveScale.dp(1.dp, min = 1.dp, max = 2.dp),
                                            bottom = adaptiveScale.dp(3.dp, min = 2.dp, max = 5.dp)
                                        )
                                } else {
                                    Modifier.fillMaxWidth()
                                }
                            ) {
                                underSourceContent()
                            }
                        }
                    }
                }
            }
        }

        Surface(
            modifier = (if (settingsPanelOpened) {
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(
                        start = mainContentOuterPaddingH,
                        end = mainContentOuterPaddingH,
                        top = centralPanelTopPadding,
                        bottom = adaptiveScale.dp(8.dp, min = 6.dp, max = 14.dp)
                    )
                    .widthIn(max = centralPanelMaxWidth)
                    .fillMaxWidth()
                    .fillMaxHeight()
            } else {
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = mainContentOuterPaddingH,
                        end = mainContentOuterPaddingH,
                        bottom = verticalScale.dp(12.dp, min = 10.dp, max = 18.dp)
                    )
                    .widthIn(max = centralPanelMaxWidth)
                    .fillMaxWidth()
                    .border(
                        width = contourStrokeWidth,
                        color = colorScheme.outlineVariant.copy(alpha = 0.62f),
                        shape = RoundedCornerShape(adaptiveScale.dp(16.dp, min = 14.dp, max = 24.dp))
                    )
                    .wrapContentHeight()
            }).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { focusManager.clearFocus() },
            shape = RoundedCornerShape(adaptiveScale.dp(16.dp, min = 14.dp, max = 24.dp)),
            color = if (settingsPanelOpened) Color.Transparent else colorScheme.surfaceVariant.copy(alpha = 0.28f),
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            // Ensure identical inner content frame for settings/recent scans
            Box(
                modifier = if (settingsPanelOpened) {
                    Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = mainContentInnerPaddingH,
                            vertical = adaptiveScale.dp(8.dp, min = 6.dp, max = 14.dp)
                        )
                } else {
                    Modifier
                        .wrapContentHeight()
                        .padding(
                            horizontal = mainContentInnerPaddingH,
                            vertical = verticalScale.dp(8.dp, min = 6.dp, max = 12.dp)
                        )
                }
            ) {
                if (settingsPanelOpened) {
                    SettingsBox(
                        transition = updateTransition(targetState = true, label = "settings-inline"),
                        allowExtensionsEditing = !sqlSourceActive,
                        highlightMissingExtensions = highlightMissingExtensions,
                        highlightMissingMatchers = highlightMissingMatchers
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

        // Keep subscreen-specific state and bottom bar logic active.
        Box(modifier = Modifier.size(0.dp)) {
        val initialConnector = remember(screenStateSettings.mainScreenSource) {
            storageValueToConnector(screenStateSettings.mainScreenSource)
        }
        NavHost(
            navController = navController,
            startDestination = initialConnector
        ) {
            composable<MainScreenConnector.FileShare> {
                FileShareScreen(
                    navController = navController,
                    expandScanState = { taskId -> showScan(taskId) },
                    onRequireScanSettings = { missingExtensions, missingMatchers ->
                        settingsPanelOpened = true
                        highlightMissingExtensions = missingExtensions
                        highlightMissingMatchers = missingMatchers
                    },
                    onRequireSourceInputs = {
                        settingsPanelOpened = false
                        highlightMissingExtensions = false
                        highlightMissingMatchers = false
                    },
                    setSidebarContent = { },
                    setBottomBarContent = { content ->
                        if (fileShareSourceActive) {
                            bottomBarContent = content
                        }
                    },
                    setUnderSourceContent = { content ->
                        if (fileShareSourceActive) {
                            underSourceContent = content
                        }
                    }
                )
            }
            composable<MainScreenConnector.S3> {
                S3Screen(
                    navController = navController,
                    expandScanState = { taskId -> showScan(taskId) },
                    onRequireScanSettings = { missingExtensions, missingMatchers ->
                        settingsPanelOpened = true
                        highlightMissingExtensions = missingExtensions
                        highlightMissingMatchers = missingMatchers
                    },
                    onRequireSourceInputs = {
                        settingsPanelOpened = false
                        highlightMissingExtensions = false
                        highlightMissingMatchers = false
                    },
                    setSidebarContent = { },
                    setBottomBarContent = { content ->
                        if (s3SourceActive) {
                            bottomBarContent = content
                        }
                    },
                    setUnderSourceContent = { content ->
                        if (s3SourceActive) {
                            underSourceContent = content
                        }
                    }
                )
            }
            composable<MainScreenConnector.HTTP> {
                HTTPScreen(
                    navController = navController,
                    expandScanState = { taskId -> showScan(taskId) },
                    onRequireScanSettings = { missingExtensions, missingMatchers ->
                        settingsPanelOpened = true
                        highlightMissingExtensions = missingExtensions
                        highlightMissingMatchers = missingMatchers
                    },
                    onRequireSourceInputs = {
                        settingsPanelOpened = false
                        highlightMissingExtensions = false
                        highlightMissingMatchers = false
                    },
                    setSidebarContent = { },
                    setBottomBarContent = { content ->
                        if (httpSourceActive) {
                            bottomBarContent = content
                        }
                    },
                    setUnderSourceContent = { content ->
                        if (httpSourceActive) {
                            underSourceContent = content
                        }
                    }
                )
            }
            composable<MainScreenConnector.Postgres> {
                DatabaseScreen(
                    expandScanState = { taskId -> showScan(taskId) },
                    onRequireScanSettings = { missingExtensions, missingMatchers ->
                        settingsPanelOpened = true
                        highlightMissingExtensions = missingExtensions
                        highlightMissingMatchers = missingMatchers
                    },
                    onRequireSourceInputs = {
                        settingsPanelOpened = false
                        highlightMissingExtensions = false
                        highlightMissingMatchers = false
                    },
                    setSidebarContent = { },
                    setBottomBarContent = { content ->
                        if (sqlSourceActive) {
                            bottomBarContent = content
                        }
                    },
                    setUnderSourceContent = { content ->
                        if (sqlSourceActive) {
                            underSourceContent = content
                        }
                    }
                )
            }
        }
        }
    }
}
}
}

@Composable
private fun SourceRadioRow(
    navController: androidx.navigation.NavController,
    settingsOpen: Boolean,
    highlightDatabase: Boolean,
    highlightColor: Color,
    onS3ItemBoundsChanged: (androidx.compose.ui.geometry.Rect?) -> Unit = {},
    onDatabaseItemBoundsChanged: (androidx.compose.ui.geometry.Rect?) -> Unit = {},
    sourceGap: Dp = 4.dp,
    modifier: Modifier = Modifier,
    onSelectedLabelClick: () -> Unit,
    onSourceSwitch: (String) -> Unit,
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
        horizontalArrangement = Arrangement.spacedBy(sourceGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SourceRadioItem(
            selected = selectedRoute == MainScreenConnector.FileShare,
            label = "File Share",
            settingsOpen = settingsOpen,
            onSelect = {
                if (selectedRoute != MainScreenConnector.FileShare) {
                    onSourceSwitch("fileshare")
                    navController.navigate(MainScreenConnector.FileShare)
                }
            },
            onSelectedLabelClick = onSelectedLabelClick
        )
        Box(
            modifier = Modifier.onGloballyPositioned { coordinates ->
                onS3ItemBoundsChanged(coordinates.boundsInRoot())
            }
        ) {
            SourceRadioItem(
                selected = selectedRoute == MainScreenConnector.S3,
                label = "AWS S3",
                settingsOpen = settingsOpen,
                onSelect = {
                    if (selectedRoute != MainScreenConnector.S3) {
                        onSourceSwitch("s3")
                        navController.navigate(MainScreenConnector.S3)
                    }
                },
                onSelectedLabelClick = onSelectedLabelClick
            )
        }
        SourceRadioItem(
            selected = selectedRoute == MainScreenConnector.HTTP,
            label = "HTTP",
            settingsOpen = settingsOpen,
            onSelect = {
                if (selectedRoute != MainScreenConnector.HTTP) {
                    onSourceSwitch("http")
                    navController.navigate(MainScreenConnector.HTTP)
                }
            },
            onSelectedLabelClick = onSelectedLabelClick
        )
        Box(
            modifier = if (highlightDatabase) {
                Modifier
                    .border(
                        width = 1.dp,
                        color = highlightColor,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            } else {
                Modifier
            }
                .onGloballyPositioned { coordinates ->
                    onDatabaseItemBoundsChanged(coordinates.boundsInRoot())
                }
        ) {
            SourceRadioItem(
                selected = selectedRoute == MainScreenConnector.Postgres,
                label = "Database",
                settingsOpen = settingsOpen,
                onSelect = {
                    if (selectedRoute != MainScreenConnector.Postgres) {
                        onSourceSwitch("database")
                        navController.navigate(MainScreenConnector.Postgres)
                    }
                },
                onSelectedLabelClick = onSelectedLabelClick
            )
        }
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
    val labelInteractionSource = remember { MutableInteractionSource() }
    val labelHovered by labelInteractionSource.collectIsHoveredAsState()
    val radioInteractionSource = remember { MutableInteractionSource() }
    val underline = selected && (labelHovered || settingsOpen)
    val color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
        alpha = when {
            selected -> 0.95f
            labelHovered -> 0.88f
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
                .padding(horizontal = 2.dp, vertical = 0.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Box(
                modifier = Modifier
                    .pointerHoverIcon(PointerIcon.Hand)
                    .clickable(
                        interactionSource = radioInteractionSource,
                        indication = null
                    ) {
                        if (!selected) onSelect()
                    }
            ) {
                RadioButton(
                    selected = selected,
                    onClick = null,
                    modifier = Modifier.scale(0.82f),
                    colors = radioColors
                )
            }
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .padding(start = 1.dp, top = 2.dp, bottom = 2.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = label,
                    modifier = Modifier
                        .hoverable(labelInteractionSource)
                        .pointerHoverIcon(PointerIcon.Hand)
                        .clickable(
                            interactionSource = labelInteractionSource,
                            indication = null,
                            onClick = {
                                if (selected) onSelectedLabelClick() else onSelect()
                            }
                        ),
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

    if (selected) {
        DescriptionTooltip(
            description = stringResource(
                if (settingsOpen) {
                    Res.string.MainScreen_SourceLabel_ScanSettingsTooltip
                } else {
                    Res.string.MainScreen_SourceLabel_LatestScansTooltip
                }
            ),
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
    val adaptiveScale = LocalMainScreenAdaptiveTokens.current.scale
    val verticalScale = LocalMainScreenAdaptiveTokens.current.verticalScale
    val allTasks by scanService.tasks.tasks.collectAsState()
    var statusFilter by remember { mutableStateOf(StatusFilter.ALL) }
    val coroutineScope = rememberCoroutineScope()

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
        verticalArrangement = Arrangement.spacedBy(verticalScale.dp(8.dp, min = 6.dp, max = 12.dp))
    ) {
        Text(
            text = stringResource(Res.string.MainScreen_RecentScans_Title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (visibleTasks.isEmpty()) {
            // Do not use fillMaxSize here: the parent Column gets the window max height, and an
            // expanding empty box would stretch the bottom Surface over the whole main area and
            // block clicks on path / source controls.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = verticalScale.dp(20.dp, min = 16.dp, max = 28.dp)),
                contentAlignment = Alignment.Center
            ) {
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
            val recentScansListHeight = verticalScale.dp(280.dp, min = 260.dp, max = 360.dp)
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = recentScansListHeight),
                verticalArrangement = Arrangement.spacedBy(verticalScale.dp(6.dp, min = 5.dp, max = 9.dp))
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
                        },
                        onRescanClick = {
                            coroutineScope.launch {
                                val newTask = scanService.startNewScanFromTask(task)
                                newTask.id.value?.let { onTaskClick(it) }
                            }
                        },
                        onEditAndRunClick = {
                            coroutineScope.launch {
                                TaskReplayHelper.set(scanService.snapshotTaskReplaySettings(task))
                            }
                        }
                    )
                }
            }
            TextButton(
                onClick = onViewAllClick,
                contentPadding = PaddingValues(
                    horizontal = adaptiveScale.dp(10.dp, min = 8.dp, max = 16.dp),
                    vertical = verticalScale.dp(4.dp, min = 3.dp, max = 6.dp)
                )
            ) {
                Text(
                    text = stringResource(Res.string.MainScreen_RecentScans_ViewFullHistory),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}
