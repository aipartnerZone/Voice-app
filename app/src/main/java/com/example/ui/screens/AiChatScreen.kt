package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.ui.ChatMessage
import com.example.ui.theme.CardBorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.ElevatedNavyCard
import com.example.ui.theme.LighterNavyCard
import com.example.ui.theme.SkyBlueBright
import com.example.ui.theme.SkyBluePrimary
import com.example.ui.theme.StatusErrorRed
import com.example.ui.theme.TextMutedBlueGray
import com.example.ui.theme.TextWhite
import com.example.voice.MicStatus
import com.example.voice.VoiceUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiChatScreen(
    messages: List<ChatMessage>,
    isLoading: Boolean,
    voiceState: VoiceUiState,
    isSpeaking: Boolean,
    onSendMessage: (String) -> Unit,
    onMicClick: () -> Unit,
    onSpeakMessage: (String) -> Unit,
    onStopSpeaking: () -> Unit,
    onNavigateBackHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBackHome()
    }

    var inputText by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavyBackground)
            .testTag("ai_chat_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onNavigateBackHome,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(LighterNavyCard)
                        .testTag("chat_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Home",
                        tint = SkyBluePrimary
                    )
                }
                Column {
                    Text(
                        text = "मुन्ना AI चैट (Munna Chat)",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "हिंदी, Hinglish या English में सवाल पूछें",
                        style = MaterialTheme.typography.labelMedium,
                        color = SkyBlueBright
                    )
                }
            }

            if (isSpeaking) {
                IconButton(
                    onClick = onStopSpeaking,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ElevatedNavyCard)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop speaking",
                        tint = CyanAccent
                    )
                }
            }
        }

        // Security & Prototype API Key Notice
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = LighterNavyCard),
            border = BorderStroke(1.dp, CardBorderSubtle)
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = SkyBluePrimary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "सुरक्षा सूचना: यह प्रोटोटाइप BuildConfig (AI Studio Secrets) के माध्यम से Gemini API का उपयोग करता है। APK को सार्वजनिक रूप से साझा न करें।",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMutedBlueGray
                )
            }
        }

        // Starter question chips
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val suggestions = listOf(
                "मुन्ना, आज कुछ मज़ेदार बताओ",
                "Chai banane ki aasan recipe batao",
                "Phone ki battery jaldi kyun khatam hoti hai?"
            )
            suggestions.forEachIndexed { idx, prompt ->
                AssistChip(
                    onClick = { onSendMessage(prompt) },
                    label = {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextWhite
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(containerColor = LighterNavyCard),
                    border = BorderStroke(1.dp, CardBorderSubtle),
                    modifier = Modifier.testTag("chat_suggestion_chip_$idx")
                )
            }
        }

        // Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("chat_messages_list"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatBubbleItem(
                    message = msg,
                    onSpeakClick = { onSpeakMessage(msg.text) }
                )
            }

            if (isLoading) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .padding(vertical = 8.dp)
                            .testTag("chat_loading_indicator")
                    ) {
                        CircularProgressIndicator(
                            color = SkyBluePrimary,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "मुन्ना जवाब लिख रहा है…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = SkyBlueBright
                        )
                    }
                }
            }
        }

        // Bottom Input Row (Voice Mic + Text Input + Send Button)
        Surface(
            color = LighterNavyCard,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isListening = voiceState.status == MicStatus.LISTENING
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isListening) CyanAccent else ElevatedNavyCard)
                        .testTag("chat_mic_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Speak question",
                        tint = if (isListening) DeepNavyBackground else SkyBluePrimary
                    )
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = "यहाँ लिखें या माइक दबाएँ…",
                            color = TextMutedBlueGray
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBluePrimary,
                        unfocusedBorderColor = CardBorderSubtle,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedContainerColor = DeepNavyBackground,
                        unfocusedContainerColor = DeepNavyBackground
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank()) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("chat_input_field")
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isLoading,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank() && !isLoading) SkyBluePrimary
                            else ElevatedNavyCard
                        )
                        .testTag("chat_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send message",
                        tint = if (inputText.isNotBlank() && !isLoading) DeepNavyBackground else TextMutedBlueGray
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubbleItem(
    message: ChatMessage,
    onSpeakClick: () -> Unit
) {
    val timeStr = SimpleDateFormat("hh:mm a", Locale.US).format(Date(message.timestampMillis))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isUser) 18.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 18.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    message.isUser -> SkyBluePrimary
                    message.isError -> StatusErrorRed.copy(alpha = 0.18f)
                    else -> ElevatedNavyCard
                }
            ),
            border = BorderStroke(
                1.dp,
                when {
                    message.isUser -> SkyBlueBright
                    message.isError -> StatusErrorRed
                    else -> CardBorderSubtle
                }
            )
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
                    Text(
                        text = if (message.isUser) "आप (You)" else "मुन्ना AI",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (message.isUser) DeepNavyBackground.copy(alpha = 0.8f) else SkyBlueBright,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (!message.isUser) {
                        IconButton(
                            onClick = onSpeakClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                contentDescription = "Speak reply",
                                tint = SkyBluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (message.isUser) DeepNavyBackground else TextWhite
                )

                Text(
                    text = timeStr,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (message.isUser) DeepNavyBackground.copy(alpha = 0.7f) else TextMutedBlueGray
                )
            }
        }
    }
}
