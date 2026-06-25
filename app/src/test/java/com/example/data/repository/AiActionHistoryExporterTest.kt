package com.example.data.repository

import com.example.data.db.AiActionHistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pure JVM tests for [AiActionHistoryExporter] — no Android/Robolectric needed. */
class AiActionHistoryExporterTest {

    private fun action(
        id: Int,
        type: String = "Chat Response",
        input: String = "in",
        output: String = "out",
        ts: Long = 100L,
    ) = AiActionHistoryEntity(id, type, input, output, ts)

    @Test
    fun `json escapes quotes, backslashes and newlines`() {
        val items = listOf(action(id = 1, output = "line1\nline2 \"quote\" \\slash"))

        val json = AiActionHistoryExporter.toJson(items)

        assertTrue(json.contains("\"generatedResponse\":\"line1\\nline2 \\\"quote\\\" \\\\slash\""))
        // Structural sanity.
        assertTrue(json.trimStart().startsWith("["))
        assertTrue(json.trimEnd().endsWith("]"))
    }

    @Test
    fun `json control characters become unicode escapes`() {
        val items = listOf(action(id = 1, input = "tab\tbell"))
        val json = AiActionHistoryExporter.toJson(items)

        assertTrue(json.contains("tab\\tbell\\u0007"))
    }

    @Test
    fun `json separates multiple records with commas`() {
        val json = AiActionHistoryExporter.toJson(listOf(action(1), action(2)))
        // Two object closings, one element separator between them.
        assertEquals(2, json.split("}").size - 1)
        assertTrue(json.contains("},"))
    }

    @Test
    fun `empty list produces an empty json array`() {
        assertEquals("[\n]\n", AiActionHistoryExporter.toJson(emptyList()))
    }

    @Test
    fun `csv has a header and quotes fields, doubling embedded quotes`() {
        val items = listOf(action(id = 7, type = "Email Analysis", input = "say \"hi\"", output = "a,b", ts = 42L))

        val csv = AiActionHistoryExporter.toCsv(items)
        val lines = csv.split("\r\n")

        assertEquals("id,actionType,inputPrompt,generatedResponse,timestamp", lines[0])
        // Embedded quotes doubled; comma-containing field quoted; row well-formed.
        assertEquals("7,\"Email Analysis\",\"say \"\"hi\"\"\",\"a,b\",42", lines[1])
    }

    @Test
    fun `export dispatches on the requested format`() {
        val items = listOf(action(1))
        assertEquals(AiActionHistoryExporter.toJson(items), AiActionHistoryExporter.export(items, ExportFormat.JSON))
        assertEquals(AiActionHistoryExporter.toCsv(items), AiActionHistoryExporter.export(items, ExportFormat.CSV))
    }
}
