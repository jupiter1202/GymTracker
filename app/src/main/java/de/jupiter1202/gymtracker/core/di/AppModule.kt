package de.jupiter1202.gymtracker.core.di

import androidx.room.Room
import de.jupiter1202.gymtracker.core.database.GymTrackerDatabase
import de.jupiter1202.gymtracker.feature.exercises.ExerciseRepository
import de.jupiter1202.gymtracker.feature.exercises.ExerciseViewModel
import de.jupiter1202.gymtracker.feature.plans.WorkoutPlanRepository
import de.jupiter1202.gymtracker.feature.plans.WorkoutPlanViewModel
import de.jupiter1202.gymtracker.feature.settings.SettingsRepository
import de.jupiter1202.gymtracker.feature.settings.SettingsViewModel
import de.jupiter1202.gymtracker.feature.workout.WorkoutSessionDao
import de.jupiter1202.gymtracker.feature.workout.WorkoutSetDao
import de.jupiter1202.gymtracker.feature.workout.WorkoutSessionRepository
import de.jupiter1202.gymtracker.feature.workout.WorkoutSetRepository
import de.jupiter1202.gymtracker.feature.workout.WorkoutLoggingViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            GymTrackerDatabase::class.java,
            "gymtracker.db"
        )
            .createFromAsset("gymtracker_seed.db")
            .build()
    }
    single { androidContext().dataStore }
    single { get<GymTrackerDatabase>().exerciseDao() }
    single { get<GymTrackerDatabase>().workoutPlanDao() }
    single { get<GymTrackerDatabase>().planExerciseDao() }
    single { get<GymTrackerDatabase>().workoutSessionDao() }
    single { get<GymTrackerDatabase>().workoutSetDao() }
    single { ExerciseRepository(get()) }
    single { SettingsRepository(get()) }
    single { WorkoutPlanRepository(get(), get(), androidContext()) }
    single { WorkoutSessionRepository(get()) }
    single { WorkoutSetRepository(get()) }
    viewModel { ExerciseViewModel(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { WorkoutPlanViewModel(get()) }
    viewModel { WorkoutLoggingViewModel(get(), get(), get(), get(), androidContext()) }
}
