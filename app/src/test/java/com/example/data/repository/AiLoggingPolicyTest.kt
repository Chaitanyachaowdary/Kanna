package com.example.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AiActionHistoryEntity
import com.example.data.db.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiLoggingPolicyTest {

    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun action(type: String) = AiActionHistoryEntity(
        actionType = type,
        inputPrompt = "in",
        generatedResponse = "out",
        timestamp = 1L,
    )

    @Test
    fun `actionType strings map to the expected categories`() {
        assertEquals(AiActionCategory.CHAT, AiActionCategory.fromActionType("Chat Response"))
        assertEquals(AiActionCategory.NOTIFICATION, AiActionCategory.fromActionType("Notification Analysis"))
        assertEquals(AiActionCategory.NOTIFICATION, AiActionCategory.fromActionType("Prioritized Digest"))
        assertEquals(AiActionCategory.MAIL, AiActionCategory.fromActionType("Email Analysis"))
        assertEquals(AiActionCategory.VOICE, AiActionCategory.fromActionType("Voice Command"))
        assertEquals(AiActionCategory.SOCIAL, AiActionCategory.fromActionType("Social Trend Analysis (Offline)"))
        assertEquals(AiActionCategory.SOCIAL, AiActionCategory.fromActionType("LinkedIn Post Draft"))
        assertEquals(AiActionCategory.MEETING, AiActionCategory.fromActionType("Call Screening Summary (Offline)"))
        assertEquals(AiActionCategory.MEETING, AiActionCategory.fromActionType("Meeting Query"))
        assertEquals(AiActionCategory.OTHER, AiActionCategory.fromActionType("Something Unknown"))
    }

    @Test
    fun `default AlwaysOn policy logs everything`() = runTest {
        val repository = AuraRepository(db.dao()) // default policy

        repository.insertAiAction(action("Email Analysis"))
        repository.insertAiAction(action("Chat Response"))

        assertEquals(2, repository.aiActionHistory.first().size)
    }

    @Test
    fun `disabling a category skips only that category`() = runTest {
        val mailDisabled = object : AiLoggingPolicy {
            override fun shouldLog(actionType: String): Boolean =
                AiActionCategory.fromActionType(actionType) != AiActionCategory.MAIL
        }
        val repository = AuraRepository(db.dao(), mailDisabled)

        repository.insertAiAction(action("Email Analysis")) // MAIL -> skipped
        repository.insertAiAction(action("Chat Response"))   // CHAT -> kept

        val logged = repository.aiActionHistory.first().map { it.actionType }
        assertEquals(listOf("Chat Response"), logged)
    }

    @Test
    fun `master-off policy logs nothing`() = runTest {
        val allOff = object : AiLoggingPolicy {
            override fun shouldLog(actionType: String): Boolean = false
        }
        val repository = AuraRepository(db.dao(), allOff)

        repository.insertAiAction(action("Chat Response"))
        repository.insertAiAction(action("Voice Command"))

        assertTrue(repository.aiActionHistory.first().isEmpty())
    }

    @Test
    fun `SharedPrefs policy honours master switch and per-category keys`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("test_logging_prefs", Context.MODE_PRIVATE)
        val policy = SharedPrefsAiLoggingPolicy(prefs)

        // Defaults: everything enabled when no keys are present.
        assertTrue(policy.shouldLog("Chat Response"))

        // Disable just the Mail category.
        prefs.edit().putBoolean(SharedPrefsAiLoggingPolicy.categoryKey(AiActionCategory.MAIL), false).apply()
        assertFalse(policy.shouldLog("Email Analysis"))
        assertTrue(policy.shouldLog("Chat Response"))

        // Master off disables everything regardless of category keys.
        prefs.edit().putBoolean(SharedPrefsAiLoggingPolicy.KEY_MASTER, false).apply()
        assertFalse(policy.shouldLog("Chat Response"))
    }
}
