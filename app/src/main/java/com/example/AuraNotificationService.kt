package com.example

import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.db.AppDatabase
import com.example.data.db.NotificationEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AuraNotificationService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: ""
        
        // 1. Check Privacy Mode and App Exclusion list from SharedPreferences
        val prefs = applicationContext.getSharedPreferences("aura_prefs", Context.MODE_PRIVATE)
        val isPrivacyModeActive = prefs.getBoolean("privacy_mode", false)
        if (isPrivacyModeActive) {
            Log.d("AuraNotification", "Notification discarded: Privacy Mode is active.")
            return
        }

        // 1d. Check Do Not Disturb (DND) Sync status (API 23+)
        val dndSyncEnabled = prefs.getBoolean("dnd_sync_enabled", false)
        if (dndSyncEnabled) {
            try {
                val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                val interruptionFilter = notificationManager.currentInterruptionFilter
                
                // INTERRUPTION_FILTER_ALL is 1 (DND off). Any other value means some level of DND is active.
                val isDndActive = interruptionFilter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL &&
                                  interruptionFilter != android.app.NotificationManager.INTERRUPTION_FILTER_UNKNOWN
                if (isDndActive) {
                    Log.d("AuraNotification", "Notification discarded: Do Not Disturb status is active.")
                    return
                }
            } catch (e: Exception) {
                Log.e("AuraNotification", "Failed to check system DND status", e)
            }
        }

        // 1a. Check Quiet Hours Schedule
        val qhEnabled = prefs.getBoolean("quiet_hours_enabled", false)
        if (qhEnabled) {
            val qhStart = prefs.getString("quiet_hours_start", "22:00") ?: "22:00"
            val qhEnd = prefs.getString("quiet_hours_end", "07:00") ?: "07:00"
            try {
                val startParts = qhStart.split(":").map { it.trim().toInt() }
                val endParts = qhEnd.split(":").map { it.trim().toInt() }
                if (startParts.size == 2 && endParts.size == 2) {
                    val startMin = startParts[0] * 60 + startParts[1]
                    val endMin = endParts[0] * 60 + endParts[1]
                    val cal = java.util.Calendar.getInstance()
                    val currentMin = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE)
                    val isInQuietHours = if (startMin <= endMin) {
                        currentMin in startMin..endMin
                    } else {
                        currentMin >= startMin || currentMin <= endMin
                    }
                    if (isInQuietHours) {
                        Log.d("AuraNotification", "Notification discarded: Quiet Hours schedule holds active.")
                        return
                    }
                }
            } catch (e: Exception) {
                Log.e("AuraNotification", "Quiet hours error", e)
            }
        }

        // 1b. Check Battery life guard (< 15%)
        try {
            val filter = android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = applicationContext.registerReceiver(null, filter)
            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    val pct = (level.toFloat() / scale.toFloat() * 100).toInt()
                    if (pct < 15) {
                        Log.d("AuraNotification", "Notification discarded: Battery low ($pct%).")
                        return
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AuraNotification", "Battery scan error", e)
        }

        val excludedStr = prefs.getString("excluded_packages_list", "") ?: ""
        val excludedPackages = excludedStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (excludedPackages.contains(packageName)) {
            Log.d("AuraNotification", "Notification discarded: Package $packageName is matched in excluded list.")
            return
        }

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        // 1c. Simple local heuristic classifier for instant offline triage
        val lowerText = "$title $text".lowercase()
        val computedUrgency = when {
            lowerText.contains("urgent") || lowerText.contains("immediate") || lowerText.contains("asap") ||
            lowerText.contains("critical") || lowerText.contains("emergency") || lowerText.contains("billing") ||
            lowerText.contains("scale") || lowerText.contains("alert") || lowerText.contains("verification") || 
            lowerText.contains("action required") -> "URGENT"
            
            lowerText.contains("promo") || lowerText.contains("newsletter") || lowerText.contains("digest") ||
            lowerText.contains("discount") || lowerText.contains("subscribe") || lowerText.contains("marketing") ||
            lowerText.contains("suggested") || lowerText.contains("vibe") || lowerText.contains("platform") -> "LOW"
            
            else -> "NORMAL"
        }

        // Check if the title/sender matches any Priority Contact designated by the user
        var isPriorityContact = false
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            isPriorityContact = kotlinx.coroutines.runBlocking {
                val contacts = db.dao().getAllContactsList()
                contacts.any { contact ->
                    (contact.isPriority || contact.category.equals("VIP", ignoreCase = true)) && (
                        title.equals(contact.name, ignoreCase = true) ||
                        text.contains(contact.name, ignoreCase = true) ||
                        (contact.phoneNumber.isNotEmpty() && (
                            title.contains(contact.phoneNumber) ||
                            text.contains(contact.phoneNumber)
                        ))
                    )
                }
            }
            if (isPriorityContact) {
                Log.d("AuraNotification", "Notification matches priority contact: $title. Bypassing Deep Work silence mode.")
            }
        } catch (e: Exception) {
            Log.e("AuraNotification", "Failed to query priority contacts database table", e)
        }

        // Check if Deep Work Mode is active and silences non-urgent notifications
        val deepWorkActive = prefs.getBoolean("deep_work_active", false)
        val deepWorkUntil = prefs.getLong("deep_work_until", 0L)
        
        // 1f. Calendar Integration Service reads local calendar events to adjust AI notification priorities based on meeting status
        var finalUrgency = computedUrgency
        var calendarStatusMuted = false
        try {
            val db = AppDatabase.getDatabase(applicationContext)
            val currentTime = System.currentTimeMillis()
            val activeEvents = kotlinx.coroutines.runBlocking {
                db.dao().getAllCalendarEventsList()
            }.filter { event ->
                event.startTime <= currentTime && currentTime <= event.endTime && event.status != "COMPLETED"
            }
            if (activeEvents.isNotEmpty()) {
                val currentMeeting = activeEvents.first()
                Log.d("AuraNotification", "Active calendar meeting detected: '${currentMeeting.title}' with status [${currentMeeting.status}]. Adjusting notification weight.")
                
                // Downgrade normal/low severity notifications to LOW and mute them since the user is in a meeting
                if (finalUrgency != "URGENT" && !isPriorityContact) {
                    calendarStatusMuted = true
                    finalUrgency = "LOW"
                    Log.d("AuraNotification", "Notification downgraded to LOW and silenced due to calendar meeting focus.")
                }
            }
        } catch (e: Exception) {
            Log.e("AuraNotification", "Error reading local calendar events for prioritization update", e)
        }

        var shouldBeSilenced = false
        if (deepWorkActive && System.currentTimeMillis() < deepWorkUntil) {
            if (finalUrgency != "URGENT" && !isPriorityContact) {
                Log.d("AuraNotification", "Notification silenced: Deep Work Focus session is active.")
                shouldBeSilenced = true
            }
        }
        if (calendarStatusMuted) {
            shouldBeSilenced = true
        }

        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val notification = NotificationEntity(
                    packageName = packageName,
                    title = title,
                    text = text,
                    timestamp = System.currentTimeMillis(),
                    urgency = finalUrgency,
                    silencedByDeepWork = shouldBeSilenced
                )
                db.dao().insertNotification(notification)
                Log.d("AuraNotification", "Saved notification from $packageName (Urgency=$finalUrgency, Silenced=$shouldBeSilenced): $title - $text")
            } catch (e: Exception) {
                Log.e("AuraNotification", "Error saving notification to Room database", e)
            }
        }
    }
}
