package de.jupiter1202.gymtracker.feature.plans

import android.content.Context
import de.jupiter1202.gymtracker.core.database.dao.PlanExerciseDao
import de.jupiter1202.gymtracker.core.database.dao.PlanExerciseWithExercise
import de.jupiter1202.gymtracker.core.database.dao.WorkoutPlanDao
import de.jupiter1202.gymtracker.core.database.entities.PlanExercise
import de.jupiter1202.gymtracker.core.database.entities.WorkoutPlan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class TemplateProgram(
    val id: String,
    val name: String,
    val description: String,
    val days: List<TemplateDay>
)

@Serializable
data class TemplateDay(
    val name: String,
    val exercises: List<TemplateExercise>
)

@Serializable
data class TemplateExercise(
    @SerialName("exercise_name") val exerciseName: String,
    @SerialName("target_sets") val targetSets: Int,
    @SerialName("target_reps") val targetReps: String
)

class WorkoutPlanRepository(
    private val planDao: WorkoutPlanDao,
    private val planExerciseDao: PlanExerciseDao,
    private val context: Context?
) {
    private val json = Json { ignoreUnknownKeys = true }

    fun getPlans(): Flow<List<WorkoutPlan>> = planDao.getAllPlans()

    suspend fun createPlan(name: String, description: String?): Long {
        val plan = WorkoutPlan(
            name = name.trim(),
            description = description,
            createdAt = System.currentTimeMillis()
        )
        return planDao.insert(plan)
    }

    suspend fun updatePlan(plan: WorkoutPlan) = planDao.update(plan)

    suspend fun deletePlan(plan: WorkoutPlan) = planDao.delete(plan)

    fun getPlanExercises(planId: Long): Flow<List<PlanExerciseWithExercise>> =
        planExerciseDao.getExercisesForPlan(planId)

    suspend fun addExercise(planId: Long, exerciseId: Long, sets: Int, reps: String) {
        val nextIndex = planExerciseDao.getMaxOrderIndex(planId) + 1
        planExerciseDao.insert(
            PlanExercise(
                planId = planId,
                exerciseId = exerciseId,
                orderIndex = nextIndex,
                targetSets = sets,
                targetReps = reps
            )
        )
    }

    suspend fun updateExerciseTargets(planExercise: PlanExercise, sets: Int, reps: String) {
        planExerciseDao.update(planExercise.copy(targetSets = sets, targetReps = reps))
    }

    suspend fun removeExercise(planExercise: PlanExercise) = planExerciseDao.delete(planExercise)

    suspend fun reorderExercises(exercises: List<PlanExercise>) {
        exercises.forEachIndexed { index, exercise ->
            planExerciseDao.update(exercise.copy(orderIndex = index))
        }
    }

    suspend fun loadTemplates(): List<TemplateProgram> = withContext(Dispatchers.IO) {
        if (context == null) emptyList() // Fallback for testing
        else {
            val jsonString = context.assets.open("templates.json")
                .bufferedReader()
                .use { it.readText() }
            json.decodeFromString<List<TemplateProgram>>(jsonString)
        }
    }

    // exerciseLookup: Map<String (lowercase name), exerciseId>
    suspend fun importTemplate(
        program: TemplateProgram,
        exerciseLookup: Map<String, Long>
    ): Long {
        val planId = createPlan(program.name, program.description)
        var orderIndex = 0
        for (day in program.days) {
            for (templateExercise in day.exercises) {
                val exerciseId = exerciseLookup[templateExercise.exerciseName.lowercase()]
                if (exerciseId != null) {
                    planExerciseDao.insert(
                        PlanExercise(
                            planId = planId,
                            exerciseId = exerciseId,
                            orderIndex = orderIndex++,
                            targetSets = templateExercise.targetSets,
                            targetReps = templateExercise.targetReps
                        )
                    )
                }
                // Unmatched exercise names are skipped; planId is still returned
            }
        }
        return planId
    }
}
