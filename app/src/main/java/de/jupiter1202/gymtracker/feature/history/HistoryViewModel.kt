package de.jupiter1202.gymtracker.feature.history

import androidx.lifecycle.ViewModel
import de.jupiter1202.gymtracker.feature.workout.WorkoutSessionRepository

class HistoryViewModel(
    private val sessionRepository: WorkoutSessionRepository
) : ViewModel() {
    val sessions = sessionRepository.getCompletedSessions()
}
