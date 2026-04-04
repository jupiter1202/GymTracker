package de.jupiter1202.gymtracker.feature.exercises

import de.jupiter1202.gymtracker.core.database.dao.ExerciseDao
import de.jupiter1202.gymtracker.core.database.entities.Exercise
import kotlinx.coroutines.flow.Flow

sealed class DeleteResult {
    object Deleted : DeleteResult()
    data class Blocked(val sessionCount: Int) : DeleteResult()
}

class ExerciseRepository(private val dao: ExerciseDao) {
    fun searchExercises(query: String, muscleGroup: String?): Flow<List<Exercise>> =
        dao.searchExercises(query, muscleGroup)

    suspend fun insertExercise(exercise: Exercise): Long = dao.insert(exercise)

    suspend fun updateExercise(exercise: Exercise) = dao.update(exercise)

    suspend fun deleteExercise(exercise: Exercise): DeleteResult {
        val count = dao.countUsagesInSessions(exercise.id)
        return if (count > 0) {
            DeleteResult.Blocked(count)
        } else {
            dao.delete(exercise)
            DeleteResult.Deleted
        }
    }
}
