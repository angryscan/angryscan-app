package org.angryscan.app.ui.windows.screens.scans.components

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

private data class IssueRow(
    val severity: String,
    val message: String,
    val location: String,
    val cveId: String,
    val description: String
)

private fun severityOrder(severity: String): Int = when (severity.lowercase()) {
    "critical" -> 0
    "warning" -> 1
    "info" -> 2
    "debug" -> 3
    else -> 4
}

@Composable
fun AIModelResultTable(resultJson: String?) {
    if (resultJson.isNullOrBlank()) {
        Text("No result data", style = MaterialTheme.typography.bodyMedium)
        return
    }
    val json = runCatching {
        Json.parseToJsonElement(resultJson) as? JsonObject
    }.getOrNull() ?: return
    val bytesScanned = json["bytes_scanned"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
    val filesScanned = json["files_scanned"]?.jsonPrimitive?.content?.toLongOrNull() ?: 0L
    val totalChecks = json["total_checks"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
    val passedChecks = json["passed_checks"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
    val failedChecks = json["failed_checks"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0
    val duration = json["duration"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: 0.0
    val issues = (json["issues"] as? JsonArray)?.mapNotNull { el ->
        val obj = el as? JsonObject ?: return@mapNotNull null
        val message = obj["message"]?.jsonPrimitive?.content ?: ""
        val severity = obj["severity"]?.jsonPrimitive?.content ?: ""
        val locationFull = obj["location"]?.jsonPrimitive?.content ?: ""
        val details = obj["details"] as? JsonObject
        val isDatasetsMessage = message.startsWith("datasets", ignoreCase = true)
        val description = if (isDatasetsMessage) {
            details?.get("impact")?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: "—"
        } else {
            val why = obj["why"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: ""
            val vulnerabilityDescription = details?.get("vulnerability_description")?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: ""
            buildList {
                if (severity.lowercase() in listOf("critical", "warning") && why.isNotEmpty()) add(why)
                if (vulnerabilityDescription.isNotEmpty()) add(vulnerabilityDescription)
            }.joinToString("\n\n")
        }
        val location = if (isDatasetsMessage) {
            val filesArray = details?.get("files") as? JsonArray
            filesArray?.mapNotNull { fileEl ->
                fileEl.jsonPrimitive?.content
                    ?: (fileEl as? JsonObject)?.get("path")?.jsonPrimitive?.content
            }?.joinToString("\n") { it.substringAfterLast('/') } ?: locationFull.substringAfterLast('/')
        } else {
            locationFull.substringAfterLast('/')
        }
        val cveId = details?.get("cve_id")?.jsonPrimitive?.content?.takeIf { it.isNotBlank() } ?: "—"
        IssueRow(severity, message, location, cveId, description)
    } ?: emptyList()

    val sortedIssues = issues.sortedWith(
        compareBy<IssueRow> { severityOrder(it.severity) }.thenBy { it.cveId == "—" }.thenBy { it.cveId }
    )

    val scrollState = rememberLazyListState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem("Bytes scanned", bytesScanned.toString())
            SummaryItem("Files scanned", filesScanned.toString())
            SummaryItem("Total checks", totalChecks.toString())
            SummaryItem("Passed", passedChecks.toString())
            SummaryItem("Failed", failedChecks.toString())
            SummaryItem("Duration (s)", "%.2f".format(duration))
        }
        Text(
            text = "Issues",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        if (issues.isEmpty()) {
            Text(
                text = "No issues found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = if (scrollState.canScrollBackward || scrollState.canScrollForward) 20.dp else 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Severity", modifier = Modifier.width(80.dp), fontSize = 12.sp, style = MaterialTheme.typography.labelMedium)
                        Text("CVE ID", modifier = Modifier.width(120.dp), fontSize = 12.sp, style = MaterialTheme.typography.labelMedium)
                        Text("Message", modifier = Modifier.weight(1f), fontSize = 12.sp, style = MaterialTheme.typography.labelMedium)
                        Text("Description", modifier = Modifier.width(220.dp), fontSize = 12.sp, style = MaterialTheme.typography.labelMedium)
                        Text("Location", modifier = Modifier.width(200.dp), fontSize = 12.sp, style = MaterialTheme.typography.labelMedium)
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        state = scrollState,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(sortedIssues) { issue ->
                    val severityColor = when (issue.severity.lowercase()) {
                        "critical" -> Color(0xFFB00020)
                        "high" -> Color(0xFFE65100)
                        "medium", "info" -> Color(0xFFF9A825)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = issue.severity,
                            modifier = Modifier.width(80.dp),
                            fontSize = 12.sp,
                            color = severityColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = issue.cveId,
                            modifier = Modifier.width(120.dp),
                            fontSize = 11.sp,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = issue.message,
                            modifier = Modifier.weight(1f),
                            fontSize = 12.sp,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3
                        )
                        Text(
                            text = issue.description.ifEmpty { "—" },
                            modifier = Modifier.width(220.dp),
                            fontSize = 11.sp,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 5
                        )
                        Text(
                            text = issue.location,
                            modifier = Modifier.width(200.dp),
                            fontSize = 11.sp,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = Int.MAX_VALUE
                        )
                    }
                }
                }
                }
                VerticalScrollbar(
                    adapter = rememberScrollbarAdapter(scrollState),
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .width(10.dp)
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(),
                    style = LocalScrollbarStyle.current.copy(
                        unhoverColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                        hoverColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall
        )
    }
}
