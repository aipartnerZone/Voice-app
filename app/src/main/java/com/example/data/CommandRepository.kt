package com.example.data

import kotlinx.coroutines.flow.Flow

class CommandRepository(
    private val dao: CommandHistoryDao
) {
    val allHistory: Flow<List<CommandHistoryEntity>> = dao.getAllHistory()
    val recentHistory: Flow<List<CommandHistoryEntity>> = dao.getRecentHistory()

    suspend fun logCommand(
        recognizedText: String,
        actionTitle: String,
        assistantResponse: String,
        category: String,
        isSuccess: Boolean
    ) {
        dao.insertCommand(
            CommandHistoryEntity(
                recognizedText = recognizedText,
                actionTitle = actionTitle,
                assistantResponse = assistantResponse,
                category = category,
                isSuccess = isSuccess,
                timestampMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteCommand(id: Long) {
        dao.deleteCommandById(id)
    }

    suspend fun clearAll() {
        dao.clearAllHistory()
    }
}
