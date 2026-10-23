package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ReadableNotification(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestampMillis: Long
)

class MunnaNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null || sbn.isOngoing || isPaused.value) return
        val pkg = sbn.packageName ?: return
        if (pkg == applicationContext.packageName || pkg == "android" || pkg.startsWith("com.android.systemui")) {
            return
        }

        val extras = sbn.notification?.extras ?: return
        val rawTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        val rawText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
            .orEmpty()

        if (rawTitle.isEmpty() && rawText.isEmpty()) return

        val appLabel = when {
            pkg.contains("whatsapp", ignoreCase = true) -> "WhatsApp"
            pkg.contains("youtube", ignoreCase = true) -> "YouTube"
            pkg.contains("messaging", ignoreCase = true) || pkg.contains("mms", ignoreCase = true) -> "Messages"
            pkg.contains("gm", ignoreCase = true) -> "Gmail"
            else -> try {
                val info = applicationContext.packageManager.getApplicationInfo(pkg, 0)
                applicationContext.packageManager.getApplicationLabel(info).toString()
            } catch (_: Exception) {
                pkg.substringAfterLast('.')
            }
        }

        val entry = ReadableNotification(
            key = sbn.key ?: "${pkg}_${sbn.postTime}",
            packageName = pkg,
            appName = appLabel,
            title = rawTitle.ifEmpty { appLabel },
            text = rawText.ifEmpty { "नया नोटिफिकेशन" },
            timestampMillis = sbn.postTime
        )

        val current = _recentNotifications.value.toMutableList()
        current.removeAll { it.key == entry.key }
        current.add(0, entry)
        _recentNotifications.value = current.take(15)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        val removedKey = sbn?.key ?: return
        _recentNotifications.value = _recentNotifications.value.filterNot { it.key == removedKey }
    }

    companion object {
        private val _recentNotifications = MutableStateFlow<List<ReadableNotification>>(emptyList())
        val recentNotifications: StateFlow<List<ReadableNotification>> = _recentNotifications.asStateFlow()

        private val _isPaused = MutableStateFlow(false)
        val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

        fun setPaused(paused: Boolean) {
            _isPaused.value = paused
        }

        fun clearCapturedNotifications() {
            _recentNotifications.value = emptyList()
        }

        fun isNotificationAccessGranted(context: Context): Boolean {
            return try {
                val enabledListeners = Settings.Secure.getString(
                    context.contentResolver,
                    "enabled_notification_listeners"
                ).orEmpty()
                val myComponent = ComponentName(context, MunnaNotificationListenerService::class.java)
                enabledListeners.contains(myComponent.flattenToString()) ||
                    enabledListeners.contains(context.packageName)
            } catch (_: Exception) {
                false
            }
        }
    }
}
