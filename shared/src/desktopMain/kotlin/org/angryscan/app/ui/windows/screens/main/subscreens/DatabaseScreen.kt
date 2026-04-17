package org.angryscan.app.ui.windows.screens.main.subscreens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.launch
import org.angryscan.app.common.*
import org.angryscan.app.resources.*
import org.angryscan.app.scan.ScanService
import org.angryscan.app.scan.common.connectors.*
import org.angryscan.app.ui.windows.components.DescriptionTooltip
import org.angryscan.app.ui.windows.screens.main.components.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun DatabaseScreen(
    expandScanState: (Int) -> Unit,
    setSidebarContent: (@Composable () -> Unit) -> Unit = {},
    setBottomBarContent: (@Composable () -> Unit) -> Unit = {},
    setUnderSourceContent: (@Composable () -> Unit) -> Unit = {},
    onSqlConnectionError: () -> Unit = {},
    showErrorSnackbar: (String) -> Unit = {}
) {
    val scanService = koinInject<ScanService>()
    val scanSettings = koinInject<ScanSettings>()
    val screenStateSettings = koinInject<ScreenStateSettings>()
    var sqlScreenState by remember { screenStateSettings.sqlScreenState }

    var selectPathError by remember { mutableStateOf(false) }
    var validationErrorDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    val noMatchersTitle = stringResource(Res.string.Validation_NoMatchersTitle)
    val noMatchersMessage = stringResource(Res.string.Validation_NoMatchersMessage)
    val postgresConnectionErrorMessage = stringResource(Res.string.Validation_PostgresConnectionMessage)

    val coroutineScope = rememberCoroutineScope()
    var sqlConnectionTestInProgress by remember { mutableStateOf(false) }
    var sqlConnectionTestSuccessful by remember { mutableStateOf(false) }
    var sqlConnectionTestMessage by remember { mutableStateOf<String?>(null) }
    var savedConnectionsExpanded by remember { mutableStateOf(false) }
    var selectedSavedConnectionKey by remember { mutableStateOf<String?>(null) }
    var pendingDeleteConnection by remember { mutableStateOf<ScreenStateSettings.SqlSavedConnection?>(null) }

    fun markSqlConnectionDirty() {
        sqlConnectionTestSuccessful = false
        sqlConnectionTestMessage = null
        selectedSavedConnectionKey = null
    }

    fun sqlConnectionKey(
        databaseType: DatabaseType,
        host: String,
        port: String,
        schema: String,
        user: String,
    ): String = "${databaseType.name}|${host.trim().lowercase()}|${port.trim()}|${schema.trim().lowercase()}|${user.trim().lowercase()}"

    fun applySavedConnection(conn: ScreenStateSettings.SqlSavedConnection) {
        sqlScreenState = sqlScreenState.copy(
            databaseType = conn.databaseType,
            host = conn.host,
            port = conn.port,
            database = conn.database,
            schema = conn.schema,
            user = conn.user,
            password = conn.password
        )
        sqlConnectionTestSuccessful = false
        sqlConnectionTestMessage = null
        selectedSavedConnectionKey = sqlConnectionKey(
            databaseType = conn.databaseType,
            host = conn.host,
            port = conn.port,
            schema = conn.schema,
            user = conn.user
        )
        coroutineScope.launch { screenStateSettings.save() }
    }

    fun databaseTypeLabel(databaseType: DatabaseType): String = when (databaseType) {
        DatabaseType.PostgreSQL -> "PostgreSQL"
        DatabaseType.MySQL -> "MySQL"
        DatabaseType.SQLite -> "SQLite"
        DatabaseType.GreenPlum -> "GreenPlum"
        DatabaseType.Hive -> "Hive"
        DatabaseType.CockroachDB -> "CockroachDB"
    }

    fun savedConnectionLabel(conn: ScreenStateSettings.SqlSavedConnection): String = buildString {
        append("${conn.host}:${conn.port}")
        if (conn.database.isNotBlank()) append(" · ${conn.database}")
        if (conn.schema.isNotBlank()) append(" · ${conn.schema}")
        if (conn.user.isNotBlank()) append(" · ${conn.user}")
    }

    fun saveCurrentSqlConnection() {
        if (sqlScreenState.databaseType == DatabaseType.SQLite) return
        val current = ScreenStateSettings.SqlSavedConnection(
            databaseType = sqlScreenState.databaseType,
            host = sqlScreenState.host.trim(),
            port = sqlScreenState.port.trim(),
            database = sqlScreenState.database.trim(),
            schema = sqlScreenState.schema.trim(),
            user = sqlScreenState.user.trim(),
            password = sqlScreenState.password
        )
        val currentKey = sqlConnectionKey(
            databaseType = current.databaseType,
            host = current.host,
            port = current.port,
            schema = current.schema,
            user = current.user
        )
        val existingIndex = screenStateSettings.sqlSavedConnections.indexOfFirst {
            sqlConnectionKey(
                databaseType = it.databaseType,
                host = it.host,
                port = it.port,
                schema = it.schema,
                user = it.user
            ) == currentKey
        }
        if (existingIndex >= 0) {
            // Same host/port/schema/user: if password differs, update the saved connection.
            val existing = screenStateSettings.sqlSavedConnections[existingIndex]
            if (existing != current) {
                screenStateSettings.sqlSavedConnections[existingIndex] = current
            }
        } else {
            screenStateSettings.sqlSavedConnections.add(current)
        }
        coroutineScope.launch { screenStateSettings.save() }
    }

    fun removeSavedConnection(conn: ScreenStateSettings.SqlSavedConnection) {
        val removedKey = sqlConnectionKey(
            databaseType = conn.databaseType,
            host = conn.host,
            port = conn.port,
            schema = conn.schema,
            user = conn.user
        )
        screenStateSettings.sqlSavedConnections.removeAll {
            sqlConnectionKey(
                databaseType = it.databaseType,
                host = it.host,
                port = it.port,
                schema = it.schema,
                user = it.user
            ) == removedKey
        }
        if (selectedSavedConnectionKey == removedKey) {
            selectedSavedConnectionKey = null
        }
        val hasItemsForCurrentType = screenStateSettings.sqlSavedConnections.any {
            it.databaseType == sqlScreenState.databaseType
        }
        if (!hasItemsForCurrentType) {
            savedConnectionsExpanded = false
        }
        coroutineScope.launch { screenStateSettings.save() }
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
                if (sqlScreenState.databaseType != DatabaseType.SQLite) {
                    saveCurrentSqlConnection()
                }
                sqlConnectionTestMessage = "Connection is valid"
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
            sqlScreenState = sqlScreenState.copy(filePath = path)
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

            val sqlScanEnabled = sqlScreenState.hasRequiredConnectionSettings()

            val startSqlScan: () -> Unit = startSqlScan@{
                if (scanSettings.matchers.isEmpty() && scanSettings.userSignatures.isEmpty()) {
                    validationErrorDialog = noMatchersTitle to noMatchersMessage
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
                    val rowLimit = sqlScreenState.rowLimit.toIntOrNull()?.takeIf { it > 0 } ?: 1000
                    val connector = when (sqlScreenState.databaseType) {
                        DatabaseType.PostgreSQL -> ConnectorPostgres(
                            host = sqlScreenState.host,
                            port = sqlScreenState.connectionPort(),
                            database = sqlScreenState.database,
                            user = sqlScreenState.user,
                            password = sqlScreenState.password,
                            rowLimit = rowLimit
                        )
                        DatabaseType.MySQL -> ConnectorMySQL(
                            host = sqlScreenState.host,
                            port = sqlScreenState.connectionPort(),
                            database = sqlScreenState.database,
                            user = sqlScreenState.user,
                            password = sqlScreenState.password,
                            rowLimit = rowLimit
                        )
                        DatabaseType.GreenPlum -> ConnectorGreenPlum(
                            host = sqlScreenState.host,
                            port = sqlScreenState.connectionPort(),
                            database = sqlScreenState.database,
                            user = sqlScreenState.user,
                            password = sqlScreenState.password,
                            rowLimit = rowLimit
                        )
                        DatabaseType.Hive -> ConnectorHive(
                            host = sqlScreenState.host,
                            port = sqlScreenState.connectionPort(),
                            database = sqlScreenState.database,
                            user = sqlScreenState.user,
                            password = sqlScreenState.password,
                            rowLimit = rowLimit
                        )
                        DatabaseType.CockroachDB -> ConnectorCockroachDB(
                            host = sqlScreenState.host,
                            port = sqlScreenState.connectionPort(),
                            database = sqlScreenState.database,
                            user = sqlScreenState.user,
                            password = sqlScreenState.password,
                            rowLimit = rowLimit
                        )
                        DatabaseType.SQLite -> ConnectorSqlite(
                            filePath = sqlScreenState.filePath,
                            rowLimit = rowLimit
                        )
                    }
                    val taskName = when (sqlScreenState.databaseType) {
                        DatabaseType.PostgreSQL, DatabaseType.MySQL, DatabaseType.GreenPlum, DatabaseType.Hive, DatabaseType.CockroachDB ->
                            "${sqlScreenState.host}:${sqlScreenState.port}/${sqlScreenState.database}" +
                                if (sqlScreenState.schema.isNotEmpty()) " schema: ${sqlScreenState.schema}" else ""
                        DatabaseType.SQLite -> sqlScreenState.filePath
                    }
                    val path = when (sqlScreenState.databaseType) {
                        DatabaseType.PostgreSQL, DatabaseType.MySQL, DatabaseType.GreenPlum, DatabaseType.Hive, DatabaseType.CockroachDB -> sqlScreenState.schema
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
                        .height(controlHeight)
                        .then(
                            if (selectPathError) {
                                Modifier.border(
                                    2.dp,
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                    controlShape
                                )
                            } else {
                                Modifier
                            }
                        ),
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
                            DatabaseType.PostgreSQL, DatabaseType.MySQL, DatabaseType.GreenPlum, DatabaseType.Hive, DatabaseType.CockroachDB -> {
                                OutlinedTextField(
                                    value = sqlScreenState.host,
                                    onValueChange = {
                                        sqlScreenState = sqlScreenState.copy(host = it)
                                        markSqlConnectionDirty()
                                        coroutineScope.launch { screenStateSettings.save() }
                                    },
                                    modifier = Modifier.weight(0.4f).heightIn(min = 40.dp),
                                    placeholder = { Text("Host", style = MaterialTheme.typography.bodyMedium) },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    isError = selectPathError,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                        errorBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                )
                                OutlinedTextField(
                                    value = sqlScreenState.port,
                                    onValueChange = {
                                        sqlScreenState = sqlScreenState.copy(port = it)
                                        markSqlConnectionDirty()
                                        coroutineScope.launch { screenStateSettings.save() }
                                    },
                                    modifier = Modifier.weight(0.2f).heightIn(min = 40.dp),
                                    placeholder = { Text("Port", style = MaterialTheme.typography.bodyMedium) },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    isError = selectPathError,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.4f),
                                        errorBorderColor = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                                    )
                                )
                                OutlinedTextField(
                                    value = sqlScreenState.database,
                                    onValueChange = {
                                        sqlScreenState = sqlScreenState.copy(database = it)
                                        markSqlConnectionDirty()
                                        coroutineScope.launch { screenStateSettings.save() }
                                    },
                                    modifier = Modifier.weight(0.4f).heightIn(min = 40.dp),
                                    placeholder = { Text("Database", style = MaterialTheme.typography.bodyMedium) },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    isError = selectPathError,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.Transparent,
                                        unfocusedBorderColor = Color.Transparent,
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
                                        sqlScreenState = sqlScreenState.copy(filePath = it)
                                        markSqlConnectionDirty()
                                        coroutineScope.launch { screenStateSettings.save() }
                                    },
                                    modifier = Modifier.weight(1f).heightIn(min = 40.dp),
                                    placeholder = { Text("Path to .db file", style = MaterialTheme.typography.bodyMedium) },
                                    textStyle = MaterialTheme.typography.bodyMedium,
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    isError = selectPathError,
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
                        Icon(
                            imageVector = Icons.Outlined.Storage,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (sqlScanEnabled) {
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
                } else {
                    DescriptionTooltip(
                        description = stringResource(Res.string.MainScreen_ScanHint_SqlDatabase),
                        delay = 400
                    ) {
                        Button(
                            enabled = false,
                            onClick = { },
                            modifier = ScanButtonModifier(
                                isReady = false,
                                modifier = Modifier
                                    .width(scanButtonWidth)
                                    .height(controlHeight)
                            ).scanButtonHoverFeedback(enabled = false).scanButtonChipBorder(),
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
            val savedConnectionsScroll = rememberScrollState()

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
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f),
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

            Column(
                modifier = Modifier
                    .widthIn(min = blockMinWidth)
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // DB type chips (minimal)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                        }
                        FilterChip(
                            selected = selected,
                            onClick = {
                                val defaultPort = when (dbType) {
                                    DatabaseType.PostgreSQL -> "5432"
                                    DatabaseType.MySQL -> "3306"
                                    DatabaseType.GreenPlum -> "5432"
                                    DatabaseType.Hive -> "10000"
                                    DatabaseType.CockroachDB -> "26257"
                                    DatabaseType.SQLite -> sqlScreenState.port
                                }
                                sqlScreenState = sqlScreenState.copy(databaseType = dbType, port = defaultPort)
                                markSqlConnectionDirty()
                                coroutineScope.launch { screenStateSettings.save() }
                            },
                            label = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = Color.Unspecified
                                    )
                                    Text(dbType.name, style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f),
                                selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            ),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            )
                        )
                    }
                    if (sqlScreenState.databaseType != DatabaseType.SQLite) {
                        val savedForCurrentType = screenStateSettings.sqlSavedConnections
                            .filter { it.databaseType == sqlScreenState.databaseType }
                        val savedMenuHeight = (savedForCurrentType.size * 40).dp.coerceIn(40.dp, 260.dp)
                        Spacer(modifier = Modifier.weight(1f))
                        Box {
                            TextButton(
                                onClick = { savedConnectionsExpanded = !savedConnectionsExpanded },
                                enabled = savedForCurrentType.isNotEmpty(),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    stringResource(Res.string.MainScreen_SavedConnections, savedForCurrentType.size),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    imageVector = if (savedConnectionsExpanded) {
                                        Icons.Outlined.KeyboardArrowUp
                                    } else {
                                        Icons.Outlined.KeyboardArrowDown
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = savedConnectionsExpanded,
                                onDismissRequest = { savedConnectionsExpanded = false },
                                modifier = Modifier
                                    .widthIn(min = 340.dp, max = 520.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(savedMenuHeight)
                                        .widthIn(min = 340.dp, max = 520.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(end = 10.dp)
                                            .verticalScroll(savedConnectionsScroll)
                                    ) {
                                        savedForCurrentType.forEach { conn ->
                                            val title = savedConnectionLabel(conn)
                                            val isSelected = sqlConnectionKey(
                                                databaseType = conn.databaseType,
                                                host = conn.host,
                                                port = conn.port,
                                                schema = conn.schema,
                                                user = conn.user
                                            ) == selectedSavedConnectionKey
                                            DescriptionTooltip(description = title, delay = 250) {
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(
                                                            text = title,
                                                            style = if (isSelected) {
                                                                MaterialTheme.typography.bodyMedium.copy(
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                            } else {
                                                                MaterialTheme.typography.bodyMedium
                                                            },
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    },
                                                    trailingIcon = {
                                                        IconButton(
                                                            onClick = { pendingDeleteConnection = conn },
                                                            modifier = Modifier.size(28.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Outlined.Close,
                                                                contentDescription = "Delete saved connection",
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                                                modifier = Modifier.size(14.dp)
                                                            )
                                                        }
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                                                    onClick = {
                                                        applySavedConnection(conn)
                                                        savedConnectionsExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    VerticalScrollbar(
                                        adapter = rememberScrollbarAdapter(savedConnectionsScroll),
                                        modifier = Modifier
                                            .align(Alignment.CenterEnd)
                                            .fillMaxHeight()
                                            .width(8.dp),
                                        style = LocalScrollbarStyle.current.copy(
                                            unhoverColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                            hoverColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                when (sqlScreenState.databaseType) {
                    DatabaseType.SQLite -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompactField(
                                value = sqlScreenState.rowLimit,
                                onValueChange = { v ->
                                    if (v.isBlank() || v.toIntOrNull() != null) {
                                        sqlScreenState = sqlScreenState.copy(rowLimit = v)
                                        markSqlConnectionDirty()
                                        coroutineScope.launch { screenStateSettings.save() }
                                    }
                                },
                                placeholder = "Rows",
                                modifier = Modifier.width(120.dp)
                            )
                            val canTest = sqlScreenState.hasRequiredConnectionSettings()
                            DescriptionTooltip(description = "Test connection", delay = 350) {
                                Button(
                                    enabled = canTest && !sqlConnectionTestInProgress,
                                    onClick = { testCurrentSqlConnection() },
                                    modifier = Modifier.size(32.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = startScanButtonColors()
                                ) {
                                    when {
                                        sqlConnectionTestInProgress -> CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp
                                        )
                                        sqlConnectionTestSuccessful -> Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        sqlConnectionTestMessage != null -> Icon(
                                            imageVector = Icons.Outlined.ErrorOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        else -> Icon(
                                            imageVector = Icons.Outlined.Sync,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CompactField(
                                value = sqlScreenState.schema,
                                onValueChange = {
                                    sqlScreenState = sqlScreenState.copy(schema = it)
                                    markSqlConnectionDirty()
                                    coroutineScope.launch { screenStateSettings.save() }
                                },
                                placeholder = "Schema",
                                modifier = Modifier.weight(0.22f)
                            )
                            CompactField(
                                value = sqlScreenState.user,
                                onValueChange = {
                                    sqlScreenState = sqlScreenState.copy(user = it)
                                    markSqlConnectionDirty()
                                    coroutineScope.launch { screenStateSettings.save() }
                                },
                                placeholder = "User",
                                modifier = Modifier.weight(0.22f)
                            )
                            CompactField(
                                value = sqlScreenState.password,
                                onValueChange = {
                                    sqlScreenState = sqlScreenState.copy(password = it)
                                    markSqlConnectionDirty()
                                    coroutineScope.launch { screenStateSettings.save() }
                                },
                                placeholder = "Password",
                                modifier = Modifier.weight(0.34f),
                                isPassword = true
                            )
                            CompactField(
                                value = sqlScreenState.rowLimit,
                                onValueChange = { v ->
                                    if (v.isBlank() || v.toIntOrNull() != null) {
                                        sqlScreenState = sqlScreenState.copy(rowLimit = v)
                                        markSqlConnectionDirty()
                                        coroutineScope.launch { screenStateSettings.save() }
                                    }
                                },
                                placeholder = "Rows",
                                modifier = Modifier.weight(0.22f)
                            )
                            val canTest = sqlScreenState.hasRequiredConnectionSettings()
                            DescriptionTooltip(description = "Test connection", delay = 350) {
                                Button(
                                    enabled = canTest && !sqlConnectionTestInProgress,
                                    onClick = { testCurrentSqlConnection() },
                                    modifier = Modifier.size(32.dp),
                                    contentPadding = PaddingValues(0.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = startScanButtonColors()
                                ) {
                                    when {
                                        sqlConnectionTestInProgress -> CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp
                                        )
                                        sqlConnectionTestSuccessful -> Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        sqlConnectionTestMessage != null -> Icon(
                                            imageVector = Icons.Outlined.ErrorOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        else -> Icon(
                                            imageVector = Icons.Outlined.Sync,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                sqlConnectionTestMessage?.let { message ->
                    val color = if (sqlConnectionTestSuccessful)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error
                    Text(
                        text = message,
                        color = color,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
