package org.angryscan.app.scan.common.writer

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import org.angryscan.app.common.AppVersion
import org.dhatim.fastexcel.BorderStyle
import org.dhatim.fastexcel.Workbook
import org.w3c.dom.Element
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

private val logger = KotlinLogging.logger {}
private val json = Json { ignoreUnknownKeys = true }

object AIModelResultWriter {

    suspend fun saveAIModelResult(filePath: String, resultJson: String, onSaveError: (String) -> Unit): Boolean {
        val extension = ResultWriter.FileExtensions.entries.find { filePath.endsWith(".${it.extension}") }
        if (extension == null) {
            onSaveError("Unsupported file extension")
            return false
        }
        if (File(filePath).exists() && !File(filePath).delete()) {
            onSaveError("Failed to replace file")
            return false
        }
        val root = runCatching {
            Json.parseToJsonElement(resultJson) as? JsonObject
        }.getOrNull()
        if (root == null) {
            onSaveError("Invalid JSON")
            return false
        }
        return try {
            when (extension) {
                ResultWriter.FileExtensions.CSV -> writeCSV(File(filePath), root)
                ResultWriter.FileExtensions.XLSX -> writeXLSX(File(filePath), root)
                ResultWriter.FileExtensions.XML -> writeXML(File(filePath), root)
            }
            true
        } catch (e: Exception) {
            logger.error { "Failed to save AI Model report. ${e.message}" }
            onSaveError("Failed to save report")
            false
        }
    }

    private fun jsonPrimitiveContent(obj: JsonObject, key: String): String =
        obj[key]?.jsonPrimitive?.content ?: ""

    private fun detailContent(details: JsonObject?, key: String): String =
        details?.get(key)?.jsonPrimitive?.content ?: ""

    private fun detailsJson(details: JsonObject?): String =
        if (details != null) json.encodeToString(JsonElement.serializer(), details) else ""

    private fun jsonElementToString(el: JsonElement?): String = when (el) {
        null -> ""
        is JsonObject -> json.encodeToString(JsonElement.serializer(), el)
        is JsonArray -> json.encodeToString(JsonElement.serializer(), el)
        else -> (el as? JsonPrimitive)?.content ?: ""
    }

    private fun arrayOfPrimitivesToCsv(arr: JsonArray?): String =
        arr?.joinToString(",") { (it as? JsonPrimitive)?.content ?: it.toString() } ?: ""

    private fun severityOrder(severity: String): Int = when (severity.lowercase()) {
        "critical" -> 0
        "warning" -> 1
        "info" -> 2
        "debug" -> 3
        else -> 4
    }

    private fun sortedIssues(root: JsonObject): List<JsonElement> {
        val issues = root["issues"] as? JsonArray ?: return emptyList()
        return issues.sortedWith(
            compareBy<JsonElement> {
                severityOrder((it as? JsonObject)?.get("severity")?.jsonPrimitive?.content ?: "")
            }.thenBy { (it as? JsonObject)?.get("message")?.jsonPrimitive?.content ?: "" }
        )
    }

    private fun escapeCsv(value: String): String {
        if (value.contains(';') || value.contains('"') || value.contains('\n') || value.contains('\r')) {
            return "\"" + value.replace("\"", "\"\"") + "\""
        }
        return value
    }

    private val issueDetailKeys = listOf(
        "cve_id", "vulnerability_description", "recommendation", "assessment",
        "module", "function", "opcode", "position", "associated_global", "pickle_filename",
        "ml_context_confidence", "opcode_count", "max_opcodes", "total_dangerous_opcodes",
        "safetensors_available", "affected_pytorch_versions", "fixed_in"
    )

    private suspend fun writeCSV(reportFile: File, root: JsonObject) {
        withContext(Dispatchers.IO) {
            FileOutputStream(reportFile).bufferedWriter(charset = Charset.forName("UTF-8"))
        }.use { writer ->
            writer.append("[Summary]\r\n")
            listOf(
                "bytes_scanned", "files_scanned", "has_errors", "content_hash", "start_time", "duration",
                "total_checks", "passed_checks", "failed_checks", "success", "scanner_names"
            ).forEach { key ->
                val value = when (key) {
                    "bytes_scanned" -> jsonPrimitiveContent(root, "bytes_scanned")
                    "files_scanned" -> jsonPrimitiveContent(root, "files_scanned")
                    "has_errors" -> jsonPrimitiveContent(root, "has_errors")
                    "content_hash" -> jsonPrimitiveContent(root, "content_hash")
                    "start_time" -> jsonPrimitiveContent(root, "start_time")
                    "duration" -> jsonPrimitiveContent(root, "duration")
                    "total_checks" -> jsonPrimitiveContent(root, "total_checks")
                    "passed_checks" -> jsonPrimitiveContent(root, "passed_checks")
                    "failed_checks" -> jsonPrimitiveContent(root, "failed_checks")
                    "success" -> jsonPrimitiveContent(root, "success")
                    "scanner_names" -> (root["scanner_names"] as? JsonArray)?.joinToString(",") { (it as? JsonPrimitive)?.content ?: it.toString() } ?: ""
                    else -> jsonPrimitiveContent(root, key)
                }
                writer.append("${escapeCsv(key)};${escapeCsv(value)}\r\n")
            }

            val fileMetadata = root["file_metadata"] as? JsonObject
            if (fileMetadata != null) {
                writer.append("\r\n[FileMetadata]\r\n")
                val fmHeaders = listOf(
                    "path", "file_size", "md5", "sha1", "sha256", "sha512",
                    "max_stack_depth", "opcode_count", "suspicious_count",
                    "ml_context_json", "license", "license_info_json", "copyright_notices_json", "license_files_nearby",
                    "is_dataset", "is_model", "risk_score", "scan_timestamp",
                    "content_hash", "pickle_files", "metadata_json"
                )
                writer.append(fmHeaders.joinToString(";") { escapeCsv(it) } + "\r\n")
                fileMetadata.entries.forEach { (path, value) ->
                    val obj = value as? JsonObject ?: return@forEach
                    val hashes = obj["file_hashes"] as? JsonObject
                    val pickleArr = obj["pickle_files"] as? JsonArray
                    val licenseFilesNearby = obj["license_files_nearby"] as? JsonArray
                    writer.append(
                        listOf(
                            path,
                            jsonPrimitiveContent(obj, "file_size"),
                            hashes?.get("md5")?.jsonPrimitive?.content ?: "",
                            hashes?.get("sha1")?.jsonPrimitive?.content ?: "",
                            hashes?.get("sha256")?.jsonPrimitive?.content ?: "",
                            hashes?.get("sha512")?.jsonPrimitive?.content ?: "",
                            jsonPrimitiveContent(obj, "max_stack_depth"),
                            jsonPrimitiveContent(obj, "opcode_count"),
                            jsonPrimitiveContent(obj, "suspicious_count"),
                            jsonElementToString(obj["ml_context"]),
                            jsonPrimitiveContent(obj, "license"),
                            jsonElementToString(obj["license_info"]),
                            jsonElementToString(obj["copyright_notices"]),
                            arrayOfPrimitivesToCsv(licenseFilesNearby),
                            jsonPrimitiveContent(obj, "is_dataset"),
                            jsonPrimitiveContent(obj, "is_model"),
                            jsonPrimitiveContent(obj, "risk_score"),
                            jsonPrimitiveContent(obj, "scan_timestamp"),
                            jsonPrimitiveContent(obj, "content_hash"),
                            arrayOfPrimitivesToCsv(pickleArr),
                            detailsJson(obj)
                        ).map { escapeCsv(it) }.joinToString(";") + "\r\n"
                    )
                }
            }

            val issues = sortedIssues(root)
            val issueHeaders = listOf("type", "message", "severity", "location", "why", "timestamp") +
                    issueDetailKeys + "details_json"
            writer.append("\r\n[Issues]\r\n")
            writer.append(issueHeaders.joinToString(";") { escapeCsv(it) } + "\r\n")
            issues.forEach { el ->
                val obj = el as? JsonObject ?: return@forEach
                val details = obj["details"] as? JsonObject
                writer.append(
                    (listOf(
                        jsonPrimitiveContent(obj, "type"),
                        jsonPrimitiveContent(obj, "message"),
                        jsonPrimitiveContent(obj, "severity"),
                        jsonPrimitiveContent(obj, "location"),
                        jsonPrimitiveContent(obj, "why"),
                        jsonPrimitiveContent(obj, "timestamp")
                    ) + issueDetailKeys.map { detailContent(details, it) } + detailsJson(details)).map { escapeCsv(it) }.joinToString(";") + "\r\n"
                )
            }

            val checks = root["checks"] as? JsonArray ?: JsonArray(emptyList())
            val checkHeaders = listOf("name", "status", "message", "location", "severity", "why", "timestamp", "cve_id", "module", "function", "opcode", "position", "associated_global", "pickle_filename", "ml_context_confidence", "opcode_count", "max_opcodes", "details_json")
            writer.append("\r\n[Checks]\r\n")
            writer.append(checkHeaders.joinToString(";") { escapeCsv(it) } + "\r\n")
            checks.forEach { el ->
                val obj = el as? JsonObject ?: return@forEach
                val details = obj["details"] as? JsonObject
                writer.append(
                    listOf(
                        jsonPrimitiveContent(obj, "name"),
                        jsonPrimitiveContent(obj, "status"),
                        jsonPrimitiveContent(obj, "message"),
                        jsonPrimitiveContent(obj, "location"),
                        jsonPrimitiveContent(obj, "severity"),
                        jsonPrimitiveContent(obj, "why"),
                        jsonPrimitiveContent(obj, "timestamp"),
                        detailContent(details, "cve_id"),
                        detailContent(details, "module"),
                        detailContent(details, "function"),
                        detailContent(details, "opcode"),
                        detailContent(details, "position"),
                        detailContent(details, "associated_global"),
                        detailContent(details, "pickle_filename"),
                        detailContent(details, "ml_context_confidence"),
                        detailContent(details, "opcode_count"),
                        detailContent(details, "max_opcodes"),
                        detailsJson(details)
                    ).map { escapeCsv(it) }.joinToString(";") + "\r\n"
                )
            }

            val assets = root["assets"] as? JsonArray ?: JsonArray(emptyList())
            val assetHeaders = listOf("path", "type", "size", "tensors", "keys", "contents_json")
            writer.append("\r\n[Assets]\r\n")
            writer.append(assetHeaders.joinToString(";") { escapeCsv(it) } + "\r\n")
            assets.forEach { el ->
                val obj = el as? JsonObject ?: return@forEach
                writer.append(
                    listOf(
                        jsonPrimitiveContent(obj, "path"),
                        jsonPrimitiveContent(obj, "type"),
                        obj["size"]?.jsonPrimitive?.content ?: "",
                        arrayOfPrimitivesToCsv(obj["tensors"] as? JsonArray),
                        arrayOfPrimitivesToCsv(obj["keys"] as? JsonArray),
                        jsonElementToString(obj["contents"])
                    ).map { escapeCsv(it) }.joinToString(";") + "\r\n"
                )
            }
        }
    }

    private suspend fun writeXLSX(reportFile: File, root: JsonObject) {
        val issues = sortedIssues(root)
        val checks = root["checks"] as? JsonArray ?: JsonArray(emptyList())
        val assets = root["assets"] as? JsonArray ?: JsonArray(emptyList())
        val fileMetadata = root["file_metadata"] as? JsonObject

        withContext(Dispatchers.IO) { FileOutputStream(reportFile) }.use { outputStream ->
            Workbook(
                outputStream,
                "Angry Data Scanner",
                if (AppVersion == "Debug") "0.1" else AppVersion.substringBeforeLast('.')
            ).use { workbook ->
                val summarySheet = workbook.newWorksheet("Summary")
                listOf(
                    "bytes_scanned", "files_scanned", "has_errors", "content_hash", "start_time", "duration",
                    "total_checks", "passed_checks", "failed_checks", "success", "scanner_names"
                ).forEachIndexed { rowIdx, key ->
                    val value = when (key) {
                        "bytes_scanned" -> jsonPrimitiveContent(root, "bytes_scanned")
                        "files_scanned" -> jsonPrimitiveContent(root, "files_scanned")
                        "has_errors" -> jsonPrimitiveContent(root, "has_errors")
                        "content_hash" -> jsonPrimitiveContent(root, "content_hash")
                        "start_time" -> jsonPrimitiveContent(root, "start_time")
                        "duration" -> jsonPrimitiveContent(root, "duration")
                        "total_checks" -> jsonPrimitiveContent(root, "total_checks")
                        "passed_checks" -> jsonPrimitiveContent(root, "passed_checks")
                        "failed_checks" -> jsonPrimitiveContent(root, "failed_checks")
                        "success" -> jsonPrimitiveContent(root, "success")
                        "scanner_names" -> (root["scanner_names"] as? JsonArray)?.joinToString(",") { (it as? JsonPrimitive)?.content ?: it.toString() } ?: ""
                        else -> jsonPrimitiveContent(root, key)
                    }
                    summarySheet.value(rowIdx, 0, key)
                    summarySheet.value(rowIdx, 1, value)
                }
                summarySheet.range(0, 0, 10, 1).style().borderStyle(BorderStyle.THIN).set()
                summarySheet.range(0, 0, 0, 1).style().bold().set()

                if (fileMetadata != null) {
                    val fmHeaders = listOf(
                        "path", "file_size", "md5", "sha1", "sha256", "sha512",
                        "max_stack_depth", "opcode_count", "suspicious_count",
                        "ml_context_json", "license", "license_info_json", "copyright_notices_json", "license_files_nearby",
                        "is_dataset", "is_model", "risk_score", "scan_timestamp",
                        "content_hash", "pickle_files", "metadata_json"
                    )
                    val fmSheet = workbook.newWorksheet("FileMetadata")
                    fmHeaders.forEachIndexed { i, h -> fmSheet.value(0, i, h) }
                    fileMetadata.entries.toList().forEachIndexed { rowIdx, (path, value) ->
                        val obj = value as? JsonObject ?: return@forEachIndexed
                        val hashes = obj["file_hashes"] as? JsonObject
                        val pickleArr = obj["pickle_files"] as? JsonArray
                        val licenseFilesNearby = obj["license_files_nearby"] as? JsonArray
                        val r = rowIdx + 1
                        fmSheet.value(r, 0, path)
                        fmSheet.value(r, 1, jsonPrimitiveContent(obj, "file_size"))
                        fmSheet.value(r, 2, hashes?.get("md5")?.jsonPrimitive?.content ?: "")
                        fmSheet.value(r, 3, hashes?.get("sha1")?.jsonPrimitive?.content ?: "")
                        fmSheet.value(r, 4, hashes?.get("sha256")?.jsonPrimitive?.content ?: "")
                        fmSheet.value(r, 5, hashes?.get("sha512")?.jsonPrimitive?.content ?: "")
                        fmSheet.value(r, 6, jsonPrimitiveContent(obj, "max_stack_depth"))
                        fmSheet.value(r, 7, jsonPrimitiveContent(obj, "opcode_count"))
                        fmSheet.value(r, 8, jsonPrimitiveContent(obj, "suspicious_count"))
                        fmSheet.value(r, 9, jsonElementToString(obj["ml_context"]))
                        fmSheet.value(r, 10, jsonPrimitiveContent(obj, "license"))
                        fmSheet.value(r, 11, jsonElementToString(obj["license_info"]))
                        fmSheet.value(r, 12, jsonElementToString(obj["copyright_notices"]))
                        fmSheet.value(r, 13, arrayOfPrimitivesToCsv(licenseFilesNearby))
                        fmSheet.value(r, 14, jsonPrimitiveContent(obj, "is_dataset"))
                        fmSheet.value(r, 15, jsonPrimitiveContent(obj, "is_model"))
                        fmSheet.value(r, 16, jsonPrimitiveContent(obj, "risk_score"))
                        fmSheet.value(r, 17, jsonPrimitiveContent(obj, "scan_timestamp"))
                        fmSheet.value(r, 18, jsonPrimitiveContent(obj, "content_hash"))
                        fmSheet.value(r, 19, arrayOfPrimitivesToCsv(pickleArr))
                        fmSheet.value(r, 20, detailsJson(obj))
                    }
                    if (fileMetadata.isNotEmpty()) {
                        fmSheet.range(0, 0, fileMetadata.size, fmHeaders.size - 1).style().borderStyle(BorderStyle.THIN).set()
                        fmSheet.range(0, 0, 0, fmHeaders.size - 1).style().bold().set()
                    }
                }

                val issueHeaders = listOf("type", "message", "severity", "location", "why", "timestamp") + issueDetailKeys + "details_json"
                val issueSheet = workbook.newWorksheet("Issues")
                issueHeaders.forEachIndexed { i, h -> issueSheet.value(0, i, h) }
                issues.forEachIndexed { rowIdx, el ->
                    val obj = el as? JsonObject ?: return@forEachIndexed
                    val details = obj["details"] as? JsonObject
                    val r = rowIdx + 1
                    issueSheet.value(r, 0, jsonPrimitiveContent(obj, "type"))
                    issueSheet.value(r, 1, jsonPrimitiveContent(obj, "message"))
                    issueSheet.value(r, 2, jsonPrimitiveContent(obj, "severity"))
                    issueSheet.value(r, 3, jsonPrimitiveContent(obj, "location"))
                    issueSheet.value(r, 4, jsonPrimitiveContent(obj, "why"))
                    issueSheet.value(r, 5, jsonPrimitiveContent(obj, "timestamp"))
                    issueDetailKeys.forEachIndexed { i, key -> issueSheet.value(r, 6 + i, detailContent(details, key)) }
                    issueSheet.value(r, 6 + issueDetailKeys.size, detailsJson(details))
                }
                if (issues.isNotEmpty()) {
                    issueSheet.range(0, 0, issues.size, issueHeaders.size - 1).style().borderStyle(BorderStyle.THIN).set()
                    issueSheet.range(0, 0, 0, issueHeaders.size - 1).style().bold().set()
                }

                val checkHeaders = listOf("name", "status", "message", "location", "severity", "why", "timestamp", "cve_id", "module", "function", "opcode", "position", "associated_global", "pickle_filename", "ml_context_confidence", "opcode_count", "max_opcodes", "details_json")
                val checkSheet = workbook.newWorksheet("Checks")
                checkHeaders.forEachIndexed { i, h -> checkSheet.value(0, i, h) }
                checks.forEachIndexed { rowIdx, el ->
                    val obj = el as? JsonObject ?: return@forEachIndexed
                    val details = obj["details"] as? JsonObject
                    val r = rowIdx + 1
                    checkSheet.value(r, 0, jsonPrimitiveContent(obj, "name"))
                    checkSheet.value(r, 1, jsonPrimitiveContent(obj, "status"))
                    checkSheet.value(r, 2, jsonPrimitiveContent(obj, "message"))
                    checkSheet.value(r, 3, jsonPrimitiveContent(obj, "location"))
                    checkSheet.value(r, 4, jsonPrimitiveContent(obj, "severity"))
                    checkSheet.value(r, 5, jsonPrimitiveContent(obj, "why"))
                    checkSheet.value(r, 6, jsonPrimitiveContent(obj, "timestamp"))
                    checkSheet.value(r, 7, detailContent(details, "cve_id"))
                    checkSheet.value(r, 8, detailContent(details, "module"))
                    checkSheet.value(r, 9, detailContent(details, "function"))
                    checkSheet.value(r, 10, detailContent(details, "opcode"))
                    checkSheet.value(r, 11, detailContent(details, "position"))
                    checkSheet.value(r, 12, detailContent(details, "associated_global"))
                    checkSheet.value(r, 13, detailContent(details, "pickle_filename"))
                    checkSheet.value(r, 14, detailContent(details, "ml_context_confidence"))
                    checkSheet.value(r, 15, detailContent(details, "opcode_count"))
                    checkSheet.value(r, 16, detailContent(details, "max_opcodes"))
                    checkSheet.value(r, 17, detailsJson(details))
                }
                if (checks.isNotEmpty()) {
                    checkSheet.range(0, 0, checks.size, checkHeaders.size - 1).style().borderStyle(BorderStyle.THIN).set()
                    checkSheet.range(0, 0, 0, checkHeaders.size - 1).style().bold().set()
                }

                val assetHeaders = listOf("path", "type", "size", "tensors", "keys", "contents_json")
                val assetSheet = workbook.newWorksheet("Assets")
                assetHeaders.forEachIndexed { i, h -> assetSheet.value(0, i, h) }
                assets.forEachIndexed { rowIdx, el ->
                    val obj = el as? JsonObject ?: return@forEachIndexed
                    val r = rowIdx + 1
                    assetSheet.value(r, 0, jsonPrimitiveContent(obj, "path"))
                    assetSheet.value(r, 1, jsonPrimitiveContent(obj, "type"))
                    assetSheet.value(r, 2, obj["size"]?.jsonPrimitive?.content ?: "")
                    assetSheet.value(r, 3, arrayOfPrimitivesToCsv(obj["tensors"] as? JsonArray))
                    assetSheet.value(r, 4, arrayOfPrimitivesToCsv(obj["keys"] as? JsonArray))
                    assetSheet.value(r, 5, jsonElementToString(obj["contents"]))
                }
                if (assets.isNotEmpty()) {
                    assetSheet.range(0, 0, assets.size, assetHeaders.size - 1).style().borderStyle(BorderStyle.THIN).set()
                    assetSheet.range(0, 0, 0, assetHeaders.size - 1).style().bold().set()
                }
            }
        }
    }

    private suspend fun writeXML(reportFile: File, root: JsonObject) {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        val aimReport = doc.createElement("aim_report")
        doc.appendChild(aimReport)

        fun addTextElement(parent: Element, name: String, value: String) {
            val el = doc.createElement(name)
            el.appendChild(doc.createTextNode(value))
            parent.appendChild(el)
        }

        val summaryEl = doc.createElement("summary")
        aimReport.appendChild(summaryEl)
        listOf(
            "bytes_scanned", "files_scanned", "has_errors", "content_hash", "start_time", "duration",
            "total_checks", "passed_checks", "failed_checks", "success", "scanner_names"
        ).forEach { key ->
            val value = when (key) {
                "bytes_scanned" -> jsonPrimitiveContent(root, "bytes_scanned")
                "files_scanned" -> jsonPrimitiveContent(root, "files_scanned")
                "has_errors" -> jsonPrimitiveContent(root, "has_errors")
                "content_hash" -> jsonPrimitiveContent(root, "content_hash")
                "start_time" -> jsonPrimitiveContent(root, "start_time")
                "duration" -> jsonPrimitiveContent(root, "duration")
                "total_checks" -> jsonPrimitiveContent(root, "total_checks")
                "passed_checks" -> jsonPrimitiveContent(root, "passed_checks")
                "failed_checks" -> jsonPrimitiveContent(root, "failed_checks")
                "success" -> jsonPrimitiveContent(root, "success")
                "scanner_names" -> (root["scanner_names"] as? JsonArray)?.joinToString(",") { (it as? JsonPrimitive)?.content ?: it.toString() } ?: ""
                else -> jsonPrimitiveContent(root, key)
            }
            addTextElement(summaryEl, key, value)
        }

        val fileMetadata = root["file_metadata"] as? JsonObject
        if (fileMetadata != null) {
            val fmEl = doc.createElement("file_metadata")
            aimReport.appendChild(fmEl)
            fileMetadata.entries.forEach { (path, value) ->
                val obj = value as? JsonObject ?: return@forEach
                val hashes = obj["file_hashes"] as? JsonObject
                val fileEl = doc.createElement("file")
                fmEl.appendChild(fileEl)
                addTextElement(fileEl, "path", path)
                addTextElement(fileEl, "file_size", jsonPrimitiveContent(obj, "file_size"))
                addTextElement(fileEl, "md5", hashes?.get("md5")?.jsonPrimitive?.content ?: "")
                addTextElement(fileEl, "sha1", hashes?.get("sha1")?.jsonPrimitive?.content ?: "")
                addTextElement(fileEl, "sha256", hashes?.get("sha256")?.jsonPrimitive?.content ?: "")
                addTextElement(fileEl, "sha512", hashes?.get("sha512")?.jsonPrimitive?.content ?: "")
                addTextElement(fileEl, "max_stack_depth", jsonPrimitiveContent(obj, "max_stack_depth"))
                addTextElement(fileEl, "opcode_count", jsonPrimitiveContent(obj, "opcode_count"))
                addTextElement(fileEl, "suspicious_count", jsonPrimitiveContent(obj, "suspicious_count"))
                addTextElement(fileEl, "license", jsonPrimitiveContent(obj, "license"))
                addTextElement(fileEl, "license_info_json", jsonElementToString(obj["license_info"]))
                addTextElement(fileEl, "copyright_notices_json", jsonElementToString(obj["copyright_notices"]))
                addTextElement(fileEl, "license_files_nearby", arrayOfPrimitivesToCsv(obj["license_files_nearby"] as? JsonArray))
                addTextElement(fileEl, "is_dataset", jsonPrimitiveContent(obj, "is_dataset"))
                addTextElement(fileEl, "is_model", jsonPrimitiveContent(obj, "is_model"))
                addTextElement(fileEl, "risk_score", jsonPrimitiveContent(obj, "risk_score"))
                addTextElement(fileEl, "scan_timestamp", jsonPrimitiveContent(obj, "scan_timestamp"))
                addTextElement(fileEl, "ml_context_json", jsonElementToString(obj["ml_context"]))
                addTextElement(fileEl, "metadata_json", detailsJson(obj))
            }
        }

        val issues = sortedIssues(root)
        val issuesEl = doc.createElement("issues")
        aimReport.appendChild(issuesEl)
        issues.forEach { el ->
            val obj = el as? JsonObject ?: return@forEach
            val issueEl = doc.createElement("issue")
            issuesEl.appendChild(issueEl)
            addTextElement(issueEl, "type", jsonPrimitiveContent(obj, "type"))
            addTextElement(issueEl, "message", jsonPrimitiveContent(obj, "message"))
            addTextElement(issueEl, "severity", jsonPrimitiveContent(obj, "severity"))
            addTextElement(issueEl, "location", jsonPrimitiveContent(obj, "location"))
            addTextElement(issueEl, "why", jsonPrimitiveContent(obj, "why"))
            addTextElement(issueEl, "timestamp", jsonPrimitiveContent(obj, "timestamp"))
            val details = obj["details"] as? JsonObject
            if (details != null) {
                val detailsEl = doc.createElement("details")
                detailsEl.appendChild(doc.createTextNode(detailsJson(details)))
                issueEl.appendChild(detailsEl)
            }
        }

        val checks = root["checks"] as? JsonArray ?: JsonArray(emptyList())
        val checksEl = doc.createElement("checks")
        aimReport.appendChild(checksEl)
        checks.forEach { el ->
            val obj = el as? JsonObject ?: return@forEach
            val checkEl = doc.createElement("check")
            checksEl.appendChild(checkEl)
            addTextElement(checkEl, "name", jsonPrimitiveContent(obj, "name"))
            addTextElement(checkEl, "status", jsonPrimitiveContent(obj, "status"))
            addTextElement(checkEl, "message", jsonPrimitiveContent(obj, "message"))
            addTextElement(checkEl, "location", jsonPrimitiveContent(obj, "location"))
            addTextElement(checkEl, "severity", jsonPrimitiveContent(obj, "severity"))
            addTextElement(checkEl, "why", jsonPrimitiveContent(obj, "why"))
            addTextElement(checkEl, "timestamp", jsonPrimitiveContent(obj, "timestamp"))
            val details = obj["details"] as? JsonObject
            if (details != null) {
                val detailsEl = doc.createElement("details")
                detailsEl.appendChild(doc.createTextNode(detailsJson(details)))
                checkEl.appendChild(detailsEl)
            }
        }

        val assets = root["assets"] as? JsonArray ?: JsonArray(emptyList())
        val assetsEl = doc.createElement("assets")
        aimReport.appendChild(assetsEl)
        assets.forEach { el ->
            val obj = el as? JsonObject ?: return@forEach
            val assetEl = doc.createElement("asset")
            assetsEl.appendChild(assetEl)
            addTextElement(assetEl, "path", jsonPrimitiveContent(obj, "path"))
            addTextElement(assetEl, "type", jsonPrimitiveContent(obj, "type"))
            addTextElement(assetEl, "size", obj["size"]?.jsonPrimitive?.content ?: "")
            addTextElement(assetEl, "tensors", arrayOfPrimitivesToCsv(obj["tensors"] as? JsonArray))
            addTextElement(assetEl, "keys", arrayOfPrimitivesToCsv(obj["keys"] as? JsonArray))
            addTextElement(assetEl, "contents_json", jsonElementToString(obj["contents"]))
        }

        withContext(Dispatchers.IO) {
            val transformer = TransformerFactory.newInstance().newTransformer()
            transformer.setOutputProperty(OutputKeys.INDENT, "yes")
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
            transformer.transform(DOMSource(doc), StreamResult(reportFile))
        }
    }
}
