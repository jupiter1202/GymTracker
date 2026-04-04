package de.jupiter1202.gymtracker

import android.app.Application
import de.jupiter1202.gymtracker.core.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class GymTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@GymTrackerApp)
            modules(appModule)
        }
    }
}
