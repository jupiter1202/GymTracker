package de.jupiter1202.gymtracker.feature.workout

import de.jupiter1202.gymtracker.core.database.entities.WorkoutSet
import de.jupiter1202.gymtracker.core.database.entities.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// Fake implementation of WorkoutSetDao for testing
internal class FakeWorkoutSetDaoForSet : WorkoutSetDao {
    private val sets = mutableListOf<WorkoutSet>()
    private var nextId = 1L
    var completedSessionIds = setOf<Long>()

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
        // Return sets from the most recent completed session for this exercise
        if (sets.isEmpty()) return emptyList()
        val maxSessionId = sets.filter { it.exerciseId == exerciseId }.maxOfOrNull { it.sessionId } ?: return emptyList()
        return sets.filter { it.sessionId == maxSessionId && it.exerciseId == exerciseId }
    }
}

class WorkoutSetRepositoryTest {
    private lateinit var fakeDao: FakeWorkoutSetDaoForSet
    private lateinit var repository: WorkoutSetRepository

    @Before
    fun setUp() {
        fakeDao = FakeWorkoutSetDaoForSet()
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
