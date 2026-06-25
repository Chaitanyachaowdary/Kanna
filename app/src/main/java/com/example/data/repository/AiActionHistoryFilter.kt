package com.example.data.repository

import com.example.data.db.AiActionHistoryEntity

/**
 * Filters AI action history for the dashboard. Pure and side-effect free so it
 * can be unit-tested directly and reused by the ViewModel's derived flow.
 *
 * @param category when non-null, keep only actions whose [AiActionCategory]
 *   (derived from `actionType`) matches.
 * @param query case-insensitive substring matched against the action type,
 *   input prompt, and generated response. Blank matches everything.
 */
fun filterAiActions(
    items: List<AiActionHistoryEntity>,
    query: String,
    category: AiActionCategory?,
): List<AiActionHistoryEntity> {
    val trimmed = query.trim()
    return items.filter { action ->
        val matchesCategory =
            category == null || AiActionCategory.fromActionType(action.actionType) == category
        val matchesQuery = trimmed.isEmpty() ||
            action.actionType.contains(trimmed, ignoreCase = true) ||
            action.inputPrompt.contains(trimmed, ignoreCase = true) ||
            action.generatedResponse.contains(trimmed, ignoreCase = true)
        matchesCategory && matchesQuery
    }
}
