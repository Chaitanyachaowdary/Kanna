package com.example.data.db

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

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
    val urgency: String = "NORMAL" // URGENT, NORMAL, LOW
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
        AuraTaskEntity::class
    ],
    version = 8, // Bumped to support tasks and social post schedule field
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AuraDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aura_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
