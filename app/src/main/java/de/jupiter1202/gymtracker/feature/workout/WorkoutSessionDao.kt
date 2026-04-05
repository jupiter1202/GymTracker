package de.jupiter1202.gymtracker.feature.workout

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import de.jupiter1202.gymtracker.core.database.entities.WorkoutSession

@Dao
interface WorkoutSessionDao {
    @Insert
    suspend fun insert(session: WorkoutSession): Long

    @Query("SELECT * FROM workout_sessions WHERE is_completed = 0 LIMIT 1")
    suspend fun getActiveSession(): WorkoutSession?

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getById(id: Long): WorkoutSession?

    @Update
    suspend fun update(session: WorkoutSession)

    @Query("SELECT * FROM workout_sessions WHERE is_completed = 1 ORDER BY started_at DESC")
    fun getCompletedSessions(): kotlinx.coroutines.flow.Flow<List<WorkoutSession>>
}
