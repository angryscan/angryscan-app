package org.angryscan.app.ui.windows.screens.main.subscreens

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.launch
import org.angryscan.app.common.*
import org.angryscan.app.resources.*
import org.angryscan.app.scan.ScanService
import org.angryscan.app.scan.common.connectors.*
import org.angryscan.app.ui.hasSelectedMatchersForScan
import org.angryscan.app.ui.windows.components.DescriptionTooltip
import org.angryscan.app.ui.windows.screens.main.components.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun DatabaseScreen(
    expandScanState: (Int) -> Unit,
    onRequireScanSettings: (missingExtensions: Boolean, missingMatchers: Boolean) -> Unit = { _, _ -> },
    onRequireSourceInputs: () -> Unit = {},
    setSidebarContent: (@Composable () -> Unit) -> Unit = {},
    setBottomBarContent: (@Composable () -> Unit) -> Unit = {},
    setUnderSourceContent: (@Composable () -> Unit) -> Unit = {},
    onSqlConnectionError: () -> Unit = {},
    showErrorSnackbar: (String) -> Unit = {}
) {
    val scanService = koinInject<ScanService>()
    val scanSettings = koinInject<ScanSettings>()
    val screenStateSettings = koinInject<ScreenStateSettings>()
    val savedConnectionsRepository = koinInject<SavedSqlConnectionsRepository>()
    var sqlScreenState by remember { screenStateSettings.sqlScreenState }

    var highlightedConnectionFields by remember { mutableStateOf<Set<DatabaseConnectionRequiredField>>(emptySet()) }
    var validationErrorDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    val postgresConnectionErrorMessage = stringResource(Res.string.Validation_PostgresConnectionMessage)
    val postgresConnectionValidMessage = stringResource(Res.string.ScanSettings_PostgresConnectionSuccess)
    val connectionNameRequiredMessage = stringResource(Res.string.MainScreen_ConnectionNameRequired)

    val coroutineScope = rememberCoroutineScope()
    var sqlConnectionTestInProgress by remember { mutableStateOf(false) }
    var sqlConnectionTestSuccessful by remember { mutableStateOf(false) }
    var sqlConnectionTestMessage by remember { mutableStateOf<String?>(null) }
    var savedConnectionsExpanded by remember { mutableStateOf(false) }
    var selectedSavedConnectionKey by remember { mutableStateOf<String?>(null) }
    var pendingDeleteConnection by remember { mutableStateOf<SavedSqlConnection?>(null) }
    var pendingConnectionNameDialog by remember { mutableStateOf(false) }
    var pendingConnectionName by remember { mutableStateOf("") }
    var pendingConnectionNameError by remember { mutableStateOf(false) }
    var savedForCurrentType by remember { mutableStateOf<List<SavedSqlConnection>>(emptyList()) }

    LaunchedEffect(highlightedConnectionFields) {
        if (highlightedConnectionFields.isNotEmpty()) {
            val snapshot = highlightedConnectionFields
            kotlinx.coroutines.delay(2000)
            if (highlightedConnectionFields == snapshot) {
                highlightedConnectionFields = emptySet()
            }
        }
    }
    val connectionFieldBorderColor = when {
        sqlConnectionTestSuccessful -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
        sqlConnectionTestMessage != null -> MaterialTheme.colorScheme.error.copy(alpha = 0.9f)
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)
    }
    fun markSqlConnectionDirty() {
        sqlConnectionTestSuccessful = false
        sqlConnectionTestMessage = null
        selectedSavedConnectionKey = null
    }

    fun applySavedConnection(conn: SavedSqlConnection) {
        coroutineScope.launch {
            val password = savedConnectionsRepository.getPassword(conn.connectionKey).orEmpty()
            sqlScreenState = sqlScreenState.copy(
                databaseType = conn.databaseType,
                host = conn.host,
                port = conn.port,
                database = conn.database,
                schema = conn.schema,
                user = conn.user,
                password = password
            )
            sqlConnectionTestSuccessful = false
            sqlConnectionTestMessage = null
            selectedSavedConnectionKey = conn.connectionKey
            screenStateSettings.save()
        }
    }

    fun databaseTypeLabel(databaseType: DatabaseType): String = when (databaseType) {
        DatabaseType.PostgreSQL -> "PostgreSQL"
        DatabaseType.MySQL -> "MySQL"
        DatabaseType.SQLite -> "SQLite"
        DatabaseType.GreenPlum -> "GreenPlum"
        DatabaseType.Hive -> "Hive"
        DatabaseType.CockroachDB -> "CockroachDB"
        DatabaseType.ClickHouse -> "ClickHouse"
    }

    fun savedConnectionLabel(conn: SavedSqlConnection): String = buildString {
        if (conn.name.isNotBlank()) {
            append("${conn.name} — ")
        }
        append("${conn.host}:${conn.port}")
        if (conn.database.isNotBlank()) append(" · ${conn.database}")
        if (conn.schema.isNotBlank()) append(" · ${conn.schema}")
        if (conn.user.isNotBlank()) append(" · ${conn.user}")
    }

    fun savedConnectionUrl(conn: SavedSqlConnection): String {
        val databasePart = conn.database.trim()
        return if (databasePart.isNotBlank()) {
            "${conn.host}:${conn.port}/$databasePart"
        } else {
            "${conn.host}:${conn.port}"
        }
    }

    fun savedConnectionPrimary(conn: SavedSqlConnection): String =
        conn.name.trim().ifBlank { savedConnectionUrl(conn) }

    fun savedConnectionSecondary(conn: SavedSqlConnection): String =
        "url: ${savedConnectionUrl(conn)}"

    fun savedConnectionTertiary(conn: SavedSqlConnection): String {
        val parts = buildList {
            if (conn.schema.isNotBlank()) add("schema: ${conn.schema}")
            if (conn.user.isNotBlank()) add("user: ${conn.user}")
        }
        return parts.joinToString(" · ")
    }

    fun defaultConnectionName(): String {
        val hostPart = sqlScreenState.host.trim().ifBlank { "connection" }
        val databasePart = sqlScreenState.database.trim()
        return if (databasePart.isNotBlank()) "$hostPart/$databasePart" else hostPart
    }

    fun refreshSavedConnections() {
        coroutineScope.launch {
            savedForCurrentType = scanService.withHistoryBatchesPaused {
                savedConnectionsRepository.list(sqlScreenState.databaseType)
            }
        }
    }

    fun saveCurrentSqlConnection(connectionName: String? = null) {
        if (sqlScreenState.databaseType == DatabaseType.SQLite) return
        val finalName = connectionName?.trim()?.takeIf { it.isNotBlank() } ?: defaultConnectionName()
        coroutineScope.launch {
            scanService.withHistoryBatchesPaused {
                val key = savedConnectionsRepository.upsert(
                    name = finalName,
                    databaseType = sqlScreenState.databaseType,
                    host = sqlScreenState.host,
                    port = sqlScreenState.port,
                    database = sqlScreenState.database,
                    schema = sqlScreenState.schema,
                    user = sqlScreenState.user,
                    password = sqlScreenState.password
                )
                selectedSavedConnectionKey = key
            }
            refreshSavedConnections()
        }
    }

    fun removeSavedConnection(conn: SavedSqlConnection) {
        coroutineScope.launch {
            scanService.withHistoryBatchesPaused {
                savedConnectionsRepository.remove(conn.connectionKey)
            }
            if (selectedSavedConnectionKey == conn.connectionKey) {
                selectedSavedConnectionKey = null
            }
            refreshSavedConnections()
            if (savedForCurrentType.isEmpty()) {
                savedConnectionsExpanded = false
            }
        }
    }

    LaunchedEffect(Unit) {
        if (screenStateSettings.sqlSavedConnections.isNotEmpty()) {
            scanService.withHistoryBatchesPaused {
                savedConnectionsRepository.migrateFromLegacy(screenStateSettings.sqlSavedConnections.toList())
            }
            screenStateSettings.sqlSavedConnections.clear()
            screenStateSettings.save()
        }
        refreshSavedConnections()
    }

    LaunchedEffect(sqlScreenState.databaseType) {
        refreshSavedConnections()
    }

    fun testCurrentSqlConnection() {
        sqlConnectionTestInProgress = true
        sqlConnectionTestSuccessful = false
        sqlConnectionTestMessage = null
        coroutineScope.launch {
            val validationError = DatabaseConnectionValidator.validate(
                databaseType = sqlScreenState.databaseType,
                host = sqlScreenState.host,
                port = sqlScreenState.connectionPort(),
                database = sqlScreenState.database,
                user = sqlScreenState.user,
                password = sqlScreenState.password,
                filePath = sqlScreenState.filePath
            )
            sqlConnectionTestInProgress = false
            if (validationError == null) {
                sqlConnectionTestSuccessful = true
                sqlConnectionTestMessage = postgresConnectionValidMessage
            } else {
                sqlConnectionTestSuccessful = false
                sqlConnectionTestMessage = postgresConnectionErrorMessage
            }
        }
    }

    val filePickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("db")),
        mode = FileKitMode.Single,
        title = "Select SQLite database"
    ) { result ->
        result?.path?.let { path ->
            val updated = sqlScreenState.copy(filePath = path)
            sqlScreenState = updated
            highlightedConnectionFields =
                updated.updatedHighlightedConnectionFields(highlightedConnectionFields)
            markSqlConnectionDirty()
            coroutineScope.launch { screenStateSettings.save() }
        }
    }

    setSidebarContent { }
    setBottomBarContent {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val controlHeight = 68.dp
            val controlShape = RoundedCornerShape(18.dp)
            val scanButtonWidth = when {
                maxWidth >= 1500.dp -> 240.dp
                maxWidth < 1200.dp -> 220.dp
                else -> 232.dp
            } * 0.75f
            val controlGap = if (maxWidth < 1200.dp) 8.dp else 12.dp
            val pathMinWidth = if (maxWidth < 1200.dp) 460.dp else 500.dp

            val startSqlScan: () -> Unit = startSqlScan@{
                val missingMatchers = !hasSelectedMatchersForScan(scanSettings)
                if (missingMatchers) {
                    highlightedConnectionFields = emptySet()
                    onRequireScanSettings(false, true)
                    return@startSqlScan
                }
                val missingConnectionFields = sqlScreenState.missingRequiredConnectionFields()
                highlightedConnectionFields = missingConnectionFields
                if (missingConnectionFields.isNotEmpty()) {
                    onRequireSourceInputs()
                    return@startSqlScan
                }

                coroutineScope.launch {
                    val connectionError = DatabaseConnectionValidator.validate(
                        databaseType = sqlScreenState.databaseType,
                        host = sqlScreenState.host,
                        port = sqlScreenState.connectionPort(),
                        database = sqlScreenState.database,
                        user = sqlScreenState.user,
                        password = sqlScreenState.password,
                        filePath = sqlScreenState.filePath
                    )
                    if (connectionError != null) {
                        onSqlConnectionError()
                        showErrorSnackbar(postgresConnectionErrorMessage)
                        return@launch
                    }
                    val connector = when (sqlScreenState.databaseType) {
                        DatabaseType.PostgreSQL -> ConnectorPostgres(
                            host = sqlScreenState.host,
                            port = sqlScreenState.connectionPort(),
                            database = sqlScreenState.database,
                            user = sqlScreenState.user,
                            password = sqlScreenState.password
                        )
                        DatabaseType.MySQL -> ConnectorMySQL(
                            host = sqlScreenState.host,
                            port = sqlScreenState.connectionPort(),
                            database = sqlScreenState.database,
                            user = sqlScreenState.user,
                            password = sqlScreenState.password
                        )
                        DatabaseType.GreenPlum -> ConnectorGreenPlum(
                            host = sqlScreenState.host,
                            port = sqlScreenState.connectionPort(),
                            database = sqlScreenState.database,
                            user = sqlScreenState.user,
                            password = sqlScreenState.password
                        )
                        DatabaseType.Hive -> ConnectorHive(
                            host = sqlScreenState.host,
                            port = sqlScreenState.connectionPort(),
                            database = sqlScreenState.database,
                            user = sqlScreenState.user,
                            password = sqlScreenState.password
                        )
                        DatabaseType.CockroachDB -> ConnectorCockroachDB(
                            host = sqlScreenState.host,
                            port = sqlScreenState.connectionPort(),
                            database = sqlScreenState.database,
                            user = sqlScreenState.user,
                            password = sqlScreenState.password
                        )
                        DatabaseType.ClickHouse -> ConnectorClickHouse(
                            host = sqlScreenState.host,
                            port = sqlScreenState.connectionPort(),
                            database = sqlScreenState.database,
                            user = sqlScreenState.user,
                            password = sqlScreenState.password
                        )
                        DatabaseType.SQLite -> ConnectorSqlite(
                            filePath = sqlScreenState.filePath
                        )
                    }
                    val taskName = when (sqlScreenState.databaseType) {
                        DatabaseType.PostgreSQL, DatabaseType.MySQL, DatabaseType.GreenPlum, DatabaseType.Hive, DatabaseType.CockroachDB, DatabaseType.ClickHouse ->
                            "${sqlScreenState.host}:${sqlScreenState.port}/${sqlScreenState.database}" +
                                if (sqlScreenState.schema.isNotEmpty()) " schema: ${sqlScreenState.schema}" else ""
                        DatabaseType.SQLite -> sqlScreenState.filePath
                    }
                    val path = when (sqlScreenState.databaseType) {
                        DatabaseType.PostgreSQL, DatabaseType.MySQL, DatabaseType.GreenPlum, DatabaseType.Hive, DatabaseType.CockroachDB, DatabaseType.ClickHouse -> sqlScreenState.schema
                        DatabaseType.SQLite -> ""
                    }
                    val task = scanService.createTask(
                        name = taskName,
                        path = path,
                        extensions = scanSettings.extensions,
                        matchers = scanSettings.matchers + scanSettings.userSignatures,
                        fastScan = scanSettings.fastScan.value,
                        connector = connector
                    )
                    scanService.startTask(task)
                    task.id.value?.let { expandScanState(it) }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(controlGap, Alignment.Start)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = pathMinWidth)
                        .height(controlHeight),
                    shape = controlShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    tonalElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (sqlScreenState.databaseType) {
                            DatabaseType.PostgreSQL, DatabaseType.MySQL, DatabaseType.GreenPlum, DatabaseType.Hive, DatabaseType.CockroachDB, DatabaseType.ClickHouse -> {
                                OutlinedTextField(
                                    value = sqlScreenState.host,
                                    onValueChange = {
                                        val updated = sqlScreenState.copy(host = it)
                                        sqlScreenState = updated
                                        highlightedConnectionFields =
                                            updated.updatedHighlightedConnectionFields(highlightedConnectionFields)
                                        markSqlConnectionDirty()
                                        coroutineScope.launch { screenStateSettings.save() }
                                    },
                                    modifier = Modifier.weight(0.4f).heightIn(min = 40.dp),
                                    placeholder = { Text("Host", style = MaterialTheme.typography.bodyMedium) },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    isError = DatabaseConnectionRequiredField.HOST in highlightedConnectionFields,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = connectionFieldBorderColor,
                                        unfocusedBorderColor = connectionFieldBorderColor,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                        errorBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                )
                                OutlinedTextField(
                                    value = sqlScreenState.port,
                                    onValueChange = {
                                        val updated = sqlScreenState.copy(port = it)
                                        sqlScreenState = updated
                                        highlightedConnectionFields =
                                            updated.updatedHighlightedConnectionFields(highlightedConnectionFields)
                                        markSqlConnectionDirty()
                                        coroutineScope.launch { screenStateSettings.save() }
                                    },
                                    modifier = Modifier.weight(0.2f).heightIn(min = 40.dp),
                                    placeholder = { Text("Port", style = MaterialTheme.typography.bodyMedium) },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    isError = DatabaseConnectionRequiredField.PORT in highlightedConnectionFields,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = connectionFieldBorderColor,
                                        unfocusedBorderColor = connectionFieldBorderColor,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                        errorBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                )
                                OutlinedTextField(
                                    value = sqlScreenState.database,
                                    onValueChange = {
                                        val updated = sqlScreenState.copy(database = it)
                                        sqlScreenState = updated
                                        highlightedConnectionFields =
                                            updated.updatedHighlightedConnectionFields(highlightedConnectionFields)
                                        markSqlConnectionDirty()
                                        coroutineScope.launch { screenStateSettings.save() }
                                    },
                                    modifier = Modifier.weight(0.4f).heightIn(min = 40.dp),
                                    placeholder = { Text("Database", style = MaterialTheme.typography.bodyMedium) },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    isError = DatabaseConnectionRequiredField.DATABASE in highlightedConnectionFields,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = connectionFieldBorderColor,
                                        unfocusedBorderColor = connectionFieldBorderColor,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                        errorBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                )
                            }
                            DatabaseType.SQLite -> {
                                OutlinedTextField(
                                    value = sqlScreenState.filePath,
                                    onValueChange = {
                                        val updated = sqlScreenState.copy(filePath = it)
                                        sqlScreenState = updated
                                        highlightedConnectionFields =
                                            updated.updatedHighlightedConnectionFields(highlightedConnectionFields)
                                        markSqlConnectionDirty()
                                        coroutineScope.launch { screenStateSettings.save() }
                                    },
                                    modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                                    placeholder = { Text("Path to .db file", style = MaterialTheme.typography.bodyMedium) },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    isError = DatabaseConnectionRequiredField.FILE_PATH in highlightedConnectionFields,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                        errorBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                )
                                IconButton(onClick = { filePickerLauncher.launch() }) {
                                    Icon(
                                        imageVector = Icons.Outlined.FileOpen,
                                        contentDescription = "Select file",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                Button(
                    enabled = true,
                    onClick = { startSqlScan() },
                    modifier = ScanButtonModifier(
                        isReady = true,
                        modifier = Modifier
                            .width(scanButtonWidth)
                            .height(controlHeight)
                    ).scanButtonHoverFeedback(enabled = true).scanButtonChipBorder(),
                    shape = controlShape,
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp,
                        disabledElevation = 0.dp
                    ),
                    colors = startScanButtonColors()
                ) {
                    StartScanButtonContent()
                }
            }
        }
    }

    ScanValidationErrorDialog(
        validationError = validationErrorDialog,
        onDismiss = { validationErrorDialog = null }
    )
    pendingDeleteConnection?.let { connToDelete ->
        AlertDialog(
            onDismissRequest = { pendingDeleteConnection = null },
            title = {
                Text(
                    stringResource(
                        Res.string.MainScreen_DeleteSavedConnectionTypeTitle,
                        databaseTypeLabel(connToDelete.databaseType)
                    )
                )
            },
            text = { Text(savedConnectionLabel(connToDelete)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        removeSavedConnection(connToDelete)
                        pendingDeleteConnection = null
                    }
                ) {
                    Text(stringResource(Res.string.ScanSettings_Profiles_Delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteConnection = null }) {
                    Text(stringResource(Res.string.Common_Cancel))
                }
            }
        )
    }
    if (pendingConnectionNameDialog) {
        AlertDialog(
            onDismissRequest = { pendingConnectionNameDialog = false },
            title = { Text(stringResource(Res.string.MainScreen_ConnectionNameTitle)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = pendingConnectionName,
                        onValueChange = {
                            pendingConnectionName = it
                            if (pendingConnectionNameError && it.isNotBlank()) {
                                pendingConnectionNameError = false
                            }
                        },
                        placeholder = { Text(stringResource(Res.string.MainScreen_ConnectionNamePlaceholder)) },
                        singleLine = true,
                        isError = pendingConnectionNameError,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pendingConnectionNameError) {
                        Text(
                            text = connectionNameRequiredMessage,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val trimmed = pendingConnectionName.trim()
                        if (trimmed.isBlank()) {
                            pendingConnectionNameError = true
                        } else {
                            saveCurrentSqlConnection(connectionName = trimmed)
                            pendingConnectionNameDialog = false
                        }
                    }
                ) {
                    Text(stringResource(Res.string.Common_Save))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConnectionNameDialog = false }) {
                    Text(stringResource(Res.string.Common_Cancel))
                }
            }
        )
    }
    if (savedConnectionsExpanded) {
        Dialog(onDismissRequest = { savedConnectionsExpanded = false }) {
            val dialogScroll = rememberScrollState()
            val cs = MaterialTheme.colorScheme
            val dialogContainerColor = cs.surfaceVariant
            val dialogBorderColor = cs.outlineVariant.copy(alpha = 0.56f)
            val dialogDividerColor = cs.outlineVariant.copy(alpha = 0.34f)
            val selectedRowColor = cs.primaryContainer.copy(alpha = 0.18f)
            val selectedTextColor = cs.primary
            Surface(
                shape = RoundedCornerShape(14.dp),
                tonalElevation = 3.dp,
                color = dialogContainerColor,
                border = BorderStroke(
                    width = 1.dp,
                    color = dialogBorderColor
                ),
                modifier = Modifier.widthIn(min = 440.dp, max = 580.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(Res.string.MainScreen_SavedConnections, savedForCurrentType.size),
                            style = MaterialTheme.typography.titleSmall,
                            color = cs.onSurface
                        )
                        TextButton(
                            onClick = { savedConnectionsExpanded = false },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.Common_Cancel),
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurfaceVariant
                            )
                        }
                    }
                    HorizontalDivider(color = dialogDividerColor)
                    if (savedForCurrentType.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.MainScreen_NoSavedConnections),
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 320.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(end = 10.dp)
                                    .verticalScroll(dialogScroll),
                                verticalArrangement = Arrangement.spacedBy(0.dp)
                            ) {
                                savedForCurrentType.forEachIndexed { index, conn ->
                                    val title = savedConnectionLabel(conn)
                                    val connectionKey = conn.connectionKey
                                    val isSelected = connectionKey == selectedSavedConnectionKey
                                    DescriptionTooltip(description = title, delay = 250) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(
                                                    if (isSelected) {
                                                        selectedRowColor
                                                    } else {
                                                        Color.Transparent
                                                    },
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .clickable {
                                                    applySavedConnection(conn)
                                                    savedConnectionsExpanded = false
                                                }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (isSelected) {
                                                    Icons.Outlined.RadioButtonChecked
                                                } else {
                                                    Icons.Outlined.RadioButtonUnchecked
                                                },
                                                contentDescription = null,
                                                tint = if (isSelected) {
                                                    selectedTextColor
                                                } else {
                                                    cs.onSurfaceVariant.copy(alpha = 0.75f)
                                                },
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(1.dp)
                                            ) {
                                                Text(
                                                    text = savedConnectionPrimary(conn),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = if (isSelected) {
                                                        selectedTextColor
                                                    } else {
                                                        cs.onSurface
                                                    }
                                                )
                                                Text(
                                                    text = savedConnectionSecondary(conn),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = cs.onSurfaceVariant
                                                )
                                                Text(
                                                    text = savedConnectionTertiary(conn).ifBlank { " " },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = cs.onSurfaceVariant
                                                )
                                            }
                                            IconButton(
                                                onClick = { pendingDeleteConnection = conn },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Close,
                                                    contentDescription = "Delete saved connection",
                                                    tint = cs.onSurfaceVariant.copy(alpha = 0.85f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                    }
                                    if (index < savedForCurrentType.lastIndex) {
                                        HorizontalDivider(
                                            color = cs.outlineVariant.copy(alpha = 0.24f),
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                    }
                                }
                            }
                            VerticalScrollbar(
                                adapter = rememberScrollbarAdapter(dialogScroll),
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .fillMaxHeight()
                                    .width(8.dp),
                                style = LocalScrollbarStyle.current.copy(
                                    unhoverColor = cs.outlineVariant.copy(alpha = 0.5f),
                                    hoverColor = cs.primary.copy(alpha = 0.85f)
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    // SQL connection params should appear under the source radio buttons (same UX as AWS S3).
    setUnderSourceContent {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val controlGap = if (maxWidth < 1200.dp) 8.dp else 12.dp
            val scanButtonWidth = when {
                maxWidth >= 1500.dp -> 240.dp
                maxWidth < 1200.dp -> 220.dp
                else -> 232.dp
            } * 0.75f
            val pathMinWidth = if (maxWidth < 1200.dp) 460.dp else 500.dp
            val blockMinWidth = pathMinWidth + controlGap + scanButtonWidth
            val inlineContentHorizontalPadding = 12.dp
            val inlineControlsGap = 10.dp
            val compactSize = MaterialTheme.typography.bodySmall.fontSize
            val compactLineHeight = MaterialTheme.typography.bodySmall.lineHeight
            val fieldTextStyle = MaterialTheme.typography.bodyMedium.copy(
                fontSize = compactSize,
                lineHeight = compactLineHeight
            )
            val placeholderStyle = fieldTextStyle.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )

            @Composable
            fun CompactField(
                value: String,
                onValueChange: (String) -> Unit,
                placeholder: String,
                modifier: Modifier,
                isError: Boolean = false,
                isPassword: Boolean = false,
                visualTransformation: VisualTransformation = VisualTransformation.None,
            ) {
                var passwordVisible by remember { mutableStateOf(false) }
                val effectiveVisualTransformation = when {
                    isPassword && passwordVisible -> VisualTransformation.None
                    isPassword -> PasswordVisualTransformation()
                    else -> visualTransformation
                }
                val shape = RoundedCornerShape(10.dp)
                Surface(
                    modifier = modifier
                        .height(32.dp)
                        .border(
                            width = 1.dp,
                            color = if (isError) {
                                MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                            } else {
                                connectionFieldBorderColor
                            },
                            shape = shape
                        ),
                    shape = shape,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.35f),
                    tonalElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.isBlank()) {
                            Text(
                                text = placeholder,
                                style = placeholderStyle,
                                maxLines = 1
                            )
                        }
                        BasicTextField(
                            value = value,
                            onValueChange = onValueChange,
                            singleLine = true,
                            textStyle = fieldTextStyle.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            visualTransformation = effectiveVisualTransformation,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = if (isPassword) 24.dp else 0.dp)
                        )
                        if (isPassword) {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible },
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .size(20.dp)
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            @Composable
            fun ConnectionActionButtons(
                canTest: Boolean,
                showAddConnection: Boolean
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        enabled = canTest && !sqlConnectionTestInProgress,
                        onClick = { testCurrentSqlConnection() },
                        modifier = Modifier.height(32.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = startScanButtonColors()
                    ) {
                        Text(
                            text = stringResource(Res.string.ScanSettings_PostgresTestConnection),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    if (showAddConnection) {
                        OutlinedButton(
                            enabled = canTest,
                            onClick = {
                                pendingConnectionName = defaultConnectionName()
                                pendingConnectionNameError = false
                                pendingConnectionNameDialog = true
                            },
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
                            ),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(
                                text = stringResource(Res.string.ScanSettings_PostgresAddConnection),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            @Composable
            fun DatabaseTypeChip(
                dbType: DatabaseType,
                iconRes: org.jetbrains.compose.resources.DrawableResource,
                selected: Boolean,
                onClick: () -> Unit
            ) {
                val cs = MaterialTheme.colorScheme
                val interaction = remember { MutableInteractionSource() }
                val hovered by interaction.collectIsHoveredAsState()
                val shape = RoundedCornerShape(10.dp)

                val fill = when {
                    selected && hovered -> cs.primary.copy(alpha = 0.24f)
                    selected -> cs.primary.copy(alpha = 0.18f)
                    hovered -> cs.primary.copy(alpha = 0.12f)
                    else -> cs.surface.copy(alpha = 0.28f)
                }
                val stroke = when {
                    selected || hovered -> cs.primary.copy(alpha = 0.45f)
                    else -> cs.outlineVariant.copy(alpha = 0.65f)
                }
                val labelColor = when {
                    selected || hovered -> cs.primary
                    else -> cs.onSurfaceVariant
                }

                Surface(
                    modifier = Modifier
                        .height(32.dp)
                        .clip(shape)
                        .clickable(
                            interactionSource = interaction,
                            indication = null,
                            onClick = onClick
                        )
                        .hoverable(interactionSource = interaction),
                    shape = shape,
                    color = fill,
                    border = BorderStroke(1.dp, stroke),
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxHeight()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Unspecified
                        )
                        Text(
                            text = dbType.name,
                            style = MaterialTheme.typography.labelSmall,
                            color = labelColor
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .widthIn(min = blockMinWidth)
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // DB type chips (minimal)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = inlineContentHorizontalPadding),
                    horizontalArrangement = Arrangement.spacedBy(inlineControlsGap),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DatabaseType.entries.forEach { dbType ->
                        val selected = sqlScreenState.databaseType == dbType
                        val iconRes = when (dbType) {
                            DatabaseType.PostgreSQL -> Res.drawable.db_postgresql_logo
                            DatabaseType.MySQL -> Res.drawable.db_mysql_logo
                            DatabaseType.SQLite -> Res.drawable.db_sqlite_logo
                            DatabaseType.GreenPlum -> Res.drawable.db_greenplum_logo
                            DatabaseType.Hive -> Res.drawable.db_hive_logo
                            DatabaseType.CockroachDB -> Res.drawable.db_cockroachdb_logo
                            DatabaseType.ClickHouse -> Res.drawable.db_clickhouse_logo
                        }
                        DatabaseTypeChip(
                            dbType = dbType,
                            iconRes = iconRes,
                            selected = selected,
                            onClick = {
                                val defaultPort = when (dbType) {
                                    DatabaseType.PostgreSQL -> "5432"
                                    DatabaseType.MySQL -> "3306"
                                    DatabaseType.GreenPlum -> "5432"
                                    DatabaseType.Hive -> "10000"
                                    DatabaseType.CockroachDB -> "26257"
                                    DatabaseType.ClickHouse -> "8123"
                                    DatabaseType.SQLite -> sqlScreenState.port
                                }
                                val updated = sqlScreenState.copy(databaseType = dbType, port = defaultPort)
                                sqlScreenState = updated
                                highlightedConnectionFields =
                                    updated.updatedHighlightedConnectionFields(highlightedConnectionFields)
                                markSqlConnectionDirty()
                                coroutineScope.launch { screenStateSettings.save() }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (sqlScreenState.databaseType != DatabaseType.SQLite) {
                        TextButton(
                            onClick = { savedConnectionsExpanded = true },
                            enabled = savedForCurrentType.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.MainScreen_SavedConnections, savedForCurrentType.size),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (savedForCurrentType.isNotEmpty()) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                }
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }

                when (sqlScreenState.databaseType) {
                    DatabaseType.SQLite -> {
                        Row(
                            modifier = Modifier.padding(horizontal = inlineContentHorizontalPadding),
                            horizontalArrangement = Arrangement.spacedBy(inlineControlsGap),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val canTest = sqlScreenState.hasRequiredConnectionSettings()
                            ConnectionActionButtons(
                                canTest = canTest,
                                showAddConnection = false
                            )
                        }
                    }
                    else -> {
                        Row(
                            modifier = Modifier.padding(horizontal = inlineContentHorizontalPadding),
                            horizontalArrangement = Arrangement.spacedBy(inlineControlsGap),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompactField(
                                value = sqlScreenState.schema,
                                onValueChange = {
                                    val updated = sqlScreenState.copy(schema = it)
                                    sqlScreenState = updated
                                    highlightedConnectionFields =
                                        updated.updatedHighlightedConnectionFields(highlightedConnectionFields)
                                    markSqlConnectionDirty()
                                    coroutineScope.launch { screenStateSettings.save() }
                                },
                                placeholder = "Schema",
                                modifier = Modifier.weight(0.22f)
                            )
                            CompactField(
                                value = sqlScreenState.user,
                                onValueChange = {
                                    val updated = sqlScreenState.copy(user = it)
                                    sqlScreenState = updated
                                    highlightedConnectionFields =
                                        updated.updatedHighlightedConnectionFields(highlightedConnectionFields)
                                    markSqlConnectionDirty()
                                    coroutineScope.launch { screenStateSettings.save() }
                                },
                                placeholder = "User",
                                modifier = Modifier.weight(0.22f),
                                isError = DatabaseConnectionRequiredField.USER in highlightedConnectionFields
                            )
                            CompactField(
                                value = sqlScreenState.password,
                                onValueChange = {
                                    val updated = sqlScreenState.copy(password = it)
                                    sqlScreenState = updated
                                    highlightedConnectionFields =
                                        updated.updatedHighlightedConnectionFields(highlightedConnectionFields)
                                    markSqlConnectionDirty()
                                    coroutineScope.launch { screenStateSettings.save() }
                                },
                                placeholder = "Password",
                                modifier = Modifier.weight(0.28f),
                                isError = DatabaseConnectionRequiredField.PASSWORD in highlightedConnectionFields,
                                isPassword = true
                            )
                            val canTest = sqlScreenState.hasRequiredConnectionSettings()
                            ConnectionActionButtons(
                                canTest = canTest,
                                showAddConnection = true
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .padding(horizontal = inlineContentHorizontalPadding),
                    contentAlignment = Alignment.CenterStart
                ) {
                    sqlConnectionTestMessage?.let { message ->
                        val color = if (sqlConnectionTestSuccessful)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                        Text(
                            text = message,
                            color = color,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
