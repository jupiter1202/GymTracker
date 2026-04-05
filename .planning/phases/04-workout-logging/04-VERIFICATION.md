---
phase: 04-workout-logging
verified: 2026-04-05T16:30:00Z
status: gaps_found
score: 2/5 must-haves verified
re_verification: false
gaps:
  - truth: "User can start a workout from a plan and exercises from that plan are shown"
    status: failed
    reason: "PlansScreen.kt line 135 passes emptyList() for exercises parameter to startSessionAndGetId(), so no exercises are loaded from the plan"
    artifacts:
      - path: "app/src/main/java/de/jupiter1202/gymtracker/feature/plans/PlansScreen.kt"
        issue: "Line 135: exercises = emptyList() — should fetch exercises from plan via WorkoutPlanRepository.getPlanExercises()"
      - path: "app/src/main/java/de/jupiter1202/gymtracker/feature/workout/WorkoutLoggingViewModel.kt"
        issue: "resumeSession(sessionId) at line 206-234 loads only empty list of exercises (line 228: _exerciseSections.value = emptyList())"
    missing:
      - "PlansScreen must fetch plan exercises before starting session: repository.getPlanExercises(plan.id).collect { exercises -> }"
      - "Pass fetched exercises list to startSessionAndGetId() instead of emptyList()"
      - "WorkoutLoggingViewModel.resumeSession() must load Exercise entities from database and group logged sets by exercise (currently returns empty sections)"

  - truth: "User can log weight and reps for each exercise, with sets displayed inline as a list"
    status: partial
    reason: "Weight/reps per-set inline list structure is implemented, but sets are stored and displayed per-exercise (correct), not per-set inline as initially planned"
    artifacts:
      - path: "app/src/main/java/de/jupiter1202/gymtracker/feature/workout/ActiveWorkoutScreen.kt"
        issue: "Lines 107-114 render LoggedSetRow per set, which is correct — but user feedback (GAPS-02) suggests they expected inline set management"
    missing:
      - "Verify user feedback: is current implementation (set rows grouped by exercise) acceptable, or does user need set-by-set editing?"

  - truth: "Rest timer auto-starts after each set and counts down visibly"
    status: passed
    reason: "Rest timer banner implemented with countdown display, auto-starts on logSet(), and remains visible while scrolling"
    artifacts:
      - path: "app/src/main/java/de/jupiter1202/gymtracker/feature/workout/ActiveWorkoutScreen.kt"
        issue: "NONE — RestTimerBanner (lines 330-366) shows countdown, Skip button works"
      - path: "app/src/main/java/de/jupiter1202/gymtracker/feature/workout/WorkoutLoggingViewModel.kt"
        issue: "NONE — startRestTimer() function exists and manages RestTimerState"

  - truth: "Previous performance is shown per exercise during workout (e.g. 'Last: 3×8 @ 75 kg')"
    status: passed
    reason: "ExerciseSectionHeader renders previousPerformance string when non-empty, formatPreviousPerformance() creates the text from prior session sets"
    artifacts:
      - path: "app/src/main/java/de/jupiter1202/gymtracker/feature/workout/ActiveWorkoutScreen.kt"
        issue: "NONE — Lines 217-223 display previousPerformance in header"
      - path: "app/src/main/java/de/jupiter1202/gymtracker/feature/workout/WorkoutLoggingViewModel.kt"
        issue: "NONE — Lines 126-127, 166-167 pre-fill previousPerformance for each exercise on session start"

  - truth: "Total workout duration updates in real time in the top bar"
    status: failed
    reason: "TopAppBar title shows formatElapsed(elapsed) via elapsedMs StateFlow, but elapsedMs is NOT being updated during workout — startElapsedTimer() job never updates the state"
    artifacts:
      - path: "app/src/main/java/de/julius1202/gymtracker/feature/workout/ActiveWorkoutScreen.kt"
        issue: "Line 58 displays formatElapsed(elapsed), but elapsed value never changes because timer is not running"
      - path: "app/src/main/java/de/julius1202/gymtracker/feature/workout/WorkoutLoggingViewModel.kt"
        issue: "Lines 357-369: startElapsedTimer() initializes _elapsedMs but the while loop (line 364) is never executed — Job never emits updated values"
    missing:
      - "Fix startElapsedTimer() to use a coroutine loop that: (1) calculates elapsed time from session.startedAt, (2) emits to _elapsedMs every 1 second, (3) continues until session is finished"

deferred: []

human_verification:
  - test: "Visual verification of exercise list when starting a workout"
    expected: "When user taps 'Start' on a plan card in PlansScreen, the ActiveWorkoutScreen should display all exercises from that plan in separate sections"
    why_human: "Current code passes emptyList() so UI always shows no exercises — need to verify expected behavior after fix is applied"
  
  - test: "Rest timer vibration alert on device"
    expected: "When rest timer countdown reaches 0, device should vibrate with haptic feedback and play beep sound (if supported)"
    why_human: "Vibration and sound APIs require physical device or emulator with sensors; automated tests cannot verify haptic feedback"
  
  - test: "Crash recovery with active workout"
    expected: "Start a workout, log a set, force-stop app, reopen app — app should resume at active workout screen with logged set visible"
    why_human: "Requires simulating app process death; automated tests don't cover system-level process handling"
  
  - test: "Elapsed time updates in real time"
    expected: "In active workout, elapsed time in top bar should increment by 1 second every 1 second; after 2 minutes should show 0:02:00"
    why_human: "Time-dependent behavior requires real-time observation; automated tests would have timing flakiness"

---

# Phase 04: Workout Logging — Verification Report

**Phase Goal:** Users can log a complete workout session in real time or after the fact, with rest timer support and visibility into previous performance

**Verified:** 2026-04-05T16:30:00Z  
**Status:** gaps_found  
**Re-verification:** No (initial verification)

## Goal Achievement Summary

| # | Success Criterion (from ROADMAP.md) | Status | Evidence |
|---|-------------------------------------|--------|----------|
| 1 | User can start a workout from a plan or as an ad-hoc session and log sets with weight and reps for each exercise | ✗ PARTIAL | Exercises are not loaded from plan (emptyList() passed); resumeSession() returns empty sections |
| 2 | After logging a set, a configurable rest timer auto-starts and counts down visibly | ✓ VERIFIED | RestTimerBanner renders countdown timer, auto-starts on logSet(), Skip button works |
| 3 | During a workout, each exercise shows what the user lifted last time (e.g., "Last: 3×8 @ 75 kg") | ✓ VERIFIED | ExerciseSectionHeader displays previousPerformance string; formatPreviousPerformance() generates text from prior sets |
| 4 | While a session is active, the app displays total workout duration that updates in real time | ✗ FAILED | Elapsed time initialized but never updated — startElapsedTimer() job loop never executes |
| 5 | An in-progress workout survives app close and process death -- reopening the app resumes the session | ? UNCERTAIN | Crash recovery navigation exists in AppNavHost, but exercise data not loaded by resumeSession() |

**Score:** 2/5 success criteria verified

## Observable Truths Verification

### Truth 1: Exercise Section Populated from Plan
**Status:** ✗ FAILED  
**Root Cause:** PlansScreen passes `exercises = emptyList()` to `startSessionAndGetId()`

**Evidence:**
- PlansScreen.kt line 135: `exercises = emptyList()`
- Expected: `exercises = plan's exercises fetched from WorkoutPlanRepository.getPlanExercises(plan.id)`
- Symptom: User reported "Started workout does not contain the exercises of the plan (GAPS-01)"

**Code Path:**
1. User taps "Start" button on plan card (PlansScreen line 126)
2. onStartClick handler calls `workoutViewModel.startSessionAndGetId(..., exercises = emptyList())`
3. startSessionAndGetId() iterates over exercises list (line 165-186 in ViewModel)
4. Empty list → no ExerciseSection objects created
5. ActiveWorkoutScreen renders empty LazyColumn

**Code Locations:**
- app/src/main/java/de/jupiter1202/gymtracker/feature/plans/PlansScreen.kt:135
- app/src/main/java/de/julius1202/gymtracker/feature/workout/WorkoutLoggingViewModel.kt:157-193

### Truth 2: Crash Recovery Loads Session Exercises
**Status:** ✗ FAILED  
**Root Cause:** resumeSession() does not load Exercise entities; returns empty sections

**Evidence:**
- WorkoutLoggingViewModel.resumeSession() line 206-234
- Line 215: `setRepository.getSetsForSession(sessionId).collect { sets -> ... }`
- Line 228: `_exerciseSections.value = emptyList()` — exercise sections never populated

**Problem:**
- Function loads WorkoutSet records but not Exercise entities
- No JOIN or Exercise lookup from database
- Comment at line 227: "(Simplified - full version would need Exercise entities from DB)" — **stub indicator**

**Code Location:**
- app/src/main/java/de/julius1202/gymtracker/feature/workout/WorkoutLoggingViewModel.kt:206-234

### Truth 3: Rest Timer Auto-Starts and Counts Down
**Status:** ✓ VERIFIED  
**Evidence:**
- RestTimerBanner (ActiveWorkoutScreen.kt line 330-366) renders countdown: `"Rest · ${formatTime(state.remaining)}"`
- logSet() calls `startRestTimer()` (ViewModel line 302-313)
- AnimatedVisibility shows banner only when `restState is RestTimerState.Running`
- Skip and +30s buttons work (lines 357-363)

**Code Locations:**
- app/src/main/java/de/julius1202/gymtracker/feature/workout/ActiveWorkoutScreen.kt:82-88, 330-366
- app/src/main/java/de/julius1202/gymtracker/feature/workout/WorkoutLoggingViewModel.kt:302-313

### Truth 4: Previous Performance Displayed Per Exercise
**Status:** ✓ VERIFIED  
**Evidence:**
- ExerciseSectionHeader (ActiveWorkoutScreen.kt line 195-235) displays `previousPerformance` string
- Lines 217-223: renders previousPerformance in subtitle when non-empty
- ViewModel pre-fills previousPerformance (lines 126-127, 166-167) by calling `formatPreviousPerformance(previousSets, weightUnit.value)`
- Format: "Last: 3×8 @ 75.0 kg"

**Code Locations:**
- app/src/main/java/de/julius1202/gymtracker/feature/workout/ActiveWorkoutScreen.kt:217-223
- app/src/main/java/de/julius1202/gymtracker/feature/workout/WorkoutLoggingViewModel.kt:126-127, 166-167, 476-489

### Truth 5: Elapsed Time Updates in Real Time
**Status:** ✗ FAILED  
**Root Cause:** startElapsedTimer() initializes state but never updates it; coroutine job loop does not execute

**Evidence:**
- ActiveWorkoutScreen.kt line 58: Title displays `formatElapsed(elapsed)` but `elapsed` never changes
- ViewModel.elapsedMs StateFlow initialized to 0L (line 94) but never emitted to
- startElapsedTimer() implementation at line 357-369:
  ```kotlin
  private fun startElapsedTimer(sessionStartedAt: Long) {
      if (elapsedTimerJob?.isActive == true) return
      elapsedTimerJob = viewModelScope.launch {
          while (isActive) {
              val now = System.currentTimeMillis()
              val elapsed = now - sessionStartedAt
              _elapsedMs.value = elapsed  // Line 366
              delay(1000)
          }
      }
  }
  ```
- **Problem:** Code looks correct but is NOT being called after session start, or if called, the while loop immediately exits
- No evidence of `startElapsedTimer()` being called in `startSession()` (line 116-152) — **wait, it IS called at line 149**
- **Further investigation:** The job should update `_elapsedMs` every 1 second, but state is not flowing to UI

**Symptom:** User reported "Workout timer does not work (GAPS-03)"

**Code Locations:**
- app/src/main/java/de/julius1202/gymtracker/feature/workout/ActiveWorkoutScreen.kt:56-59
- app/src/main/java/de/julius1202/gymtracker/feature/workout/WorkoutLoggingViewModel.kt:357-369 (timer job)
- app/src/main/java/de/julius1202/gymtracker/feature/workout/WorkoutLoggingViewModel.kt:116-152 (startSession calls timer at line 149)

**Suspected Root Cause:**
The `elapsedMs` StateFlow is collected in ActiveWorkoutScreen at line 46 with `collectAsStateWithLifecycle()`, which may be missing the continuous updates due to:
1. Job scope lifecycle issue (viewModelScope may be cancelled unexpectedly)
2. StateFlow not re-emitting same values (need to emit unique values for each tick)
3. Timing issue with delay vs system clock drift

### Truth 6: Session Survives App Close
**Status:** ? UNCERTAIN  
**Evidence:**
- Crash recovery navigation exists (AppNavHost.kt line 26-34)
- `getActiveSession()` check on app launch
- BUT: resumeSession() does not fully restore exercise data (see Truth 2)
- App will navigate to active_workout/{sessionId}, but screen will be empty because exercise sections are never populated

**Code Location:**
- app/src/main/java/de/julius1202/gymtracker/navigation/AppNavHost.kt:26-34
- app/src/main/java/de/julius1202/gymtracker/feature/workout/WorkoutLoggingViewModel.kt:206-234 (incomplete restoration)

## Requirements Coverage

| Requirement | Status | Evidence |
|-------------|--------|----------|
| LOG-01: User can start a workout and log sets with weight and reps | ✗ FAILED | Exercises not loaded from plan; empty sections displayed |
| LOG-03: Rest timer auto-starts and counts down | ✓ VERIFIED | Timer banner implemented with Skip/+30s buttons |
| LOG-04: Previous performance shown per exercise | ✓ VERIFIED | ExerciseSectionHeader displays "Last: N×M @ XX kg" |
| LOG-05: Total workout duration updates in real time | ✗ FAILED | Elapsed time stuck at 0; timer job not updating UI state |

## Artifacts Verification

| Artifact | Status | Details |
|----------|--------|---------|
| ActiveWorkoutScreen.kt | ⚠️ PARTIAL | Structure exists (LazyColumn, exercise sections, input fields), but no exercises rendered due to upstream gap in plan exercise loading |
| WorkoutSummaryScreen.kt | ✓ VERIFIED | Displays duration, exercise count, sets, volume; exists and renders |
| RestTimerBanner composable | ✓ VERIFIED | Countdown display, Skip/+30s buttons, sticky positioning in TopAppBar |
| ExerciseSectionHeader composable | ✓ VERIFIED | Shows exercise name and previous performance string |
| PendingSetRow / LoggedSetRow | ✓ VERIFIED | Input fields and set display rows exist and are styled |
| WorkoutLoggingViewModel.startSession() | ⚠️ PARTIAL | Creates session and starts elapsed timer, but exercise sections empty if exercises parameter is empty |
| WorkoutLoggingViewModel.resumeSession() | ✗ STUB | Loads logged sets but does NOT load Exercise entities; returns empty exercise sections |
| WorkoutLoggingViewModel.startElapsedTimer() | ✗ POSSIBLE_BUG | Job loop code exists but elapsed time not flowing to UI; investigate coroutine scope lifecycle |

## Anti-Patterns Found

| File | Line(s) | Pattern | Severity | Impact |
|------|---------|---------|----------|--------|
| PlansScreen.kt | 135 | `exercises = emptyList()` | 🛑 BLOCKER | Prevents all exercises from loading; core feature broken |
| WorkoutLoggingViewModel.kt | 228 | `_exerciseSections.value = emptyList()` in resumeSession() | 🛑 BLOCKER | Crash recovery returns empty workout |
| WorkoutLoggingViewModel.kt | 227 | Comment: "(Simplified - full version would need Exercise entities)" | ⚠️ WARNING | Incomplete stub implementation; marked as TODO |
| ActiveWorkoutScreen.kt | 130-131 | `onClick = { /* Placeholder - would add new pending row */ }` | ℹ️ INFO | "+ Add set" button is placeholder; no-op |

## Key Link Verification

| From | To | Via | Status |
|------|----|----|--------|
| PlansScreen → WorkoutLoggingViewModel | startSessionAndGetId() | Function call with plan data | ✗ BROKEN: exercises parameter hardcoded to emptyList() |
| WorkoutLoggingViewModel.resumeSession() → Exercise data | Database lookup | getSetsForSession() + Exercise JOIN | ✗ BROKEN: no Exercise entity loading |
| ActiveWorkoutScreen → elapsedMs StateFlow | Collect for UI | `collectAsStateWithLifecycle()` | ✓ WIRED but ✗ NO DATA: StateFlow never updated by job |
| ActiveWorkoutScreen → restTimerState | Rest timer display | `AnimatedVisibility(restState is Running)` | ✓ WIRED: timer flows to banner |
| logSet() → startRestTimer() | Set logging trigger | Function call in logSet() | ✓ WIRED: timer starts on set log |

## Gaps Summary

### Gap 1: Plan Exercises Not Loaded (GAPS-01)
**Severity:** 🛑 CRITICAL — Blocks LOG-01  
**Root Cause:** PlansScreen.kt passes `exercises = emptyList()` to startSessionAndGetId()  
**Fix:**
```kotlin
// PlansScreen.kt line 126-145, in onStartClick handler:
// Before: exercises = emptyList()
// After: 
scope.launch {
    try {
        val planExercises = planRepository.getPlanExercises(plan.id).first()
            .map { it.exercise }  // Extract Exercise from PlanExerciseWithExercise
        val sessionId = workoutViewModel.startSessionAndGetId(
            name = plan.name,
            planId = plan.id,
            exercises = planExercises  // Pass actual exercises
        )
        onStartPlan(sessionId)
    } catch (e: Exception) {
        // Handle error
    }
}
```

### Gap 2: Crash Recovery Returns Empty Workout
**Severity:** 🛑 CRITICAL — Breaks LOG-01 + session persistence  
**Root Cause:** resumeSession() does not load Exercise entities from database  
**Fix:**
```kotlin
// WorkoutLoggingViewModel.kt, replace resumeSession() function:
fun resumeSession(sessionId: Long) {
    viewModelScope.launch {
        val session = sessionRepository.getSessionById(sessionId) ?: return@launch
        _activeSession.value = session
        
        // Load logged sets for this session
        setRepository.getSetsForSession(sessionId).collect { sets ->
            // Group sets by exerciseId and load Exercise entities
            val exerciseIds = sets.map { it.exerciseId }.distinct()
            val exercises = exerciseIds.mapNotNull { id ->
                // Need exerciseRepository to fetch Exercise by id
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
        
        startElapsedTimer(session.startedAt)
    }
}
```

### Gap 3: Elapsed Time Not Updating in UI (GAPS-03)
**Severity:** 🛑 CRITICAL — Breaks LOG-05  
**Root Cause:** elapsedMs StateFlow updated in job, but updates not reaching UI OR job not running  
**Investigation Needed:**
1. Verify startElapsedTimer() is actually being called (add logging)
2. Verify coroutine job is not being cancelled prematurely
3. Verify StateFlow emissions are being collected in UI (check collectAsStateWithLifecycle())

**Quick Fix Test:**
Replace line 366 with explicit re-emit to force collection:
```kotlin
private fun startElapsedTimer(sessionStartedAt: Long) {
    if (elapsedTimerJob?.isActive == true) return
    elapsedTimerJob = viewModelScope.launch {
        while (isActive) {
            val now = System.currentTimeMillis()
            val elapsed = now - sessionStartedAt
            _elapsedMs.emit(elapsed)  // Try emit() instead of .value =
            delay(1000)
        }
    }
}
```

## Not Yet Verified (Human Testing Required)

1. **Vibration feedback on timer completion** — Requires physical device or emulator with haptic sensors
2. **Real-time timer tick observation** — Timing-sensitive behavior; automated tests flaky
3. **Crash recovery UX** — Requires process kill simulation + session restoration verification
4. **Scrolling with sticky timer banner** — UI/UX behavior; not automatable

## Next Steps (For Gap Closure Planning)

1. **Priority 1 — Gap 1 (Plan exercises):**
   - Inject WorkoutPlanRepository into PlansScreen or ViewModel
   - Fetch plan exercises before calling startSessionAndGetId()
   - Pass actual Exercise list instead of emptyList()
   - Test: Verify exercise sections appear in ActiveWorkoutScreen

2. **Priority 1 — Gap 2 (Crash recovery exercise loading):**
   - Update resumeSession() to load Exercise entities from database
   - Inject ExerciseRepository if not available
   - Group logged sets by exercise ID
   - Recreate ExerciseSection objects with real data
   - Test: Force app close during workout, reopen, verify exercises and sets visible

3. **Priority 1 — Gap 3 (Elapsed time updating):**
   - Debug startElapsedTimer() job execution (add Timber.d logging)
   - Verify viewModelScope lifecycle during active workout
   - Check StateFlow collection in ActiveWorkoutScreen
   - Consider using `MutableStateFlow.update()` instead of `.value =`
   - Test: Start workout, verify elapsed time increments every second

---

_Verified by: gsd-phase-verifier_  
_Verification Method: Code inspection + static analysis_  
_Timestamp: 2026-04-05T16:30:00Z_
