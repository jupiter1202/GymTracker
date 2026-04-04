package de.jupiter1202.gymtracker.feature.exercises

import de.jupiter1202.gymtracker.core.database.dao.ExerciseDao
import de.jupiter1202.gymtracker.core.database.entities.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExerciseRepositoryTest {

    private lateinit var fakeDao: FakeExerciseDao
    private lateinit var repository: ExerciseRepository

    @Before
    fun setUp() {
        fakeDao = FakeExerciseDao()
        repository = ExerciseRepository(fakeDao)
    }

    // EXER-02: deleteExercise where countUsagesInSessions returns 0 → returns DeleteResult.Deleted
    @Test
    fun deleteExercise_withNoUsages_returnsDeleted() = runTest {
        fakeDao.usageCount = 0
        val exercise = Exercise(id = 1, name = "Bench Press", primaryMuscleGroup = "Chest", equipmentType = "Barbell")

        val result = repository.deleteExercise(exercise)

        assertTrue("Expected DeleteResult.Deleted", result is DeleteResult.Deleted)
    }

    // EXER-02: deleteExercise where countUsagesInSessions returns 3 → returns DeleteResult.Blocked(sessionCount=3)
    @Test
    fun deleteExercise_withUsages_returnsBlocked() = runTest {
        fakeDao.usageCount = 3
        val exercise = Exercise(id = 1, name = "Bench Press", primaryMuscleGroup = "Chest", equipmentType = "Barbell")

        val result = repository.deleteExercise(exercise)

        assertTrue("Expected DeleteResult.Blocked", result is DeleteResult.Blocked)
        assertEquals("Expected sessionCount = 3", 3, (result as DeleteResult.Blocked).sessionCount)
    }

    // EXER-02 guard enforcement: deleteExercise with count > 0 does NOT call dao.delete()
    @Test
    fun deleteExercise_withUsages_doesNotCallDaoDelete() = runTest {
        fakeDao.usageCount = 3
        val exercise = Exercise(id = 1, name = "Bench Press", primaryMuscleGroup = "Chest", equipmentType = "Barbell")

        repository.deleteExercise(exercise)

        assertFalse("dao.delete() should NOT be called when exercise has usages", fakeDao.deleteCalled)
    }

    // --- Fake DAO ---

    private inner class FakeExerciseDao : ExerciseDao {
        var usageCount: Int = 0
        var deleteCalled: Boolean = false

        override fun searchExercises(query: String, muscleGroup: String?): Flow<List<Exercise>> {
            return flowOf(emptyList())
        }

        override suspend fun insert(exercise: Exercise): Long = 0L

        override suspend fun update(exercise: Exercise) {}

        override suspend fun delete(exercise: Exercise) {
            deleteCalled = true
        }

        override suspend fun countUsagesInSessions(exerciseId: Long): Int = usageCount
    }
}
