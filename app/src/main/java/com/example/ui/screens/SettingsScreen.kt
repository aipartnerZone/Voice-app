package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.AppLanguage
import com.example.data.UserSettings
import com.example.service.ReadableNotification
import com.example.ui.theme.CardBorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.ElevatedNavyCard
import com.example.ui.theme.LighterNavyCard
import com.example.ui.theme.SkyBlueBright
import com.example.ui.theme.SkyBluePrimary
import com.example.ui.theme.StatusErrorRed
import com.example.ui.theme.StatusSuccessGreen
import com.example.ui.theme.StatusWarningAmber
import com.example.ui.theme.TextMutedBlueGray
import com.example.ui.theme.TextWhite
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(
    settings: UserSettings,
    isMicGranted: Boolean,
    isOverlayGranted: Boolean,
    isNotificationGranted: Boolean,
    isFloatingServiceRunning: Boolean,
    inAppFloatingBubbleVisible: Boolean,
    recentNotifications: List<ReadableNotification>,
    onLanguageChange: (AppLanguage) -> Unit,
    onSpokenResponsesChange: (Boolean) -> Unit,
    onSpeechRateChange: (Float) -> Unit,
    onVoicePitchChange: (Float) -> Unit,
    onReducedMotionChange: (Boolean) -> Unit,
    onTestVoiceClick: () -> Unit,
    onToggleFloatingMic: (Boolean) -> String?,
    onRequestMicPermission: () -> Unit,
    onOpenOverlayPermissionSettings: () -> Unit,
    onOpenNotificationListenerSettings: () -> Unit,
    onToggleNotificationPaused: (Boolean) -> Unit,
    onReadNotificationAloud: (String) -> Unit,
    onClearHistory: () -> Unit,
    onNavigateBackHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBackHome()
    }

    var floatingStatusBanner by remember { mutableStateOf<String?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavyBackground)
            .testTag("settings_screen_list"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
            ) {
                Text(
                    text = "सेटिंग्स और अनुमतियाँ (Settings)",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "मुन्ना AI की भाषा, आवाज़, फ़्लोटिंग माइक और फ़ोन अनुमतियाँ प्रबंधित करें",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMutedBlueGray
                )
            }
        }

        // 1. Language Setting (Hindi / Hinglish / English)
        item {
            SettingsCardWrapper(
                title = "ऐप की भाषा (App Language)",
                subtitle = "मुन्ना किस भाषा में आपसे बात करेगा",
                icon = Icons.Default.Language
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AppLanguage.entries.forEach { lang ->
                        val selected = settings.language == lang
                        FilterChip(
                            selected = selected,
                            onClick = { onLanguageChange(lang) },
                            label = {
                                Text(
                                    text = lang.displayName,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SkyBluePrimary,
                                selectedLabelColor = DeepNavyBackground,
                                containerColor = ElevatedNavyCard,
                                labelColor = TextWhite
                            ),
                            modifier = Modifier.testTag("lang_chip_${lang.code}")
                        )
                    }
                }
            }
        }

        // 2. Assistant Voice, Spoken Responses & Speech Rate
        item {
            SettingsCardWrapper(
                title = "आवाज़ और बोलने की गति (Voice & TTS)",
                subtitle = "Text-to-Speech जवाब और रफ़्तार",
                icon = Icons.Default.RecordVoiceOver
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "बोलकर जवाब दें (Spoken Responses)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextWhite
                        )
                        Text(
                            text = "कमांड पूरा होने पर मुन्ना बोलकर पुष्टि करेगा",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMutedBlueGray
                        )
                    }
                    Switch(
                        checked = settings.spokenResponsesEnabled,
                        onCheckedChange = onSpokenResponsesChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepNavyBackground,
                            checkedTrackColor = SkyBluePrimary
                        ),
                        modifier = Modifier.testTag("spoken_responses_switch")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "बोलने की गति (Speech Rate): ${(settings.speechRate * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = SkyBlueBright
                )
                Slider(
                    value = settings.speechRate,
                    onValueChange = onSpeechRateChange,
                    valueRange = 0.6f..1.5f,
                    colors = SliderDefaults.colors(
                        thumbColor = SkyBluePrimary,
                        activeTrackColor = SkyBluePrimary
                    )
                )

                Text(
                    text = "आवाज़ की पिच (Voice Pitch): ${(settings.voicePitch * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    color = SkyBlueBright
                )
                Slider(
                    value = settings.voicePitch,
                    onValueChange = onVoicePitchChange,
                    valueRange = 0.75f..1.3f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanAccent,
                        activeTrackColor = CyanAccent
                    )
                )

                OutlinedButton(
                    onClick = onTestVoiceClick,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, SkyBluePrimary),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SkyBluePrimary),
                    modifier = Modifier.testTag("test_voice_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "मुन्ना की आवाज़ टेस्ट करें")
                }
            }
        }

        // 3. Device Permissions Status (Microphone, Overlay, Notification Access)
        item {
            SettingsCardWrapper(
                title = "फ़ोन अनुमतियों की स्थिति (Device Permissions)",
                subtitle = "असली Android सिस्टम अनुमतियाँ",
                icon = Icons.Default.Mic
            ) {
                PermissionRowItem(
                    label = "माइक्रोफ़ोन अनुमति (Microphone)",
                    isGranted = isMicGranted,
                    actionLabel = if (isMicGranted) "चालू है" else "अनुमति दें",
                    onActionClick = onRequestMicPermission
                )

                PermissionRowItem(
                    label = "अन्य ऐप्स के ऊपर दिखना (Overlay Permission)",
                    isGranted = isOverlayGranted,
                    actionLabel = if (isOverlayGranted) "चालू है" else "सेटिंग्स खोलें",
                    onActionClick = onOpenOverlayPermissionSettings
                )

                PermissionRowItem(
                    label = "नोटिफिकेशन पढ़ने की अनुमति (Notification Access)",
                    isGranted = isNotificationGranted,
                    actionLabel = if (isNotificationGranted) "चालू है" else "सेटिंग्स खोलें",
                    onActionClick = onOpenNotificationListenerSettings
                )
            }
        }

        // 4. Floating Microphone Toggle (Section 6 — Real Native Android Overlay + In-App Bubble)
        item {
            SettingsCardWrapper(
                title = "फ़्लोटिंग माइक (Floating Microphone)",
                subtitle = "स्क्रीन के किनारे नीला ड्रैगेबल माइक बटन",
                icon = Icons.Default.Layers
            ) {
                val isActive = isFloatingServiceRunning || inAppFloatingBubbleVisible
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isFloatingServiceRunning) {
                                "सिस्टम-वाइड ओवरले चालू है (Active)"
                            } else if (inAppFloatingBubbleVisible) {
                                "इन-ऐप फ़्लोटिंग माइक चालू है"
                            } else {
                                "फ़्लोटिंग माइक बंद है (Inactive)"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isActive) StatusSuccessGreen else TextWhite,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "अन्य ऐप्स के ऊपर माइक दिखाने के लिए 'Display over other apps' अनुमति आवश्यक है।",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMutedBlueGray
                        )
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { enable ->
                            floatingStatusBanner = onToggleFloatingMic(enable)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepNavyBackground,
                            checkedTrackColor = CyanAccent
                        ),
                        modifier = Modifier.testTag("floating_mic_switch")
                    )
                }

                if (!floatingStatusBanner.isNullOrBlank()) {
                    Surface(
                        color = ElevatedNavyCard,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SkyBluePrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = floatingStatusBanner.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SkyBlueBright,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                if (!isOverlayGranted) {
                    OutlinedButton(
                        onClick = onOpenOverlayPermissionSettings,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, CyanAccent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent)
                    ) {
                        Text(text = "Android Overlay सेटिंग्स खोलें")
                    }
                }
            }
        }

        // 5. "Hey Munna" Wake-Word Setup Status (Section 7 — Honest Engineering Status)
        item {
            SettingsCardWrapper(
                title = "“Hey Munna” वेक-वर्ड (Wake-Word Status)",
                subtitle = "बिना बटन दबाए बोलकर एक्टिवेट करने की सुविधा",
                icon = Icons.Default.GraphicEq
            ) {
                Surface(
                    color = StatusWarningAmber.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StatusWarningAmber.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "स्थिति: अभी बंद रखा गया है (Disabled — Phase 5 Planned)",
                            style = MaterialTheme.typography.labelLarge,
                            color = StatusWarningAmber
                        )
                        Text(
                            text = "क्यों बंद है: लगातार बैकग्राउंड में 'Hey Munna' सुनने के लिए ऑन-डिवाइस DSP/Porcupine Hotword इंजन और लगातार माइक्रोफ़ोन एक्सेस चाहिए, जिससे बैटरी खर्च बढ़ती है। आपकी गोपनीयता और बैटरी बचाने के लिए मुन्ना बिना अनुमति के बैकग्राउंड ऑडियो रिकॉर्ड नहीं करता।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextWhite
                        )
                        Text(
                            text = "काम करने वाला विकल्प: आप ऊपर दिए गए 'फ़्लोटिंग माइक (Floating Microphone)' को चालू कर सकते हैं!",
                            style = MaterialTheme.typography.labelMedium,
                            color = CyanAccent
                        )
                    }
                }
            }
        }

        // 6. Notification & Message Reading Module (Section 9)
        item {
            SettingsCardWrapper(
                title = "नोटिफिकेशन और WhatsApp रीडर (Notification Reader)",
                subtitle = "अनुमति मिलने पर आने वाले नोटिफिकेशन बोलकर सुनाएँ",
                icon = Icons.Default.NotificationsActive
            ) {
                Text(
                    text = "गोपनीयता नियम: मुन्ना WhatsApp या किसी ऐप के प्राइवेट डेटाबेस को नहीं छूता और न ही आपके मैसेज किसी AI सर्वर पर भेजता है। यह केवल Android Notification Access द्वारा अनुमत नोटिफिकेशन को आपके कहने पर पढ़ता है।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMutedBlueGray
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "नोटिफिकेशन पढ़ना कुछ समय के लिए रोकें (Pause)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextWhite,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = settings.notificationReaderPaused,
                        onCheckedChange = onToggleNotificationPaused,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepNavyBackground,
                            checkedTrackColor = StatusWarningAmber
                        )
                    )
                }

                if (!isNotificationGranted) {
                    Button(
                        onClick = onOpenNotificationListenerSettings,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SkyBluePrimary,
                            contentColor = DeepNavyBackground
                        ),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = "Android सेटिंग्स में Notification Access चालू करें",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Text(
                        text = "हाल के पकड़े गए नोटिफिकेशन (${recentNotifications.size}):",
                        style = MaterialTheme.typography.labelLarge,
                        color = StatusSuccessGreen
                    )
                    if (recentNotifications.isEmpty()) {
                        Text(
                            text = "अभी कोई नया नोटिफिकेशन नहीं आया है। आप होम स्क्रीन पर बोल सकते हैं: “Munna, WhatsApp message padho”",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMutedBlueGray
                        )
                    } else {
                        recentNotifications.take(3).forEach { notif ->
                            Surface(
                                color = ElevatedNavyCard,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${notif.appName}: ${notif.title}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = SkyBlueBright
                                        )
                                        Text(
                                            text = notif.text,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = TextWhite,
                                            maxLines = 2
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onReadNotificationAloud("${notif.appName} से संदेश: ${notif.title}, ${notif.text}")
                                        },
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text("सुनें")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Reduced Motion & Privacy Controls
        item {
            SettingsCardWrapper(
                title = "गोपनीयता और परफॉर्मेंस (Privacy & Performance)",
                subtitle = "बजट Android फ़ोन और डेटा सुरक्षा विकल्प",
                icon = Icons.Default.PrivacyTip
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "हल्का एनिमेशन मोड (Reduced Motion)",
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextWhite
                        )
                        Text(
                            text = "बैटरी बचाने और बजट फ़ोन पर तेज़ परफॉर्मेंस के लिए",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMutedBlueGray
                        )
                    }
                    Switch(
                        checked = settings.reducedMotion,
                        onCheckedChange = onReducedMotionChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeepNavyBackground,
                            checkedTrackColor = SkyBluePrimary
                        )
                    )
                }

                OutlinedButton(
                    onClick = onClearHistory,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, StatusErrorRed.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusErrorRed),
                    modifier = Modifier.testTag("settings_clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "पूरी कमांड हिस्ट्री साफ़ करें (Clear Command History)")
                }
            }
        }

        // 8. App Version & About Section
        item {
            SettingsCardWrapper(
                title = "Munna AI के बारे में (About & Architecture)",
                subtitle = "संस्करण 1.0 • Native Android Kotlin + Jetpack Compose",
                icon = Icons.Default.Info
            ) {
                Text(
                    text = "• टैगलाइन: “Bolo, Munna Karega!”\n" +
                        "• काम करने वाले फीचर्स: हिंदी/हिंग्लिश वॉइस रिकग्निशन, लोकल कमांड पार्सर, असली Android ऐप लॉन्चर (YouTube, WhatsApp, Chrome, Files), समय/तारीख/बैटरी जानकारी, Text-to-Speech, Room कमांड हिस्ट्री, फ़्लोटिंग माइक सर्विस, और Gemini AI चैट।\n" +
                        "• गोपनीयता: कोई कच्ची ऑडियो रिकॉर्डिंग सेव नहीं की जाती।",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMutedBlueGray
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun SettingsCardWrapper(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = LighterNavyCard),
        border = BorderStroke(1.dp, CardBorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = ElevatedNavyCard,
                    modifier = Modifier.size(38.dp)
                ) {
                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = SkyBluePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMutedBlueGray
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun PermissionRowItem(
    label: String,
    isGranted: Boolean,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Surface(
        color = ElevatedNavyCard,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = if (isGranted) StatusSuccessGreen else StatusWarningAmber,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextWhite
                )
            }
            OutlinedButton(
                onClick = onActionClick,
                shape = RoundedCornerShape(50),
                border = BorderStroke(
                    1.dp,
                    if (isGranted) StatusSuccessGreen.copy(alpha = 0.5f) else SkyBluePrimary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isGranted) StatusSuccessGreen else SkyBlueBright
                )
            }
        }
    }
}
