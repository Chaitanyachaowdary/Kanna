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

        serviceScope.launch {
            try {
                val db = AppDatabase.getDatabase(applicationContext)
                val notification = NotificationEntity(
                    packageName = packageName,
                    title = title,
                    text = text,
                    timestamp = System.currentTimeMillis(),
                    urgency = computedUrgency
                )
                db.dao().insertNotification(notification)
                Log.d("AuraNotification", "Saved notification from $packageName (Urgency=$computedUrgency): $title - $text")
            } catch (e: Exception) {
                Log.e("AuraNotification", "Error saving notification to Room database", e)
            }
        }
    }
}
