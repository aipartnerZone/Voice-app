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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CommandHistoryEntity
import com.example.ui.theme.CardBorderSubtle
import com.example.ui.theme.DeepNavyBackground
import com.example.ui.theme.ElevatedNavyCard
import com.example.ui.theme.LighterNavyCard
import com.example.ui.theme.SkyBlueBright
import com.example.ui.theme.SkyBluePrimary
import com.example.ui.theme.StatusErrorRed
import com.example.ui.theme.StatusSuccessGreen
import com.example.ui.theme.TextMutedBlueGray
import com.example.ui.theme.TextWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    historyList: List<CommandHistoryEntity>,
    saveHistoryEnabled: Boolean,
    onToggleSaveHistory: (Boolean) -> Unit,
    onRepeatCommand: (String) -> Unit,
    onDeleteCommand: (Long) -> Unit,
    onClearAll: () -> Unit,
    onNavigateBackHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler {
        onNavigateBackHome()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNavyBackground)
            .testTag("history_screen_list"),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Header + Clear History Button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "कमांड हिस्ट्री (Command History)",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "आपके द्वारा बोले गए कमांड्स और मुन्ना की कार्रवाई",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMutedBlueGray
                    )
                }

                if (historyList.isNotEmpty()) {
                    OutlinedButton(
                        onClick = onClearAll,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, StatusErrorRed.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = StatusErrorRed),
                        modifier = Modifier.testTag("clear_all_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear all history",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "साफ़ करें")
                    }
                }
            }
        }

        // Privacy Control Banner (Section 11: Do not save raw audio recordings by default)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = LighterNavyCard),
                border = BorderStroke(1.dp, CardBorderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = SkyBluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "कमांड हिस्ट्री सेव करें (Save History)",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextWhite
                            )
                        }
                        Switch(
                            checked = saveHistoryEnabled,
                            onCheckedChange = onToggleSaveHistory,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeepNavyBackground,
                                checkedTrackColor = SkyBluePrimary
                            ),
                            modifier = Modifier.testTag("toggle_save_history_switch")
                        )
                    }
                    Text(
                        text = "गोपनीयता सुरक्षा: मुन्ना आपकी कच्ची ऑडियो रिकॉर्डिंग (Raw Audio) कभी सेव नहीं करता। केवल पहचाने गए टेक्स्ट और स्टेटस को आपके फ़ोन में लोकल तौर पर रखा जाता है।",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMutedBlueGray
                    )
                }
            }
        }

        if (historyList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .padding(top = 24.dp)
                        .testTag("empty_history_state_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = LighterNavyCard),
                    border = BorderStroke(1.dp, CardBorderSubtle)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = SkyBluePrimary,
                            modifier = Modifier.size(44.dp)
                        )
                        Text(
                            text = "कमांड हिस्ट्री खाली है",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextWhite
                        )
                        Text(
                            text = "जब आप होम स्क्रीन पर माइक दबाकर कोई कमांड बोलेंगे, तो उसका समय और परिणाम यहाँ दिखेगा।",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMutedBlueGray
                        )
                    }
                }
            }
        } else {
            items(historyList, key = { it.id }) { entry ->
                HistoryDetailCard(
                    entry = entry,
                    onRepeat = { onRepeatCommand(entry.recognizedText) },
                    onDelete = { onDeleteCommand(entry.id) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun HistoryDetailCard(
    entry: CommandHistoryEntity,
    onRepeat: () -> Unit,
    onDelete: () -> Unit
) {
    val dateTimeStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US)
        .format(Date(entry.timestampMillis))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 600.dp)
            .testTag("history_item_${entry.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = LighterNavyCard),
        border = BorderStroke(1.dp, CardBorderSubtle)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (entry.isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                        contentDescription = if (entry.isSuccess) "Success" else "Failed",
                        tint = if (entry.isSuccess) StatusSuccessGreen else StatusErrorRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Surface(
                        color = ElevatedNavyCard,
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = if (entry.isSuccess) "सफल (Success)" else "असफल (Failed)",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (entry.isSuccess) StatusSuccessGreen else StatusErrorRed,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = dateTimeStr,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextMutedBlueGray
                )
            }

            Text(
                text = "“${entry.recognizedText}”",
                style = MaterialTheme.typography.titleMedium,
                color = TextWhite,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "कार्रवाई: ${entry.actionTitle}",
                style = MaterialTheme.typography.bodyMedium,
                color = SkyBlueBright
            )

            Text(
                text = "मुन्ना का जवाब: ${entry.assistantResponse}",
                style = MaterialTheme.typography.bodyMedium,
                color = TextMutedBlueGray,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onRepeat,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = "Repeat command",
                        tint = SkyBluePrimary
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete item",
                        tint = StatusErrorRed
                    )
                }
            }
        }
    }
}
