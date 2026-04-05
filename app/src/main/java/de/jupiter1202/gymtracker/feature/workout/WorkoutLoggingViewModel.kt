package de.jupiter1202.gymtracker.feature.workout

import android.content.Context
import android.media.SoundPool
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.jupiter1202.gymtracker.core.UnitConverter
import de.jupiter1202.gymtracker.core.database.entities.Exercise
import de.jupiter1202.gymtracker.core.database.entities.WorkoutSession
import de.jupiter1202.gymtracker.core.database.entities.WorkoutSet
import de.jupiter1202.gymtracker.feature.settings.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// Data classes for UI state
data class WorkoutSummary(
    val sessionName: String,
    val durationMs: Long,
    val exerciseCount: Int,
    val totalSets: Int,
    val totalVolumeKg: Double
)

data class PendingSetInput(
    val weightDisplay: String,
    val reps: String
)

data class LoggedSet(
    val id: Long,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int
)

data class ExerciseSection(
    val exercise: Exercise,
    val loggedSets: List<LoggedSet>,
    val pendingInput: PendingSetInput,
    val previousPerformance: String
)

sealed class RestTimerState {
    object Idle : RestTimerState()
    data class Running(val remaining: Int, val total: Int) : RestTimerState()
}

class WorkoutLoggingViewModel(
    private val sessionRepository: WorkoutSessionRepository,
    private val setRepository: WorkoutSetRepository,
    private val settingsRepository: SettingsRepository,
    private val exerciseRepository: de.jupiter1202.gymtracker.feature.exercises.ExerciseRepository,
    private val context: Context
) : ViewModel() {

    // SoundPool for timer alert
    private val soundPool = SoundPool.Builder().setMaxStreams(1).build()
    private var soundLoaded = false
    private var soundId: Int = 0

    init {
        // Try to load timer beep sound
        try {
            val resourceId = context.resources.getIdentifier("timer_beep", "raw", context.packageName)
            if (resourceId != 0) {
                soundId = soundPool.load(context, resourceId, 1)
                soundPool.setOnLoadCompleteListener { _, _, status ->
                    soundLoaded = status == 0
                }
            }
        } catch (e: Exception) {
            // Vibration-only fallback if sound asset missing
        }
    }

    // Active session state
    private val _activeSession = MutableStateFlow<WorkoutSession?>(null)
    val activeSession: StateFlow<WorkoutSession?> = _activeSession.asStateFlow()

    // Exercise sections with logged sets and pending input
    private val _exerciseSections = MutableStateFlow<List<ExerciseSection>>(emptyList())
    val exerciseSections: StateFlow<List<ExerciseSection>> = _exerciseSections.asStateFlow()

    // Elapsed time (separate StateFlow to avoid full recompose on every tick)
    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    // Rest timer state
    private val _restTimerState = MutableStateFlow<RestTimerState>(RestTimerState.Idle)
    val restTimerState: StateFlow<RestTimerState> = _restTimerState.asStateFlow()

    // Workout summary (post-completion)
    private val _summary = MutableStateFlow<WorkoutSummary?>(null)
    val summary: StateFlow<WorkoutSummary?> = _summary.asStateFlow()

    // Weight unit from settings
    val weightUnit: StateFlow<String> = settingsRepository.weightUnit
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "kg")

    // Timer jobs
    private var elapsedTimerJob: Job? = null
    private var restTimerJob: Job? = null

    /**
     * Start a new workout session with given exercises
     */
    fun startSession(name: String, planId: Long?, exercises: List<Exercise>) {
        viewModelScope.launch {
            val sessionId = sessionRepository.createSession(name, planId)
            val session = sessionRepository.getSessionById(sessionId)
            
            if (session != null) {
                _activeSession.value = session
                
                // Create exercise sections with previous performance pre-filled
                val sections = exercises.map { exercise ->
                    val previousSets = setRepository.getPreviousSessionSets(exercise.id)
                    val previousPerformance = formatPreviousPerformance(previousSets, weightUnit.value)
                    val initialWeight = if (previousSets.isNotEmpty()) {
                        val lastSetKg = previousSets.last().weightKg
                        if (weightUnit.value == "lbs") {
                            UnitConverter.kgToLbs(lastSetKg).toString()
                        } else {
                            lastSetKg.toString()
                        }
                    } else {
                        ""
                    }
                    val initialReps = previousSets.lastOrNull()?.reps?.toString() ?: ""
                    
                    ExerciseSection(
                        exercise = exercise,
                        loggedSets = emptyList(),
                        pendingInput = PendingSetInput(initialWeight, initialReps),
                        previousPerformance = previousPerformance
                    )
                }
                
                _exerciseSections.value = sections
                startElapsedTimer(session.startedAt)
            }
        }
    }

    /**
     * Start a new workout session and return the session ID
     */
    suspend fun startSessionAndGetId(name: String, planId: Long?, exercises: List<Exercise>): Long {
        val sessionId = sessionRepository.createSession(name, planId)
        val session = sessionRepository.getSessionById(sessionId)
        
        if (session != null) {
            _activeSession.value = session
            
            // Create exercise sections with previous performance pre-filled
            val sections = exercises.map { exercise ->
                val previousSets = setRepository.getPreviousSessionSets(exercise.id)
                val previousPerformance = formatPreviousPerformance(previousSets, weightUnit.value)
                val initialWeight = if (previousSets.isNotEmpty()) {
                    val lastSetKg = previousSets.last().weightKg
                    if (weightUnit.value == "lbs") {
                        UnitConverter.kgToLbs(lastSetKg).toString()
                    } else {
                        lastSetKg.toString()
                    }
                } else {
                    ""
                }
                val initialReps = previousSets.lastOrNull()?.reps?.toString() ?: ""
                
                ExerciseSection(
                    exercise = exercise,
                    loggedSets = emptyList(),
                    pendingInput = PendingSetInput(initialWeight, initialReps),
                    previousPerformance = previousPerformance
                )
            }
            
            _exerciseSections.value = sections
            startElapsedTimer(session.startedAt)
        }
        
        return sessionId
    }

    /**
     * Start a post-hoc workout session with a specific start time
     * Post-hoc sessions are immediately marked as completed
     */
    suspend fun startPostHocSession(name: String, planId: Long?, startedAt: Long) {
        sessionRepository.createPostHocSession(name, planId, startedAt)
    }

    /**
     * Resume an existing session (crash recovery)
     */
    fun resumeSession(sessionId: Long) {
        viewModelScope.launch {
            try {
                val session = sessionRepository.getSessionById(sessionId)
                
                if (session != null) {
                    _activeSession.value = session
                    
                    // GUARD: Only reload exercises if we don't have any (crash recovery scenario)
                    // Fresh starts already have exercises from startSessionAndGetId
                    if (_exerciseSections.value.isEmpty()) {
                        // Load existing exercise sections with logged sets
                        setRepository.getSetsForSession(sessionId).collect { sets ->
                            // Group sets by exerciseId
                            val exerciseIds = sets.map { it.exerciseId }.distinct()
                            val exercises = exerciseIds.mapNotNull { id ->
                                exerciseRepository.getExerciseById(id)
                            }
                            
                            // Create ExerciseSection for each exercise with its logged sets
                            val sections = exercises.map { exercise ->
                                val exerciseSets = sets.filter { it.exerciseId == exercise.id }
                                    .mapIndexed { index, set ->
                                        LoggedSet(
                                            id = set.id,
                                            setNumber = index + 1,
                                            weightKg = set.weightKg,
                                            reps = set.reps
                                        )
                                    }
                                
                                val previousSets = setRepository.getPreviousSessionSets(exercise.id)
                                val previousPerformance = formatPreviousPerformance(previousSets, weightUnit.value)
                                
                                ExerciseSection(
                                    exercise = exercise,
                                    loggedSets = exerciseSets,
                                    pendingInput = PendingSetInput("", ""),
                                    previousPerformance = previousPerformance
                                )
                            }
                            _exerciseSections.value = sections
                        }
                    }
                    
                    startElapsedTimer(session.startedAt)
                }
            } catch (e: Exception) {
                // Log error silently - handle gracefully without crashing
                // Session data may be incomplete, but recovery can proceed
            }
        }
    }

    /**
     * Add a new exercise to the active session
     */
    fun addExercise(exercise: Exercise) {
        val previousSets = viewModelScope.launch {
            val previousSets = setRepository.getPreviousSessionSets(exercise.id)
            val previousPerformance = formatPreviousPerformance(previousSets, weightUnit.value)
            val initialWeight = if (previousSets.isNotEmpty()) {
                val lastSetKg = previousSets.last().weightKg
                if (weightUnit.value == "lbs") {
                    UnitConverter.kgToLbs(lastSetKg).toString()
                } else {
                    lastSetKg.toString()
                }
            } else {
                ""
            }
            val initialReps = previousSets.lastOrNull()?.reps?.toString() ?: ""
            
            val newSection = ExerciseSection(
                exercise = exercise,
                loggedSets = emptyList(),
                pendingInput = PendingSetInput(initialWeight, initialReps),
                previousPerformance = previousPerformance
            )
            
            _exerciseSections.value = _exerciseSections.value + newSection
        }
    }

    /**
     * Remove an exercise from the active session
     */
    fun removeExercise(exercise: Exercise) {
        _exerciseSections.value = _exerciseSections.value.filter { it.exercise.id != exercise.id }
    }

    /**
     * Update pending weight input for an exercise
     */
    fun updatePendingWeight(exerciseId: Long, weight: String) {
        _exerciseSections.value = _exerciseSections.value.map { section ->
            if (section.exercise.id == exerciseId) {
                section.copy(pendingInput = section.pendingInput.copy(weightDisplay = weight))
            } else {
                section
            }
        }
    }

    /**
     * Update pending reps input for an exercise
     */
    fun updatePendingReps(exerciseId: Long, reps: String) {
        _exerciseSections.value = _exerciseSections.value.map { section ->
            if (section.exercise.id == exerciseId) {
                section.copy(pendingInput = section.pendingInput.copy(reps = reps))
            } else {
                section
            }
        }
    }

    /**
     * Log a set for an exercise with validation
     */
    fun logSet(exerciseId: Long) {
        val section = _exerciseSections.value.find { it.exercise.id == exerciseId } ?: return
        val activeSession = _activeSession.value ?: return
        
        val unit = weightUnit.value
        val weightDisplay = section.pendingInput.weightDisplay
        val repsStr = section.pendingInput.reps
        
        // Parse and validate inputs
        val kg = if (unit == "lbs") {
            val lbs = weightDisplay.toDoubleOrNull() ?: return
            UnitConverter.lbsToKg(lbs)
        } else {
            weightDisplay.toDoubleOrNull() ?: return
        }
        
        val repsInt = repsStr.toIntOrNull() ?: return
        
        // Validate ranges
        if (kg < 0.0 || repsInt < 1) return
        
        viewModelScope.launch {
            val setId = setRepository.logSet(activeSession.id, exerciseId, kg, repsInt)
            
            // Update section with new logged set
            _exerciseSections.value = _exerciseSections.value.map { sec ->
                if (sec.exercise.id == exerciseId) {
                    val newLoggedSet = LoggedSet(
                        id = setId,
                        setNumber = (sec.loggedSets.maxOfOrNull { it.setNumber } ?: 0) + 1,
                        weightKg = kg,
                        reps = repsInt
                    )
                    sec.copy(
                        loggedSets = sec.loggedSets + newLoggedSet,
                        pendingInput = PendingSetInput(
                            weightDisplay = if (unit == "lbs") {
                                UnitConverter.kgToLbs(kg).toString()
                            } else {
                                kg.toString()
                            },
                            reps = repsInt.toString()
                        )
                    )
                } else {
                    sec
                }
            }
            
            // Start rest timer
            val restTimerDuration = settingsRepository.restTimerSeconds
            restTimerDuration.collect { duration ->
                startRestTimer(duration)
            }
        }
    }

    /**
     * Delete a logged set
     */
    fun deleteSet(set: LoggedSet, exerciseId: Long) {
        viewModelScope.launch {
            val workoutSet = WorkoutSet(
                id = set.id,
                sessionId = _activeSession.value?.id ?: return@launch,
                exerciseId = exerciseId,
                setNumber = set.setNumber,
                weightKg = set.weightKg,
                reps = set.reps,
                completedAt = System.currentTimeMillis()
            )
            setRepository.deleteSet(workoutSet)
            
            // Update section
            _exerciseSections.value = _exerciseSections.value.map { sec ->
                if (sec.exercise.id == exerciseId) {
                    sec.copy(loggedSets = sec.loggedSets.filter { it.id != set.id })
                } else {
                    sec
                }
            }
        }
    }

    /**
     * Skip the current rest timer
     */
    fun skipRestTimer() {
        restTimerJob?.cancel()
        _restTimerState.value = RestTimerState.Idle
    }

    /**
     * Extend the rest timer by extra seconds
     */
    fun extendRestTimer(extraSeconds: Int) {
        val currentState = _restTimerState.value
        if (currentState is RestTimerState.Running) {
            val newDuration = currentState.remaining + extraSeconds
            skipRestTimer()
            startRestTimer(newDuration)
        }
    }

    /**
     * Finish the current session
     * Returns false if any exercise has no logged sets, true otherwise
     * This is a suspend function to ensure summary is computed before returning
     */
    suspend fun finishSession(): Boolean {
        val activeSession = _activeSession.value ?: return false
        
        // Check if any section has no sets
        if (_exerciseSections.value.any { it.loggedSets.isEmpty() }) {
            return false
        }
        
        sessionRepository.finishSession(activeSession)
        
        // Compute and set summary
        val allSets = _exerciseSections.value.flatMap { it.loggedSets }
        _summary.value = WorkoutSummary(
            sessionName = activeSession.name,
            durationMs = System.currentTimeMillis() - activeSession.startedAt,
            exerciseCount = _exerciseSections.value.size,
            totalSets = allSets.size,
            totalVolumeKg = allSets.sumOf { it.weightKg * it.reps }
        )
        
        _activeSession.value = null
        _exerciseSections.value = emptyList()
        _elapsedMs.value = 0L
        cancelTimers()
        
        return true
    }

    /**
     * Mark an exercise as done and start rest timer
     * Called when user taps "Done" button without logging another set
     */
    fun markExerciseDone(exerciseId: Long) {
        viewModelScope.launch {
            // Get rest timer duration from settings
            val restTimerDuration = settingsRepository.restTimerSeconds
            restTimerDuration.collect { duration ->
                startRestTimer(duration)
            }
        }
    }

    /**
     * Discard the active session
     */
    fun discardSession() {
        val activeSession = _activeSession.value ?: return
        
        viewModelScope.launch {
            // In a full implementation, would delete the session and all its sets
            // For now, just clear the state
            _activeSession.value = null
            _exerciseSections.value = emptyList()
            _elapsedMs.value = 0L
            cancelTimers()
        }
    }

    /**
     * Start elapsed timer (updates every second)
     */
    private fun startElapsedTimer(startedAt: Long) {
        elapsedTimerJob?.cancel()
        elapsedTimerJob = viewModelScope.launch {
            // Emit initial value immediately
            val elapsed = System.currentTimeMillis() - startedAt
            _elapsedMs.emit(elapsed)
            
            // Then loop with delay first to ensure updates every 1 second
            while (isActive) {
                delay(1_000L)
                val elapsed = System.currentTimeMillis() - startedAt
                _elapsedMs.emit(elapsed)
            }
        }
    }

    /**
     * Start rest timer countdown
     */
    private fun startRestTimer(durationSeconds: Int) {
        restTimerJob?.cancel()
        _restTimerState.value = RestTimerState.Running(remaining = durationSeconds, total = durationSeconds)
        
        restTimerJob = viewModelScope.launch {
            var remaining = durationSeconds
            while (remaining > 0 && isActive) {
                delay(1_000L)
                remaining--
                _restTimerState.value = RestTimerState.Running(remaining = remaining, total = durationSeconds)
            }
            if (isActive) {
                _restTimerState.value = RestTimerState.Idle
                triggerTimerAlert()
            }
        }
    }

    /**
     * Trigger timer alert (vibration + sound)
     */
    private fun triggerTimerAlert() {
        // Vibration
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(VibrationEffect.createOneShot(300L, VibrationEffect.DEFAULT_AMPLITUDE))
        
        // Sound
        if (soundLoaded && soundId > 0) {
            soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
    }

    /**
     * Cancel all timers
     */
    private fun cancelTimers() {
        elapsedTimerJob?.cancel()
        restTimerJob?.cancel()
    }

    /**
     * Format previous performance string
     */
    private fun formatPreviousPerformance(sets: List<WorkoutSet>, unit: String): String {
        if (sets.isEmpty()) return ""
        
        // Group by set number and count
        val setCount = sets.size
        val lastSet = sets.last()
        val weight = if (unit == "lbs") {
            UnitConverter.kgToLbs(lastSet.weightKg)
        } else {
            lastSet.weightKg
        }
        
        return "Last: ${setCount}×${lastSet.reps} @ ${String.format("%.1f", weight)} $unit"
    }

    override fun onCleared() {
        soundPool.release()
        cancelTimers()
        super.onCleared()
    }
}
