package de.jupiter1202.gymtracker.core.database.dao

import androidx.room.*
import de.jupiter1202.gymtracker.core.database.entities.WorkoutPlan
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutPlanDao {
    @Query("SELECT * FROM workout_plans ORDER BY created_at DESC")
    fun getAllPlans(): Flow<List<WorkoutPlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: WorkoutPlan): Long

    @Update
    suspend fun update(plan: WorkoutPlan)

    @Delete
    suspend fun delete(plan: WorkoutPlan)
}
