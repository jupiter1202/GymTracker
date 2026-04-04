package de.jupiter1202.gymtracker.feature.plans

import de.jupiter1202.gymtracker.core.database.entities.PlanExercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// ---------------------------------------------------------------------------
// Stub repository — replaced by real production class in plan 03-03
// ---------------------------------------------------------------------------

class PlanExerciseRepository(
    private val planExerciseDao: PlanExerciseDao
) {
    suspend fun addExercise(planId: Long, exerciseId: Long, targetSets: Int, targetReps: String): Long {
        val maxIndex = planExerciseDao.getMaxOrderIndex(planId)
        val nextIndex = maxIndex + 1
        val pe = PlanExercise(
            planId = planId,
            exerciseId = exerciseId,
            orderIndex = nextIndex,
            targetSets = targetSets,
            targetReps = targetReps
        )
        return planExerciseDao.insert(pe)
    }

    suspend fun removeExercise(planExercise: PlanExercise) {
        planExerciseDao.delete(planExercise)
    }

    suspend fun reorderExercises(exercises: List<PlanExercise>) {
        exercises.forEachIndexed { index, pe ->
            planExerciseDao.update(pe.copy(orderIndex = index))
        }
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

class PlanExerciseRepositoryTest {

    private lateinit var fakeDao: FakePlanExerciseDaoForRepo
    private lateinit var repository: PlanExerciseRepository

    @Before
    fun setUp() {
        fakeDao = FakePlanExerciseDaoForRepo()
        repository = PlanExerciseRepository(fakeDao)
    }

    // PLAN-03: addExercise appends with orderIndex = maxIndex + 1
    @Test
    fun addExercise_appendsWithCorrectOrderIndex() = runTest {
        fakeDao.maxIndex = 2
        repository.addExercise(planId = 1, exerciseId = 5, targetSets = 3, targetReps = "10")
        val inserted = fakeDao.lastInserted!!
        assertEquals("Expected orderIndex = 3 (maxIndex + 1)", 3, inserted.orderIndex)
    }

    // PLAN-03: removeExercise deletes the correct row
    @Test
    fun removeExercise_deletesCorrectRow() = runTest {
        val pe = PlanExercise(id = 7, planId = 1, exerciseId = 2, orderIndex = 0, targetSets = 3, targetReps = "8")
        repository.removeExercise(pe)
        assertEquals("Expected the exact PlanExercise to be deleted", pe, fakeDao.lastDeleted)
    }

    // PLAN-03: reorderExercises updates all orderIndex values in sequence
    @Test
    fun reorderExercises_updatesAllOrderIndexValues() = runTest {
        val exercises = listOf(
            PlanExercise(id = 1, planId = 1, exerciseId = 10, orderIndex = 2, targetSets = 3, targetReps = "8"),
            PlanExercise(id = 2, planId = 1, exerciseId = 11, orderIndex = 0, targetSets = 3, targetReps = "8"),
            PlanExercise(id = 3, planId = 1, exerciseId = 12, orderIndex = 1, targetSets = 3, targetReps = "8")
        )
        repository.reorderExercises(exercises)
        assertEquals("Expected 3 updates", 3, fakeDao.updatedItems.size)
        fakeDao.updatedItems.forEachIndexed { index, pe ->
            assertEquals("Expected orderIndex = $index for item $index", index, pe.orderIndex)
        }
    }

    // --- Fake DAO ---

    private inner class FakePlanExerciseDaoForRepo : PlanExerciseDao {
        var maxIndex: Int = -1
        var lastInserted: PlanExercise? = null
        var lastDeleted: PlanExercise? = null
        val updatedItems = mutableListOf<PlanExercise>()

        override suspend fun insert(pe: PlanExercise): Long {
            lastInserted = pe
            return 1L
        }
        override suspend fun update(pe: PlanExercise) { updatedItems.add(pe) }
        override suspend fun delete(pe: PlanExercise) { lastDeleted = pe }
        override fun getExercisesForPlan(planId: Long): Flow<List<Any>> = flowOf(emptyList())
        override suspend fun getMaxOrderIndex(planId: Long): Int = maxIndex
    }
}
