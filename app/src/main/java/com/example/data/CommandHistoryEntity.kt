package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "command_history")
data class CommandHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recognizedText: String,
    val actionTitle: String,
    val assistantResponse: String,
    val category: String, // "APP_LAUNCH", "TIME_DATE", "GREETING", "AI_CHAT", "SYSTEM", "UNKNOWN"
    val isSuccess: Boolean,
    val timestampMillis: Long = System.currentTimeMillis()
)
