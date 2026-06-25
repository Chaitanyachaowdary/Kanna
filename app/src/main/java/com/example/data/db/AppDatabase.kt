package com.example.data.db

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import com.example.data.security.DatabaseKeyProvider
import kotlinx.coroutines.flow.Flow
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

// --- Room Entities for Aura AI ---

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "USER" or "AI"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 1,
    val userName: String = "Sovereign User",
    val userEmail: String = "user@example.com",
    val statusText: String = "Aura Active and Alert",
    val autoReplyEnabled: Boolean = false,
    val securityLevel: String = "Maximum Local Privacy",
    val wakeWord: String = "Aura",
    val lockscreenActivationEnabled: Boolean = true,
    val activeMode: String = "Unlocked" // "Locked" or "Unlocked"
)

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val packageName: String, // e.g. "com.whatsapp", "com.google.android.gm"
    val title: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val summary: String = "",
    val replyDraft: String = "",
    val status: String = "PENDING", // PENDING, SUMMARIZED, DEALT
    val urgency: String = "NORMAL", // URGENT, NORMAL, LOW
    val silencedByDeepWork: Boolean = false
)

@Entity(tableName = "emails")
data class EmailEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String,
    val subject: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val summary: String = "",
    val replyDraft: String = "",
    val status: String = "UNREAD" // UNREAD, SUMMARIZED, REPLIED
)

@Entity(tableName = "secure_files")
data class SecureFileEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val sizeStr: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isEncrypted: Boolean = true,
    val content: String = ""
)

@Entity(tableName = "social_posts")
data class SocialPostEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val platform: String, // "LINKEDIN", "INSTAGRAM", "TWITTER/X"
    val title: String, // Destination or target context
    val text: String, // Original notification content or trigger
    val replyDraft: String, // Draft formulated by Aura
    val status: String = "DRAFT", // "DRAFT", "PUBLISHED", "PROCESSING", "SCHEDULED"
    val timestamp: Long = System.currentTimeMillis(),
    val scheduledTime: Long = 0L
)

@Entity(tableName = "aura_tasks")
data class AuraTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val assignee: String = "Chaitanya",
    val status: String = "PENDING", // "PENDING", "COMPLETED"
    val sourceMeeting: String = "Manual Entry",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "call_sessions")
data class CallSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val callerName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val transcript: String, // Full chat log as text
    val summary: String, // AI Generated summary
    val voiceProfileUsed: String = "Kanna Classic"
)

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val organizer: String,
    val startTime: Long, // Epoch ms
    val endTime: Long, // Epoch ms
    val status: String = "PENDING", // PENDING, JOINING, RECORDING, COMPLETED
    val transcript: String = "",
    val summary: String = ""
)

@Entity(tableName = "privacy_insights")
data class PrivacyInsightEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appOrServiceName: String,
    val dataProcessedSummary: String,
    val sessionTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "call_screening_rules")
data class CallScreeningRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val pattern: String, // Phone number match pattern (e.g. "+1-555*", "*") or Caller ID
    val action: String,  // "AUTO_ANSWER" or "BLOCK"
    val description: String = ""
)

@Entity(tableName = "email_templates")
data class EmailTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val content: String,
    val category: String, // "work", "personal", "urgent"
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "screened_transcripts")
data class ScreenedTranscriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "CALL" or "NOTIFICATION"
    val source: String, // e.g. Contact Name / App Name
    val transcriptText: String, // Encrypted if isEncrypted is true
    val summary: String, // Encrypted if isEncrypted is true
    val isEncrypted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "aura_contacts")
data class AuraContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phoneNumber: String,
    val category: String, // "VIP", "Work", "Family"
    val aiResponseTone: String = "Formal", // "Formal", "Casual", "Enthusiastic"
    val isPriority: Boolean = false
)

@Entity(tableName = "version_installations")
data class VersionInstallationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val version: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_action_history")
data class AiActionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val actionType: String, // e.g. "Chat Response", "Notification Analysis", "Email Reply Draft", etc.
    val inputPrompt: String,
    val generatedResponse: String,
    val timestamp: Long = System.currentTimeMillis()
)

// --- DAO Interface ---

@Dao
interface AuraDao {
    // Calendar Events
    @Query("SELECT * FROM calendar_events ORDER BY startTime ASC")
    fun getAllCalendarEvents(): Flow<List<CalendarEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalendarEvent(event: CalendarEventEntity)

    @Update
    suspend fun updateCalendarEvent(event: CalendarEventEntity)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun deleteCalendarEvent(id: Int)

    @Query("SELECT * FROM calendar_events ORDER BY startTime ASC")
    suspend fun getAllCalendarEventsList(): List<CalendarEventEntity>

    @Query("DELETE FROM calendar_events")
    suspend fun clearCalendarEvents()

    // Call Sessions
    @Query("SELECT * FROM call_sessions ORDER BY timestamp DESC")
    fun getAllCallSessions(): Flow<List<CallSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallSession(session: CallSessionEntity)

    @Query("DELETE FROM call_sessions WHERE id = :id")
    suspend fun deleteCallSession(id: Int)

    @Query("DELETE FROM call_sessions")
    suspend fun clearCallSessions()
    // Chat messages
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getChatMessages(): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChatMessages()

    // User profile
    @Query("SELECT * FROM user_profiles WHERE id = 1 LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profiles WHERE id = 1 LIMIT 1")
    suspend fun getUserProfile(): UserProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserProfile(profile: UserProfileEntity)

    // Notifications
    @Query("SELECT * FROM notifications ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Update
    suspend fun updateNotification(notification: NotificationEntity)

    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: Int)

    @Query("DELETE FROM notifications")
    suspend fun clearNotifications()

    // Emails
    @Query("SELECT * FROM emails ORDER BY timestamp DESC")
    fun getAllEmails(): Flow<List<EmailEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmail(email: EmailEntity)

    @Update
    suspend fun updateEmail(email: EmailEntity)

    @Query("DELETE FROM emails WHERE id = :id")
    suspend fun deleteEmail(id: Int)

    @Query("DELETE FROM emails")
    suspend fun clearEmails()

    // Secure Files
    @Query("SELECT * FROM secure_files ORDER BY timestamp DESC")
    fun getAllSecureFiles(): Flow<List<SecureFileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSecureFile(file: SecureFileEntity)

    @Query("DELETE FROM secure_files WHERE id = :id")
    suspend fun deleteSecureFile(id: Int)

    @Query("DELETE FROM secure_files")
    suspend fun clearSecureFiles()

    // Social Posts
    @Query("SELECT * FROM social_posts ORDER BY timestamp DESC")
    fun getAllSocialPosts(): Flow<List<SocialPostEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSocialPost(post: SocialPostEntity)

    @Update
    suspend fun updateSocialPost(post: SocialPostEntity)

    @Query("DELETE FROM social_posts WHERE id = :id")
    suspend fun deleteSocialPost(id: Int)

    @Query("DELETE FROM social_posts")
    suspend fun clearSocialPosts()

    // Aura Tasks
    @Query("SELECT * FROM aura_tasks ORDER BY timestamp DESC")
    fun getAllTasks(): Flow<List<AuraTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: AuraTaskEntity)

    @Update
    suspend fun updateTask(task: AuraTaskEntity)

    @Query("DELETE FROM aura_tasks WHERE id = :id")
    suspend fun deleteTask(id: Int)

    @Query("DELETE FROM aura_tasks")
    suspend fun clearTasks()

    // Privacy Insights
    @Query("SELECT * FROM privacy_insights ORDER BY sessionTimestamp DESC")
    fun getAllPrivacyInsights(): Flow<List<PrivacyInsightEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrivacyInsight(insight: PrivacyInsightEntity)

    @Query("DELETE FROM privacy_insights WHERE id = :id")
    suspend fun deletePrivacyInsight(id: Int)

    @Query("DELETE FROM privacy_insights")
    suspend fun clearPrivacyInsights()

    // Call Screening Rules
    @Query("SELECT * FROM call_screening_rules ORDER BY pattern ASC")
    fun getAllCallScreeningRules(): Flow<List<CallScreeningRuleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallScreeningRule(rule: CallScreeningRuleEntity)

    @Query("DELETE FROM call_screening_rules WHERE id = :id")
    suspend fun deleteCallScreeningRule(id: Int)

    // Email Templates
    @Query("SELECT * FROM email_templates ORDER BY category ASC, name ASC")
    fun getAllEmailTemplates(): Flow<List<EmailTemplateEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmailTemplate(template: EmailTemplateEntity)

    @Query("DELETE FROM email_templates WHERE id = :id")
    suspend fun deleteEmailTemplate(id: Int)

    // Screened Transcripts
    @Query("SELECT * FROM screened_transcripts ORDER BY timestamp DESC")
    fun getAllScreenedTranscripts(): Flow<List<ScreenedTranscriptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScreenedTranscript(transcript: ScreenedTranscriptEntity)

    @Query("DELETE FROM screened_transcripts WHERE id = :id")
    suspend fun deleteScreenedTranscript(id: Int)

    @Query("DELETE FROM screened_transcripts")
    suspend fun clearScreenedTranscripts()

    @Query("DELETE FROM screened_transcripts WHERE timestamp < :cutoff")
    suspend fun deleteOldScreenedTranscripts(cutoff: Long)

    // Aura Contacts
    @Query("SELECT * FROM aura_contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<AuraContactEntity>>

    @Query("SELECT * FROM aura_contacts")
    suspend fun getAllContactsList(): List<AuraContactEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: AuraContactEntity)

    @Query("DELETE FROM aura_contacts WHERE id = :id")
    suspend fun deleteContact(id: Int)

    // --- Auto Cleanup Old Logs and Transcripts ---
    @Query("DELETE FROM chat_messages WHERE timestamp < :cutoff")
    suspend fun deleteOldChatMessages(cutoff: Long)

    @Query("DELETE FROM notifications WHERE timestamp < :cutoff")
    suspend fun deleteOldNotifications(cutoff: Long)

    @Query("DELETE FROM emails WHERE timestamp < :cutoff")
    suspend fun deleteOldEmails(cutoff: Long)

    @Query("DELETE FROM secure_files WHERE timestamp < :cutoff")
    suspend fun deleteOldSecureFiles(cutoff: Long)

    @Query("DELETE FROM social_posts WHERE timestamp < :cutoff")
    suspend fun deleteOldSocialPosts(cutoff: Long)

    @Query("DELETE FROM aura_tasks WHERE timestamp < :cutoff")
    suspend fun deleteOldTasks(cutoff: Long)

    @Query("DELETE FROM call_sessions WHERE timestamp < :cutoff")
    suspend fun deleteOldCallSessions(cutoff: Long)

    @Query("DELETE FROM privacy_insights WHERE sessionTimestamp < :cutoff")
    suspend fun deleteOldPrivacyInsights(cutoff: Long)

    // Version Installations
    @Query("SELECT * FROM version_installations ORDER BY timestamp DESC")
    fun getAllVersionInstallations(): Flow<List<VersionInstallationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersionInstallation(installation: VersionInstallationEntity)

    // AI Action History
    @Query("SELECT * FROM ai_action_history ORDER BY timestamp DESC")
    fun getAllAiActionHistory(): Flow<List<AiActionHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAiAction(action: AiActionHistoryEntity)

    @Query("DELETE FROM ai_action_history WHERE id = :id")
    suspend fun deleteAiAction(id: Int)

    @Query("DELETE FROM ai_action_history")
    suspend fun clearAiActionHistory()

    @Query("DELETE FROM ai_action_history WHERE timestamp < :cutoff")
    suspend fun deleteOldAiActions(cutoff: Long)
}

// --- AppDatabase ---

@Database(
    entities = [
        ChatMessageEntity::class,
        UserProfileEntity::class,
        NotificationEntity::class,
        EmailEntity::class,
        SecureFileEntity::class,
        SocialPostEntity::class,
        CallSessionEntity::class,
        CalendarEventEntity::class,
        AuraTaskEntity::class,
        PrivacyInsightEntity::class,
        CallScreeningRuleEntity::class,
        EmailTemplateEntity::class,
        ScreenedTranscriptEntity::class,
        AuraContactEntity::class,
        VersionInstallationEntity::class,
        AiActionHistoryEntity::class
    ],
    version = 14, // Bumped to support AI action history
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AuraDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private const val DB_NAME = "aura_database"

        /**
         * Ordered Room migrations. v14 is the exported baseline schema (see
         * app/schemas). When you change any entity, bump the [Database] version
         * and add a `Migration(oldVersion, newVersion)` here that ALTERs the
         * tables — do NOT reintroduce `fallbackToDestructiveMigration`, which
         * silently wipes all on-device user data on every schema change.
         */
        val ALL_MIGRATIONS: Array<Migration> = arrayOf()

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildEncryptedDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }

        private fun buildEncryptedDatabase(appContext: Context): AppDatabase {
            // Native SQLCipher library; safe to call repeatedly.
            System.loadLibrary("sqlcipher")

            val passphrase = DatabaseKeyProvider.getOrCreatePassphrase(appContext)

            // The very first encrypted launch cannot open any database file left
            // behind by a previous unencrypted build — discard it so SQLCipher can
            // create a fresh, encrypted store rather than failing to open plaintext.
            if (DatabaseKeyProvider.passphraseWasJustCreated) {
                deleteLegacyPlaintextDatabase(appContext)
            }

            return Room.databaseBuilder(appContext, AppDatabase::class.java, DB_NAME)
                .openHelperFactory(SupportOpenHelperFactory(passphrase))
                .addMigrations(*ALL_MIGRATIONS)
                .build()
        }

        private fun deleteLegacyPlaintextDatabase(appContext: Context) {
            listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
                val file = appContext.getDatabasePath(DB_NAME + suffix)
                if (file.exists()) file.delete()
            }
        }
    }
}
