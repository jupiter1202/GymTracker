package de.jupiter1202.gymtracker.feature.workout

import de.jupiter1202.gymtracker.core.database.entities.WorkoutSession

class WorkoutSessionRepository(private val dao: WorkoutSessionDao) {
    suspend fun createSession(name: String, planId: Long?): Long {
        val session = WorkoutSession(
            name = name.trim(),
            planId = planId,
            startedAt = System.currentTimeMillis(),
            isCompleted = false
        )
        return dao.insert(session)
    }

    suspend fun getActiveSession(): WorkoutSession? = dao.getActiveSession()

    suspend fun getSessionById(id: Long): WorkoutSession? = dao.getById(id)

    suspend fun finishSession(session: WorkoutSession) {
        dao.update(session.copy(
            finishedAt = System.currentTimeMillis(),
            isCompleted = true
        ))
    }

    fun getCompletedSessions() = dao.getCompletedSessions()

    suspend fun createPostHocSession(name: String, planId: Long?, startedAt: Long): Long {
        val session = WorkoutSession(
            name = name.trim(),
            planId = planId,
            startedAt = startedAt,
            finishedAt = System.currentTimeMillis(),
            isCompleted = true
        )
        return dao.insert(session)
    }
}
