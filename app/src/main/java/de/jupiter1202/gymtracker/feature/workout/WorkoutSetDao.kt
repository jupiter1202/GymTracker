package de.jupiter1202.gymtracker.feature.workout

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import de.jupiter1202.gymtracker.core.database.entities.WorkoutSet
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSetDao {
    @Insert
    suspend fun insert(set: WorkoutSet): Long

    @Query("SELECT * FROM workout_sets WHERE session_id = :sessionId ORDER BY set_number ASC")
    fun getSetsForSession(sessionId: Long): Flow<List<WorkoutSet>>

    @Query("SELECT * FROM workout_sets WHERE session_id = :sessionId AND exercise_id = :exerciseId ORDER BY set_number ASC")
    fun getSetsForExercise(sessionId: Long, exerciseId: Long): Flow<List<WorkoutSet>>

    @Query("SELECT MAX(set_number) FROM workout_sets WHERE session_id = :sessionId AND exercise_id = :exerciseId")
    suspend fun getMaxSetNumber(sessionId: Long, exerciseId: Long): Int?

    @Delete
    suspend fun delete(set: WorkoutSet)

    @Query("""
        SELECT ws.* FROM workout_sets ws
        INNER JOIN workout_sessions s ON ws.session_id = s.id
        WHERE ws.exercise_id = :exerciseId
          AND s.is_completed = 1
          AND s.id = (
              SELECT MAX(s2.id) FROM workout_sessions s2
              INNER JOIN workout_sets ws2 ON ws2.session_id = s2.id
              WHERE ws2.exercise_id = :exerciseId AND s2.is_completed = 1
          )
        ORDER BY ws.set_number ASC
    """)
    suspend fun getPreviousSessionSets(exerciseId: Long): List<WorkoutSet>
}
