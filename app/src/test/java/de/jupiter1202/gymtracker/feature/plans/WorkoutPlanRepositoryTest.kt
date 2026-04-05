package de.jupiter1202.gymtracker.feature.plans

import android.content.Context
import de.jupiter1202.gymtracker.core.database.dao.PlanExerciseDao
import de.jupiter1202.gymtracker.core.database.dao.PlanExerciseWithExercise
import de.jupiter1202.gymtracker.core.database.dao.WorkoutPlanDao
import de.jupiter1202.gymtracker.core.database.entities.Exercise
import de.jupiter1202.gymtracker.core.database.entities.PlanExercise
import de.jupiter1202.gymtracker.core.database.entities.WorkoutPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WorkoutPlanRepositoryTest {

    private lateinit var fakePlanDao: FakeWorkoutPlanDao
    private lateinit var fakePlanExerciseDao: FakePlanExerciseDao
    private lateinit var repository: WorkoutPlanRepository

    @Before
    fun setUp() {
        fakePlanDao = FakeWorkoutPlanDao()
        fakePlanExerciseDao = FakePlanExerciseDao()
        // Pass null as context - tests don't call loadTemplates() so it won't be accessed
        repository = WorkoutPlanRepository(fakePlanDao, fakePlanExerciseDao, null)
    }

    // PLAN-01: createPlan returns a valid (positive) row id
    @Test
    fun createPlan_returnsValidId() = runTest {
        val result = repository.createPlan("Test", null)
        assertTrue("Expected id > 0, got $result", result > 0)
    }

    // PLAN-01: createPlan trims the name
    @Test
    fun createPlan_trimsName() = runTest {
        repository.createPlan("  My Plan  ", null)
        assertEquals("Expected trimmed name", "My Plan", fakePlanDao.lastInserted?.name)
    }

    // PLAN-01: createPlan stores null description as null
    @Test
    fun createPlan_storesNullDescription() = runTest {
        repository.createPlan("My Plan", null)
        assertEquals("Expected null description", null, fakePlanDao.lastInserted?.description)
    }

    // PLAN-01: deletePlan completes without exception
    @Test
    fun deletePlan_isAlwaysAllowed() = runTest {
        val plan = WorkoutPlan(id = 1, name = "Test", createdAt = 0L)
        repository.deletePlan(plan)
        assertTrue("deletePlan should complete without exception", fakePlanDao.deleteCalled)
    }

    // PLAN-02: importTemplate creates one plan row + N exercise rows
    @Test
    fun importTemplate_createsWorkoutPlanAndExerciseRows() = runTest {
        val program = TemplateProgram(
            id = "ppl",
            name = "Push Pull Legs",
            description = "Test description",
            days = listOf(
                TemplateDay(
                    name = "Push",
                    exercises = listOf(
                        TemplateExercise("Bench Press", 4, "8-12"),
                        TemplateExercise("Overhead Press", 3, "10")
                    )
                ),
                TemplateDay(
                    name = "Pull",
                    exercises = listOf(
                        TemplateExercise("Pull-Up", 3, "8-12")
                    )
                )
            )
        )
        val lookup = mapOf(
            "bench press" to 1L,
            "overhead press" to 2L,
            "pull-up" to 3L
        )
        val planId = repository.importTemplate(program, lookup)
        assertTrue("Expected planId > 0, got $planId", planId > 0)
        assertEquals("Expected 3 exercise rows inserted", 3, fakePlanExerciseDao.inserted.size)
    }

    // PLAN-02: importTemplate skips exercises with no matching lookup entry
    @Test
    fun importTemplate_skipsUnmatchedExercises() = runTest {
        val program = TemplateProgram(
            id = "test",
            name = "Test Program",
            description = "Desc",
            days = listOf(
                TemplateDay(
                    name = "Day 1",
                    exercises = listOf(
                        TemplateExercise("Known Exercise", 3, "8"),
                        TemplateExercise("Unknown Exercise", 3, "8")
                    )
                )
            )
        )
        val lookup = mapOf("known exercise" to 1L)
        val planId = repository.importTemplate(program, lookup)
        assertTrue("Expected planId > 0", planId > 0)
        assertEquals("Expected only 1 exercise inserted (unknown skipped)", 1, fakePlanExerciseDao.inserted.size)
    }

     // PLAN-02: importTemplate assigns sequential orderIndex (0..N-1)
     @Test
     fun importTemplate_assignsSequentialOrderIndex() = runTest {
         val program = TemplateProgram(
             id = "test",
             name = "Test",
             description = "Desc",
             days = listOf(
                 TemplateDay(
                     name = "Day 1",
                     exercises = listOf(
                         TemplateExercise("Exercise A", 3, "8"),
                         TemplateExercise("Exercise B", 3, "8")
                     )
                 )
             )
         )
         val lookup = mapOf("exercise a" to 1L, "exercise b" to 2L)
         repository.importTemplate(program, lookup)
         assertEquals("Expected orderIndex 0 for first exercise", 0, fakePlanExerciseDao.inserted[0].orderIndex)
         assertEquals("Expected orderIndex 1 for second exercise", 1, fakePlanExerciseDao.inserted[1].orderIndex)
     }

     // PLAN-03: reorderExercises updates orderIndex for all exercises in the list
     @Test
     fun reorderExercises_updatesOrderIndexCorrectly() = runTest {
         val ex1 = PlanExercise(id = 1, planId = 1, exerciseId = 1, orderIndex = 0, targetSets = 3, targetReps = "8")
         val ex2 = PlanExercise(id = 2, planId = 1, exerciseId = 2, orderIndex = 1, targetSets = 3, targetReps = "8")
         val ex3 = PlanExercise(id = 3, planId = 1, exerciseId = 3, orderIndex = 2, targetSets = 3, targetReps = "8")
         
         // Insert in order 0, 1, 2
         fakePlanExerciseDao.insert(ex1)
         fakePlanExerciseDao.insert(ex2)
         fakePlanExerciseDao.insert(ex3)
         
         // Reorder to 2, 0, 1 (swap first and last)
         val reordered = listOf(ex3, ex1, ex2)
         repository.reorderExercises(reordered)
         
         // Verify order indices were updated
         assertEquals("Expected 3 updates", 3, fakePlanExerciseDao.updated.size)
         assertEquals("Expected first exercise to have orderIndex 0", 0, fakePlanExerciseDao.updated[0].orderIndex)
         assertEquals("Expected second exercise to have orderIndex 1", 1, fakePlanExerciseDao.updated[1].orderIndex)
         assertEquals("Expected third exercise to have orderIndex 2", 2, fakePlanExerciseDao.updated[2].orderIndex)
     }


    // --- Fake DAOs ---

    private inner class FakeWorkoutPlanDao : WorkoutPlanDao {
        private var nextId = 1L
        var deleteCalled = false
        var lastInserted: WorkoutPlan? = null

        override suspend fun insert(plan: WorkoutPlan): Long {
            lastInserted = plan
            return nextId++
        }
        override suspend fun update(plan: WorkoutPlan) {}
        override suspend fun delete(plan: WorkoutPlan) { deleteCalled = true }
        override fun getAllPlans(): Flow<List<WorkoutPlan>> = flowOf(emptyList())
        override fun getMostRecentlyUsedPlan(): Flow<WorkoutPlan?> = flowOf(null)
    }

     private inner class FakePlanExerciseDao : PlanExerciseDao {
         private var nextId = 1L
         val inserted = mutableListOf<PlanExercise>()
         val updated = mutableListOf<PlanExercise>()

         override suspend fun insert(planExercise: PlanExercise): Long {
             inserted.add(planExercise)
             return nextId++
         }
         override suspend fun update(planExercise: PlanExercise) {
             updated.add(planExercise)
         }
         override suspend fun delete(planExercise: PlanExercise) {}
         override fun getExercisesForPlan(planId: Long): Flow<List<PlanExerciseWithExercise>> = flowOf(emptyList())
         override suspend fun getMaxOrderIndex(planId: Long): Int = inserted.count { it.planId == planId } - 1
         override suspend fun countExercisesInPlan(planId: Long): Int = inserted.count { it.planId == planId }
     }
}

    // Actually, we need a real Context. Let me use a workaround - we'll make the tests not call loadTemplates
    // The repository won't try to access assets since we don't call loadTemplates() in any of these tests
