package de.jupiter1202.gymtracker.feature.workout

import de.jupiter1202.gymtracker.core.database.entities.WorkoutSet
import kotlinx.coroutines.flow.Flow

class WorkoutSetRepository(private val dao: WorkoutSetDao) {
    suspend fun logSet(
        sessionId: Long,
        exerciseId: Long,
        weightKg: Double,
        reps: Int
    ): Long {
        val nextSetNumber = (dao.getMaxSetNumber(sessionId, exerciseId) ?: 0) + 1
        val set = WorkoutSet(
            sessionId = sessionId,
            exerciseId = exerciseId,
            setNumber = nextSetNumber,
            weightKg = weightKg,
            reps = reps,
            completedAt = System.currentTimeMillis()
        )
        return dao.insert(set)
    }

    fun getSetsForSession(sessionId: Long) = dao.getSetsForSession(sessionId)

    fun getSetsForExercise(sessionId: Long, exerciseId: Long) =
        dao.getSetsForExercise(sessionId, exerciseId)

    suspend fun getPreviousSessionSets(exerciseId: Long): List<WorkoutSet> =
        dao.getPreviousSessionSets(exerciseId)

    suspend fun deleteSet(set: WorkoutSet) = dao.delete(set)
}
