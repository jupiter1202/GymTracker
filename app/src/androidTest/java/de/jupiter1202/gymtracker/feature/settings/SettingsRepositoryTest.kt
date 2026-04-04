package de.jupiter1202.gymtracker.feature.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {
    @get:Rule
    val tmpFolder = TemporaryFolder(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir)

    private fun createRepository(): SettingsRepository {
        val dataStore = PreferenceDataStoreFactory.create {
            tmpFolder.newFile("test_settings.preferences_pb")
        }
        return SettingsRepository(dataStore)
    }

    @Test
    fun weightUnit_defaultsToKg() = runTest {
        val repo = createRepository()
        assertEquals("kg", repo.weightUnit.first())
    }

    @Test
    fun setWeightUnit_lbs_persistsAcrossInstances() = runTest {
        val repo = createRepository()
        repo.setWeightUnit("lbs")
        assertEquals("lbs", repo.weightUnit.first())
    }
}
