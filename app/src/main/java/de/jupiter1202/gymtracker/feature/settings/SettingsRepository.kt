package de.jupiter1202.gymtracker.feature.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object PreferenceKeys {
    val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
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
}
