package de.jupiter1202.gymtracker.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GymTrackerDatabaseTest {
    @Test
    fun database_createsAllSixTables() {
        // TODO: implement GymTrackerDatabase in Plan 02, then write real assertions
        // Expected: in-memory DB has tables: exercises, workout_plans, plan_exercises,
        //           workout_sessions, workout_sets, body_measurements
        fail("GymTrackerDatabase not yet implemented — implement in Plan 02")
    }

    @Test
    fun database_versionIs1() {
        fail("GymTrackerDatabase not yet implemented — implement in Plan 02")
    }
}
