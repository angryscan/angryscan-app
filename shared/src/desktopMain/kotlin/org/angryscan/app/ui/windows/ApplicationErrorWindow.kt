package org.angryscan.app.ui.windows

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import org.angryscan.app.resources.Res
import org.angryscan.app.resources.close
import org.angryscan.app.resources.save
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.system.exitProcess

@OptIn(ExperimentalResourceApi::class)
@Composable
fun ApplicationErrorWindow(lastError: Throwable) {
    Column {
        Column(
            modifier = Modifier.height(420.dp)
        ) {
            SelectionContainer {
                Text(
                    text = lastError.message ?: "Unknown error",
                    modifier = Modifier.padding(8.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier.fillMaxHeight().fillMaxWidth().background(color = Color(0xFFb5b5b5))
            ) {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    SelectionContainer {
                        Text(lastError.stackTraceToString(), Modifier.padding(4.dp))
                    }
                }
            }
        }
        Row {
            Button(
                onClick = {
                    val f = JFileChooser()
                    f.fileSelectionMode = JFileChooser.FILES_ONLY
                    f.isMultiSelectionEnabled = false
                    f.fileFilter = FileNameExtensionFilter("Logs (*.log)", "log")
                    f.selectedFile = File("errors.log")
                    val resDialog = f.showSaveDialog(null)
                    if (resDialog == JFileChooser.APPROVE_OPTION) {
                        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                        File(f.selectedFile.absolutePath).appendText(
                            "\n${
                                LocalDateTime.now().format(formatter)
                            }\n" + lastError.message + "\n" + lastError.stackTraceToString() + "###\n"
                        )
                    }
                },
                modifier = Modifier.testTag("application_error_save")
            ) { Text(stringResource(Res.string.save).uppercase()) }
            Button(
                onClick = { exitProcess(0) },
                modifier = Modifier.testTag("application_error_close")
            ) { Text(stringResource(Res.string.close).uppercase()) }
        }
    }
}