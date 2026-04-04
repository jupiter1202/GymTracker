package de.jupiter1202.gymtracker.feature.exercises

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.jupiter1202.gymtracker.core.database.GymTrackerDatabase
import de.jupiter1202.gymtracker.core.database.entities.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

// ---------------------------------------------------------------------------
// Wave 0 stub — removed and replaced by the real @Dao in Wave 2 (02-02-PLAN.md)
// The real ExerciseDao will be an @Dao-annotated interface in the feature package.
// ---------------------------------------------------------------------------

/** Placeholder DAO interface — real @Dao annotation and Room implementation added in Wave 2. */
interface ExerciseDao {
    fun searchExercises(query: String, muscleGroup: String?): Flow<List<Exercise>>
    suspend fun insert(exercise: Exercise): Long
    suspend fun update(exercise: Exercise)
    suspend fun delete(exercise: Exercise)
    suspend fun countUsagesInSessions(exerciseId: Long): Int
}

// ---------------------------------------------------------------------------
// Tests
// ---------------------------------------------------------------------------

@Ignore("Wave 0 scaffold — ExerciseDao not yet implemented")
@RunWith(AndroidJUnit4::class)
class ExerciseDaoTest {

    private lateinit var db: GymTrackerDatabase
    // dao assigned from db.exerciseDao() once GymTrackerDatabase declares the abstract fun in Wave 2
    private lateinit var dao: ExerciseDao

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymTrackerDatabase::class.java
        ).build()
        // Wave 2: replace with dao = db.exerciseDao()
        dao = TODO("db.exerciseDao() added in Wave 2")
    }

    @After
    fun closeDb() { db.close() }

    // EXER-01: searchExercises("", null) returns all inserted exercises
    @Test
    fun searchExercises_emptyQueryNoFilter_returnsAllExercises() = runBlocking {
        dao.insert(Exercise(name = "Bench Press", primaryMuscleGroup = "Chest", equipmentType = "Barbell"))
        dao.insert(Exercise(name = "Squat", primaryMuscleGroup = "Quads", equipmentType = "Barbell"))
        dao.insert(Exercise(name = "Pull-up", primaryMuscleGroup = "Back", equipmentType = "Bodyweight"))

        val results = dao.searchExercises("", null).first()

        assertEquals("Expected 3 exercises", 3, results.size)
    }

    // EXER-03: searchExercises("bench", null) returns only exercises whose name contains "bench" (case-insensitive)
    @Test
    fun searchExercises_queryMatchesName_returnsMatchingExercises() = runBlocking {
        dao.insert(Exercise(name = "Bench Press", primaryMuscleGroup = "Chest", equipmentType = "Barbell"))
        dao.insert(Exercise(name = "Incline Bench Press", primaryMuscleGroup = "Chest", equipmentType = "Dumbbell"))
        dao.insert(Exercise(name = "Squat", primaryMuscleGroup = "Quads", equipmentType = "Barbell"))

        val results = dao.searchExercises("bench", null).first()

        assertEquals("Expected 2 exercises matching 'bench'", 2, results.size)
        assertTrue("All results should contain 'bench' in name (case-insensitive)",
            results.all { it.name.contains("bench", ignoreCase = true) })
    }

    // EXER-03: searchExercises("", "Chest") returns only Chest exercises
    @Test
    fun searchExercises_muscleGroupFilter_returnsOnlyMatchingGroup() = runBlocking {
        dao.insert(Exercise(name = "Bench Press", primaryMuscleGroup = "Chest", equipmentType = "Barbell"))
        dao.insert(Exercise(name = "Incline Dumbbell Press", primaryMuscleGroup = "Chest", equipmentType = "Dumbbell"))
        dao.insert(Exercise(name = "Squat", primaryMuscleGroup = "Quads", equipmentType = "Barbell"))

        val results = dao.searchExercises("", "Chest").first()

        assertEquals("Expected 2 Chest exercises", 2, results.size)
        assertTrue("All results should be Chest exercises",
            results.all { it.primaryMuscleGroup == "Chest" })
    }

    // EXER-03 combined filter: searchExercises("press", "Chest") returns only Chest exercises containing "press"
    @Test
    fun searchExercises_queryAndMuscleGroupFilter_returnsCombinedResults() = runBlocking {
        dao.insert(Exercise(name = "Bench Press", primaryMuscleGroup = "Chest", equipmentType = "Barbell"))
        dao.insert(Exercise(name = "Incline Dumbbell Press", primaryMuscleGroup = "Chest", equipmentType = "Dumbbell"))
        dao.insert(Exercise(name = "Overhead Press", primaryMuscleGroup = "Shoulders", equipmentType = "Barbell"))
        dao.insert(Exercise(name = "Squat", primaryMuscleGroup = "Quads", equipmentType = "Barbell"))

        val results = dao.searchExercises("press", "Chest").first()

        assertEquals("Expected 2 Chest exercises containing 'press'", 2, results.size)
        assertTrue("All results should be Chest exercises with 'press' in name",
            results.all { it.primaryMuscleGroup == "Chest" && it.name.contains("press", ignoreCase = true) })
    }

    // EXER-02: insert + searchExercises("", null) includes new exercise
    @Test
    fun insert_thenSearch_includesNewExercise() = runBlocking {
        val newExercise = Exercise(name = "Romanian Deadlift", primaryMuscleGroup = "Hamstrings", equipmentType = "Barbell")
        dao.insert(newExercise)

        val results = dao.searchExercises("", null).first()

        assertTrue("Inserted exercise should appear in results",
            results.any { it.name == "Romanian Deadlift" })
    }

    // EXER-02 guard precondition: countUsagesInSessions returns 0 for an exercise with no workout_sets rows
    @Test
    fun countUsagesInSessions_exerciseWithNoSets_returnsZero() = runBlocking {
        val id = dao.insert(Exercise(name = "Deadlift", primaryMuscleGroup = "Back", equipmentType = "Barbell"))

        val count = dao.countUsagesInSessions(id)

        assertEquals("Exercise with no sets should have 0 usages", 0, count)
    }

    // EXER-01: All seeded exercises (from seed db) have isCustom = false
    @Ignore("seed db not yet present")
    @Test
    fun seededExercises_allHaveIsCustomFalse() = runBlocking {
        val results = dao.searchExercises("", null).first()

        assertTrue("Seed database should contain at least 100 exercises", results.size >= 100)
        assertTrue("All seeded exercises should have isCustom = false",
            results.all { !it.isCustom })
    }
}
