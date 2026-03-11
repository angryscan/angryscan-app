package org.angryscan.app.ui.windows.screens.main.subscreens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.ScreenStateSettings
import org.angryscan.app.resources.*
import org.angryscan.app.scan.ScanService
import org.angryscan.app.scan.common.connectors.ConnectorPostgres
import org.angryscan.app.scan.common.connectors.PostgresConnectionValidator
import org.angryscan.app.ui.windows.components.DescriptionTooltip
import org.angryscan.app.ui.windows.screens.main.components.*
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun PostgresScreen(
    expandScanState: (Int) -> Unit,
    setSidebarContent: (@Composable () -> Unit) -> Unit = {},
    setBottomBarContent: (@Composable () -> Unit) -> Unit = {},
    onPostgresConnectionError: () -> Unit = {},
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

    setSidebarContent { }
    setBottomBarContent {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(0.5f)
                    .height(72.dp)
                    .then(
                        if (selectPathError) {
                            Modifier.border(
                                2.dp,
                                MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                RoundedCornerShape(20.dp)
                            )
                        } else {
                            Modifier
                        }
                    ),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = sqlScreenState.schema,
                        onValueChange = {
                            sqlScreenState = sqlScreenState.copy(schema = it)
                            coroutineScope.launch {
                                screenStateSettings.save()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp),
                        placeholder = {
                            Text(
                                stringResource(Res.string.MainScreen_Placeholder_Postgres),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        },
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
                    Icon(
                        imageVector = Icons.Outlined.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            val postgresScanEnabled = sqlScreenState.host.isNotBlank() &&
                    sqlScreenState.port.toIntOrNull() != null &&
                    sqlScreenState.database.isNotBlank() &&
                    sqlScreenState.user.isNotBlank() &&
                    sqlScreenState.password.isNotBlank()

            if (postgresScanEnabled) {
                Button(
                    enabled = true,
                    onClick = {
                        if (scanSettings.matchers.isEmpty() && scanSettings.userSignatures.isEmpty()) {
                            validationErrorDialog = noMatchersTitle to noMatchersMessage
                            return@Button
                        }

                        coroutineScope.launch {
                            val port = sqlScreenState.port.toIntOrNull() ?: 5432
                            val connectionError = PostgresConnectionValidator.validate(
                                host = sqlScreenState.host,
                                port = port,
                                database = sqlScreenState.database,
                                user = sqlScreenState.user,
                                password = sqlScreenState.password
                            )
                            if (connectionError != null) {
                                onPostgresConnectionError()
                                showErrorSnackbar(postgresConnectionErrorMessage)
                                return@launch
                            }
                            val task = scanService.createTask(
                                name = "${sqlScreenState.host}:${sqlScreenState.port}/${sqlScreenState.database}" + if (sqlScreenState.schema.isNotEmpty())
                                    " schema: ${sqlScreenState.schema}" else "",
                                path = sqlScreenState.schema,
                                extensions = scanSettings.extensions,
                                matchers = scanSettings.matchers + scanSettings.userSignatures,
                                fastScan = scanSettings.fastScan.value,
                                connector = ConnectorPostgres(
                                    host = sqlScreenState.host,
                                    port = port,
                                    database = sqlScreenState.database,
                                    user = sqlScreenState.user,
                                    password = sqlScreenState.password,
                                    rowLimit = sqlScreenState.rowLimit.toIntOrNull()?.takeIf { it > 0 } ?: 1000
                                )
                            )
                            scanService.startTask(task)
                            task.id.value?.let { expandScanState(it) }
                        }
                    },
                    modifier = ScanButtonModifier(
                        isReady = true,
                        modifier = Modifier.wrapContentWidth().height(72.dp).widthIn(min = 200.dp)
                    ).scanButtonHoverFeedback(enabled = true).scanButtonChipBorder(),
                    shape = RoundedCornerShape(20.dp),
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
                    description = stringResource(Res.string.MainScreen_ScanHint_Postgres),
                    delay = 400
                ) {
                    Button(
                        enabled = false,
                        onClick = { },
                        modifier = ScanButtonModifier(
                            isReady = false,
                            modifier = Modifier.wrapContentWidth().height(72.dp).widthIn(min = 200.dp)
                        ).scanButtonHoverFeedback(enabled = false).scanButtonChipBorder(),
                        shape = RoundedCornerShape(20.dp),
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

    ScanValidationErrorDialog(
        validationError = validationErrorDialog,
        onDismiss = { validationErrorDialog = null }
    )
}
