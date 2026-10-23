package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AppLanguage
import com.example.data.CommandHistoryEntity
import com.example.domain.TargetApp
import com.example.ui.LastActionFeedback
import com.example.ui.theme.BrandCyanGlow
import com.example.ui.theme.BrandDeepBlueOrb
import com.example.ui.theme.BrandRoyalBlue
import com.example.ui.theme.IceWhiteBackground
import com.example.ui.theme.LighterSkyBlueSurface
import com.example.ui.theme.NavyTextPrimary
import com.example.ui.theme.NavyTextSecondary
import com.example.ui.theme.PureWhiteSurface
import com.example.ui.theme.SoftBlueBorder
import com.example.ui.theme.SoftSkyBlueCard
import com.example.ui.theme.StatusErrorRed
import com.example.ui.theme.StatusSuccessGreen
import com.example.voice.MicStatus
import com.example.voice.VoiceEngineMode
import com.example.voice.VoiceUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    language: AppLanguage,
    userDisplayName: String,
    voiceState: VoiceUiState,
    lastFeedback: LastActionFeedback?,
    recentHistory: List<CommandHistoryEntity>,
    reducedMotion: Boolean,
    isSpeaking: Boolean,
    isAppInstalledProvider: (TargetApp) -> Boolean,
    onMicClick: () -> Unit,
    onFinishListening: () -> Unit,
    onCancelListening: () -> Unit,
    onSelectEngineMode: (VoiceEngineMode) -> Unit,
    onLaunchSystemSpeechDialog: () -> Unit,
    onShortcutClick: (TargetApp) -> Unit,
    onQuickCommandClick: (String) -> Unit,
    onSpeakText: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onOpenLoginScreen: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenAiChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spokenGreeting = "नमस्ते भाई! मैं मुन्ना हूँ। आज आपका दिन कैसा है? कुछ पूछना है?"
    var showInlineCommandBox by rememberSaveable { mutableStateOf(false) }
    var customCommandInput by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF3F9FF),
                        Color(0xFFFFFFFF),
                        Color(0xFFEAF5FF)
                    )
                )
            )
            .testTag("home_screen_list"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Top Header: Circular Munna Avatar + "Munna AI" ("हैलो, मैं हूँ Munna!") + Login Chip + Settings Icon
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SoftSkyBlueCard,
                        border = BorderStroke(2.dp, Color(0xFFB4DAFE)),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .clickable { onOpenLoginScreen() }
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_munna_avatar),
                            contentDescription = "Munna AI Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Column {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(
                                    SpanStyle(
                                        color = NavyTextPrimary,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                ) {
                                    append("Munna ")
                                }
                                withStyle(
                                    SpanStyle(
                                        color = BrandRoyalBlue,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                ) {
                                    append("AI")
                                }
                            },
                            fontSize = 24.sp,
                            modifier = Modifier.testTag("home_title_text")
                        )
                        Text(
                            text = "हैलो, मैं हूँ Munna!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NavyTextSecondary
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SoftSkyBlueCard,
                        border = BorderStroke(1.dp, SoftBlueBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onOpenLoginScreen() }
                            .testTag("open_login_screen_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Login Options",
                                tint = BrandRoyalBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = userDisplayName,
                                style = MaterialTheme.typography.labelMedium,
                                color = BrandRoyalBlue,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .testTag("top_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = NavyTextPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        // 2. Greeting Card ("👋 नमस्ते! आज आपका दिन कैसा है? कुछ पूछना है?") + Right Callout ("मैं मुन्ना हूँ ◡̈")
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .testTag("greeting_card"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FE)),
                    border = BorderStroke(1.dp, Color(0xFFC7E3FD)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "👋 नमस्ते!",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = NavyTextPrimary,
                                modifier = Modifier.testTag("greeting_hindi_text")
                            )

                            IconButton(
                                onClick = {
                                    if (isSpeaking) onStopSpeaking() else onSpeakText(spokenGreeting)
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .testTag("speak_greeting_button")
                            ) {
                                Icon(
                                    imageVector = if (isSpeaking) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Speak greeting",
                                    tint = BrandRoyalBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Text(
                            text = "आज आपका दिन\nकैसा है? कुछ पूछना है?",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                lineHeight = 23.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            color = Color(0xFF1B3A61)
                        )
                    }
                }

                // Playful handwritten-style note on the right matching the reference image
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .rotate(-8f)
                ) {
                    Text(
                        text = "मैं\nमुन्ना हूँ",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 15.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = BrandRoyalBlue,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "◡̈",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandRoyalBlue
                    )
                }
            }
        }

        // 3. Central Glowing Blue Orb Microphone with Left & Right Soundwave Bars
        item {
            GlowingBlueOrbMicSection(
                voiceState = voiceState,
                reducedMotion = reducedMotion,
                onMicClick = onMicClick,
                onFinishListening = onFinishListening,
                onCancelListening = onCancelListening,
                onToggleTypeInput = { showInlineCommandBox = !showInlineCommandBox },
                onSelectEngineMode = onSelectEngineMode,
                onLaunchSystemSpeechDialog = onLaunchSystemSpeechDialog
            )
        }

        // Expandable Inline Command Input Box ("या लिखकर भी पूछ सकते हैं")
        item {
            AnimatedVisibility(visible = showInlineCommandBox) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhiteSurface),
                    border = BorderStroke(1.dp, SoftBlueBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = customCommandInput,
                                onValueChange = { customCommandInput = it },
                                placeholder = {
                                    Text(
                                        text = "लिखें: YouTube खोलो, WhatsApp खोलो, समय बताओ…",
                                        color = NavyTextSecondary,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandRoyalBlue,
                                    unfocusedBorderColor = SoftBlueBorder,
                                    focusedTextColor = NavyTextPrimary,
                                    unfocusedTextColor = NavyTextPrimary,
                                    focusedContainerColor = IceWhiteBackground,
                                    unfocusedContainerColor = IceWhiteBackground
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (customCommandInput.isNotBlank()) {
                                            onQuickCommandClick(customCommandInput)
                                            customCommandInput = ""
                                        }
                                    }
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("home_command_input")
                            )

                            IconButton(
                                onClick = {
                                    if (customCommandInput.isNotBlank()) {
                                        onQuickCommandClick(customCommandInput)
                                        customCommandInput = ""
                                    }
                                },
                                enabled = customCommandInput.isNotBlank(),
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (customCommandInput.isNotBlank()) BrandRoyalBlue
                                        else SoftSkyBlueCard
                                    )
                                    .testTag("home_command_send_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send command",
                                    tint = if (customCommandInput.isNotBlank()) Color.White else NavyTextSecondary
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val quickList = listOf(
                                "Munna, YouTube kholo",
                                "WhatsApp kholo",
                                "Chrome open karo",
                                "Meri files kholo",
                                "Munna, abhi kitne baje hain?"
                            )
                            quickList.forEachIndexed { idx, cmd ->
                                AssistChip(
                                    onClick = { onQuickCommandClick(cmd) },
                                    label = {
                                        Text(
                                            text = cmd,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = NavyTextPrimary
                                        )
                                    },
                                    colors = AssistChipDefaults.assistChipColors(
                                        containerColor = SoftSkyBlueCard
                                    ),
                                    border = BorderStroke(1.dp, SoftBlueBorder),
                                    modifier = Modifier.testTag("quick_cmd_chip_$idx")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Voice Error / Help Banner
        item {
            AnimatedVisibility(visible = !voiceState.errorMessageHindi.isNullOrBlank()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .testTag("voice_error_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFEF2F2)
                    ),
                    border = BorderStroke(1.dp, StatusErrorRed.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Error",
                                tint = StatusErrorRed
                            )
                            Text(
                                text = "आवाज़ सहायता",
                                style = MaterialTheme.typography.titleMedium,
                                color = StatusErrorRed,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = voiceState.errorMessageHindi.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = NavyTextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    onSelectEngineMode(VoiceEngineMode.DIRECT_HARDWARE_MIC)
                                    onMicClick()
                                },
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, BrandRoyalBlue)
                            ) {
                                Text("सीधा माइक आज़माएँ", color = BrandRoyalBlue)
                            }
                            OutlinedButton(
                                onClick = onLaunchSystemSpeechDialog,
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, BrandRoyalBlue)
                            ) {
                                Text("सिस्टम माइक पॉपअप", color = BrandRoyalBlue)
                            }
                        }
                    }
                }
            }
        }

        // Last Recognized Command & Action Feedback Card
        item {
            AnimatedVisibility(visible = lastFeedback != null) {
                lastFeedback?.let { feedback ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 600.dp)
                            .testTag("last_command_feedback_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PureWhiteSurface),
                        border = BorderStroke(
                            1.5.dp,
                            if (feedback.isSuccess) StatusSuccessGreen.copy(alpha = 0.6f)
                            else StatusErrorRed.copy(alpha = 0.6f)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (feedback.isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (feedback.isSuccess) StatusSuccessGreen else StatusErrorRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = feedback.actionTitle,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (feedback.isSuccess) StatusSuccessGreen else StatusErrorRed,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                IconButton(
                                    onClick = { onSpeakText(feedback.spokenResponse) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                        contentDescription = "Speak response again",
                                        tint = BrandRoyalBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Text(
                                text = "आपने कहा: “${feedback.recognizedCommand}”",
                                style = MaterialTheme.typography.bodyMedium,
                                color = BrandRoyalBlue,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = "मुन्ना: “${feedback.spokenResponse}”",
                                style = MaterialTheme.typography.bodyLarge,
                                color = NavyTextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 4. "⚡ जल्दी करने के लिए" (4 Square App Shortcut Cards in a Single Row matching the design!)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = LighterSkyBlueSurface),
                border = BorderStroke(1.dp, Color(0xFFD0E6FD)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = BrandRoyalBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "जल्दी करने के लिए",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = NavyTextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SquareShortcutCard(
                            targetApp = TargetApp.YOUTUBE,
                            iconRes = R.drawable.ic_shortcut_youtube,
                            onClick = { onShortcutClick(TargetApp.YOUTUBE) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shortcut_card_youtube")
                        )
                        SquareShortcutCard(
                            targetApp = TargetApp.WHATSAPP,
                            iconRes = R.drawable.ic_shortcut_whatsapp,
                            onClick = { onShortcutClick(TargetApp.WHATSAPP) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shortcut_card_whatsapp")
                        )
                        SquareShortcutCard(
                            targetApp = TargetApp.CHROME,
                            iconRes = R.drawable.ic_shortcut_chrome,
                            onClick = { onShortcutClick(TargetApp.CHROME) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shortcut_card_chrome")
                        )
                        SquareShortcutCard(
                            targetApp = TargetApp.FILES,
                            iconRes = R.drawable.ic_shortcut_files,
                            onClick = { onShortcutClick(TargetApp.FILES) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("shortcut_card_files")
                        )
                    }
                }
            }
        }

        // Recent Commands History Section (if any commands have been run)
        if (recentHistory.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "हाल के कमांड्स",
                        style = MaterialTheme.typography.titleMedium,
                        color = NavyTextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "इतिहास देखें →",
                        style = MaterialTheme.typography.labelLarge,
                        color = BrandRoyalBlue,
                        modifier = Modifier
                            .clickable { onOpenHistory() }
                            .padding(vertical = 4.dp, horizontal = 8.dp)
                            .testTag("view_all_history_button")
                    )
                }
            }

            items(recentHistory.take(3), key = { it.id }) { item ->
                RecentCommandItemCard(
                    item = item,
                    onRepeatClick = { onQuickCommandClick(item.recognizedText) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GlowingBlueOrbMicSection(
    voiceState: VoiceUiState,
    reducedMotion: Boolean,
    onMicClick: () -> Unit,
    onFinishListening: () -> Unit,
    onCancelListening: () -> Unit,
    onToggleTypeInput: () -> Unit,
    onSelectEngineMode: (VoiceEngineMode) -> Unit,
    onLaunchSystemSpeechDialog: () -> Unit
) {
    val isListening = voiceState.status == MicStatus.LISTENING
    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening && !reducedMotion) {
            (1.12f + (voiceState.rmsDbLevel / 40f)).coerceAtMost(1.30f)
        } else {
            1.04f
        },
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb_scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row with Left Soundwave Bars + Glowing Blue Orb + Right Soundwave Bars (matching the reference image!)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            // Left Vertical Soundwave Bars
            SideSoundwaveBars(
                isListening = isListening,
                rmsDb = voiceState.rmsDbLevel,
                barColor = BrandRoyalBlue
            )

            Spacer(modifier = Modifier.width(16.dp))

            // Central Glowing Blue Orb Button
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(168.dp)
            ) {
                // Concentric outer glow rings
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(if (reducedMotion) 1f else pulseScale)
                ) {
                    val r = size.minDimension / 2f
                    drawCircle(
                        color = Color(0xFFD6EDFF),
                        radius = r
                    )
                    drawCircle(
                        color = Color(0xFFB3DFFE),
                        radius = r * 0.84f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                BrandCyanGlow.copy(alpha = 0.65f),
                                BrandRoyalBlue.copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        ),
                        radius = r * 0.92f
                    )
                }

                // Main 3D Radial Blue Orb
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(124.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.radialGradient(
                                colors = if (isListening) {
                                    listOf(
                                        Color(0xFF00B4FF),
                                        Color(0xFF0066E6),
                                        Color(0xFF002B7A)
                                    )
                                } else {
                                    listOf(
                                        Color(0xFF0077FF),
                                        Color(0xFF0044AA),
                                        Color(0xFF002266)
                                    )
                                }
                            )
                        )
                        .clickable {
                            if (isListening) onFinishListening() else onMicClick()
                        }
                        .testTag("central_mic_button")
                ) {
                    // Subtle inner cyan rim
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            color = Color(0xFF67E8F9).copy(alpha = 0.55f),
                            radius = (size.minDimension / 2f) - 3.dp.toPx(),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
                        )
                    }

                    Icon(
                        imageVector = if (isListening) Icons.Default.Check else Icons.Default.Mic,
                        contentDescription = if (isListening) "Done speaking" else "Tap microphone to speak",
                        tint = Color.White,
                        modifier = Modifier.size(54.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Right Vertical Soundwave Bars
            SideSoundwaveBars(
                isListening = isListening,
                rmsDb = voiceState.rmsDbLevel,
                barColor = BrandCyanGlow.copy(alpha = 0.7f)
            )
        }

        // Bold Status Label Below Orb: "बोलने के लिए टैप करें"
        Text(
            text = voiceState.statusLabelHindi,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            ),
            color = when (voiceState.status) {
                MicStatus.LISTENING -> BrandRoyalBlue
                MicStatus.SUCCESS -> StatusSuccessGreen
                MicStatus.ERROR -> StatusErrorRed
                else -> NavyTextPrimary
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("mic_status_label")
        )

        // Live Recognized Words Preview
        if (voiceState.partialOrFinalText.isNotBlank()) {
            Surface(
                color = SoftSkyBlueCard,
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, SoftBlueBorder)
            ) {
                Text(
                    text = "“${voiceState.partialOrFinalText}”",
                    style = MaterialTheme.typography.bodyLarge,
                    color = BrandRoyalBlue,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .testTag("recognized_speech_text")
                )
            }
        }

        if (isListening) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onFinishListening,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StatusSuccessGreen,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("finish_listening_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("बोल दिया (Done)", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onCancelListening,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, StatusErrorRed),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusErrorRed),
                    modifier = Modifier.testTag("cancel_listening_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("रद्द करें")
                }
            }
        } else {
            // Pill Button: "या लिखकर भी पूछ सकते हैं" matching the reference image!
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFDCEEFF),
                border = BorderStroke(1.dp, Color(0xFFBBDDFD)),
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onToggleTypeInput() }
                    .testTag("toggle_type_input_pill")
            ) {
                Text(
                    text = "या लिखकर भी पूछ सकते हैं",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color(0xFF1E4E8C),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // Compact Mic Engine Mode Selector Row
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                VoiceEngineMode.entries.forEach { mode ->
                    val selected = voiceState.engineMode == mode
                    FilterChip(
                        selected = selected,
                        onClick = { onSelectEngineMode(mode) },
                        label = {
                            Text(
                                text = mode.labelHindi,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandRoyalBlue,
                            selectedLabelColor = Color.White,
                            containerColor = PureWhiteSurface,
                            labelColor = NavyTextSecondary
                        )
                    )
                }

                AssistChip(
                    onClick = onLaunchSystemSpeechDialog,
                    label = {
                        Text(
                            text = "सिस्टम माइक",
                            style = MaterialTheme.typography.labelMedium,
                            color = BrandRoyalBlue
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = BrandRoyalBlue,
                            modifier = Modifier.size(15.dp)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(containerColor = PureWhiteSurface),
                    border = BorderStroke(1.dp, SoftBlueBorder),
                    modifier = Modifier.testTag("system_speech_dialog_button")
                )
            }
        }
    }
}

@Composable
private fun SideSoundwaveBars(
    isListening: Boolean,
    rmsDb: Float,
    barColor: Color
) {
    val activeFactor = if (isListening) (rmsDb / 10f).coerceIn(0.35f, 1.35f) else 0.65f
    val baseHeights = listOf(14f, 26f, 38f, 24f, 14f)
    Row(
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        baseHeights.forEach { baseH ->
            val h = (baseH * activeFactor).coerceIn(10f, 48f).dp
            Box(
                modifier = Modifier
                    .width(4.5.dp)
                    .height(h)
                    .clip(RoundedCornerShape(50))
                    .background(barColor)
            )
        }
    }
}

@Composable
private fun SquareShortcutCard(
    targetApp: TargetApp,
    iconRes: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PureWhiteSurface,
        border = BorderStroke(1.dp, Color(0xFFDCEBFA)),
        shadowElevation = 2.dp,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = targetApp.displayName,
                modifier = Modifier.size(34.dp)
            )
            Text(
                text = targetApp.displayName,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = NavyTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentCommandItemCard(
    item: CommandHistoryEntity,
    onRepeatClick: () -> Unit
) {
    val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date(item.timestampMillis))
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onRepeatClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhiteSurface),
        border = BorderStroke(1.dp, SoftBlueBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = if (item.isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = if (item.isSuccess) StatusSuccessGreen else StatusErrorRed,
                    modifier = Modifier.size(20.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "“${item.recognizedText}”",
                        style = MaterialTheme.typography.bodyLarge,
                        color = NavyTextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.actionTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = NavyTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelMedium,
                color = BrandRoyalBlue
            )
        }
    }
}
