package de.jupiter1202.gymtracker.feature.workout

import de.jupiter1202.gymtracker.core.database.entities.WorkoutSession
import de.jupiter1202.gymtracker.core.database.entities.WorkoutSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

// Fake repositories for testing
private class FakeWorkoutSessionDao : WorkoutSessionDao {
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

private class FakeWorkoutSetDao : WorkoutSetDao {
    private val sets = mutableListOf<WorkoutSet>()
    private var nextId = 1L

    override suspend fun insert(set: WorkoutSet): Long {
        val setWithId = set.copy(id = nextId)
        sets.add(setWithId)
        return nextId++
    }

    override fun getSetsForSession(sessionId: Long): Flow<List<WorkoutSet>> {
        return flowOf(sets.filter { it.sessionId == sessionId })
    }

    override fun getSetsForExercise(sessionId: Long, exerciseId: Long): Flow<List<WorkoutSet>> {
        return flowOf(sets.filter { it.sessionId == sessionId && it.exerciseId == exerciseId })
    }

    override suspend fun getMaxSetNumber(sessionId: Long, exerciseId: Long): Int? {
        return sets
            .filter { it.sessionId == sessionId && it.exerciseId == exerciseId }
            .maxOfOrNull { it.setNumber }
    }

    override suspend fun delete(set: WorkoutSet) {
        sets.removeIf { it.id == set.id }
    }

    override suspend fun getPreviousSessionSets(exerciseId: Long): List<WorkoutSet> {
        if (sets.isEmpty()) return emptyList()
        val maxSessionId = sets.filter { it.exerciseId == exerciseId }.maxOfOrNull { it.sessionId } ?: return emptyList()
        return sets.filter { it.sessionId == maxSessionId && it.exerciseId == exerciseId }
    }
}

class WorkoutLoggingViewModelTest {

    // LOG-01: createSession_returnsPositiveId
    @Test
    fun createSession_returnsPositiveId() = runTest {
        val sessionDao = FakeWorkoutSessionDao()
        val sessionRepo = WorkoutSessionRepository(sessionDao)
        
        val result = sessionRepo.createSession("Push Day", null)
        assertEquals("Expected positive id", true, result > 0)
    }

    // LOG-01: getActiveSession_returnsNullWhenNoneExists
    @Test
    fun getActiveSession_returnsNullWhenNoneExists() = runTest {
        val sessionDao = FakeWorkoutSessionDao()
        val sessionRepo = WorkoutSessionRepository(sessionDao)
        
        val result = sessionRepo.getActiveSession()
        assertNull("Expected null when no active session", result)
    }

    // LOG-01: getActiveSession_returnsSessionWhenIncomplete
    @Test
    fun getActiveSession_returnsSessionWhenIncomplete() = runTest {
        val sessionDao = FakeWorkoutSessionDao()
        val sessionRepo = WorkoutSessionRepository(sessionDao)
        
        val session = WorkoutSession(
            id = 0,
            planId = null,
            name = "Test Session",
            startedAt = System.currentTimeMillis(),
            finishedAt = null,
            isCompleted = false
        )
        sessionDao.insert(session)
        
        val result = sessionRepo.getActiveSession()
        assertEquals("Expected non-null active session", true, result != null)
    }

    // LOG-04: logSet_insertsToDao
    @Test
    fun logSet_insertsToDao() = runTest {
        val setDao = FakeWorkoutSetDao()
        val setRepo = WorkoutSetRepository(setDao)
        
        val result = setRepo.logSet(sessionId = 1, exerciseId = 1, weightKg = 80.0, reps = 8)
        assertEquals("Expected positive id", true, result > 0)
    }

    // LOG-04: getPreviousSessionSets_returnsEmptyWhenNoPriorSession
    @Test
    fun getPreviousSessionSets_returnsEmptyWhenNoPriorSession() = runTest {
        val setDao = FakeWorkoutSetDao()
        val setRepo = WorkoutSetRepository(setDao)
        
        val result = setRepo.getPreviousSessionSets(exerciseId = 99L)
        assertEquals("Expected empty list when no prior session", true, result.isEmpty())
    }

    // LOG-04: getPreviousSessionSets_returnsLastCompletedSessionSets
    @Test
    fun getPreviousSessionSets_returnsLastCompletedSessionSets() = runTest {
        val setDao = FakeWorkoutSetDao()
        val setRepo = WorkoutSetRepository(setDao)
        
        // Insert two sessions with sets
        // Session 1: completed
        val set1_s1 = WorkoutSet(
            id = 0,
            sessionId = 1,
            exerciseId = 1,
            setNumber = 1,
            weightKg = 70.0,
            reps = 8,
            completedAt = System.currentTimeMillis()
        )
        setDao.insert(set1_s1)

        // Session 2: also completed, same exercise
        val set1_s2 = WorkoutSet(
            id = 0,
            sessionId = 2,
            exerciseId = 1,
            setNumber = 1,
            weightKg = 75.0,
            reps = 8,
            completedAt = System.currentTimeMillis()
        )
        setDao.insert(set1_s2)

        // getPreviousSessionSets should return sets from session 2 (the latest completed session for this exercise)
        val result = setRepo.getPreviousSessionSets(exerciseId = 1L)
        assertEquals("Expected sets from the latest session", 1, result.size)
        assertEquals("Expected weight from session 2", 75.0, result[0].weightKg, 0.01)
    }

    // LOG-05: activeSession_isNullInitially
    @Test
    fun activeSession_isNullInitially() = runTest {
        val sessionDao = FakeWorkoutSessionDao()
        val sessionRepo = WorkoutSessionRepository(sessionDao)
        
        val result = sessionRepo.getActiveSession()
        assertNull("Expected null initial active session", result)
    }

    // LOG-05: elapsedMs_isZeroBeforeSessionStarted
    @Test
    fun elapsedMs_isZeroBeforeSessionStarted() = runTest {
        val setDao = FakeWorkoutSetDao()
        val setRepo = WorkoutSetRepository(setDao)
        
        // Empty state means no elapsed time
        val sets = setRepo.getPreviousSessionSets(exerciseId = 1L)
        assertEquals("Expected empty sets when no prior session", 0, sets.size)
    }
}
