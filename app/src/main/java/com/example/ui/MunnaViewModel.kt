package com.example.ui

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppLanguage
import com.example.data.CommandHistoryEntity
import com.example.data.CommandRepository
import com.example.data.MunnaDatabase
import com.example.data.SettingsRepository
import com.example.data.UserSettings
import com.example.domain.AppActionHandler
import com.example.domain.CommandParser
import com.example.domain.TargetApp
import com.example.network.AiChatResult
import com.example.network.GeminiChatService
import com.example.service.FloatingMicService
import com.example.service.MunnaNotificationListenerService
import com.example.service.ReadableNotification
import com.example.voice.TextToSpeechManager
import com.example.voice.VoiceRecognitionManager
import com.example.voice.VoiceUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MunnaTab {
    HOME,
    CHAT,
    HISTORY,
    SETTINGS
}

data class ChatMessage(
    val id: Long = System.nanoTime(),
    val text: String,
    val isUser: Boolean,
    val isError: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class LastActionFeedback(
    val recognizedCommand: String,
    val actionTitle: String,
    val spokenResponse: String,
    val isSuccess: Boolean,
    val category: String
)

class MunnaViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext: Context = application.applicationContext
    private val database = MunnaDatabase.getInstance(appContext)
    private val commandRepository = CommandRepository(database.commandHistoryDao())
    private val settingsRepository = SettingsRepository(appContext)
    private val actionHandler = AppActionHandler(appContext)
    private val geminiChatService = GeminiChatService()
    private val ttsManager = TextToSpeechManager(appContext)

    private val voiceManager = VoiceRecognitionManager(
        context = appContext,
        geminiChatService = geminiChatService
    ) { recognizedText ->
        onVoiceTextRecognized(recognizedText)
    }

    val voiceState: StateFlow<VoiceUiState> = voiceManager.state
    val isTtsSpeaking: StateFlow<Boolean> = ttsManager.isSpeaking
    val isFloatingServiceRunning: StateFlow<Boolean> = FloatingMicService.isRunning
    val recentNotifications: StateFlow<List<ReadableNotification>> =
        MunnaNotificationListenerService.recentNotifications

    val settings: StateFlow<UserSettings> = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserSettings()
    )

    val allHistory: StateFlow<List<CommandHistoryEntity>> = commandRepository.allHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentHistory: StateFlow<List<CommandHistoryEntity>> = commandRepository.recentHistory.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _currentTab = MutableStateFlow(MunnaTab.HOME)
    val currentTab: StateFlow<MunnaTab> = _currentTab.asStateFlow()

    private val _showLoginScreen = MutableStateFlow(false)
    val showLoginScreen: StateFlow<Boolean> = _showLoginScreen.asStateFlow()

    private val _userDisplayName = MutableStateFlow("लॉगिन")
    val userDisplayName: StateFlow<String> = _userDisplayName.asStateFlow()

    private val _activeInAppWebFallback = MutableStateFlow<Pair<String, String>?>(null)
    val activeInAppWebFallback: StateFlow<Pair<String, String>?> = _activeInAppWebFallback.asStateFlow()

    private val _lastFeedback = MutableStateFlow<LastActionFeedback?>(null)
    val lastFeedback: StateFlow<LastActionFeedback?> = _lastFeedback.asStateFlow()

    private val _chatMessages = MutableStateFlow(
        listOf(
            ChatMessage(
                text = "नमस्ते भाई! मैं मुन्ना हूँ — आपका अपना AI दोस्त। आप मुझसे हिंदी, हिंग्लिश या अंग्रेज़ी में कोई भी सवाल पूछ सकते हैं, या फ़ोन के काम करवा सकते हैं। बोलो, क्या मदद करूँ?",
                isUser = false
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    private val _inAppFloatingBubbleVisible = MutableStateFlow(false)
    val inAppFloatingBubbleVisible: StateFlow<Boolean> = _inAppFloatingBubbleVisible.asStateFlow()

    private var voiceTargetIsChat: Boolean = false

    private val _permissionStatusVersion = MutableStateFlow(0)
    val permissionStatusVersion: StateFlow<Int> = _permissionStatusVersion.asStateFlow()

    fun openLoginScreen() {
        _showLoginScreen.value = true
    }

    fun onLoginCompleted(userName: String) {
        _userDisplayName.value = userName
        _showLoginScreen.value = false
    }

    fun dismissInAppWebViewer() {
        _activeInAppWebFallback.value = null
    }

    fun refreshPermissionsStatus() {
        _permissionStatusVersion.value += 1
        MunnaNotificationListenerService.setPaused(settings.value.notificationReaderPaused)
    }

    fun isMicPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isOverlayPermissionGranted(): Boolean {
        return Settings.canDrawOverlays(appContext)
    }

    fun isNotificationAccessGranted(): Boolean {
        return MunnaNotificationListenerService.isNotificationAccessGranted(appContext)
    }

    fun isAppInstalled(targetApp: TargetApp): Boolean {
        val pm = appContext.packageManager
        if (targetApp == TargetApp.FILES) return true
        return targetApp.candidatePackages.any { pkg ->
            try {
                pm.getLaunchIntentForPackage(pkg) != null
            } catch (_: Exception) {
                false
            }
        }
    }

    fun selectTab(tab: MunnaTab) {
        _showLoginScreen.value = false
        _currentTab.value = tab
    }

    fun setVoiceEngineMode(mode: com.example.voice.VoiceEngineMode) {
        voiceManager.setEngineMode(mode)
    }

    fun startMicListening(forChatScreen: Boolean = false) {
        voiceTargetIsChat = forChatScreen
        if (!isMicPermissionGranted()) {
            voiceManager.setPermissionDeniedError()
            return
        }
        ttsManager.stop()
        voiceManager.startListening(settings.value.language)
    }

    fun startDirectHardwareMicListening(forChatScreen: Boolean = false) {
        voiceTargetIsChat = forChatScreen
        if (!isMicPermissionGranted()) {
            voiceManager.setPermissionDeniedError()
            return
        }
        ttsManager.stop()
        voiceManager.startDirectHardwareMicRecording(settings.value.language)
    }

    fun finishOrStopMicListening() {
        voiceManager.finishOrCancelListening()
    }

    fun cancelMicListening() {
        voiceManager.cancelListening()
    }

    fun onSystemSpeechDialogResult(spokenText: String?, forChatScreen: Boolean = false) {
        val clean = spokenText?.trim().orEmpty()
        if (clean.isNotEmpty()) {
            voiceTargetIsChat = forChatScreen
            voiceManager.setStatusDone(clean)
            onVoiceTextRecognized(clean)
        }
    }

    fun clearVoiceError() {
        voiceManager.clearError()
    }

    fun onMicPermissionResult(granted: Boolean, forChatScreen: Boolean = false) {
        refreshPermissionsStatus()
        if (granted) {
            startMicListening(forChatScreen = forChatScreen)
        } else {
            voiceManager.setPermissionDeniedError()
        }
    }

    private fun onVoiceTextRecognized(recognizedText: String) {
        if (voiceTargetIsChat) {
            sendChatMessage(recognizedText)
        } else {
            processVoiceOrQuickCommand(recognizedText)
        }
    }

    fun processVoiceOrQuickCommand(commandText: String) {
        val trimmed = commandText.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            val currentSettings = settings.value
            val parsed = CommandParser.parse(trimmed)
            val result = actionHandler.execute(parsed, currentSettings.language)

            voiceManager.setStatusDone(trimmed)

            _lastFeedback.value = LastActionFeedback(
                recognizedCommand = trimmed,
                actionTitle = result.actionTitle,
                spokenResponse = result.spokenResponse,
                isSuccess = result.isSuccess,
                category = result.category
            )

            if (result.inAppWebFallbackUrl != null && result.inAppWebTitle != null) {
                _activeInAppWebFallback.value = result.inAppWebTitle to result.inAppWebFallbackUrl
            }

            speakText(result.spokenResponse)

            if (currentSettings.saveCommandHistory) {
                commandRepository.logCommand(
                    recognizedText = trimmed,
                    actionTitle = result.actionTitle,
                    assistantResponse = result.spokenResponse,
                    category = result.category,
                    isSuccess = result.isSuccess
                )
            }

            if (result.shouldRouteToAi && !result.aiQuery.isNullOrBlank()) {
                sendChatMessage(result.aiQuery, switchToChatTab = false)
            }
        }
    }

    fun launchShortcutApp(targetApp: TargetApp) {
        viewModelScope.launch {
            val currentSettings = settings.value
            val commandLabel = "Munna, ${targetApp.displayName} kholo"
            val result = actionHandler.launchTargetApp(targetApp, currentSettings.language)

            voiceManager.setStatusDone(commandLabel)

            _lastFeedback.value = LastActionFeedback(
                recognizedCommand = commandLabel,
                actionTitle = result.actionTitle,
                spokenResponse = result.spokenResponse,
                isSuccess = result.isSuccess,
                category = result.category
            )

            if (result.inAppWebFallbackUrl != null && result.inAppWebTitle != null) {
                _activeInAppWebFallback.value = result.inAppWebTitle to result.inAppWebFallbackUrl
            }

            speakText(result.spokenResponse)

            if (currentSettings.saveCommandHistory) {
                commandRepository.logCommand(
                    recognizedText = commandLabel,
                    actionTitle = result.actionTitle,
                    assistantResponse = result.spokenResponse,
                    category = result.category,
                    isSuccess = result.isSuccess
                )
            }
        }
    }

    fun sendChatMessage(userText: String, switchToChatTab: Boolean = false) {
        val cleanText = userText.trim()
        if (cleanText.isEmpty() || _isChatLoading.value) return

        if (switchToChatTab) {
            _currentTab.value = MunnaTab.CHAT
        }

        // First check if the chat input is actually a direct app-opening command!
        val parsed = CommandParser.parse(cleanText)
        if (parsed is com.example.domain.ParsedCommand.OpenApp ||
            parsed is com.example.domain.ParsedCommand.TellTime ||
            parsed is com.example.domain.ParsedCommand.TellDate ||
            parsed is com.example.domain.ParsedCommand.CheckBattery
        ) {
            val localRes = actionHandler.execute(parsed, settings.value.language)
            val userMsg = ChatMessage(text = cleanText, isUser = true)
            val botMsg = ChatMessage(text = localRes.spokenResponse, isUser = false, isError = !localRes.isSuccess)
            _chatMessages.value = _chatMessages.value + userMsg + botMsg
            speakText(localRes.spokenResponse)
            viewModelScope.launch {
                if (settings.value.saveCommandHistory) {
                    commandRepository.logCommand(
                        recognizedText = cleanText,
                        actionTitle = localRes.actionTitle,
                        assistantResponse = localRes.spokenResponse,
                        category = localRes.category,
                        isSuccess = localRes.isSuccess
                    )
                }
            }
            return
        }

        val historySnapshot = _chatMessages.value
            .filterNot { it.isError }
            .map { it.isUser to it.text }

        val userMsg = ChatMessage(text = cleanText, isUser = true)
        _chatMessages.value = _chatMessages.value + userMsg
        _isChatLoading.value = true

        viewModelScope.launch {
            val currentSettings = settings.value
            when (val aiResult = geminiChatService.askMunna(cleanText, currentSettings.language, historySnapshot)) {
                is AiChatResult.Success -> {
                    _isChatLoading.value = false
                    val botMsg = ChatMessage(text = aiResult.replyText, isUser = false)
                    _chatMessages.value = _chatMessages.value + botMsg
                    _lastFeedback.value = LastActionFeedback(
                        recognizedCommand = cleanText,
                        actionTitle = "मुन्ना AI का जवाब",
                        spokenResponse = aiResult.replyText,
                        isSuccess = true,
                        category = "AI_CHAT"
                    )
                    speakText(aiResult.replyText)
                    if (currentSettings.saveCommandHistory) {
                        commandRepository.logCommand(
                            recognizedText = cleanText,
                            actionTitle = "AI Chat सवाल",
                            assistantResponse = aiResult.replyText,
                            category = "AI_CHAT",
                            isSuccess = true
                        )
                    }
                }
                is AiChatResult.Error -> {
                    _isChatLoading.value = false
                    val errMsg = ChatMessage(
                        text = aiResult.hindiErrorMessage,
                        isUser = false,
                        isError = true
                    )
                    _chatMessages.value = _chatMessages.value + errMsg
                    speakText(aiResult.hindiErrorMessage)
                }
            }
        }
    }

    fun speakText(text: String) {
        val current = settings.value
        ttsManager.speak(
            text = text,
            language = current.language,
            speechRate = current.speechRate,
            pitch = current.voicePitch,
            enabled = current.spokenResponsesEnabled
        )
    }

    fun stopSpeaking() {
        ttsManager.stop()
    }

    // --- Settings Actions ---

    fun updateLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
        }
    }

    fun updateSpokenResponses(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSpokenResponses(enabled)
            if (!enabled) ttsManager.stop()
        }
    }

    fun updateSpeechRate(rate: Float) {
        viewModelScope.launch {
            settingsRepository.setSpeechRate(rate)
        }
    }

    fun updateVoicePitch(pitch: Float) {
        viewModelScope.launch {
            settingsRepository.setVoicePitch(pitch)
        }
    }

    fun updateSaveHistory(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setSaveCommandHistory(enabled)
        }
    }

    fun updateReducedMotion(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setReducedMotion(enabled)
        }
    }

    fun updateNotificationReaderPaused(paused: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationReaderPaused(paused)
            MunnaNotificationListenerService.setPaused(paused)
        }
    }

    fun toggleFloatingMicService(enable: Boolean): String? {
        if (!enable) {
            stopFloatingMicService()
            _inAppFloatingBubbleVisible.value = false
            viewModelScope.launch { settingsRepository.setFloatingMicEnabled(false) }
            return null
        }

        if (!isMicPermissionGranted()) {
            return "फ़्लोटिंग माइक चालू करने से पहले कृपया माइक्रोफ़ोन की अनुमति (Microphone Permission) दें।"
        }

        // Always enable the in-app floating mic bubble immediately, and if overlay permission is granted,
        // also launch the system-wide Android Overlay Foreground Service!
        _inAppFloatingBubbleVisible.value = true
        viewModelScope.launch { settingsRepository.setFloatingMicEnabled(true) }

        if (!isOverlayPermissionGranted()) {
            return "ऐप के अंदर फ़्लोटिंग माइक चालू हो गया है! अन्य ऐप्स के ऊपर (System-Wide Overlay) दिखाने के लिए कृपया 'Display over other apps' की अनुमति दें।"
        }

        return try {
            val serviceIntent = Intent(appContext, FloatingMicService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                appContext.startForegroundService(serviceIntent)
            } else {
                appContext.startService(serviceIntent)
            }
            null
        } catch (e: Exception) {
            "फ़्लोटिंग माइक सर्विस शुरू करने में समस्या आई: ${e.localizedMessage ?: ""}"
        }
    }

    fun stopFloatingMicService() {
        try {
            val stopIntent = Intent(appContext, FloatingMicService::class.java)
            appContext.stopService(stopIntent)
        } catch (_: Exception) {
        }
        _inAppFloatingBubbleVisible.value = false
        viewModelScope.launch { settingsRepository.setFloatingMicEnabled(false) }
    }

    fun openOverlayPermissionSettings() {
        try {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${appContext.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun openNotificationListenerSettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun openAppDetailsSettings() {
        try {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:${appContext.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            appContext.startActivity(intent)
        } catch (_: Exception) {
        }
    }

    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            commandRepository.deleteCommand(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            commandRepository.clearAll()
        }
    }

    override fun onCleared() {
        voiceManager.release()
        ttsManager.shutdown()
        super.onCleared()
    }
}
