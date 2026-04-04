package de.jupiter1202.gymtracker.core.di

import androidx.room.Room
import de.jupiter1202.gymtracker.core.database.GymTrackerDatabase
import de.jupiter1202.gymtracker.feature.settings.SettingsRepository
import de.jupiter1202.gymtracker.feature.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            GymTrackerDatabase::class.java,
            "gymtracker.db"
        ).build()
    }
    single { androidContext().dataStore }
    single { SettingsRepository(get()) }
    viewModel { SettingsViewModel(get()) }
}
