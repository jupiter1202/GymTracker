package de.jupiter1202.gymtracker.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import de.jupiter1202.gymtracker.core.database.dao.ExerciseDao
import de.jupiter1202.gymtracker.core.database.dao.PlanExerciseDao
import de.jupiter1202.gymtracker.core.database.dao.WorkoutPlanDao
import de.jupiter1202.gymtracker.core.database.entities.BodyMeasurement
import de.jupiter1202.gymtracker.core.database.entities.Exercise
import de.jupiter1202.gymtracker.core.database.entities.PlanExercise
import de.jupiter1202.gymtracker.core.database.entities.WorkoutPlan
import de.jupiter1202.gymtracker.core.database.entities.WorkoutSession
import de.jupiter1202.gymtracker.core.database.entities.WorkoutSet

@Database(
    entities = [
        Exercise::class,
        WorkoutPlan::class,
        PlanExercise::class,
        WorkoutSession::class,
        WorkoutSet::class,
        BodyMeasurement::class
    ],
    version = 1,
    exportSchema = true
)
abstract class GymTrackerDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutPlanDao(): WorkoutPlanDao
    abstract fun planExerciseDao(): PlanExerciseDao
}
