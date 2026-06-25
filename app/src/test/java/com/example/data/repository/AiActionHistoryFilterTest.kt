package com.example.data.repository

import com.example.data.db.AiActionHistoryEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/** Pure JVM tests for [filterAiActions] — no Android/Robolectric needed. */
class AiActionHistoryFilterTest {

    private val items = listOf(
        AiActionHistoryEntity(id = 1, actionType = "Chat Response", inputPrompt = "weather today?", generatedResponse = "sunny", timestamp = 1),
        AiActionHistoryEntity(id = 2, actionType = "Email Analysis", inputPrompt = "invoice from Acme", generatedResponse = "summary: payment due", timestamp = 2),
        AiActionHistoryEntity(id = 3, actionType = "LinkedIn Post Draft", inputPrompt = "promotion news", generatedResponse = "Excited to share...", timestamp = 3),
        AiActionHistoryEntity(id = 4, actionType = "Call Screening Summary", inputPrompt = "unknown caller", generatedResponse = "spam likely", timestamp = 4),
    )

    @Test
    fun `no filters returns everything`() {
        assertEquals(items, filterAiActions(items, query = "", category = null))
    }

    @Test
    fun `category filter keeps only matching category`() {
        // LinkedIn maps to SOCIAL.
        val result = filterAiActions(items, query = "", category = AiActionCategory.SOCIAL)
        assertEquals(listOf(3), result.map { it.id })
    }

    @Test
    fun `category filter spans multiple action types`() {
        // Call Screening Summary maps to MEETING.
        val result = filterAiActions(items, query = "", category = AiActionCategory.MEETING)
        assertEquals(listOf(4), result.map { it.id })
    }

    @Test
    fun `text query matches input, output, and action type case-insensitively`() {
        assertEquals(listOf(2), filterAiActions(items, query = "acme", category = null).map { it.id })
        assertEquals(listOf(4), filterAiActions(items, query = "SPAM", category = null).map { it.id })
        assertEquals(listOf(1), filterAiActions(items, query = "chat", category = null).map { it.id })
    }

    @Test
    fun `category and query combine conjunctively`() {
        // SOCIAL category + a query that only matches a non-social row -> empty.
        assertEquals(emptyList<Int>(), filterAiActions(items, query = "invoice", category = AiActionCategory.SOCIAL).map { it.id })
        // SOCIAL category + a query that matches the social row -> kept.
        assertEquals(listOf(3), filterAiActions(items, query = "promotion", category = AiActionCategory.SOCIAL).map { it.id })
    }

    @Test
    fun `blank-ish query is treated as no query`() {
        assertEquals(items.size, filterAiActions(items, query = "   ", category = null).size)
    }
}
