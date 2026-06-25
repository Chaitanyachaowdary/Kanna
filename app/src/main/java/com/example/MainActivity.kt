package com.example

import android.os.Bundle
import android.net.Uri
import android.util.Base64
import android.content.Intent
import android.content.ComponentName
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import kotlin.math.sin
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.data.db.AppDatabase
import com.example.data.db.ChatMessageEntity
import com.example.data.db.NotificationEntity
import com.example.data.db.EmailEntity
import com.example.data.db.UserProfileEntity
import com.example.data.db.SecureFileEntity
import com.example.data.db.SocialPostEntity
import com.example.data.db.CallSessionEntity
import com.example.data.db.CalendarEventEntity
import com.example.data.db.AuraTaskEntity
import com.example.data.db.PrivacyInsightEntity
import com.example.data.db.CallScreeningRuleEntity
import com.example.data.db.EmailTemplateEntity
import com.example.data.diagnostics.*
import com.example.data.repository.AiActionCategory
import com.example.data.repository.AuraRepository
import com.example.data.repository.SharedPrefsAiLoggingPolicy
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuraViewModel
import com.example.ui.viewmodel.AuraViewModelFactory
import com.example.ui.viewmodel.RecentReplyItem
import kotlinx.coroutines.launch

// High-Fidelity Cyber Obsidian Palette
val PrimaryAccent = Color(0xFFd946ef)     // Neon Mystic Lavender
val SecondaryAccent = Color(0xFF06b6d4)   // Tech Cyberspace Cyan
val AlertOrange = Color(0xFFf97316)       // Safe warning orange
val BackgroundDark = Color(0xFF030712)    // Abyss deep midnight black
val SurfaceDark = Color(0xFF111827)       // Deep space graphite card
val BorderGrey = Color(0xFF1f2937)        // Stealth matte gridline
val TextLight = Color(0xFFf3f4f6)         // Pure bright starlight
val TextMuted = Color(0xFF9ca3af)         // Cosmic titanium grey

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        val database = AppDatabase.getDatabase(this)
        val loggingPolicy = SharedPrefsAiLoggingPolicy(
            getSharedPreferences("aura_prefs", MODE_PRIVATE)
        )
        val repository = AuraRepository(database.dao(), loggingPolicy)

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundDark
                ) {
                    AuraAppContent(repository)
                }
            }
        }
    }
}

@Composable
fun AuraAppContent(repository: AuraRepository) {
    val vm: AuraViewModel = viewModel(
        factory = AuraViewModelFactory(repository)
    )

    val currentMessages by vm.chatMessages.collectAsStateWithLifecycle()
    val activeProfile by vm.userProfile.collectAsStateWithLifecycle()
    val interceptedNotifications by vm.notifications.collectAsStateWithLifecycle()
    val localMails by vm.emails.collectAsStateWithLifecycle()
    val secureFiles by vm.secureFiles.collectAsStateWithLifecycle()
    val socialPosts by vm.socialPosts.collectAsStateWithLifecycle()
    val highThinking by vm.highThinkingEnabled.collectAsStateWithLifecycle()
    val chatPending by vm.isChatPending.collectAsStateWithLifecycle()
    val processingNotificationId by vm.processingNotificationId.collectAsStateWithLifecycle()
    val processingEmailId by vm.processingEmailId.collectAsStateWithLifecycle()
    val processingSocialPostId by vm.processingSocialPostId.collectAsStateWithLifecycle()
    val isVoiceActive by vm.isVoiceActive.collectAsStateWithLifecycle()
    val voiceOutputText by vm.voiceOutputText.collectAsStateWithLifecycle()
    val isKannaOverlayVisible by vm.isKannaOverlayVisible.collectAsStateWithLifecycle()

    val currentSuggestions by vm.currentSuggestions.collectAsStateWithLifecycle()
    val isSuggestionsLoading by vm.isSuggestionsLoading.collectAsStateWithLifecycle()
    val prioritizedDigest by vm.prioritizedDigest.collectAsStateWithLifecycle()
    val isDigestLoading by vm.isDigestLoading.collectAsStateWithLifecycle()
    val recentReplies by vm.recentReplies.collectAsStateWithLifecycle()

    val isPrivacyModeActive by vm.isPrivacyModeActive.collectAsStateWithLifecycle()
    val quietHoursEnabled by vm.quietHoursEnabled.collectAsStateWithLifecycle()
    val quietHoursStart by vm.quietHoursStart.collectAsStateWithLifecycle()
    val quietHoursEnd by vm.quietHoursEnd.collectAsStateWithLifecycle()
    val batteryLevel by vm.batteryLevel.collectAsStateWithLifecycle()
    val isLowBatteryModeActive by vm.isLowBatteryModeActive.collectAsStateWithLifecycle()
    val isPrivacyModeEffectivelyActive by vm.isPrivacyModeEffectivelyActive.collectAsStateWithLifecycle()

    val isIncomingCallActive by vm.isIncomingCallActive.collectAsStateWithLifecycle()
    val isCallActive by vm.isCallActive.collectAsStateWithLifecycle()
    val callerName by vm.callerName.collectAsStateWithLifecycle()
    val callerStatus by vm.callerStatus.collectAsStateWithLifecycle()
    val callTranscripts by vm.callTranscripts.collectAsStateWithLifecycle()
    val callSummary by vm.callSummary.collectAsStateWithLifecycle()

    val excludedPackages by vm.excludedPackages.collectAsStateWithLifecycle()
    val customWakeWordState by vm.customWakeWord.collectAsStateWithLifecycle()
    val isWakeWordActiveState by vm.isWakeWordEnabled.collectAsStateWithLifecycle()
    val wakeStatus by vm.wakeStatus.collectAsStateWithLifecycle()

    val autoAnswerEnabled by vm.autoAnswerEnabled.collectAsStateWithLifecycle()
    val customGreetingScript by vm.customGreetingScript.collectAsStateWithLifecycle()
    val callerFilteringEnabled by vm.callerFilteringEnabled.collectAsStateWithLifecycle()
    val allowedCallersList by vm.allowedCallersList.collectAsStateWithLifecycle()
    val blockedCallersList by vm.blockedCallersList.collectAsStateWithLifecycle()
    val assistantName by vm.assistantName.collectAsStateWithLifecycle()
    val selectedVoiceProfile by vm.selectedVoiceProfile.collectAsStateWithLifecycle()
    val callSessions by vm.callSessions.collectAsStateWithLifecycle()
    val socialTrendResult by vm.socialTrendResult.collectAsStateWithLifecycle()
    val linkedinDraftPost by vm.linkedinDraftPost.collectAsStateWithLifecycle()
    val isSocialProcessing by vm.isSocialProcessing.collectAsStateWithLifecycle()
    val meetingAnswerResult by vm.meetingAnswerResult.collectAsStateWithLifecycle()
    val isMeetingProcessing by vm.isMeetingProcessing.collectAsStateWithLifecycle()
    val isRepresentedInMeeting by vm.isRepresentedInMeeting.collectAsStateWithLifecycle()
    val activeMeetingTranscript by vm.activeMeetingTranscript.collectAsStateWithLifecycle()

    val calendarEvents by vm.calendarEvents.collectAsStateWithLifecycle()
    val isRecordingActive by vm.isRecordingActive.collectAsStateWithLifecycle()
    val joiningEventId by vm.joiningEventId.collectAsStateWithLifecycle()
    val joiningEventTitle by vm.joiningEventTitle.collectAsStateWithLifecycle()
    val isScrapingInProgress by vm.isScrapingInProgress.collectAsStateWithLifecycle()
    val generatedLinkedInVisualPrompt by vm.generatedLinkedInVisualPrompt.collectAsStateWithLifecycle()
    val linkedinVisualImageReady by vm.linkedinVisualImageReady.collectAsStateWithLifecycle()
    val auraTasks by vm.auraTasks.collectAsStateWithLifecycle()
    val professionalTone by vm.professionalTone.collectAsStateWithLifecycle()
    val callScreeningRules by vm.callScreeningRules.collectAsStateWithLifecycle()
    val emailTemplates by vm.emailTemplates.collectAsStateWithLifecycle()
    val isOnDeviceProcessingEnabled by vm.isOnDeviceProcessingEnabled.collectAsStateWithLifecycle()

    val geminiStatus by AuraDiagnostics.geminiStatus.collectAsStateWithLifecycle()
    val localStorageStatus by AuraDiagnostics.localStorageStatus.collectAsStateWithLifecycle()
    val lastErrorMessage by AuraDiagnostics.lastErrorMessage.collectAsStateWithLifecycle()
    val diagnosticLogs by AuraDiagnostics.logs.collectAsStateWithLifecycle()
    val customApiKey by vm.customApiKey.collectAsStateWithLifecycle()
    val customEndpoint by vm.customEndpoint.collectAsStateWithLifecycle()
    val customModelOverride by vm.customModelOverride.collectAsStateWithLifecycle()
    val customTimeoutSeconds by vm.customTimeoutSeconds.collectAsStateWithLifecycle()
    val backoffStrategy by vm.backoffStrategy.collectAsStateWithLifecycle()

    val autoModelUpdatesEnabled by vm.autoModelUpdatesEnabled.collectAsStateWithLifecycle()
    val privacyFirstMode by vm.privacyFirstMode.collectAsStateWithLifecycle()
    val dndSyncEnabled by vm.dndSyncEnabled.collectAsStateWithLifecycle()
    val isSystemDndActive by vm.isSystemDndActive.collectAsStateWithLifecycle()
    val cryptoPassphrase by vm.cryptoPassphrase.collectAsStateWithLifecycle()
    val isEncryptionActive by vm.isEncryptionActive.collectAsStateWithLifecycle()
    val privacyInsights by vm.privacyInsights.collectAsStateWithLifecycle()

    val appPasscode by vm.appPasscode.collectAsStateWithLifecycle()
    val isPasscodeLockEnabled by vm.isPasscodeLockEnabled.collectAsStateWithLifecycle()
    val isAppUnlocked by vm.isAppUnlocked.collectAsStateWithLifecycle()
    val autoDeleteDays by vm.autoDeleteDays.collectAsStateWithLifecycle()

    val isDeepWorkActive by vm.isDeepWorkActive.collectAsStateWithLifecycle()
    val deepWorkTimeRemaining by vm.deepWorkTimeRemaining.collectAsStateWithLifecycle()
    val deepWorkDurationMinutes by vm.deepWorkDurationMinutes.collectAsStateWithLifecycle()
    val contacts by vm.contacts.collectAsStateWithLifecycle()
    val screenedTranscripts by vm.screenedTranscripts.collectAsStateWithLifecycle()
    val versionInstallations by vm.versionInstallations.collectAsStateWithLifecycle()
    val aiActionHistory by vm.aiActionHistory.collectAsStateWithLifecycle()
    val filteredAiActionHistory by vm.filteredAiActionHistory.collectAsStateWithLifecycle()
    val aiHistorySearchQuery by vm.aiHistorySearchQuery.collectAsStateWithLifecycle()
    val aiHistoryCategoryFilter by vm.aiHistoryCategoryFilter.collectAsStateWithLifecycle()
    val aiLoggingEnabled by vm.aiLoggingEnabled.collectAsStateWithLifecycle()
    val aiLoggingCategoryEnabled by vm.aiLoggingCategoryEnabled.collectAsStateWithLifecycle()
    val currentVersion by vm.currentVersion.collectAsStateWithLifecycle()

    val availableVersion by vm.availableVersion.collectAsStateWithLifecycle()
    val isUpdateAvailable by vm.isUpdateAvailable.collectAsStateWithLifecycle()
    val isCheckingForUpdates by vm.isCheckingForUpdates.collectAsStateWithLifecycle()
    val updateStatusMessage by vm.updateStatusMessage.collectAsStateWithLifecycle()

    val researchModeEnabled by vm.researchModeEnabled.collectAsStateWithLifecycle()
    val powerSaverEnabled by vm.powerSaverEnabled.collectAsStateWithLifecycle()
    val customVoiceToneGenerated by vm.customVoiceToneGenerated.collectAsStateWithLifecycle()
    val notificationFrequency24h by vm.notificationFrequency24h.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var activeTab by remember { mutableStateOf(0) } // 0: Command Centre, 1: Interceptor Feed, 2: Postmaster Mail Hub, 3: Kanna AI Cabin
    var showProfileConfig by remember { mutableStateOf(false) }
    var settingsActiveSubTab by remember { mutableStateOf(0) }
    var showUpdateToast by remember { mutableStateOf(true) }
    var isPermissionActive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.initPreferencesAndAudio(context)
        vm.refreshSystemDndActiveState(context)
    }

    LaunchedEffect(isUpdateAvailable) {
        if (isUpdateAvailable) {
            showUpdateToast = true
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                vm.lockApp()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        var activeToast: android.widget.Toast? = null
        var lastToastTime = 0L
        var lastMsg = ""
        AuraDiagnostics.errorEvents.collect { errorMsg ->
            val now = System.currentTimeMillis()
            // Avoid showing duplicate error messages inside 2.5 seconds
            if (errorMsg != lastMsg || (now - lastToastTime) > 2500) {
                lastMsg = errorMsg
                lastToastTime = now
                try {
                    activeToast?.cancel()
                } catch (e: Exception) {
                    // Ignore
                }
                val newToast = android.widget.Toast.makeText(
                    context,
                    "API Request Failed: $errorMsg",
                    android.widget.Toast.LENGTH_LONG
                )
                activeToast = newToast
                newToast.show()
            }
            // Automatically expand the diagnostic/configuration panel
            showProfileConfig = true
        }
    }

    // Continuous Speech Wake Word Detection Engine Integration
    var isMicGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    var isHotwordEngineActive by remember { mutableStateOf(false) }

    val currentWakeWord = activeProfile?.wakeWord ?: "Aura"

    val wakeWordEngine = remember(context) {
        AuraWakeWordEngine(
            context = context,
            wakeWord = currentWakeWord,
            onWakeWordDetected = { remaining ->
                android.util.Log.d("AuraContent", "Wake Word matched: '$remaining'")
                
                // Bring MainActivity to the front instantly (overlaying lockscreen using our onCreate window flags)
                try {
                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    }
                    context.startActivity(launchIntent)
                } catch (e: Exception) {
                    android.util.Log.e("AuraContent", "Failed bringing aura to focus", e)
                }

                // Process continuous vocal command query
                vm.processVoiceInput(remaining.ifBlank { "compose localized LinkedIn response" })
            },
            onStateChanged = { running ->
                isHotwordEngineActive = running
            }
        )
    }

    // React to wake word target modification in database profile configuration
    LaunchedEffect(currentWakeWord) {
        wakeWordEngine.updateWakeWord(currentWakeWord)
    }

    // Auto-clean on composition disposal
    DisposableEffect(wakeWordEngine) {
        onDispose {
            wakeWordEngine.stop()
        }
    }

    val micPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            isMicGranted = granted
            if (granted) {
                wakeWordEngine.start()
            }
        }
    )

    val calendarPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                vm.syncLocalCalendarEvents(context)
                android.widget.Toast.makeText(context, "Calendar integration configured successfully.", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                android.widget.Toast.makeText(context, "Calendar permission denied. Priority-filtering will rely on static presets.", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Periodically sync permission check and sync calendar during active composition
    LaunchedEffect(Unit) {
        val cn = ComponentName(context, AuraNotificationService::class.java)
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        isPermissionActive = flat != null && flat.contains(cn.flattenToString())
        
        val hasCalPerm = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CALENDAR
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasCalPerm) {
            vm.syncLocalCalendarEvents(context)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (isPasscodeLockEnabled && !isAppUnlocked) {
            LockScreenOverlay(
                onUnlockAttempt = { pin ->
                    vm.unlockApp(pin)
                }
            )
        } else {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = SurfaceDark,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Command Centre") },
                    label = { Text("Command", fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryAccent,
                        selectedTextColor = PrimaryAccent,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = BorderGrey
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Interceptor Feed") },
                    label = { Text("Interceptor", fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SecondaryAccent,
                        selectedTextColor = SecondaryAccent,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = BorderGrey
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.MailOutline, contentDescription = "Postmaster Hub") },
                    label = { Text("Mail Hub", fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryAccent,
                        selectedTextColor = PrimaryAccent,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = BorderGrey
                    )
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { activeTab = 3 },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Kanna AI") },
                    label = { Text("Kanna AI", fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = SecondaryAccent,
                        selectedTextColor = SecondaryAccent,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted,
                        indicatorColor = BorderGrey
                    )
                )
            }
        },
        containerColor = BackgroundDark,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // High-Tech Unified Header
            HeaderBlock(
                highThinking = highThinking,
                profile = activeProfile ?: UserProfileEntity(),
                geminiStatus = geminiStatus,
                localStorageStatus = localStorageStatus,
                isUpdateAvailable = isUpdateAvailable,
                onLockClick = { settingsActiveSubTab = 0; showProfileConfig = !showProfileConfig },
                onToggleHighThinking = { vm.toggleHighThinking(it) }
            )

            HorizontalDivider(color = BorderGrey, thickness = 1.dp)

            if (showProfileConfig) {
                ProfileConfigurationPanel(
                    initialSubTab = settingsActiveSubTab,
                    profile = activeProfile ?: UserProfileEntity(),
                    customKey = customApiKey,
                    customEndpoint = customEndpoint,
                    customModel = customModelOverride,
                    customTimeout = customTimeoutSeconds,
                    customBackoff = backoffStrategy,
                    geminiStatus = geminiStatus,
                    localStorageStatus = localStorageStatus,
                    logs = diagnosticLogs,
                    autoModelUpdatesEnabled = autoModelUpdatesEnabled,
                    privacyFirstMode = privacyFirstMode,
                    cryptoPassphrase = cryptoPassphrase,
                    isEncryptionActive = isEncryptionActive,
                    privacyInsights = privacyInsights,
                    appPasscode = appPasscode,
                    isPasscodeLockEnabled = isPasscodeLockEnabled,
                    autoDeleteDays = autoDeleteDays,
                    currentVersion = currentVersion,
                    isUpdateAvailable = isUpdateAvailable,
                    availableVersion = availableVersion,
                    isCheckingForUpdates = isCheckingForUpdates,
                    updateStatusMessage = updateStatusMessage,
                    versionInstallations = versionInstallations,
                    onCheckForUpdates = { vm.checkForUpdatesManual() },
                    onApplyUpdateNow = { vm.applyUpdateNow() },
                    onClose = { showProfileConfig = false },
                    onSaveProfile = { name, email, autoReply, secLevel, wakeWord, lockscreen ->
                        vm.saveProfile(name, email, autoReply, secLevel, wakeWord, lockscreen)
                    },
                    onSaveApiSettings = { key, endPt, modelSym, timeoutSecs, backoffStrat ->
                        vm.updateCustomApiSettings(key, endPt, modelSym, timeoutSecs, backoffStrat)
                    },
                    onResetApiSettings = {
                        vm.resetApiSettingsToDefaults()
                    },
                    onTestDiagnostics = {
                        coroutineScope.launch {
                            val success = vm.testDiagnosticsHandshakeDirect()
                            if (success) {
                                android.widget.Toast.makeText(context, "Handshake successful! Gemini API is responsive.", android.widget.Toast.LENGTH_LONG).show()
                            } else {
                                android.widget.Toast.makeText(context, "Handshake failed. Check key, gateway endpoint, or connection parameters.", android.widget.Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    onClearLogs = {
                        AuraDiagnostics.clear()
                    },
                    onToggleAutoModelUpdates = { vm.updateAutoModelUpdatesEnabled(it) },
                    onTogglePrivacyFirstMode = { vm.updatePrivacyFirstMode(it) },
                    onUpdateEncryptionSettings = { active, phrase -> vm.updateEncryptionAndPassphrase(active, phrase) },
                    onClearPrivacyInsights = { vm.clearPrivacyInsightsList() },
                    onManualTrigger = { vm.runManualDiagnosticsAndPolling() },
                    onUpdatePasscodeSettings = { pin, active -> vm.updatePasscodeSettings(pin, active) },
                    onUpdateAutoDeleteInterval = { days -> vm.updateAutoDeleteInterval(days) },
                    onClearAllLocalData = { vm.clearAllLocalDataSecurely() },
                    callScreeningRules = callScreeningRules,
                    emailTemplates = emailTemplates,
                    isOnDeviceProcessingEnabled = isOnDeviceProcessingEnabled,
                    onAddCallScreeningRule = { pattern, action, desc -> vm.addCallScreeningRule(pattern, action, desc) },
                    onDeleteCallScreeningRule = { id -> vm.deleteCallScreeningRule(id) },
                    onAddEmailTemplate = { name, content, category -> vm.addEmailTemplate(name, content, category) },
                    onDeleteEmailTemplate = { id -> vm.deleteEmailTemplate(id) },
                    onToggleOnDeviceProcessing = { enabled -> vm.updateOnDeviceProcessing(enabled) },
                    onSyncCalendar = {
                        val hasCalPerm = androidx.core.content.ContextCompat.checkSelfPermission(
                            context,
                            android.Manifest.permission.READ_CALENDAR
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        if (hasCalPerm) {
                            vm.syncLocalCalendarEvents(context)
                        } else {
                            calendarPermissionLauncher.launch(android.Manifest.permission.READ_CALENDAR)
                        }
                    },
                    dndSyncEnabled = dndSyncEnabled,
                    onToggleDndSync = { vm.updateDndSyncEnabled(context, it) },
                    isSystemDndActive = isSystemDndActive,
                    isDeepWorkActive = isDeepWorkActive,
                    deepWorkTimeRemaining = deepWorkTimeRemaining,
                    deepWorkDurationMinutes = deepWorkDurationMinutes,
                    onToggleDeepWorkMode = { vm.toggleDeepWorkMode() },
                    onUpdateDeepWorkDuration = { mins -> vm.updateDeepWorkDuration(mins) },
                    contacts = contacts,
                    onAddContact = { name, phone, category, tone, priority -> vm.addContact(name, phone, category, tone, priority) },
                    onDeleteContact = { id -> vm.deleteContact(id) },
                    screenedTranscripts = screenedTranscripts,
                    onDeleteScreenedTranscript = { id -> vm.deleteScreenedTranscript(id) },
                    onClearScreenedTranscripts = { vm.clearScreenedTranscripts() },
                    getDecryptedText = { txt, enc -> vm.getDecryptedText(txt, enc) },
                    onExportPdfReport = {
                        vm.exportWeeklyReportAsPdf(context) { file ->
                            if (file != null) {
                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                    try {
                                        val uri = androidx.core.content.FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            file
                                        )
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/pdf"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Secure PDF Report"))
                                    } catch (e: java.lang.Exception) {
                                        android.widget.Toast.makeText(context, "Error sharing PDF: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.Main) {
                                    android.widget.Toast.makeText(context, "Failed to compile secure report.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    researchModeEnabled = researchModeEnabled,
                    onToggleResearchMode = { vm.toggleResearchMode(it) },
                    notificationFrequency24h = notificationFrequency24h,
                    powerSaverEnabled = powerSaverEnabled,
                    onTogglePowerSaver = { vm.togglePowerSaver(it) },
                    onPreviewVoiceProfile = { vm.previewVoiceProfile(it) },
                    selectedVoiceProfile = selectedVoiceProfile,
                    onSelectVoiceProfile = { vm.selectVoiceProfile(it) },
                    aiActionHistory = aiActionHistory,
                    onDeleteAiAction = { vm.deleteAiAction(it) },
                    onClearAiActionHistory = { vm.clearAiActionHistory() },
                    aiLoggingEnabled = aiLoggingEnabled,
                    aiLoggingCategoryEnabled = aiLoggingCategoryEnabled,
                    onToggleAiLogging = { vm.updateAiLoggingEnabled(it) },
                    onToggleAiLoggingCategory = { cat, enabled -> vm.updateAiLoggingCategoryEnabled(cat, enabled) },
                    filteredAiActionHistory = filteredAiActionHistory,
                    aiHistorySearchQuery = aiHistorySearchQuery,
                    aiHistoryCategoryFilter = aiHistoryCategoryFilter,
                    onAiHistorySearchQueryChange = { vm.updateAiHistorySearchQuery(it) },
                    onAiHistoryCategoryFilterChange = { vm.updateAiHistoryCategoryFilter(it) },
                    onExportAiHistory = { uri, format -> vm.exportAiActionHistory(uri, format, context.contentResolver) }
                )
                HorizontalDivider(color = BorderGrey, thickness = 1.dp)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (activeTab) {
                    0 -> CommandCentreScreen(
                        messages = currentMessages,
                        pending = chatPending,
                        highThinking = highThinking,
                        onSendMessage = { vm.sendMessage(it) },
                        onClearChat = { vm.clearChat() },
                        geminiStatus = geminiStatus,
                        lastErrorMessage = lastErrorMessage,
                        onRetryConnection = { vm.runDiagnosticConnectivityChecks() }
                    )
                    1 -> InterceptorScreen(
                        notifications = interceptedNotifications,
                        processingId = processingNotificationId,
                        isPermissionActive = isPermissionActive,
                        onGrantPermission = {
                            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                            context.startActivity(intent)
                        },
                        onAnalyze = { vm.analyzeNotification(it) },
                        onSendReply = { item, txt -> vm.sendNotificationReply(item, txt) },
                        onDelete = { vm.deleteNotificationItem(it) },
                        onSimulate = { app, title, text -> vm.simulateNotificationDrop(app, title, text) },
                        callSessions = callSessions
                    )
                    2 -> PostmasterMailScreen(
                        emails = localMails,
                        processingId = processingEmailId,
                        onAnalyze = { vm.analyzeEmail(it) },
                        onSendReply = { email, txt -> vm.dispatchEmailResponse(email, txt) },
                        onDelete = { vm.deleteEmailItem(it) },
                        onSimulate = { sender, subtitle, body -> vm.simulateEmailIncoming(sender, subtitle, body) },
                        emailTemplates = emailTemplates
                    )
                    3 -> KannaAiCabinScreen(
                        profile = activeProfile ?: UserProfileEntity(),
                        secureFiles = secureFiles,
                        socialPosts = socialPosts,
                        isVoiceActive = isVoiceActive,
                        voiceOutputText = voiceOutputText,
                        processingSocialPostId = processingSocialPostId,
                        isHotwordActive = isHotwordEngineActive,
                        isMicGranted = isMicGranted,
                        socialTrendResult = socialTrendResult,
                        linkedinDraftPost = linkedinDraftPost,
                        isSocialProcessing = isSocialProcessing,
                        meetingAnswerResult = meetingAnswerResult,
                        isMeetingProcessing = isMeetingProcessing,
                        isRepresentedInMeeting = isRepresentedInMeeting,
                        activeMeetingTranscript = activeMeetingTranscript,
                        onToggleHotword = { active ->
                            if (active) {
                                  if (isMicGranted) {
                                      wakeWordEngine.start()
                                  } else {
                                      micPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                  }
                              } else {
                                  wakeWordEngine.stop()
                              }
                        },
                        onProcessVoice = { vm.processVoiceInput(it) },
                        onCreateFile = { name, body -> vm.createSecureFile(name, body) },
                        onDeleteFile = { vm.deleteFileItem(it) },
                        onAnalyzePost = { vm.analyzeSocialMediaAction(it) },
                        onPublishPost = { post, content -> vm.publishSocialPost(post, content) },
                        onSimulatePost = { platform, author, content -> vm.createSocialPostSimulation(platform, author, content) },
                        onDeletePost = { vm.deleteSocialPostItem(it) },
                        onSimulateCall = { vm.simulateIncomingCall(it) },
                        onRunSocialAnalysis = { platform, topic -> vm.runSocialTrendAnalysis(platform, topic) },
                        onDraftLinkedIn = { vm.draftLinkedInCampaign(it) },
                        onQueryMeeting = { topic, question -> vm.queryActiveMeeting(topic, question) },
                        onToggleMeetingJoin = { vm.toggleRepresentativeMeetingJoin(it) },
                        onUpdateMeetingTranscript = { vm.updateMeetingTranscriptPreset(it) },
                        geminiStatus = geminiStatus,
                        lastErrorMessage = lastErrorMessage,
                        onRetryConnection = { vm.runDiagnosticConnectivityChecks() }
                    )
                }
            }
        }
      }
    }

    AnimatedVisibility(
        visible = isKannaOverlayVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        KannaOverlayHUD(
            lastNotification = interceptedNotifications.firstOrNull(),
            isVoiceActive = isVoiceActive,
            voiceOutputText = voiceOutputText,
            currentSuggestions = currentSuggestions,
            isSuggestionsLoading = isSuggestionsLoading,
            prioritizedDigest = prioritizedDigest,
            isDigestLoading = isDigestLoading,
            recentReplies = recentReplies,
            isPrivacyModeActive = isPrivacyModeActive,
            quietHoursEnabled = quietHoursEnabled,
            quietHoursStart = quietHoursStart,
            quietHoursEnd = quietHoursEnd,
            batteryLevel = batteryLevel,
            isLowBatteryModeActive = isLowBatteryModeActive,
            excludedPackages = excludedPackages,
            customWakeWord = customWakeWordState,
            isWakeWordEnabled = isWakeWordActiveState,
            wakeStatus = wakeStatus,
            autoAnswerEnabled = autoAnswerEnabled,
            customGreetingScript = customGreetingScript,
            callerFilteringEnabled = callerFilteringEnabled,
            allowedCallersList = allowedCallersList,
            blockedCallersList = blockedCallersList,
            assistantName = assistantName,
            selectedVoiceProfile = selectedVoiceProfile,
            callSessions = callSessions,
            onDismiss = { vm.setKannaOverlayVisible(false) },
            onProcessCommand = { vm.processVoiceInput(it) },
            onOneTapReply = { text ->
                interceptedNotifications.firstOrNull()?.let { lastNotif ->
                    vm.sendNotificationReply(lastNotif, text)
                }
            },
            onUndoReply = { vm.undoRecentReply(it) },
            onRefreshDigest = { vm.generateNotificationDigest() },
            onTogglePrivacyMode = { vm.togglePrivacyMode(it) },
            onToggleQuietHours = { vm.toggleQuietHours(it) },
            onUpdateQuietHoursRange = { start, end -> vm.updateQuietHoursRange(start, end) },
            onToggleExcludedPackage = { vm.toggleExcludedPackage(it) },
            onUpdateWakeWord = { vm.updateCustomWakeWord(it) },
            onToggleWakeWord = { vm.toggleWakeWordEnabled(it) },
            onToggleAutoAnswer = { vm.toggleAutoAnswer(it) },
            onUpdateGreeting = { vm.updateCustomGreetingScript(it) },
            onToggleCallerFiltering = { vm.toggleCallerFiltering(it) },
            onUpdateAllowedCallers = { vm.updateAllowedCallers(it) },
            onUpdateBlockedCallers = { vm.updateBlockedCallers(it) },
            onUpdateAssistantName = { vm.updateAssistantName(it) },
            onSelectVoiceProfile = { vm.selectVoiceProfile(it) },
            onDeleteCallSession = { vm.deleteCallSession(it) },
            onClearCallSessions = { vm.clearCallSessions() },
            calendarEvents = calendarEvents,
            socialPosts = socialPosts,
            isRecordingActive = isRecordingActive,
            joiningEventId = joiningEventId,
            joiningEventTitle = joiningEventTitle,
            isScrapingInProgress = isScrapingInProgress,
            generatedLinkedInVisualPrompt = generatedLinkedInVisualPrompt,
            linkedinVisualImageReady = linkedinVisualImageReady,
            onSimulateCalendarEventJoin = { vm.simulateCalendarEventJoinSequence(it) },
            onAddNewCalendarEvent = { title, org -> vm.addNewCalendarEvent(title, org) },
            onDeleteCalendarEvent = { vm.deleteCalendarEvent(it) },
            onUpdateCalendarEvent = { vm.updateCalendarEvent(it) },
            onCreateSecureFile = { name, body -> vm.createSecureFile(name, body) },
            onDraftEmailForMeeting = { vm.draftEmailForMeeting(it) },
            onTriggerWebScrapeTask = { vm.triggerWebScrapeTask() },
            onApproveScrapedComment = { vm.approveScrapedComment(it) },
            onRejectScrapedComment = { vm.rejectScrapedComment(it) },
            onGenerateLinkedInPostWithGraphic = { vm.generateLinkedInPostWithGraphic(it) },
            auraTasks = auraTasks,
            onAddTask = { vm.addTask(it) },
            onUpdateTask = { vm.updateTask(it) },
            onDeleteTask = { vm.deleteTask(it) },
            professionalTone = professionalTone,
            onUpdateProfessionalTone = { vm.updateProfessionalTone(it) },
            customVoiceToneGenerated = customVoiceToneGenerated,
            onVoiceSnippetRecorded = { vm.triggerVoiceSnippedRecorded() },
            onPreviewVoiceProfile = { vm.previewVoiceProfile(it) }
        )
    }

    if (isIncomingCallActive || isCallActive) {
        AuraCallScreenHUD(
            isIncoming = isIncomingCallActive,
            isCallActive = isCallActive,
            callerName = callerName,
            callerStatus = callerStatus,
            transcripts = callTranscripts,
            onDecline = { vm.declineCall() },
            onAnswer = { vm.answerCallWithAura() },
            onSendMessage = { vm.sendCallerMessage(it) },
            onEndCall = { vm.endCallAndSummarize() }
        )
    }

    AnimatedVisibility(
        visible = isUpdateAvailable && showUpdateToast,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = 90.dp, start = 16.dp, end = 16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, PrimaryAccent),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("update_toast_component")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(PrimaryAccent)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "New Kanna AI Update Available!",
                            color = TextLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Version v$availableVersion is ready to install.",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            settingsActiveSubTab = 5
                            showProfileConfig = true
                            showUpdateToast = false
                        }
                    ) {
                        Text(
                            text = "GO TO UPDATES",
                            color = PrimaryAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    IconButton(
                        onClick = { showUpdateToast = false },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss update toast",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
}

@Composable
fun KannaOverlayHUD(
    lastNotification: NotificationEntity?,
    isVoiceActive: Boolean,
    voiceOutputText: String,
    currentSuggestions: List<String>,
    isSuggestionsLoading: Boolean,
    prioritizedDigest: String,
    isDigestLoading: Boolean,
    recentReplies: List<RecentReplyItem>,
    isPrivacyModeActive: Boolean,
    quietHoursEnabled: Boolean,
    quietHoursStart: String,
    quietHoursEnd: String,
    batteryLevel: Int,
    isLowBatteryModeActive: Boolean,
    excludedPackages: Set<String>,
    customWakeWord: String,
    isWakeWordEnabled: Boolean,
    wakeStatus: String,
    autoAnswerEnabled: Boolean,
    customGreetingScript: String,
    callerFilteringEnabled: Boolean,
    allowedCallersList: String,
    blockedCallersList: String,
    assistantName: String,
    selectedVoiceProfile: String,
    callSessions: List<CallSessionEntity>,
    calendarEvents: List<CalendarEventEntity>,
    socialPosts: List<SocialPostEntity>,
    isRecordingActive: Boolean,
    joiningEventId: Int?,
    joiningEventTitle: String,
    isScrapingInProgress: Boolean,
    generatedLinkedInVisualPrompt: String,
    linkedinVisualImageReady: Boolean,
    onDismiss: () -> Unit,
    onProcessCommand: (String) -> Unit,
    onOneTapReply: (String) -> Unit,
    onUndoReply: (RecentReplyItem) -> Unit,
    onRefreshDigest: () -> Unit,
    onTogglePrivacyMode: (Boolean) -> Unit,
    onToggleQuietHours: (Boolean) -> Unit,
    onUpdateQuietHoursRange: (String, String) -> Unit,
    onToggleExcludedPackage: (String) -> Unit,
    onUpdateWakeWord: (String) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onToggleAutoAnswer: (Boolean) -> Unit,
    onUpdateGreeting: (String) -> Unit,
    onToggleCallerFiltering: (Boolean) -> Unit,
    onUpdateAllowedCallers: (String) -> Unit,
    onUpdateBlockedCallers: (String) -> Unit,
    onUpdateAssistantName: (String) -> Unit,
    onSelectVoiceProfile: (String) -> Unit,
    onDeleteCallSession: (Int) -> Unit,
    onClearCallSessions: () -> Unit,
    onSimulateCalendarEventJoin: (Int) -> Unit,
    onAddNewCalendarEvent: (String, String) -> Unit,
    onDeleteCalendarEvent: (Int) -> Unit,
    onUpdateCalendarEvent: (com.example.data.db.CalendarEventEntity) -> Unit = {},
    onCreateSecureFile: (String, String) -> Unit = { _, _ -> },
    onDraftEmailForMeeting: (com.example.data.db.CalendarEventEntity) -> Unit = {},
    onTriggerWebScrapeTask: () -> Unit,
    onApproveScrapedComment: (Int) -> Unit,
    onRejectScrapedComment: (Int) -> Unit,
    onGenerateLinkedInPostWithGraphic: (String) -> Unit,
    auraTasks: List<AuraTaskEntity>,
    onAddTask: (AuraTaskEntity) -> Unit,
    onUpdateTask: (AuraTaskEntity) -> Unit,
    onDeleteTask: (Int) -> Unit,
    professionalTone: String,
    onUpdateProfessionalTone: (String) -> Unit,
    customVoiceToneGenerated: Boolean = false,
    onVoiceSnippetRecorded: () -> Unit = {},
    onPreviewVoiceProfile: (String) -> Unit = {}
) {
    var rawInputText by remember { mutableStateOf("") }
    var hudTabSelected by remember { mutableStateOf(0) } // 0: ALERTS, 1: SECURE MEETINGS, 2: SOCIAL AUDITING
    var editingSummaryEventId by remember { mutableStateOf<Int?>(null) }
    var editingSummaryText by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val dynBgColor = if (isSystemDark) Color(0xE6030712) else Color(0xE6F9FAFB)
    val dynTextLight = if (isSystemDark) TextLight else Color(0xFF111827)
    val dynTextMuted = if (isSystemDark) TextMuted else Color(0xFF4B5563)
    val dynSurfaceDark = if (isSystemDark) SurfaceDark else Color(0xFFF3F4F6)
    val dynBorderGrey = if (isSystemDark) BorderGrey else Color(0xFFD1D5DB)
    val dynPrimaryAccent = if (isSystemDark) PrimaryAccent else Color(0xFF6366F1)
    val dynSecondaryAccent = if (isSystemDark) SecondaryAccent else Color(0xFF8B5CF6)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(dynBgColor) // Dynamic translucent mode theme background
            .clickable(enabled = false) { /* prevent pass through clicks */ }
            .padding(16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Glowing cyan/purple background aura decoration
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            // draw soft radial gradient to mimic glowing Siri orbs
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(dynSecondaryAccent.copy(alpha = 0.15f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(width / 2f, height * 0.8f),
                    radius = width * 0.6f
                ),
                radius = width * 0.6f,
                center = androidx.compose.ui.geometry.Offset(width / 2f, height * 0.8f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(dynPrimaryAccent.copy(alpha = 0.12f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(width / 2f, height * 0.9f),
                    radius = width * 0.4f
                ),
                radius = width * 0.4f,
                center = androidx.compose.ui.geometry.Offset(width / 2f, height * 0.9f)
            )
        }

        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxWidth()
                .navigationBarsPadding() // Keep clear of system gesture bars
                .padding(bottom = 12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Dismiss button at the top right of overlay
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .background(BorderGrey, CircleShape)
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Overlay",
                        tint = TextLight,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Pulse wave canvas signifying live listener like Siri/Google
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .border(BorderStroke(1.dp, SecondaryAccent), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Glow circles
                val infiniteTransition = rememberInfiniteTransition()
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.8f,
                    targetValue = 1.2f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = SecondaryAccent.copy(alpha = 0.25f),
                        radius = (size.width / 2f) * pulseScale
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.Refresh, // Standard available icon representing processing state
                    contentDescription = "Aura Glowing Core",
                    tint = SecondaryAccent,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "AURA INTEL OVERLAY",
                color = PrimaryAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )

            Text(
                text = "PERSISTENT LOCKSCREEN SECURE CONSOLE",
                color = TextMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // SLICK HUD TAB BAR CONTROL Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(dynSurfaceDark)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("ALERTS & FILTER", "SECURE MEETINGS", "SOCIAL BULLETINS").forEachIndexed { index, label ->
                    val isTabSelected = hudTabSelected == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isTabSelected) dynSecondaryAccent else Color.Transparent)
                            .clickable { hudTabSelected = index }
                            .padding(vertical = 10.dp)
                            .testTag("hud_tab_btn_$index"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isTabSelected) BackgroundDark else dynTextLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (hudTabSelected == 0) {
                // Display latest intercepted message block
                if (lastNotification != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderGrey)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(AlertOrange)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LATEST INTERCEPTED MESSAGE",
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = lastNotification.title,
                            color = TextLight,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = lastNotification.text,
                            color = TextMuted,
                            fontSize = 11.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        if (lastNotification.replyDraft.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = BorderGrey, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Aura Drafted Quick Reply:",
                                color = SecondaryAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = lastNotification.replyDraft,
                                color = TextLight,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Text(
                                text = "Status: ${lastNotification.status}",
                                color = if (lastNotification.status == "DEALT") SecondaryAccent else AlertOrange,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.9f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderGrey)
                ) {
                    Box(
                        modifier = Modifier.padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No messages intercepted yet. Send one to test live lockscreen reply updates!",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 1. Suggested Context-Aware One-tap Quick Replies Section
            if (lastNotification != null && lastNotification.status != "DEALT") {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "AURA ONE-TAP SUGGESTIONS",
                    color = SecondaryAccent,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 12.dp)
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                if (isSuggestionsLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = SecondaryAccent,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Synthesizing dynamic options...",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                } else if (currentSuggestions.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currentSuggestions.forEach { suggestion ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(SurfaceDark)
                                    .border(BorderStroke(1.dp, SecondaryAccent.copy(alpha = 0.8f)), RoundedCornerShape(16.dp))
                                    .clickable { onOneTapReply(suggestion) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                                    .testTag("one_tap_${suggestion.replace(" ", "_").lowercase()}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = "One-tap send",
                                        tint = SecondaryAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = suggestion,
                                        color = TextLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Prioritized aggregated briefing digest section
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Digest icon",
                                tint = PrimaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AURA PRIORITY BULLET BRIEFING",
                                color = PrimaryAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        
                        IconButton(
                            onClick = onRefreshDigest,
                            modifier = Modifier.size(24.dp)
                        ) {
                            if (isDigestLoading) {
                                CircularProgressIndicator(color = PrimaryAccent, strokeWidth = 1.5.dp, modifier = Modifier.size(12.dp))
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh Digest",
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (prioritizedDigest.isNotBlank()) {
                        Text(
                            text = prioritizedDigest,
                            color = TextLight,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 16.sp,
                            modifier = Modifier.heightIn(max = 140.dp)
                        )
                    } else {
                        Text(
                            text = "No notification digest aggregated for this period yet. Tap the refresh icon to build a combined, prioritized summary.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // 2.5 AURA INTELLIGENT SEGREGATION & PRIVACY SHIELDS CARD
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.95f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Title section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Shield Guard",
                            tint = SecondaryAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AURA PRIVACY & INTERCEPT FILTERS",
                            color = SecondaryAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 1. Privacy Shield Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isPrivacyModeActive) Color(0x33991b1b) else Color(0x11FFFFFF))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPrivacyModeActive) "SECURE PRIVACY SHIELD ON" else "PRIVACY SHIELD OFF",
                                color = if (isPrivacyModeActive) Color(0xFFf87171) else TextLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (isPrivacyModeActive) "AI Interception & summary features paused safely" else "AI is processing & summarizing messages",
                                color = TextMuted,
                                fontSize = 9.sp,
                                lineHeight = 12.sp
                            )
                        }
                        Switch(
                            checked = isPrivacyModeActive,
                            onCheckedChange = { onTogglePrivacyMode(it) },
                            modifier = Modifier.scale(0.8f).testTag("privacy_mode_toggle"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFFf87171),
                                checkedTrackColor = Color(0xFF991b1b)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 2. Custom Wake Word controls
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x11FFFFFF))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "HANDS-FREE WAKE WORD",
                                    color = TextLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Speak wake phrase to open assistant",
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }
                            
                            val micLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                                contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
                                onResult = { granted ->
                                    if (granted) {
                                        onToggleWakeWord(true)
                                    }
                                }
                            )
                            val context = LocalContext.current

                            Switch(
                                checked = isWakeWordEnabled,
                                onCheckedChange = { enabled ->
                                    if (enabled) {
                                        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                                            context,
                                            android.Manifest.permission.RECORD_AUDIO
                                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                        if (hasPermission) {
                                            onToggleWakeWord(true)
                                        } else {
                                            micLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                        }
                                    } else {
                                        onToggleWakeWord(false)
                                    }
                                },
                                modifier = Modifier.scale(0.8f).testTag("wake_word_toggle")
                            )
                        }

                        if (isWakeWordEnabled) {
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Audio listener status banner
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0x2200E5FF))
                                    .padding(vertical = 4.dp, horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryAccent)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = wakeStatus,
                                    color = PrimaryAccent,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Editable wake word box
                            var currentTextVal by remember(customWakeWord) { mutableStateOf(customWakeWord) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = currentTextVal,
                                    onValueChange = { currentTextVal = it },
                                    label = { Text("Wake Phrase", fontSize = 9.sp) },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = TextLight),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SecondaryAccent,
                                        unfocusedBorderColor = BorderGrey,
                                        focusedContainerColor = BackgroundDark,
                                        unfocusedContainerColor = BackgroundDark
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                    onClick = { onUpdateWakeWord(currentTextVal) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                    modifier = Modifier.height(36.dp).align(Alignment.CenterVertically).testTag("apply_wake_word_btn")
                                ) {
                                    Text("APPLY", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BackgroundDark, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 3. App Interception filter list
                    Text(
                        text = "CHANNELS TO AI RECEPTACLE",
                        color = TextLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Excluding channel stops its notifications & summary triggers",
                        color = TextMuted,
                        fontSize = 8.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    val appList = listOf(
                        "com.whatsapp" to "WhatsApp",
                        "com.facebook.orca" to "Messenger",
                        "com.slack" to "Slack",
                        "com.google.android.apps.messaging" to "Google Messages",
                        "com.android.email" to "Email Client"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        appList.forEach { (pkg, label) ->
                            val isIntercepted = !excludedPackages.contains(pkg)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isIntercepted) SecondaryAccent.copy(alpha = 0.25f) else Color(0x11FFFFFF))
                                    .border(
                                        BorderStroke(
                                            1.dp,
                                            if (isIntercepted) SecondaryAccent else BorderGrey
                                        ),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onToggleExcludedPackage(pkg) }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                    .testTag("exclude_toggle_$pkg")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isIntercepted) Icons.Default.Check else Icons.Default.Clear,
                                        contentDescription = if (isIntercepted) "Intercepting" else "Excluded",
                                        tint = if (isIntercepted) SecondaryAccent else TextMuted,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = label,
                                        color = if (isIntercepted) TextLight else TextMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(color = dynBorderGrey.copy(alpha = 0.5f))

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quiet Hours Scheduler settings block
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x0CFFFF00))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "QUIET HOURS SCHEDULE",
                                    color = dynTextLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Auto-toggle privacy during select hours",
                                    color = dynTextMuted,
                                    fontSize = 9.sp
                                )
                            }
                            Switch(
                                checked = quietHoursEnabled,
                                onCheckedChange = { onToggleQuietHours(it) },
                                modifier = Modifier.scale(0.8f).testTag("quiet_hours_toggle")
                            )
                        }

                        if (quietHoursEnabled) {
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                var tempStart by remember(quietHoursStart) { mutableStateOf(quietHoursStart) }
                                var tempEnd by remember(quietHoursEnd) { mutableStateOf(quietHoursEnd) }

                                OutlinedTextField(
                                    value = tempStart,
                                    onValueChange = { tempStart = it },
                                    label = { Text("Start (HH:mm)", fontSize = 8.sp, color = dynTextMuted) },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = dynTextLight),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = dynSecondaryAccent,
                                        unfocusedBorderColor = dynBorderGrey
                                    )
                                )

                                OutlinedTextField(
                                    value = tempEnd,
                                    onValueChange = { tempEnd = it },
                                    label = { Text("End (HH:mm)", fontSize = 8.sp, color = dynTextMuted) },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = dynTextLight),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = dynSecondaryAccent,
                                        unfocusedBorderColor = dynBorderGrey
                                    )
                                )

                                Button(
                                    onClick = { onUpdateQuietHoursRange(tempStart, tempEnd) },
                                    colors = ButtonDefaults.buttonColors(containerColor = dynSecondaryAccent),
                                    contentPadding = PaddingValues(horizontal = 6.dp),
                                    modifier = Modifier.height(36.dp).testTag("quiet_hours_range_btn")
                                ) {
                                    Text("SET", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BackgroundDark, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    // AURA SECURE AUTO-ANSWER TELEPHONY SETTINGS
                    Spacer(modifier = Modifier.height(12.dp))

                    HorizontalDivider(color = dynBorderGrey.copy(alpha = 0.5f))

                    Spacer(modifier = Modifier.height(10.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(dynSurfaceDark)
                            .border(BorderStroke(1.2.dp, dynSecondaryAccent), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Telephony Config",
                                    tint = dynSecondaryAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "AUTO-ANSWER SECURE DIGITAL CABINET",
                                        color = dynTextLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Manage automated voice screening & identity",
                                        color = dynTextMuted,
                                        fontSize = 9.sp
                                    )
                                }
                            }
                            Switch(
                                checked = autoAnswerEnabled,
                                onCheckedChange = { onToggleAutoAnswer(it) },
                                modifier = Modifier.scale(0.8f).testTag("telephony_auto_answer_toggle")
                            )
                        }

                        if (autoAnswerEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))

                            // Assistant Name Config
                            var tempName by remember(assistantName) { mutableStateOf(assistantName) }
                            OutlinedTextField(
                                value = tempName,
                                onValueChange = { tempName = it },
                                label = { Text("Assistant Voice Identity (Name)", fontSize = 8.sp, color = dynTextMuted) },
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = dynTextLight, fontFamily = FontFamily.Monospace),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(
                                        onClick = { onUpdateAssistantName(tempName) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = "Save assistant name", tint = dynSecondaryAccent, modifier = Modifier.size(14.dp))
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = dynSecondaryAccent,
                                    unfocusedBorderColor = dynBorderGrey
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Custom Greeting Voice Script
                            var tempGreeting by remember(customGreetingScript) { mutableStateOf(customGreetingScript) }
                            OutlinedTextField(
                                value = tempGreeting,
                                onValueChange = { tempGreeting = it },
                                label = { Text("TTS Voice Synthesizer Greeting Script", fontSize = 8.sp, color = dynTextMuted) },
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = dynTextLight, fontFamily = FontFamily.Monospace),
                                maxLines = 4,
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = dynSecondaryAccent,
                                    unfocusedBorderColor = dynBorderGrey
                                )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Button(
                                onClick = { onUpdateGreeting(tempGreeting) },
                                colors = ButtonDefaults.buttonColors(containerColor = dynSecondaryAccent),
                                modifier = Modifier.fillMaxWidth().height(36.dp).testTag("save_greeting_btn")
                            ) {
                                Text("COMMIT NEW VOICE GREETING", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BackgroundDark, fontFamily = FontFamily.Monospace)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = dynBorderGrey.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Caller filtering block toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "CALLER WHITE / BLACK LIST FILTER",
                                        color = dynTextLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "Filter or block incoming simulated calls",
                                        color = dynTextMuted,
                                        fontSize = 9.sp
                                    )
                                }
                                Switch(
                                    checked = callerFilteringEnabled,
                                    onCheckedChange = { onToggleCallerFiltering(it) },
                                    modifier = Modifier.scale(0.8f).testTag("caller_filtering_toggle")
                                )
                            }

                            if (callerFilteringEnabled) {
                                Spacer(modifier = Modifier.height(10.dp))

                                // Allowed callers list
                                var tempAllowed by remember(allowedCallersList) { mutableStateOf(allowedCallersList) }
                                OutlinedTextField(
                                    value = tempAllowed,
                                    onValueChange = { tempAllowed = it },
                                    label = { Text("Allowed Callers (Whitelisted, comma list)", fontSize = 8.sp, color = dynTextMuted) },
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = dynTextLight, fontFamily = FontFamily.Monospace),
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { onUpdateAllowedCallers(tempAllowed) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = "Save Allowed", tint = dynSecondaryAccent, modifier = Modifier.size(14.dp))
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = dynSecondaryAccent,
                                        unfocusedBorderColor = dynBorderGrey
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Blocked callers list
                                var tempBlocked by remember(blockedCallersList) { mutableStateOf(blockedCallersList) }
                                OutlinedTextField(
                                    value = tempBlocked,
                                    onValueChange = { tempBlocked = it },
                                    label = { Text("Blocked Callers (Blacklisted spam, comma list)", fontSize = 8.sp, color = dynTextMuted) },
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = dynTextLight, fontFamily = FontFamily.Monospace),
                                    singleLine = true,
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { onUpdateBlockedCallers(tempBlocked) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Default.Check, contentDescription = "Save Blocked", tint = dynSecondaryAccent, modifier = Modifier.size(14.dp))
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(50.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = dynSecondaryAccent,
                                        unfocusedBorderColor = dynBorderGrey
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = dynBorderGrey.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Synthesized Voice Profile Selector
                            Text(
                                text = "SYNTHESIZED VOICE PROFILE",
                                color = dynTextLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val voiceProfiles = if (customVoiceToneGenerated) {
                                listOf("Kanna Classic", "Calm Professional", "Echo Sentinel", "Stellar Voice", "My Custom Profile")
                             } else {
                                listOf("Kanna Classic", "Calm Professional", "Echo Sentinel", "Stellar Voice")
                             }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                voiceProfiles.forEach { profile ->
                                    val isSelected = (selectedVoiceProfile == profile) || (selectedVoiceProfile.isEmpty() && profile == "Kanna Classic")
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) dynSecondaryAccent.copy(alpha = 0.2f) else dynSurfaceDark)
                                            .border(
                                                BorderStroke(
                                                    if (isSelected) 1.5.dp else 1.dp,
                                                    if (isSelected) dynSecondaryAccent else dynBorderGrey.copy(alpha = 0.5f)
                                                ),
                                                RoundedCornerShape(6.dp)
                                            )
                                            .clickable { onSelectVoiceProfile(profile) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = profile,
                                            color = if (isSelected) dynSecondaryAccent else dynTextLight,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = { 
                                    onPreviewVoiceProfile(selectedVoiceProfile.ifEmpty { "Kanna Classic" }) 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = dynSecondaryAccent),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(34.dp)
                                    .testTag("preview_voice_profile_button")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Preview Tone",
                                        tint = BackgroundDark,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "HEAR PREVIEW: ${selectedVoiceProfile.ifEmpty { "Kanna Classic" }.uppercase()}",
                                        color = BackgroundDark,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // PERSIONALIZED VOICE SNIPPET RECORDING UNIT
                            var recordingSecondsLeft by remember { mutableStateOf(0) }
                            var isSnippetRecording by remember { mutableStateOf(false) }

                            LaunchedEffect(isSnippetRecording) {
                                if (isSnippetRecording) {
                                    recordingSecondsLeft = 4
                                    while (recordingSecondsLeft > 0) {
                                        kotlinx.coroutines.delay(1000L)
                                        recordingSecondsLeft--
                                    }
                                    isSnippetRecording = false
                                    onVoiceSnippetRecorded()
                                    onSelectVoiceProfile("My Custom Profile")
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(dynSurfaceDark)
                                    .border(1.dp, dynBorderGrey.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                                    .testTag("synthetic_tone_snippet_recorder_wrapper")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "AURA VOICE CLONENT SYNTHESIZER",
                                            color = dynTextLight,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Text(
                                            text = if (customVoiceToneGenerated) 
                                                "STATUS: PERSONALIZED VOICE INSTANCE REGISTERED" 
                                                else "STATUS: NO CUSTOM PROFILE TRAINED (STANDBY)",
                                            color = if (customVoiceToneGenerated) dynSecondaryAccent else dynTextMuted,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Medium,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }

                                    Button(
                                        onClick = { isSnippetRecording = true },
                                        enabled = !isSnippetRecording,
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSnippetRecording) Color.Red else dynSecondaryAccent
                                        ),
                                        modifier = Modifier.testTag("record_voice_snippet_button"),
                                        shape = RoundedCornerShape(4.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isSnippetRecording) "REC: ${recordingSecondsLeft}S" else "TRAIN VOICE",
                                            color = if (isSnippetRecording) Color.White else BackgroundDark,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                if (isSnippetRecording) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Please read out loud: \n\"Configure Aura custom voice profiling, analyze intercepted notification logs to compose automatic context-aware answers.\"",
                                        color = dynSecondaryAccent,
                                        fontSize = 10.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(BackgroundDark.copy(alpha = 0.4f))
                                            .padding(8.dp)
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Simulating moving audio waveform
                                    Row(
                                        modifier = Modifier.fillMaxWidth().height(16.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        repeat(16) { idx ->
                                            val currentBarHeight = remember(recordingSecondsLeft) { (10..30).random() }
                                            Box(
                                                modifier = Modifier
                                                    .width(3.dp)
                                                    .height(currentBarHeight.dp)
                                                    .background(dynSecondaryAccent, RoundedCornerShape(1.dp))
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Personality Tone Slider
                            Text(
                                text = "KANNA PERSONALITY TONE",
                                color = dynTextLight,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Adjusts Kanna's tone for social media comments and email responses: $professionalTone",
                                color = dynTextMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            var toneSliderPosition by remember(professionalTone) {
                                mutableStateOf(
                                    when (professionalTone) {
                                        "Formal" -> 0f
                                        "Casual" -> 1f
                                        else -> 2f
                                    }
                                )
                            }
                            
                            Slider(
                                value = toneSliderPosition,
                                onValueChange = { pos ->
                                    toneSliderPosition = pos
                                    val index = Math.round(pos)
                                    val newTone = when (index) {
                                        0 -> "Formal"
                                        1 -> "Casual"
                                        else -> "Enthusiastic"
                                    }
                                    if (newTone != professionalTone) {
                                        onUpdateProfessionalTone(newTone)
                                    }
                                },
                                valueRange = 0f..2f,
                                steps = 1,
                                colors = SliderDefaults.colors(
                                    thumbColor = dynSecondaryAccent,
                                    activeTrackColor = dynSecondaryAccent,
                                    inactiveTrackColor = dynBorderGrey
                                ),
                                modifier = Modifier.fillMaxWidth().testTag("professional_tone_slider")
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Formal", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (professionalTone == "Formal") dynSecondaryAccent else dynTextMuted)
                                Text("Casual", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (professionalTone == "Casual") dynSecondaryAccent else dynTextMuted)
                                Text("Enthusiastic", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (professionalTone == "Enthusiastic") dynSecondaryAccent else dynTextMuted)
                            }

                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = dynBorderGrey.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Call Logs history
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "KANNA TELEPHONY ACTIVITY LOGS (${callSessions.size})",
                                    color = dynTextLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (callSessions.isNotEmpty()) {
                                    Text(
                                        text = "CLEAR ALL",
                                        color = Color.Red,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier
                                            .clickable { onClearCallSessions() }
                                            .testTag("clear_call_logs_btn")
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))

                            if (callSessions.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(BackgroundDark)
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No recorded call screening histories yet.",
                                        color = dynTextMuted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    callSessions.forEach { session ->
                                        var isExpanded by remember { mutableStateOf(false) }
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(BackgroundDark)
                                                .border(BorderStroke(1.dp, dynBorderGrey.copy(alpha = 0.3f)), RoundedCornerShape(6.dp))
                                                .clickable { isExpanded = !isExpanded }
                                                .padding(10.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "📞 Caller: ${session.callerName}",
                                                        color = dynTextLight,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                    Text(
                                                        text = java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(session.timestamp)),
                                                        color = dynTextMuted,
                                                        fontSize = 9.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Icon(
                                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                        contentDescription = "Expand",
                                                        tint = dynSecondaryAccent,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    IconButton(
                                                        onClick = { onDeleteCallSession(session.id) },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = "Delete log", tint = Color.Red, modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                            }

                                            if (isExpanded) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                HorizontalDivider(color = dynBorderGrey.copy(alpha = 0.2f))
                                                Spacer(modifier = Modifier.height(6.dp))
                                                
                                                Text(
                                                    text = "TRANSCRIPT:",
                                                    color = dynSecondaryAccent,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = session.transcript.ifBlank { "No transcription captured." },
                                                    color = dynTextLight,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    lineHeight = 14.sp
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "AI SUMMARIZATION:",
                                                    color = dynPrimaryAccent,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Text(
                                                    text = session.summary.ifBlank { "No summary available." },
                                                    color = dynTextLight,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    lineHeight = 14.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Dynamic Standby Battery Status dashboard banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isLowBatteryModeActive) Color(0x33B91C1C) else Color(0x11059669))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isLowBatteryModeActive) Icons.Default.Warning else Icons.Default.CheckCircle,
                                contentDescription = "Battery Status",
                                tint = if (isLowBatteryModeActive) Color.Red else Color.Green,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "BATTERY SAVER MONITOR",
                                    color = dynTextLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = if (isLowBatteryModeActive) "STANDBY ENGAGED: Intercept paused below 15%" else "System safe: background processing active",
                                    color = dynTextMuted,
                                    fontSize = 9.sp
                                )
                            }
                        }
                        Text(
                            text = "$batteryLevel%",
                            color = if (isLowBatteryModeActive) Color.Red else Color.Green,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // 3. Recent Replies History & Undo list section
            if (recentReplies.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "RECENT REPLIES HISTORY (LAST 3)",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = 12.dp)
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                recentReplies.forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = BackgroundDark.copy(alpha = 0.9f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BorderGrey.copy(alpha = 0.7f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "To: ${item.senderName}",
                                    color = SecondaryAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = item.replyText,
                                    color = TextLight,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                            
                            Button(
                                onClick = { onUndoReply(item) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991b1b)), // Deep red warning hue
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .height(28.dp)
                                    .testTag("undo_${item.notificationId}")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Undo Action",
                                        tint = TextLight,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "UNDO",
                                        color = TextLight,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }

            } // End of hudTabSelected == 0 block

            // --- TAB 1: SECURE MEETINGS ---
            if (hudTabSelected == 1) {
                // Connection Status Card
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isRecordingActive) Color.Green else BorderGrey)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (isRecordingActive) Color.Green else AlertOrange)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CALENDAR DISPATCH & RECORDING STATUS",
                                color = PrimaryAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        if (isRecordingActive) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    color = Color.Green,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "LIVE AUDIO SPECTRUM STREAM ACTIVE",
                                    color = Color.Green,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Kanna Proxy has joined '$joiningEventTitle' and is streaming audio transcripts securely to local RAG.",
                                color = TextLight,
                                fontSize = 11.sp
                            )
                        } else {
                            Text(
                                text = "📡 DEPLOYMENT READY: SECURE TELEPHONY CALENDAR DAEMON STANDBY",
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No active audio streams. Kanna will automatically join scheduled meetings to represent you.",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick meeting builder
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderGrey)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "SCHEDULE NEW CALENDAR MEETING PROXY",
                            color = SecondaryAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        var newTitle by remember { mutableStateOf("") }
                        var newOrganizer by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Meeting Title (e.g., Tech Sync)", fontSize = 9.sp) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = TextLight),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SecondaryAccent, unfocusedBorderColor = BorderGrey)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = newOrganizer,
                            onValueChange = { newOrganizer = it },
                            label = { Text("Organizer/Platform (e.g., Google Meet)", fontSize = 9.sp) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = TextLight),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SecondaryAccent, unfocusedBorderColor = BorderGrey)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (newTitle.isNotBlank()) {
                                    onAddNewCalendarEvent(newTitle, newOrganizer.ifBlank { "Google Meet" })
                                    newTitle = ""
                                    newOrganizer = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("add_meeting_btn")
                        ) {
                            Text("INSERT CALENDAR TELEPHONY DISPATCH", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BackgroundDark, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // List Scheduled Events
                Text(
                    text = "UPCOMING SCHEDULED PROXIES (${calendarEvents.size})",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Start).padding(start = 12.dp, bottom = 6.dp)
                )

                if (calendarEvents.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceDark).padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No scheduled events listed in database.", color = TextMuted, fontSize = 11.sp)
                    }
                } else {
                    calendarEvents.forEach { event ->
                        val isJoiningThis = event.id == joiningEventId
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.9f)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (isJoiningThis || event.status == "RECORDING") Color.Green else BorderGrey)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = "📅 ${event.title}", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(text = "Vendor/Host: ${event.organizer}", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                when (event.status) {
                                                    "COMPLETED" -> Color(0x33059669)
                                                    "RECORDING", "JOINING" -> Color(0x3300E5FF)
                                                    else -> Color(0x11FFFFFF)
                                                }
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = event.status,
                                            color = when (event.status) {
                                                "COMPLETED" -> Color.Green
                                                "RECORDING", "JOINING" -> Color.Cyan
                                                else -> TextLight
                                            },
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                if (event.summary.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    val isEditing = editingSummaryEventId == event.id
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(BackgroundDark)
                                            .border(BorderStroke(1.dp, if (isEditing) SecondaryAccent else Color.Transparent), RoundedCornerShape(6.dp))
                                            .padding(10.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (isEditing) "✏️ EDITING SESSION SUMMARY:" else "🧠 AI SESSION SUMMARY:",
                                                    color = if (isEditing) SecondaryAccent else PrimaryAccent,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                
                                                if (!isEditing) {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        // Edit Summary Trigger
                                                        Row(
                                                            modifier = Modifier
                                                                .clickable {
                                                                    editingSummaryEventId = event.id
                                                                    editingSummaryText = event.summary
                                                                }
                                                                .padding(vertical = 2.dp)
                                                                .testTag("edit_summary_btn_${event.id}"),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Edit,
                                                                contentDescription = "Edit Summary",
                                                                tint = SecondaryAccent,
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text(
                                                                text = "EDIT",
                                                                color = SecondaryAccent,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                fontFamily = FontFamily.Monospace
                                                            )
                                                        }
                                                        
                                                        // Export to notes
                                                        Row(
                                                            modifier = Modifier
                                                                .clickable {
                                                                    val cleanTitle = event.title.replace("[^a-zA-Z0-9]".toRegex(), "_")
                                                                        .replace("_+".toRegex(), "_").trim('_')
                                                                    val fileName = "Meeting_Summary_${cleanTitle}_Manual.txt"
                                                                    onCreateSecureFile(fileName, event.summary)
                                                                }
                                                                .padding(vertical = 2.dp)
                                                                .testTag("save_notes_btn_${event.id}"),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Check,
                                                                contentDescription = "Save as Notes",
                                                                tint = Color.Green,
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text(
                                                                text = "SAVE AS NOTE",
                                                                color = Color.Green,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                fontFamily = FontFamily.Monospace
                                                            )
                                                        }

                                                        // Draft Email containing the generated post-meeting summary
                                                        Row(
                                                            modifier = Modifier
                                                                .clickable {
                                                                    onDraftEmailForMeeting(event)
                                                                }
                                                                .padding(vertical = 2.dp)
                                                                .testTag("draft_email_btn_${event.id}"),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Email,
                                                                contentDescription = "Draft Email",
                                                                tint = PrimaryAccent,
                                                                modifier = Modifier.size(11.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(3.dp))
                                                            Text(
                                                                text = "DRAFT EMAIL",
                                                                color = PrimaryAccent,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                fontFamily = FontFamily.Monospace
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            if (isEditing) {
                                                OutlinedTextField(
                                                    value = editingSummaryText,
                                                    onValueChange = { editingSummaryText = it },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(min = 100.dp, max = 220.dp)
                                                        .testTag("summary_edit_field_${event.id}"),
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = TextLight, lineHeight = 15.sp),
                                                    colors = OutlinedTextFieldDefaults.colors(
                                                        focusedBorderColor = SecondaryAccent,
                                                        unfocusedBorderColor = BorderGrey,
                                                        cursorColor = SecondaryAccent
                                                    )
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Button(
                                                        onClick = {
                                                            onUpdateCalendarEvent(event.copy(summary = editingSummaryText))
                                                            editingSummaryEventId = null
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(30.dp)
                                                            .testTag("save_edited_summary_${event.id}"),
                                                        contentPadding = PaddingValues(0.dp),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text("SAVE CHANGES", color = BackgroundDark, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                    }
                                                    
                                                    Button(
                                                        onClick = {
                                                            val cleanTitle = event.title.replace("[^a-zA-Z0-9]".toRegex(), "_")
                                                                .replace("_+".toRegex(), "_").trim('_')
                                                            val fileName = "Meeting_Summary_${cleanTitle}_Edited.txt"
                                                            onCreateSecureFile(fileName, editingSummaryText)
                                                            onUpdateCalendarEvent(event.copy(summary = editingSummaryText))
                                                            editingSummaryEventId = null
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                                        modifier = Modifier
                                                            .weight(1.3f)
                                                            .height(30.dp)
                                                            .testTag("save_edited_summary_note_${event.id}"),
                                                        contentPadding = PaddingValues(0.dp),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text("SAVE & COPY TO VAULT", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                    }

                                                    Button(
                                                        onClick = {
                                                            editingSummaryEventId = null
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                                                        modifier = Modifier
                                                            .weight(0.7f)
                                                            .height(30.dp)
                                                            .testTag("cancel_edit_summary_${event.id}"),
                                                        contentPadding = PaddingValues(0.dp),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text("CANCEL", color = TextLight, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                                    }
                                                }
                                            } else {
                                                Text(text = event.summary, color = TextLight, fontSize = 11.sp, lineHeight = 15.sp)
                                            }
                                        }
                                    }
                                }

                                // Extracted tasks lists for this specific completed meeting
                                val meetingTasks = auraTasks.filter { it.sourceMeeting.equals(event.title, ignoreCase = true) }
                                if (meetingTasks.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(dynSurfaceDark.copy(alpha = 0.5f))
                                            .border(BorderStroke(1.dp, dynBorderGrey.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.List,
                                                contentDescription = "Action items",
                                                tint = dynSecondaryAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "IDENTIFIED MEETING ACTION ITEMS:",
                                                color = dynSecondaryAccent,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        meetingTasks.forEach { task ->
                                            val isAdded = task.status == "CALENDAR_ADDED"
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                when (task.assignee.uppercase()) {
                                                                    "BRENDA" -> Color(0xFF818CF8)
                                                                    "KEITH VANCE" -> Color(0xFF34D399)
                                                                    else -> Color(0xFFF472B6)
                                                                }
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                     ) {
                                                        Text(
                                                            text = task.assignee.firstOrNull()?.uppercase()?.toString() ?: "?",
                                                            color = Color.White,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Column {
                                                        Text(
                                                            text = task.title,
                                                            color = dynTextLight,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Medium
                                                        )
                                                        Text(
                                                            text = "Assignee: ${task.assignee}",
                                                            color = dynTextMuted,
                                                            fontSize = 8.sp,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(4.dp))
                                                
                                                Button(
                                                    onClick = {
                                                        if (!isAdded) {
                                                            onUpdateTask(task.copy(status = "CALENDAR_ADDED"))
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isAdded) Color(0x3334D399) else dynSecondaryAccent
                                                    ),
                                                    contentPadding = PaddingValues(horizontal = 8.dp),
                                                    modifier = Modifier
                                                        .height(24.dp)
                                                        .testTag("convert_task_${task.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = if (isAdded) Icons.Default.Check else Icons.Default.DateRange,
                                                        contentDescription = "Alert converter icon",
                                                        tint = if (isAdded) Color.Green else BackgroundDark,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (isAdded) "CALENDAR REMINDER ACTIVE" else "Create Reminder",
                                                        fontSize = 8.sp,
                                                        color = if (isAdded) Color.Green else BackgroundDark,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { onDeleteCalendarEvent(event.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete Event", tint = Color.Red, modifier = Modifier.size(14.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (event.status != "COMPLETED" && event.status != "RECORDING" && event.status != "JOINING") {
                                        Button(
                                            onClick = { onSimulateCalendarEventJoin(event.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                                            contentPadding = PaddingValues(horizontal = 10.dp),
                                            modifier = Modifier.height(28.dp).testTag("join_meet_${event.id}")
                                        ) {
                                            Text("JOIN LIVE AS PROXY", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = BackgroundDark, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // RAG QUERIES CARD
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderGrey)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "SECURE RAG SECURE CORPUS QUERY PATH",
                            color = PrimaryAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Queries local SQLite databases & vector meeting transcript files safely",
                            color = TextMuted,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        var ragTopicQuery by remember { mutableStateOf("General sync discussions") }
                        var ragQuestionInput by remember { mutableStateOf("") }

                        OutlinedTextField(
                            value = ragTopicQuery,
                            onValueChange = { ragTopicQuery = it },
                            label = { Text("Query Topic context", fontSize = 8.sp) },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = TextLight, fontFamily = FontFamily.Monospace),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryAccent, unfocusedBorderColor = BorderGrey)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = ragQuestionInput,
                            onValueChange = { ragQuestionInput = it },
                            label = { Text("What specifically would you like to retrieve?", fontSize = 8.sp) },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = TextLight),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryAccent, unfocusedBorderColor = BorderGrey)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        var isRAGQueryLoading by remember { mutableStateOf(false) }
                        var ragAnswerOutput by remember { mutableStateOf("") }

                        Button(
                            onClick = {
                                if (ragQuestionInput.isNotBlank()) {
                                    isRAGQueryLoading = true
                                    onProcessCommand("retrieve transcript query topic '$ragTopicQuery' question '$ragQuestionInput'")
                                    kotlinx.coroutines.GlobalScope.launch {
                                        kotlinx.coroutines.delay(1200)
                                        isRAGQueryLoading = false
                                        val match = calendarEvents.find { it.summary.isNotBlank() }
                                        ragAnswerOutput = if (match != null) {
                                            "Based on local transcripts from scheduled meeting: ${match.title}\n" +
                                            "Q: $ragQuestionInput\n" +
                                            "Retrieving: Simulated local search suggests the discussion involved \"${match.summary}\"."
                                        } else {
                                            "Database search complete. Q: $ragQuestionInput\n" +
                                            "Answer: Secure local database lists 0 total completed transcripts. Record a meeting to compile indexes."
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("query_rag_btn")
                        ) {
                            Text("QUERY RAG ARCHIVES", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BackgroundDark, fontFamily = FontFamily.Monospace)
                        }

                        if (isRAGQueryLoading) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = PrimaryAccent, strokeWidth = 2.dp, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Engaging vector-space distance ranking...", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        } else if (ragAnswerOutput.isNotBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(BackgroundDark).padding(8.dp)
                            ) {
                                Text(
                                    text = ragAnswerOutput,
                                    color = TextLight,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // MASTER TO-DO CARD
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = dynSurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, dynBorderGrey)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "🔒 KANNA TRUSTED TO-DO TASKBOARD",
                                    color = dynPrimaryAccent,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Secure list of AI extracted items & manual targets",
                                    color = dynTextMuted,
                                    fontSize = 9.sp
                                )
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(dynSecondaryAccent.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${auraTasks.size} CHORES",
                                        color = dynSecondaryAccent,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        var manualTaskTitle by remember { mutableStateOf("") }
                        var manualTaskAssignee by remember { mutableStateOf("Chaitanya") }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = manualTaskTitle,
                                onValueChange = { manualTaskTitle = it },
                                placeholder = { Text("Enter manual reminder target...", fontSize = 10.sp, color = dynTextMuted) },
                                modifier = Modifier.weight(1f).height(42.dp),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = dynTextLight),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = dynSecondaryAccent, unfocusedBorderColor = dynBorderGrey)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Button(
                                onClick = {
                                    if (manualTaskTitle.isNotBlank()) {
                                        onAddTask(
                                            AuraTaskEntity(
                                                title = manualTaskTitle,
                                                assignee = manualTaskAssignee,
                                                status = "PENDING",
                                                sourceMeeting = "Manual Entry"
                                            )
                                        )
                                        manualTaskTitle = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = dynSecondaryAccent),
                                contentPadding = PaddingValues(horizontal = 12.dp),
                                modifier = Modifier.height(42.dp).testTag("add_manual_task_btn")
                            ) {
                                Text("ADD", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = BackgroundDark, fontFamily = FontFamily.Monospace)
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Assignee proxy: ", fontSize = 9.sp, color = dynTextMuted, fontFamily = FontFamily.Monospace)
                            listOf("Chaitanya", "Brenda", "Keith Vance").forEach { member ->
                                val isSelected = manualTaskAssignee == member
                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 4.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (isSelected) dynSecondaryAccent.copy(alpha = 0.2f) else dynBorderGrey.copy(alpha = 0.3f))
                                        .border(BorderStroke(1.dp, if (isSelected) dynSecondaryAccent else Color.Transparent), RoundedCornerShape(4.dp))
                                        .clickable { manualTaskAssignee = member }
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = member.uppercase(),
                                        color = if (isSelected) dynSecondaryAccent else dynTextLight,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        if (auraTasks.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(60.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No pending tasks listed. Automatically extracts tasks when you join a proxy meeting!", color = dynTextMuted, fontSize = 10.sp, textAlign = TextAlign.Center)
                            }
                        } else {
                            auraTasks.forEach { task ->
                                val isDone = task.status == "COMPLETED"
                                val isConverted = task.status == "CALENDAR_ADDED"
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(BackgroundDark.copy(alpha = 0.3f))
                                        .border(BorderStroke(1.dp, dynBorderGrey.copy(alpha = 0.15f)), RoundedCornerShape(6.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        androidx.compose.material3.Checkbox(
                                            checked = isDone,
                                            onCheckedChange = { checked ->
                                                onUpdateTask(task.copy(status = if (checked) "COMPLETED" else "PENDING"))
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = dynSecondaryAccent),
                                            modifier = Modifier.size(24.dp).testTag("chk_task_${task.id}")
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = task.title,
                                                color = if (isDone) dynTextMuted else dynTextLight,
                                                fontSize = 11.sp,
                                                style = androidx.compose.ui.text.TextStyle(
                                                    textDecoration = if (isDone) androidx.compose.ui.text.style.TextDecoration.LineThrough else androidx.compose.ui.text.style.TextDecoration.None
                                                )
                                            )
                                            Text(
                                                text = "Proxy assignee: ${task.assignee} | Topic context: ${task.sourceMeeting}",
                                                color = dynTextMuted,
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                    
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        if (!isDone && !isConverted) {
                                            IconButton(
                                                onClick = {
                                                    onUpdateTask(task.copy(status = "CALENDAR_ADDED"))
                                                },
                                                modifier = Modifier.size(24.dp).testTag("task_convert_reminder_${task.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DateRange,
                                                    contentDescription = "Create Reminder",
                                                    tint = dynSecondaryAccent,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        } else if (isConverted) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Scheduled",
                                                tint = Color.Green,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(4.dp))
                                        IconButton(
                                            onClick = { onDeleteTask(task.id) },
                                            modifier = Modifier.size(24.dp).testTag("del_task_${task.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete task",
                                                tint = Color.Red,
                                                modifier = Modifier.size(13.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // --- TAB 2: SOCIAL AUDITING & LINKEDIN AUTOMATION ---
            if (hudTabSelected == 2) {
                // LinkedIn Scrape task block
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderGrey)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Share, contentDescription = "Scrape", tint = SecondaryAccent, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LINKEDIN CRAWLER & AUDITING ENGINE",
                                color = SecondaryAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            text = "Extracts trending high-impact posters & pre-drafts contextual high-value comments of the day.",
                            color = TextMuted,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = { onTriggerWebScrapeTask() },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("trigger_scraping_btn")
                        ) {
                            Text("RUN LINKEDIN TREND SCRAPER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BackgroundDark, fontFamily = FontFamily.Monospace)
                        }

                        if (isScrapingInProgress) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                                CircularProgressIndicator(color = SecondaryAccent, strokeWidth = 1.5.dp, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Crawling LinkedIn API feeds dynamically...", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pending Comments Approver list
                val pendingSocialPosts = socialPosts.filter { it.status == "PENDING_APPROVAL" || it.platform == "LINKEDIN" }
                Text(
                    text = "DRAFT AUDITS REQUIRING APPROVAL (${pendingSocialPosts.size})",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.Start).padding(start = 12.dp, bottom = 6.dp)
                )

                if (pendingSocialPosts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp).clip(RoundedCornerShape(8.dp)).background(SurfaceDark).padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No pending drafts waiting approval in sqlite.", color = TextMuted, fontSize = 11.sp)
                    }
                } else {
                    pendingSocialPosts.forEach { post ->
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark.copy(alpha = 0.95f)),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, if (post.status == "PUBLISHED") Color.Green else BorderGrey)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(text = "🗣️ Trending Post by @${post.title}", color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text(text = "Topic: ${post.platform}", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (post.status == "PUBLISHED") Color(0x33059669) else Color(0x2200E5FF))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = post.status,
                                            color = if (post.status == "PUBLISHED") Color.Green else Color.Cyan,
                                            fontSize = 8.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Post Draft Content:",
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = post.replyDraft,
                                    color = TextLight,
                                    fontSize = 11.sp,
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(BackgroundDark).padding(6.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    if (post.status != "PUBLISHED") {
                                        Button(
                                            onClick = { onRejectScrapedComment(post.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991b1b)),
                                            contentPadding = PaddingValues(horizontal = 10.dp),
                                            modifier = Modifier.height(28.dp).testTag("reject_comment_${post.id}")
                                        ) {
                                            Text("REJECT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextLight, fontFamily = FontFamily.Monospace)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { onApproveScrapedComment(post.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                            contentPadding = PaddingValues(horizontal = 10.dp),
                                            modifier = Modifier.height(28.dp).testTag("approve_comment_${post.id}")
                                        ) {
                                            Text("APPROVE & TRANSMIT", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = BackgroundDark, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // LinkedIn Postmaker Generative Block
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, BorderGrey)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "CAMPAIGN POST WRITER & ILLUSTRATOR",
                            color = PrimaryAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Dynamically crafts professional articles and draws layout templates",
                            color = TextMuted,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        var writeTopic by remember { mutableStateOf("Privacy and notification exhaustion") }
                        OutlinedTextField(
                            value = writeTopic,
                            onValueChange = { writeTopic = it },
                            label = { Text("What theme or topic?", fontSize = 8.sp) },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = TextLight),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryAccent, unfocusedBorderColor = BorderGrey)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = { onGenerateLinkedInPostWithGraphic(writeTopic) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("generate_linkedin_btn")
                        ) {
                            Text("DRAFT HIGH-IMPACT CAMPAIGN", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BackgroundDark, fontFamily = FontFamily.Monospace)
                        }

                        if (generatedLinkedInVisualPrompt.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("PROMPT GENERATED FOR ATTACHED VISUAL STICKER / SLIDE:", color = PrimaryAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            Text(
                                text = generatedLinkedInVisualPrompt,
                                color = TextLight,
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(BackgroundDark).padding(6.dp)
                            )

                            if (linkedinVisualImageReady) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Text("COMPRESSED COMPANION GRAPHIC READY FOR REVIEW:", color = Color.Green, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                Spacer(modifier = Modifier.height(6.dp))

                                Card(
                                    modifier = Modifier.fillMaxWidth().height(150.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.5.dp, PrimaryAccent)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(dynSecondaryAccent, BackgroundDark, dynPrimaryAccent)
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Lock,
                                                contentDescription = "Lock Logo",
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = writeTopic.uppercase(),
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "🔒 KANNA WORKSPACE SECURITY SLIDE",
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Speech Output Display
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(containerColor = BackgroundDark.copy(alpha = 0.8f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderGrey)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SPEECH OUTPUT FEED",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = voiceOutputText.ifBlank { "Aura AI is responsive and awaiting oral instructions..." },
                        color = TextLight,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Quick speech simulation chips! Excellent for keyboard / web browser sandbox testing
            Text(
                text = "SIMULATE VOCAL DIRECTIVES",
                color = TextMuted,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val commandsList = listOf(
                    "give reply to that",
                    "send reply",
                    "what is my profile",
                    "status audit",
                    "compose LinkedIn review"
                )
                commandsList.forEach { cmd ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(BorderGrey)
                            .border(BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.4f)), RoundedCornerShape(20.dp))
                            .clickable { onProcessCommand(cmd) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = cmd,
                            color = TextLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Text input fallback matching Siri keyboard invocation style
            OutlinedTextField(
                value = rawInputText,
                onValueChange = { rawInputText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                placeholder = {
                    Text(
                        "Or speak/type command (e.g. \"give reply to that\")...",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                },
                maxLines = 1,
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (rawInputText.isNotBlank()) {
                                onProcessCommand(rawInputText)
                                rawInputText = ""
                                keyboardController?.hide()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Submit Command",
                            tint = SecondaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextLight,
                    unfocusedTextColor = TextLight,
                    focusedBorderColor = SecondaryAccent,
                    unfocusedBorderColor = BorderGrey,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        if (rawInputText.isNotBlank()) {
                            onProcessCommand(rawInputText)
                            rawInputText = ""
                            keyboardController?.hide()
                        }
                    }
                )
            )
        }
    }
}

@Composable
fun StatusBadge(name: String, status: ServiceStatus, modifier: Modifier = Modifier) {
    val (color, text) = when (status) {
        ServiceStatus.CONNECTED -> Color(0xFF00FF66) to "CONNECTED"
        ServiceStatus.TESTING -> Color(0xFF33B3FF) to "TESTING"
        ServiceStatus.FAILED -> Color(0xFFFF3333) to "OFFLINE"
        ServiceStatus.MISSING_KEY -> Color(0xFFFFB333) to "NO_KEY"
        ServiceStatus.UNTESTED -> Color(0xFF888888) to "UNTESTED"
    }
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(5.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$name: $text",
            color = color,
            fontSize = 7.5.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun HeaderBlock(
    highThinking: Boolean,
    profile: UserProfileEntity,
    geminiStatus: ServiceStatus,
    localStorageStatus: ServiceStatus,
    isUpdateAvailable: Boolean = false,
    onLockClick: () -> Unit,
    onToggleHighThinking: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isUpdateAvailable) Color(0xFFE57373) else SecondaryAccent)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "KANNA AI",
                        color = TextLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    if (isUpdateAvailable) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFE57373))
                                .padding(horizontal = 4.dp, vertical = 1.5.dp)
                        ) {
                            Text(
                                "UPDATE READY",
                                color = BackgroundDark,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    StatusBadge(name = "API", status = geminiStatus)
                    Spacer(modifier = Modifier.width(5.dp))
                    StatusBadge(name = "DB", status = localStorageStatus)
                }
                Text(
                    text = "${profile.userName} | ${profile.securityLevel}",
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Thinking Level
            IconButton(
                onClick = { onToggleHighThinking(!highThinking) },
                modifier = Modifier.testTag("thinking_mode_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Thinking Mode",
                    tint = if (highThinking) PrimaryAccent else TextMuted,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Settings Lock
            IconButton(
                onClick = onLockClick,
                modifier = Modifier.testTag("security_settings_button")
            ) {
                Box {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Security Parameters",
                        tint = if (isUpdateAvailable) Color(0xFFE57373) else SecondaryAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    if (isUpdateAvailable) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE57373))
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
        }
    }
}

// --- Data Visualization Models & Utilities ---

data class ChartDayData(
    val dayLabel: String,
    val notificationsCount: Int,
    val callsCount: Int
)

fun getWeeklySummaryData(
    notifications: List<com.example.data.db.NotificationEntity>,
    callSessions: List<com.example.data.db.CallSessionEntity>
): List<ChartDayData> {
    val sdf = java.text.SimpleDateFormat("EEE", java.util.Locale.US)
    val days = mutableListOf<String>()
    val dayTimeRanges = mutableListOf<Pair<Long, Long>>()
    
    for (i in 6 downTo 0) {
        val dCal = java.util.Calendar.getInstance()
        dCal.add(java.util.Calendar.DAY_OF_YEAR, -i)
        
        // start of day
        dCal.set(java.util.Calendar.HOUR_OF_DAY, 0)
        dCal.set(java.util.Calendar.MINUTE, 0)
        dCal.set(java.util.Calendar.SECOND, 0)
        dCal.set(java.util.Calendar.MILLISECOND, 0)
        val startMs = dCal.timeInMillis
        
        // end of day
        dCal.set(java.util.Calendar.HOUR_OF_DAY, 23)
        dCal.set(java.util.Calendar.MINUTE, 59)
        dCal.set(java.util.Calendar.SECOND, 59)
        dCal.set(java.util.Calendar.MILLISECOND, 999)
        val endMs = dCal.timeInMillis
        
        days.add(sdf.format(dCal.time))
        dayTimeRanges.add(startMs to endMs)
    }
    
    return days.mapIndexed { index, dayName ->
        val (start, end) = dayTimeRanges[index]
        val notificationsCount = notifications.count { it.timestamp in start..end }
        val callsCount = callSessions.count { it.timestamp in start..end }
        ChartDayData(
            dayLabel = dayName,
            notificationsCount = notificationsCount,
            callsCount = callsCount
        )
    }
}

@Composable
fun InterceptionDashboard(
    notifications: List<com.example.data.db.NotificationEntity>,
    callSessions: List<com.example.data.db.CallSessionEntity>
) {
    val rawData = getWeeklySummaryData(notifications, callSessions)
    val hasData = rawData.any { it.notificationsCount > 0 || it.callsCount > 0 }
    val displayData = if (hasData) {
        rawData
    } else {
        listOf(
            ChartDayData("Mon", 14, 5),
            ChartDayData("Tue", 22, 8),
            ChartDayData("Wed", 19, 4),
            ChartDayData("Thu", 32, 11),
            ChartDayData("Fri", 25, 9),
            ChartDayData("Sat", 12, 3),
            ChartDayData("Sun", 8, 2)
        )
    }

    val totalIntercepted = displayData.sumOf { it.notificationsCount }
    val totalScreened = displayData.sumOf { it.callsCount }
    val peakDay = displayData.maxByOrNull { it.notificationsCount + it.callsCount }?.dayLabel ?: "Thu"

    var selectedDay by remember { mutableStateOf<ChartDayData?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BackgroundDark)
            .border(1.dp, BorderGrey, RoundedCornerShape(8.dp))
            .padding(12.dp)
            .testTag("analytics_dashboard_container")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "RECHARTS SECURE TELEMETRY ENGINE",
                    color = PrimaryAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = if (hasData) "Real-time metrics collected in last 7 days." else "Telemetry simulation mode (No live events detected).",
                    color = if (hasData) SecondaryAccent else TextMuted,
                    fontSize = 9.5.sp
                )
            }
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(PrimaryAccent.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "RECHARTS PROJECTION",
                    color = PrimaryAccent,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(SurfaceDark, RoundedCornerShape(6.dp))
                    .border(0.5.dp, BorderGrey, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text("PROCESSED SUMMARIES", color = TextMuted, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$totalIntercepted",
                    color = PrimaryAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(SurfaceDark, RoundedCornerShape(6.dp))
                    .border(0.5.dp, BorderGrey, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text("BLOCKED CALLS", color = TextMuted, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$totalScreened",
                    color = SecondaryAccent,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .background(SurfaceDark, RoundedCornerShape(6.dp))
                    .border(0.5.dp, BorderGrey, RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text("PEAK DAY", color = TextMuted, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = peakDay,
                    color = TextLight,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        val maxVal = (displayData.maxOfOrNull { maxOf(it.notificationsCount, it.callsCount) } ?: 10).coerceAtLeast(10)
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(SurfaceDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 12.dp)
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val barCount = displayData.size
                val sectionWidth = width / barCount
                val gap = sectionWidth * 0.25f
                val singleBarWidth = (sectionWidth - gap) / 2
                
                val linesCount = 4
                for (i in 0 until linesCount) {
                    val y = height * (i.toFloat() / (linesCount - 1))
                    drawLine(
                        color = BorderGrey.copy(alpha = 0.4f),
                        start = androidx.compose.ui.geometry.Offset(0f, y),
                        end = androidx.compose.ui.geometry.Offset(width, y),
                        strokeWidth = 1f
                    )
                }

                displayData.forEachIndexed { idx, day ->
                    val xStart = idx * sectionWidth + gap/2
                    
                    val percentNotif = day.notificationsCount.toFloat() / maxVal
                    val notifBarHeight = height * percentNotif
                    if (notifBarHeight > 0) {
                        drawRoundRect(
                            color = PrimaryAccent,
                            topLeft = androidx.compose.ui.geometry.Offset(xStart, height - notifBarHeight),
                            size = androidx.compose.ui.geometry.Size(singleBarWidth, notifBarHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )
                    }

                    val percentCalls = day.callsCount.toFloat() / maxVal
                    val callsBarHeight = height * percentCalls
                    if (callsBarHeight > 0) {
                        drawRoundRect(
                            color = SecondaryAccent,
                            topLeft = androidx.compose.ui.geometry.Offset(xStart + singleBarWidth, height - callsBarHeight),
                            size = androidx.compose.ui.geometry.Size(singleBarWidth, callsBarHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            displayData.forEach { day ->
                Box(
                    modifier = Modifier
                        .clickable { selectedDay = day }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = day.dayLabel,
                        color = if (selectedDay?.dayLabel == day.dayLabel) PrimaryAccent else TextMuted,
                        fontSize = 9.sp,
                        fontWeight = if (selectedDay?.dayLabel == day.dayLabel) FontWeight.Bold else FontWeight.Normal,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(PrimaryAccent, RoundedCornerShape(1.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Processed Summaries", color = TextMuted, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(6.dp).background(SecondaryAccent, RoundedCornerShape(1.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Blocked Calls", color = TextMuted, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                }
            }

            if (selectedDay != null) {
                Box(
                    modifier = Modifier
                        .border(1.dp, PrimaryAccent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .background(SurfaceDark)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${selectedDay!!.dayLabel} ⮕ PM: ${selectedDay!!.notificationsCount} | Blocked: ${selectedDay!!.callsCount}",
                        color = PrimaryAccent,
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "Tap a day for Recharts Tooltip",
                    color = TextMuted,
                    fontSize = 8.5.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun AiHistoryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) PrimaryAccent.copy(alpha = 0.2f) else SurfaceDark)
            .border(
                1.dp,
                if (selected) PrimaryAccent else BorderGrey,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            color = if (selected) PrimaryAccent else TextMuted,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun ProfileConfigurationPanel(
    initialSubTab: Int = 0,
    profile: UserProfileEntity,
    customKey: String,
    customEndpoint: String,
    customModel: String,
    customTimeout: Int,
    customBackoff: String,
    geminiStatus: ServiceStatus,
    localStorageStatus: ServiceStatus,
    logs: List<DiagnosticLog>,
    autoModelUpdatesEnabled: Boolean,
    privacyFirstMode: Boolean,
    cryptoPassphrase: String,
    isEncryptionActive: Boolean,
    privacyInsights: List<PrivacyInsightEntity>,
    appPasscode: String,
    isPasscodeLockEnabled: Boolean,
    autoDeleteDays: Int,
    currentVersion: String = "2.4.1",
    isUpdateAvailable: Boolean = false,
    availableVersion: String = "2.4.1",
    isCheckingForUpdates: Boolean = false,
    updateStatusMessage: String = "Version 2.4.1 (Latest)",
    onCheckForUpdates: () -> Unit = {},
    onApplyUpdateNow: () -> Unit = {},
    onClose: () -> Unit,
    onSaveProfile: (String, String, Boolean, String, String, Boolean) -> Unit,
    onSaveApiSettings: (String, String, String, Int, String) -> Unit,
    onResetApiSettings: () -> Unit,
    onTestDiagnostics: () -> Unit,
    onClearLogs: () -> Unit,
    onToggleAutoModelUpdates: (Boolean) -> Unit,
    onTogglePrivacyFirstMode: (Boolean) -> Unit,
    onUpdateEncryptionSettings: (Boolean, String) -> Unit,
    onClearPrivacyInsights: () -> Unit,
    onManualTrigger: () -> Unit,
    onUpdatePasscodeSettings: (String, Boolean) -> Unit,
    onUpdateAutoDeleteInterval: (Int) -> Unit,
    onClearAllLocalData: () -> Unit,
    callScreeningRules: List<CallScreeningRuleEntity> = emptyList(),
    emailTemplates: List<EmailTemplateEntity> = emptyList(),
    isOnDeviceProcessingEnabled: Boolean = true,
    onAddCallScreeningRule: (String, String, String) -> Unit = { _, _, _ -> },
    onDeleteCallScreeningRule: (Int) -> Unit = {},
    onAddEmailTemplate: (String, String, String) -> Unit = { _, _, _ -> },
    onDeleteEmailTemplate: (Int) -> Unit = {},
    onToggleOnDeviceProcessing: (Boolean) -> Unit = {},
    onSyncCalendar: () -> Unit = {},
    dndSyncEnabled: Boolean = false,
    onToggleDndSync: (Boolean) -> Unit = {},
    isDeepWorkActive: Boolean = false,
    deepWorkTimeRemaining: Long = 0L,
    deepWorkDurationMinutes: Int = 25,
    onToggleDeepWorkMode: () -> Unit = {},
    onUpdateDeepWorkDuration: (Int) -> Unit = {},
    contacts: List<com.example.data.db.AuraContactEntity> = emptyList(),
    onAddContact: (String, String, String, String, Boolean) -> Unit = { _, _, _, _, _ -> },
    onDeleteContact: (Int) -> Unit = {},
    versionInstallations: List<com.example.data.db.VersionInstallationEntity> = emptyList(),
    screenedTranscripts: List<com.example.data.db.ScreenedTranscriptEntity> = emptyList(),
    onDeleteScreenedTranscript: (Int) -> Unit = {},
    onClearScreenedTranscripts: () -> Unit = {},
    getDecryptedText: (String, Boolean) -> String = { t, _ -> t },
    onExportPdfReport: () -> Unit = {},
    researchModeEnabled: Boolean = false,
    onToggleResearchMode: (Boolean) -> Unit = {},
    notificationFrequency24h: Map<Int, Int> = emptyMap(),
    powerSaverEnabled: Boolean = false,
    onTogglePowerSaver: (Boolean) -> Unit = {},
    onPreviewVoiceProfile: (String) -> Unit = {},
    selectedVoiceProfile: String = "Kanna Classic",
    onSelectVoiceProfile: (String) -> Unit = {},
    isSystemDndActive: Boolean = false,
    aiActionHistory: List<com.example.data.db.AiActionHistoryEntity> = emptyList(),
    onDeleteAiAction: (Int) -> Unit = {},
    onClearAiActionHistory: () -> Unit = {},
    aiLoggingEnabled: Boolean = true,
    aiLoggingCategoryEnabled: Map<AiActionCategory, Boolean> = emptyMap(),
    onToggleAiLogging: (Boolean) -> Unit = {},
    onToggleAiLoggingCategory: (AiActionCategory, Boolean) -> Unit = { _, _ -> },
    filteredAiActionHistory: List<com.example.data.db.AiActionHistoryEntity> = emptyList(),
    aiHistorySearchQuery: String = "",
    aiHistoryCategoryFilter: AiActionCategory? = null,
    onAiHistorySearchQueryChange: (String) -> Unit = {},
    onAiHistoryCategoryFilterChange: (AiActionCategory?) -> Unit = {},
    onExportAiHistory: (android.net.Uri, com.example.data.repository.ExportFormat) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val exportJsonLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let { onExportAiHistory(it, com.example.data.repository.ExportFormat.JSON) } }
    val exportCsvLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let { onExportAiHistory(it, com.example.data.repository.ExportFormat.CSV) } }
    var activeSubTab by remember(initialSubTab) { mutableStateOf(initialSubTab) } // 0: Profile, 1: API Authorize, 2: Logs, 3: Privacy
    
    // Profile Fields
    var name by remember { mutableStateOf(profile.userName) }
    var email by remember { mutableStateOf(profile.userEmail) }
    var autoReply by remember { mutableStateOf(profile.autoReplyEnabled) }
    var secLevel by remember { mutableStateOf(profile.securityLevel) }
    var sysWakeWord by remember { mutableStateOf(profile.wakeWord) }
    var lockscreenActive by remember { mutableStateOf(profile.lockscreenActivationEnabled) }

    // API Fields
    var apiKeyInput by remember { mutableStateOf(customKey) }
    var endpointInput by remember { mutableStateOf(customEndpoint) }
    var modelInput by remember { mutableStateOf(customModel) }
    var timeoutInput by remember { mutableStateOf(customTimeout.toFloat()) }
    var backoffStratInput by remember { mutableStateOf(customBackoff) }
    var isApiKeyVisible by remember { mutableStateOf(false) }

    // Cryptography Fields
    var encryptPassInput by remember { mutableStateOf(cryptoPassphrase) }

    val isApiKeyValid = remember(apiKeyInput) {
        val trimmed = apiKeyInput.trim()
        // Accept classic "AIzaSy..." keys and newer "AQ.<...>" AI Studio keys (longer, may contain dots).
        trimmed.isEmpty() || (trimmed.length in 20..200 && trimmed.all { it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' })
    }

    LaunchedEffect(customKey, customEndpoint, customModel, customTimeout, customBackoff, cryptoPassphrase) {
        apiKeyInput = customKey
        endpointInput = customEndpoint
        modelInput = customModel
        timeoutInput = customTimeout.toFloat()
        backoffStratInput = customBackoff
        encryptPassInput = cryptoPassphrase
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(16.dp)
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "KANNA AI SECURE PARAMETERS",
                color = SecondaryAccent,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close settings", tint = TextMuted)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Sub tab row
        androidx.compose.material3.ScrollableTabRow(
            selectedTabIndex = activeSubTab,
            containerColor = BackgroundDark,
            contentColor = SecondaryAccent,
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[activeSubTab]),
                    color = SecondaryAccent
                )
            },
            divider = { HorizontalDivider(color = BorderGrey, thickness = 1.dp) }
        ) {
            Tab(
                selected = activeSubTab == 0,
                onClick = { activeSubTab = 0 },
                text = { Text("LOCAL PROFILE", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeSubTab == 1,
                onClick = { activeSubTab = 1 },
                text = { Text("API KEY GATEWAY", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeSubTab == 2,
                onClick = { activeSubTab = 2 },
                text = { Text("LOGS & DIAGNOSTICS", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeSubTab == 3,
                onClick = { activeSubTab = 3 },
                text = { Text("PRIVACY CONTROLS", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeSubTab == 4,
                onClick = { activeSubTab = 4 },
                text = { Text("DEEP WORK", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = activeSubTab == 5,
                onClick = { activeSubTab = 5 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("UPDATES", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        if (isUpdateAvailable) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE57373))
                            )
                        }
                    }
                }
            )
            Tab(
                selected = activeSubTab == 6,
                onClick = { activeSubTab = 6 },
                text = { Text("AI ACTION HISTORY", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (activeSubTab) {
            0 -> {
                // LOCAL PROFILE TAB
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Profile Signatory Name", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = BorderGrey,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("profile_name_field")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("User Authorization Address", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = BorderGrey,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = sysWakeWord,
                        onValueChange = { sysWakeWord = it },
                        label = { Text("Microphone Wake Word Name (e.g. Kanna, Aura)", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = BorderGrey,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BackgroundDark)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Lockscreen Intercept",
                                color = TextLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Let Aura automatically wake in Lock or Unlock",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = lockscreenActive,
                            onCheckedChange = { lockscreenActive = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SecondaryAccent,
                                checkedTrackColor = SecondaryAccent.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = BorderGrey
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BackgroundDark)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Enable Automated Summaries Dispatch",
                                color = TextLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Synthesizes secure quick replies automatically",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = autoReply,
                            onCheckedChange = { autoReply = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SecondaryAccent,
                                checkedTrackColor = SecondaryAccent.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = BorderGrey
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = { onSaveProfile(name, email, autoReply, secLevel, sysWakeWord, lockscreenActive) },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent)
                        ) {
                            Text("APPLY TO PROFILE", color = BackgroundDark, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
            1 -> {
                // API KEY GATEWAY TAB
                Column {
                    Text(
                        text = "AURA WEB REASONING ENGINE AUTHORIZATION",
                        color = PrimaryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Configure custom authorization API keys and connection gateways below. Fallbacks to default AI Studio keys if blank.",
                        color = TextMuted,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        label = { Text("Gemini API Authorization Key", color = if (isApiKeyValid) TextMuted else Color(0xFFFF5555)) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (isApiKeyValid) PrimaryAccent else Color(0xFFFF5555),
                            unfocusedBorderColor = if (isApiKeyValid) BorderGrey else Color(0xFFFF5555).copy(alpha = 0.5f),
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        placeholder = { Text("Enter custom Gemini API key...", color = TextMuted.copy(alpha = 0.5f)) },
                        isError = !isApiKeyValid,
                        supportingText = {
                            if (!isApiKeyValid) {
                                Text("Invalid format: use only letters, digits, '.', '-', '_' (20-200 chars)", color = Color(0xFFFF5555), fontSize = 10.sp)
                            }
                        },
                        visualTransformation = if (isApiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            val image = if (isApiKeyVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                            val description = if (isApiKeyVisible) "Hide API Key" else "Show API Key"
                            IconButton(
                                onClick = { isApiKeyVisible = !isApiKeyVisible },
                                modifier = Modifier.testTag("api_key_visibility_toggle")
                            ) {
                                Icon(imageVector = image, contentDescription = description, tint = TextMuted)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().testTag("api_key_field")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = endpointInput,
                        onValueChange = { endpointInput = it },
                        label = { Text("Gateway Base URL Endpoint", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = BorderGrey,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("endpoint_field")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = modelInput,
                        onValueChange = { modelInput = it },
                        label = { Text("Model Signature Override (Optional)", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = BorderGrey,
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight
                        ),
                        placeholder = { Text("e.g. gemini-3.5-flash", color = TextMuted.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth().testTag("model_field")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "API CONNECTION TIMEOUT: ${timeoutInput.toInt()}s",
                        color = TextLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Slider(
                        value = timeoutInput,
                        onValueChange = { timeoutInput = it },
                        valueRange = 5f..120f,
                        steps = 22,
                        colors = SliderDefaults.colors(
                            thumbColor = SecondaryAccent,
                            activeTrackColor = SecondaryAccent,
                            inactiveTrackColor = BorderGrey
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("api_timeout_slider")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "RETRY BACKOFF STRATEGY",
                        color = TextLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BackgroundDark)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Aggressive", "Conservative").forEach { strat ->
                            val selected = backoffStratInput == strat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (selected) SecondaryAccent else Color.Transparent)
                                    .clickable { backoffStratInput = strat }
                                    .padding(vertical = 8.dp)
                                    .testTag("backoff_strategy_$strat"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = strat.uppercase(),
                                    color = if (selected) BackgroundDark else TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Gateway status indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BackgroundDark)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Systems Telemetry",
                                color = TextLight,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                StatusBadge(name = "GEMINI_API", status = geminiStatus)
                                Spacer(modifier = Modifier.width(6.dp))
                                StatusBadge(name = "ROOM_DB", status = localStorageStatus)
                            }
                        }
                        
                        Button(
                            onClick = {
                                if (!isApiKeyValid) {
                                    android.widget.Toast.makeText(context, "Cannot test: Invalid API Key format.", android.widget.Toast.LENGTH_LONG).show()
                                } else {
                                    onTestDiagnostics()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BackgroundDark),
                            border = BorderStroke(1.dp, if (isApiKeyValid) SecondaryAccent.copy(alpha = 0.5f) else Color(0xFFFF5555).copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("test_handshake_button")
                        ) {
                            Text("TEST HANDSHAKE", color = if (isApiKeyValid) SecondaryAccent else Color(0xFFFF5555), fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                onResetApiSettings()
                                android.widget.Toast.makeText(context, "API Gateway Settings reverted to factory defaults.", android.widget.Toast.LENGTH_LONG).show()
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5555)),
                            border = BorderStroke(1.dp, Color(0xFFFF5555).copy(alpha = 0.5f)),
                            modifier = Modifier.testTag("reset_api_settings_button")
                        ) {
                            Text("RESET DEFAULTS", fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }

                        Button(
                            onClick = {
                                if (!isApiKeyValid) {
                                    android.widget.Toast.makeText(context, "Cannot save: Invalid API Key format.", android.widget.Toast.LENGTH_SHORT).show()
                                } else {
                                    onSaveApiSettings(apiKeyInput, endpointInput, modelInput, timeoutInput.toInt(), backoffStratInput)
                                    android.widget.Toast.makeText(context, "Configuration parameters saved successfully.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isApiKeyValid) SecondaryAccent else BorderGrey),
                            enabled = isApiKeyValid,
                            modifier = Modifier.testTag("save_api_settings_button")
                        ) {
                            Text("SAVE GATEWAY PARAMS", color = if (isApiKeyValid) BackgroundDark else TextMuted, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                }
            }
            2 -> {
                // LOGS & DIAGNOSTICS TAB
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AURA NET REALTIME DIAGNOSTIC LAB",
                            color = PrimaryAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Download Logs Button
                            TextButton(
                                onClick = {
                                    // Local hand-rolled JSON generator
                                    val sb = StringBuilder()
                                    sb.append("[\n")
                                    logs.forEachIndexed { idx, log ->
                                        sb.append("  {\n")
                                        sb.append("    \"id\": \"${log.id}\",\n")
                                        sb.append("    \"timestamp\": ${log.timestamp},\n")
                                        sb.append("    \"formattedTime\": \"${log.formattedTime}\",\n")
                                        sb.append("    \"module\": \"${log.module}\",\n")
                                        sb.append("    \"level\": \"${log.level}\",\n")
                                        val escMessage = log.message.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
                                        sb.append("    \"message\": \"$escMessage\",\n")
                                        val escDetails = log.details?.replace("\\", "\\\\")?.replace("\"", "\\\"")?.replace("\n", "\\n")
                                        sb.append("    \"details\": ${if (escDetails == null) "null" else "\"$escDetails\""},\n")
                                        sb.append("    \"latencyMs\": ${log.latencyMs ?: "null"}\n")
                                        sb.append("  }${if (idx < logs.size - 1) "," else ""}\n")
                                    }
                                    sb.append("]")
                                    val jsonStr = sb.toString()
                                    
                                    try {
                                        val filename = "aura_diagnostic_logs.json"
                                        val file = java.io.File(context.getExternalFilesDir(null), filename)
                                        file.writeText(jsonStr)
                                        
                                        // Copy to clipboard as quick fallback
                                        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clipData = android.content.ClipData.newPlainText("Aura Diagnostics", jsonStr)
                                        clipboardManager.setPrimaryClip(clipData)
                                        
                                        // Sharing intent
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "application/json"
                                            putExtra(Intent.EXTRA_SUBJECT, "Aura Net Diagnostics Export")
                                            putExtra(Intent.EXTRA_TEXT, jsonStr)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Share Diagnostics JSON"))
                                        
                                        android.widget.Toast.makeText(context, "Telemetry JSON exported! File saved & copied.", android.widget.Toast.LENGTH_LONG).show()
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "Export error: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
                                    }
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.testTag("download_logs_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Export Diagnostics",
                                        tint = SecondaryAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("DOWNLOAD LOGS", color = SecondaryAccent, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                }
                            }

                            TextButton(onClick = onClearLogs, contentPadding = PaddingValues(0.dp)) {
                                Text("PURGE LOGS", color = Color(0xFFFF5555), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (logs.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No telemetry logs recorded yet.", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        // Diagnostic Table Interface
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundDark)
                                .border(1.dp, BorderGrey.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        ) {
                            // Table Headers
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SurfaceDark)
                                    .padding(vertical = 8.dp, horizontal = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("TIME", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted), modifier = Modifier.weight(1.3f))
                                Text("LEVEL", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted), modifier = Modifier.weight(0.9f))
                                Text("DIAGNOSTIC TRACE", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted), modifier = Modifier.weight(3.5f))
                                Text("LATENCY", style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted), modifier = Modifier.weight(1.1f), textAlign = TextAlign.End)
                            }
                            
                            HorizontalDivider(color = BorderGrey.copy(alpha = 0.5f), thickness = 1.dp)

                            // Table Cells list
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .padding(horizontal = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                items(logs.size) { index ->
                                    val log = logs[index]
                                    val (levelBg, levelTextCol) = when(log.level) {
                                        "ERROR" -> Color(0x33FF5555) to Color(0xFFFF5555)
                                        "WARN" -> Color(0x33FFB333) to Color(0xFFFFB333)
                                        "INFO" -> Color(0x1133B3FF) to Color(0xFF33B3FF)
                                        else -> Color(0x11FFFFFF) to TextLight
                                    }
                                    val latencyColor = when {
                                        log.latencyMs == null -> TextMuted
                                        log.latencyMs > 2000L -> Color(0xFFFF5555)
                                        log.latencyMs > 800L -> Color(0xFFFFB333)
                                        else -> Color(0xFF4CAF50)
                                    }
                                    val latencyText = if (log.latencyMs != null) "${log.latencyMs}ms" else "--"
                                    var expanded by remember { mutableStateOf(false) }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(levelBg)
                                            .clickable { expanded = !expanded }
                                            .padding(vertical = 6.dp, horizontal = 6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(log.formattedTime, style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, color = TextMuted), modifier = Modifier.weight(1.3f))
                                            Text(
                                                text = log.level,
                                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 8.5.sp, fontWeight = FontWeight.Bold, color = levelTextCol),
                                                modifier = Modifier.weight(0.9f)
                                            )
                                            Text(
                                                text = log.message,
                                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.5.sp, color = TextLight),
                                                maxLines = if (expanded) Int.MAX_VALUE else 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(3.5f)
                                            )
                                            Text(
                                                text = latencyText,
                                                style = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = latencyColor),
                                                modifier = Modifier.weight(1.1f),
                                                textAlign = TextAlign.End
                                            )
                                        }

                                        if (expanded && !log.details.isNullOrBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = log.details,
                                                color = TextLight.copy(alpha = 0.8f),
                                                fontSize = 8.5.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(SurfaceDark.copy(alpha = 0.6f))
                                                    .padding(6.dp)
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = BorderGrey.copy(alpha = 0.1f), thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                // PRIVACY CONTROLS TAB
                PrivacyControlsTab(
                    notificationFrequency24h = notificationFrequency24h,
                    autoModelUpdatesEnabled = autoModelUpdatesEnabled,
                    onToggleAutoModelUpdates = onToggleAutoModelUpdates,
                    privacyFirstMode = privacyFirstMode,
                    onTogglePrivacyFirstMode = onTogglePrivacyFirstMode,
                    researchModeEnabled = researchModeEnabled,
                    onToggleResearchMode = onToggleResearchMode,
                    powerSaverEnabled = powerSaverEnabled,
                    onTogglePowerSaver = onTogglePowerSaver,
                    onManualTrigger = onManualTrigger,
                    isEncryptionActive = isEncryptionActive,
                    cryptoPassphrase = cryptoPassphrase,
                    onUpdateEncryptionSettings = onUpdateEncryptionSettings,
                    privacyInsights = privacyInsights,
                    onClearPrivacyInsights = onClearPrivacyInsights,
                    appPasscode = appPasscode,
                    isPasscodeLockEnabled = isPasscodeLockEnabled,
                    onUpdatePasscodeSettings = onUpdatePasscodeSettings,
                    autoDeleteDays = autoDeleteDays,
                    onUpdateAutoDeleteInterval = onUpdateAutoDeleteInterval,
                    isOnDeviceProcessingEnabled = isOnDeviceProcessingEnabled,
                    onToggleOnDeviceProcessing = onToggleOnDeviceProcessing,
                    dndSyncEnabled = dndSyncEnabled,
                    onToggleDndSync = onToggleDndSync,
                    isSystemDndActive = isSystemDndActive,
                    onSyncCalendar = onSyncCalendar,
                    callScreeningRules = callScreeningRules,
                    onDeleteCallScreeningRule = onDeleteCallScreeningRule,
                    onAddCallScreeningRule = onAddCallScreeningRule,
                    emailTemplates = emailTemplates,
                    onDeleteEmailTemplate = onDeleteEmailTemplate,
                    onAddEmailTemplate = onAddEmailTemplate,
                    onClearAllLocalData = onClearAllLocalData,
                    selectedVoiceProfile = selectedVoiceProfile,
                    onSelectVoiceProfile = onSelectVoiceProfile,
                    onPreviewVoiceProfile = onPreviewVoiceProfile
                )
            }
            4 -> {
                DeepWorkSubTabPanel(
                    isDeepWorkActive = isDeepWorkActive,
                    deepWorkTimeRemaining = deepWorkTimeRemaining,
                    deepWorkDurationMinutes = deepWorkDurationMinutes,
                    onToggleDeepWorkMode = onToggleDeepWorkMode,
                    onUpdateDeepWorkDuration = onUpdateDeepWorkDuration,
                    contacts = contacts,
                    onAddContact = onAddContact,
                    onDeleteContact = onDeleteContact,
                    screenedTranscripts = screenedTranscripts,
                    onDeleteScreenedTranscript = onDeleteScreenedTranscript,
                    onClearScreenedTranscripts = onClearScreenedTranscripts,
                    getDecryptedText = getDecryptedText,
                    onExportPdfReport = onExportPdfReport
                )
            }
            5 -> {
                // SOFTWARE UPDATES TAB PANEL
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundDark, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                        .testTag("updates_tab_panel")
                ) {
                    Text(
                        text = "KANNA AI SYSTEM UPGRADE ENGINE",
                        color = SecondaryAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Verify signature protocols and download encrypted code release modules directly from Kanna release centers.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Version status Box
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceDark)
                            .border(1.dp, BorderGrey, RoundedCornerShape(6.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Current Active Version:", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text("v$currentVersion", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Remote Manifest Version:", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text(
                                    text = "v$availableVersion",
                                    color = if (isUpdateAvailable) PrimaryAccent else SecondaryAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = BorderGrey, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Status: $updateStatusMessage",
                                color = if (isUpdateAvailable) PrimaryAccent else TextMuted,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Check for Updates button
                        OutlinedButton(
                            onClick = onCheckForUpdates,
                            enabled = !isCheckingForUpdates,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryAccent),
                            border = BorderStroke(1.dp, if (isCheckingForUpdates) BorderGrey else SecondaryAccent),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("check_updates_btn")
                        ) {
                            Text(
                                text = if (isCheckingForUpdates) "CHECKING..." else "CHECK UPDATES",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Update Now button
                        Button(
                            onClick = onApplyUpdateNow,
                            enabled = isUpdateAvailable && !isCheckingForUpdates,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isUpdateAvailable) PrimaryAccent else BorderGrey
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("update_now_btn")
                        ) {
                            Text(
                                text = "UPDATE NOW",
                                color = if (isUpdateAvailable) BackgroundDark else TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    HorizontalDivider(color = BorderGrey, thickness = 1.dp)

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "VERSION INSTALLATION LOGS",
                        color = SecondaryAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (versionInstallations.isEmpty()) {
                        Text(
                            text = "No installation logs recorded in local secure DB storage. Current running build v$currentVersion initialized successfully.",
                            color = TextMuted,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("version_history_list")
                        ) {
                            versionInstallations.forEach { installation ->
                                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(
                                    java.util.Date(installation.timestamp)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SurfaceDark)
                                        .border(1.dp, BorderGrey, RoundedCornerShape(6.dp))
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Success",
                                            tint = SecondaryAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "v${installation.version}",
                                            color = TextLight,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = dateStr,
                                        color = TextMuted,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
            6 -> {
                // AI ACTION HISTORY TAB
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BackgroundDark, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                        .testTag("ai_action_history_panel")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "AURA SECURE AI ACTION RECORD",
                            color = SecondaryAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)
                        )
                        if (aiActionHistory.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { exportJsonLauncher.launch("aura_ai_history.json") },
                                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, SecondaryAccent),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp).testTag("export_ai_history_json_btn")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Export JSON", tint = SecondaryAccent, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("JSON", color = SecondaryAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Button(
                                    onClick = { exportCsvLauncher.launch("aura_ai_history.csv") },
                                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, SecondaryAccent),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp).testTag("export_ai_history_csv_btn")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = SecondaryAccent, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("CSV", color = SecondaryAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                                Button(
                                    onClick = onClearAiActionHistory,
                                    colors = ButtonDefaults.buttonColors(containerColor = AlertOrange.copy(alpha = 0.15f)),
                                    border = BorderStroke(1.dp, AlertOrange),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp).testTag("clear_ai_history_btn")
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Clear all", tint = AlertOrange, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("CLEAR ALL", color = AlertOrange, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Your localized AI interaction history is compiled securely on-device with zero cloud propagation.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // --- Logging controls: master switch + per-category opt-outs ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(SurfaceDark)
                            .border(1.dp, BorderGrey, RoundedCornerShape(6.dp))
                            .padding(12.dp)
                            .testTag("ai_logging_controls")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "RECORD AI ACTIONS",
                                    color = SecondaryAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Master switch for on-device AI action logging. Disable specific categories below.",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Switch(
                                checked = aiLoggingEnabled,
                                onCheckedChange = { onToggleAiLogging(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = SecondaryAccent,
                                    checkedTrackColor = SecondaryAccent.copy(alpha = 0.4f),
                                    uncheckedThumbColor = BorderGrey,
                                    uncheckedTrackColor = SurfaceDark
                                ),
                                modifier = Modifier.testTag("ai_logging_master_switch")
                            )
                        }

                        if (aiLoggingEnabled) {
                            HorizontalDivider(
                                color = BorderGrey,
                                thickness = 1.dp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            AiActionCategory.entries.forEach { category ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = category.label,
                                        color = TextLight,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = aiLoggingCategoryEnabled[category] ?: true,
                                        onCheckedChange = { onToggleAiLoggingCategory(category, it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = SecondaryAccent,
                                            checkedTrackColor = SecondaryAccent.copy(alpha = 0.4f),
                                            uncheckedThumbColor = BorderGrey,
                                            uncheckedTrackColor = SurfaceDark
                                        ),
                                        modifier = Modifier.testTag("ai_logging_cat_${category.name}")
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- Search + category filter (only when there is history to filter) ---
                    if (aiActionHistory.isNotEmpty()) {
                        OutlinedTextField(
                            value = aiHistorySearchQuery,
                            onValueChange = onAiHistorySearchQueryChange,
                            label = { Text("Search prompts & responses", color = TextMuted, fontFamily = FontFamily.Monospace, fontSize = 11.sp) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextMuted, modifier = Modifier.size(16.dp)) },
                            trailingIcon = {
                                if (aiHistorySearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onAiHistorySearchQueryChange("") }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear search", tint = TextMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryAccent,
                                unfocusedBorderColor = BorderGrey,
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("ai_history_search_field")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .testTag("ai_history_filter_chips"),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // "All" chip clears the category filter.
                            AiHistoryFilterChip(
                                label = "All",
                                selected = aiHistoryCategoryFilter == null,
                                onClick = { onAiHistoryCategoryFilterChange(null) }
                            )
                            AiActionCategory.entries.forEach { category ->
                                AiHistoryFilterChip(
                                    label = category.label,
                                    selected = aiHistoryCategoryFilter == category,
                                    onClick = {
                                        onAiHistoryCategoryFilterChange(
                                            if (aiHistoryCategoryFilter == category) null else category
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (aiActionHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceDark)
                                .border(1.dp, BorderGrey, RoundedCornerShape(6.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No logged AI action records available.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else if (filteredAiActionHistory.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(6.dp))
                                .background(SurfaceDark)
                                .border(1.dp, BorderGrey, RoundedCornerShape(6.dp))
                                .padding(16.dp)
                                .testTag("ai_history_no_matches"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No actions match your search or filter.",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("ai_history_list")
                        ) {
                            filteredAiActionHistory.forEach { action ->
                                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(
                                    java.util.Date(action.timestamp)
                                )
                                var isExpanded by remember { mutableStateOf(false) }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                                    border = BorderStroke(1.dp, if (isExpanded) PrimaryAccent.copy(alpha = 0.5f) else BorderGrey),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isExpanded = !isExpanded }
                                        .testTag("ai_action_item_${action.id}")
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(4.dp))
                                                            .background(PrimaryAccent.copy(alpha = 0.15f))
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = action.actionType.uppercase(),
                                                            color = PrimaryAccent,
                                                            fontSize = 8.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            fontFamily = FontFamily.Monospace
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = dateStr,
                                                        color = TextMuted,
                                                        fontSize = 9.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }

                                            IconButton(
                                                onClick = { onDeleteAiAction(action.id) },
                                                modifier = Modifier.size(24.dp).testTag("delete_ai_action_${action.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete entry",
                                                    tint = TextMuted,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(
                                            text = "Input: " + (if (action.inputPrompt.length > 80 && !isExpanded) action.inputPrompt.take(80) + "..." else action.inputPrompt),
                                            color = TextLight,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace,
                                            lineHeight = 15.sp
                                        )

                                        if (isExpanded) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Divider(color = BorderGrey, thickness = 0.5.dp)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = "AURA RESPONSE:",
                                                color = SecondaryAccent,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = action.generatedResponse,
                                                color = TextLight.copy(alpha = 0.85f),
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                lineHeight = 14.sp
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Output: " + (if (action.generatedResponse.length > 80) action.generatedResponse.take(80) + "..." else action.generatedResponse),
                                                color = TextMuted,
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                lineHeight = 14.sp
                                            )
                                            Text(
                                                text = "Tap to expand response",
                                                color = SecondaryAccent,
                                                fontSize = 8.sp,
                                                fontFamily = FontFamily.Monospace,
                                                modifier = Modifier.padding(top = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConnectionErrorComponent(
    errorMessage: String?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1E1E)), // Dark red tinted surface
        border = BorderStroke(1.dp, Color(0xFFFF5555)), // High-contrast red accent
        modifier = modifier
            .fillMaxWidth()
            .testTag("connection_error_component")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Connection Failure",
                    tint = Color(0xFFFF5555),
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "AURA SECURE CORE GATEWAY OFFLINE",
                    color = Color(0xFFFF8888),
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }
            
            Text(
                text = "The secure neural model API request failed. Voice processing, email compilation, and predictive commands are temporarily unavailable.",
                color = Color(0xFFE2D6D6),
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontFamily = FontFamily.SansSerif
            )
            
            if (!errorMessage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF1E1414))
                        .border(0.5.dp, Color(0xFF422828), RoundedCornerShape(6.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFFB3B3),
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5555)),
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .testTag("connection_retry_button"),
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RE-INITIALIZE TELEMETRY HANDSHAKE",
                        color = Color.White,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

// --- SCREEN 1: AURA COMMAND CENTRE ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CommandCentreScreen(
    messages: List<ChatMessageEntity>,
    pending: Boolean,
    highThinking: Boolean,
    onSendMessage: (String) -> Unit,
    onClearChat: () -> Unit,
    geminiStatus: com.example.data.diagnostics.ServiceStatus,
    lastErrorMessage: String?,
    onRetryConnection: () -> Unit
) {
    var rawText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val kb = LocalSoftwareKeyboardController.current

    // Auto scroll down
    LaunchedEffect(messages.size, pending) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Real-time Pulsing Network Health Widget
        val infiniteTransition = rememberInfiniteTransition(label = "pulse")
        val pulseAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        val pulseScale by infiniteTransition.animateFloat(
            initialValue = 0.8f,
            targetValue = 1.3f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )

        val geminiStatus by com.example.data.diagnostics.AuraDiagnostics.geminiStatus.collectAsStateWithLifecycle()
        val statusText = when(geminiStatus) {
            com.example.data.diagnostics.ServiceStatus.CONNECTED -> "AURA CONNECTED"
            com.example.data.diagnostics.ServiceStatus.TESTING -> "AURA HANDSHAKING..."
            com.example.data.diagnostics.ServiceStatus.FAILED -> "AURA ACCESS BLOCKED"
            com.example.data.diagnostics.ServiceStatus.UNTESTED -> "AURA UNINITIALIZED"
            else -> "AURA DEGRADED"
        }
        val statusColor = when(geminiStatus) {
            com.example.data.diagnostics.ServiceStatus.CONNECTED -> Color(0xFF4CAF50)
            com.example.data.diagnostics.ServiceStatus.TESTING -> Color(0xFFFFB333)
            com.example.data.diagnostics.ServiceStatus.FAILED -> Color(0xFFFF5555)
            com.example.data.diagnostics.ServiceStatus.UNTESTED -> TextMuted
            else -> TextMuted
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundDark)
                .border(0.5.dp, BorderGrey.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("network_health_widget"),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pulse indicator
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(12.dp * pulseScale)
                            .clip(CircleShape)
                            .background(statusColor.copy(alpha = pulseAlpha * 0.4f))
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "GEMINI CORE GATEWAY",
                        color = TextLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
            }
            
            Text(
                text = "REALTIME TELEMETRY",
                color = TextMuted,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Chat Console Space
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages) { msg ->
                    AuraConsoleBubble(msg)
                }

                if (pending) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(8.dp)
                        ) {
                            CircularProgressIndicator(
                                color = PrimaryAccent,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = if (highThinking) "Aura Reasoning Engaged..." else "Aura processing...",
                                color = PrimaryAccent,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // Floating clean action
            IconButton(
                onClick = onClearChat,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark.copy(alpha = 0.8f))
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Purge Core History",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (geminiStatus == com.example.data.diagnostics.ServiceStatus.FAILED) {
            ConnectionErrorComponent(
                errorMessage = lastErrorMessage,
                onRetry = onRetryConnection,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // Action Quick Triggers for voice / prompt simulations
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val suggestions = listOf(
                "Summarize daily logs",
                "Draft weekly LinkedIn update",
                "Verify secure sandbox status"
            )
            suggestions.forEach { suggestion ->
                Box(
                    modifier = Modifier
                        .background(SurfaceDark, RoundedCornerShape(16.dp))
                        .clickable { onSendMessage(suggestion) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = suggestion, color = SecondaryAccent, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        }

        // Input Core Bar
        Surface(
            color = SurfaceDark,
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.ime)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = rawText,
                    onValueChange = { rawText = it },
                    placeholder = { Text("Query Aura System Console...", color = TextMuted, fontSize = 13.sp) },
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = BorderGrey,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("aura_chat_input"),
                    textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (rawText.isNotBlank()) {
                            onSendMessage(rawText)
                            rawText = ""
                            kb?.hide()
                        }
                    })
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (rawText.isNotBlank()) {
                            onSendMessage(rawText)
                            rawText = ""
                            kb?.hide()
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryAccent)
                        .testTag("aura_chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Dispatch prompt",
                        tint = BackgroundDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AuraConsoleBubble(msg: ChatMessageEntity) {
    val isUser = msg.sender == "USER"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(0.85f),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Text(
                text = if (isUser) "AUTHORIZED SIGNATORY" else "AURA CORE COMMAND",
                color = if (isUser) SecondaryAccent else PrimaryAccent,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(bottom = 3.dp)
            )

            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 12.dp
                        )
                    )
                    .background(if (isUser) SurfaceDark else BorderGrey.copy(alpha = 0.5f))
                    .border(
                        BorderStroke(
                            1.dp,
                            if (isUser) SecondaryAccent.copy(alpha = 0.3f) else PrimaryAccent.copy(alpha = 0.3f)
                        ),
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isUser) 12.dp else 2.dp,
                            bottomEnd = if (isUser) 2.dp else 12.dp
                        )
                    )
                    .padding(14.dp)
            ) {
                Text(
                    text = msg.text,
                    color = TextLight,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

// --- SCREEN 2: ALERTS INTERCEPTOR ---
@Composable
fun InterceptorScreen(
    notifications: List<NotificationEntity>,
    processingId: Int?,
    isPermissionActive: Boolean,
    onGrantPermission: () -> Unit,
    onAnalyze: (NotificationEntity) -> Unit,
    onSendReply: (NotificationEntity, String?) -> Unit,
    onDelete: (Int) -> Unit,
    onSimulate: (String, String, String) -> Unit,
    callSessions: List<com.example.data.db.CallSessionEntity> = emptyList()
) {
    var showSimulator by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            InterceptionDashboard(notifications = notifications, callSessions = callSessions)
        }

        // Warning if live notification listeners are not granted
        if (!isPermissionActive) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AlertOrange.copy(alpha = 0.15f)),
                    border = BorderStroke(1.2.dp, AlertOrange),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = "Security Alert", tint = AlertOrange)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SYSTEM ACCESS OFFLINE",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Aura requires system-level Notification Listener permission to intercept, read, and summarize notifications on your behalf.",
                            color = TextLight,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onGrantPermission,
                            colors = ButtonDefaults.buttonColors(containerColor = AlertOrange)
                        ) {
                            Text("GRANT PERMISSION ACCESS", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        // Live notification summary feed
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "INTERCEPTED PHONE NOTIFICATIONS",
                        color = TextLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Encrypted local device sandbox feed",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = { showSimulator = !showSimulator },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                ) {
                    Icon(
                        imageVector = if (showSimulator) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Simulator trigger",
                        tint = SecondaryAccent
                    )
                }
            }
        }

        if (showSimulator) {
            item {
                NotificationSimulatorCard(
                    onSimulateDrop = { app, title, text ->
                        onSimulate(app, title, text)
                        showSimulator = false
                    }
                )
            }
        }

        if (notifications.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, BorderGrey), RoundedCornerShape(12.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Lock, contentDescription = "Secured", tint = TextMuted, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No Intercepted Notifications in Database",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        } else {
            items(notifications) { item ->
                NotificationConsoleCard(
                    item = item,
                    isProcessing = processingId == item.id,
                    onAnalyze = { onAnalyze(item) },
                    onSendReply = { onSendReply(item, it) },
                    onDelete = { onDelete(item.id) }
                )
            }
        }
    }
}

@Composable
fun NotificationConsoleCard(
    item: NotificationEntity,
    isProcessing: Boolean,
    onAnalyze: () -> Unit,
    onSendReply: (String?) -> Unit,
    onDelete: () -> Unit
) {
    var rawCustomDraft by remember(item.replyDraft) { mutableStateOf(item.replyDraft) }
    var isEditingDraft by remember { mutableStateOf(false) }

    val iconVector = when {
        item.packageName.contains("whatsapp") -> Icons.Default.Send
        item.packageName.contains("instagram") -> Icons.Default.AccountCircle
        item.packageName.contains("twitter") -> Icons.Default.Star
        item.packageName.contains("linkedin") -> Icons.Default.Share
        else -> Icons.Default.Email
    }
    val sourceTitle = when {
        item.packageName.contains("whatsapp") -> "WHATSAPP INTERCEPTED"
        item.packageName.contains("instagram") -> "INSTAGRAM ACTION"
        item.packageName.contains("twitter") -> "X / TWITTER LOG"
        item.packageName.contains("linkedin") -> "LINKEDIN FEED"
        else -> "ALERT DECODER"
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderGrey),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(iconVector, contentDescription = "App Source", tint = SecondaryAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = sourceTitle,
                        color = SecondaryAccent,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (item.status == "DEALT") SecondaryAccent.copy(alpha = 0.2f) else BorderGrey,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.status,
                            color = if (item.status == "DEALT") SecondaryAccent else TextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Purge notification", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body
            Text(
                text = "${item.title}:",
                color = TextLight,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.text,
                color = TextLight.copy(alpha = 0.9f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Generated summaries/replies section
            if (item.summary.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, BorderGrey), RoundedCornerShape(8.dp))
                        .background(BackgroundDark)
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = "Aura Logic", tint = PrimaryAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AURA SECURE DECODER SUMMARY",
                                color = PrimaryAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = item.summary,
                            color = TextLight,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        if (item.replyDraft.isNotBlank()) {
                            HorizontalDivider(color = BorderGrey, modifier = Modifier.padding(vertical = 10.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SYNTHESIZED SANDBOX REPLY DRAFT",
                                    color = SecondaryAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                IconButton(
                                    onClick = { isEditingDraft = !isEditingDraft },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isEditingDraft) Icons.Default.Check else Icons.Default.Edit,
                                        contentDescription = "Edit Draft",
                                        tint = TextMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (isEditingDraft && item.status != "DEALT") {
                                OutlinedTextField(
                                    value = rawCustomDraft,
                                    onValueChange = { rawCustomDraft = it },
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = TextLight),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SecondaryAccent,
                                        unfocusedBorderColor = BorderGrey
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    text = rawCustomDraft.ifBlank { item.replyDraft },
                                    color = TextLight.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            if (item.status != "DEALT") {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { onSendReply(rawCustomDraft.ifBlank { item.replyDraft }) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "APPROVE & DISPATCH RESPONSE",
                                        color = BackgroundDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = PrimaryAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Decoding with Aura Logic...", color = PrimaryAccent, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onAnalyze,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, PrimaryAccent)
                    ) {
                        Text(
                            text = "AURA SECURE DECODE",
                            color = PrimaryAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationSimulatorCard(onSimulateDrop: (String, String, String) -> Unit) {
    var appChoice by remember { mutableStateOf("WHATSAPP") }
    var title by remember { mutableStateOf("") }
    var bodyText by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.2.dp, SecondaryAccent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "INTERRUPT SIMULATOR CONSOLE",
                color = SecondaryAccent,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Selector app Choice
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("WHATSAPP", "INSTAGRAM", "TWITTER", "LINKEDIN", "GMAIL").forEach { choice ->
                    val selected = appChoice == choice
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (selected) SecondaryAccent else BorderGrey,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { appChoice = choice }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = choice,
                            color = if (selected) BackgroundDark else TextLight,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("App Alert Title (e.g. Sender Name)", color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SecondaryAccent, unfocusedBorderColor = BorderGrey),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = bodyText,
                onValueChange = { bodyText = it },
                label = { Text("Notification body content", color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SecondaryAccent, unfocusedBorderColor = BorderGrey),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (title.isNotBlank() && bodyText.isNotBlank()) {
                        onSimulateDrop(appChoice, title, bodyText)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "TRIGGER SECURE PHONE EVENT",
                    color = BackgroundDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// --- SCREEN 3: POSTMASTER MAIL SCREEN ---
@Composable
fun PostmasterMailScreen(
    emails: List<EmailEntity>,
    processingId: Int?,
    onAnalyze: (EmailEntity) -> Unit,
    onSendReply: (EmailEntity, String) -> Unit,
    onDelete: (Int) -> Unit,
    onSimulate: (String, String, String) -> Unit,
    emailTemplates: List<EmailTemplateEntity> = emptyList()
) {
    var showMailCreator by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "POSTMASTER ENCRYPTED MAIL HUB",
                        color = TextLight,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Zero-cloud secure automated draft center",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }

                IconButton(
                    onClick = { showMailCreator = !showMailCreator },
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceDark)
                ) {
                    Icon(
                        imageVector = if (showMailCreator) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Simulate mail",
                        tint = PrimaryAccent
                    )
                }
            }
        }

        if (showMailCreator) {
            item {
                MailSimulatorCard(
                    onSimulateIncoming = { sender, subtitle, body ->
                        onSimulate(sender, subtitle, body)
                        showMailCreator = false
                    }
                )
            }
        }

        if (emails.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, BorderGrey), RoundedCornerShape(12.dp))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Email, contentDescription = "Inbox safe", tint = TextMuted, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Mail Hub Database Clean",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        } else {
            items(emails) { email ->
                EmailHubCard(
                    email = email,
                    isProcessing = processingId == email.id,
                    onAnalyze = { onAnalyze(email) },
                    onSendReply = { onSendReply(email, it) },
                    onDelete = { onDelete(email.id) },
                    emailTemplates = emailTemplates
                )
            }
        }
    }
}

@Composable
fun EmailHubCard(
    email: EmailEntity,
    isProcessing: Boolean,
    onAnalyze: () -> Unit,
    onSendReply: (String) -> Unit,
    onDelete: () -> Unit,
    emailTemplates: List<EmailTemplateEntity> = emptyList()
) {
    var rawDraftInput by remember(email.replyDraft) { mutableStateOf(email.replyDraft) }
    var isEditing by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.dp, BorderGrey),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccountBox, contentDescription = "Sender", tint = PrimaryAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = email.sender,
                        color = PrimaryAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (email.status == "REPLIED") SecondaryAccent.copy(alpha = 0.2f) else BorderGrey,
                                shape = RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = email.status,
                            color = if (email.status == "REPLIED") SecondaryAccent else TextMuted,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Discard Mail", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Content
            Text(
                text = email.subject,
                color = TextLight,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = email.body,
                color = TextLight.copy(alpha = 0.8f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            // AI summary & draft pane
            if (email.summary.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, BorderGrey), RoundedCornerShape(8.dp))
                        .background(BackgroundDark)
                        .padding(12.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = "Aura summary", tint = PrimaryAccent, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AURA MAIL SUMMARY LOG",
                                color = PrimaryAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = email.summary,
                            color = TextLight,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )

                        // ML Sentiment classification & suggested templates selection
                        val sentimentClassifier = remember(email.subject, email.body) {
                            val text = "${email.subject} ${email.body}".lowercase()
                            when {
                                text.contains("urgent") || text.contains("immediate") || text.contains("asap") ||
                                text.contains("critical") || text.contains("emergency") || text.contains("timeline") ||
                                text.contains("deadline") || text.contains("action required") || text.contains("break") -> "URGENT ⚡"
                                
                                text.contains("angry") || text.contains("unhappy") || text.contains("disappointed") ||
                                text.contains("fail") || text.contains("error") || text.contains("issue") ||
                                text.contains("resign") || text.contains("defect") || text.contains("leave") ||
                                text.contains("problem") -> "NEGATIVE 😡"
                                
                                text.contains("thank") || text.contains("great") || text.contains("awesome") ||
                                text.contains("delighted") || text.contains("happy") || text.contains("perfect") ||
                                text.contains("pleased") || text.contains("success") || text.contains("solved") -> "POSITIVE 😊"
                                
                                text.contains("how") || text.contains("what") || text.contains("where") ||
                                text.contains("query") || text.contains("question") || text.contains("inquiry") ||
                                text.contains("ask") || text.contains("explain") -> "INQUIRY ℹ️"
                                
                                else -> "NEUTRAL 💬"
                            }
                        }

                        val themeColor = remember(sentimentClassifier) {
                            when {
                                sentimentClassifier.startsWith("URGENT") -> Color(0xFFFF5555)
                                sentimentClassifier.startsWith("NEGATIVE") -> Color(0xFFFF8888)
                                sentimentClassifier.startsWith("POSITIVE") -> Color(0xFF55FF55)
                                sentimentClassifier.startsWith("INQUIRY") -> Color(0xFF55FFFF)
                                else -> Color(0xFFFFFF55)
                            }
                        }

                        HorizontalDivider(color = BorderGrey, modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(themeColor, RoundedCornerShape(1.dp))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "SENTIMENT LOG: $sentimentClassifier",
                                    color = themeColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.testTag("email_sentiment_badge")
                                )
                            }
                        }

                        val suggestedTemplates = remember(sentimentClassifier, emailTemplates) {
                            emailTemplates.filter { template ->
                                when {
                                    sentimentClassifier.startsWith("URGENT") -> template.category.lowercase() == "urgent"
                                    sentimentClassifier.startsWith("POSITIVE") -> template.category.lowercase() == "personal"
                                    else -> template.category.lowercase() == "work"
                                }
                            }.ifEmpty { emailTemplates.take(2) }
                        }

                        if (suggestedTemplates.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "AURA RECON: SYSTEM RECOMMENDED RESPONSES",
                                color = TextMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                suggestedTemplates.forEach { template ->
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(SurfaceDark)
                                            .border(0.5.dp, SecondaryAccent.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                            .clickable {
                                                rawDraftInput = template.content
                                                isEditing = true
                                            }
                                            .padding(8.dp)
                                            .testTag("apply_template_${template.id}")
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = template.name.uppercase() + " [${template.category.uppercase()}]",
                                                color = SecondaryAccent,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .background(SecondaryAccent.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "APPLY TEMPLATE",
                                                    color = SecondaryAccent,
                                                    fontSize = 7.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = template.content,
                                            color = TextLight.copy(alpha = 0.7f),
                                            fontSize = 11.sp,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        if (email.replyDraft.isNotBlank()) {
                            HorizontalDivider(color = BorderGrey, modifier = Modifier.padding(vertical = 10.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SECURE FORMAL REPLY SCHEME",
                                    color = SecondaryAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                IconButton(
                                    onClick = { isEditing = !isEditing },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                        contentDescription = "Edit Draft",
                                        tint = TextMuted,
                                        modifier = Modifier.size(12.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            if (isEditing && email.status != "REPLIED") {
                                OutlinedTextField(
                                    value = rawDraftInput,
                                    onValueChange = { rawDraftInput = it },
                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = TextLight, fontFamily = FontFamily.Monospace),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = SecondaryAccent,
                                        unfocusedBorderColor = BorderGrey
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    text = rawDraftInput.ifBlank { email.replyDraft },
                                    color = TextLight.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            if (email.status != "REPLIED") {
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = { onSendReply(rawDraftInput.ifBlank { email.replyDraft }) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "DISPATCH AUTHORIZED AUTO-RESPONSE",
                                        color = BackgroundDark,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            } else if (isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = PrimaryAccent, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Summarizing and compiling drafts...", color = PrimaryAccent, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onAnalyze,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent.copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, PrimaryAccent)
                    ) {
                        Text(
                            text = "AURA DECODE & FORMULATE REPLY",
                            color = PrimaryAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MailSimulatorCard(onSimulateIncoming: (String, String, String) -> Unit) {
    var sender by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = BorderStroke(1.2.dp, PrimaryAccent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "INCOMING EMAIL SANDBOX PRODUCER",
                color = PrimaryAccent,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = sender,
                onValueChange = { sender = it },
                label = { Text("Sender address (e.g. boss@corp.com)", color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryAccent, unfocusedBorderColor = BorderGrey),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = subject,
                onValueChange = { subject = it },
                label = { Text("Email Subject Line", color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryAccent, unfocusedBorderColor = BorderGrey),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Email body content", color = TextMuted) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryAccent, unfocusedBorderColor = BorderGrey),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (sender.isNotBlank() && subject.isNotBlank() && body.isNotBlank()) {
                        onSimulateIncoming(sender, subject, body)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SEED INCOMING MAIL TO PIPELINE",
                    color = BackgroundDark,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun KannaAiCabinScreen(
    profile: UserProfileEntity,
    secureFiles: List<SecureFileEntity>,
    socialPosts: List<SocialPostEntity>,
    isVoiceActive: Boolean,
    voiceOutputText: String,
    processingSocialPostId: Int?,
    isHotwordActive: Boolean,
    isMicGranted: Boolean,
    socialTrendResult: String,
    linkedinDraftPost: String,
    isSocialProcessing: Boolean,
    meetingAnswerResult: String,
    isMeetingProcessing: Boolean,
    isRepresentedInMeeting: Boolean,
    activeMeetingTranscript: String,
    onToggleHotword: (Boolean) -> Unit,
    onProcessVoice: (String) -> Unit,
    onCreateFile: (String, String) -> Unit,
    onDeleteFile: (Int) -> Unit,
    onAnalyzePost: (SocialPostEntity) -> Unit,
    onPublishPost: (SocialPostEntity, String) -> Unit,
    onSimulatePost: (String, String, String) -> Unit,
    onDeletePost: (Int) -> Unit,
    onSimulateCall: (String) -> Unit,
    onRunSocialAnalysis: (String, String) -> Unit,
    onDraftLinkedIn: (String) -> Unit,
    onQueryMeeting: (String, String) -> Unit,
    onToggleMeetingJoin: (Boolean) -> Unit,
    onUpdateMeetingTranscript: (String) -> Unit,
    geminiStatus: com.example.data.diagnostics.ServiceStatus,
    lastErrorMessage: String?,
    onRetryConnection: () -> Unit
) {
    var speechInputSim by remember { mutableStateOf("") }
    var newFileName by remember { mutableStateOf("") }
    var newFileContent by remember { mutableStateOf("") }
    var showFileCreator by remember { mutableStateOf(false) }

    var simPlatform by remember { mutableStateOf("LINKEDIN") }
    var simAuthor by remember { mutableStateOf("") }
    var simText by remember { mutableStateOf("") }
    var showPostCreator by remember { mutableStateOf(false) }

    var researchPlatform by remember { mutableStateOf("LINKEDIN") }
    var researchTopic by remember { mutableStateOf("Focus Mode and AI Agents") }
    var linkedinTopicInput by remember { mutableStateOf("Why digital noise cancels deep productivity") }

    var meetingTopicInput by remember { mutableStateOf("Quarterly Engineering Roadmap Sync") }
    var meetingQuestionInput by remember { mutableStateOf("Who is representing Chaitanya and what are the deployment milestones?") }

    // Wave Animation parameters
    var wavePhase by remember { mutableStateOf(0f) }
    LaunchedEffect(isVoiceActive) {
        if (isVoiceActive) {
            while (true) {
                wavePhase += 0.15f
                kotlinx.coroutines.delay(20)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (geminiStatus == com.example.data.diagnostics.ServiceStatus.FAILED) {
            item {
                ConnectionErrorComponent(
                    errorMessage = lastErrorMessage,
                    onRetry = onRetryConnection,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        // --- SECTION 1: HELIX KANNA COGNITIVE INTENT HUB ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.2.dp, SecondaryAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Kanna Mode",
                                tint = SecondaryAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "KANNA AI SECURE VOICE CORE",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isVoiceActive) PrimaryAccent.copy(alpha = 0.2f) else BorderGrey,
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isVoiceActive) "LISTENING" else "STANDBY",
                                color = if (isVoiceActive) PrimaryAccent else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Configured Activation Name: \"${profile.wakeWord}\". Trigger Aura instantly even when your absolute device status is locked.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(BackgroundDark)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Continuous Hotword Listener",
                                color = TextLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = if (!isMicGranted) "Requires Microphone Permission" else if (isHotwordActive) "Listening continuously..." else "Press switch to run Aura voice wake core",
                                color = if (isHotwordActive) SecondaryAccent else TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Switch(
                            checked = isHotwordActive,
                            onCheckedChange = { onToggleHotword(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = SecondaryAccent,
                                checkedTrackColor = SecondaryAccent.copy(alpha = 0.3f),
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = BorderGrey
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Pulse Voice Wave Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(BackgroundDark, RoundedCornerShape(8.dp))
                            .border(BorderStroke(1.dp, BorderGrey), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isVoiceActive) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val width = this.size.width
                                val height = this.size.height
                                val midY = height / 2f
                                val pointsCount = 100
                                val amplitude = height * 0.35f
                                val stroke1Val = 2.dp.toPx()
                                val stroke2Val = 1.dp.toPx()

                                val path = androidx.compose.ui.graphics.Path()
                                path.moveTo(0f, midY)

                                val path2 = androidx.compose.ui.graphics.Path()
                                path2.moveTo(0f, midY)

                                for (i in 0..pointsCount) {
                                    val x = (i.toFloat() / pointsCount) * width
                                    val factor = sin(i.toFloat() / pointsCount * Math.PI).toFloat() // Envelope to fade ends
                                    val y1 = midY + sin(i * 0.15f - wavePhase) * amplitude * factor
                                    val y2 = midY + sin(i * 0.25f + wavePhase * 1.5f) * amplitude * 0.6f * factor

                                    path.lineTo(x, y1)
                                    path2.lineTo(x, y2)
                                }

                                drawPath(
                                    path = path,
                                    color = SecondaryAccent,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke1Val)
                                )
                                drawPath(
                                    path = path2,
                                    color = PrimaryAccent.copy(alpha = 0.6f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke2Val)
                                )
                            }
                        } else {
                            Text(
                                text = "Speak: \"${profile.wakeWord}, compose LinkedIn reply...\"",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Simulated voice input query box
                    OutlinedTextField(
                        value = speechInputSim,
                        onValueChange = { speechInputSim = it },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = TextLight, fontFamily = FontFamily.Monospace),
                        label = { Text("Dictate or type Voice Command query...", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SecondaryAccent,
                            unfocusedBorderColor = BorderGrey
                        ),
                        placeholder = { Text("E.g., reply to Recruiter on LinkedIn with details...", color = TextMuted.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (speechInputSim.isNotBlank()) {
                                    onProcessVoice(speechInputSim)
                                    speechInputSim = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Execute", tint = BackgroundDark, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("TRANSLATE", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        Button(
                            onClick = {
                                onProcessVoice("Reply to LinkedIn recruiter telling them I am interested in the Principal Architect role.")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                            border = BorderStroke(1.dp, BorderGrey),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("SIMULATE AI WAKE", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            onProcessVoice("") // launches aura style overlay hud
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = "Launch Overlay", tint = BackgroundDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("LAUNCH AURA SECURE OVERLAY HUD", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    if (voiceOutputText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BackgroundDark, RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, BorderGrey), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "AURA SECURE VOCAL SYNTH OUTPUT:",
                                    color = PrimaryAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = voiceOutputText,
                                    color = TextLight,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            var simCallerName by remember { mutableStateOf("Recruiter Sarah") }
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.2.dp, PrimaryAccent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Telephony Intercept",
                            tint = PrimaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "AURA TELEPHONY SCREENING INTEGRATION",
                            color = TextLight,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Enable Aura to lift incoming phone calls automatically, state that she is lifting Chaitanya's call as his assistant, discuss with the caller directly, and deliver a comprehensive summary.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = simCallerName,
                        onValueChange = { simCallerName = it },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, color = TextLight, fontFamily = FontFamily.Monospace),
                        label = { Text("Simulation Caller Name", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryAccent,
                            unfocusedBorderColor = BorderGrey
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onSimulateCall(simCallerName) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SIMULATE INCOMING PHONE CALL", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // --- SECTION 2: SECURED GOOGLE DRIVE CABINET (LOCAL PRIVATE STORAGE) ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderGrey),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Local Drive",
                                tint = PrimaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "GOOGLE DRIVE SECURED CABINET",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        }

                        IconButton(onClick = { showFileCreator = !showFileCreator }) {
                            Icon(
                                imageVector = if (showFileCreator) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = "Add Local File",
                                tint = PrimaryAccent
                            )
                        }
                    }

                    Text(
                        text = "Real-time Google Drive local mirror. Any documents stored here are heavily sandbox encrypted offline-only. Completely decoupled from external cloud tracking schemas.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (showFileCreator) {
                        OutlinedTextField(
                            value = newFileName,
                            onValueChange = { newFileName = it },
                            label = { Text("File Name (e.g. Secret_Resume.pdf)", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryAccent, unfocusedBorderColor = BorderGrey),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newFileContent,
                            onValueChange = { newFileContent = it },
                            label = { Text("File Content Decrypt Body", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryAccent, unfocusedBorderColor = BorderGrey),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (newFileName.isNotBlank() && newFileContent.isNotBlank()) {
                                    onCreateFile(newFileName, newFileContent)
                                    newFileName = ""
                                    newFileContent = ""
                                    showFileCreator = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("SAVE SECURE LOCALIZED FILE", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (secureFiles.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BackgroundDark, RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No secure mirrored files active.", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            secureFiles.forEach { file ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BackgroundDark, RoundedCornerShape(8.dp))
                                        .border(BorderStroke(1.dp, BorderGrey), RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Edit, contentDescription = "File icon", tint = SecondaryAccent, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(text = file.name, color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                            Text(text = "${file.sizeStr} | Locally AES-256 Mocked Encrypted", color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Lock, contentDescription = "SECURE", tint = SecondaryAccent, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        IconButton(onClick = { onDeleteFile(file.id) }, modifier = Modifier.size(24.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "Delete Local document", tint = TextMuted, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- SECTION 3: LINKEDIN & SOCIAL PUBLISHING AUTOPILOT ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, BorderGrey),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Social Autopost",
                                tint = SecondaryAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "SOCIAL AUTOPILOT PUBLISHER",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp
                            )
                        }

                        IconButton(onClick = { showPostCreator = !showPostCreator }) {
                            Icon(
                                imageVector = if (showPostCreator) Icons.Default.Close else Icons.Default.Add,
                                contentDescription = "Simulate Social Trigger",
                                tint = SecondaryAccent
                            )
                        }
                    }

                    Text(
                        text = "Connects on-device listeners to auto-reply and publish status outputs directly on LinkedIn, Instagram, or FaceBook with 1-click manual secure confirmation.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (showPostCreator) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf("LINKEDIN", "INSTAGRAM", "TWITTER/X").forEach { platform ->
                                Button(
                                    onClick = { simPlatform = platform },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (simPlatform == platform) SecondaryAccent else SurfaceDark
                                    ),
                                    border = BorderStroke(1.dp, if (simPlatform == platform) SecondaryAccent else BorderGrey),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(text = platform, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (simPlatform == platform) BackgroundDark else TextLight)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = simAuthor,
                            onValueChange = { simAuthor = it },
                            label = { Text("App Alert Context or Sender", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SecondaryAccent, unfocusedBorderColor = BorderGrey),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = simText,
                            onValueChange = { simText = it },
                            label = { Text("Original Alert or Target text", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SecondaryAccent, unfocusedBorderColor = BorderGrey),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (simAuthor.isNotBlank() && simText.isNotBlank()) {
                                    onSimulatePost(simPlatform, simAuthor, simText)
                                    simAuthor = ""
                                    simText = ""
                                    showPostCreator = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("SEED SIMULATED INTERCEPT", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    if (socialPosts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BackgroundDark, RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No pending social media alert drafts.", color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            socialPosts.forEach { post ->
                                val isProcessing = processingSocialPostId == post.id
                                var finalDraftText by remember(post.replyDraft) { mutableStateOf(post.replyDraft) }
                                var isEditing by remember { mutableStateOf(false) }

                                Card(
                                    colors = CardDefaults.cardColors(containerColor = BackgroundDark),
                                    border = BorderStroke(1.dp, BorderGrey),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            color = if (post.platform == "LINKEDIN") Color(0xFF0a66c2).copy(alpha = 0.2f) else Color(0xFFc13584).copy(alpha = 0.2f),
                                                            shape = RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(text = post.platform, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (post.platform == "LINKEDIN") Color(0xFF0a66c2) else Color(0xFFc13584))
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(text = post.title, color = TextLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .background(
                                                            color = if (post.status == "PUBLISHED") Color.Green.copy(alpha = 0.15f) else BorderGrey,
                                                            shape = RoundedCornerShape(4.dp)
                                                        )
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(text = post.status, color = if (post.status == "PUBLISHED") Color.Green else TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                                }
                                                Spacer(modifier = Modifier.width(6.dp))
                                                IconButton(onClick = { onDeletePost(post.id) }, modifier = Modifier.size(24.dp)) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Dismiss", tint = TextMuted, modifier = Modifier.size(13.dp))
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Text(text = post.text, color = TextLight.copy(alpha = 0.8f), fontSize = 11.sp, lineHeight = 15.sp)

                                        if (post.replyDraft.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            HorizontalDivider(color = BorderGrey)
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(text = "AURA SMART SOCIAL FORMULATION:", color = SecondaryAccent, fontSize = 9.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                                                IconButton(onClick = { isEditing = !isEditing }, modifier = Modifier.size(20.dp)) {
                                                    Icon(
                                                        imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Edit,
                                                        contentDescription = "Edit Draft",
                                                        tint = TextMuted,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))

                                            if (isEditing && post.status != "PUBLISHED") {
                                                OutlinedTextField(
                                                    value = finalDraftText,
                                                    onValueChange = { finalDraftText = it },
                                                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = TextLight, fontFamily = FontFamily.Monospace),
                                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = SecondaryAccent, unfocusedBorderColor = BorderGrey),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            } else {
                                                Text(
                                                    text = finalDraftText,
                                                    color = TextLight,
                                                    fontSize = 11.sp,
                                                    lineHeight = 15.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }

                                            if (post.status != "PUBLISHED") {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Button(
                                                    onClick = { onPublishPost(post, finalDraftText) },
                                                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("CONFIRM SECURE PUBLISH LIVE", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                                }
                                            }
                                        } else if (isProcessing) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            CircularProgressIndicator(color = SecondaryAccent, modifier = Modifier.size(14.dp))
                                        } else {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Button(
                                                onClick = { onAnalyzePost(post) },
                                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent.copy(alpha = 0.12f)),
                                                border = BorderStroke(1.dp, SecondaryAccent),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text("DECODE ALERT & COMPOSE RESPONSE", color = SecondaryAccent, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.2.dp, SecondaryAccent),
                modifier = Modifier.fillMaxWidth().testTag("social_researcher_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Social Automator",
                            tint = SecondaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "KANNA SOCIAL AUTOMATOR & RESEARCH",
                            color = TextLight,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Real-time automated analytics to research trending posts or videos, format accompanying poster descriptions, and construct the absolute best copy-paste metrics comment.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("LINKEDIN", "INSTAGRAM").forEach { platform ->
                            Button(
                                onClick = { researchPlatform = platform },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (researchPlatform == platform) SecondaryAccent else BackgroundDark
                                ),
                                border = BorderStroke(1.dp, if (researchPlatform == platform) SecondaryAccent else BorderGrey),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(text = platform, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (researchPlatform == platform) BackgroundDark else TextLight)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = researchTopic,
                        onValueChange = { researchTopic = it },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = TextLight, fontFamily = FontFamily.Monospace),
                        label = { Text("Trending Topic/Keyword", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SecondaryAccent,
                            unfocusedBorderColor = BorderGrey
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onRunSocialAnalysis(researchPlatform, researchTopic) },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                        modifier = Modifier.fillMaxWidth().testTag("run_social_analysis_btn")
                    ) {
                        if (isSocialProcessing) {
                            CircularProgressIndicator(color = BackgroundDark, modifier = Modifier.size(16.dp))
                        } else {
                            Text("RESEARCH TRENDS & GENERATE COMMENT", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    if (socialTrendResult.isNotBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BackgroundDark, RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, SecondaryAccent.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "⚡ KANNA RESEARCH FORMULATIONS:",
                                        color = SecondaryAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "COPY",
                                        color = TextMuted,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.clickable {
                                            // Simulated copy action
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = socialTrendResult,
                                    color = TextLight,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = BorderGrey.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "🚀 Kanna Smart LinkedIn Post Compiler",
                        color = TextLight,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = linkedinTopicInput,
                        onValueChange = { linkedinTopicInput = it },
                        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = TextLight, fontFamily = FontFamily.Monospace),
                        label = { Text("What should the LinkedIn post be about?", color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SecondaryAccent,
                            unfocusedBorderColor = BorderGrey
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onDraftLinkedIn(linkedinTopicInput) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        modifier = Modifier.fillMaxWidth().testTag("draft_linkedin_btn")
                    ) {
                        if (isSocialProcessing) {
                            CircularProgressIndicator(color = BackgroundDark, modifier = Modifier.size(16.dp))
                        } else {
                            Text("COMPILE FULL LINKEDIN POST", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                    }

                    if (linkedinDraftPost.isNotBlank()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BackgroundDark, RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.4f)), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "📝 DRAFTED CAMPAIGN BLUEPRINT:",
                                    color = PrimaryAccent,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = linkedinDraftPost,
                                    color = TextLight,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        // --- SOCIAL CALENDAR & PERFORMANCE METRICS GATEWAY ---
                        Spacer(modifier = Modifier.height(14.dp))
                        
                        Divider(color = PrimaryAccent.copy(alpha = 0.2f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = "📅 KANNA SMART SOCIAL CALENDAR",
                            color = PrimaryAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Suggestions optimized by historical comment interest data",
                            color = TextMuted,
                            fontSize = 8.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        var selectedSlotIndex by remember { mutableStateOf(0) }
                        val optimalSlots = listOf(
                            Triple("Thursday 2:30 PM", "210% Reach Multiplier", "Best for deep engineering threads"),
                            Triple("Tuesday 9:15 AM", "154% Reach Multiplier", "Best for product launch blueprints"),
                            Triple("Monday 5:45 PM", "112% Reach Multiplier", "Best for career progression insights")
                        )
                        
                        Column {
                            optimalSlots.forEachIndexed { idx, slot ->
                                val isSelected = selectedSlotIndex == idx
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) PrimaryAccent.copy(alpha = 0.08f) else SurfaceDark.copy(alpha = 0.4f))
                                        .border(BorderStroke(1.dp, if (isSelected) PrimaryAccent else BorderGrey.copy(alpha = 0.3f)), RoundedCornerShape(6.dp))
                                        .clickable { selectedSlotIndex = idx }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(CircleShape)
                                                .background(if (isSelected) PrimaryAccent else Color.Gray.copy(alpha = 0.5f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(Icons.Default.Check, contentDescription = "Active", tint = BackgroundDark, modifier = Modifier.size(10.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(text = slot.first, color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            Text(text = slot.third, color = TextMuted, fontSize = 8.sp)
                                        }
                                    }
                                    
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSelected) Color(0x3334D399) else Color.DarkGray)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = slot.second,
                                            color = if (isSelected) Color.Green else TextLight,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        var calendarSuccessMsg by remember { mutableStateOf("") }
                        
                        Button(
                            onClick = {
                                val chosenSlot = optimalSlots[selectedSlotIndex]
                                onSimulatePost("LINKEDIN", "Scheduled LinkedIn Topic: $linkedinTopicInput", linkedinDraftPost)
                                calendarSuccessMsg = "SUCCESS: LinkedIn campaign auto-locked for ${chosenSlot.first}! (Projected reach: ${chosenSlot.second})"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                            modifier = Modifier.fillMaxWidth().height(36.dp).testTag("action_schedule_slot_btn")
                        ) {
                            Icon(Icons.Default.DateRange, contentDescription = "Schedule icon", tint = BackgroundDark, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SCHEDULE DRAFT FOR ${optimalSlots[selectedSlotIndex].first.uppercase()}", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                        
                        if (calendarSuccessMsg.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.Green.copy(alpha = 0.15f))
                                    .border(BorderStroke(1.dp, Color.Green.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = Color.Green, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = calendarSuccessMsg, color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                        
                        // --- LINKEDIN METRICS & COMMENT ENGAGEMENT DATA CHART ---
                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = PrimaryAccent.copy(alpha = 0.2f), thickness = 1.dp)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = "📈 KANNA LINKEDIN PERFORMANCE DISCOURSES",
                            color = SecondaryAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Real-time engagement impact ratios with Kanna helper active",
                            color = TextMuted,
                            fontSize = 8.sp,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf(
                                Pair("Comment Rate", "15.1% (+12%)"),
                                Pair("Weekly Post Reach", "5,420 (+345%)"),
                                Pair("Sentiment Quality", "92% Positive")
                            ).forEach { stat ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 4.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SurfaceDark.copy(alpha = 0.6f))
                                        .border(BorderStroke(1.dp, BorderGrey.copy(alpha = 0.5f)), RoundedCornerShape(6.dp))
                                        .padding(6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = stat.first, color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                    Text(text = stat.second, color = PrimaryAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BackgroundDark)
                                .border(BorderStroke(1.dp, BorderGrey.copy(alpha = 0.3f)), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            androidx.compose.foundation.Canvas(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                val chartWidth = size.width
                                val chartHeight = size.height
                                
                                drawLine(
                                    color = Color.DarkGray,
                                    start = androidx.compose.ui.geometry.Offset(0f, chartHeight - 15f),
                                    end = androidx.compose.ui.geometry.Offset(chartWidth, chartHeight - 15f),
                                    strokeWidth = 1f
                                )
                                drawLine(
                                    color = Color.DarkGray,
                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                    end = androidx.compose.ui.geometry.Offset(0f, chartHeight),
                                    strokeWidth = 1f
                                )
                                
                                val reachDataPoints = listOf(1500f, 2100f, 3800f, 5420f)
                                val maxReachValue = 6000f
                                val path = androidx.compose.ui.graphics.Path()
                                
                                reachDataPoints.forEachIndexed { idx, value ->
                                    val x = (chartWidth / (reachDataPoints.size - 1)) * idx
                                    val y = chartHeight - 15f - ((value / maxReachValue) * (chartHeight - 30f))
                                    if (idx == 0) {
                                        path.moveTo(x, y)
                                    } else {
                                        path.lineTo(x, y)
                                    }
                                }
                                
                                val fillPath = androidx.compose.ui.graphics.Path().apply {
                                    addPath(path)
                                    lineTo(chartWidth, chartHeight - 15f)
                                    lineTo(0f, chartHeight - 15f)
                                    close()
                                }
                                
                                drawPath(
                                    path = fillPath,
                                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                        colors = listOf(PrimaryAccent.copy(alpha = 0.25f), Color.Transparent)
                                    )
                                )
                                
                                drawPath(
                                    path = path,
                                    color = PrimaryAccent,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                                )
                                
                                val engagementRates = listOf(2.5f, 6.4f, 11.2f, 15.1f)
                                val maxRate = 20f
                                val barWidth = 18f
                                
                                engagementRates.forEachIndexed { idx, rate ->
                                    val x = (chartWidth / (engagementRates.size - 1)) * idx - (barWidth / 2f)
                                    val y = chartHeight - 15f - ((rate / maxRate) * (chartHeight - 30f))
                                    
                                    drawRect(
                                        color = SecondaryAccent.copy(alpha = 0.85f),
                                        topLeft = androidx.compose.ui.geometry.Offset(x, y),
                                        size = androidx.compose.ui.geometry.Size(barWidth, chartHeight - 15f - y)
                                    )
                                    
                                    drawCircle(
                                        color = Color.White,
                                        radius = 2.5f,
                                        center = androidx.compose.ui.geometry.Offset(x + (barWidth / 2f), y)
                                    )
                                }
                            }
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Week 1", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            Text("Week 2 (Pure Kanna)", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            Text("Week 3 (Tone custom)", color = TextMuted, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                            Text("Active Session", color = PrimaryAccent, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.2.dp, PrimaryAccent),
                modifier = Modifier.fillMaxWidth().testTag("meeting_automator_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Meeting Automator",
                                tint = PrimaryAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CORPORATE MEETINGS REPRESENTATION",
                                color = TextLight,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = isRepresentedInMeeting,
                            onCheckedChange = { onToggleMeetingJoin(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PrimaryAccent,
                                checkedTrackColor = PrimaryAccent.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.scale(0.8f).testTag("anytime_kanna_toggle")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Anytime Kanna Active. Enable Kanna to check schedules, access secure credentials, attend video meetings instead of Chaitanya, record live transcription logs, and serve as the absolute representing node.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontFamily = FontFamily.Monospace
                    )

                    if (isRepresentedInMeeting) {
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = BorderGrey.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "🎙️ LIVE TRANSCRIBED MEETING PROTOCOL:",
                            color = PrimaryAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = activeMeetingTranscript,
                            onValueChange = { onUpdateMeetingTranscript(it) },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, color = TextLight, fontFamily = FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryAccent,
                                unfocusedBorderColor = BorderGrey
                            ),
                            modifier = Modifier.fillMaxWidth().height(100.dp),
                            maxLines = 6
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = meetingTopicInput,
                            onValueChange = { meetingTopicInput = it },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = TextLight, fontFamily = FontFamily.Monospace),
                            label = { Text("Meeting Context/Topic Label", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryAccent, unfocusedBorderColor = BorderGrey),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = meetingQuestionInput,
                            onValueChange = { meetingQuestionInput = it },
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = TextLight, fontFamily = FontFamily.Monospace),
                            label = { Text("Ask Kanna a Question regarding this meeting", color = TextMuted) },
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryAccent, unfocusedBorderColor = BorderGrey),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onQueryMeeting(meetingTopicInput, meetingQuestionInput) },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                            modifier = Modifier.fillMaxWidth().testTag("query_meeting_btn")
                        ) {
                            if (isMeetingProcessing) {
                                CircularProgressIndicator(color = BackgroundDark, modifier = Modifier.size(16.dp))
                            } else {
                                Text("QUERY MEETING REPRESENTATIVE DATA", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }
                        }

                        if (meetingAnswerResult.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BackgroundDark, RoundedCornerShape(8.dp))
                                    .border(BorderStroke(1.dp, PrimaryAccent.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "💬 KANNA AUDIT LOG ANALYSIS:",
                                        color = PrimaryAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = meetingAnswerResult,
                                        color = TextLight,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AuraCallScreenHUD(
    isIncoming: Boolean,
    isCallActive: Boolean,
    callerName: String,
    callerStatus: String,
    transcripts: List<ChatMessageEntity>,
    onDecline: () -> Unit,
    onAnswer: () -> Unit,
    onSendMessage: (String) -> Unit,
    onEndCall: () -> Unit
) {
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val bgColor = if (isSystemDark) Color(0xF20F172A) else Color(0xF2F1F5F9)
    val textPrimary = if (isSystemDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSecondary = if (isSystemDark) Color(0xFF94A3B8) else Color(0xFF475569)
    val cardBg = if (isSystemDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    val accentColor = if (isSystemDark) SecondaryAccent else Color(0xFF8B5CF6)
    val dynamicBorderGrey = if (isSystemDark) BorderGrey else Color(0xFFD1D5DB)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .clickable(enabled = false) {}
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isIncoming) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Animated pulsating call icon
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(96.dp)
                        .background(Color.Green.copy(alpha = 0.15f), shape = androidx.compose.foundation.shape.CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Incoming Call",
                        tint = Color.Green,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = callerName,
                    color = textPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.SansSerif
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "AURA SECURE TELEPHONY AGENT CHANNELS",
                    color = textSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Incoming Call State: $callerStatus",
                    color = accentColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .background(cardBg, RoundedCornerShape(12.dp))
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Aura will lift this call instead of Chaitanya. Aura will introduce herself, talk politely, and transcribe everything for your review.",
                        color = textSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.SansSerif
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = onDecline,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier.height(56.dp).weight(1f).padding(horizontal = 12.dp).testTag("decline_call_btn")
                    ) {
                        Text("DECLINE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Button(
                        onClick = onAnswer,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(32.dp),
                        modifier = Modifier.height(56.dp).weight(1.2f).padding(horizontal = 12.dp).testTag("answer_call_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Answer", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LET AURA ANSWER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        } else if (isCallActive) {
            var customReplyText by remember { mutableStateOf("") }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
            ) {
                // Header status
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Active call", tint = accentColor, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "LIVE SCREENING: $callerName",
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = callerStatus,
                                color = accentColor,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable dialogue transcripts
                val listState = rememberLazyListState()
                LaunchedEffect(transcripts.size) {
                    if (transcripts.isNotEmpty()) {
                        listState.animateScrollToItem(transcripts.size - 1)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .background(cardBg.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transcripts) { msg ->
                        val isAura = msg.sender == "Aura"
                        val bubbleBg = if (isAura) {
                            if (isSystemDark) Color(0xFF6366F1) else Color(0xFFE0E7FF)
                        } else {
                            if (isSystemDark) Color(0xFF334155) else Color(0xFFF1F5F9)
                        }
                        val bubbleText = if (isAura) {
                            if (isSystemDark) Color.White else Color(0xFF1E1B4B)
                        } else {
                            if (isSystemDark) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                        }

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = if (isAura) Alignment.End else Alignment.Start
                        ) {
                            Text(
                                text = msg.sender,
                                color = textSecondary,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                            Box(
                                modifier = Modifier
                                    .background(bubbleBg, RoundedCornerShape(16.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .widthIn(max = 260.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    color = bubbleText,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Caller Speaking tool (Simulation HUD helper)
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "🎭 CALLER SPEECH SIMULATOR (USER)",
                            color = textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Play as the caller to test Aura's conversation screening directly:",
                            color = textSecondary,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Dialog preset fast suggestions
                        val prompts = listOf(
                            "Hey! Tell Chaitanya server is down!",
                            "Can Chaitanya do a zoom call today?",
                            "Urgent billing failure alert!"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            prompts.forEach { p ->
                                Button(
                                    onClick = { onSendMessage(p) },
                                    colors = ButtonDefaults.buttonColors(containerColor = bgColor),
                                    modifier = Modifier.weight(1f).height(38.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(2.dp)
                                ) {
                                    Text(
                                        text = p,
                                        color = accentColor,
                                        fontSize = 8.sp,
                                        lineHeight = 10.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customReplyText,
                                onValueChange = { customReplyText = it },
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, color = textPrimary),
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, unfocusedBorderColor = dynamicBorderGrey),
                                label = { Text("As Caller: Say...", color = textSecondary, fontSize = 10.sp) },
                                modifier = Modifier.weight(1f).height(50.dp),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (customReplyText.isNotBlank()) {
                                        onSendMessage(customReplyText)
                                        customReplyText = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(50.dp)
                            ) {
                                Text("SPEAK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // End call controls
                Button(
                    onClick = onEndCall,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("end_call_btn")
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Hang Up", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("HANG UP & SUMMARIZE DISK", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun LockScreenOverlay(
    onUnlockAttempt: (String) -> Boolean
) {
    var pinText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF030712)) // Dark sleek background matching app theme
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Glowing Lock Icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(if (isError) Color(0x33EF4444) else Color(0x1A8B5CF6)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Lock Shield Indicator",
                    tint = if (isError) Color(0xFFEF4444) else Color(0xFF8B5CF6),
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "AURA PRIVACY GATEWAY",
                color = if (isError) Color(0xFFEF4444) else Color(0xFF8B5CF6),
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isError) "INVALID CREDENTIAL ENTRY" else "ENTER PIN TO RESTORE ACCESS",
                color = if (isError) Color(0xFFEF4444).copy(alpha = 0.8f) else TextMuted,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Progress Dots (PIN progress indicators)
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { index ->
                    val isFilled = index < pinText.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) {
                                    if (isError) Color(0xFFEF4444) else Color(0xFF8B5CF6)
                                } else {
                                    BorderGrey.copy(alpha = 0.3f)
                                }
                            )
                            .border(
                                1.5.dp,
                                if (isError) Color(0xFFEF4444) else Color(0xFF8B5CF6).copy(alpha = 0.5f),
                                CircleShape
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(50.dp))

            // Num Pad (Grid of PIN buttons)
            val numKeys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "CLR", "0", "DEL")
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                numKeys.chunked(3).forEach { rowKeys ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        rowKeys.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.2f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, BorderGrey.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        isError = false
                                        when (key) {
                                            "CLR" -> pinText = ""
                                            "DEL" -> {
                                                if (pinText.isNotEmpty()) {
                                                    pinText = pinText.dropLast(1)
                                                }
                                            }
                                            else -> {
                                                if (pinText.length < 4) {
                                                    pinText += key
                                                    if (pinText.length == 4) {
                                                        // Attempt unlock
                                                        val success = onUnlockAttempt(pinText)
                                                        if (!success) {
                                                            isError = true
                                                            pinText = ""
                                                            android.widget.Toast.makeText(context, "Incorrect PIN lock code.", android.widget.Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            android.widget.Toast.makeText(context, "Access restored.", android.widget.Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    .testTag("pin_key_$key"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = key,
                                    color = if (key == "CLR" || key == "DEL") Color(0xFFEF4444).copy(alpha = 0.8f) else TextLight,
                                    fontSize = if (key == "CLR" || key == "DEL") 12.sp else 20.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableRawText(headline: String, content: String, isEncrypted: Boolean) {
    var isExpanded by remember { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isExpanded) "COLLAPSE RAW CONVERSATION" else headline,
                color = if (isEncrypted) SecondaryAccent else PrimaryAccent,
                fontSize = 8.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "Arrow Toggle",
                tint = TextMuted,
                modifier = Modifier.size(14.dp)
            )
        }
        if (isExpanded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundDark, RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Text(
                    text = content,
                    color = TextLight,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    style = androidx.compose.ui.text.TextStyle(lineHeight = 12.sp)
                )
            }
        }
    }
}

@Composable
fun DeepWorkSubTabPanel(
    isDeepWorkActive: Boolean,
    deepWorkTimeRemaining: Long,
    deepWorkDurationMinutes: Int,
    onToggleDeepWorkMode: () -> Unit,
    onUpdateDeepWorkDuration: (Int) -> Unit,
    contacts: List<com.example.data.db.AuraContactEntity>,
    onAddContact: (String, String, String, String, Boolean) -> Unit,
    onDeleteContact: (Int) -> Unit,
    screenedTranscripts: List<com.example.data.db.ScreenedTranscriptEntity>,
    onDeleteScreenedTranscript: (Int) -> Unit,
    onClearScreenedTranscripts: () -> Unit,
    getDecryptedText: (String, Boolean) -> String,
    onExportPdfReport: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Deep Work Focus Controller Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundDark)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Deep Work Focus Gate",
                        color = TextLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Silences incoming normal alerts automatically and marks secure calendar status as 'Busy'. Only urgent notifications pass through.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = isDeepWorkActive,
                    onCheckedChange = { onToggleDeepWorkMode() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SecondaryAccent,
                        checkedTrackColor = SecondaryAccent.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("deep_work_focus_switch")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isDeepWorkActive) {
                // Active status card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark.copy(alpha = 0.8f), RoundedCornerShape(6.dp))
                        .border(1.dp, SecondaryAccent.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "FOCUS BLOCK ACTIVE",
                            color = SecondaryAccent,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        val minutes = (deepWorkTimeRemaining / 1000) / 60
                        val seconds = (deepWorkTimeRemaining / 1000) % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            color = TextLight,
                            fontSize = 28.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Non-urgent notifications are silent",
                            color = TextMuted,
                            fontSize = 9.5.sp
                        )
                    }
                }
            } else {
                // Configuration selection
                Text(
                    text = "DEFINE FOCUS DURATION",
                    color = TextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val durations = listOf(15, 25, 45, 60)
                    durations.forEach { mins ->
                        val isSel = deepWorkDurationMinutes == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) SecondaryAccent else SurfaceDark)
                                .border(1.dp, if (isSel) SecondaryAccent else BorderGrey.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .clickable { onUpdateDeepWorkDuration(mins) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$mins MIN",
                                style = TextStyle(
                                    color = if (isSel) BackgroundDark else TextLight,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                var customMinsText by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedTextField(
                        value = customMinsText,
                        onValueChange = { newVal ->
                            if (newVal.all { it.isDigit() }) {
                                customMinsText = newVal
                                newVal.toIntOrNull()?.let {
                                    if (it in 1..480) {
                                        onUpdateDeepWorkDuration(it)
                                    }
                                }
                            }
                        },
                        label = { Text("Custom Interval (Mins)", color = TextMuted, fontSize = 7.5.sp, fontFamily = FontFamily.Monospace) },
                        textStyle = TextStyle(color = TextLight, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("deep_work_custom_duration_input"),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SecondaryAccent,
                            unfocusedBorderColor = BorderGrey.copy(alpha = 0.3f),
                            focusedContainerColor = SurfaceDark,
                            unfocusedContainerColor = SurfaceDark
                        )
                    )
                    Button(
                        onClick = {
                            customMinsText.toIntOrNull()?.let {
                                if (it in 1..480) {
                                    onUpdateDeepWorkDuration(it)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.height(48.dp).testTag("deep_work_custom_duration_apply_btn")
                    ) {
                        Text("SET", color = BackgroundDark, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // 2. Contacts Tone manager Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundDark)
                .padding(12.dp)
        ) {
            Text(
                text = "AURA SECURE DIRECTORY & AI TONES",
                color = PrimaryAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = "Map target contacts to VIP, Colleague, or Personal classification parameters. Aura dynamically formats its telephone answers and auto-replies matching specific response tones.",
                color = TextMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Add contact input fields
            var newContactName by remember { mutableStateOf("") }
            var newContactPhone by remember { mutableStateOf("") }
            var newContactCategory by remember { mutableStateOf("VIP") }
            var newContactTone by remember { mutableStateOf("Enthusiastic") }
            var newContactIsPriority by remember { mutableStateOf(false) }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("REGISTER CONTACT PARAMETER", color = TextLight, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                
                OutlinedTextField(
                    value = newContactName,
                    onValueChange = { newContactName = it },
                    label = { Text("Contact Name", color = TextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = BorderGrey,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    ),
                    textStyle = TextStyle(fontSize = 11.sp),
                    modifier = Modifier.fillMaxWidth().testTag("contact_name_input")
                )

                OutlinedTextField(
                    value = newContactPhone,
                    onValueChange = { newContactPhone = it },
                    label = { Text("Phone Number Pattern (wildcards allowed)", color = TextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = BorderGrey,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight
                    ),
                    textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth().testTag("contact_phone_input")
                )

                // Tone selection Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tone Limit:", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    val toneOpts = listOf("Formal", "Casual", "Enthusiastic")
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        toneOpts.forEach { t ->
                            val isSelected = newContactTone == t
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) SecondaryAccent else SurfaceDark)
                                    .clickable { newContactTone = t }
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(t, color = if (isSelected) BackgroundDark else TextLight, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                // Category selection Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Group Classification:", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                    val catOpts = listOf("VIP", "Colleague", "Personal")
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        catOpts.forEach { c ->
                            val isSelected = newContactCategory == c
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isSelected) PrimaryAccent else SurfaceDark)
                                    .clickable { newContactCategory = c }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(c, color = if (isSelected) BackgroundDark else TextLight, fontSize = 8.5.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }

                // Priority Bypass switch row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Priority Bypass (Deep Work)", color = TextLight, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        Text("Bypass Deep Work silence mode automatically", color = TextMuted, fontSize = 8.sp)
                    }
                    Switch(
                        checked = newContactIsPriority,
                        onCheckedChange = { newContactIsPriority = it },
                        modifier = Modifier.scale(0.8f).testTag("priority_bypass_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SecondaryAccent,
                            checkedTrackColor = SecondaryAccent.copy(alpha = 0.4f)
                        )
                    )
                }

                Button(
                    onClick = {
                        if (newContactName.trim().isNotBlank() && newContactPhone.trim().isNotBlank()) {
                            onAddContact(newContactName, newContactPhone, newContactCategory, newContactTone, newContactIsPriority)
                            newContactName = ""
                            newContactPhone = ""
                            newContactIsPriority = false
                            android.widget.Toast.makeText(context, "Contact registered successfully.", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Contact fields cannot be empty.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.align(Alignment.End).testTag("add_contact_button")
                ) {
                    Text("REGISTER CONTACT", color = BackgroundDark, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Current contact entries list
            if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No parameterized contacts registered.", color = TextMuted, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    contacts.forEach { contact ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDark, RoundedCornerShape(6.dp))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(contact.name, color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (contact.category == "VIP") Color(0xFFD4AF37) else if (contact.category == "Colleague") Color(0xFF1E90FF) else Color(0xFFFF69B4))
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(contact.category, color = Color.White, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    if (contact.isPriority) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(AlertOrange)
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text("PRIORITY", color = Color.White, fontSize = 7.5.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(contact.phoneNumber, color = TextMuted, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .border(1.dp, SecondaryAccent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(contact.aiResponseTone, color = SecondaryAccent, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                IconButton(
                                    onClick = { onDeleteContact(contact.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete Contact", tint = Color.Red.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Screened Transcripts Secure Vault Section
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundDark)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SCREENED RAW TRANSCRIPT VAULT",
                    color = SecondaryAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                if (screenedTranscripts.isNotEmpty()) {
                    Text(
                        text = "PURGE VAULT",
                        color = Color.Red.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onClearScreenedTranscripts() }
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Absolute historical dialogue transcripts of automated call screen sessions and notification summaries. Stored inside encrypted local database.",
                color = TextMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (screenedTranscripts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RoundedCornerShape(6.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No secure screened transcript records mapped.", color = TextMuted, fontSize = 9.5.sp, fontFamily = FontFamily.Monospace)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    screenedTranscripts.forEach { trans ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDark, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(if (trans.type == "CALL") SecondaryAccent else PrimaryAccent)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(trans.type, color = BackgroundDark, fontSize = 8.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(trans.source, color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                IconButton(
                                    onClick = { onDeleteScreenedTranscript(trans.id) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete transcript", tint = TextMuted, modifier = Modifier.size(14.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Summary decrypted check
                            val mSummary = getDecryptedText(trans.summary, trans.isEncrypted)
                            val mTranscript = getDecryptedText(trans.transcriptText, trans.isEncrypted)

                            Text(
                                text = mSummary,
                                color = TextLight,
                                fontSize = 9.5.sp,
                                style = TextStyle(lineHeight = 13.sp)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            ExpandableRawText(
                                headline = "VIEW RAW CRYPTO TRANSLATION",
                                content = mTranscript,
                                isEncrypted = trans.isEncrypted
                            )
                        }
                    }
                }
            }
        }

        // 4. Format PDF Compilation report
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundDark)
                .padding(12.dp)
        ) {
            Text(
                text = "OFFLINE ARCHIVAL COMPILATION EXPORT",
                color = PrimaryAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Compile and package a formatted cryptographic PDF archive report detailing call screening records, intercepted warnings, and automated statistics directly to phone memory.",
                color = TextMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Button(
                onClick = {
                    onExportPdfReport()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("export_pdf_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Share, contentDescription = "PDF Report Export", tint = BackgroundDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "COMPILE & EXPORT SECURE WEEKLY PDF REPORT",
                        color = BackgroundDark,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun D3FrequencyTrackerCanvas(
    frequency: Map<Int, Int>,
    modifier: Modifier = Modifier
) {
    val points = (0..23).map { frequency[it] ?: 0 }
    val maxVal = (points.maxOrNull() ?: 0).coerceAtLeast(1)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BackgroundDark, RoundedCornerShape(8.dp))
            .border(1.dp, BorderGrey.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(12.dp)
            .testTag("d3_frequency_canvas_wrapper")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "D3-GROUNDED SECURE TELEMETRY FEED (24H)",
                    color = PrimaryAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Real-time query visualizer for intercepted and filtered alerts",
                    color = TextMuted,
                    fontSize = 9.5.sp
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(PrimaryAccent.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "LIVE FEED",
                    color = PrimaryAccent,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .testTag("d3_frequency_canvas")
        ) {
            val width = size.width
            val height = size.height
            val paddingLeft = 32f
            val paddingBottom = 40f
            val chartWidth = width - paddingLeft
            val chartHeight = height - paddingBottom

            // Draw Grid Lines (Y-Axis)
            val gridCount = 4
            for (i in 0..gridCount) {
                val y = chartHeight - (chartHeight / gridCount) * i
                val valLabel = ((maxVal.toFloat() / gridCount) * i).toInt()
                
                // Grid line
                drawLine(
                    color = BorderGrey.copy(alpha = 0.12f),
                    start = androidx.compose.ui.geometry.Offset(paddingLeft, y),
                    end = androidx.compose.ui.geometry.Offset(width, y),
                    strokeWidth = 1f
                )

                // Label
                drawContext.canvas.nativeCanvas.drawText(
                    valLabel.toString(),
                    8f,
                    y + 4f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 20f
                        typeface = android.graphics.Typeface.MONOSPACE
                    }
                )
            }

            // Draw X-Axis labels (every 3 hours)
            val colWidth = chartWidth / 24f
            for (h in 0..23 step 3) {
                val x = paddingLeft + h * colWidth
                drawContext.canvas.nativeCanvas.drawText(
                    String.format("%02d:00", h),
                    x - 18f,
                    height - 8f,
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.GRAY
                        textSize = 20f
                        typeface = android.graphics.Typeface.MONOSPACE
                    }
                )
            }

            // Draw Bezier Curve and Gradient Fill
            val path = androidx.compose.ui.graphics.Path()
            val fillPath = androidx.compose.ui.graphics.Path()

            val pList = points.mapIndexed { idx, value ->
                val x = paddingLeft + idx * colWidth
                val y = chartHeight - (value.toFloat() / maxVal) * chartHeight
                androidx.compose.ui.geometry.Offset(x, y)
            }

            if (pList.isNotEmpty()) {
                path.moveTo(pList[0].x, pList[0].y)
                fillPath.moveTo(pList[0].x, chartHeight)
                fillPath.lineTo(pList[0].x, pList[0].y)

                for (i in 1 until pList.size) {
                    val prev = pList[i - 1]
                    val curr = pList[i]
                    val controlX1 = prev.x + colWidth / 2f
                    val controlY1 = prev.y
                    val controlX2 = curr.x - colWidth / 2f
                    val controlY2 = curr.y

                    path.cubicTo(controlX1, controlY1, controlX2, controlY2, curr.x, curr.y)
                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, curr.x, curr.y)
                }

                fillPath.lineTo(pList.last().x, chartHeight)
                fillPath.close()

                // Draw translucent gradient fill
                drawPath(
                    path = fillPath,
                    brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(SecondaryAccent.copy(alpha = 0.35f), androidx.compose.ui.graphics.Color.Transparent)
                    )
                )

                // Draw main path line
                drawPath(
                    path = path,
                    color = PrimaryAccent,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                )

                // Draw circles for peak values
                pList.forEachIndexed { i, pt ->
                    if (points[i] > 0) {
                        drawCircle(
                            color = SecondaryAccent,
                            radius = 4f,
                            center = pt
                        )
                    }
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun PrivacyControlsTab(
    notificationFrequency24h: Map<Int, Int>,
    autoModelUpdatesEnabled: Boolean,
    onToggleAutoModelUpdates: (Boolean) -> Unit,
    privacyFirstMode: Boolean,
    onTogglePrivacyFirstMode: (Boolean) -> Unit,
    researchModeEnabled: Boolean,
    onToggleResearchMode: (Boolean) -> Unit,
    powerSaverEnabled: Boolean,
    onTogglePowerSaver: (Boolean) -> Unit,
    onManualTrigger: () -> Unit,
    isEncryptionActive: Boolean,
    cryptoPassphrase: String,
    onUpdateEncryptionSettings: (Boolean, String) -> Unit,
    privacyInsights: List<PrivacyInsightEntity>,
    onClearPrivacyInsights: () -> Unit,
    appPasscode: String,
    isPasscodeLockEnabled: Boolean,
    onUpdatePasscodeSettings: (String, Boolean) -> Unit,
    autoDeleteDays: Int,
    onUpdateAutoDeleteInterval: (Int) -> Unit,
    isOnDeviceProcessingEnabled: Boolean,
    onToggleOnDeviceProcessing: (Boolean) -> Unit,
    dndSyncEnabled: Boolean,
    onToggleDndSync: (Boolean) -> Unit,
    isSystemDndActive: Boolean = false,
    onSyncCalendar: () -> Unit,
    callScreeningRules: List<CallScreeningRuleEntity>,
    onDeleteCallScreeningRule: (Int) -> Unit,
    onAddCallScreeningRule: (String, String, String) -> Unit,
    emailTemplates: List<EmailTemplateEntity>,
    onDeleteEmailTemplate: (Int) -> Unit,
    onAddEmailTemplate: (String, String, String) -> Unit,
    onClearAllLocalData: () -> Unit,
    selectedVoiceProfile: String = "Kanna Classic",
    onSelectVoiceProfile: (String) -> Unit = {},
    onPreviewVoiceProfile: (String) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var encryptPassInput by remember(cryptoPassphrase) { mutableStateOf(cryptoPassphrase) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "AURA PRIVACY & ENCRYPTION HUB",
            color = PrimaryAccent,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        D3FrequencyTrackerCanvas(
            frequency = notificationFrequency24h,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // 1. Automatic definition updates toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundDark)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Auto Model Definition Updates",
                    color = TextLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Enable automatic real-time updates for AI model definitions.",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
            Switch(
                checked = autoModelUpdatesEnabled,
                onCheckedChange = { onToggleAutoModelUpdates(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SecondaryAccent,
                    checkedTrackColor = SecondaryAccent.copy(alpha = 0.4f),
                    uncheckedThumbColor = BorderGrey,
                    uncheckedTrackColor = SurfaceDark
                ),
                modifier = Modifier.testTag("auto_model_updates_toggle")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Privacy First mode Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundDark)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Privacy-First Mode (Manual Only)",
                    color = TextLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Restricts all background API polling and AI processing to manual triggers only.",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
            Switch(
                checked = privacyFirstMode,
                onCheckedChange = { onTogglePrivacyFirstMode(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SecondaryAccent,
                    checkedTrackColor = SecondaryAccent.copy(alpha = 0.4f),
                    uncheckedThumbColor = BorderGrey,
                    uncheckedTrackColor = SurfaceDark
                ),
                modifier = Modifier.testTag("privacy_first_mode_toggle")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Fact-checking Research Mode Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundDark)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Fact-Checking Research Mode",
                    color = TextLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Use Google Search grounding to verify truth values of facts in incoming emails before generating suggested answers.",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
            Switch(
                checked = researchModeEnabled,
                onCheckedChange = { onToggleResearchMode(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SecondaryAccent,
                    checkedTrackColor = SecondaryAccent.copy(alpha = 0.4f),
                    uncheckedThumbColor = BorderGrey,
                    uncheckedTrackColor = SurfaceDark
                ),
                modifier = Modifier.testTag("research_mode_toggle")
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2b. AI Power Saver setting Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundDark)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI Power Saver (Battery Throttling)",
                    color = TextLight,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Throttles background AI processing frequency to 120s during low-battery states (< 20%) to preserve device energy.",
                    color = TextMuted,
                    fontSize = 10.sp
                )
            }
            Switch(
                checked = powerSaverEnabled,
                onCheckedChange = { onTogglePowerSaver(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SecondaryAccent,
                    checkedTrackColor = SecondaryAccent.copy(alpha = 0.4f),
                    uncheckedThumbColor = BorderGrey,
                    uncheckedTrackColor = SurfaceDark
                ),
                modifier = Modifier.testTag("power_saver_toggle")
            )
        }

        if (privacyFirstMode) {
            Spacer(modifier = Modifier.height(10.dp))
            // Manual Handshake Button
            Button(
                onClick = {
                    onManualTrigger()
                    android.widget.Toast.makeText(context, "Executing manual telemetry handshake...", android.widget.Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("manual_handshake_trigger_button")
            ) {
                Text(
                    text = "EXECUTE MANUAL POLL & TELEMETRY HANDSHAKE",
                    color = BackgroundDark,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3. AES-256 On-Device Encryption Settings
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundDark)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "AES-256 Local Encryption",
                        color = TextLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Secure local API credentials using passphrase-derived keystore.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = isEncryptionActive,
                    onCheckedChange = { active ->
                        if (!active) {
                            onUpdateEncryptionSettings(false, "")
                            encryptPassInput = ""
                        } else {
                            onUpdateEncryptionSettings(true, encryptPassInput)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SecondaryAccent,
                        checkedTrackColor = SecondaryAccent.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("encryption_toggle")
                )
            }

            if (isEncryptionActive) {
                Spacer(modifier = Modifier.height(8.dp))

                var isPassVisible by remember { mutableStateOf(false) }

                OutlinedTextField(
                    value = encryptPassInput,
                    onValueChange = { encryptPassInput = it },
                    label = { Text("Crypto Passphrase", color = TextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    visualTransformation = if (isPassVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPassVisible = !isPassVisible }) {
                            Icon(
                                imageVector = if (isPassVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Toggle Passphrase Visibility",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecondaryAccent,
                        unfocusedBorderColor = BorderGrey,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight,
                        focusedLabelColor = SecondaryAccent,
                        unfocusedLabelColor = TextMuted
                    ),
                    textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("crypto_passphrase_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (encryptPassInput.trim().isEmpty()) {
                            android.widget.Toast.makeText(context, "Passphrase cannot be empty.", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            onUpdateEncryptionSettings(true, encryptPassInput)
                            android.widget.Toast.makeText(context, "AES-256 local credentials re-keyed successfully.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, SecondaryAccent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("save_encryption_button")
                ) {
                    Text(
                        text = "APPLY CRYPTO PASSPHRASE",
                        color = SecondaryAccent,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Privacy Insights Dashboard
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundDark)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PRIVACY INSIGHTS FEED",
                    color = TextLight,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                if (privacyInsights.isNotEmpty()) {
                    TextButton(
                        onClick = onClearPrivacyInsights,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = "PURGE RECOGNITION LOGS",
                            color = Color(0xFFFF5555),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            if (privacyInsights.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(SurfaceDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No private data accessed in this session.",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(privacyInsights.size) { idx ->
                        val log = privacyInsights[idx]
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDark, RoundedCornerShape(6.dp))
                                .padding(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = "Shield Indicator",
                                        tint = SecondaryAccent,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = log.appOrServiceName.uppercase(),
                                        color = SecondaryAccent,
                                        fontSize = 9.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(log.sessionTimestamp)),
                                    color = TextMuted,
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = log.dataProcessedSummary,
                                color = TextLight,
                                fontSize = 9.5.sp,
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 3.5 Passcode Lock Security Layer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundDark)
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Passcode Access Barrier",
                        color = TextLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Triggers a secure PIN code gate when launching or returning to the app.",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = isPasscodeLockEnabled,
                    onCheckedChange = { checked ->
                        if (!checked) {
                            onUpdatePasscodeSettings("", false)
                        } else {
                            onUpdatePasscodeSettings(appPasscode.ifEmpty { "1234" }, true)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SecondaryAccent,
                        checkedTrackColor = SecondaryAccent.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.testTag("passcode_barrier_switch")
                )
            }

            if (isPasscodeLockEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                var tempPin by remember { mutableStateOf(appPasscode) }

                OutlinedTextField(
                    value = tempPin,
                    onValueChange = { newVal ->
                        if (newVal.all { it.isDigit() } && newVal.length <= 4) {
                            tempPin = newVal
                        }
                    },
                    label = { Text("Define 4-Digit PIN Access Token", color = TextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecondaryAccent,
                        unfocusedBorderColor = BorderGrey,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight,
                        focusedLabelColor = SecondaryAccent,
                        unfocusedLabelColor = TextMuted
                    ),
                    textStyle = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("app_passcode_input_field")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (tempPin.length != 4) {
                            android.widget.Toast.makeText(context, "PIN code must be exactly 4 digits.", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            onUpdatePasscodeSettings(tempPin, true)
                            android.widget.Toast.makeText(context, "Access protection PIN registered.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
                    border = BorderStroke(1.dp, SecondaryAccent.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("save_passcode_button")
                ) {
                    Text(
                        text = "REGISTER PIN ACCESS SHIELD",
                        color = SecondaryAccent,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4.5 Auto-Cleanup Retention Policy
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundDark)
                .padding(12.dp)
        ) {
            Text(
                text = "AUTOMATED MEMORY SANITIZATION",
                color = TextLight,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Automatically clean caches, logs, and transcripts older than specified intervals.",
                color = TextMuted,
                fontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val options = listOf(0 to "Keep All", 7 to "7 Days", 30 to "30 Days", 90 to "90 Days")
                options.forEach { (daysValue, label) ->
                    val isSelected = autoDeleteDays == daysValue
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isSelected) SecondaryAccent else SurfaceDark)
                            .border(1.dp, if (isSelected) SecondaryAccent else BorderGrey.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .clickable {
                                onUpdateAutoDeleteInterval(daysValue)
                                android.widget.Toast.makeText(context, "Storage life cycle set to $label.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 8.dp)
                            .testTag("auto_delete_${daysValue}_days"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = TextStyle(
                                color = if (isSelected) BackgroundDark else TextLight,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4.75 Privacy Dashboard & On-Device Processing
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundDark)
                .padding(12.dp)
        ) {
            Text(
                text = "PRIVACY & ON-DEVICE ENGINE DASHBOARD",
                color = PrimaryAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = "Direct and regulate Aura's localized artificial intelligence, memory storage, and operating permissions.",
                color = TextMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Toggle for On-Device Processing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "On-Device Processing Mode",
                        color = TextLight,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isOnDeviceProcessingEnabled) 
                            "Aura runs local analysis without cloud relays where possible." 
                            else "Aura is transmitting payloads via cloud gateway endpoints.",
                        color = if (isOnDeviceProcessingEnabled) SecondaryAccent else TextMuted,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = isOnDeviceProcessingEnabled,
                    onCheckedChange = { onToggleOnDeviceProcessing(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SecondaryAccent,
                        checkedTrackColor = SecondaryAccent.copy(alpha = 0.4f),
                        uncheckedThumbColor = BorderGrey,
                        uncheckedTrackColor = SurfaceDark
                    ),
                    modifier = Modifier.testTag("on_device_processing_toggle")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Toggle for Do Not Disturb Sync
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Sync with System 'Do Not Disturb'",
                            color = TextLight,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSystemDndActive) SecondaryAccent.copy(alpha = 0.2f) else BorderGrey.copy(alpha = 0.2f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isSystemDndActive) "SYSTEM: ACTIVE 🔴" else "SYSTEM: INACTIVE 🟢",
                                color = if (isSystemDndActive) SecondaryAccent else TextMuted,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    Text(
                        text = if (dndSyncEnabled) 
                            "Automated notification screening is suspended when DND is enabled on device." 
                            else "Screening logic remains active regardless of system DND status.",
                        color = if (dndSyncEnabled) SecondaryAccent else TextMuted,
                        fontSize = 10.sp
                    )
                }
                Switch(
                    checked = dndSyncEnabled,
                    onCheckedChange = { onToggleDndSync(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SecondaryAccent,
                        checkedTrackColor = SecondaryAccent.copy(alpha = 0.4f),
                        uncheckedThumbColor = BorderGrey,
                        uncheckedTrackColor = SurfaceDark
                    ),
                    modifier = Modifier.testTag("dnd_sync_toggle")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "SYSTEM PERMISSIONS STATUS",
                color = TextLight,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            // Permissions checks
            val hasCalendar = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_CALENDAR
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            val hasMic = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.RECORD_AUDIO
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            // 1. Calendar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasCalendar) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Permission Check icon",
                        tint = if (hasCalendar) SecondaryAccent else Color(0xFFFFCC00),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Calendar Events Read Integration", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (hasCalendar) "Integrated list read and prioritized filter active." else "Priority filters rely on static limits.",
                            color = TextMuted,
                            fontSize = 9.5.sp
                        )
                    }
                }
                Button(
                    onClick = onSyncCalendar,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (hasCalendar) SurfaceDark else SecondaryAccent),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.height(28.dp).testTag("sync_calendar_button")
                ) {
                    Text(
                        text = if (hasCalendar) "SYNC CALENDAR" else "GRANT ACCESS",
                        color = if (hasCalendar) SecondaryAccent else BackgroundDark,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 2. Microphone
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (hasMic) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = "Mic Check Icon",
                        tint = if (hasMic) SecondaryAccent else Color(0xFFFFCC00),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Continuous Wake Engine (Microphone)", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = if (hasMic) "Mic listener fully configured." else "Continuous hotword recognition turned off.",
                            color = TextMuted,
                            fontSize = 9.5.sp
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (hasMic) SecondaryAccent.copy(alpha = 0.2f) else Color(0xFFFFCC00).copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (hasMic) "AUTHORIZED" else "STANDBY",
                        color = if (hasMic) SecondaryAccent else Color(0xFFFFCC00),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // CALL-SCREENING RULES CONFIGURATION MATRIX
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundDark)
                .padding(12.dp)
        ) {
            Text(
                text = "AURA CALL-SCREENING AUTOPILOT",
                color = PrimaryAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = "Define automated response actions or instant blocks based on target phone number wildcard patterns.",
                color = TextMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Synthetic voice profiles setting
            Text(
                text = "AUTOMATED SYNTHETIC VOICE PROFILE",
                color = SecondaryAccent,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = "Choose the synthesized voice profile Aura utilizes when answering and screening automated calls.",
                color = TextMuted,
                fontSize = 9.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val availableVoices = listOf("Kanna Classic", "Calm Professional", "Echo Sentinel", "Stellar Voice", "My Custom Profile")
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                availableVoices.forEach { voice ->
                    val isChosen = (selectedVoiceProfile == voice) || (selectedVoiceProfile.isEmpty() && voice == "Kanna Classic")
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isChosen) SecondaryAccent.copy(alpha = 0.15f) else SurfaceDark)
                            .border(1.dp, if (isChosen) SecondaryAccent else BorderGrey.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .clickable { onSelectVoiceProfile(voice) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("voice_profile_selection_${voice.replace(" ", "_")}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(if (isChosen) SecondaryAccent else TextMuted, shape = androidx.compose.foundation.shape.CircleShape)
                            )
                            Text(
                                text = voice,
                                color = if (isChosen) TextLight else TextMuted,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = { 
                    onPreviewVoiceProfile(selectedVoiceProfile.ifEmpty { "Kanna Classic" }) 
                },
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                shape = RoundedCornerShape(4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
                    .testTag("preview_voice_profile_button_privacy")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Preview Tone",
                        tint = BackgroundDark,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "HEAR PREVIEW: ${selectedVoiceProfile.ifEmpty { "Kanna Classic" }.uppercase()}",
                        color = BackgroundDark,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderGrey.copy(alpha = 0.15f), thickness = 0.5.dp, modifier = Modifier.padding(bottom = 12.dp))

            // List of current rules
            if (callScreeningRules.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RoundedCornerShape(6.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No screening rules registered. Global filter pass active.", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    callScreeningRules.forEach { rule ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDark, RoundedCornerShape(6.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (rule.action == "AUTO_ANSWER") SecondaryAccent.copy(alpha = 0.2f) else Color(0xFFFF5555).copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = rule.action,
                                            color = if (rule.action == "AUTO_ANSWER") SecondaryAccent else Color(0xFFFF5555),
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = rule.pattern,
                                        color = TextLight,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                if (rule.description.isNotEmpty()) {
                                    Text(
                                        text = rule.description,
                                        color = TextMuted,
                                        fontSize = 9.5.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = { onDeleteCallScreeningRule(rule.id) },
                                modifier = Modifier.size(24.dp).testTag("delete_rule_btn_${rule.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Screening Rule",
                                    tint = Color(0xFFFF5555),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Add Screening Rule Form
            Spacer(modifier = Modifier.height(10.dp))
            var rulePatternInput by remember { mutableStateOf("") }
            var ruleActionInput by remember { mutableStateOf("AUTO_ANSWER") } // AUTO_ANSWER, BLOCK
            var ruleDescInput by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text("REGISTER NEW SCREENING ACTION", color = TextLight, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = rulePatternInput,
                    onValueChange = { rulePatternInput = it },
                    label = { Text("Number Pattern (e.g. +1-555-*, 1800*)", color = TextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecondaryAccent,
                        unfocusedBorderColor = BorderGrey,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight,
                        focusedLabelColor = SecondaryAccent,
                        unfocusedLabelColor = TextMuted
                    ),
                    textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth().testTag("rule_pattern_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = ruleDescInput,
                    onValueChange = { ruleDescInput = it },
                    label = { Text("Short Description (e.g., Recruiters, Spam Bots)", color = TextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SecondaryAccent,
                        unfocusedBorderColor = BorderGrey,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight,
                        focusedLabelColor = SecondaryAccent,
                        unfocusedLabelColor = TextMuted
                    ),
                    textStyle = TextStyle(fontSize = 11.sp),
                    modifier = Modifier.fillMaxWidth().testTag("rule_desc_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("AUTO_ANSWER" to "AUTO ANSWER", "BLOCK" to "BLOCK CALL").forEach { (actionCode, actionLabel) ->
                            val actSelected = ruleActionInput == actionCode
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (actSelected) SecondaryAccent else SurfaceDark)
                                    .border(1.dp, if (actSelected) SecondaryAccent else BorderGrey.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .clickable { ruleActionInput = actionCode }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("rule_action_$actionCode"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = actionLabel,
                                    color = if (actSelected) BackgroundDark else TextLight,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (rulePatternInput.trim().isEmpty()) {
                                android.widget.Toast.makeText(context, "Number pattern cannot be empty.", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                onAddCallScreeningRule(rulePatternInput.trim(), ruleActionInput, ruleDescInput.trim())
                                rulePatternInput = ""
                                ruleDescInput = ""
                                android.widget.Toast.makeText(context, "Call-screening protection configured.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp).testTag("save_rule_button")
                    ) {
                        Text("ADD RULE", color = BackgroundDark, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // EMAIL RESPONSE TEMPLATES & SNEAK-PEEKS LIBRARY
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(BackgroundDark)
                .padding(12.dp)
        ) {
            Text(
                text = "AURA EMAIL RESPONSE SNIPPETS",
                color = PrimaryAccent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Text(
                text = "Manage customized response drafts used by Aura's Postmaster engine to draft answers instantly based on priority.",
                color = TextMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Category filter tabs
            var selectedCategoryFilter by remember { mutableStateOf("ALL") }
            val categories = listOf("ALL", "WORK", "PERSONAL", "URGENT")

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                categories.forEach { cat ->
                    val activeF = selectedCategoryFilter == cat
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (activeF) PrimaryAccent else SurfaceDark)
                            .border(1.dp, if (activeF) PrimaryAccent else BorderGrey.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                            .clickable { selectedCategoryFilter = cat }
                            .padding(vertical = 6.dp)
                            .testTag("filter_template_$cat"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = cat,
                            color = if (activeF) BackgroundDark else TextLight,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Filter list
            val displayedTemplates = remember(emailTemplates, selectedCategoryFilter) {
                if (selectedCategoryFilter == "ALL") emailTemplates
                else emailTemplates.filter { it.category.uppercase() == selectedCategoryFilter }
            }

            if (displayedTemplates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RoundedCornerShape(6.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No template snippets in this category.", color = TextMuted, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    displayedTemplates.forEach { template ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(SurfaceDark, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(PrimaryAccent.copy(alpha = 0.2f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = template.category.uppercase(),
                                            color = PrimaryAccent,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = template.name,
                                        color = TextLight,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteEmailTemplate(template.id) },
                                    modifier = Modifier.size(24.dp).testTag("delete_template_btn_${template.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Email Template",
                                        tint = Color(0xFFFF5555),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = template.content,
                                color = TextMuted,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Add Email Template Form
            Spacer(modifier = Modifier.height(12.dp))
            var tempNameInput by remember { mutableStateOf("") }
            var tempCategoryInput by remember { mutableStateOf("WORK") } // WORK, PERSONAL, URGENT
            var tempContentInput by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                    .padding(8.dp)
            ) {
                Text("DRAFT CUSTOM RESPONSE PRESET", color = TextLight, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = tempNameInput,
                    onValueChange = { tempNameInput = it },
                    label = { Text("Response Label (e.g. Meeting Decline, Out of Office)", color = TextMuted, fontSize = 11.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = BorderGrey,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight,
                        focusedLabelColor = PrimaryAccent,
                        unfocusedLabelColor = TextMuted
                    ),
                    textStyle = TextStyle(fontSize = 11.sp),
                    modifier = Modifier.fillMaxWidth().testTag("temp_name_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = tempContentInput,
                    onValueChange = { tempContentInput = it },
                    label = { Text("Email Response Template Content Snippet", color = TextMuted, fontSize = 11.sp) },
                    minLines = 2,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryAccent,
                        unfocusedBorderColor = BorderGrey,
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight,
                        focusedLabelColor = PrimaryAccent,
                        unfocusedLabelColor = TextMuted
                    ),
                    textStyle = TextStyle(fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                    modifier = Modifier.fillMaxWidth().testTag("temp_content_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("WORK", "PERSONAL", "URGENT").forEach { catSym ->
                            val activeCatS = tempCategoryInput == catSym
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (activeCatS) PrimaryAccent else SurfaceDark)
                                    .border(1.dp, if (activeCatS) PrimaryAccent else BorderGrey.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .clickable { tempCategoryInput = catSym }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                    .testTag("temp_category_$catSym"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = catSym,
                                    color = if (activeCatS) BackgroundDark else TextLight,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (tempNameInput.trim().isEmpty() || tempContentInput.trim().isEmpty()) {
                                android.widget.Toast.makeText(context, "Label and Snippet contents cannot be empty.", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                onAddEmailTemplate(tempNameInput.trim(), tempContentInput.trim(), tempCategoryInput)
                                tempNameInput = ""
                                tempContentInput = ""
                                android.widget.Toast.makeText(context, "Email preset snippet enrolled.", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(28.dp).testTag("save_template_button")
                    ) {
                        Text("SAVE PRESET", color = BackgroundDark, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 5. Complete Hard Storage Sanitizer Wipe
        Button(
            onClick = {
                onClearAllLocalData()
                android.widget.Toast.makeText(context, "All localized credentials, chat records, transcripts and cached system logs zeroed successfully.", android.widget.Toast.LENGTH_LONG).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAA1111)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("clear_all_local_data_button")
        ) {
            Text(
                text = "SHRED ALL PERSISTED STORES & CALIBRATION CACHES",
                color = Color.White,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
