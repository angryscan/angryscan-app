package org.angryscan.app.ui.windows.screens.main.subscreens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun DatabaseScreen(
    expandScanState: (Int) -> Unit,
    setSidebarContent: (@Composable () -> Unit) -> Unit = {},
    setBottomBarContent: (@Composable () -> Unit) -> Unit = {},
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

    val filePickerLauncher = rememberFilePickerLauncher(
        type = FileKitType.File(extensions = listOf("db")),
        mode = FileKitMode.Single,
        title = "Select SQLite database"
    ) { result ->
        result?.path?.let { path ->
            sqlScreenState = sqlScreenState.copy(filePath = path)
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
}
