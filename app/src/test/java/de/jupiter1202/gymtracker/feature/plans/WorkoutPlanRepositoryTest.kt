package de.jupiter1202.gymtracker.feature.plans

import de.jupiter1202.gymtracker.core.database.entities.Exercise
import de.jupiter1202.gymtracker.core.database.entities.PlanExercise
import de.jupiter1202.gymtracker.core.database.entities.WorkoutPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

// ---------------------------------------------------------------------------
// Stub types — replaced by real production classes in plan 03-03
// ---------------------------------------------------------------------------

interface WorkoutPlanDao {
    suspend fun insert(plan: WorkoutPlan): Long
    suspend fun update(plan: WorkoutPlan)
    suspend fun delete(plan: WorkoutPlan)
    fun getAllPlans(): Flow<List<WorkoutPlan>>
}

interface PlanExerciseDao {
    suspend fun insert(pe: PlanExercise): Long
    suspend fun update(pe: PlanExercise)
    suspend fun delete(pe: PlanExercise)
    fun getExercisesForPlan(planId: Long): Flow<List<Any>>
    suspend fun getMaxOrderIndex(planId: Long): Int
}

class WorkoutPlanRepository(
    private val planDao: WorkoutPlanDao,
    private val planExerciseDao: PlanExerciseDao
) {
    suspend fun createPlan(name: String, description: String? = null): Long {
        val plan = WorkoutPlan(name = name, description = description, createdAt = System.currentTimeMillis())
        return planDao.insert(plan)
    }

    suspend fun deletePlan(plan: WorkoutPlan) {
        planDao.delete(plan)
    }

    suspend fun importTemplate(
        templatePlan: WorkoutPlan,
        exercises: List<PlanExercise>
    ): Long {
        val planId = planDao.insert(templatePlan)
        exercises.forEach { pe ->
            planExerciseDao.insert(pe.copy(planId = planId))
        }
        return planId
    }
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

class WorkoutPlanRepositoryTest {

    private lateinit var fakePlanDao: FakeWorkoutPlanDao
    private lateinit var fakePlanExerciseDao: FakePlanExerciseDao
    private lateinit var repository: WorkoutPlanRepository

    @Before
    fun setUp() {
        fakePlanDao = FakeWorkoutPlanDao()
        fakePlanExerciseDao = FakePlanExerciseDao()
        repository = WorkoutPlanRepository(fakePlanDao, fakePlanExerciseDao)
    }

    // PLAN-01: createPlan returns a valid (positive) row id
    @Test
    fun createPlan_returnsValidId() = runTest {
        val result = repository.createPlan("Test", null)
        assertTrue("Expected id > 0, got $result", result > 0)
    }

    // PLAN-01: deletePlan completes without exception
    @Test
    fun deletePlan_isAlwaysAllowed() = runTest {
        val plan = WorkoutPlan(id = 1, name = "Test", createdAt = 0L)
        repository.deletePlan(plan)
        assertTrue("deletePlan should complete without exception", fakePlanDao.deleteCalled)
    }

    // PLAN-02: importTemplate inserts a plan row and returns a positive planId
    @Test
    fun importTemplate_createsWorkoutPlanAndExerciseRows() = runTest {
        val templatePlan = WorkoutPlan(name = "PPL", createdAt = 0L)
        val exercises = listOf(
            PlanExercise(planId = 0, exerciseId = 1, orderIndex = 0, targetSets = 4, targetReps = "8-12")
        )
        val planId = repository.importTemplate(templatePlan, exercises)
        assertTrue("Expected planId > 0, got $planId", planId > 0)
    }

    // --- Fake DAOs ---

    private inner class FakeWorkoutPlanDao : WorkoutPlanDao {
        private var nextId = 1L
        var deleteCalled = false

        override suspend fun insert(plan: WorkoutPlan): Long = nextId++
        override suspend fun update(plan: WorkoutPlan) {}
        override suspend fun delete(plan: WorkoutPlan) { deleteCalled = true }
        override fun getAllPlans(): Flow<List<WorkoutPlan>> = flowOf(emptyList())
    }

    private inner class FakePlanExerciseDao : PlanExerciseDao {
        private var nextId = 1L
        val inserted = mutableListOf<PlanExercise>()

        override suspend fun insert(pe: PlanExercise): Long {
            inserted.add(pe)
            return nextId++
        }
        override suspend fun update(pe: PlanExercise) {}
        override suspend fun delete(pe: PlanExercise) {}
        override fun getExercisesForPlan(planId: Long): Flow<List<Any>> = flowOf(emptyList())
        override suspend fun getMaxOrderIndex(planId: Long): Int = inserted.count { it.planId == planId } - 1
    }
}
