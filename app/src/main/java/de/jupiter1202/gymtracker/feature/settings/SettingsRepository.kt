package de.jupiter1202.gymtracker.feature.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object PreferenceKeys {
    val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
    val REST_TIMER_SECONDS = intPreferencesKey("rest_timer_seconds")
}

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val weightUnit: Flow<String> = dataStore.data.map { prefs ->
        prefs[PreferenceKeys.WEIGHT_UNIT] ?: "kg"
    }

    suspend fun setWeightUnit(unit: String) {
        require(unit == "kg" || unit == "lbs") { "unit must be 'kg' or 'lbs'" }
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.WEIGHT_UNIT] = unit
        }
    }

    val restTimerSeconds: Flow<Int> = dataStore.data.map { prefs ->
        prefs[PreferenceKeys.REST_TIMER_SECONDS] ?: 90
    }

    suspend fun setRestTimerSeconds(seconds: Int) {
        require(seconds in 10..600) { "seconds must be between 10 and 600" }
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.REST_TIMER_SECONDS] = seconds
        }
    }
}
