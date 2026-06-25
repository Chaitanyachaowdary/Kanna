package com.example.data.repository

import android.content.SharedPreferences

/**
 * Broad categories of AI actions, used so the user can opt specific kinds of
 * activity in or out of the on-device history. Each persisted `actionType`
 * string maps to exactly one category via [fromActionType].
 */
enum class AiActionCategory(val label: String) {
    CHAT("Chat"),
    NOTIFICATION("Notification"),
    MAIL("Mail"),
    VOICE("Voice"),
    SOCIAL("Social"),
    MEETING("Meeting"),
    OTHER("Other");

    companion object {
        /** Maps a repository `actionType` (e.g. "Email Analysis", "Call Screening Summary (Offline)") to a category. */
        fun fromActionType(actionType: String): AiActionCategory = when {
            actionType.startsWith("Chat") -> CHAT
            actionType.startsWith("Notification") -> NOTIFICATION
            actionType.startsWith("Prioritized Digest") -> NOTIFICATION
            actionType.startsWith("Email") -> MAIL
            actionType.startsWith("Voice") -> VOICE
            actionType.startsWith("Social") -> SOCIAL
            actionType.startsWith("LinkedIn") -> SOCIAL
            actionType.startsWith("Call") -> MEETING
            actionType.startsWith("Meeting") -> MEETING
            else -> OTHER
        }
    }
}

/**
 * Decides whether a given AI action should be recorded to the local history.
 * Injected into [AuraRepository] so every logging call site is gated in one
 * place ([AuraRepository.insertAiAction]).
 */
interface AiLoggingPolicy {
    fun shouldLog(actionType: String): Boolean

    /** Default policy used by tests and any caller that doesn't supply one: log everything. */
    object AlwaysOn : AiLoggingPolicy {
        override fun shouldLog(actionType: String): Boolean = true
    }
}

/**
 * [AiLoggingPolicy] backed by the app's shared preferences, so the user's
 * toggles (written by the ViewModel) and the repository's read share one source
 * of truth. A missing key defaults to enabled, preserving prior behaviour for
 * users who never opened the settings.
 */
class SharedPrefsAiLoggingPolicy(private val prefs: SharedPreferences) : AiLoggingPolicy {

    override fun shouldLog(actionType: String): Boolean {
        if (!prefs.getBoolean(KEY_MASTER, true)) return false
        val category = AiActionCategory.fromActionType(actionType)
        return prefs.getBoolean(categoryKey(category), true)
    }

    companion object {
        const val KEY_MASTER = "ai_logging_enabled"
        fun categoryKey(category: AiActionCategory): String = "ai_logging_cat_${category.name}"
    }
}
