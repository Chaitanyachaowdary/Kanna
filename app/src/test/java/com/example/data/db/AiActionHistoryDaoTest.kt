package com.example.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
 * Unit tests for the AI Action History persistence layer ([AuraDao] +
 * [AiActionHistoryEntity]). Runs on the host JVM via Robolectric against an
 * in-memory Room database, so no device/emulator is required.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AiActionHistoryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: AuraDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.dao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun action(
        type: String = "Chat Response",
        input: String = "prompt",
        response: String = "response",
        timestamp: Long = 1_000L,
    ) = AiActionHistoryEntity(
        actionType = type,
        inputPrompt = input,
        generatedResponse = response,
        timestamp = timestamp,
    )

    @Test
    fun `insert then read returns the action`() = runTest {
        dao.insertAiAction(action(type = "Notification Analysis"))

        val all = dao.getAllAiActionHistory().first()

        assertEquals(1, all.size)
        assertEquals("Notification Analysis", all[0].actionType)
        assertEquals("prompt", all[0].inputPrompt)
        assertEquals("response", all[0].generatedResponse)
    }

    @Test
    fun `history is ordered by timestamp descending`() = runTest {
        dao.insertAiAction(action(type = "oldest", timestamp = 100L))
        dao.insertAiAction(action(type = "newest", timestamp = 300L))
        dao.insertAiAction(action(type = "middle", timestamp = 200L))

        val all = dao.getAllAiActionHistory().first()

        assertEquals(listOf("newest", "middle", "oldest"), all.map { it.actionType })
    }

    @Test
    fun `deleteAiAction removes only the targeted row`() = runTest {
        dao.insertAiAction(action(type = "keep", timestamp = 100L))
        dao.insertAiAction(action(type = "remove", timestamp = 200L))

        val toRemove = dao.getAllAiActionHistory().first().first { it.actionType == "remove" }
        dao.deleteAiAction(toRemove.id)

        val remaining = dao.getAllAiActionHistory().first()
        assertEquals(1, remaining.size)
        assertEquals("keep", remaining[0].actionType)
    }

    @Test
    fun `clearAiActionHistory empties the table`() = runTest {
        dao.insertAiAction(action(timestamp = 100L))
        dao.insertAiAction(action(timestamp = 200L))

        dao.clearAiActionHistory()

        assertTrue(dao.getAllAiActionHistory().first().isEmpty())
    }

    @Test
    fun `deleteOldAiActions purges rows strictly older than the cutoff`() = runTest {
        dao.insertAiAction(action(type = "stale", timestamp = 50L))
        dao.insertAiAction(action(type = "boundary", timestamp = 100L))
        dao.insertAiAction(action(type = "fresh", timestamp = 150L))

        // Matches the DAO query: WHERE timestamp < :cutoff (boundary value is retained).
        dao.deleteOldAiActions(cutoff = 100L)

        val remaining = dao.getAllAiActionHistory().first().map { it.actionType }
        assertEquals(listOf("fresh", "boundary"), remaining)
    }
}
