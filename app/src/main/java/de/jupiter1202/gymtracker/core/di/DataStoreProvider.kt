package de.jupiter1202.gymtracker.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

// SINGLE definition of the dataStore extension property.
// Never define preferencesDataStore("settings") in another file.
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
