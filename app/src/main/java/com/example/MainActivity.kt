package com.example

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MunnaTab
import com.example.ui.MunnaViewModel
import com.example.ui.components.InAppWebViewerDialog
import com.example.ui.screens.AiChatScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BrandCyanGlow
import com.example.ui.theme.BrandRoyalBlue
import com.example.ui.theme.IceWhiteBackground
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NavyTextPrimary
import com.example.ui.theme.NavyTextSecondary
import com.example.ui.theme.PureWhiteSurface
import com.example.ui.theme.SoftSkyBlueCard
import com.example.voice.MicStatus
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MunnaAssistantApp()
            }
        }
    }
}

@Composable
fun MunnaAssistantApp(
    viewModel: MunnaViewModel = viewModel()
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val showLoginScreen by viewModel.showLoginScreen.collectAsStateWithLifecycle()
    val userDisplayName by viewModel.userDisplayName.collectAsStateWithLifecycle()
    val activeInAppWebFallback by viewModel.activeInAppWebFallback.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()
    val lastFeedback by viewModel.lastFeedback.collectAsStateWithLifecycle()
    val recentHistory by viewModel.recentHistory.collectAsStateWithLifecycle()
    val allHistory by viewModel.allHistory.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val isChatLoading by viewModel.isChatLoading.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isTtsSpeaking.collectAsStateWithLifecycle()
    val isFloatingServiceRunning by viewModel.isFloatingServiceRunning.collectAsStateWithLifecycle()
    val inAppFloatingVisible by viewModel.inAppFloatingBubbleVisible.collectAsStateWithLifecycle()
    val recentNotifications by viewModel.recentNotifications.collectAsStateWithLifecycle()
    val permVersion by viewModel.permissionStatusVersion.collectAsStateWithLifecycle()

    var pendingChatMicRequest by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onMicPermissionResult(granted, forChatScreen = pendingChatMicRequest)
    }

    val systemSpeechDialogLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull { it.isNotBlank() }
            viewModel.onSystemSpeechDialogResult(spoken, forChatScreen = pendingChatMicRequest)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionsStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val isMicGranted = remember(permVersion) { viewModel.isMicPermissionGranted() }
    val isOverlayGranted = remember(permVersion) { viewModel.isOverlayPermissionGranted() }
    val isNotificationGranted = remember(permVersion) { viewModel.isNotificationAccessGranted() }

    val triggerMicAction: (Boolean) -> Unit = { forChat ->
        pendingChatMicRequest = forChat
        if (viewModel.isMicPermissionGranted()) {
            viewModel.startMicListening(forChatScreen = forChat)
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val launchSystemSpeechPopup: (Boolean) -> Unit = { forChat ->
        pendingChatMicRequest = forChat
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, settings.language.speechLocale)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "बोलो भाई, मुन्ना सुन रहा है…")
            }
            systemSpeechDialogLauncher.launch(intent)
        } catch (_: Exception) {
            if (viewModel.isMicPermissionGranted()) {
                viewModel.startDirectHardwareMicListening(forChatScreen = forChat)
            } else {
                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    if (showLoginScreen) {
        LoginScreen(
            onContinueSuccess = { chosenName ->
                viewModel.onLoginCompleted(chosenName)
            }
        )
        return
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = IceWhiteBackground,
            contentWindowInsets = WindowInsets.safeDrawing,
            bottomBar = {
                Surface(
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    color = PureWhiteSurface,
                    shadowElevation = 10.dp
                ) {
                    NavigationBar(
                        containerColor = PureWhiteSurface,
                        tonalElevation = 0.dp,
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        NavigationBarItem(
                            selected = currentTab == MunnaTab.HOME,
                            onClick = { viewModel.selectTab(MunnaTab.HOME) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MunnaTab.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                                    contentDescription = "होम"
                                )
                            },
                            label = {
                                Text(
                                    text = "होम",
                                    fontWeight = if (currentTab == MunnaTab.HOME) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandRoyalBlue,
                                selectedTextColor = BrandRoyalBlue,
                                indicatorColor = SoftSkyBlueCard,
                                unselectedIconColor = NavyTextSecondary,
                                unselectedTextColor = NavyTextSecondary
                            ),
                            modifier = Modifier.testTag("nav_tab_home")
                        )

                        NavigationBarItem(
                            selected = currentTab == MunnaTab.CHAT,
                            onClick = { viewModel.selectTab(MunnaTab.CHAT) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MunnaTab.CHAT) Icons.AutoMirrored.Filled.Chat else Icons.Outlined.ChatBubbleOutline,
                                    contentDescription = "AI चैट"
                                )
                            },
                            label = {
                                Text(
                                    text = "AI चैट",
                                    fontWeight = if (currentTab == MunnaTab.CHAT) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandRoyalBlue,
                                selectedTextColor = BrandRoyalBlue,
                                indicatorColor = SoftSkyBlueCard,
                                unselectedIconColor = NavyTextSecondary,
                                unselectedTextColor = NavyTextSecondary
                            ),
                            modifier = Modifier.testTag("nav_tab_chat")
                        )

                        NavigationBarItem(
                            selected = currentTab == MunnaTab.HISTORY,
                            onClick = { viewModel.selectTab(MunnaTab.HISTORY) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MunnaTab.HISTORY) Icons.Filled.History else Icons.Outlined.History,
                                    contentDescription = "इतिहास"
                                )
                            },
                            label = {
                                Text(
                                    text = "इतिहास",
                                    fontWeight = if (currentTab == MunnaTab.HISTORY) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandRoyalBlue,
                                selectedTextColor = BrandRoyalBlue,
                                indicatorColor = SoftSkyBlueCard,
                                unselectedIconColor = NavyTextSecondary,
                                unselectedTextColor = NavyTextSecondary
                            ),
                            modifier = Modifier.testTag("nav_tab_history")
                        )

                        NavigationBarItem(
                            selected = currentTab == MunnaTab.SETTINGS,
                            onClick = { viewModel.selectTab(MunnaTab.SETTINGS) },
                            icon = {
                                Icon(
                                    imageVector = if (currentTab == MunnaTab.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                                    contentDescription = "सेटिंग्स"
                                )
                            },
                            label = {
                                Text(
                                    text = "सेटिंग्स",
                                    fontWeight = if (currentTab == MunnaTab.SETTINGS) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = BrandRoyalBlue,
                                selectedTextColor = BrandRoyalBlue,
                                indicatorColor = SoftSkyBlueCard,
                                unselectedIconColor = NavyTextSecondary,
                                unselectedTextColor = NavyTextSecondary
                            ),
                            modifier = Modifier.testTag("nav_tab_settings")
                        )
                    }
                }
            }
        ) { innerPadding ->
            when (currentTab) {
                MunnaTab.HOME -> HomeScreen(
                    language = settings.language,
                    userDisplayName = userDisplayName,
                    voiceState = voiceState,
                    lastFeedback = lastFeedback,
                    recentHistory = recentHistory,
                    reducedMotion = settings.reducedMotion,
                    isSpeaking = isSpeaking,
                    isAppInstalledProvider = { viewModel.isAppInstalled(it) },
                    onMicClick = { triggerMicAction(false) },
                    onFinishListening = { viewModel.finishOrStopMicListening() },
                    onCancelListening = { viewModel.cancelMicListening() },
                    onSelectEngineMode = { viewModel.setVoiceEngineMode(it) },
                    onLaunchSystemSpeechDialog = { launchSystemSpeechPopup(false) },
                    onShortcutClick = { viewModel.launchShortcutApp(it) },
                    onQuickCommandClick = { viewModel.processVoiceOrQuickCommand(it) },
                    onSpeakText = { viewModel.speakText(it) },
                    onStopSpeaking = { viewModel.stopSpeaking() },
                    onOpenLoginScreen = { viewModel.openLoginScreen() },
                    onOpenSettings = { viewModel.selectTab(MunnaTab.SETTINGS) },
                    onOpenHistory = { viewModel.selectTab(MunnaTab.HISTORY) },
                    onOpenAiChat = { viewModel.selectTab(MunnaTab.CHAT) },
                    modifier = Modifier.padding(innerPadding)
                )

                MunnaTab.CHAT -> AiChatScreen(
                    messages = chatMessages,
                    isLoading = isChatLoading,
                    voiceState = voiceState,
                    isSpeaking = isSpeaking,
                    onSendMessage = { viewModel.sendChatMessage(it) },
                    onMicClick = {
                        if (voiceState.status == MicStatus.LISTENING) {
                            viewModel.finishOrStopMicListening()
                        } else {
                            triggerMicAction(true)
                        }
                    },
                    onSpeakMessage = { viewModel.speakText(it) },
                    onStopSpeaking = { viewModel.stopSpeaking() },
                    onNavigateBackHome = { viewModel.selectTab(MunnaTab.HOME) },
                    modifier = Modifier.padding(innerPadding)
                )

                MunnaTab.HISTORY -> HistoryScreen(
                    historyList = allHistory,
                    saveHistoryEnabled = settings.saveCommandHistory,
                    onToggleSaveHistory = { viewModel.updateSaveHistory(it) },
                    onRepeatCommand = { cmd ->
                        viewModel.selectTab(MunnaTab.HOME)
                        viewModel.processVoiceOrQuickCommand(cmd)
                    },
                    onDeleteCommand = { viewModel.deleteHistoryItem(it) },
                    onClearAll = { viewModel.clearAllHistory() },
                    onNavigateBackHome = { viewModel.selectTab(MunnaTab.HOME) },
                    modifier = Modifier.padding(innerPadding)
                )

                MunnaTab.SETTINGS -> SettingsScreen(
                    settings = settings,
                    isMicGranted = isMicGranted,
                    isOverlayGranted = isOverlayGranted,
                    isNotificationGranted = isNotificationGranted,
                    isFloatingServiceRunning = isFloatingServiceRunning,
                    inAppFloatingBubbleVisible = inAppFloatingVisible,
                    recentNotifications = recentNotifications,
                    onLanguageChange = { viewModel.updateLanguage(it) },
                    onSpokenResponsesChange = { viewModel.updateSpokenResponses(it) },
                    onSpeechRateChange = { viewModel.updateSpeechRate(it) },
                    onVoicePitchChange = { viewModel.updateVoicePitch(it) },
                    onReducedMotionChange = { viewModel.updateReducedMotion(it) },
                    onTestVoiceClick = {
                        viewModel.speakText("नमस्ते भाई! मैं मुन्ना हूँ। बोलो, मुन्ना करेगा!")
                    },
                    onToggleFloatingMic = { enable ->
                        viewModel.toggleFloatingMicService(enable)
                    },
                    onRequestMicPermission = {
                        if (!viewModel.isMicPermissionGranted()) {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        } else {
                            viewModel.openAppDetailsSettings()
                        }
                    },
                    onOpenOverlayPermissionSettings = {
                        viewModel.openOverlayPermissionSettings()
                    },
                    onOpenNotificationListenerSettings = {
                        viewModel.openNotificationListenerSettings()
                    },
                    onToggleNotificationPaused = {
                        viewModel.updateNotificationReaderPaused(it)
                    },
                    onReadNotificationAloud = { text ->
                        viewModel.speakText(text)
                    },
                    onClearHistory = {
                        viewModel.clearAllHistory()
                    },
                    onNavigateBackHome = { viewModel.selectTab(MunnaTab.HOME) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }

        // Draggable Floating Microphone Button (Section 6)
        if ((inAppFloatingVisible || settings.floatingMicEnabled) && !isFloatingServiceRunning) {
            DraggableInAppFloatingMic(
                isListening = voiceState.status == MicStatus.LISTENING,
                onTapMic = {
                    if (voiceState.status == MicStatus.LISTENING) {
                        viewModel.finishOrStopMicListening()
                    } else {
                        triggerMicAction(currentTab == MunnaTab.CHAT)
                    }
                },
                onHideFloatingMic = {
                    viewModel.stopFloatingMicService()
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 12.dp)
            )
        }

        // In-App Quick WebView Viewer when an app like YouTube/WhatsApp/Chrome is opened on an emulator without the native APK
        activeInAppWebFallback?.let { (appTitle, webUrl) ->
            InAppWebViewerDialog(
                title = appTitle,
                url = webUrl,
                onDismiss = { viewModel.dismissInAppWebViewer() }
            )
        }
    }
}

@Composable
private fun DraggableInAppFloatingMic(
    isListening: Boolean,
    onTapMic: () -> Unit,
    onHideFloatingMic: () -> Unit,
    modifier: Modifier = Modifier
) {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .testTag("floating_mic_bubble")
    ) {
        Surface(
            shape = CircleShape,
            color = if (isListening) BrandCyanGlow else BrandRoyalBlue,
            shadowElevation = 10.dp,
            border = BorderStroke(2.dp, Color.White),
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .clickable { onTapMic() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = "Floating Microphone",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = PureWhiteSurface,
            border = BorderStroke(1.dp, BrandRoyalBlue.copy(alpha = 0.4f)),
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable { onHideFloatingMic() }
                .testTag("hide_floating_mic_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Hide floating microphone",
                    tint = NavyTextPrimary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}
