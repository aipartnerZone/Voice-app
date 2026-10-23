package com.example.domain

enum class TargetApp(
    val id: String,
    val displayName: String,
    val hindiName: String,
    val candidatePackages: List<String>,
    val labelKeywords: List<String>,
    val webFallbackUrl: String? = null
) {
    YOUTUBE(
        id = "youtube",
        displayName = "YouTube",
        hindiName = "YouTube",
        candidatePackages = listOf(
            "com.google.android.youtube",
            "com.google.android.apps.youtube.mango",
            "app.rvx.android.youtube"
        ),
        labelKeywords = listOf("youtube"),
        webFallbackUrl = "https://m.youtube.com"
    ),
    WHATSAPP(
        id = "whatsapp",
        displayName = "WhatsApp",
        hindiName = "WhatsApp",
        candidatePackages = listOf(
            "com.whatsapp",
            "com.whatsapp.w4b"
        ),
        labelKeywords = listOf("whatsapp"),
        webFallbackUrl = "https://web.whatsapp.com"
    ),
    CHROME(
        id = "chrome",
        displayName = "Chrome",
        hindiName = "Chrome",
        candidatePackages = listOf(
            "com.android.chrome",
            "com.chrome.beta",
            "com.sec.android.app.sbrowser",
            "org.mozilla.firefox",
            "com.opera.browser"
        ),
        labelKeywords = listOf("chrome", "browser", "internet"),
        webFallbackUrl = "https://www.google.com"
    ),
    FILES(
        id = "files",
        displayName = "Files",
        hindiName = "Files (फ़ाइलें)",
        candidatePackages = listOf(
            "com.google.android.apps.nbu.files",
            "com.android.documentsui",
            "com.google.android.documentsui",
            "com.sec.android.app.myfiles",
            "com.mi.android.globalFileexplorer",
            "com.coloros.filemanager",
            "com.google.android.apps.docs"
        ),
        labelKeywords = listOf("files", "file manager", "my files", "downloads"),
        webFallbackUrl = null
    )
}

sealed class ParsedCommand {
    data class OpenApp(val targetApp: TargetApp, val rawInput: String) : ParsedCommand()
    data class TellTime(val rawInput: String) : ParsedCommand()
    data class TellDate(val rawInput: String) : ParsedCommand()
    data class CheckBattery(val rawInput: String) : ParsedCommand()
    data class GreetingOrListenCheck(val rawInput: String) : ParsedCommand()
    data class ReadNotifications(val filterWhatsAppOnly: Boolean, val rawInput: String) : ParsedCommand()
    data class OpenSystemSettings(val settingType: String, val rawInput: String) : ParsedCommand()
    data class AskAiAssistant(val query: String, val rawInput: String) : ParsedCommand()
    data class UnknownLocalCommand(val rawInput: String) : ParsedCommand()
}

object CommandParser {

    fun normalizeInput(raw: String): String {
        val cleaned = raw
            .lowercase()
            .replace(Regex("[.,!?।\"'`~\\-_]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        val wakePrefixes = listOf(
            "hey munna", "hello munna", "hi munna", "namaste munna",
            "munna bhai", "munna ji", "oye munna", "sun munna", "suno munna",
            "मुन्ना भाई", "हे मुन्ना", "नमस्ते मुन्ना", "सुनो मुन्ना", "अरे मुन्ना"
        )

        var stripped = cleaned
        for (prefix in wakePrefixes) {
            if (stripped.startsWith("$prefix ")) {
                stripped = stripped.removePrefix(prefix).trim()
            }
        }
        if (stripped.startsWith("munna ") && stripped.length > 6) {
            stripped = stripped.removePrefix("munna ").trim()
        }
        if (stripped.startsWith("मुन्ना ") && stripped.length > 7) {
            stripped = stripped.removePrefix("मुन्ना ").trim()
        }
        return stripped
    }

    fun parse(rawInput: String): ParsedCommand {
        val raw = rawInput.trim()
        if (raw.isEmpty()) return ParsedCommand.UnknownLocalCommand(raw)

        val norm = normalizeInput(raw)
        val fullLower = raw.lowercase().replace(Regex("[.,!?।]"), " ").replace(Regex("\\s+"), " ").trim()

        // 1. Check Notification Reading commands first (before generic WhatsApp match)
        val notifKeywords = listOf(
            "notification padho", "notifications padho", "message padho", "messages padho",
            "whatsapp message padho", "whatsapp notification", "नोटिफिकेशन पढ़ो",
            "नोटिफिकेशन सुनाओ", "मैसेज पढ़ो", "मैसेज सुनाओ", "read notifications", "read my messages"
        )
        if (notifKeywords.any { norm.contains(it) }) {
            val isWa = norm.contains("whatsapp") || norm.contains("व्हाट्सएप") || norm.contains("वाट्सएप")
            return ParsedCommand.ReadNotifications(filterWhatsAppOnly = isWa, rawInput = raw)
        }

        // 2. Check App Launching commands (YouTube, WhatsApp, Chrome, Files)
        // We match whenever the user mentions any supported app in a short command or with any action verb!
        val openActionVerbs = listOf(
            "kholo", "khol do", "kholiye", "khol", "kholna", "open", "chalu karo", "chalu",
            "chalao", "chala do", "chala", "start", "launch", "on karo", "lagao", "dikhao",
            "खोलो", "खोल दो", "खोलिए", "खोल", "खोलना", "चालू करो", "चालू", "चलाओ", "चला दो",
            "ओपन", "ऑन करो", "लगाओ", "दिखाओ"
        )
        val hasOpenVerb = openActionVerbs.any { norm.contains(it) }
        val isShortAppCommand = norm.split(" ").size <= 5 &&
            !norm.contains("kya hai") && !norm.contains("क्या है") &&
            !norm.contains("kyun") && !norm.contains("क्यों")

        val mentionsYouTube = listOf(
            "youtube", "you tube", "yt", "utube", "u tube",
            "यूट्यूब", "यू ट्यूब", "युटुब", "यूटूब", "युट्यूब"
        ).any { norm.contains(it) }

        val mentionsWhatsApp = listOf(
            "whatsapp", "whats app", "watsapp", "whatsup", "whats up",
            "व्हाट्सएप", "वाट्सएप", "व्हाट्सऐप", "वाट्सऐप", "वॉट्सऐप", "वॉट्सएप"
        ).any { norm.contains(it) }

        val mentionsChrome = listOf(
            "chrome", "google chrome", "browser", "crome", "krome",
            "क्रोम", "ब्राउज़र", "ब्राउजर", "गूगल क्रोम"
        ).any { norm.contains(it) }

        val mentionsFiles = listOf(
            "files", "file manager", "meri files", "my files", "meri file", "folder", "storage", "downloads",
            "फाइल्स", "फाइल", "फ़ाइल", "मेरी फाइलें", "मेरी फाइल्स", "फाइल मैनेजर", "फ़ोल्डर", "डाउनलोड"
        ).any { norm.contains(it) }

        if (mentionsYouTube && (hasOpenVerb || isShortAppCommand)) {
            return ParsedCommand.OpenApp(TargetApp.YOUTUBE, raw)
        }
        if (mentionsWhatsApp && (hasOpenVerb || isShortAppCommand)) {
            return ParsedCommand.OpenApp(TargetApp.WHATSAPP, raw)
        }
        if (mentionsChrome && (hasOpenVerb || isShortAppCommand)) {
            return ParsedCommand.OpenApp(TargetApp.CHROME, raw)
        }
        if (mentionsFiles && (hasOpenVerb || isShortAppCommand)) {
            return ParsedCommand.OpenApp(TargetApp.FILES, raw)
        }

        // 3. Check Greeting / "Munna, meri baat suno" / Identity check
        val listenAndGreetingPhrases = listOf(
            "meri baat suno", "baat suno", "मेरी बात सुनो", "बात सुनो",
            "kya haal hai", "kaise ho", "कैसे हो", "क्या हाल है",
            "tum kaun ho", "aap kaun ho", "तुम कौन हो", "आप कौन हो",
            "namaste", "नमस्ते", "hello", "hi munna", "hey munna", "munna"
        )
        if (listenAndGreetingPhrases.any { norm == it || fullLower == it } ||
            norm.contains("meri baat suno") || norm.contains("मेरी बात सुनो")
        ) {
            return ParsedCommand.GreetingOrListenCheck(raw)
        }

        // 4. Check Time queries ("Munna, abhi kitne baje hain?", "time kya hua hai", etc.)
        val timePatterns = listOf(
            "kitne baje", "kitna baja", "time kya", "kya time", "samay kya", "abhi time",
            "what time", "current time", "tell me the time", "time batao", "samay batao",
            "कितने बजे", "समय क्या", "टाइम क्या", "अभी कितने बजे", "क्या समय हुआ", "समय बताओ", "टाइम बताओ"
        )
        if (timePatterns.any { norm.contains(it) } || norm == "time" || norm == "समय" || norm == "टाइम") {
            return ParsedCommand.TellTime(raw)
        }

        // 5. Check Date / Day queries
        val datePatterns = listOf(
            "tareekh", "tarikh", "aaj kya date", "date kya hai", "kaun sa din", "aaj ka din",
            "what is the date", "today's date", "what day is today",
            "तारीख", "तारीख़", "आज कौन सा दिन", "आज की तारीख", "आज क्या डेट है"
        )
        if (datePatterns.any { norm.contains(it) }) {
            return ParsedCommand.TellDate(raw)
        }

        // 6. Check Battery queries
        val batteryPatterns = listOf(
            "battery", "charge kitna", "kitni battery", "बैटरी", "चार्ज कितना"
        )
        if (batteryPatterns.any { norm.contains(it) }) {
            return ParsedCommand.CheckBattery(raw)
        }

        // 7. Check Wi-Fi / Bluetooth / General Settings shortcuts
        if (norm.contains("wifi") || norm.contains("wi fi") || norm.contains("वाईफाई")) {
            return ParsedCommand.OpenSystemSettings("WIFI", raw)
        }
        if (norm.contains("bluetooth") || norm.contains("ब्लूटूथ")) {
            return ParsedCommand.OpenSystemSettings("BLUETOOTH", raw)
        }
        if ((norm.contains("setting") || norm.contains("सेटिंग")) && (hasOpenVerb || isShortAppCommand)) {
            return ParsedCommand.OpenSystemSettings("SETTINGS", raw)
        }

        // 8. Route conversational questions and general knowledge queries to AI Chat
        val questionIndicators = listOf(
            "kya", "kaise", "kyun", "kab", "kaun", "kahan", "kitna", "batao", "samjhao", "sunao",
            "what", "how", "why", "when", "who", "where", "explain", "tell me", "help",
            "क्या", "कैसे", "क्यों", "कब", "कौन", "कहाँ", "बताओ", "समझाओ", "सुनाओ", "मदद"
        )
        if (questionIndicators.any { norm.contains(it) } || norm.split(" ").size >= 3) {
            return ParsedCommand.AskAiAssistant(query = raw, rawInput = raw)
        }

        return ParsedCommand.UnknownLocalCommand(raw)
    }
}
