package com.example.data.diagnostics

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ServiceStatus {
    UNTESTED,
    TESTING,
    CONNECTED,
    FAILED,
    MISSING_KEY
}

object GeminiKeyManager {
    var customApiKey: String = ""
    var customEndpoint: String = "https://generativelanguage.googleapis.com/"
    var customModelOverride: String = ""
    var customTimeoutSeconds: Int = 60
    var backoffStrategy: String = "Aggressive"

    fun getApiKey(): String {
        return if (customApiKey.isNotBlank()) customApiKey else com.example.BuildConfig.GEMINI_API_KEY
    }
}

data class DiagnosticLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val module: String, // "GEMINI_API" or "LOCAL_STORAGE", "SYSTEM"
    val level: String, // "INFO", "WARN", "ERROR"
    val message: String,
    val details: String? = null,
    val latencyMs: Long? = null
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
}

object AuraDiagnostics {
    private val _logs = MutableStateFlow<List<DiagnosticLog>>(emptyList())
    val logs: StateFlow<List<DiagnosticLog>> = _logs.asStateFlow()

    private val _errorEvents = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val errorEvents: SharedFlow<String> = _errorEvents.asSharedFlow()

    private val _geminiStatus = MutableStateFlow(ServiceStatus.UNTESTED)
    val geminiStatus: StateFlow<ServiceStatus> = _geminiStatus.asStateFlow()

    private val _localStorageStatus = MutableStateFlow(ServiceStatus.UNTESTED)
    val localStorageStatus: StateFlow<ServiceStatus> = _localStorageStatus.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    fun log(module: String, level: String, message: String, details: String? = null, latencyMs: Long? = null) {
        val newLog = DiagnosticLog(
            module = module,
            level = level,
            message = message,
            details = details,
            latencyMs = latencyMs
        )
        // Cap logs at 200 entries to optimize memory
        _logs.value = (listOf(newLog) + _logs.value).take(200)

        if (level == "ERROR") {
            _errorEvents.tryEmit(message)
        }
    }

    fun setGeminiStatus(status: ServiceStatus) {
        _geminiStatus.value = status
        if (status == ServiceStatus.CONNECTED) {
            _lastErrorMessage.value = null
        }
    }

    fun setLastErrorMessage(message: String?) {
        _lastErrorMessage.value = message
    }

    fun setLocalStorageStatus(status: ServiceStatus) {
        _localStorageStatus.value = status
    }

    fun clear() {
        _logs.value = emptyList()
        log("SYSTEM", "INFO", "Diagnostic logs flushed and initiated.")
    }
}
