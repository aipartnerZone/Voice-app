package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "munna_settings")

enum class AppLanguage(val code: String, val displayName: String, val speechLocale: String) {
    HINDI("HI", "हिंदी (Hindi)", "hi-IN"),
    HINGLISH("HINGLISH", "Hinglish (हिंग्लिश)", "hi-IN"),
    ENGLISH("EN", "English", "en-IN");

    companion object {
        fun fromCode(code: String): AppLanguage =
            entries.firstOrNull { it.code == code } ?: HINDI
    }
}

data class UserSettings(
    val language: AppLanguage = AppLanguage.HINDI,
    val spokenResponsesEnabled: Boolean = true,
    val speechRate: Float = 1.0f,
    val voicePitch: Float = 1.0f,
    val floatingMicEnabled: Boolean = false,
    val notificationReaderPaused: Boolean = false,
    val saveCommandHistory: Boolean = true,
    val reducedMotion: Boolean = false
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("app_language")
        val SPOKEN_RESPONSES = booleanPreferencesKey("spoken_responses_enabled")
        val SPEECH_RATE = floatPreferencesKey("speech_rate")
        val VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val FLOATING_MIC = booleanPreferencesKey("floating_mic_enabled")
        val NOTIFICATION_PAUSED = booleanPreferencesKey("notification_reader_paused")
        val SAVE_HISTORY = booleanPreferencesKey("save_command_history")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE] ?: AppLanguage.HINDI.code),
            spokenResponsesEnabled = prefs[Keys.SPOKEN_RESPONSES] ?: true,
            speechRate = prefs[Keys.SPEECH_RATE] ?: 1.0f,
            voicePitch = prefs[Keys.VOICE_PITCH] ?: 1.0f,
            floatingMicEnabled = prefs[Keys.FLOATING_MIC] ?: false,
            notificationReaderPaused = prefs[Keys.NOTIFICATION_PAUSED] ?: false,
            saveCommandHistory = prefs[Keys.SAVE_HISTORY] ?: true,
            reducedMotion = prefs[Keys.REDUCED_MOTION] ?: false
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setSpokenResponses(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SPOKEN_RESPONSES] = enabled }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { it[Keys.SPEECH_RATE] = rate.coerceIn(0.6f, 1.6f) }
    }

    suspend fun setVoicePitch(pitch: Float) {
        context.dataStore.edit { it[Keys.VOICE_PITCH] = pitch.coerceIn(0.7f, 1.4f) }
    }

    suspend fun setFloatingMicEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.FLOATING_MIC] = enabled }
    }

    suspend fun setNotificationReaderPaused(paused: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATION_PAUSED] = paused }
    }

    suspend fun setSaveCommandHistory(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SAVE_HISTORY] = enabled }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.dataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }
}
