package de.jupiter1202.gymtracker.core.database.dao

import androidx.room.*
import de.jupiter1202.gymtracker.core.database.entities.Exercise
import de.jupiter1202.gymtracker.core.database.entities.PlanExercise
import kotlinx.coroutines.flow.Flow

data class PlanExerciseWithExercise(
    @Embedded val planExercise: PlanExercise,
    @Relation(
        parentColumn = "exercise_id",
        entityColumn = "id"
    )
    val exercise: Exercise
)

@Dao
interface PlanExerciseDao {
    @Transaction
    @Query("SELECT * FROM plan_exercises WHERE plan_id = :planId ORDER BY order_index ASC")
    fun getExercisesForPlan(planId: Long): Flow<List<PlanExerciseWithExercise>>

    @Query("SELECT COALESCE(MAX(order_index), -1) FROM plan_exercises WHERE plan_id = :planId")
    suspend fun getMaxOrderIndex(planId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(planExercise: PlanExercise): Long

    @Update
    suspend fun update(planExercise: PlanExercise)

    @Delete
    suspend fun delete(planExercise: PlanExercise)

    @Query("SELECT COUNT(*) FROM plan_exercises WHERE plan_id = :planId")
    suspend fun countExercisesInPlan(planId: Long): Int
}
