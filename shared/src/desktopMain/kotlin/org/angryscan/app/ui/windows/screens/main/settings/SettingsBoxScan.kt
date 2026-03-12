package org.angryscan.app.ui.windows.screens.main.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import kotlinx.coroutines.delay
import org.angryscan.app.common.ScanSettings
import org.angryscan.app.common.ScreenStateSettings
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.S3Screen_Tooltip_ConnectionSettings
import org.angryscan.app.resources.ScanSettings_FastScan
import org.angryscan.app.resources.ScanSettings_Tooltip_FastScan
import org.angryscan.app.ui.windows.screens.main.components.MainScreenConnector
import org.angryscan.app.ui.windows.screens.main.settings.items.SettingsTextField
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

@Composable
fun SettingsBoxScan(
    scanSettings: ScanSettings,
    navController: NavController,
    postgresConnectionBlinkSignal: Int,
    selectedTab: Int
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isS3Source = backStackEntry?.destination?.hasRoute(MainScreenConnector.S3::class) == true
    val isPostgresSource = backStackEntry?.destination?.hasRoute(MainScreenConnector.Postgres::class) == true
    val screenStateSettings = koinInject<ScreenStateSettings>()
    val fastScan by scanSettings.fastScan

    var postgresFieldsBlinking by remember { mutableStateOf(false) }

    LaunchedEffect(postgresConnectionBlinkSignal) {
        if (postgresConnectionBlinkSignal == 0) {
            return@LaunchedEffect
        }

        repeat(3) {
            postgresFieldsBlinking = true
            delay(360)
            postgresFieldsBlinking = false
            delay(280)
        }
    }


    Column(
        modifier = Modifier
            .padding(end = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (!isPostgresSource) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.ScanSettings_FastScan),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(Res.string.ScanSettings_Tooltip_FastScan),
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = fastScan,
                    onCheckedChange = {
                        scanSettings.fastScan.value = it
                        scanSettings.save()
                    }
                )
            }
        }
        if (isS3Source) {
            var s3Endpoint by remember { mutableStateOf(screenStateSettings.s3ScreenState.endpoint) }
            var s3Bucket by remember { mutableStateOf(screenStateSettings.s3ScreenState.bucket) }
            var s3AccessKey by remember { mutableStateOf(screenStateSettings.s3ScreenState.accessKey) }
            var s3SecretKey by remember { mutableStateOf(screenStateSettings.s3ScreenState.secretKey) }
            LaunchedEffect(selectedTab, isS3Source) {
                if (selectedTab == SettingsTab.Scan.ordinal) {
                    s3Endpoint = screenStateSettings.s3ScreenState.endpoint
                    s3Bucket = screenStateSettings.s3ScreenState.bucket
                    s3AccessKey = screenStateSettings.s3ScreenState.accessKey
                    s3SecretKey = screenStateSettings.s3ScreenState.secretKey
                }
            }
            Text(
                text = stringResource(Res.string.S3Screen_Tooltip_ConnectionSettings),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = s3Endpoint,
                    onValueChange = {
                        s3Endpoint = it
                        screenStateSettings.s3ScreenState.endpoint = it
                        screenStateSettings.save()
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    placeholder = { Text("Endpoint", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = s3Bucket,
                    onValueChange = {
                        s3Bucket = it
                        screenStateSettings.s3ScreenState.bucket = it
                        screenStateSettings.save()
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    placeholder = { Text("Bucket", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = s3AccessKey,
                    onValueChange = {
                        s3AccessKey = it
                        screenStateSettings.s3ScreenState.accessKey = it
                        screenStateSettings.save()
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    placeholder = { Text("Access key", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = s3SecretKey,
                    onValueChange = {
                        s3SecretKey = it
                        screenStateSettings.s3ScreenState.secretKey = it
                        screenStateSettings.save()
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    placeholder = { Text("Secret key", style = MaterialTheme.typography.bodyMedium) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        }
        if (isPostgresSource) {
            var sqlScreenState by remember { screenStateSettings.sqlScreenState }

            Text(
                text = "SQL Database connection",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsTextField(
                    placeholder = "Host",
                    value = sqlScreenState.host,
                    onValueChange = {
                        sqlScreenState = sqlScreenState.copy(host = it)
                        screenStateSettings.save()
                    },
                    isError = postgresFieldsBlinking
                )
                SettingsTextField(
                    placeholder = "Port",
                    value = sqlScreenState.port,
                    onValueChange = { port ->
                        port.toIntOrNull()?.let {
                            sqlScreenState = sqlScreenState.copy(port = port)
                            screenStateSettings.save()
                        }
                    },
                    isError = postgresFieldsBlinking
                )
                SettingsTextField(
                    placeholder = "Database",
                    value = sqlScreenState.database,
                    onValueChange = {
                        sqlScreenState = sqlScreenState.copy(database = it)
                        screenStateSettings.save()
                    },
                    isError = postgresFieldsBlinking
                )
                SettingsTextField(
                    placeholder = "User",
                    value = sqlScreenState.user,
                    onValueChange = {
                        sqlScreenState = sqlScreenState.copy(user = it)
                        screenStateSettings.save()
                    },
                    isError = postgresFieldsBlinking
                )
                SettingsTextField(
                    placeholder = "Password",
                    value = sqlScreenState.password,
                    onValueChange = {
                        sqlScreenState = sqlScreenState.copy(password = it)
                        screenStateSettings.save()
                    },
                    isPassword = true,
                    isError = postgresFieldsBlinking
                )
                SettingsTextField(
                    placeholder = "Rows to scan",
                    value = sqlScreenState.rowLimit,
                    onValueChange = { rowLimit ->
                        rowLimit.toIntOrNull()?.let {
                            sqlScreenState = sqlScreenState.copy(rowLimit = rowLimit)
                            screenStateSettings.save()
                        }
                    }
                )
            }
        }
    }
}