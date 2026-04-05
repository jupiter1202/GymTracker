package de.jupiter1202.gymtracker.feature.workout

import de.jupiter1202.gymtracker.core.database.entities.WorkoutSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

// Stub ViewModel for testing (LOG-05, LOG-01)
private class WorkoutLoggingViewModel {
    var activeSession: MutableStateFlow<WorkoutSession?> = MutableStateFlow(null)

    fun computeElapsedMs(): Long = TODO()
}

class WorkoutLoggingViewModelTest {

    // LOG-05: elapsedMs_equalsCurrentTimeMinusStartedAt
    @Test
    fun elapsedMs_equalsCurrentTimeMinusStartedAt() = runTest {
        val viewModel = WorkoutLoggingViewModel()
        val startedAt = System.currentTimeMillis() - 5000
        viewModel.activeSession.value = WorkoutSession(
            id = 1,
            planId = null,
            name = "Test",
            startedAt = startedAt,
            finishedAt = null,
            isCompleted = false
        )
        val elapsed = viewModel.computeElapsedMs()
        assertEquals("Expected elapsed time between 4900 and 5100 ms", true, elapsed in 4900L..5100L)
    }

    // LOG-05: elapsedMs_isZeroBeforeSessionStarted
    @Test
    fun elapsedMs_isZeroBeforeSessionStarted() = runTest {
        val viewModel = WorkoutLoggingViewModel()
        val elapsed = viewModel.computeElapsedMs()
        assertEquals("Expected 0 ms when no active session", 0L, elapsed)
    }

    // LOG-01: activeSession_isNullInitially
    @Test
    fun activeSession_isNullInitially() = runTest {
        val viewModel = WorkoutLoggingViewModel()
        assertNull("Expected null initial active session", viewModel.activeSession.value)
    }
}
