package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AuraWakeWordWatcher
import com.example.data.repository.AiActionCategory
import com.example.data.repository.SharedPrefsAiLoggingPolicy
import com.example.data.db.ChatMessageEntity
import com.example.data.db.UserProfileEntity
import com.example.data.db.NotificationEntity
import com.example.data.db.EmailEntity
import com.example.data.db.SecureFileEntity
import com.example.data.db.SocialPostEntity
import com.example.data.db.CallSessionEntity
import com.example.data.db.CalendarEventEntity
import com.example.data.db.AuraTaskEntity
import com.example.data.db.PrivacyInsightEntity
import com.example.data.db.CallScreeningRuleEntity
import com.example.data.db.EmailTemplateEntity
import com.example.data.repository.AuraRepository
import com.example.data.diagnostics.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AuraViewModel(private val repository: AuraRepository) : ViewModel() {

    // streams from DB
    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val notifications: StateFlow<List<NotificationEntity>> = repository.notifications
        .map { list -> list.filter { !it.silencedByDeepWork } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emails: StateFlow<List<EmailEntity>> = repository.emails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val secureFiles: StateFlow<List<SecureFileEntity>> = repository.secureFiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val socialPosts: StateFlow<List<SocialPostEntity>> = repository.socialPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callSessions: StateFlow<List<CallSessionEntity>> = repository.callSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val calendarEvents: StateFlow<List<CalendarEventEntity>> = repository.calendarEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auraTasks: StateFlow<List<AuraTaskEntity>> = repository.auraTasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val privacyInsights: StateFlow<List<PrivacyInsightEntity>> = repository.privacyInsights
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callScreeningRules: StateFlow<List<CallScreeningRuleEntity>> = repository.callScreeningRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val emailTemplates: StateFlow<List<EmailTemplateEntity>> = repository.emailTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val screenedTranscripts: StateFlow<List<com.example.data.db.ScreenedTranscriptEntity>> = repository.screenedTranscripts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val contacts: StateFlow<List<com.example.data.db.AuraContactEntity>> = repository.contacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val versionInstallations: StateFlow<List<com.example.data.db.VersionInstallationEntity>> = repository.versionInstallations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val aiActionHistory: StateFlow<List<com.example.data.db.AiActionHistoryEntity>> = repository.aiActionHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dashboard search/filter state for the AI action history.
    private val _aiHistorySearchQuery = MutableStateFlow("")
    val aiHistorySearchQuery: StateFlow<String> = _aiHistorySearchQuery.asStateFlow()

    private val _aiHistoryCategoryFilter = MutableStateFlow<AiActionCategory?>(null)
    val aiHistoryCategoryFilter: StateFlow<AiActionCategory?> = _aiHistoryCategoryFilter.asStateFlow()

    val filteredAiActionHistory: StateFlow<List<com.example.data.db.AiActionHistoryEntity>> =
        combine(repository.aiActionHistory, _aiHistorySearchQuery, _aiHistoryCategoryFilter) { list, query, category ->
            com.example.data.repository.filterAiActions(list, query, category)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateAiHistorySearchQuery(query: String) {
        _aiHistorySearchQuery.value = query
    }

    fun updateAiHistoryCategoryFilter(category: AiActionCategory?) {
        _aiHistoryCategoryFilter.value = category
    }

    /** Writes the currently filtered AI action history to [uri] in [format], off the main thread. */
    fun exportAiActionHistory(
        uri: android.net.Uri,
        format: com.example.data.repository.ExportFormat,
        contentResolver: android.content.ContentResolver,
    ) {
        val items = filteredAiActionHistory.value
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val payload = com.example.data.repository.AiActionHistoryExporter.export(items, format)
                contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(payload.toByteArray(Charsets.UTF_8))
                } ?: throw java.io.IOException("Could not open output stream for export.")
                AuraDiagnostics.log("SYSTEM", "INFO", "Exported ${items.size} AI action records as ${format.name}.")
                addPrivacyInsight("Data Portability", "Exported ${items.size} AI action history records to a user-selected ${format.name} file.")
            } catch (e: Exception) {
                AuraDiagnostics.log("SYSTEM", "ERROR", "AI action history export failed: ${e.message}")
            }
        }
    }

    // Deep Work state
    private val _isDeepWorkActive = MutableStateFlow(false)
    val isDeepWorkActive: StateFlow<Boolean> = _isDeepWorkActive.asStateFlow()

    private val _deepWorkTimeRemaining = MutableStateFlow(0L) // Remaining ms
    val deepWorkTimeRemaining: StateFlow<Long> = _deepWorkTimeRemaining.asStateFlow()

    private val _deepWorkDurationMinutes = MutableStateFlow(25) // Default 25 min duration
    val deepWorkDurationMinutes: StateFlow<Int> = _deepWorkDurationMinutes.asStateFlow()

    private var deepWorkTimerJob: kotlinx.coroutines.Job? = null

    private val _professionalTone = MutableStateFlow("Formal")
    val professionalTone: StateFlow<String> = _professionalTone.asStateFlow()

    // UI Configuration States
    private val _highThinkingEnabled = MutableStateFlow(false)
    val highThinkingEnabled: StateFlow<Boolean> = _highThinkingEnabled.asStateFlow()

    private val _isOnDeviceProcessingEnabled = MutableStateFlow(true)
    val isOnDeviceProcessingEnabled: StateFlow<Boolean> = _isOnDeviceProcessingEnabled.asStateFlow()

    // Autonomous Social & Corporate Automation elements
    private val _socialTrendResult = MutableStateFlow("")
    val socialTrendResult: StateFlow<String> = _socialTrendResult.asStateFlow()

    private val _linkedinDraftPost = MutableStateFlow("")
    val linkedinDraftPost: StateFlow<String> = _linkedinDraftPost.asStateFlow()

    private val _isSocialProcessing = MutableStateFlow(false)
    val isSocialProcessing: StateFlow<Boolean> = _isSocialProcessing.asStateFlow()

    private val _meetingAnswerResult = MutableStateFlow("")
    val meetingAnswerResult: StateFlow<String> = _meetingAnswerResult.asStateFlow()

    private val _isMeetingProcessing = MutableStateFlow(false)
    val isMeetingProcessing: StateFlow<Boolean> = _isMeetingProcessing.asStateFlow()

    private val _isRepresentedInMeeting = MutableStateFlow(false)
    val isRepresentedInMeeting: StateFlow<Boolean> = _isRepresentedInMeeting.asStateFlow()

    private val _activeMeetingTranscript = MutableStateFlow("CHAIRPERSON: Thanks everyone for joining this product sync. Let's make sure the automation features represent Chaitanya cleanly.\nDEVELOPER (Kanna Representation): Yes, Chaitanya is focused right now. Kanna is attending and tracking transcript logs seamlessly. We are ready on the deployment metrics.")
    val activeMeetingTranscript: StateFlow<String> = _activeMeetingTranscript.asStateFlow()

    private val _isChatPending = MutableStateFlow(false)
    val isChatPending: StateFlow<Boolean> = _isChatPending.asStateFlow()

    // Processing status for notifications & emails
    private val _processingNotificationId = MutableStateFlow<Int?>(null)
    val processingNotificationId: StateFlow<Int?> = _processingNotificationId.asStateFlow()

    private val _processingEmailId = MutableStateFlow<Int?>(null)
    val processingEmailId: StateFlow<Int?> = _processingEmailId.asStateFlow()

    // Kanna Voice Core States
    private val _isVoiceActive = MutableStateFlow(false)
    val isVoiceActive: StateFlow<Boolean> = _isVoiceActive.asStateFlow()

    private val _voiceOutputText = MutableStateFlow("")
    val voiceOutputText: StateFlow<String> = _voiceOutputText.asStateFlow()

    private val _isKannaOverlayVisible = MutableStateFlow(false)
    val isKannaOverlayVisible: StateFlow<Boolean> = _isKannaOverlayVisible.asStateFlow()

    // Context-Aware One-tap Reply Suggestions
    private val _currentSuggestions = MutableStateFlow<List<String>>(emptyList())
    val currentSuggestions: StateFlow<List<String>> = _currentSuggestions.asStateFlow()

    private val _isSuggestionsLoading = MutableStateFlow(false)
    val isSuggestionsLoading: StateFlow<Boolean> = _isSuggestionsLoading.asStateFlow()

    // Aggregated prioritized digest state
    private val _prioritizedDigest = MutableStateFlow("")
    val prioritizedDigest: StateFlow<String> = _prioritizedDigest.asStateFlow()

    private val _isDigestLoading = MutableStateFlow(false)
    val isDigestLoading: StateFlow<Boolean> = _isDigestLoading.asStateFlow()

    // Updates and Version Management state flows
    private val _currentVersion = MutableStateFlow("2.4.1")
    val currentVersion: StateFlow<String> = _currentVersion.asStateFlow()
    private val _availableVersion = MutableStateFlow("2.4.1")
    val availableVersion: StateFlow<String> = _availableVersion.asStateFlow()

    private val _isUpdateAvailable = MutableStateFlow(false)
    val isUpdateAvailable: StateFlow<Boolean> = _isUpdateAvailable.asStateFlow()

    private val _isCheckingForUpdates = MutableStateFlow(false)
    val isCheckingForUpdates: StateFlow<Boolean> = _isCheckingForUpdates.asStateFlow()

    private val _updateStatusMessage = MutableStateFlow("Version 2.4.1 (Latest)")
    val updateStatusMessage: StateFlow<String> = _updateStatusMessage.asStateFlow()

    // Recent Replies undo list state
    private val _recentReplies = MutableStateFlow<List<RecentReplyItem>>(emptyList())
    val recentReplies: StateFlow<List<RecentReplyItem>> = _recentReplies.asStateFlow()

    // Meetings & Calendar Auto-Join Recording States
    private val _isRecordingActive = MutableStateFlow(false)
    val isRecordingActive: StateFlow<Boolean> = _isRecordingActive.asStateFlow()

    private val _joiningEventId = MutableStateFlow<Int?>(null)
    val joiningEventId: StateFlow<Int?> = _joiningEventId.asStateFlow()

    private val _joiningEventTitle = MutableStateFlow("")
    val joiningEventTitle: StateFlow<String> = _joiningEventTitle.asStateFlow()

    // Background LinkedIn Scraping and Professional illustration details
    private val _isScrapingInProgress = MutableStateFlow(false)
    val isScrapingInProgress: StateFlow<Boolean> = _isScrapingInProgress.asStateFlow()

    private val _generatedLinkedInVisualPrompt = MutableStateFlow("")
    val generatedLinkedInVisualPrompt: StateFlow<String> = _generatedLinkedInVisualPrompt.asStateFlow()

    private val _linkedinVisualImageReady = MutableStateFlow(false)
    val linkedinVisualImageReady: StateFlow<Boolean> = _linkedinVisualImageReady.asStateFlow()

    // --- Privacy Mode & Wake Word Preference States ---
    private var appContext: Context? = null
    private var prefs: android.content.SharedPreferences? = null
    private var wakeWordWatcher: AuraWakeWordWatcher? = null

    private val _isPrivacyModeActive = MutableStateFlow(false)
    val isPrivacyModeActive: StateFlow<Boolean> = _isPrivacyModeActive.asStateFlow()

    private val _quietHoursEnabled = MutableStateFlow(false)
    val quietHoursEnabled: StateFlow<Boolean> = _quietHoursEnabled.asStateFlow()

    private val _quietHoursStart = MutableStateFlow("22:00")
    val quietHoursStart: StateFlow<String> = _quietHoursStart.asStateFlow()

    private val _quietHoursEnd = MutableStateFlow("07:00")
    val quietHoursEnd: StateFlow<String> = _quietHoursEnd.asStateFlow()

    private val _batteryLevel = MutableStateFlow(100)
    val batteryLevel: StateFlow<Int> = _batteryLevel.asStateFlow()

    val isLowBatteryModeActive: StateFlow<Boolean> = _batteryLevel
        .map { it < 15 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isPrivacyModeEffectivelyActive: StateFlow<Boolean> = combine(
        _isPrivacyModeActive,
        _quietHoursEnabled,
        _quietHoursStart,
        _quietHoursEnd,
        _batteryLevel
    ) { priv, qhEnabled, qhStart, qhEnd, batt ->
        if (priv) return@combine true
        if (batt < 15) return@combine true
        if (!qhEnabled) return@combine false
        try {
            val startParts = qhStart.split(":").map { it.trim().toInt() }
            val endParts = qhEnd.split(":").map { it.trim().toInt() }
            if (startParts.size != 2 || endParts.size != 2) return@combine false
            val startMin = startParts[0] * 60 + startParts[1]
            val endMin = endParts[0] * 60 + endParts[1]

            val cal = java.util.Calendar.getInstance()
            val currentMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)

            if (startMin <= endMin) {
                currentMin in startMin..endMin
            } else {
                currentMin >= startMin || currentMin <= endMin
            }
        } catch (e: Exception) {
            false
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // --- Interactive AI Phone Call Simulator States ---
    private val _isIncomingCallActive = MutableStateFlow(false)
    val isIncomingCallActive: StateFlow<Boolean> = _isIncomingCallActive.asStateFlow()

    private val _isCallActive = MutableStateFlow(false)
    val isCallActive: StateFlow<Boolean> = _isCallActive.asStateFlow()

    private val _callerName = MutableStateFlow("Recruiter Sarah")
    val callerName: StateFlow<String> = _callerName.asStateFlow()

    private val _callerStatus = MutableStateFlow("Incoming Phone Call")
    val callerStatus: StateFlow<String> = _callerStatus.asStateFlow()

    private val _callTranscripts = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val callTranscripts: StateFlow<List<ChatMessageEntity>> = _callTranscripts.asStateFlow()

    private val _callSummary = MutableStateFlow("")
    val callSummary: StateFlow<String> = _callSummary.asStateFlow()

    // --- Auto-Answer, Assistant Name, TTS & Caller Filtering configuration states ---
    private val _autoAnswerEnabled = MutableStateFlow(true)
    val autoAnswerEnabled: StateFlow<Boolean> = _autoAnswerEnabled.asStateFlow()

    private val _customGreetingScript = MutableStateFlow("Hello, my name is Kanna. I am attending this call instead of Chaitanya since he is currently busy. Please declare your identity and your message, and I will record it.")
    val customGreetingScript: StateFlow<String> = _customGreetingScript.asStateFlow()

    private val _callerFilteringEnabled = MutableStateFlow(false)
    val callerFilteringEnabled: StateFlow<Boolean> = _callerFilteringEnabled.asStateFlow()

    private val _allowedCallersList = MutableStateFlow("Sarah, Mom, Boss, John")
    val allowedCallersList: StateFlow<String> = _allowedCallersList.asStateFlow()

    private val _blockedCallersList = MutableStateFlow("Spam, Telemarketer, Robocall")
    val blockedCallersList: StateFlow<String> = _blockedCallersList.asStateFlow()

    private val _assistantName = MutableStateFlow("Kanna")
    val assistantName: StateFlow<String> = _assistantName.asStateFlow()

    private val _selectedVoiceProfile = MutableStateFlow("Kanna Classic")
    val selectedVoiceProfile: StateFlow<String> = _selectedVoiceProfile.asStateFlow()

    private var textToSpeech: android.speech.tts.TextToSpeech? = null

    private fun initTTS(context: Context) {
        try {
            textToSpeech = android.speech.tts.TextToSpeech(context.applicationContext) { status ->
                if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                    textToSpeech?.language = java.util.Locale.US
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("AuraVM", "Failed to initialize TextToSpeech", e)
        }
    }

    fun speakText(text: String) {
        try {
            textToSpeech?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "aura_screening_tts_id")
        } catch (e: Exception) {
            android.util.Log.e("AuraVM", "TTS Speak error: ${e.localizedMessage}")
        }
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {
            // ignore
        }
    }

    private val _excludedPackages = MutableStateFlow<Set<String>>(emptySet())
    val excludedPackages: StateFlow<Set<String>> = _excludedPackages.asStateFlow()

    private val _customWakeWord = MutableStateFlow("anytime kanna")
    val customWakeWord: StateFlow<String> = _customWakeWord.asStateFlow()

    private val _customApiKey = MutableStateFlow("")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _customEndpoint = MutableStateFlow("https://generativelanguage.googleapis.com/")
    val customEndpoint: StateFlow<String> = _customEndpoint.asStateFlow()

    private val _customModelOverride = MutableStateFlow("")
    val customModelOverride: StateFlow<String> = _customModelOverride.asStateFlow()

    private val _customTimeoutSeconds = MutableStateFlow(60)
    val customTimeoutSeconds: StateFlow<Int> = _customTimeoutSeconds.asStateFlow()

    private val _backoffStrategy = MutableStateFlow("Aggressive")
    val backoffStrategy: StateFlow<String> = _backoffStrategy.asStateFlow()

    private val _isWakeWordEnabled = MutableStateFlow(false)
    val isWakeWordEnabled: StateFlow<Boolean> = _isWakeWordEnabled.asStateFlow()

    private val _wakeStatus = MutableStateFlow("Wake word detection disabled")
    val wakeStatus: StateFlow<String> = _wakeStatus.asStateFlow()

    private val _autoModelUpdatesEnabled = MutableStateFlow(true)
    val autoModelUpdatesEnabled: StateFlow<Boolean> = _autoModelUpdatesEnabled.asStateFlow()

    private val _privacyFirstMode = MutableStateFlow(false)
    val privacyFirstMode: StateFlow<Boolean> = _privacyFirstMode.asStateFlow()

    private val _dndSyncEnabled = MutableStateFlow(false)
    val dndSyncEnabled: StateFlow<Boolean> = _dndSyncEnabled.asStateFlow()

    private val _isSystemDndActive = MutableStateFlow(false)
    val isSystemDndActive: StateFlow<Boolean> = _isSystemDndActive.asStateFlow()

    private val _cryptoPassphrase = MutableStateFlow("")
    val cryptoPassphrase: StateFlow<String> = _cryptoPassphrase.asStateFlow()

    private val _isEncryptionActive = MutableStateFlow(false)
    val isEncryptionActive: StateFlow<Boolean> = _isEncryptionActive.asStateFlow()

    // Passcode lock configurations
    private val _appPasscode = MutableStateFlow("")
    val appPasscode: StateFlow<String> = _appPasscode.asStateFlow()

    private val _isPasscodeLockEnabled = MutableStateFlow(false)
    val isPasscodeLockEnabled: StateFlow<Boolean> = _isPasscodeLockEnabled.asStateFlow()

    private val _isAppUnlocked = MutableStateFlow(true)
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked.asStateFlow()

    // Automatic data cleanup configurations
    private val _autoDeleteDays = MutableStateFlow(0) // 0 means disabled
    val autoDeleteDays: StateFlow<Int> = _autoDeleteDays.asStateFlow()

    // Master switch + per-category opt-outs for AI action history logging.
    private val _aiLoggingEnabled = MutableStateFlow(true)
    val aiLoggingEnabled: StateFlow<Boolean> = _aiLoggingEnabled.asStateFlow()

    private val _aiLoggingCategoryEnabled =
        MutableStateFlow(AiActionCategory.entries.associateWith { true })
    val aiLoggingCategoryEnabled: StateFlow<Map<AiActionCategory, Boolean>> =
        _aiLoggingCategoryEnabled.asStateFlow()

    private val _researchModeEnabled = MutableStateFlow(false)
    val researchModeEnabled: StateFlow<Boolean> = _researchModeEnabled.asStateFlow()

    private val _powerSaverEnabled = MutableStateFlow(false)
    val powerSaverEnabled: StateFlow<Boolean> = _powerSaverEnabled.asStateFlow()

    private val _customVoiceToneGenerated = MutableStateFlow(false)
    val customVoiceToneGenerated: StateFlow<Boolean> = _customVoiceToneGenerated.asStateFlow()

    val notificationFrequency24h: StateFlow<Map<Int, Int>> = notifications
        .map { list ->
            val frequencyMap = mutableMapOf<Int, Int>()
            for (h in 0..23) frequencyMap[h] = 0
            list.forEach { notif ->
                val cal = java.util.Calendar.getInstance()
                cal.timeInMillis = notif.timestamp
                val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                frequencyMap[hour] = (frequencyMap[hour] ?: 0) + 1
            }
            frequencyMap
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    private var pollingJob: kotlinx.coroutines.Job? = null

    fun initPreferencesAndAudio(context: Context) {
        if (appContext != null) return // Already initialized
        appContext = context.applicationContext
        prefs = context.getSharedPreferences("aura_prefs", Context.MODE_PRIVATE)
        
        prefs?.let { p ->
            _isPrivacyModeActive.value = p.getBoolean("privacy_mode", false)
            _quietHoursEnabled.value = p.getBoolean("quiet_hours_enabled", false)
            _quietHoursStart.value = p.getString("quiet_hours_start", "22:00") ?: "22:00"
            _quietHoursEnd.value = p.getString("quiet_hours_end", "07:00") ?: "07:00"
            val listStr = p.getString("excluded_packages_list", "") ?: ""
            _excludedPackages.value = listStr.split(",").filter { it.isNotEmpty() }.toSet()
            _customWakeWord.value = p.getString("custom_wake_word", "anytime kanna") ?: "anytime kanna"
            _isWakeWordEnabled.value = p.getBoolean("wake_word_enabled", false)

            _autoAnswerEnabled.value = p.getBoolean("auto_answer_enabled", true)
            _customGreetingScript.value = p.getString("custom_greeting_script", "Hello, my name is Kanna. I am attending this call instead of Chaitanya since he is currently busy. Please declare your identity and your message, and I will record it.") ?: "Hello, my name is Kanna. I am attending this call instead of Chaitanya since he is currently busy. Please declare your identity and your message, and I will record it."
            _callerFilteringEnabled.value = p.getBoolean("caller_filtering_enabled", false)
            _allowedCallersList.value = p.getString("allowed_callers_list", "Sarah, Mom, Boss, John") ?: "Sarah, Mom, Boss, John"
            _blockedCallersList.value = p.getString("blocked_callers_list", "Spam, Telemarketer, Robocall") ?: "Spam, Telemarketer, Robocall"
            _assistantName.value = p.getString("assistant_name", "Kanna") ?: "Kanna"
            _selectedVoiceProfile.value = p.getString("selected_voice_profile", "Kanna Classic") ?: "Kanna Classic"
            _professionalTone.value = p.getString("professional_tone", "Formal") ?: "Formal"

            _autoModelUpdatesEnabled.value = p.getBoolean("auto_model_updates", true)
            _privacyFirstMode.value = p.getBoolean("privacy_first_mode", false)
            _dndSyncEnabled.value = p.getBoolean("dnd_sync_enabled", false)
            _cryptoPassphrase.value = p.getString("crypto_passphrase", "") ?: ""
            _isEncryptionActive.value = p.getBoolean("is_encryption_active", false)

            _appPasscode.value = p.getString("app_passcode", "") ?: ""
            _isPasscodeLockEnabled.value = p.getBoolean("app_passcode_enabled", false)
            _isAppUnlocked.value = !p.getBoolean("app_passcode_enabled", false)
            _autoDeleteDays.value = p.getInt("auto_delete_interval_days", 0)
            _isOnDeviceProcessingEnabled.value = p.getBoolean("on_device_processing_enabled", true)

            _aiLoggingEnabled.value = p.getBoolean(SharedPrefsAiLoggingPolicy.KEY_MASTER, true)
            _aiLoggingCategoryEnabled.value = AiActionCategory.entries.associateWith { cat ->
                p.getBoolean(SharedPrefsAiLoggingPolicy.categoryKey(cat), true)
            }

            val cleanupDays = p.getInt("auto_delete_interval_days", 0)
            if (cleanupDays > 0) {
                viewModelScope.launch {
                    repository.autoCleanupOldData(cleanupDays)
                    AuraDiagnostics.log("SYSTEM", "INFO", "Automatic cleanup executed. Purged local database caches and transcripts older than $cleanupDays days.")
                }
            }

            val rawApiKey = p.getString("custom_api_key", "") ?: ""
            _customApiKey.value = if (_isEncryptionActive.value && _cryptoPassphrase.value.isNotEmpty()) {
                com.example.data.security.EncryptionHelper.decrypt(rawApiKey, _cryptoPassphrase.value)
            } else {
                rawApiKey
            }
            _customEndpoint.value = p.getString("custom_endpoint", "https://generativelanguage.googleapis.com/") ?: "https://generativelanguage.googleapis.com/"
            _customModelOverride.value = p.getString("custom_model_override", "") ?: ""
            _customTimeoutSeconds.value = p.getInt("custom_api_timeout", 60)
            _backoffStrategy.value = p.getString("custom_backoff_strategy", "Aggressive") ?: "Aggressive"
            
            GeminiKeyManager.customApiKey = _customApiKey.value
            GeminiKeyManager.customEndpoint = _customEndpoint.value
            GeminiKeyManager.customModelOverride = _customModelOverride.value
            GeminiKeyManager.customTimeoutSeconds = _customTimeoutSeconds.value
            GeminiKeyManager.backoffStrategy = _backoffStrategy.value

            // Implement local storage caching for last known connection status on restart
            val cachedStatusName = p.getString("cached_gemini_status", ServiceStatus.UNTESTED.name) ?: ServiceStatus.UNTESTED.name
            val cachedStatus = try { ServiceStatus.valueOf(cachedStatusName) } catch(e: Exception) { ServiceStatus.UNTESTED }
            AuraDiagnostics.setGeminiStatus(cachedStatus)

            // Restore deep work focus configurations
            val wasDeepWorkActive = p.getBoolean("deep_work_active", false)
            val deepWorkUntil = p.getLong("deep_work_until", 0L)
            _deepWorkDurationMinutes.value = p.getInt("deep_work_duration_minutes", 25)
            if (wasDeepWorkActive && System.currentTimeMillis() < deepWorkUntil) {
                _isDeepWorkActive.value = true
                startDeepWorkCountdown(deepWorkUntil - System.currentTimeMillis())
            } else {
                p.edit().putBoolean("deep_work_active", false).apply()
            }
            _researchModeEnabled.value = p.getBoolean("research_mode_enabled", false)
            _powerSaverEnabled.value = p.getBoolean("power_saver_enabled", false)
            _customVoiceToneGenerated.value = p.getBoolean("custom_voice_tone_generated", false)
        }

        // Reactively observe and update connection status cache whenever it changes
        viewModelScope.launch {
            AuraDiagnostics.geminiStatus.collect { status ->
                prefs?.edit()?.putString("cached_gemini_status", status.name)?.apply()
            }
        }

        initTTS(context)

        // 1d. Battery Level dynamic updates broadcast receiver configuration
        try {
            val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            val receiver = object : android.content.BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: android.content.Intent?) {
                    if (intent != null) {
                        val level = intent.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                        val scale = intent.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                        if (level != -1 && scale != -1) {
                            val pct = (level.toFloat() / scale.toFloat() * 100).toInt()
                            _batteryLevel.value = pct
                        }
                    }
                }
            }
            context.applicationContext.registerReceiver(receiver, filter)
        } catch (e: Exception) {
            android.util.Log.e("AuraVM", "Error launching battery helper receiver", e)
        }

        wakeWordWatcher = AuraWakeWordWatcher(
            context = context.applicationContext,
            onWakeWordDetected = {
                // Trigger Kanna voice console hud
                setKannaOverlayVisible(true)
                processVoiceInput("") // launch kanna overlay in active listener mode
            },
            onStatusUpdate = { status ->
                _wakeStatus.value = status
            }
        ).apply {
            setWakeWord(_customWakeWord.value)
        }

        // Auto start if enabled
        if (_isWakeWordEnabled.value) {
            wakeWordWatcher?.startListening()
        }

        // Trigger diagnostic connectivity checks on app launch!
        runDiagnosticConnectivityChecks()

        // Start background polling service for the Network Health widget status updates
        startBackgroundConnectivityPolling()
    }

    fun updateProfessionalTone(tone: String) {
        _professionalTone.value = tone
        prefs?.edit()?.putString("professional_tone", tone)?.apply()
    }

    private fun isBatteryLow(): Boolean {
        return try {
            val ctx = appContext ?: return false
            val ifilter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = ctx.registerReceiver(null, ifilter)
            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    val pct = (level.toFloat() / scale.toFloat() * 100).toInt()
                    return pct < 20
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    fun togglePowerSaver(enabled: Boolean) {
        _powerSaverEnabled.value = enabled
        prefs?.edit()?.putBoolean("power_saver_enabled", enabled)?.apply()
        AuraDiagnostics.log("SYSTEM", "INFO", "AI Power Saver setting updated: ${if (enabled) "Enabled (Background AI throttling under 20% battery active)" else "Disabled"}")
    }

    fun startBackgroundConnectivityPolling() {
        if (_privacyFirstMode.value) return
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            while (true) {
                // Polling interval of 30 seconds by default, throttled to 120 seconds if low battery and power saver enabled
                var delayMs = 30000L
                if (_powerSaverEnabled.value && isBatteryLow()) {
                    delayMs = 120000L
                    AuraDiagnostics.log("POWER_SAVER", "INFO", "Background AI processing frequency throttled to 120s due to low battery.", "Power Saver throttled execution parameters from 30s to 120s.")
                }
                kotlinx.coroutines.delay(delayMs)
                if (_privacyFirstMode.value) break
                
                val apiKey = GeminiKeyManager.getApiKey()
                if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                    AuraDiagnostics.setGeminiStatus(ServiceStatus.MISSING_KEY)
                    continue
                }
                
                if (!isValidGeminiApiKey(apiKey)) {
                    AuraDiagnostics.setGeminiStatus(ServiceStatus.FAILED)
                    continue
                }
                
                try {
                    val testPrompt = "Ping!"
                    val request = com.example.data.network.GenerateContentRequest(
                        contents = listOf(
                            com.example.data.network.Content(
                                role = "user",
                                parts = listOf(com.example.data.network.Part(text = testPrompt))
                            )
                        )
                    )
                    val modelName = if (GeminiKeyManager.customModelOverride.isNotBlank()) {
                        GeminiKeyManager.customModelOverride
                    } else {
                        "gemini-3.5-flash"
                    }
                    val response = com.example.data.network.RetrofitClient.service.generateContent(modelName, apiKey, request)
                    val result = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (result != null) {
                        AuraDiagnostics.setGeminiStatus(ServiceStatus.CONNECTED)
                    } else {
                        AuraDiagnostics.setGeminiStatus(ServiceStatus.FAILED)
                    }
                } catch (e: Exception) {
                    AuraDiagnostics.setGeminiStatus(ServiceStatus.FAILED)
                }
            }
        }
    }

    suspend fun testDiagnosticsHandshakeDirect(): Boolean {
        val apiKey = GeminiKeyManager.getApiKey()
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            AuraDiagnostics.log("GEMINI_API", "WARN", "API Key is missing or default placeholder. Handshake aborted.")
            return false
        }
        
        if (!isValidGeminiApiKey(apiKey)) {
            AuraDiagnostics.log(
                module = "GEMINI_API",
                level = "ERROR",
                message = "Handshake aborted due to invalid API Key format.",
                details = "Key must start with 'AIzaSy', contain only letters/digits/hyphens/underscores, and be 35-45 characters long."
            )
            AuraDiagnostics.setGeminiStatus(ServiceStatus.FAILED)
            return false
        }
        
        AuraDiagnostics.setGeminiStatus(ServiceStatus.TESTING)
        AuraDiagnostics.log("GEMINI_API", "INFO", "Initiating direct health-check ping request...")
        return try {
            val testPrompt = "Ping!"
            val request = com.example.data.network.GenerateContentRequest(
                contents = listOf(
                    com.example.data.network.Content(
                        role = "user",
                        parts = listOf(com.example.data.network.Part(text = testPrompt))
                    )
                )
            )
            val modelName = if (GeminiKeyManager.customModelOverride.isNotBlank()) {
                GeminiKeyManager.customModelOverride
            } else {
                "gemini-3.5-flash"
            }
            val response = com.example.data.network.RetrofitClient.service.generateContent(modelName, apiKey, request)
            val result = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (result != null) {
                AuraDiagnostics.log("GEMINI_API", "INFO", "Direct health check ping succeeded.", "Result: $result")
                AuraDiagnostics.setGeminiStatus(ServiceStatus.CONNECTED)
                true
            } else {
                AuraDiagnostics.log("GEMINI_API", "WARN", "Direct health check completed with empty response payload.")
                AuraDiagnostics.setGeminiStatus(ServiceStatus.FAILED)
                false
            }
        } catch (e: Exception) {
            AuraDiagnostics.log("GEMINI_API", "ERROR", "Direct health-check integration exception: ${e.localizedMessage}")
            AuraDiagnostics.setGeminiStatus(ServiceStatus.FAILED)
            false
        }
    }

    fun runDiagnosticConnectivityChecks() {
        viewModelScope.launch {
            // Local Storage check
            AuraDiagnostics.setLocalStorageStatus(ServiceStatus.TESTING)
            AuraDiagnostics.log("LOCAL_STORAGE", "INFO", "Initiating local Room storage connectivity checks on launch...")
            try {
                // Read model profile to verify DAO access
                val profile = repository.userProfile.first() ?: UserProfileEntity()
                AuraDiagnostics.log("LOCAL_STORAGE", "INFO", "Room Database read query completed successfully.", "Primary signatory: ${profile.userName}")
                AuraDiagnostics.setLocalStorageStatus(ServiceStatus.CONNECTED)
            } catch (e: Exception) {
                AuraDiagnostics.log("LOCAL_STORAGE", "ERROR", "Room Database check failed: ${e.localizedMessage}", e.stackTraceToString())
                AuraDiagnostics.setLocalStorageStatus(ServiceStatus.FAILED)
            }

            // Gemini API connectivity check
            val apiKey = GeminiKeyManager.getApiKey()
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                AuraDiagnostics.log("GEMINI_API", "WARN", "API Key is missing or default placeholder. Skipping active network validation.")
                AuraDiagnostics.setGeminiStatus(ServiceStatus.MISSING_KEY)
                return@launch
            }

            if (!isValidGeminiApiKey(apiKey)) {
                AuraDiagnostics.log(
                    module = "GEMINI_API",
                    level = "ERROR",
                    message = "Format validation failed: custom key does not match Gemini standards.",
                    details = "Key must start with 'AIzaSy', contain only letters/digits/hyphens/underscores, and be 35-45 characters long."
                )
                AuraDiagnostics.setGeminiStatus(ServiceStatus.FAILED)
                return@launch
            }

            AuraDiagnostics.setGeminiStatus(ServiceStatus.TESTING)
            AuraDiagnostics.log("GEMINI_API", "INFO", "Initiating network handshake validation...")
            try {
                // Let's do a lightweight model test call (ping request)
                val testPrompt = "Ping! Respond with 'connected' in under 3 words."
                val request = com.example.data.network.GenerateContentRequest(
                    contents = listOf(
                        com.example.data.network.Content(
                            role = "user",
                            parts = listOf(com.example.data.network.Part(text = testPrompt))
                        )
                    )
                )
                val response = com.example.data.network.RetrofitClient.service.generateContent("gemini-3.5-flash", apiKey, request)
                val result = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (result != null) {
                    AuraDiagnostics.log("GEMINI_API", "INFO", "Geographical handshake verified successfully. Connection is fully authorized.", "Result: $result")
                    AuraDiagnostics.setGeminiStatus(ServiceStatus.CONNECTED)
                } else {
                    AuraDiagnostics.log("GEMINI_API", "WARN", "Network completed successfully but returned empty response payload. Check geoblocking restrictions.")
                    AuraDiagnostics.setGeminiStatus(ServiceStatus.FAILED)
                }
            } catch (e: Exception) {
                AuraDiagnostics.log("GEMINI_API", "ERROR", "Handshake validation failure: ${e.localizedMessage}", e.stackTraceToString())
                if (AuraDiagnostics.geminiStatus.value == ServiceStatus.TESTING) {
                    AuraDiagnostics.setGeminiStatus(ServiceStatus.FAILED)
                }
            }
        }
    }

    fun isValidGeminiApiKey(key: String): Boolean {
        val trimmed = key.trim()
        if (trimmed.isEmpty()) return true
        // Format validation: must start with 'AIzaSy', consist of alphanumeric/hyphen/underscore, and be 35-45 characters long
        return trimmed.startsWith("AIzaSy") && trimmed.length in 35..45 && trimmed.all { it.isLetterOrDigit() || it == '-' || it == '_' }
    }

    fun resetApiSettingsToDefaults() {
        val defaultApiKey = ""
        val defaultEndpoint = "https://generativelanguage.googleapis.com/"
        val defaultModel = ""
        val defaultTimeout = 60
        val defaultBackoff = "Aggressive"

        _customApiKey.value = defaultApiKey
        _customEndpoint.value = defaultEndpoint
        _customModelOverride.value = defaultModel
        _customTimeoutSeconds.value = defaultTimeout
        _backoffStrategy.value = defaultBackoff

        GeminiKeyManager.customApiKey = defaultApiKey
        GeminiKeyManager.customEndpoint = defaultEndpoint
        GeminiKeyManager.customModelOverride = defaultModel
        GeminiKeyManager.customTimeoutSeconds = defaultTimeout
        GeminiKeyManager.backoffStrategy = defaultBackoff

        prefs?.edit()
            ?.putString("custom_api_key", defaultApiKey)
            ?.putString("custom_endpoint", defaultEndpoint)
            ?.putString("custom_model_override", defaultModel)
            ?.putInt("custom_api_timeout", defaultTimeout)
            ?.putString("custom_backoff_strategy", defaultBackoff)
            ?.apply()

        AuraDiagnostics.log("SYSTEM", "INFO", "Reverted API gateway configurations to initial factory defaults.", "Endpoint: $defaultEndpoint, Timeout: ${defaultTimeout}s, Backoff: $defaultBackoff")
        runDiagnosticConnectivityChecks()
    }

    fun updateCustomApiSettings(apiKey: String, endpoint: String, modelOverride: String, timeoutSeconds: Int, backoffStrategy: String) {
        _customApiKey.value = apiKey
        _customEndpoint.value = endpoint
        _customModelOverride.value = modelOverride
        _customTimeoutSeconds.value = timeoutSeconds
        _backoffStrategy.value = backoffStrategy

        GeminiKeyManager.customApiKey = apiKey
        GeminiKeyManager.customEndpoint = endpoint
        GeminiKeyManager.customModelOverride = modelOverride
        GeminiKeyManager.customTimeoutSeconds = timeoutSeconds
        GeminiKeyManager.backoffStrategy = backoffStrategy

        val apiKeyToSave = if (_isEncryptionActive.value && _cryptoPassphrase.value.isNotEmpty()) {
            com.example.data.security.EncryptionHelper.encrypt(apiKey, _cryptoPassphrase.value)
        } else {
            apiKey
        }

        prefs?.edit()
            ?.putString("custom_api_key", apiKeyToSave)
            ?.putString("custom_endpoint", endpoint)
            ?.putString("custom_model_override", modelOverride)
            ?.putInt("custom_api_timeout", timeoutSeconds)
            ?.putString("custom_backoff_strategy", backoffStrategy)
            ?.apply()

        AuraDiagnostics.log("SYSTEM", "INFO", "Authentication parameters updated dynamically inside secure preferences.", "Endpoint override: $endpoint, Timeout: ${timeoutSeconds}s, Backoff: $backoffStrategy")
        runDiagnosticConnectivityChecks()
    }

    fun addTask(task: AuraTaskEntity) {
        viewModelScope.launch {
            repository.insertTask(task)
        }
    }

    fun updateTask(task: AuraTaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun deleteTask(id: Int) {
        viewModelScope.launch {
            repository.deleteTask(id)
        }
    }

    fun togglePrivacyMode(active: Boolean) {
        _isPrivacyModeActive.value = active
        prefs?.edit()?.putBoolean("privacy_mode", active)?.apply()
        
        // Log to chat messages on privacy activation/deactivation
        viewModelScope.launch {
            val statusStr = if (active) "ACTIVATED. Interception and AI parsing paused." else "DEACTIVATED. Interception and AI parsing resumed."
            val chatMsg = ChatMessageEntity(
                sender = "AI",
                text = "Privacy Shield update: Secure Privacy Mode has been $statusStr"
            )
            repository.insertChatMessage(chatMsg)
        }
    }

    fun toggleExcludedPackage(packageName: String) {
        val current = _excludedPackages.value.toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        _excludedPackages.value = current
        prefs?.edit()?.putString("excluded_packages_list", current.joinToString(","))?.apply()
    }

    fun updateCustomWakeWord(word: String) {
        if (word.isBlank()) return
        val sanitized = word.trim().lowercase()
        _customWakeWord.value = sanitized
        prefs?.edit()?.putString("custom_wake_word", sanitized)?.apply()
        wakeWordWatcher?.setWakeWord(sanitized)
    }

    fun toggleWakeWordEnabled(enabled: Boolean) {
        _isWakeWordEnabled.value = enabled
        prefs?.edit()?.putBoolean("wake_word_enabled", enabled)?.apply()
        if (enabled) {
            wakeWordWatcher?.startListening()
        } else {
            wakeWordWatcher?.stopListening()
        }
    }

    fun setKannaOverlayVisible(visible: Boolean) {
        _isKannaOverlayVisible.value = visible
    }

    fun toggleHighThinking(enabled: Boolean) {
        _highThinkingEnabled.value = enabled
    }

    // --- Chat Interface Actions ---

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        
        viewModelScope.launch {
            val userMsg = ChatMessageEntity(sender = "USER", text = text)
            repository.insertChatMessage(userMsg)
            
            _isChatPending.value = true

            val currentHistory = chatMessages.value
            val responseText = repository.sendChatMessage(
                userText = text,
                highThinking = _highThinkingEnabled.value,
                messageHistory = currentHistory
            )

            val aiMsg = ChatMessageEntity(sender = "AI", text = responseText)
            repository.insertChatMessage(aiMsg)
            
            _isChatPending.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChatHistory()
            val welcomeMsg = ChatMessageEntity(
                sender = "AI",
                text = "Secure Connection established. I am Aura. I can process your notifications, read logs, write safe emails, or answer search queries securely with absolute physical authorization."
            )
            repository.insertChatMessage(welcomeMsg)
        }
    }

    // Initialize welcoming or fallback messages
    init {
        // Initialize with default installation record if none exists
        viewModelScope.launch {
            try {
                val list = repository.versionInstallations.first()
                if (list.isEmpty()) {
                    repository.insertVersionInstallation(
                        com.example.data.db.VersionInstallationEntity(
                            version = "2.4.1",
                            timestamp = System.currentTimeMillis() - 86400000L * 3
                        )
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("AuraViewModel", "Error checking/initializing version installations", e)
            }
        }

        // Background release monitoring thread periodically checking for new software releases
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(12000) // check every 12 seconds in background
                if (!_isUpdateAvailable.value && _availableVersion.value == "2.4.1") {
                    _availableVersion.value = "2.5.0"
                    _isUpdateAvailable.value = true
                    _updateStatusMessage.value = "New release v2.5.0 (Latest Security Patch) is ready to be applied."
                    AuraDiagnostics.log("SYSTEM", "INFO", "Periodic Update Service: New software v2.5.0 ready. State flag: Update Available.")
                }
            }
        }

        // Observe notifications flow to auto-prepare suggestions for the latest incoming item
        viewModelScope.launch {
            var lastId = -1
            notifications.collect { list ->
                val latest = list.firstOrNull()
                if (latest != null && latest.id != lastId) {
                    lastId = latest.id
                    loadSuggestionsForNotification(latest)
                } else if (latest == null) {
                    _currentSuggestions.value = emptyList()
                    lastId = -1
                }
            }
        }

        // Periodic background processing task that aggregates multiple notification summaries over a 30s set period
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(30000)
                if (_privacyFirstMode.value) continue
                if (!isPrivacyModeEffectivelyActive.value) {
                    val activeNotifs = notifications.value.filter { it.status != "DEALT" }
                    if (activeNotifs.isNotEmpty()) {
                        val result = repository.generatePrioritizedDigest(activeNotifs, _highThinkingEnabled.value)
                        _prioritizedDigest.value = result
                    }
                }
            }
        }

        viewModelScope.launch {
            repository.chatMessages.first().let { current ->
                if (current.isEmpty()) {
                    val welcomeMsg = ChatMessageEntity(
                        sender = "AI",
                        text = "I am Aura Core. Operating with maximum local encryption.\n\nUse the tabs below to monitor intercepted notifications or summarize incoming mail streams completely on-device."
                    )
                    repository.insertChatMessage(welcomeMsg)
                }
            }
            // Seed profile and dummy emails/notifications if empty, to ensure awesome UX on first launch!
            repository.userProfile.first().let { current ->
                if (current == null) {
                    repository.saveUserProfile(UserProfileEntity())
                }
            }
            repository.emails.first().let { current ->
                if (current.isEmpty()) {
                    seedDummyEmails()
                }
            }
            repository.notifications.first().let { current ->
                if (current.isEmpty()) {
                    seedDummyNotifications()
                }
            }
            repository.secureFiles.first().let { current ->
                if (current.isEmpty()) {
                    seedDummyFiles()
                }
            }
            repository.socialPosts.first().let { current ->
                if (current.isEmpty()) {
                    seedDummySocialPosts()
                }
            }
            repository.calendarEvents.first().let { current ->
                if (current.isEmpty()) {
                    seedDummyCalendarEvents()
                }
            }
            repository.privacyInsights.first().let { current ->
                if (current.isEmpty()) {
                    seedDummyPrivacyInsights()
                }
            }
            repository.callScreeningRules.first().let { current ->
                if (current.isEmpty()) {
                    seedDummyCallScreeningRules()
                }
            }
            repository.emailTemplates.first().let { current ->
                if (current.isEmpty()) {
                    seedDummyEmailTemplates()
                }
            }
        }
    }

    private fun seedDummyCallScreeningRules() {
        addCallScreeningRule("+1-555-*", "AUTO_ANSWER", "Auto-answer incoming firm leads and office exchange extensions.")
        addCallScreeningRule("*Spam*", "BLOCK", "Drop all cold callers or matching keyword logs.")
        addCallScreeningRule("+1-800-*", "BLOCK", "Mute corporate sales calls.")
    }

    private fun seedDummyEmailTemplates() {
        addEmailTemplate("Formal Resignation Reply", "Thank you, I will prepare the handover metrics and follow up with HR.", "work")
        addEmailTemplate("Busy Auto-Response", "I am currently in focus mode but have registered your message. I'll connect by 5:00 PM.", "urgent")
        addEmailTemplate("Personal Catchup", "That sounds perfect! Let's arrange a time to chat this weekend.", "personal")
        addEmailTemplate("Out of Office Vacation Response", "Thank you for getting in touch. I am currently Out of Office / on Vacation with limited access to modern email channels. I will reply properly to your message upon my return.", "personal")
    }

    private fun seedDummyPrivacyInsights() {
        addPrivacyInsight("Secure Guard", "Initialized on-device cryptographic environment with customized AES key-derivation protocol.")
        addPrivacyInsight("Email Agent", "Audited and intercepted corporate scaling permissions query from engineering-leads@firmwide.com.")
        addPrivacyInsight("Notification Interceptor", "Filtered message notification from 'Sarah Jenkins' regarding urgent Aura product alignment sync.")
    }

    // --- Profile Settings ---

    fun saveProfile(
        name: String,
        email: String,
        replyOption: Boolean,
        security: String,
        wakeWord: String,
        lockscreenEnabled: Boolean
    ) {
        viewModelScope.launch {
            val existing = repository.userProfile.first() ?: UserProfileEntity()
            val updated = existing.copy(
                userName = name,
                userEmail = email,
                autoReplyEnabled = replyOption,
                securityLevel = security,
                wakeWord = wakeWord,
                lockscreenActivationEnabled = lockscreenEnabled
            )
            repository.saveUserProfile(updated)
        }
    }

    // --- Notifications actions ---

    fun analyzeNotification(notification: NotificationEntity) {
        viewModelScope.launch {
            _processingNotificationId.value = notification.id
            val now = System.currentTimeMillis()
            val isInMeeting = calendarEvents.value.any { event ->
                now >= event.startTime && now <= event.endTime
            }
            val result = repository.processNotificationDetails(
                packageName = notification.packageName,
                title = notification.title,
                bodyText = notification.text,
                highThinking = _highThinkingEnabled.value,
                tone = _professionalTone.value,
                isCurrentInMeeting = isInMeeting
            )
            val updatedNotification = notification.copy(
                summary = result.summary,
                replyDraft = result.replyDraft,
                urgency = result.urgency,
                status = "SUMMARIZED"
            )
            repository.updateNotification(updatedNotification)
            _processingNotificationId.value = null
            addPrivacyInsight(
                "Notification Interceptor",
                "Processed notification from '${notification.title}' (${notification.packageName}) to determine priority, formulate reply drafts, and assess urgency."
            )
        }
    }

    fun sendNotificationReply(notification: NotificationEntity, customText: String?) {
        viewModelScope.launch {
            val replyToUse = customText ?: notification.replyDraft.ifBlank { "Acknowledged! (Sent with Aura)" }
            val updated = notification.copy(
                status = "DEALT",
                replyDraft = replyToUse
            )
            repository.updateNotification(updated)
            
            // Add to recent replies (retaining up to last 3)
            val newItem = RecentReplyItem(
                notificationId = notification.id,
                senderName = notification.title,
                replyText = replyToUse,
                originalStatus = notification.status
            )
            val updatedList = _recentReplies.value.toMutableList()
            updatedList.add(0, newItem)
            _recentReplies.value = updatedList.take(3)
            
            // Log interaction inside general assistant chat
            val interactionMsg = ChatMessageEntity(
                sender = "AI",
                text = "Authorized Action: Formulated and executed quick reply to notification from ${notification.title} via ${notification.packageName}:\n\n\"$replyToUse\""
            )
            repository.insertChatMessage(interactionMsg)
        }
    }

    fun loadSuggestionsForNotification(notification: NotificationEntity) {
        viewModelScope.launch {
            if (_isPrivacyModeActive.value) {
                _currentSuggestions.value = emptyList()
                return@launch
            }
            _isSuggestionsLoading.value = true
            val parsedResult = repository.generateQuickSuggestions(
                title = notification.title,
                text = notification.text,
                highThinking = _highThinkingEnabled.value
            )
            _currentSuggestions.value = parsedResult
            _isSuggestionsLoading.value = false
            addPrivacyInsight(
                "Notification Interceptor Proxy",
                "Analyzed data block for app: '${notification.packageName}' (Title: ${notification.title}). Created quick reply suggestions."
            )
        }
    }

    fun generateNotificationDigest() {
        viewModelScope.launch {
            if (_isPrivacyModeActive.value) {
                _prioritizedDigest.value = "⚠️ Privacy Shield Active: AI notification interception and digestion is paused."
                return@launch
            }
            _isDigestLoading.value = true
            val list = notifications.value.filter { it.status != "DEALT" }
            val result = repository.generatePrioritizedDigest(list, _highThinkingEnabled.value)
            _prioritizedDigest.value = result
            _isDigestLoading.value = false
            addPrivacyInsight(
                "Global Notification Digest",
                "Processed feed aggregation of ${list.size} notification payloads. Compiled security overview report (${String.format("%.2f", result.length / 1024.0)} KB)."
            )
        }
    }

    fun undoRecentReply(item: RecentReplyItem) {
        viewModelScope.launch {
            val databaseList = notifications.value
            val match = databaseList.find { it.id == item.notificationId }
            if (match != null) {
                val reverted = match.copy(
                    status = item.originalStatus,
                    replyDraft = ""
                )
                repository.updateNotification(reverted)
                
                // Remove from recent replies list
                _recentReplies.value = _recentReplies.value.filter { it.notificationId != item.notificationId }
                
                // Insert undo message log
                repository.insertChatMessage(
                    ChatMessageEntity(
                        sender = "AI",
                        text = "Recalled auto-reply: Formally reverted status of notification from ${item.senderName}."
                    )
                )
                _voiceOutputText.value = "Successfully recalled auto-reply to ${item.senderName}!"
            } else {
                _voiceOutputText.value = "Failed: Could not locate original notification to recall."
            }
        }
    }

    fun deleteNotificationItem(id: Int) {
        viewModelScope.launch {
            repository.deleteNotification(id)
        }
    }

    fun simulateNotificationDrop(appName: String, title: String, text: String) {
        viewModelScope.launch {
            val lowerText = "$title $text".lowercase()
            val computedUrgency = when {
                lowerText.contains("urgent") || lowerText.contains("immediate") || lowerText.contains("asap") ||
                lowerText.contains("critical") || lowerText.contains("emergency") || lowerText.contains("billing") ||
                lowerText.contains("scale") || lowerText.contains("alert") || lowerText.contains("verification") || 
                lowerText.contains("action required") -> "URGENT"
                
                lowerText.contains("promo") || lowerText.contains("newsletter") || lowerText.contains("digest") ||
                lowerText.contains("discount") || lowerText.contains("subscribe") || lowerText.contains("marketing") ||
                lowerText.contains("suggested") || lowerText.contains("vibe") || lowerText.contains("platform") -> "LOW"
                
                else -> "NORMAL"
            }

            if (_isDeepWorkActive.value) {
                if (computedUrgency != "URGENT") {
                    AuraDiagnostics.log("DEEP WORK", "SILENCED", "Silenced incoming notification from $appName: $title")
                    return@launch
                }
            }

            val packageName = when (appName.uppercase()) {
                "WHATSAPP" -> "com.whatsapp"
                "INSTAGRAM" -> "com.instagram.android"
                "TWITTER" -> "com.twitter.android"
                "LINKEDIN" -> "com.linkedin.android"
                else -> "com.google.android.gm"
            }
            val item = NotificationEntity(
                packageName = packageName,
                title = title,
                text = text,
                urgency = computedUrgency
            )
            repository.insertNotification(item)

            saveScreenedTranscript("NOTIFICATION", appName, text, "Notification Intercepted from $title: [Urgency $computedUrgency]")
        }
    }

    private fun seedDummyNotifications() {
        viewModelScope.launch {
            repository.insertNotification(
                NotificationEntity(
                    packageName = "com.whatsapp",
                    title = "Sarah Jenkins",
                    text = "Hey! Are we still meeting for the Aura product alignment sync today at 4:00 PM? Please let me know soon."
                )
            )
            repository.insertNotification(
                NotificationEntity(
                    packageName = "com.linkedin.android",
                    title = "LinkedIn Recruiter",
                    text = "Your resume was top-ranked for the Principal Systems Architect opening. Let's Hop on a Call."
                )
            )
        }
    }

    // --- Email actions ---

    fun analyzeEmail(email: EmailEntity) {
        viewModelScope.launch {
            _processingEmailId.value = email.id
            val result = repository.processEmailDetails(
                sender = email.sender,
                subject = email.subject,
                body = email.body,
                highThinking = _highThinkingEnabled.value,
                tone = _professionalTone.value,
                researchMode = _researchModeEnabled.value
            )
            val updatedEmail = email.copy(
                summary = result.first,
                replyDraft = result.second,
                status = "SUMMARIZED"
            )
            repository.updateEmail(updatedEmail)
            _processingEmailId.value = null
            addPrivacyInsight(
                "Email Agent",
                "Processed email from '${email.sender}' regarding '${email.subject}' to generate concise insights, summaries, and response templates."
            )
        }
    }

    fun dispatchEmailResponse(email: EmailEntity, finalDraft: String) {
        viewModelScope.launch {
            val updated = email.copy(
                status = "REPLIED",
                replyDraft = finalDraft
            )
            repository.updateEmail(updated)

            // Add authorization trace in system console feed
            val logMessage = ChatMessageEntity(
                sender = "AI",
                text = "Secure Despatch Approved: Reply dispatched to ${email.sender} regarding '${email.subject}':\n\n$finalDraft"
            )
            repository.insertChatMessage(logMessage)
        }
    }

    fun deleteEmailItem(id: Int) {
        viewModelScope.launch {
            repository.deleteEmail(id)
        }
    }

    fun simulateEmailIncoming(sender: String, subject: String, body: String) {
        viewModelScope.launch {
            val email = EmailEntity(
                sender = sender,
                subject = subject,
                body = body
            )
            repository.insertEmail(email)
            
            // Check Out of Office (OOO) status
            val now = System.currentTimeMillis()
            val events = calendarEvents.value
            val isOooNow = events.any { event ->
                val titleUpper = event.title.uppercase()
                val summaryUpper = event.summary.uppercase()
                val isMatching = titleUpper.contains("OUT OF OFFICE") || titleUpper.contains("OOO") || titleUpper.contains("VACATION") ||
                                  summaryUpper.contains("OUT OF OFFICE") || summaryUpper.contains("OOO") || summaryUpper.contains("VACATION")
                val isCurrent = now >= event.startTime && now <= event.endTime
                isMatching && isCurrent
            }
            
            if (isOooNow) {
                val templates = emailTemplates.value
                val personalTemplate = templates.find { it.category.lowercase() == "personal" }
                val replyContent = personalTemplate?.content ?: "Thank you for reaching out. I am currently out of office on vacation. I will respond to your email as soon as possible upon my return."
                
                val refreshedEmails = repository.emails.firstOrNull() ?: emptyList()
                val insertedEmail = refreshedEmails.find { it.sender == sender && it.subject == subject }
                if (insertedEmail != null) {
                    dispatchEmailResponse(insertedEmail, replyContent)
                    AuraDiagnostics.log("AUTOMATION", "INFO", "Out-of-office auto-reply dispatched to $sender.", "Active Out of Office / Vacation event detected. Using Personal Category template: ${personalTemplate?.name ?: "Default"}")
                }
            }
        }
    }

    private fun seedDummyEmails() {
        viewModelScope.launch {
            repository.insertEmail(
                EmailEntity(
                    sender = "engineering-leads@firmwide.com",
                    subject = "URGENT: Cloud Compute Resource Scaling Permissions",
                    body = "We noticed high ingress resource limits during the last deployment audit. Can you verify with Aura permissions if we should auto-scale or limit parameters at midnight?"
                )
            )
            repository.insertEmail(
                EmailEntity(
                    sender = "billing-alerts@google.cloud.com",
                    subject = "GCP Project Invoice Completed for May 2026",
                    body = "A secure transaction was initialized and processed successfully for Project bbf3a... Total charge incurred was USD 0.00 under research sandbox tier."
                )
            )
        }
    }

    // --- Secured Mobile Drive (Google Drive Mimic) Operations ---

    fun createSecureFile(name: String, content: String) {
        viewModelScope.launch {
            val size = "${(content.length * 2) + 120} B"
            repository.insertSecureFile(
                SecureFileEntity(
                    name = name,
                    sizeStr = size,
                    content = content
                )
            )
        }
    }

    fun deleteFileItem(id: Int) {
        viewModelScope.launch {
            repository.deleteSecureFile(id)
        }
    }

    private fun seedDummyFiles() {
        viewModelScope.launch {
            repository.insertSecureFile(
                SecureFileEntity(
                    name = "Quarterly_Growth_Report.pdf",
                    sizeStr = "2.4 MB",
                    content = "Aura secure business analytical review data for Q2 2026. Projected ingress limits showing 45% increase due to privacy-centric mobile agent demand."
                )
            )
            repository.insertSecureFile(
                SecureFileEntity(
                    name = "Confidential_Resume_Optimized_Sovereign.docx",
                    sizeStr = "412 KB",
                    content = "Chaitanya - Principal Security Architect / AI Specialist. Fully ATS-scanned, optimized, zero-cloud profile."
                )
            )
        }
    }

    // --- Social Posting Hub Operations ---

    private val _processingSocialPostId = MutableStateFlow<Int?>(null)
    val processingSocialPostId: StateFlow<Int?> = _processingSocialPostId.asStateFlow()

    fun analyzeSocialMediaAction(post: SocialPostEntity) {
        viewModelScope.launch {
            _processingSocialPostId.value = post.id
            val result = repository.processSocialPostDetails(
                platform = post.platform,
                senderOrTitle = post.title,
                text = post.text,
                highThinking = _highThinkingEnabled.value,
                tone = _professionalTone.value
            )
            val updated = post.copy(
                replyDraft = result.second,
                status = "DRAFTED"
            )
            repository.updateSocialPost(updated)
            _processingSocialPostId.value = null
            addPrivacyInsight(
                "Social Posting Hub",
                "Processed post action for platform '${post.platform}' titled '${post.title}' to draft context-appropriate social communications."
            )
        }
    }

    fun publishSocialPost(post: SocialPostEntity, finalContent: String) {
        viewModelScope.launch {
            val updated = post.copy(
                replyDraft = finalContent,
                status = "PUBLISHED"
            )
            repository.updateSocialPost(updated)

            // Log action in Main chatbot screen for synergistic feedback
            repository.insertChatMessage(
                ChatMessageEntity(
                    sender = "AI",
                    text = "Aura Secure Dispatcher: Posted live on ${post.platform} regarding \"${post.title}\":\n\n$finalContent"
                )
            )
        }
    }

    fun createSocialPostSimulation(platform: String, title: String, text: String) {
        viewModelScope.launch {
            repository.insertSocialPost(
                SocialPostEntity(
                    platform = platform,
                    title = title,
                    text = text,
                    replyDraft = "",
                    status = "DRAFT"
                )
            )
        }
    }

    fun deleteSocialPostItem(id: Int) {
        viewModelScope.launch {
            repository.deleteSocialPost(id)
        }
    }

    private fun seedDummySocialPosts() {
        viewModelScope.launch {
            repository.insertSocialPost(
                SocialPostEntity(
                    platform = "LINKEDIN",
                    title = "Senior Recruiter (Systems Corp)",
                    text = "We're expanding our AI Security Division to scale secure localized models. Know any top Principal Architects?",
                    replyDraft = "",
                    status = "DRAFT"
                )
            )
            repository.insertSocialPost(
                SocialPostEntity(
                    platform = "INSTAGRAM",
                    title = "Aura AI Sovereign",
                    text = "Our new design theme Cosmic Slate is officially open source and offline-first!",
                    replyDraft = "",
                    status = "DRAFT"
                )
            )
        }
    }

    private fun seedDummyCalendarEvents() {
        viewModelScope.launch {
            repository.insertCalendarEvent(
                CalendarEventEntity(
                    title = "🔒 Q2 Engineering Milestones & Security Architecture",
                    organizer = "Keith Vance (Product Director)",
                    startTime = System.currentTimeMillis() + 60000 * 5, // 5 min from now
                    endTime = System.currentTimeMillis() + 60000 * 35,
                    status = "PENDING",
                    transcript = "",
                    summary = ""
                )
            )
            repository.insertCalendarEvent(
                CalendarEventEntity(
                    title = "⚡ Kanna Scaling & Edge AI Integration",
                    organizer = "Sovereign Eng Team Node-7",
                    startTime = System.currentTimeMillis() + 60000 * 60, // 1 hr from now
                    endTime = System.currentTimeMillis() + 60000 * 90,
                    status = "PENDING",
                    transcript = "",
                    summary = ""
                )
            )
            repository.insertCalendarEvent(
                CalendarEventEntity(
                    title = "📂 Offline-First Compliance & Local Policy Sync",
                    organizer = "Brenda Shields (Compliance)",
                    startTime = System.currentTimeMillis() + 60000 * 180, // 3 hr from now
                    endTime = System.currentTimeMillis() + 60000 * 210,
                    status = "PENDING",
                    transcript = "",
                    summary = ""
                )
            )
        }
    }

    fun addNewCalendarEvent(title: String, organizer: String) {
        viewModelScope.launch {
            repository.insertCalendarEvent(
                CalendarEventEntity(
                    title = title,
                    organizer = organizer,
                    startTime = System.currentTimeMillis() + 60000 * 10,
                    endTime = System.currentTimeMillis() + 60000 * 40,
                    status = "PENDING"
                )
            )
        }
    }

    fun deleteCalendarEvent(id: Int) {
        viewModelScope.launch {
            repository.deleteCalendarEvent(id)
        }
    }

    fun updateCalendarEvent(event: CalendarEventEntity) {
        viewModelScope.launch {
            repository.updateCalendarEvent(event)
        }
    }

    fun simulateCalendarEventJoinSequence(eventId: Int) {
        viewModelScope.launch {
            val list = calendarEvents.value
            val event = list.find { it.id == eventId } ?: return@launch
            
            _joiningEventId.value = eventId
            _joiningEventTitle.value = event.title
            _isRecordingActive.value = true
            
            // Phase 1: JOINING (Dialing and setting security nodes)
            repository.updateCalendarEvent(
                event.copy(
                    status = "JOINING",
                    transcript = "📡 Initializing local representation secure tunnel...\n🎙️ Calibrating decibel level meters..."
                )
            )
            
            kotlinx.coroutines.delay(4000)
            
            // Phase 2: RECORDING (Live Transcription Stream Simulation)
            val streamRecord = """
[00:01] CHAIRPERSON (Keith Vance): Thanks for tuning in to ${event.title}. Let's discuss performance and security goals.
[00:15] ARCHITECT (Brenda): We must ensure that our on-device security processes run completely locally.
[00:32] AI REPRESENTATIVE (Kanna): Correct. Chaitanya's local node is now active and monitoring. I have established a physical encrypted bridge.
[00:48] CHAIRPERSON: Perfect. Let's record these deployment milestones: Phase 1 starts on Tuesday, and full compliance alignment should be finalized by Q3.
            """.trimIndent()
            
            repository.updateCalendarEvent(
                event.copy(
                    status = "RECORDING",
                    transcript = streamRecord
                )
            )
            
            kotlinx.coroutines.delay(5000)
            
            // Phase 3: COMPLETED. Use Gemini or Fallback to create summary, and append to call history
            val summaryPrompt = "Given this professional transcript of a team meeting, write a clear, concise, bulleted summary focusing on alignment plans and decisions:\n\n$streamRecord"
            val sessionSummary = repository.generateMeetingSummaryWithAI(summaryPrompt)
            
            val finishedEvent = event.copy(
                status = "COMPLETED",
                transcript = streamRecord,
                summary = sessionSummary
            )
            repository.updateCalendarEvent(finishedEvent)
            
            // --- AUTOMATED ACTION ITEM EXTRACTION ENGINE ---
            val taskPrompt = """
                Analyze this meeting transcript or summary and extract concrete action items or tasks. 
                Assign each task to the corresponding person mentioned in the transcript. If there is no specific person, assign it to "Chaitanya".
                Format each task exactly in a single line like:
                [TASK] Action Item Title | Assignee
                
                Transcript:
                $streamRecord
                
                Summary:
                $sessionSummary
            """.trimIndent()
            
            val taskResponse = try {
                repository.generateMeetingSummaryWithAI(taskPrompt)
            } catch (e: Exception) {
                "" 
            }
            
            val tasksList = mutableListOf<AuraTaskEntity>()
            taskResponse.lines().forEach { line ->
                if (line.contains("[TASK]")) {
                    val cleanLine = line.substringAfter("[TASK]").trim()
                    val parts = cleanLine.split("|")
                    if (parts.isNotEmpty()) {
                        val taskTitle = parts[0].trim()
                        val assigneeName = if (parts.size > 1) parts[1].trim() else "Chaitanya"
                        if (taskTitle.isNotBlank()) {
                            tasksList.add(
                                AuraTaskEntity(
                                    title = taskTitle,
                                    assignee = assigneeName,
                                    status = "PENDING",
                                    sourceMeeting = event.title
                                )
                            )
                        }
                    }
                }
            }
            
            if (tasksList.isEmpty()) {
                tasksList.add(
                    AuraTaskEntity(
                        title = "Verify local encryption on-device processes completely locally",
                        assignee = "Brenda",
                        status = "PENDING",
                        sourceMeeting = event.title
                    )
                )
                tasksList.add(
                    AuraTaskEntity(
                        title = "Establish physics encrypted bridge and activate node monitoring",
                        assignee = "Chaitanya",
                        status = "PENDING",
                        sourceMeeting = event.title
                    )
                )
                tasksList.add(
                    AuraTaskEntity(
                        title = "Finalize Q3 deployment compliance guidelines and milestones",
                        assignee = "Keith Vance",
                        status = "PENDING",
                        sourceMeeting = event.title
                    )
                )
            }
            
            tasksList.forEach { task ->
                repository.insertTask(task)
            }
            
            // Mirror inside telephony call logs too for accessibility
            repository.insertCallSession(
                CallSessionEntity(
                    callerName = event.title,
                    transcript = streamRecord,
                    summary = sessionSummary,
                    voiceProfileUsed = "Kanna Proxy Voice"
                )
            )
            
            // Clear recording status
            _isRecordingActive.value = false
            _joiningEventId.value = null
            _joiningEventTitle.value = ""

            // Automatically compile post-meeting summary parsing screened notifications
            generatePostMeetingSummary(eventId)
        }
    }

    fun triggerWebScrapeTask() {
        if (_isScrapingInProgress.value) return
        _isScrapingInProgress.value = true
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000) // Simulate advanced web crawler tracking LinkIn trends
            val trendHeadlines = listOf(
                "Is localized deep concentration becoming a luxury?",
                "Stop scheduling meetings that interrupt development compilation loops!",
                "Scale and protect: Local LLMs on Android phones are the future of corporate secrecy."
            )
            val trendWriters = listOf(
                "Sarah Vance (VP Talent @ SiliconAlpha)",
                "David S. (Compiler Engineering Director)",
                "Kanna Developer Node-9"
            )
            val idx = (0..2).random()
            
            // Ask Gemini (if API Key exists) to build a suitable response based on the trending post,
            // or use high-quality simulated offline defaults that fit beautifully!
            val trendTitle = trendWriters[idx]
            val trendText = trendHeadlines[idx]
            val suggestedComment = when(idx) {
                0 -> "Insightful share, Sarah! Preserving cognitive focus is exactly why we delegate meeting dial-ins to autonomous security agents (like Kanna). Peak productivity demands localized automation!"
                1 -> "Absolutely spot-on! Interruptions are the enemy. Automating representative presence or screening schedules saves hours of deep-work compile loops."
                else -> "Perfect synthesis! On-device security guarantees total data privacy without reliance on third-party API exposure. Local representation is the sovereign choice."
            }
            
            repository.insertSocialPost(
                SocialPostEntity(
                    platform = "LINKEDIN",
                    title = trendTitle,
                    text = trendText,
                    replyDraft = suggestedComment,
                    status = "PENDING_APPROVAL"
                )
            )
            _isScrapingInProgress.value = false
        }
    }

    fun approveScrapedComment(postId: Int) {
        viewModelScope.launch {
            val list = socialPosts.value
            val item = list.find { it.id == postId }
            if (item != null) {
                repository.updateSocialPost(item.copy(status = "PUBLISHED"))
            }
        }
    }

    fun rejectScrapedComment(postId: Int) {
        viewModelScope.launch {
            repository.deleteSocialPost(postId)
        }
    }

    fun generateLinkedInPostWithGraphic(topic: String) {
        _isSocialProcessing.value = true
        _linkedinVisualImageReady.value = false
        viewModelScope.launch {
            // Generates post text + hashtags + mentions
            val craftedContent = repository.generateLinkedInPostCraft(topic, _highThinkingEnabled.value, _professionalTone.value)
            _linkedinDraftPost.value = craftedContent
            
            // Create a matching professional poster illustration description
            _generatedLinkedInVisualPrompt.value = "Sleek professional modern slide layout for: '$topic'. Cosmic slate dark dashboard, glowing indigo highlights, neat geometric graphics, professional vector style."
            
            // Simulate the graphic generation response delay
            kotlinx.coroutines.delay(4000)
            _linkedinVisualImageReady.value = true
            _isSocialProcessing.value = false
        }
    }

    // --- Voice Assistant Command Processing ---

    fun processVoiceInput(voiceText: String) {
        _isKannaOverlayVisible.value = true
        _isVoiceActive.value = true
        
        if (voiceText.isBlank()) {
            _voiceOutputText.value = "Aura Active & Listening..."
            return
        }
        
        viewModelScope.launch {
            _voiceOutputText.value = "Aura listening and deciphering secure command..."
            val lowerMsg = voiceText.lowercase()
 
            val isActionDone = if (lowerMsg.contains("kanna, draft a post about") || lowerMsg.contains("kanna draft a post about") || lowerMsg.contains("draft a post about")) {
                val prefix = if (lowerMsg.contains("kanna, draft a post about")) {
                    "kanna, draft a post about"
                } else if (lowerMsg.contains("kanna draft a post about")) {
                    "kanna draft a post about"
                } else {
                    "draft a post about"
                }
                val index = lowerMsg.indexOf(prefix)
                val topic = voiceText.substring(index + prefix.length).trim().removeSuffix(".").trim()
                if (topic.isNotBlank()) {
                    _voiceOutputText.value = "Kanna voice command active! Generating campaign post structure and poster layout blueprint for: '$topic' with ${_professionalTone.value} tone..."
                    generateLinkedInPostWithGraphic(topic)
                    repository.insertChatMessage(ChatMessageEntity(sender = "USER", text = "🎤 Voice command: \"$voiceText\""))
                    repository.insertChatMessage(ChatMessageEntity(sender = "AI", text = "Kanna voice command processed: Launched campaign builder & graphic render pipeline for topic: '$topic' with ${_professionalTone.value} tone."))
                } else {
                    _voiceOutputText.value = "Voice model parsed topic as empty. Say 'Kanna, draft a post about [topic]'"
                }
                _isVoiceActive.value = false
                true
            } else if (lowerMsg.contains("send reply") || lowerMsg.contains("send that reply") || lowerMsg.contains("publish reply") || lowerMsg.contains("approve reply")) {
                val lastNotif = notifications.value.firstOrNull()
                if (lastNotif != null && lastNotif.replyDraft.isNotBlank()) {
                    val originalStatusBeforeDealt = lastNotif.status
                    val updated = lastNotif.copy(status = "DEALT")
                    repository.updateNotification(updated)
                    
                    // Add to recent replies (keeping last 3)
                    val newItem = RecentReplyItem(
                        notificationId = lastNotif.id,
                        senderName = lastNotif.title,
                        replyText = lastNotif.replyDraft,
                        originalStatus = originalStatusBeforeDealt
                    )
                    val currentList = _recentReplies.value.toMutableList()
                    currentList.add(0, newItem)
                    _recentReplies.value = currentList.take(3)

                    _voiceOutputText.value = "Authorized reply dispatched successfully to ${lastNotif.title}."
                    _isVoiceActive.value = false
                    repository.insertChatMessage(ChatMessageEntity(sender = "USER", text = "🎤 Voice command: \"$voiceText\""))
                    repository.insertChatMessage(ChatMessageEntity(sender = "AI", text = "Dispatched approved reply to notification from ${lastNotif.title}."))
                } else {
                    _voiceOutputText.value = "No formulated reply draft was found to send."
                    _isVoiceActive.value = false
                }
                true
            } else if (lowerMsg.contains("give reply to that") || lowerMsg.contains("reply to that") || lowerMsg.contains("give reply") || lowerMsg.contains("reply to last message")) {
                val lastNotif = notifications.value.firstOrNull()
                if (lastNotif != null) {
                    _voiceOutputText.value = "Aura formulating secure draft reply to ${lastNotif.title}..."
                    val now = System.currentTimeMillis()
                    val isInMeeting = calendarEvents.value.any { event ->
                        now >= event.startTime && now <= event.endTime
                    }
                    val result = repository.processNotificationDetails(
                        packageName = lastNotif.packageName,
                        title = lastNotif.title,
                        bodyText = lastNotif.text,
                        highThinking = _highThinkingEnabled.value,
                        isCurrentInMeeting = isInMeeting
                    )
                    val updatedNotification = lastNotif.copy(
                        summary = result.summary,
                        replyDraft = result.replyDraft,
                        urgency = result.urgency,
                        status = "SUMMARIZED"
                    )
                    repository.updateNotification(updatedNotification)
                    
                    _voiceOutputText.value = "Drafted reply for ${lastNotif.title}: \"${result.replyDraft}\"\n\nSay 'send reply' to dispatch it!"
                    _isVoiceActive.value = false
                    
                    repository.insertChatMessage(ChatMessageEntity(sender = "USER", text = "🎤 Voice reply command: \"$voiceText\""))
                    repository.insertChatMessage(ChatMessageEntity(sender = "AI", text = "Formulated smart response draft for notification from ${lastNotif.title}: \"${result.replyDraft}\""))
                    
                    // Regenerate suggestions for the newly summarized notification
                    loadSuggestionsForNotification(updatedNotification)
                } else {
                    _voiceOutputText.value = "Status alert: No active notifications found to reply to."
                    _isVoiceActive.value = false
                }
                true
            } else {
                false
            }

            if (!isActionDone) {
                var actionTakenText = ""
                if (lowerMsg.contains("linkedin") || lowerMsg.contains("insta") || lowerMsg.contains("reply")) {
                    actionTakenText = "\n\n[Action taken: Formulated secure auto-response draft for your review!]"
                }

                val response = repository.processVoiceQuery(voiceText, _highThinkingEnabled.value)
                _voiceOutputText.value = response + actionTakenText
                _isVoiceActive.value = false

                // Insert matching chat traces
                repository.insertChatMessage(ChatMessageEntity(sender = "USER", text = "🎤 Voice command: \"$voiceText\""))
                repository.insertChatMessage(ChatMessageEntity(sender = "AI", text = "Aura Voice Agent: \"$response\"$actionTakenText"))
            }
        }
    }

    fun toggleQuietHours(enabled: Boolean) {
        _quietHoursEnabled.value = enabled
        prefs?.edit()?.putBoolean("quiet_hours_enabled", enabled)?.apply()
    }

    fun updateQuietHoursRange(start: String, end: String) {
        _quietHoursStart.value = start
        _quietHoursEnd.value = end
        prefs?.edit()?.putString("quiet_hours_start", start)?.putString("quiet_hours_end", end)?.apply()
    }

    fun simulateIncomingCall(callerNameInput: String = "Sarah Jenkins") {
        val nameToUse = callerNameInput.ifBlank { "Sarah Jenkins" }
        _callerName.value = nameToUse
        _callerStatus.value = "Ringing..."
        _callTranscripts.value = emptyList()
        _callSummary.value = ""

        // Check Allowed / Blocked list if enabled
        if (_callerFilteringEnabled.value) {
            val blockedList = _blockedCallersList.value.split(",").map { it.trim().lowercase() }
            val allowedList = _allowedCallersList.value.split(",").map { it.trim().lowercase() }
            val inputLower = nameToUse.lowercase()

            val isBlocked = blockedList.any { it.isNotEmpty() && inputLower.contains(it) }
            val isAllowed = allowedList.isEmpty() || allowedList.all { it.isEmpty() } || allowedList.any { inputLower.contains(it) }

            if (isBlocked || !isAllowed) {
                // Instantly decline and notify user in active feeds
                _isIncomingCallActive.value = false
                _isCallActive.value = false
                _callerStatus.value = "Call Screen Filter Blocked"
                
                viewModelScope.launch {
                    repository.insertChatMessage(ChatMessageEntity(
                        sender = "AI",
                        text = "🚫 Blocked incoming call from $nameToUse based on your phone caller filter configuration."
                    ))
                }
                return
            }
        }

        _isIncomingCallActive.value = true
        _isCallActive.value = false

        // Auto-answer after a standard ring interval
        if (_autoAnswerEnabled.value) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(1800)
                if (_isIncomingCallActive.value && !_isCallActive.value) {
                    answerCallWithAura()
                }
            }
        }
    }

    fun declineCall() {
        _isIncomingCallActive.value = false
        _isCallActive.value = false
        stopSpeaking()
    }

    fun answerCallWithAura() {
        _isIncomingCallActive.value = false
        _isCallActive.value = true
        _callerStatus.value = "${_assistantName.value} Speaking..."
        val greeting = _customGreetingScript.value.ifBlank {
            "Hello, this is Vesper, Chaitanya's assistant. Chaitanya is currently busy, but I am screening this call to help him stay focused. Please declare your identity and your message, and I will record it."
        }
        val initialMessage = ChatMessageEntity(
            sender = _assistantName.value,
            text = greeting
        )
        _callTranscripts.value = listOf(initialMessage)
        speakText(greeting)
    }

    fun sendCallerMessage(text: String) {
        if (text.isBlank()) return
        val currentList = _callTranscripts.value.toMutableList()
        val callerMsg = ChatMessageEntity(
            sender = _callerName.value,
            text = text
        )
        currentList.add(callerMsg)
        _callTranscripts.value = currentList
        _callerStatus.value = "${_assistantName.value} Thinking..."

        viewModelScope.launch {
            val auraReplyText = repository.generateCallAssistantResponse(text, _highThinkingEnabled.value)
                .replace("Aura", _assistantName.value)
            val updatedList = _callTranscripts.value.toMutableList()
            val auraMsg = ChatMessageEntity(
                sender = _assistantName.value,
                text = auraReplyText
            )
            updatedList.add(auraMsg)
            _callTranscripts.value = updatedList
            _callerStatus.value = "Call Active"
            speakText(auraReplyText)
        }
    }

    fun endCallAndSummarize() {
        _isIncomingCallActive.value = false
        _isCallActive.value = false
        _callerStatus.value = "Processing secure summary..."
        stopSpeaking()

        viewModelScope.launch {
            val transcriptText = _callTranscripts.value.joinToString("\n") { "${it.sender}: ${it.text}" }
            val summaryText = repository.generateCallAssistantSummary(transcriptText, _highThinkingEnabled.value)
                .replace("Aura", _assistantName.value)
            _callSummary.value = summaryText
            _callerStatus.value = "Call Completed"

            val session = CallSessionEntity(
                callerName = _callerName.value,
                transcript = transcriptText,
                summary = summaryText,
                voiceProfileUsed = _selectedVoiceProfile.value
            )
            repository.insertCallSession(session)

            saveScreenedTranscript("CALL", _callerName.value, transcriptText, summaryText)

            // Post as high priority alert in chat console
            val alertChatMessage = ChatMessageEntity(
                sender = "AI",
                text = "⚡ ${_assistantName.value.uppercase()} SCREENED PHONE CALL:\n\n$summaryText"
            )
            repository.insertChatMessage(alertChatMessage)
        }
    }

    // Setters for Auto-Answer & Screening preferences
    fun toggleAutoAnswer(enabled: Boolean) {
        _autoAnswerEnabled.value = enabled
        prefs?.edit()?.putBoolean("auto_answer_enabled", enabled)?.apply()
    }

    fun updateCustomGreetingScript(script: String) {
        _customGreetingScript.value = script
        prefs?.edit()?.putString("custom_greeting_script", script)?.apply()
    }

    fun toggleCallerFiltering(enabled: Boolean) {
        _callerFilteringEnabled.value = enabled
        prefs?.edit()?.putBoolean("caller_filtering_enabled", enabled)?.apply()
    }

    fun updateAllowedCallers(list: String) {
        _allowedCallersList.value = list
        prefs?.edit()?.putString("allowed_callers_list", list)?.apply()
    }

    fun updateBlockedCallers(list: String) {
        _blockedCallersList.value = list
        prefs?.edit()?.putString("blocked_callers_list", list)?.apply()
    }

    fun updateAssistantName(name: String) {
        _assistantName.value = name
        prefs?.edit()?.putString("assistant_name", name)?.apply()
    }

    fun selectVoiceProfile(profile: String) {
        _selectedVoiceProfile.value = profile
        prefs?.edit()?.putString("selected_voice_profile", profile)?.apply()
    }

    fun deleteCallSession(id: Int) {
        viewModelScope.launch {
            repository.deleteCallSession(id)
        }
    }

    fun clearCallSessions() {
        viewModelScope.launch {
            repository.clearCallSessions()
        }
    }

    fun runSocialTrendAnalysis(platform: String, trendTopic: String) {
        _isSocialProcessing.value = true
        viewModelScope.launch {
            val result = repository.generateSocialTrendAnalysis(platform, trendTopic, _highThinkingEnabled.value)
            _socialTrendResult.value = result
            _isSocialProcessing.value = false
        }
    }

    fun draftLinkedInCampaign(topic: String) {
        _isSocialProcessing.value = true
        viewModelScope.launch {
            val result = repository.generateLinkedInPostCraft(topic, _highThinkingEnabled.value)
            _linkedinDraftPost.value = result
            _isSocialProcessing.value = false
        }
    }

    fun queryActiveMeeting(topic: String, question: String) {
        _isMeetingProcessing.value = true
        viewModelScope.launch {
            val result = repository.queryMeetingTranscript(topic, _activeMeetingTranscript.value, question, _highThinkingEnabled.value)
            _meetingAnswerResult.value = result
            _isMeetingProcessing.value = false
        }
    }

    fun toggleRepresentativeMeetingJoin(joined: Boolean) {
        _isRepresentedInMeeting.value = joined
    }

    fun updateMeetingTranscriptPreset(text: String) {
        _activeMeetingTranscript.value = text
    }

    fun updateAutoModelUpdatesEnabled(enabled: Boolean) {
        _autoModelUpdatesEnabled.value = enabled
        prefs?.edit()?.putBoolean("auto_model_updates", enabled)?.apply()
        AuraDiagnostics.log("SYSTEM", "INFO", "Automatic updates for AI model definitions has been ${if (enabled) "ENABLED" else "DISABLED"}.")
    }

    fun updatePrivacyFirstMode(enabled: Boolean) {
        _privacyFirstMode.value = enabled
        prefs?.edit()?.putBoolean("privacy_first_mode", enabled)?.apply()
        if (enabled) {
            pollingJob?.cancel()
            AuraDiagnostics.log("SYSTEM", "INFO", "Privacy-First mode ACTIVATED. Restricting all background API polling and AI processing to manual triggers.")
        } else {
            startBackgroundConnectivityPolling()
            AuraDiagnostics.log("SYSTEM", "INFO", "Privacy-First mode DEACTIVATED. Background service polling restored.")
        }
    }

    fun updateDndSyncEnabled(context: android.content.Context, enabled: Boolean) {
        _dndSyncEnabled.value = enabled
        prefs?.edit()?.putBoolean("dnd_sync_enabled", enabled)?.apply()
        refreshSystemDndActiveState(context)
        AuraDiagnostics.log("PRIVACY", "INFO", "Do Not Disturb logic synchronization has been ${if (enabled) "ENABLED" else "DISABLED"}.")
    }

    fun updateAiLoggingEnabled(enabled: Boolean) {
        _aiLoggingEnabled.value = enabled
        prefs?.edit()?.putBoolean(SharedPrefsAiLoggingPolicy.KEY_MASTER, enabled)?.apply()
        AuraDiagnostics.log("PRIVACY", "INFO", "AI action history logging has been ${if (enabled) "ENABLED" else "DISABLED"}.")
    }

    fun updateAiLoggingCategoryEnabled(category: AiActionCategory, enabled: Boolean) {
        _aiLoggingCategoryEnabled.value = _aiLoggingCategoryEnabled.value.toMutableMap()
            .apply { put(category, enabled) }
        prefs?.edit()?.putBoolean(SharedPrefsAiLoggingPolicy.categoryKey(category), enabled)?.apply()
        AuraDiagnostics.log("PRIVACY", "INFO", "AI action logging for ${category.label} category has been ${if (enabled) "ENABLED" else "DISABLED"}.")
    }

    fun refreshSystemDndActiveState(context: android.content.Context) {
        try {
            val notificationManager = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val filter = notificationManager.currentInterruptionFilter
            _isSystemDndActive.value = filter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL &&
                                       filter != android.app.NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        } catch (e: Exception) {
            _isSystemDndActive.value = false
        }
    }

    fun updateEncryptionAndPassphrase(enabled: Boolean, passphrase: String) {
        _isEncryptionActive.value = enabled
        _cryptoPassphrase.value = passphrase

        prefs?.edit()
            ?.putBoolean("is_encryption_active", enabled)
            ?.putString("crypto_passphrase", passphrase)
            ?.apply()

        // Re-encrypt/Decrypt stored API Key
        val rawKey = _customApiKey.value
        val keyToSave = if (enabled && passphrase.isNotEmpty()) {
            com.example.data.security.EncryptionHelper.encrypt(rawKey, passphrase)
        } else {
            rawKey
        }
        prefs?.edit()?.putString("custom_api_key", keyToSave)?.apply()

        AuraDiagnostics.log(
            "SECURITY",
            "INFO",
            "Encryption module updated.",
            "Active: $enabled, Passphrase: ${if (passphrase.isNotEmpty()) "Configured" else "None"}"
        )
    }

    fun addPrivacyInsight(appOrServiceName: String, detail: String) {
        viewModelScope.launch {
            repository.insertPrivacyInsight(
                PrivacyInsightEntity(
                    appOrServiceName = appOrServiceName,
                    dataProcessedSummary = detail
                )
            )
        }
    }

    fun clearPrivacyInsightsList() {
        viewModelScope.launch {
            repository.clearPrivacyInsights()
        }
    }

    fun runManualDiagnosticsAndPolling() {
        viewModelScope.launch {
            AuraDiagnostics.log("SYSTEM", "INFO", "Executing Manual Poll & Diagnostic Health Validation check...")
            runDiagnosticConnectivityChecks()
            val activeNotifs = notifications.value.filter { it.status != "DEALT" }
            if (activeNotifs.isNotEmpty()) {
                val result = repository.generatePrioritizedDigest(activeNotifs, _highThinkingEnabled.value)
                _prioritizedDigest.value = result
                addPrivacyInsight("Manual Digest Generator", "Triggered manual analysis of ${activeNotifs.size} notifications. Created local flow index digest (${String.format("%.2f", result.length / 1024.0)} KB).")
            } else {
                addPrivacyInsight("Manual Diagnostic Pinger", "Triggered manual diagnostics handshake verification. Model handshake completed.")
            }
        }
    }

    // --- Biometric/Passcode Authentication Layer & Data Sanitizing ---

    fun updatePasscodeSettings(passcode: String, enabled: Boolean) {
        _appPasscode.value = passcode
        _isPasscodeLockEnabled.value = enabled
        if (!enabled) {
            _isAppUnlocked.value = true
        } else if (_isAppUnlocked.value && passcode.isNotEmpty()) {
            // Keep unlocked for current session unless locked
        } else {
            _isAppUnlocked.value = false
        }

        prefs?.edit()
            ?.putString("app_passcode", passcode)
            ?.putBoolean("app_passcode_enabled", enabled)
            ?.apply()

        AuraDiagnostics.log("SECURITY", "INFO", "Passcode settings altered. Lock: $enabled")
    }

    fun unlockApp(passcodeAttempt: String): Boolean {
        return if (passcodeAttempt == _appPasscode.value) {
            _isAppUnlocked.value = true
            AuraDiagnostics.log("SECURITY", "INFO", "Passcode Authentication successful. Restoring access dashboard.")
            addPrivacyInsight("Credential Gatekeeper", "Successful biometric/passcode handshake completed. Dashboard unlocked.")
            true
        } else {
            AuraDiagnostics.log("SECURITY", "WARN", "Unauthorized device access attempt. Incorrect passcode credentials entered.")
            addPrivacyInsight("Credential Gatekeeper", "Blocked unauthorized dashboard access attempt. Invalid credentials.")
            false
        }
    }

    fun lockApp() {
        if (_isPasscodeLockEnabled.value) {
            _isAppUnlocked.value = false
            AuraDiagnostics.log("SECURITY", "INFO", "Application locked. Re-authentication required on return.")
        }
    }

    fun updateAutoDeleteInterval(days: Int) {
        _autoDeleteDays.value = days
        prefs?.edit()?.putInt("auto_delete_interval_days", days)?.apply()
        
        AuraDiagnostics.log("SYSTEM", "INFO", "Auto-cleanup policy adjusted to $days days.")
        addPrivacyInsight("Privacy Policy Manager", "Data retention window updated to reference $days-day maximum threshold.")

        if (days > 0) {
            viewModelScope.launch {
                repository.autoCleanupOldData(days)
                AuraDiagnostics.log("SYSTEM", "INFO", "Automatic cleanup applied instantly for $days days cutoff filter.")
            }
        }
    }

    fun clearAllLocalDataSecurely() {
        viewModelScope.launch {
            repository.clearAllLocalData()
            AuraDiagnostics.clear()
            
            _prioritizedDigest.value = ""
            _currentSuggestions.value = emptyList()
            _activeMeetingTranscript.value = ""
            _recentReplies.value = emptyList()

            // Record the purge action itself in the fresh database
            addPrivacyInsight(
                "Secure Memory Sanitizer",
                "Executed localized complete wipe of database logs, cache files, transcripts, and model configurations from device."
            )
            AuraDiagnostics.log("SECURITY", "INFO", "Safe-wipe initiated. All localized database cache blocks have been zeroed out.")
        }
    }

    fun addCallScreeningRule(pattern: String, action: String, description: String = "") {
        viewModelScope.launch {
            repository.insertCallScreeningRule(
                CallScreeningRuleEntity(pattern = pattern, action = action, description = description)
            )
            AuraDiagnostics.log("SCREENING", "INFO", "Added screening rule: $pattern -> $action")
            addPrivacyInsight(
                "Call Filter Gatekeeper",
                "Saved caller ID rule for '$pattern' tagged for on-device activity [$action]."
            )
        }
    }

    fun deleteCallScreeningRule(id: Int) {
        viewModelScope.launch {
            repository.deleteCallScreeningRule(id)
            AuraDiagnostics.log("SCREENING", "INFO", "Deleted screening rule with identifier: $id")
        }
    }

    fun addEmailTemplate(name: String, content: String, category: String) {
        viewModelScope.launch {
            repository.insertEmailTemplate(
                EmailTemplateEntity(name = name, content = content, category = category)
            )
            AuraDiagnostics.log("REPLY_TEMPLATE", "INFO", "Saved automated snippet: '$name'")
            addPrivacyInsight(
                "Template Store",
                "Saved local quick response draft '$name' under group '$category'."
            )
        }
    }

    fun deleteEmailTemplate(id: Int) {
        viewModelScope.launch {
            repository.deleteEmailTemplate(id)
            AuraDiagnostics.log("REPLY_TEMPLATE", "INFO", "Removed response snippet token: $id")
        }
    }

    fun updateOnDeviceProcessing(enabled: Boolean) {
        _isOnDeviceProcessingEnabled.value = enabled
        prefs?.edit()?.putBoolean("on_device_processing_enabled", enabled)?.apply()
        AuraDiagnostics.log("PRIVACY", "INFO", "On-device processing state: $enabled")
        addPrivacyInsight(
            "AI Sandbox Engine",
            "On-device processing sandboxed toggle state set to: ${if (enabled) "RESTRICTED" else "OFFLINE"}."
        )
    }

    fun syncLocalCalendarEvents(context: Context) {
        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_CALENDAR) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            AuraDiagnostics.log("CALENDAR", "WARN", "Cannot synchronize system events: READ_CALENDAR permission missing.")
            return
        }
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val uri = android.provider.CalendarContract.Events.CONTENT_URI
                val projection = arrayOf(
                    android.provider.CalendarContract.Events._ID,
                    android.provider.CalendarContract.Events.TITLE,
                    android.provider.CalendarContract.Events.ORGANIZER,
                    android.provider.CalendarContract.Events.DTSTART,
                    android.provider.CalendarContract.Events.DTEND
                )
                val now = System.currentTimeMillis()
                val selection = "${android.provider.CalendarContract.Events.DTSTART} >= ? AND ${android.provider.CalendarContract.Events.DTSTART} <= ?"
                val selectionArgs = arrayOf(
                    (now - 12 * 60 * 60 * 1000L).toString(), // 12 hours ago
                    (now + 48 * 60 * 60 * 1000L).toString()  // 48 hours in future
                )
                
                val cursor = contentResolver.query(uri, projection, selection, selectionArgs, "${android.provider.CalendarContract.Events.DTSTART} ASC")
                cursor?.use { c ->
                    val titleIdx = c.getColumnIndex(android.provider.CalendarContract.Events.TITLE)
                    val orgIdx = c.getColumnIndex(android.provider.CalendarContract.Events.ORGANIZER)
                    val startIdx = c.getColumnIndex(android.provider.CalendarContract.Events.DTSTART)
                    val endIdx = c.getColumnIndex(android.provider.CalendarContract.Events.DTEND)
                    
                    while (c.moveToNext()) {
                        val title = if (titleIdx >= 0) c.getString(titleIdx) ?: "Joint Project alignment" else "Joint Project alignment"
                        val organizer = if (orgIdx >= 0) c.getString(orgIdx) ?: "engineering-leads@firmwide.com" else "engineering-leads@firmwide.com"
                        val start = if (startIdx >= 0) c.getLong(startIdx) else now
                        val end = if (endIdx >= 0) c.getLong(endIdx) else now + 3600000L
                        
                        val event = CalendarEventEntity(
                            title = title,
                            organizer = organizer,
                            startTime = start,
                            endTime = end,
                            status = "PENDING",
                            summary = "Synced from Android calendar provider"
                        )
                        repository.insertCalendarEvent(event)
                    }
                }
                AuraDiagnostics.log("CALENDAR", "INFO", "Successfully synchronized system calendar events with Aura secure vault.")
                addPrivacyInsight("Calendar Provider", "Synchronized localized metadata for upcoming engagements with on-screen prioritization.")
            } catch (e: Exception) {
                AuraDiagnostics.log("CALENDAR", "ERROR", "Secure Calendar sync failed: ${e.localizedMessage}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        try {
            textToSpeech?.shutdown()
        } catch (e: Exception) {}
        try {
            wakeWordWatcher?.stopListening()
        } catch (e: Exception) {}
        deepWorkTimerJob?.cancel()
    }

    // --- DEEP WORK FOCUS CONTROLLER ---
    fun toggleDeepWorkMode() {
        val newVal = !_isDeepWorkActive.value
        _isDeepWorkActive.value = newVal
        val p = prefs ?: return
        
        if (newVal) {
            val durationMs = _deepWorkDurationMinutes.value * 60 * 1000L
            val untilMs = System.currentTimeMillis() + durationMs
            p.edit()
                .putBoolean("deep_work_active", true)
                .putLong("deep_work_until", untilMs)
                .putInt("deep_work_duration_minutes", _deepWorkDurationMinutes.value)
                .apply()
            
            startDeepWorkCountdown(durationMs)
            
            // Insert calendar busy event
            viewModelScope.launch {
                repository.insertCalendarEvent(
                    com.example.data.db.CalendarEventEntity(
                        title = "Deep Work Focus Block [Busy]",
                        organizer = _assistantName.value,
                        startTime = System.currentTimeMillis(),
                        endTime = untilMs,
                        status = "Busy",
                        summary = "Deep Work mode initiated. Non-urgent incoming alerts are automatically silent."
                    )
                )
                
                // Add privacy insight
                val insight = com.example.data.db.PrivacyInsightEntity(
                    appOrServiceName = "Deep Work Monitor",
                    dataProcessedSummary = "Active. Automatically silencing notifications and set calendar status to Busy for ${_deepWorkDurationMinutes.value} minutes."
                )
                repository.insertPrivacyInsight(insight)
            }
        } else {
            p.edit()
                .putBoolean("deep_work_active", false)
                .putLong("deep_work_until", 0L)
                .apply()
            
            _deepWorkTimeRemaining.value = 0L
            deepWorkTimerJob?.cancel()
            deepWorkTimerJob = null
            
            // Generate non-urgent digested alert summary
            aggregateDeepWorkSilencedNotifications()

            // Log manually stopped
            viewModelScope.launch {
                val insight = com.example.data.db.PrivacyInsightEntity(
                    appOrServiceName = "Deep Work Monitor",
                    dataProcessedSummary = "Deep Work focus period terminated manually."
                )
                repository.insertPrivacyInsight(insight)
            }
        }
    }

    private fun aggregateDeepWorkSilencedNotifications() {
        viewModelScope.launch {
            // Find all notifications matching silencedByDeepWork flag
            val silencedList = notifications.value.filter { it.silencedByDeepWork }
            if (silencedList.isEmpty()) {
                val analyticsNotification = NotificationEntity(
                    packageName = "com.aura.digest",
                    title = "Deep Work Analytics Review",
                    text = "A perfect session. Zero intrusive notifications or disruptions were intercepted during this focus period.",
                    urgency = "LOW",
                    silencedByDeepWork = false
                )
                repository.insertNotification(analyticsNotification)
                return@launch
            }
            
            val sb = java.lang.StringBuilder()
            sb.append("While you were focus locked, Aura intercepted ${silencedList.size} alerts to safeguard your time:\n\n")
            silencedList.forEach { notif ->
                val appLabel = when {
                    notif.packageName.contains("whatsapp") -> "WhatsApp"
                    notif.packageName.contains("gmail") -> "Gmail"
                    notif.packageName.contains("google.android.gm") -> "Gmail"
                    notif.packageName.contains("linkedin") -> "LinkedIn"
                    else -> "App notification"
                }
                sb.append("• [ $appLabel ] ${notif.title}: ${notif.text}\n")
            }
            
            // Insert single consolidated digested alert notification
            val digestNotification = NotificationEntity(
                packageName = "com.aura.digest",
                title = "Deep Work Serenity Summary (${silencedList.size} Alerts)",
                text = sb.toString(),
                urgency = "URGENT",
                silencedByDeepWork = false
            )
            repository.insertNotification(digestNotification)
            
            // Update individual silenced alerts so they are marked as processed/not silenced
            silencedList.forEach { notif ->
                val updated = notif.copy(
                    silencedByDeepWork = false,
                    status = "SUMMARIZED"
                )
                repository.updateNotification(updated)
            }
            
            AuraDiagnostics.log("DIGEST", "INFO", "Generated composite digested block summary for ${silencedList.size} notifications.")
        }
    }

    fun previewVoiceProfile(profileName: String) {
        val sampleText = when (profileName) {
            "Kanna Classic" -> "Hello, this is the Kanna Classic voice profile. Delivering crisp and friendly security responses."
            "Calm Professional" -> "Greetings. This is the Calm Professional voice profile. Optimized for clear, authoritative professional communication."
            "Echo Sentinel" -> "System check. Echo Sentinel online. Monitoring all communication channels with strict privacy protection."
            "Stellar Voice" -> "Hi there! I am your Stellar Voice assistant. Bright, prompt, and ready to assist you anytime."
            "My Custom Profile" -> "Voice cloning initialized. This is a personalized synthesis of your own unique vocal signature."
            else -> "Hello. This is the custom audio template feedback loop."
        }
        
        val pitch = when (profileName) {
            "Kanna Classic" -> 1.0f
            "Calm Professional" -> 0.82f
            "Echo Sentinel" -> 0.68f
            "Stellar Voice" -> 1.25f
            "My Custom Profile" -> 1.08f
            else -> 1.0f
        }
        val rate = when (profileName) {
            "Kanna Classic" -> 1.0f
            "Calm Professional" -> 0.88f
            "Echo Sentinel" -> 0.82f
            "Stellar Voice" -> 1.18f
            "My Custom Profile" -> 1.0f
            else -> 1.0f
        }
        
        try {
            textToSpeech?.setPitch(pitch)
            textToSpeech?.setSpeechRate(rate)
            speakText(sampleText)
            // Restore default pitch and rate
            viewModelScope.launch {
                kotlinx.coroutines.delay(8000L)
                textToSpeech?.setPitch(1.0f)
                textToSpeech?.setSpeechRate(1.0f)
            }
        } catch (e: Exception) {
            android.util.Log.e("AuraVM", "TTS update failed in previewVoiceProfile: ${e.localizedMessage}")
        }
    }

    private fun startDeepWorkCountdown(durationMs: Long) {
        deepWorkTimerJob?.cancel()
        _deepWorkTimeRemaining.value = durationMs
        deepWorkTimerJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.Main) {
            var remaining = durationMs
            while (remaining > 0) {
                kotlinx.coroutines.delay(1000)
                remaining -= 1000
                _deepWorkTimeRemaining.value = remaining.coerceAtLeast(0L)
            }
            // Countdown finished!
            _isDeepWorkActive.value = false
            _deepWorkTimeRemaining.value = 0L
            prefs?.edit()?.putBoolean("deep_work_active", false)?.apply()
            
            // Consolidate intercepted non-urgent notifications at end of session
            aggregateDeepWorkSilencedNotifications()

            val insight = com.example.data.db.PrivacyInsightEntity(
                appOrServiceName = "Deep Work Monitor",
                dataProcessedSummary = "Deep Work focus period completed. Automatic notification interceptor rules normalized."
            )
            repository.insertPrivacyInsight(insight)
        }
    }

    fun updateDeepWorkDuration(minutes: Int) {
        _deepWorkDurationMinutes.value = minutes
        prefs?.edit()?.putInt("deep_work_duration_minutes", minutes)?.apply()
    }

    // --- CONTACTS & AI TONES ---
    fun addContact(name: String, phoneNumber: String, category: String, aiResponseTone: String, isPriority: Boolean = false) {
        viewModelScope.launch {
            repository.insertContact(
                com.example.data.db.AuraContactEntity(
                    name = name,
                    phoneNumber = phoneNumber,
                    category = category,
                    aiResponseTone = aiResponseTone,
                    isPriority = isPriority
                )
            )
            val insight = com.example.data.db.PrivacyInsightEntity(
                appOrServiceName = "Contacts Engine",
                dataProcessedSummary = "Registered secure contact $name. Categorized under $category group with $aiResponseTone Tone constraint (Priority Bypass: $isPriority)."
            )
            repository.insertPrivacyInsight(insight)
        }
    }

    fun deleteContact(id: Int) {
        viewModelScope.launch {
            repository.deleteContact(id)
        }
    }

    // --- TRANSCRIBED SECURE VAULT ---
    fun saveScreenedTranscript(type: String, source: String, rawText: String, rawSummary: String) {
        viewModelScope.launch {
            val isEncrypted = _isEncryptionActive.value
            val passphrase = _cryptoPassphrase.value
            
            val finalFormatText = if (isEncrypted && passphrase.isNotBlank()) {
                com.example.data.security.EncryptionHelper.encrypt(rawText, passphrase)
            } else {
                rawText
            }

            val finalFormatSummary = if (isEncrypted && passphrase.isNotBlank()) {
                com.example.data.security.EncryptionHelper.encrypt(rawSummary, passphrase)
            } else {
                rawSummary
            }

            repository.insertScreenedTranscript(
                com.example.data.db.ScreenedTranscriptEntity(
                    type = type,
                    source = source,
                    transcriptText = finalFormatText,
                    summary = finalFormatSummary,
                    isEncrypted = isEncrypted
                )
            )
        }
    }

    fun deleteScreenedTranscript(id: Int) {
        viewModelScope.launch {
            repository.deleteScreenedTranscript(id)
        }
    }

    fun clearScreenedTranscripts() {
        viewModelScope.launch {
            repository.clearScreenedTranscripts()
        }
    }

    fun deleteAiAction(id: Int) {
        viewModelScope.launch {
            repository.deleteAiAction(id)
        }
    }

    fun clearAiActionHistory() {
        viewModelScope.launch {
            repository.clearAiActionHistory()
        }
    }

    fun getDecryptedText(encryptedText: String, isEncrypted: Boolean): String {
        return if (isEncrypted) {
            val passphrase = _cryptoPassphrase.value
            if (passphrase.isNotBlank()) {
                try {
                    com.example.data.security.EncryptionHelper.decrypt(encryptedText, passphrase)
                } catch (e: Exception) {
                    "[Decryption Failed - Verification Passphrase Required]"
                }
            } else {
                "[Encrypted SQLite Data Vault]"
            }
        } else {
            encryptedText
        }
    }

    // --- PDF ARCHIVAL REPORT GENERATION ---
    fun exportWeeklyReportAsPdf(context: Context, onComplete: (java.io.File?) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val notifList = notifications.value
                val sessionList = callSessions.value
                
                val document = android.graphics.pdf.PdfDocument()
                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
                val page = document.startPage(pageInfo)
                val canvas = page.canvas
                val paint = android.graphics.Paint()
                
                // Draw title
                paint.color = android.graphics.Color.DKGRAY
                paint.textSize = 18f
                paint.isFakeBoldText = true
                canvas.drawText("AURA AI SECURE AUTOMATION NODE REPORTS", 50f, 60f, paint)
                
                // Draw metadata
                paint.textSize = 10f
                paint.color = android.graphics.Color.GRAY
                paint.isFakeBoldText = false
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                canvas.drawText("Report Timestamp: ${sdf.format(java.util.Date())}", 50f, 85f, paint)
                canvas.drawText("Privacy Standard: Local On-Device Verification (Absolute Offline Encrypted)", 50f, 100f, paint)
                
                // Draw dividing line
                paint.color = android.graphics.Color.GRAY
                canvas.drawLine(50f, 115f, 545f, 115f, paint)
                
                var currentY = 145f
                
                // Notifs summary header
                paint.color = android.graphics.Color.BLACK
                paint.textSize = 13f
                paint.isFakeBoldText = true
                canvas.drawText("I. SUMMARY REPORT OF RECENT ALERTS INTERCEPTED", 50f, currentY, paint)
                currentY += 25f
                
                paint.textSize = 9.5f
                paint.isFakeBoldText = false
                if (notifList.isEmpty()) {
                    canvas.drawText("Check-In: No intercepted notification logs recorded in this period.", 70f, currentY, paint)
                    currentY += 20f
                } else {
                    notifList.take(8).forEach { item ->
                        if (currentY > 440f) return@forEach // boundary guard
                        paint.isFakeBoldText = true
                        canvas.drawText("[${item.urgency}] [${item.packageName}] - ${item.title}", 70f, currentY, paint)
                        currentY += 15f
                        paint.isFakeBoldText = false
                        val cleanText = if (item.text.length > 85) item.text.take(82) + "..." else item.text
                        canvas.drawText(cleanText, 85f, currentY, paint)
                        currentY += 20f
                    }
                }
                
                currentY = 480f
                // Call screening summary header
                paint.isFakeBoldText = true
                paint.textSize = 13f
                canvas.drawText("II. RECENT LOGS OF SCREENED RECOGNITION CALLS", 50f, currentY, paint)
                currentY += 25f
                
                paint.textSize = 9.5f
                paint.isFakeBoldText = false
                if (sessionList.isEmpty()) {
                    canvas.drawText("Check-In: No voice-telephony call screening logs recorded in this period.", 70f, currentY, paint)
                    currentY += 20f
                } else {
                    sessionList.take(5).forEach { call ->
                        if (currentY > 780f) return@forEach
                        paint.isFakeBoldText = true
                        canvas.drawText("Screened Caller: ${call.callerName} [Profile: ${call.voiceProfileUsed}]", 70f, currentY, paint)
                        currentY += 15f
                        paint.isFakeBoldText = false
                        val cleanSum = if (call.summary.length > 85) call.summary.take(82) + "..." else call.summary
                        canvas.drawText("Summary: $cleanSum", 85f, currentY, paint)
                        currentY += 20f
                    }
                }
                
                // Footer
                paint.textSize = 8f
                paint.color = android.graphics.Color.LTGRAY
                canvas.drawText("AUTHENTIC AUTONOMOUS DATA VAULT. DO NOT COPY.", 190f, 810f, paint)
                
                document.finishPage(page)
                
                // Write PDF
                val file = java.io.File(context.cacheDir, "Aura_Weekly_Report.pdf")
                val os = java.io.FileOutputStream(file)
                document.writeTo(os)
                os.close()
                document.close()
                
                // Fire privacy insight
                val insight = com.example.data.db.PrivacyInsightEntity(
                    appOrServiceName = "Data Archiver",
                    dataProcessedSummary = "Generated formatted PDF report containing ${notifList.size} alerts and ${sessionList.size} call screenings."
                )
                repository.insertPrivacyInsight(insight)
                
                onComplete(file)
            } catch (e: Exception) {
                android.util.Log.e("AuraPDF", "Failed to compile report", e)
                onComplete(null)
            }
        }
    }

    fun toggleResearchMode(enabled: Boolean) {
        _researchModeEnabled.value = enabled
        prefs?.edit()?.putBoolean("research_mode_enabled", enabled)?.apply()
        AuraDiagnostics.log("SYSTEM", "INFO", "Fact-checking Research Mode: ${if (enabled) "Enabled (Google Search Grounding Connected)" else "Disabled"}")
    }

    fun triggerVoiceSnippedRecorded() {
        _customVoiceToneGenerated.value = true
        prefs?.edit()?.putBoolean("custom_voice_tone_generated", true)?.apply()
        _selectedVoiceProfile.value = "My Custom Profile"
        prefs?.edit()?.putString("selected_voice_profile", "My Custom Profile")?.apply()
        AuraDiagnostics.log("SYSTEM", "INFO", "Personalized custom voice tone profile successfully compiled & registered.")
        addPrivacyInsight("Voice Modeler", "Synthesized local biometric tone profile 'My Custom Profile' safely stored in private app sandbox.")
    }

    fun generatePostMeetingSummary(eventId: Int) {
        viewModelScope.launch {
            try {
                val eventList = repository.calendarEvents.first()
                val event = eventList.find { it.id == eventId } ?: return@launch
                
                // Find notifications incoming between meeting start and end
                var rawNotifs = notifications.value.filter { it.timestamp in event.startTime..event.endTime }
                if (rawNotifs.isEmpty()) {
                    rawNotifs = notifications.value.filter { 
                        val diffMs = Math.abs(it.timestamp - System.currentTimeMillis())
                        diffMs < 7200000 // Last 2 hours fallback for resilient simulation
                    }
                }
                
                val sb = java.lang.StringBuilder()
                sb.append("AURA AUTOMATION POST-MEETING RECON SUMMARY\n")
                sb.append("========================================\n")
                sb.append("Meeting: ${event.title}\n")
                sb.append("Organizer: ${event.organizer}\n")
                val dateFmt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
                sb.append("Interception Window: ${dateFmt.format(java.util.Date(event.startTime))} to ${dateFmt.format(java.util.Date(event.endTime))}\n\n")
                
                sb.append("I. SCREENED DISRUPTIONS SUMMARY:\n")
                if (rawNotifs.isEmpty()) {
                    sb.append("- Absolute serenity. No intrusive alerts were intercepted during this period.\n")
                } else {
                    rawNotifs.forEach { notif ->
                        val stateStr = if (notif.status == "REPLIED" || notif.status == "DEALT") "Auto-replied" else "Silenced to Vault"
                        sb.append("- [${notif.urgency}] [${notif.packageName}] ${notif.title}: ${notif.text} (Handled: $stateStr)\n")
                    }
                }
                sb.append("\nII. AI KANNA PROXY TRANSCRIBER RECON:\n")
                if (event.transcript.isNotBlank()) {
                    sb.append("Live recorded proxy channel transcript:\n")
                    sb.append(event.transcript)
                } else {
                    sb.append("System noted virtual attending session. Prepared autonomous binder details for Chaitanya.")
                }
                
                val summaryText = sb.toString()
                val updatedEvent = event.copy(status = "COMPLETED", summary = summaryText)
                repository.insertCalendarEvent(updatedEvent)
                
                // Create a secure document for the meeting summary in the local vault with customized file name decoration
                val cleanTitle = event.title.replace("[^a-zA-Z0-9]".toRegex(), "_")
                    .replace("_+".toRegex(), "_").trim('_')
                val fileName = "Meeting_Summary_${cleanTitle}_${System.currentTimeMillis() % 10000}.txt"
                val size = "${(summaryText.length * 2) + 120} B"
                repository.insertSecureFile(
                    com.example.data.db.SecureFileEntity(
                        name = fileName,
                        sizeStr = size,
                        content = summaryText
                    )
                )
                
                AuraDiagnostics.log("SYSTEM", "INFO", "Generated post-meeting summary for '${event.title}' parsing ${rawNotifs.size} screened notifications.")
                addPrivacyInsight("Post-Meeting Summarizer", "Compiled composite intelligence record mapping ${rawNotifs.size} screened items to meeting context.")
            } catch (e: Exception) {
                android.util.Log.e("AuraVM", "Failed to generate meeting summary", e)
            }
        }
    }

    fun draftEmailForMeeting(event: CalendarEventEntity) {
        viewModelScope.launch {
            val summaryText = event.summary
            val targetEmail = if (event.organizer.contains("@")) {
                event.organizer
            } else {
                val cleanName = event.organizer.lowercase().replace(" ", ".").replace("[^a-z.]".toRegex(), "")
                if (cleanName.isNotEmpty()) "$cleanName@example.com" else "team@example.com"
            }
            val subjectLine = "RECON: ${event.title}"
            val bodyText = "Hello,\n\nPlease find the post-meeting summary for our synchronization:\n\n$summaryText\n\nGenerated secure draft from calendar logs."
            val email = EmailEntity(
                sender = targetEmail,
                subject = subjectLine,
                body = bodyText,
                replyDraft = "The meeting summary is perfect. Dispatched and acknowledged.",
                status = "SUMMARIZED"
            )
            repository.insertEmail(email)
            
            AuraDiagnostics.log("SYSTEM", "INFO", "Drafted post-meeting summary email for ${event.organizer}.")
            addPrivacyInsight("Email Draft Engine", "Automatically synthesized custom email draft pre-filled with organizer '${event.organizer}' and summary for '${event.title}'.")
        }
    }

    fun checkForUpdatesManual() {
        if (_isCheckingForUpdates.value) return
        viewModelScope.launch {
            _isCheckingForUpdates.value = true
            _updateStatusMessage.value = "Connecting to Kanna secure update server..."
            kotlinx.coroutines.delay(1000)
            _updateStatusMessage.value = "Reading cryptographic release manifest..."
            kotlinx.coroutines.delay(800)
            _availableVersion.value = "2.5.0"
            _isUpdateAvailable.value = true
            _updateStatusMessage.value = "New release v2.5.0 available. Critical stability patches."
            AuraDiagnostics.log("SYSTEM", "INFO", "Manual update check complete. Found update v2.5.0.")
            _isCheckingForUpdates.value = false
        }
    }

    fun applyUpdateNow() {
        if (_isCheckingForUpdates.value) return
        viewModelScope.launch {
            _isCheckingForUpdates.value = true
            _updateStatusMessage.value = "Downloading release v2.5.0 cryptopackage..."
            kotlinx.coroutines.delay(1200)
            _updateStatusMessage.value = "Reconstructing application binary state..."
            kotlinx.coroutines.delay(1000)
            _isUpdateAvailable.value = false
            _currentVersion.value = "2.5.0"
            _availableVersion.value = "2.5.0"
            _updateStatusMessage.value = "Upgrade complete. Current: Kanna AI v2.5.0"
            
            // Record version installation in Room database
            try {
                repository.insertVersionInstallation(
                    com.example.data.db.VersionInstallationEntity(
                        version = "2.5.0",
                        timestamp = System.currentTimeMillis()
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("AuraViewModel", "Failed to log version installation in database", e)
            }

            AuraDiagnostics.log("SYSTEM", "SECURE", "Applied Kanna AI software update v2.5.0 successfully.")
            _isCheckingForUpdates.value = false
        }
    }
}

// Factory Configuration
class AuraViewModelFactory(private val repository: AuraRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuraViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

data class RecentReplyItem(
    val notificationId: Int,
    val senderName: String,
    val replyText: String,
    val originalStatus: String,
    val timestamp: Long = System.currentTimeMillis()
)
