package de.jupiter1202.gymtracker.core.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GymTrackerDatabaseTest {
    private lateinit var db: GymTrackerDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            GymTrackerDatabase::class.java
        ).build()
    }

    @After
    fun closeDb() { db.close() }

    @Test
    fun database_createsAllSixTables() {
        val cursor = db.openHelper.readableDatabase.query(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'android_%' AND name NOT LIKE 'sqlite_%' AND name != 'room_master_table'"
        )
        val tables = mutableSetOf<String>()
        while (cursor.moveToNext()) { tables.add(cursor.getString(0)) }
        cursor.close()
        val expected = setOf("exercises", "workout_plans", "plan_exercises", "workout_sessions", "workout_sets", "body_measurements")
        assertTrue("Missing tables: ${expected - tables}", tables.containsAll(expected))
    }

    @Test
    fun database_versionIs1() {
        val version = db.openHelper.readableDatabase.version
        assertTrue("Expected version 1, got $version", version == 1)
    }
}
