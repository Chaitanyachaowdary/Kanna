package com.example.data.repository

import com.example.data.db.AiActionHistoryEntity

/** Formats supported when exporting the AI action history. */
enum class ExportFormat(val mimeType: String, val extension: String) {
    JSON("application/json", "json"),
    CSV("text/csv", "csv"),
}

/**
 * Serializes AI action history to JSON or CSV for the user to export/audit.
 *
 * Pure and self-contained (no `org.json`, which is unavailable in plain JVM
 * unit tests) so the escaping logic can be tested directly.
 */
object AiActionHistoryExporter {

    fun export(items: List<AiActionHistoryEntity>, format: ExportFormat): String = when (format) {
        ExportFormat.JSON -> toJson(items)
        ExportFormat.CSV -> toCsv(items)
    }

    fun toJson(items: List<AiActionHistoryEntity>): String {
        val sb = StringBuilder("[\n")
        items.forEachIndexed { index, a ->
            sb.append("  {")
                .append("\"id\":").append(a.id).append(',')
                .append("\"actionType\":").append(jsonString(a.actionType)).append(',')
                .append("\"inputPrompt\":").append(jsonString(a.inputPrompt)).append(',')
                .append("\"generatedResponse\":").append(jsonString(a.generatedResponse)).append(',')
                .append("\"timestamp\":").append(a.timestamp)
                .append('}')
            if (index < items.lastIndex) sb.append(',')
            sb.append('\n')
        }
        sb.append("]\n")
        return sb.toString()
    }

    fun toCsv(items: List<AiActionHistoryEntity>): String {
        val sb = StringBuilder("id,actionType,inputPrompt,generatedResponse,timestamp\r\n")
        items.forEach { a ->
            sb.append(a.id).append(',')
                .append(csvField(a.actionType)).append(',')
                .append(csvField(a.inputPrompt)).append(',')
                .append(csvField(a.generatedResponse)).append(',')
                .append(a.timestamp).append("\r\n")
        }
        return sb.toString()
    }

    /**
     * Wraps a JSON string value in quotes with full escaping. Control characters
     * below 0x20 (including backspace and form-feed) are emitted as \uXXXX.
     */
    private fun jsonString(value: String): String {
        val sb = StringBuilder("\"")
        value.forEach { c ->
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> if (c < ' ') sb.append("\\u%04x".format(c.code)) else sb.append(c)
            }
        }
        return sb.append('"').toString()
    }

    /** Quotes a CSV field, doubling embedded quotes (RFC 4180). */
    private fun csvField(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""
}
