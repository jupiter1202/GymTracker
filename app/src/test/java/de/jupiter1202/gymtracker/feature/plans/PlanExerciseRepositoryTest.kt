package de.jupiter1202.gymtracker.feature.plans

import android.content.Context
import de.jupiter1202.gymtracker.core.database.dao.PlanExerciseDao
import de.jupiter1202.gymtracker.core.database.dao.PlanExerciseWithExercise
import de.jupiter1202.gymtracker.core.database.dao.WorkoutPlanDao
import de.jupiter1202.gymtracker.core.database.entities.PlanExercise
import de.jupiter1202.gymtracker.core.database.entities.WorkoutPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PlanExerciseRepositoryTest {

    private lateinit var fakePlanDao: FakeWorkoutPlanDaoForExercise
    private lateinit var fakeExerciseDao: FakePlanExerciseDaoForRepo
    private lateinit var repository: WorkoutPlanRepository

    @Before
    fun setUp() {
        fakePlanDao = FakeWorkoutPlanDaoForExercise()
        fakeExerciseDao = FakePlanExerciseDaoForRepo()
        repository = WorkoutPlanRepository(fakePlanDao, fakeExerciseDao, null)
    }

    // PLAN-03: addExercise appends with orderIndex = maxIndex + 1
    @Test
    fun addExercise_appendsWithCorrectOrderIndex() = runTest {
        fakeExerciseDao.maxIndex = 2
        repository.addExercise(planId = 1, exerciseId = 5, sets = 3, reps = "10")
        val inserted = fakeExerciseDao.lastInserted!!
        assertEquals("Expected orderIndex = 3 (maxIndex + 1)", 3, inserted.orderIndex)
    }

    // PLAN-03: removeExercise deletes the correct row
    @Test
    fun removeExercise_deletesCorrectRow() = runTest {
        val pe = PlanExercise(id = 7, planId = 1, exerciseId = 2, orderIndex = 0, targetSets = 3, targetReps = "8")
        repository.removeExercise(pe)
        assertEquals("Expected the exact PlanExercise to be deleted", pe, fakeExerciseDao.lastDeleted)
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
        assertEquals("Expected 3 updates", 3, fakeExerciseDao.updatedItems.size)
        fakeExerciseDao.updatedItems.forEachIndexed { index, pe ->
            assertEquals("Expected orderIndex = $index for item $index", index, pe.orderIndex)
        }
    }

    // --- Fake DAOs ---

    private inner class FakeWorkoutPlanDaoForExercise : WorkoutPlanDao {
        override suspend fun insert(plan: WorkoutPlan): Long = 1L
        override suspend fun update(plan: WorkoutPlan) {}
        override suspend fun delete(plan: WorkoutPlan) {}
        override fun getAllPlans(): Flow<List<WorkoutPlan>> = flowOf(emptyList())
        override fun getMostRecentlyUsedPlan(): Flow<WorkoutPlan?> = flowOf(null)
    }

    private inner class FakePlanExerciseDaoForRepo : PlanExerciseDao {
        var maxIndex: Int = -1
        var lastInserted: PlanExercise? = null
        var lastDeleted: PlanExercise? = null
        val updatedItems = mutableListOf<PlanExercise>()

        override suspend fun insert(planExercise: PlanExercise): Long {
            lastInserted = planExercise
            return 1L
        }
        override suspend fun update(planExercise: PlanExercise) { updatedItems.add(planExercise) }
        override suspend fun delete(planExercise: PlanExercise) { lastDeleted = planExercise }
        override fun getExercisesForPlan(planId: Long): Flow<List<PlanExerciseWithExercise>> = flowOf(emptyList())
        override suspend fun getMaxOrderIndex(planId: Long): Int = maxIndex
        override suspend fun countExercisesInPlan(planId: Long): Int = 0
    }
}
