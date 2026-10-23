package com.example.network

import android.util.Base64
import com.example.BuildConfig
import com.example.data.AppLanguage
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null,
    val generationConfig: GeminiGenerationConfig? = null
)

@JsonClass(generateAdapter = true)
data class GeminiContent(
    val role: String? = null,
    val parts: List<GeminiPart>
)

@JsonClass(generateAdapter = true)
data class GeminiInlineData(
    val mimeType: String,
    val data: String
)

@JsonClass(generateAdapter = true)
data class GeminiPart(
    val text: String? = null,
    val inlineData: GeminiInlineData? = null
)

@JsonClass(generateAdapter = true)
data class GeminiGenerationConfig(
    val temperature: Float = 0.7f,
    val maxOutputTokens: Int = 512
)

@JsonClass(generateAdapter = true)
data class GeminiResponse(
    val candidates: List<GeminiCandidate>? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(
    val content: GeminiContent? = null
)

interface GeminiRestApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

sealed class AiChatResult {
    data class Success(val replyText: String) : AiChatResult()
    data class Error(val hindiErrorMessage: String) : AiChatResult()
}

class GeminiChatService {

    private val api: GeminiRestApi by lazy {
        val okHttp = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiRestApi::class.java)
    }

    /**
     * Transcribes recorded 16kHz mono WAV audio when Android SpeechRecognizer service is
     * unavailable or fails on the device/emulator.
     */
    suspend fun transcribeWavAudio(
        wavBytes: ByteArray,
        language: AppLanguage
    ): AiChatResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext AiChatResult.Error(
                "आवाज़ रिकॉर्ड हो गई है, लेकिन इस एमुलेटर/फ़ोन पर Google Speech सर्विस नहीं है और GEMINI_API_KEY भी सेट नहीं है। कृपया नीचे सिस्टम माइक पॉपअप या टेक्स्ट बॉक्स का उपयोग करें।"
            )
        }

        val base64Audio = Base64.encodeToString(wavBytes, Base64.NO_WRAP)
        val langHint = when (language) {
            AppLanguage.HINDI -> "Hindi (Devanagari) or Hinglish"
            AppLanguage.HINGLISH -> "Hinglish (Hindi in Roman script) or English"
            AppLanguage.ENGLISH -> "English or Hinglish"
        }

        val prompt = """
            Listen to this user voice recording. The user is speaking a voice command or question in $langHint (for example: "Munna YouTube kholo", "WhatsApp kholo", "Chrome open karo", "Meri files kholo", "Munna abhi kitne baje hain", "Munna meri baat suno", or a general question).
            Return ONLY the exact transcription of what the user said, with no quotes, no extra commentary, and no markdown.
            If there is only silence or noise and no human speech at all, return the exact word: SILENCE
        """.trimIndent()

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    role = "user",
                    parts = listOf(
                        GeminiPart(inlineData = GeminiInlineData(mimeType = "audio/wav", data = base64Audio)),
                        GeminiPart(text = prompt)
                    )
                )
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.1f,
                maxOutputTokens = 120
            )
        )

        try {
            val response = api.generateContent(apiKey = apiKey, request = request)
            val text = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
                .orEmpty()

            if (text.isEmpty() || text.equals("SILENCE", ignoreCase = true)) {
                AiChatResult.Error("कोई साफ़ आवाज़ सुनाई नहीं दी भाई। कृपया माइक के पास थोड़ा ज़ोर से बोलें।")
            } else {
                AiChatResult.Success(text)
            }
        } catch (e: Exception) {
            AiChatResult.Error("आवाज़ पहचानने में नेटवर्क समस्या आई: ${e.localizedMessage ?: "त्रुटि"}")
        }
    }

    suspend fun askMunna(
        userPrompt: String,
        language: AppLanguage,
        conversationHistory: List<Pair<Boolean, String>> = emptyList()
    ): AiChatResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY.trim()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext AiChatResult.Error(
                "AI सेवा के लिए अभी GEMINI_API_KEY सेट नहीं है। कृपया AI Studio के Secrets पैनल में अपनी GEMINI_API_KEY जोड़ें। तब तक आप मुन्ना के सभी लोकल वॉइस कमांड्स (YouTube, WhatsApp, Chrome, Files, समय और बैटरी) का पूरा उपयोग कर सकते हैं!"
            )
        }

        val languageInstruction = when (language) {
            AppLanguage.HINDI -> "हमेशा सरल, दोस्ताना और साफ़ हिंदी (Devanagari) में जवाब दें। उपयोगकर्ता को प्यार से 'भाई' या 'दोस्त' कहकर संबोधित करें।"
            AppLanguage.HINGLISH -> "Always reply in friendly, natural Hinglish (Hindi written in Roman script) and address the user warmly as 'Bhai' or 'Dost'."
            AppLanguage.ENGLISH -> "Always reply in simple, warm, friendly English as Munna AI, a helpful Indian personal voice assistant."
        }

        val systemPrompt = """
            तुम 'मुन्ना AI' (Munna AI) हो — एक बेहद दोस्ताना, समझदार और भरोसेमंद भारतीय पर्सनल वॉइस असिस्टेंट।
            तुम्हारी टैगलाइन है: "बोलो, मुन्ना करेगा!" (Bolo, Munna Karega!)।
            नियम:
            1. $languageInstruction
            2. जवाब छोटे, स्पष्ट और बोलने में आसान रखें (2 से 4 वाक्य), ताकि Text-to-Speech से सुनने में अच्छा लगे।
            3. कभी भी कोई झूठी जानकारी न गढ़ें और यह दावा न करें कि तुमने कोई ऐप खोल दिया है जब तक सिस्टम ने वास्तव में उसे न खोला हो।
        """.trimIndent()

        val contents = mutableListOf<GeminiContent>()
        conversationHistory.takeLast(6).forEach { (isUser, message) ->
            contents.add(
                GeminiContent(
                    role = if (isUser) "user" else "model",
                    parts = listOf(GeminiPart(text = message))
                )
            )
        }
        contents.add(
            GeminiContent(
                role = "user",
                parts = listOf(GeminiPart(text = userPrompt))
            )
        )

        val request = GeminiRequest(
            contents = contents,
            systemInstruction = GeminiContent(
                parts = listOf(GeminiPart(text = systemPrompt))
            ),
            generationConfig = GeminiGenerationConfig(
                temperature = 0.7f,
                maxOutputTokens = 512
            )
        )

        try {
            val response = api.generateContent(apiKey = apiKey, request = request)
            val reply = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()

            if (!reply.isNullOrEmpty()) {
                AiChatResult.Success(reply)
            } else {
                AiChatResult.Error("मुन्ना को AI सर्वर से खाली जवाब मिला। कृपया दोबारा पूछें।")
            }
        } catch (e: java.net.UnknownHostException) {
            AiChatResult.Error("इंटरनेट कनेक्शन बंद है भाई। AI Chat के लिए कृपया अपना इंटरनेट चालू करें।")
        } catch (e: java.net.SocketTimeoutException) {
            AiChatResult.Error("सर्वर से जवाब आने में ज़्यादा समय लग गया। कृपया एक बार फिर कोशिश करें।")
        } catch (e: Exception) {
            AiChatResult.Error("AI सेवा अभी उपलब्ध नहीं है (${e.localizedMessage ?: "नेटवर्क त्रुटि"})। कृपया अपनी GEMINI_API_KEY और इंटरनेट कनेक्शन जाँचें।")
        }
    }
}
