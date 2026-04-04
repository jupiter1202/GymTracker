package de.jupiter1202.gymtracker.core.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import de.jupiter1202.gymtracker.core.database.entities.Exercise
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("""
        SELECT * FROM exercises
        WHERE name LIKE '%' || :query || '%'
        AND (:muscleGroup IS NULL OR primary_muscle_group = :muscleGroup)
        ORDER BY primary_muscle_group ASC, name ASC
    """)
    fun searchExercises(query: String, muscleGroup: String?): Flow<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: Exercise): Long

    @Update
    suspend fun update(exercise: Exercise)

    @Delete
    suspend fun delete(exercise: Exercise)

    @Query("SELECT COUNT(*) FROM workout_sets WHERE exercise_id = :exerciseId")
    suspend fun countUsagesInSessions(exerciseId: Long): Int
}
