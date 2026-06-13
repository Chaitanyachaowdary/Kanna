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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.db.AppDatabase
import com.example.data.db.ChatMessageEntity
import com.example.data.db.NotificationEntity
import com.example.data.db.EmailEntity
import com.example.data.db.UserProfileEntity
import com.example.data.db.SecureFileEntity
import com.example.data.db.SocialPostEntity
import com.example.data.db.CallSessionEntity
import com.example.data.db.CalendarEventEntity
import com.example.data.repository.JobHunterRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.JobHunterViewModel
import com.example.ui.viewmodel.JobHunterViewModelFactory
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
        val repository = JobHunterRepository(database.dao())

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
fun AuraAppContent(repository: JobHunterRepository) {
    val vm: JobHunterViewModel = viewModel(
        factory = JobHunterViewModelFactory(repository)
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
    val isSiriOverlayVisible by vm.isSiriOverlayVisible.collectAsStateWithLifecycle()

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

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vm.initPreferencesAndAudio(context)
    }

    var activeTab by remember { mutableStateOf(0) } // 0: Command Centre, 1: Interceptor Feed, 2: Postmaster Mail Hub, 3: Siri & Drive Cabin
    var showProfileConfig by remember { mutableStateOf(false) }
    var isPermissionActive by remember { mutableStateOf(false) }

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

    // Periodically sync permission check during active composition
    LaunchedEffect(Unit) {
        val cn = ComponentName(context, AuraNotificationService::class.java)
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        isPermissionActive = flat != null && flat.contains(cn.flattenToString())
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Siri & Drive") },
                    label = { Text("Siri & Drive", fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1) },
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
                onLockClick = { showProfileConfig = !showProfileConfig },
                onToggleHighThinking = { vm.toggleHighThinking(it) }
            )

            HorizontalDivider(color = BorderGrey, thickness = 1.dp)

            if (showProfileConfig) {
                ProfileConfigurationPanel(
                    profile = activeProfile ?: UserProfileEntity(),
                    onClose = { showProfileConfig = false },
                    onSave = { name, email, autoReply, secLevel, wakeWord, lockscreen ->
                        vm.saveProfile(name, email, autoReply, secLevel, wakeWord, lockscreen)
                        showProfileConfig = false
                    }
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
                        onClearChat = { vm.clearChat() }
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
                        onSimulate = { app, title, text -> vm.simulateNotificationDrop(app, title, text) }
                    )
                    2 -> PostmasterMailScreen(
                        emails = localMails,
                        processingId = processingEmailId,
                        onAnalyze = { vm.analyzeEmail(it) },
                        onSendReply = { email, txt -> vm.dispatchEmailResponse(email, txt) },
                        onDelete = { vm.deleteEmailItem(it) },
                        onSimulate = { sender, subtitle, body -> vm.simulateEmailIncoming(sender, subtitle, body) }
                    )
                    3 -> SiriAndDriveScreen(
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
                        onUpdateMeetingTranscript = { vm.updateMeetingTranscriptPreset(it) }
                    )
                }
            }
        }
    }

    AnimatedVisibility(
        visible = isSiriOverlayVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        SiriOverlayHUD(
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
            onDismiss = { vm.setSiriOverlayVisible(false) },
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
            onTriggerWebScrapeTask = { vm.triggerWebScrapeTask() },
            onApproveScrapedComment = { vm.approveScrapedComment(it) },
            onRejectScrapedComment = { vm.rejectScrapedComment(it) },
            onGenerateLinkedInPostWithGraphic = { vm.generateLinkedInPostWithGraphic(it) }
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
}
}

@Composable
fun SiriOverlayHUD(
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
    onTriggerWebScrapeTask: () -> Unit,
    onApproveScrapedComment: (Int) -> Unit,
    onRejectScrapedComment: (Int) -> Unit,
    onGenerateLinkedInPostWithGraphic: (String) -> Unit
) {
    var rawInputText by remember { mutableStateOf("") }
    var hudTabSelected by remember { mutableStateOf(0) } // 0: ALERTS, 1: SECURE MEETINGS, 2: SOCIAL AUDITING
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
                            val voiceProfiles = listOf("Kanna Classic", "Calm Professional", "Echo Sentinel", "Stellar Voice")
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
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
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
                                    Box(
                                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(6.dp)).background(BackgroundDark).padding(8.dp)
                                    ) {
                                        Column {
                                            Text("🧠 AI SESSION SUMMARY:", color = PrimaryAccent, fontSize = 9.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                            Text(text = event.summary, color = TextLight, fontSize = 11.sp, lineHeight = 15.sp)
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
fun HeaderBlock(
    highThinking: Boolean,
    profile: UserProfileEntity,
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
                    .background(SecondaryAccent)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "AURA SECURE SYSTEM",
                    color = TextLight,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
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
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Security Parameters",
                    tint = SecondaryAccent,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ProfileConfigurationPanel(
    profile: UserProfileEntity,
    onClose: () -> Unit,
    onSave: (String, String, Boolean, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(profile.userName) }
    var email by remember { mutableStateOf(profile.userEmail) }
    var autoReply by remember { mutableStateOf(profile.autoReplyEnabled) }
    var secLevel by remember { mutableStateOf(profile.securityLevel) }
    var sysWakeWord by remember { mutableStateOf(profile.wakeWord) }
    var lockscreenActive by remember { mutableStateOf(profile.lockscreenActivationEnabled) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceDark)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AURA SECURE PARAMETERS",
                color = SecondaryAccent,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = "Close settings", tint = TextMuted)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

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
            modifier = Modifier.fillMaxWidth()
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
            label = { Text("Microphone Wake Word Name (e.g. Aura, Siri)", color = TextMuted) },
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
                onClick = { onSave(name, email, autoReply, secLevel, sysWakeWord, lockscreenActive) },
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryAccent)
            ) {
                Text("APPLY TO CORE", color = BackgroundDark, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
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
    onClearChat: () -> Unit
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
    onSimulate: (String, String, String) -> Unit
) {
    var showSimulator by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
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
    onSimulate: (String, String, String) -> Unit
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
                    onDelete = { onDelete(email.id) }
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
    onDelete: () -> Unit
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
fun SiriAndDriveScreen(
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
    onUpdateMeetingTranscript: (String) -> Unit
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
        // --- SECTION 1: HELIX SIRI COGNITIVE INTENT HUB ---
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
                                contentDescription = "Siri Mode",
                                tint = SecondaryAccent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "AURA SECURE VOICE WAKE CORE",
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
                            onProcessVoice("") // launches siri style overlay hud
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Refresh, contentDescription = "Launch Overlay", tint = BackgroundDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("LAUNCH SIRI SECURE OVERLAY HUD", color = BackgroundDark, fontWeight = FontWeight.Bold, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
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
