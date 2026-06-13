package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.AuraWakeWordWatcher
import com.example.data.db.ChatMessageEntity
import com.example.data.db.UserProfileEntity
import com.example.data.db.NotificationEntity
import com.example.data.db.EmailEntity
import com.example.data.db.SecureFileEntity
import com.example.data.db.SocialPostEntity
import com.example.data.db.CallSessionEntity
import com.example.data.db.CalendarEventEntity
import com.example.data.db.AuraTaskEntity
import com.example.data.repository.JobHunterRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class JobHunterViewModel(private val repository: JobHunterRepository) : ViewModel() {

    // streams from DB
    val chatMessages: StateFlow<List<ChatMessageEntity>> = repository.chatMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfileEntity?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val notifications: StateFlow<List<NotificationEntity>> = repository.notifications
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

    private val _professionalTone = MutableStateFlow("Formal")
    val professionalTone: StateFlow<String> = _professionalTone.asStateFlow()

    // UI Configuration States
    private val _highThinkingEnabled = MutableStateFlow(false)
    val highThinkingEnabled: StateFlow<Boolean> = _highThinkingEnabled.asStateFlow()

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

    // Siri Voice Core States
    private val _isVoiceActive = MutableStateFlow(false)
    val isVoiceActive: StateFlow<Boolean> = _isVoiceActive.asStateFlow()

    private val _voiceOutputText = MutableStateFlow("")
    val voiceOutputText: StateFlow<String> = _voiceOutputText.asStateFlow()

    private val _isSiriOverlayVisible = MutableStateFlow(false)
    val isSiriOverlayVisible: StateFlow<Boolean> = _isSiriOverlayVisible.asStateFlow()

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
            android.util.Log.e("JobHunterVM", "Failed to initialize TextToSpeech", e)
        }
    }

    fun speakText(text: String) {
        try {
            textToSpeech?.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "aura_screening_tts_id")
        } catch (e: Exception) {
            android.util.Log.e("JobHunterVM", "TTS Speak error: ${e.localizedMessage}")
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

    private val _isWakeWordEnabled = MutableStateFlow(false)
    val isWakeWordEnabled: StateFlow<Boolean> = _isWakeWordEnabled.asStateFlow()

    private val _wakeStatus = MutableStateFlow("Wake word detection disabled")
    val wakeStatus: StateFlow<String> = _wakeStatus.asStateFlow()

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
            android.util.Log.e("JobHunterVM", "Error launching battery helper receiver", e)
        }

        wakeWordWatcher = AuraWakeWordWatcher(
            context = context.applicationContext,
            onWakeWordDetected = {
                // Trigger Siri voice console hud
                setSiriOverlayVisible(true)
                processVoiceInput("") // launch siri overlay in active listener mode
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
    }

    fun updateProfessionalTone(tone: String) {
        _professionalTone.value = tone
        prefs?.edit()?.putString("professional_tone", tone)?.apply()
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

    fun setSiriOverlayVisible(visible: Boolean) {
        _isSiriOverlayVisible.value = visible
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
        }
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
            val result = repository.processNotificationDetails(
                packageName = notification.packageName,
                title = notification.title,
                bodyText = notification.text,
                highThinking = _highThinkingEnabled.value
            )
            val updatedNotification = notification.copy(
                summary = result.summary,
                replyDraft = result.replyDraft,
                urgency = result.urgency,
                status = "SUMMARIZED"
            )
            repository.updateNotification(updatedNotification)
            _processingNotificationId.value = null
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
                text = text
            )
            repository.insertNotification(item)
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
                highThinking = _highThinkingEnabled.value
            )
            val updatedEmail = email.copy(
                summary = result.first,
                replyDraft = result.second,
                status = "SUMMARIZED"
            )
            repository.updateEmail(updatedEmail)
            _processingEmailId.value = null
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
                highThinking = _highThinkingEnabled.value
            )
            val updated = post.copy(
                replyDraft = result.second,
                status = "DRAFTED"
            )
            repository.updateSocialPost(updated)
            _processingSocialPostId.value = null
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
            val craftedContent = repository.generateLinkedInPostCraft(topic, _highThinkingEnabled.value)
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
        _isSiriOverlayVisible.value = true
        _isVoiceActive.value = true
        
        if (voiceText.isBlank()) {
            _voiceOutputText.value = "Aura Active & Listening..."
            return
        }
        
        viewModelScope.launch {
            _voiceOutputText.value = "Aura listening and deciphering secure command..."
            val lowerMsg = voiceText.lowercase()
 
            val isActionDone = if (lowerMsg.contains("send reply") || lowerMsg.contains("send that reply") || lowerMsg.contains("publish reply") || lowerMsg.contains("approve reply")) {
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
                    val result = repository.processNotificationDetails(
                        packageName = lastNotif.packageName,
                        title = lastNotif.title,
                        bodyText = lastNotif.text,
                        highThinking = _highThinkingEnabled.value
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

    override fun onCleared() {
        super.onCleared()
        try {
            textToSpeech?.shutdown()
        } catch (e: Exception) {}
        try {
            wakeWordWatcher?.stopListening()
        } catch (e: Exception) {}
    }
}

// Factory Configuration
class JobHunterViewModelFactory(private val repository: JobHunterRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(JobHunterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return JobHunterViewModel(repository) as T
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
