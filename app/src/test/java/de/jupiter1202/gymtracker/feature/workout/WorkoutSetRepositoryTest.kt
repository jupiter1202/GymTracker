package de.jupiter1202.gymtracker.feature.workout

import de.jupiter1202.gymtracker.core.database.entities.WorkoutSet
import de.jupiter1202.gymtracker.core.database.entities.WorkoutSession
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Stub interface for WorkoutSetRepository (LOG-01, LOG-04)
interface StubWorkoutSetDao {
    suspend fun insert(set: WorkoutSet): Long
    suspend fun getMaxSetNumber(sessionId: Long, exerciseId: Long): Int?
    suspend fun getPreviousSessionSets(exerciseId: Long): List<WorkoutSet>
}

// Fake implementation of StubWorkoutSetDao for testing
private class FakeWorkoutSetDao : StubWorkoutSetDao {
    private val sets = mutableListOf<WorkoutSet>()
    private var nextId = 1L

    override suspend fun insert(set: WorkoutSet): Long {
        val setWithId = set.copy(id = nextId)
        sets.add(setWithId)
        return nextId++
    }

    override suspend fun getMaxSetNumber(sessionId: Long, exerciseId: Long): Int? {
        return sets
            .filter { it.sessionId == sessionId && it.exerciseId == exerciseId }
            .maxOfOrNull { it.setNumber }
    }

    override suspend fun getPreviousSessionSets(exerciseId: Long): List<WorkoutSet> {
        // Return sets from the most recent completed session for this exercise
        // (For stub purposes, return only sets for exercise that have highest session ID)
        if (sets.isEmpty()) return emptyList()
        val maxSessionId = sets.filter { it.exerciseId == exerciseId }.maxOfOrNull { it.sessionId } ?: return emptyList()
        return sets.filter { it.sessionId == maxSessionId && it.exerciseId == exerciseId }
    }
}

// Stub repository for testing (LOG-01, LOG-04)
private class WorkoutSetRepository(val dao: StubWorkoutSetDao) {
    suspend fun logSet(sessionId: Long, exerciseId: Long, weightKg: Double, reps: Int): Long = TODO()

    suspend fun getPreviousSessionSets(exerciseId: Long): List<WorkoutSet> = TODO()
}

class WorkoutSetRepositoryTest {
    private lateinit var fakeDao: FakeWorkoutSetDao
    private lateinit var repository: WorkoutSetRepository

    @Before
    fun setUp() {
        fakeDao = FakeWorkoutSetDao()
        repository = WorkoutSetRepository(fakeDao)
    }

    // LOG-01: logSet_insertsToDao
    @Test
    fun logSet_insertsToDao() = runTest {
        val result = repository.logSet(sessionId = 1, exerciseId = 1, weightKg = 80.0, reps = 8)
        assertEquals("Expected positive id", true, result > 0)
    }

    // LOG-04: getPreviousSessionSets_returnsEmptyWhenNoPriorSession
    @Test
    fun getPreviousSessionSets_returnsEmptyWhenNoPriorSession() = runTest {
        val result = repository.getPreviousSessionSets(exerciseId = 99L)
        assertTrue("Expected empty list when no prior session", result.isEmpty())
    }

    // LOG-04: getPreviousSessionSets_returnsLastCompletedSessionSets
    @Test
    fun getPreviousSessionSets_returnsLastCompletedSessionSets() = runTest {
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
        fakeDao.insert(set1_s1)

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
        fakeDao.insert(set1_s2)

        // getPreviousSessionSets should return sets from session 2 (the latest completed session for this exercise)
        val result = repository.getPreviousSessionSets(exerciseId = 1L)
        assertEquals("Expected sets from the latest session", 1, result.size)
        assertEquals("Expected weight from session 2", 75.0, result[0].weightKg, 0.01)
    }
}
