package de.jupiter1202.gymtracker.feature.workout

import de.jupiter1202.gymtracker.core.database.entities.WorkoutSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

// Fake implementation of WorkoutSessionDao for testing
internal class FakeWorkoutSessionDaoForSession : WorkoutSessionDao {
    private val sessions = mutableListOf<WorkoutSession>()
    private var nextId = 1L

    override suspend fun insert(session: WorkoutSession): Long {
        val sessionWithId = session.copy(id = nextId)
        sessions.add(sessionWithId)
        return nextId++
    }

    override suspend fun getActiveSession(): WorkoutSession? {
        return sessions.firstOrNull { !it.isCompleted }
    }

    override suspend fun getById(id: Long): WorkoutSession? {
        return sessions.firstOrNull { it.id == id }
    }

    override suspend fun update(session: WorkoutSession) {
        val index = sessions.indexOfFirst { it.id == session.id }
        if (index >= 0) {
            sessions[index] = session
        }
    }
}

class WorkoutSessionRepositoryTest {
    private lateinit var fakeDao: FakeWorkoutSessionDaoForSession
    private lateinit var repository: WorkoutSessionRepository

    @Before
    fun setUp() {
        fakeDao = FakeWorkoutSessionDaoForSession()
        repository = WorkoutSessionRepository(fakeDao)
    }

    // LOG-01: createSession_returnsPositiveId
    @Test
    fun createSession_returnsPositiveId() = runTest {
        val result = repository.createSession("Push Day", null)
        assertEquals("Expected positive id", true, result > 0)
    }

    // LOG-01: getActiveSession_returnsNullWhenNoneExists
    @Test
    fun getActiveSession_returnsNullWhenNoneExists() = runTest {
        val result = repository.getActiveSession()
        assertNull("Expected null when no active session", result)
    }

    // LOG-01: getActiveSession_returnsSessionWhenIncomplete
    @Test
    fun getActiveSession_returnsSessionWhenIncomplete() = runTest {
        val session = WorkoutSession(
            id = 0,
            planId = null,
            name = "Test Session",
            startedAt = System.currentTimeMillis(),
            finishedAt = null,
            isCompleted = false
        )
        fakeDao.insert(session)
        val result = repository.getActiveSession()
        assertNotNull("Expected non-null active session", result)
    }
}
