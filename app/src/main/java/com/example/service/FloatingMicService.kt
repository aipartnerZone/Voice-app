package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppLanguage
import com.example.data.CommandRepository
import com.example.data.MunnaDatabase
import com.example.domain.AppActionHandler
import com.example.domain.CommandParser
import com.example.voice.MicStatus
import com.example.voice.TextToSpeechManager
import com.example.voice.VoiceRecognitionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs

class FloatingMicService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var floatingRootView: View? = null
    private var micButtonContainer: FrameLayout? = null
    private var statusBadgeView: TextView? = null

    private var voiceManager: VoiceRecognitionManager? = null
    private var ttsManager: TextToSpeechManager? = null
    private lateinit var actionHandler: AppActionHandler
    private lateinit var commandRepository: CommandRepository

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        actionHandler = AppActionHandler(applicationContext)
        commandRepository = CommandRepository(
            MunnaDatabase.getInstance(applicationContext).commandHistoryDao()
        )
        ttsManager = TextToSpeechManager(applicationContext)
        voiceManager = VoiceRecognitionManager(applicationContext) { recognizedText ->
            handleFloatingCommand(recognizedText)
        }

        serviceScope.launch {
            voiceManager?.state?.collectLatest { voiceState ->
                updateOverlayVisualState(voiceState.status)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_FLOATING_MIC) {
            stopSelf()
            return START_NOT_STICKY
        }

        if (!Settings.canDrawOverlays(this)) {
            _isRunning.value = false
            stopSelf()
            return START_NOT_STICKY
        }

        val hasMicPerm = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasMicPerm) {
            _isRunning.value = false
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val notification = buildForegroundNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            showFloatingOverlay()
            _isRunning.value = true
        } catch (_: Exception) {
            _isRunning.value = false
            stopSelf()
        }

        return START_STICKY
    }

    private fun handleFloatingCommand(recognizedText: String) {
        serviceScope.launch {
            val parsed = CommandParser.parse(recognizedText)
            val result = actionHandler.execute(parsed, AppLanguage.HINDI)
            ttsManager?.speak(
                text = result.spokenResponse,
                language = AppLanguage.HINDI
            )
            commandRepository.logCommand(
                recognizedText = recognizedText,
                actionTitle = "[Floating Mic] ${result.actionTitle}",
                assistantResponse = result.spokenResponse,
                category = result.category,
                isSuccess = result.isSuccess
            )
        }
    }

    private fun showFloatingOverlay() {
        if (floatingRootView != null) return
        val wm = getSystemService(Context.WINDOW_SERVICE) as? WindowManager ?: return
        windowManager = wm

        val density = resources.displayMetrics.density
        fun Int.dp(): Int = (this * density).toInt()

        val rootContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }

        // Small status chip above the floating mic
        val badge = TextView(this).apply {
            text = "मुन्ना"
            textSize = 11f
            setTextColor(Color.WHITE)
            setPadding(8.dp(), 3.dp(), 8.dp(), 3.dp())
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12.dp().toFloat()
                setColor(Color.parseColor("#E60F1A34"))
                setStroke(1.dp(), Color.parseColor("#38BDF8"))
            }
        }
        statusBadgeView = badge

        // Circular Sky-Blue Button with Soft Cyan Glow
        val circleFrame = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(60.dp(), 60.dp()).apply {
                topMargin = 4.dp()
            }
            background = createBubbleDrawable(isListening = false, density = density)
            elevation = 10.dp().toFloat()
        }
        micButtonContainer = circleFrame

        val micIcon = ImageView(this).apply {
            setImageResource(R.drawable.ic_munna_logo)
            layoutParams = FrameLayout.LayoutParams(36.dp(), 36.dp(), Gravity.CENTER)
        }
        circleFrame.addView(micIcon)

        // Small Dismiss 'X' pill below so user can easily hide/stop the floating mic
        val closeButton = TextView(this).apply {
            text = "✕ बंद"
            textSize = 10f
            setTextColor(Color.parseColor("#CBD5E1"))
            setPadding(8.dp(), 2.dp(), 8.dp(), 2.dp())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 4.dp()
            }
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 10.dp().toFloat()
                setColor(Color.parseColor("#CC070D1F"))
                setStroke(1, Color.parseColor("#334155"))
            }
            setOnClickListener {
                stopSelf()
            }
        }

        rootContainer.addView(badge)
        rootContainer.addView(circleFrame)
        rootContainer.addView(closeButton)

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16.dp()
            y = 220.dp()
        }

        // Draggable + Tap listener
        circleFrame.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDrag = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDrag = false
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (initialTouchX - event.rawX).toInt() // Gravity.END inverts X
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (abs(dx) > 10 || abs(dy) > 10) {
                            isDrag = true
                            params.x = (initialX + dx).coerceAtLeast(0)
                            params.y = (initialY + dy).coerceAtLeast(48)
                            try {
                                wm.updateViewLayout(rootContainer, params)
                            } catch (_: Exception) {
                            }
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!isDrag) {
                            v.performClick()
                            toggleVoiceListening()
                        }
                        return true
                    }
                }
                return false
            }
        })

        try {
            wm.addView(rootContainer, params)
            floatingRootView = rootContainer
        } catch (_: Exception) {
            _isRunning.value = false
        }
    }

    private fun toggleVoiceListening() {
        val currentStatus = voiceManager?.state?.value?.status ?: MicStatus.IDLE
        if (currentStatus == MicStatus.LISTENING) {
            voiceManager?.finishOrCancelListening()
        } else {
            voiceManager?.startListening(AppLanguage.HINDI)
        }
    }

    private fun updateOverlayVisualState(status: MicStatus) {
        val density = resources.displayMetrics.density
        val isListening = status == MicStatus.LISTENING
        micButtonContainer?.background = createBubbleDrawable(isListening = isListening, density = density)
        statusBadgeView?.text = when (status) {
            MicStatus.IDLE -> "मुन्ना"
            MicStatus.LISTENING -> "सुन रहा हूँ…"
            MicStatus.PROCESSING -> "समझ रहा हूँ…"
            MicStatus.SUCCESS -> "हो गया!"
            MicStatus.ERROR -> "फिर बोलें"
        }
    }

    private fun createBubbleDrawable(isListening: Boolean, density: Float): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            colors = if (isListening) {
                intArrayOf(
                    Color.parseColor("#22D3EE"),
                    Color.parseColor("#0284C7")
                )
            } else {
                intArrayOf(
                    Color.parseColor("#38BDF8"),
                    Color.parseColor("#0369A1")
                )
            }
            setStroke(
                (if (isListening) (3 * density).toInt() else (2 * density).toInt()),
                Color.parseColor(if (isListening) "#A5F3FC" else "#7DD3FC")
            )
        }
    }

    private fun buildForegroundNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.floating_mic_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Munna AI Floating Microphone Service"
            }
            nm.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(this, MainActivity::class.java)
        val openPending = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FloatingMicService::class.java).apply {
            action = ACTION_STOP_FLOATING_MIC
        }
        val stopPending = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_munna_logo)
            .setContentTitle(getString(R.string.floating_mic_notification_title))
            .setContentText(getString(R.string.floating_mic_notification_text))
            .setContentIntent(openPending)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "बंद करें (Stop)",
                stopPending
            )
            .build()
    }

    override fun onDestroy() {
        _isRunning.value = false
        try {
            floatingRootView?.let { windowManager?.removeView(it) }
        } catch (_: Exception) {
        }
        floatingRootView = null
        voiceManager?.release()
        ttsManager?.shutdown()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "munna_floating_mic_channel"
        private const val NOTIFICATION_ID = 1009
        const val ACTION_STOP_FLOATING_MIC = "com.example.action.STOP_FLOATING_MIC"

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    }
}
