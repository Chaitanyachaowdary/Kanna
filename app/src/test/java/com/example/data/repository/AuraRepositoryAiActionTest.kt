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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for the AI action history surface of [AuraRepository], exercised
 * against an in-memory Room database via Robolectric (host JVM, no device).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AuraRepositoryAiActionTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: AuraRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AuraRepository(db.dao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun action(type: String, timestamp: Long) = AiActionHistoryEntity(
        actionType = type,
        inputPrompt = "in",
        generatedResponse = "out",
        timestamp = timestamp,
    )

    @Test
    fun `insertAiAction is observable through the history flow`() = runTest {
        repository.insertAiAction(action("Chat Response", 100L))

        val history = repository.aiActionHistory.first()

        assertEquals(1, history.size)
        assertEquals("Chat Response", history[0].actionType)
    }

    @Test
    fun `deleteAiAction removes the targeted entry`() = runTest {
        repository.insertAiAction(action("keep", 100L))
        repository.insertAiAction(action("remove", 200L))

        val target = repository.aiActionHistory.first().first { it.actionType == "remove" }
        repository.deleteAiAction(target.id)

        assertEquals(listOf("keep"), repository.aiActionHistory.first().map { it.actionType })
    }

    @Test
    fun `clearAiActionHistory empties the history`() = runTest {
        repository.insertAiAction(action("a", 100L))
        repository.insertAiAction(action("b", 200L))

        repository.clearAiActionHistory()

        assertTrue(repository.aiActionHistory.first().isEmpty())
    }

    @Test
    fun `autoCleanupOldData purges AI actions older than the retention window`() = runTest {
        val now = System.currentTimeMillis()
        val tenDaysMs = 10L * 24 * 60 * 60 * 1000
        repository.insertAiAction(action("stale", now - tenDaysMs))
        repository.insertAiAction(action("fresh", now))

        repository.autoCleanupOldData(days = 7)

        assertEquals(listOf("fresh"), repository.aiActionHistory.first().map { it.actionType })
    }
}
