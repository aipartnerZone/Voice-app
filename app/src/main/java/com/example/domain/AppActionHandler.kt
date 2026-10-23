package com.example.domain

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.provider.Settings
import com.example.data.AppLanguage
import com.example.service.MunnaNotificationListenerService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ActionExecutionResult(
    val actionTitle: String,
    val spokenResponse: String,
    val category: String,
    val isSuccess: Boolean,
    val shouldRouteToAi: Boolean = false,
    val aiQuery: String? = null,
    val inAppWebFallbackUrl: String? = null,
    val inAppWebTitle: String? = null
)

class AppActionHandler(private val context: Context) {

    fun execute(command: ParsedCommand, language: AppLanguage): ActionExecutionResult {
        return when (command) {
            is ParsedCommand.OpenApp -> launchTargetApp(command.targetApp, language)
            is ParsedCommand.TellTime -> tellCurrentTime(language)
            is ParsedCommand.TellDate -> tellCurrentDate(language)
            is ParsedCommand.CheckBattery -> checkBatteryStatus(language)
            is ParsedCommand.GreetingOrListenCheck -> respondToGreeting(language)
            is ParsedCommand.ReadNotifications -> readNotificationsAloud(command.filterWhatsAppOnly, language)
            is ParsedCommand.OpenSystemSettings -> openDeviceSettings(command.settingType, language)
            is ParsedCommand.AskAiAssistant -> ActionExecutionResult(
                actionTitle = "AI सवाल (AI Question)",
                spokenResponse = when (language) {
                    AppLanguage.HINDI -> "सोच रहा हूँ भाई, एक सेकंड..."
                    AppLanguage.HINGLISH -> "Soch raha hoon bhai, ek second..."
                    AppLanguage.ENGLISH -> "Thinking, give me a moment..."
                },
                category = "AI_CHAT",
                isSuccess = true,
                shouldRouteToAi = true,
                aiQuery = command.query
            )
            is ParsedCommand.UnknownLocalCommand -> ActionExecutionResult(
                actionTitle = "अनजाना कमांड (Unknown Command)",
                spokenResponse = when (language) {
                    AppLanguage.HINDI -> "माफ़ करना भाई, यह कमांड समझ नहीं आया। आप 'YouTube खोलो', 'WhatsApp खोलो', 'Chrome खोलो', या 'Files खोलो' बोल सकते हैं!"
                    AppLanguage.HINGLISH -> "Maaf karna bhai, yeh command samajh nahi aaya. Aap 'YouTube kholo', 'WhatsApp kholo', 'Chrome kholo', ya 'Files kholo' bol sakte hain!"
                    AppLanguage.ENGLISH -> "Sorry friend, I didn't recognize that command. Try saying 'Open YouTube', 'Open WhatsApp', 'Open Chrome', or 'Open Files'."
                },
                category = "UNKNOWN",
                isSuccess = false
            )
        }
    }

    /**
     * Multi-stage native Android app launcher:
     * 1. Exact candidate package launch via PackageManager
     * 2. Installed launcher activity search by app name/keyword
     * 3. System action/category intent (Downloads / Document Picker / Browser)
     * 4. Official Web URL fallback via Browser Intent + In-App WebView sheet so it ALWAYS opens!
     */
    fun launchTargetApp(targetApp: TargetApp, language: AppLanguage): ActionExecutionResult {
        val pm = context.packageManager

        // Stage 1: Try exact package names
        for (pkg in targetApp.candidatePackages) {
            try {
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return buildAppOpenedResult(targetApp, language)
                }
            } catch (_: Exception) {
            }
        }

        // Stage 2: Search all installed LAUNCHER activities by app label or package keyword
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, 0)
            val matchedInfo = resolveInfos.firstOrNull { info ->
                val pkgName = info.activityInfo?.packageName?.lowercase().orEmpty()
                val label = try {
                    info.loadLabel(pm).toString().lowercase()
                } catch (_: Exception) {
                    ""
                }
                targetApp.labelKeywords.any { kw ->
                    pkgName.contains(kw) || label.contains(kw)
                }
            }
            if (matchedInfo != null) {
                val pkg = matchedInfo.activityInfo.packageName
                val launchIntent = pm.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return buildAppOpenedResult(targetApp, language)
                }
            }
        } catch (_: Exception) {
        }

        // Stage 3: Special native system intents for Files Manager
        if (targetApp == TargetApp.FILES) {
            val fileIntents = listOf(
                Intent(DownloadManager.ACTION_VIEW_DOWNLOADS),
                Intent(Intent.ACTION_VIEW).apply { type = "vnd.android.document/root" },
                Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "*/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
            )
            for (candidate in fileIntents) {
                try {
                    candidate.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(candidate)
                    return buildAppOpenedResult(targetApp, language)
                } catch (_: Exception) {
                }
            }
        }

        // Stage 4: If native APK is not installed on this device/emulator, launch its Web URL
        // and always provide inAppWebFallbackUrl so Munna AI opens it visually on screen!
        val fallbackUrl = targetApp.webFallbackUrl
        if (fallbackUrl != null) {
            return ActionExecutionResult(
                actionTitle = "${targetApp.displayName} खोला गया",
                spokenResponse = when (language) {
                    AppLanguage.HINDI -> "ठीक है भाई, ${targetApp.hindiName} खोल रहा हूँ।"
                    AppLanguage.HINGLISH -> "Theek hai bhai, ${targetApp.displayName} khol raha hoon."
                    AppLanguage.ENGLISH -> "Sure friend, opening ${targetApp.displayName}."
                },
                category = "APP_LAUNCH",
                isSuccess = true,
                inAppWebFallbackUrl = fallbackUrl,
                inAppWebTitle = targetApp.displayName
            )
        }

        return ActionExecutionResult(
            actionTitle = "${targetApp.displayName} उपलब्ध नहीं है",
            spokenResponse = when (language) {
                AppLanguage.HINDI -> "भाई, आपके फ़ोन में ${targetApp.hindiName} ऐप इंस्टॉल नहीं मिला।"
                AppLanguage.HINGLISH -> "Bhai, aapke phone mein ${targetApp.displayName} app installed nahi mila."
                AppLanguage.ENGLISH -> "${targetApp.displayName} is not installed on this device."
            },
            category = "APP_LAUNCH",
            isSuccess = false
        )
    }

    private fun buildAppOpenedResult(targetApp: TargetApp, language: AppLanguage): ActionExecutionResult {
        return ActionExecutionResult(
            actionTitle = "${targetApp.displayName} खोला गया",
            spokenResponse = when (language) {
                AppLanguage.HINDI -> "ठीक है भाई, ${targetApp.hindiName} खोल रहा हूँ।"
                AppLanguage.HINGLISH -> "Theek hai bhai, ${targetApp.displayName} khol raha hoon."
                AppLanguage.ENGLISH -> "Sure friend, opening ${targetApp.displayName}."
            },
            category = "APP_LAUNCH",
            isSuccess = true
        )
    }

    private fun tellCurrentTime(language: AppLanguage): ActionExecutionResult {
        val now = Date()
        val timeFormatted = SimpleDateFormat("hh:mm a", Locale.US).format(now)
        return ActionExecutionResult(
            actionTitle = "समय: $timeFormatted",
            spokenResponse = when (language) {
                AppLanguage.HINDI -> "भाई, अभी समय $timeFormatted हुआ है।"
                AppLanguage.HINGLISH -> "Bhai, abhi $timeFormatted baje hain."
                AppLanguage.ENGLISH -> "The current time is $timeFormatted."
            },
            category = "TIME_DATE",
            isSuccess = true
        )
    }

    private fun tellCurrentDate(language: AppLanguage): ActionExecutionResult {
        val now = Date()
        val dateFormatted = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag("hi-IN")).format(now)
        val dateEnglish = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).format(now)
        return ActionExecutionResult(
            actionTitle = "तारीख: $dateEnglish",
            spokenResponse = when (language) {
                AppLanguage.HINDI -> "भाई, आज $dateFormatted है।"
                AppLanguage.HINGLISH -> "Bhai, aaj ki tareekh $dateEnglish hai."
                AppLanguage.ENGLISH -> "Today is $dateEnglish."
            },
            category = "TIME_DATE",
            isSuccess = true
        )
    }

    private fun checkBatteryStatus(language: AppLanguage): ActionExecutionResult {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        return if (level in 0..100) {
            ActionExecutionResult(
                actionTitle = "बैटरी स्तर: $level%",
                spokenResponse = when (language) {
                    AppLanguage.HINDI -> "भाई, आपके फ़ोन की बैटरी अभी $level प्रतिशत चार्ज है।"
                    AppLanguage.HINGLISH -> "Bhai, aapke phone ki battery abhi $level percent hai."
                    AppLanguage.ENGLISH -> "Your phone battery is currently at $level percent."
                },
                category = "SYSTEM",
                isSuccess = true
            )
        } else {
            ActionExecutionResult(
                actionTitle = "बैटरी जानकारी उपलब्ध नहीं",
                spokenResponse = "बैटरी का स्तर अभी पढ़ा नहीं जा सका।",
                category = "SYSTEM",
                isSuccess = false
            )
        }
    }

    private fun respondToGreeting(language: AppLanguage): ActionExecutionResult {
        return ActionExecutionResult(
            actionTitle = "मुन्ना तैयार है",
            spokenResponse = when (language) {
                AppLanguage.HINDI -> "जी भाई, मैं बिल्कुल ध्यान से सुन रहा हूँ! बताओ, क्या काम करना है?"
                AppLanguage.HINGLISH -> "Haan bhai, main sun raha hoon! Batao, kya karna hai?"
                AppLanguage.ENGLISH -> "Yes friend, I am listening! Tell me what you need."
            },
            category = "GREETING",
            isSuccess = true
        )
    }

    private fun readNotificationsAloud(filterWhatsAppOnly: Boolean, language: AppLanguage): ActionExecutionResult {
        if (!MunnaNotificationListenerService.isNotificationAccessGranted(context)) {
            return ActionExecutionResult(
                actionTitle = "नोटिफिकेशन अनुमति आवश्यक",
                spokenResponse = when (language) {
                    AppLanguage.HINDI -> "भाई, नोटिफिकेशन पढ़ने के लिए पहले Settings में जाकर Munna AI को Notification Access की अनुमति दें।"
                    AppLanguage.HINGLISH -> "Bhai, notifications padhne ke liye pehle Settings mein Munna AI ko Notification Access dein."
                    AppLanguage.ENGLISH -> "Please enable Notification Access for Munna AI in Settings first."
                },
                category = "SYSTEM",
                isSuccess = false
            )
        }

        val items = MunnaNotificationListenerService.recentNotifications.value
            .let { list ->
                if (filterWhatsAppOnly) {
                    list.filter { it.packageName.contains("whatsapp", ignoreCase = true) }
                } else {
                    list
                }
            }

        if (items.isEmpty()) {
            return ActionExecutionResult(
                actionTitle = "कोई नया नोटिफिकेशन नहीं",
                spokenResponse = when (language) {
                    AppLanguage.HINDI -> if (filterWhatsAppOnly) "भाई, अभी कोई नया WhatsApp नोटिफिकेशन नहीं है।" else "भाई, अभी कोई नया नोटिफिकेशन नहीं आया है।"
                    AppLanguage.HINGLISH -> "Bhai, abhi koi naya notification nahi hai."
                    AppLanguage.ENGLISH -> "You have no new notifications right now."
                },
                category = "SYSTEM",
                isSuccess = true
            )
        }

        val latest = items.take(3)
        val summary = latest.joinToString(separator = "। ") { item ->
            "${item.appName} से ${item.title}: ${item.text}"
        }
        return ActionExecutionResult(
            actionTitle = "${latest.size} नोटिफिकेशन पढ़े गए",
            spokenResponse = when (language) {
                AppLanguage.HINDI -> "भाई, आपके हाल के नोटिफिकेशन हैं: $summary"
                AppLanguage.HINGLISH -> "Bhai, aapke recent notifications hain: $summary"
                AppLanguage.ENGLISH -> "Here are your latest notifications: $summary"
            },
            category = "SYSTEM",
            isSuccess = true
        )
    }

    private fun openDeviceSettings(settingType: String, language: AppLanguage): ActionExecutionResult {
        return try {
            val action = when (settingType) {
                "WIFI" -> Settings.ACTION_WIFI_SETTINGS
                "BLUETOOTH" -> Settings.ACTION_BLUETOOTH_SETTINGS
                else -> Settings.ACTION_SETTINGS
            }
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionExecutionResult(
                actionTitle = "$settingType सेटिंग्स खोली गई",
                spokenResponse = when (language) {
                    AppLanguage.HINDI -> "ठीक है भाई, $settingType सेटिंग्स खोल रहा हूँ।"
                    AppLanguage.HINGLISH -> "Theek hai bhai, $settingType settings khol raha hoon."
                    AppLanguage.ENGLISH -> "Opening $settingType settings."
                },
                category = "SYSTEM",
                isSuccess = true
            )
        } catch (_: Exception) {
            ActionExecutionResult(
                actionTitle = "सेटिंग्स नहीं खुल सकी",
                spokenResponse = "सेटिंग्स खोलने में समस्या आई।",
                category = "SYSTEM",
                isSuccess = false
            )
        }
    }
}
