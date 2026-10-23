package com.example.voice

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.example.data.AppLanguage
import com.example.network.AiChatResult
import com.example.network.GeminiChatService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Locale
import kotlin.math.log10
import kotlin.math.sqrt

enum class MicStatus {
    IDLE,
    LISTENING,
    PROCESSING,
    SUCCESS,
    ERROR
}

enum class VoiceEngineMode(val labelHindi: String) {
    AUTO_HYBRID("ऑटो माइक (Auto)"),
    DIRECT_HARDWARE_MIC("सीधा माइक (Direct Mic)")
}

data class VoiceUiState(
    val status: MicStatus = MicStatus.IDLE,
    val statusLabelHindi: String = "बोलने के लिए टैप करें",
    val partialOrFinalText: String = "",
    val errorMessageHindi: String? = null,
    val rmsDbLevel: Float = 0f,
    val engineMode: VoiceEngineMode = VoiceEngineMode.AUTO_HYBRID,
    val isUsingHardwareRecorder: Boolean = false,
    val isSpeechAvailableOnDevice: Boolean = true
)

class VoiceRecognitionManager(
    private val context: Context,
    private val geminiChatService: GeminiChatService = GeminiChatService(),
    private val onCommandRecognized: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var speechRecognizer: SpeechRecognizer? = null
    private var audioRecordJob: Job? = null
    private var watchdogJob: Job? = null
    private var partialCommitJob: Job? = null

    @Volatile
    private var shouldStopHardwareRecordNow = false
    private var lastPartialText: String = ""
    private var currentLanguage: AppLanguage = AppLanguage.HINDI
    private var hasRetriedFallbackLocale = false
    private var hasDeliveredCurrentSessionResult = false
    private var speechServiceResponded = false

    private val _state = MutableStateFlow(VoiceUiState())
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    fun setEngineMode(mode: VoiceEngineMode) {
        _state.value = _state.value.copy(engineMode = mode)
    }

    fun startListening(language: AppLanguage) {
        currentLanguage = language
        hasRetriedFallbackLocale = false
        lastPartialText = ""
        hasDeliveredCurrentSessionResult = false
        speechServiceResponded = false
        watchdogJob?.cancel()
        partialCommitJob?.cancel()

        if (_state.value.engineMode == VoiceEngineMode.DIRECT_HARDWARE_MIC) {
            startDirectHardwareMicRecording(language)
            return
        }

        mainHandler.post {
            val isServiceReported = try {
                SpeechRecognizer.isRecognitionAvailable(context)
            } catch (_: Exception) {
                false
            }

            if (!isServiceReported) {
                startDirectHardwareMicRecording(language)
                return@post
            }

            startAndroidSpeechRecognizer(
                localeTag = when (language) {
                    AppLanguage.HINDI -> "hi-IN"
                    AppLanguage.HINGLISH -> "en-IN"
                    AppLanguage.ENGLISH -> "en-IN"
                }
            )
        }
    }

    private fun startAndroidSpeechRecognizer(localeTag: String) {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(createListener())
                }

                val recognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, localeTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1400L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 800L)
                }

                _state.value = _state.value.copy(
                    status = MicStatus.LISTENING,
                    statusLabelHindi = "सुन रहा हूँ… अब बोलिए!",
                    partialOrFinalText = "",
                    errorMessageHindi = null,
                    rmsDbLevel = 2.5f,
                    isUsingHardwareRecorder = false
                )

                speechRecognizer?.startListening(recognizerIntent)

                // Watchdog: if Android SpeechRecognizer is a silent stub (e.g. on an emulator)
                // and doesn't respond within 2.0 seconds, automatically switch to Direct Hardware Mic!
                watchdogJob?.cancel()
                watchdogJob = scope.launch {
                    delay(2000L)
                    if (!speechServiceResponded &&
                        !hasDeliveredCurrentSessionResult &&
                        _state.value.status == MicStatus.LISTENING &&
                        !_state.value.isUsingHardwareRecorder
                    ) {
                        try {
                            speechRecognizer?.cancel()
                            speechRecognizer?.destroy()
                            speechRecognizer = null
                        } catch (_: Exception) {
                        }
                        startDirectHardwareMicRecording(currentLanguage)
                    }
                }
            } catch (_: Exception) {
                startDirectHardwareMicRecording(currentLanguage)
            }
        }
    }

    /**
     * Direct Hardware Microphone Recorder using Android AudioRecord (16kHz, 16-bit mono PCM).
     * Fast 3.2-second window with 0.9s silence detection so commands execute quickly!
     */
    @SuppressLint("MissingPermission")
    fun startDirectHardwareMicRecording(language: AppLanguage = currentLanguage) {
        watchdogJob?.cancel()
        partialCommitJob?.cancel()
        audioRecordJob?.cancel()
        shouldStopHardwareRecordNow = false
        hasDeliveredCurrentSessionResult = false

        _state.value = _state.value.copy(
            status = MicStatus.LISTENING,
            statusLabelHindi = "सुन रहा हूँ… अब बोलिए!",
            partialOrFinalText = "🎙️ बोलिए (जैसे: YouTube खोलो)…",
            errorMessageHindi = null,
            rmsDbLevel = 3.5f,
            isUsingHardwareRecorder = true
        )

        audioRecordJob = scope.launch(Dispatchers.IO) {
            val sampleRate = 16000
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                .coerceAtLeast(sampleRate / 5)

            var recorder: AudioRecord? = null
            val pcmOutput = ByteArrayOutputStream()

            try {
                recorder = try {
                    AudioRecord(
                        MediaRecorder.AudioSource.VOICE_RECOGNITION,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        minBufSize * 2
                    )
                } catch (_: Exception) {
                    null
                }

                if (recorder == null || recorder.state != AudioRecord.STATE_INITIALIZED) {
                    recorder?.release()
                    recorder = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        minBufSize * 2
                    )
                }

                if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                    recorder.release()
                    _state.value = _state.value.copy(
                        status = MicStatus.ERROR,
                        statusLabelHindi = "बोलने के लिए टैप करें",
                        errorMessageHindi = "माइक शुरू नहीं हो सका। कृपया नीचे 'या लिखकर भी पूछ सकते हैं' या 'सिस्टम माइक' का उपयोग करें।",
                        rmsDbLevel = 0f,
                        isUsingHardwareRecorder = false
                    )
                    return@launch
                }

                recorder.startRecording()

                val shortBuffer = ShortArray(1024)
                val startTime = System.currentTimeMillis()
                var detectedSpeechAtLeastOnce = false
                var lastVoiceActivityTime = System.currentTimeMillis()
                val maxDurationMs = 3400L
                val silenceStopDurationMs = 950L

                while (isActive && !shouldStopHardwareRecordNow) {
                    val readCount = recorder.read(shortBuffer, 0, shortBuffer.size)
                    if (readCount > 0) {
                        var sumSquares = 0.0
                        val byteBuf = ByteBuffer.allocate(readCount * 2).order(ByteOrder.LITTLE_ENDIAN)
                        for (i in 0 until readCount) {
                            val sample = shortBuffer[i]
                            byteBuf.putShort(sample)
                            sumSquares += sample.toDouble() * sample.toDouble()
                        }
                        pcmOutput.write(byteBuf.array())

                        val rms = sqrt(sumSquares / readCount)
                        val db = if (rms > 1.0) (20.0 * log10(rms / 32768.0) + 65.0).toFloat() else 1f
                        val normalizedLevel = db.coerceIn(1.5f, 12f)

                        _state.value = _state.value.copy(rmsDbLevel = normalizedLevel)

                        if (rms > 180.0) {
                            detectedSpeechAtLeastOnce = true
                            lastVoiceActivityTime = System.currentTimeMillis()
                            _state.value = _state.value.copy(
                                partialOrFinalText = "🎙️ आवाज़ आ रही है..."
                            )
                        }

                        val now = System.currentTimeMillis()
                        if (detectedSpeechAtLeastOnce && (now - lastVoiceActivityTime) > silenceStopDurationMs) {
                            break
                        }
                        if ((now - startTime) > maxDurationMs) {
                            break
                        }
                    }
                }

                try {
                    recorder.stop()
                } catch (_: Exception) {
                }
                recorder.release()
                recorder = null

                val rawPcmBytes = pcmOutput.toByteArray()
                if (rawPcmBytes.size < 2400) {
                    _state.value = _state.value.copy(
                        status = MicStatus.ERROR,
                        statusLabelHindi = "बोलने के लिए टैप करें",
                        partialOrFinalText = "",
                        errorMessageHindi = "आवाज़ बहुत छोटी थी। कृपया फिर से बोलें (जैसे: 'YouTube खोलो')।",
                        rmsDbLevel = 0f,
                        isUsingHardwareRecorder = false
                    )
                    return@launch
                }

                _state.value = _state.value.copy(
                    status = MicStatus.PROCESSING,
                    statusLabelHindi = "आपकी बात समझ रहा हूँ…",
                    partialOrFinalText = "⏳ मुन्ना आपकी कमांड खोल रहा है...",
                    rmsDbLevel = 0f,
                    isUsingHardwareRecorder = false
                )

                val wavBytes = encodePcmToWav(rawPcmBytes, sampleRate = sampleRate, channels = 1, bitsPerSample = 16)
                when (val transcription = geminiChatService.transcribeWavAudio(wavBytes, language)) {
                    is AiChatResult.Success -> {
                        val recognized = transcription.replyText.trim()
                        deliverRecognizedText(recognized)
                    }
                    is AiChatResult.Error -> {
                        _state.value = _state.value.copy(
                            status = MicStatus.ERROR,
                            statusLabelHindi = "बोलने के लिए टैप करें",
                            partialOrFinalText = "",
                            errorMessageHindi = transcription.hindiErrorMessage,
                            rmsDbLevel = 0f
                        )
                    }
                }
            } catch (e: Exception) {
                try {
                    recorder?.release()
                } catch (_: Exception) {
                }
                _state.value = _state.value.copy(
                    status = MicStatus.ERROR,
                    statusLabelHindi = "बोलने के लिए टैप करें",
                    partialOrFinalText = "",
                    errorMessageHindi = "माइक में समस्या आई: ${e.localizedMessage ?: "त्रुटि"}",
                    rmsDbLevel = 0f,
                    isUsingHardwareRecorder = false
                )
            }
        }
    }

    private fun deliverRecognizedText(recognized: String) {
        if (hasDeliveredCurrentSessionResult || recognized.isBlank()) return
        hasDeliveredCurrentSessionResult = true
        watchdogJob?.cancel()
        partialCommitJob?.cancel()

        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {
            }
            _state.value = _state.value.copy(
                status = MicStatus.SUCCESS,
                statusLabelHindi = "काम पूरा हुआ!",
                partialOrFinalText = recognized,
                errorMessageHindi = null,
                rmsDbLevel = 0f,
                isUsingHardwareRecorder = false
            )
            onCommandRecognized(recognized)
        }
    }

    /**
     * Tapping the mic while listening finishes and commits the spoken words immediately!
     */
    fun finishOrCancelListening() {
        if (_state.value.isUsingHardwareRecorder && _state.value.status == MicStatus.LISTENING) {
            shouldStopHardwareRecordNow = true
            return
        }
        if (lastPartialText.isNotBlank()) {
            val captured = lastPartialText.trim()
            lastPartialText = ""
            deliverRecognizedText(captured)
            return
        }
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {
                cancelListening()
            }
        }
    }

    fun cancelListening() {
        shouldStopHardwareRecordNow = true
        watchdogJob?.cancel()
        partialCommitJob?.cancel()
        audioRecordJob?.cancel()
        audioRecordJob = null
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {
            }
            _state.value = _state.value.copy(
                status = MicStatus.IDLE,
                statusLabelHindi = "बोलने के लिए टैप करें",
                partialOrFinalText = "",
                rmsDbLevel = 0f,
                isUsingHardwareRecorder = false
            )
        }
    }

    fun setStatusDone(recognizedText: String) {
        _state.value = _state.value.copy(
            status = MicStatus.SUCCESS,
            statusLabelHindi = "काम पूरा हुआ!",
            partialOrFinalText = recognizedText,
            errorMessageHindi = null,
            rmsDbLevel = 0f,
            isUsingHardwareRecorder = false
        )
    }

    fun setPermissionDeniedError() {
        _state.value = _state.value.copy(
            status = MicStatus.ERROR,
            statusLabelHindi = "बोलने के लिए टैप करें",
            errorMessageHindi = "माइक्रोफ़ोन की अनुमति नहीं मिली। आवाज़ से कमांड देने के लिए कृपया माइक अनुमति (Microphone Permission) चालू करें।"
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(
            status = MicStatus.IDLE,
            statusLabelHindi = "बोलने के लिए टैप करें",
            errorMessageHindi = null
        )
    }

    fun release() {
        shouldStopHardwareRecordNow = true
        watchdogJob?.cancel()
        partialCommitJob?.cancel()
        audioRecordJob?.cancel()
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (_: Exception) {
            }
        }
    }

    private fun createListener(): RecognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            speechServiceResponded = true
            _state.value = _state.value.copy(
                status = MicStatus.LISTENING,
                statusLabelHindi = "सुन रहा हूँ… अब बोलिए!",
                errorMessageHindi = null
            )
        }

        override fun onBeginningOfSpeech() {
            speechServiceResponded = true
            _state.value = _state.value.copy(
                status = MicStatus.LISTENING,
                statusLabelHindi = "सुन रहा हूँ…"
            )
        }

        override fun onRmsChanged(rmsdB: Float) {
            if (rmsdB > 0.5f) {
                speechServiceResponded = true
            }
            _state.value = _state.value.copy(
                rmsDbLevel = rmsdB.coerceIn(0f, 12f)
            )
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _state.value = _state.value.copy(
                status = MicStatus.PROCESSING,
                statusLabelHindi = "आपकी बात समझ रहा हूँ…",
                rmsDbLevel = 0f
            )
        }

        override fun onError(error: Int) {
            if (hasDeliveredCurrentSessionResult) return

            if (lastPartialText.isNotBlank()) {
                val captured = lastPartialText.trim()
                lastPartialText = ""
                deliverRecognizedText(captured)
                return
            }

            if ((error == 12 || error == 13) && !hasRetriedFallbackLocale) {
                hasRetriedFallbackLocale = true
                startAndroidSpeechRecognizer(Locale.getDefault().toLanguageTag())
                return
            }

            if (error == SpeechRecognizer.ERROR_CLIENT ||
                error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                error == SpeechRecognizer.ERROR_SERVER ||
                error == 11
            ) {
                startDirectHardwareMicRecording(currentLanguage)
                return
            }

            val hindiError = when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                    "माइक्रोफ़ोन की अनुमति नहीं दी गई है। कृपया माइक अनुमति दें।"
                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                    "आवाज़ साफ़ सुनाई नहीं दी भाई। कृपया माइक दबाकर बोलें: 'YouTube खोलो' या 'WhatsApp खोलो'।"
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                    "इंटरनेट कनेक्शन में समस्या है। कृपया इंटरनेट चालू करें।"
                else ->
                    "आवाज़ पहचानने में समस्या आई। कृपया दोबारा माइक दबाएँ या नीचे लिखकर पूछें।"
            }
            _state.value = _state.value.copy(
                status = MicStatus.ERROR,
                statusLabelHindi = "बोलने के लिए टैप करें",
                errorMessageHindi = hindiError,
                rmsDbLevel = 0f
            )
        }

        override fun onResults(results: Bundle?) {
            if (hasDeliveredCurrentSessionResult) return
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val bestText = matches?.firstOrNull { it.isNotBlank() }?.trim()
                ?: lastPartialText.trim()
            lastPartialText = ""

            if (bestText.isNotEmpty()) {
                deliverRecognizedText(bestText)
            } else {
                _state.value = _state.value.copy(
                    status = MicStatus.ERROR,
                    statusLabelHindi = "बोलने के लिए टैप करें",
                    errorMessageHindi = "कोई शब्द पहचाना नहीं जा सका। कृपया फिर से बोलें।",
                    rmsDbLevel = 0f
                )
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (hasDeliveredCurrentSessionResult) return
            speechServiceResponded = true
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull { it.isNotBlank() }
                ?.trim()
                .orEmpty()
            if (partial.isNotEmpty()) {
                lastPartialText = partial
                _state.value = _state.value.copy(
                    partialOrFinalText = partial
                )

                // Fast auto-commit: if user spoke a command and paused for 1.2s, execute immediately
                // without waiting for SpeechRecognizer's slow silence timeout!
                partialCommitJob?.cancel()
                partialCommitJob = scope.launch {
                    delay(1250L)
                    if (!hasDeliveredCurrentSessionResult && lastPartialText.isNotBlank()) {
                        val captured = lastPartialText.trim()
                        lastPartialText = ""
                        deliverRecognizedText(captured)
                    }
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun encodePcmToWav(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray {
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val blockAlign = (channels * bitsPerSample / 8).toShort()
        val totalDataLen = pcmData.size + 36
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)

        header.put("RIFF".toByteArray(Charsets.US_ASCII))
        header.putInt(totalDataLen)
        header.put("WAVE".toByteArray(Charsets.US_ASCII))
        header.put("fmt ".toByteArray(Charsets.US_ASCII))
        header.putInt(16)
        header.putShort(1)
        header.putShort(channels.toShort())
        header.putInt(sampleRate)
        header.putInt(byteRate)
        header.putShort(blockAlign)
        header.putShort(bitsPerSample.toShort())
        header.put("data".toByteArray(Charsets.US_ASCII))
        header.putInt(pcmData.size)

        val out = ByteArrayOutputStream(44 + pcmData.size)
        out.write(header.array())
        out.write(pcmData)
        return out.toByteArray()
    }
}
