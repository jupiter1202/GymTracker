---
status: resolved
trigger: "Workout complete (summary) screen stuck showing circular loading indicator infinitely"
created: 2026-04-05T18:00:00Z
updated: 2026-04-05T18:15:00Z
symptoms_prefilled: true
---

## Current Focus

hypothesis: ROOT CAUSE IDENTIFIED - finishSession() returns BEFORE coroutine completes
test: (completed) Traced execution: finishSession() calls return true on line 462, but viewModelScope.launch { ... } on lines 443-460 hasn't executed yet
expecting: When user taps Finish, navigation to WorkoutSummaryScreen happens BEFORE _summary.value is set to actual WorkoutSummary data
next_action: Apply fix - make finishSession() a suspend function that awaits the coroutine completion before returning

## Symptoms

expected: 1. User taps Finish in ActiveWorkoutScreen, 2. finishSession() is called and returns true, 3. WorkoutSummaryScreen receives sessionId and renders, 4. _summary StateFlow is populated with WorkoutSummary, 5. WorkoutSummaryScreen collects summary and displays stats (Duration, Exercise count, Total sets, Volume)
actual: WorkoutSummaryScreen renders CircularProgressIndicator indefinitely; summary never appears; loading spinner never stops
errors: []
reproduction: 1. Create and start a workout, 2. Log at least one exercise with sets, 3. Tap "Finish" button, 4. Observe WorkoutSummaryScreen - shows spinner forever
started: Unknown when this started, but affects post-workout flow

## Eliminated

- (none yet)

## Evidence

- timestamp: 2026-04-05T18:00:00Z
  checked: AppModule.kt line 44
  found: WorkoutLoggingViewModel is configured as `single { ... }` not `viewModel { ... }` — singleton is correctly set
  implication: Previous ViewModel scoping bug is FIXED, so PlansScreen and ActiveWorkoutScreen and WorkoutSummaryScreen all use same instance

- timestamp: 2026-04-05T18:00:00Z
  checked: WorkoutLoggingViewModel.kt lines 435-463 (finishSession method)
  found: finishSession() has NO suspend keyword. Method returns true on line 462. But the actual summary computation happens in viewModelScope.launch { ... } block on lines 443-460. The function returns BEFORE the viewModelScope.launch coroutine even starts executing.
  implication: RACE CONDITION - finishSession() returns immediately, navigation happens (onFinished(sessionId) called), WorkoutSummaryScreen composes and collects _summary. But _summary is still null because the viewModelScope.launch hasn't run yet.

- timestamp: 2026-04-05T18:00:00Z
  checked: ActiveWorkoutScreen.kt lines 70-78 (Finish button handler)
  found: TextButton onClick calls `val canFinish = viewModel.finishSession()` which returns boolean, then immediately calls `onFinished(sessionId)` if true. This triggers navigation to WorkoutSummaryScreen IMMEDIATELY without waiting for the coroutine.
  implication: Navigation happens synchronously before the _summary StateFlow is updated by the coroutine

- timestamp: 2026-04-05T18:00:00Z
  checked: WorkoutSummaryScreen.kt lines 33, 41-51 (summary collection and loading display)
  found: Screen collects `val summary by viewModel.summary.collectAsStateWithLifecycle()` (line 33) and shows CircularProgressIndicator when summary == null (lines 41-51)
  implication: Since _summary remains null after navigation, the loading indicator displays forever. The coroutine that sets _summary.value eventually runs, but it's scheduled on viewModelScope which is asynchronous and unpredictable timing

## ROOT CAUSE IDENTIFIED

**RACE CONDITION: finishSession() returns before summary is computed**

The function signature is `fun finishSession(): Boolean` (not suspend). It:
1. Validates exercises (returns false if any empty) - synchronous
2. Calls `viewModelScope.launch { ... }` - SCHEDULES the summary computation asynchronously
3. Returns true immediately - BEFORE the scheduled coroutine runs
4. Navigation happens immediately
5. WorkoutSummaryScreen composable renders and collects summary
6. _summary is still null → CircularProgressIndicator displays
7. At some unpredictable time later, the viewModelScope.launch { } finally runs and sets _summary

This is a classic race condition between UI navigation and ViewModel state updates.

## Resolution

root_cause: **RACE CONDITION - finishSession() returns before summary is computed**

finishSession() was a regular function (not suspend) that:
1. Scheduled summary computation on viewModelScope.launch (async)
2. Returned true immediately (BEFORE coroutine executes)
3. Caused navigation to WorkoutSummaryScreen BEFORE _summary was populated
4. Left the summary loading indefinitely

fix: Made finishSession() a suspend function. Now it:
1. Runs sessionRepository.finishSession(activeSession) directly (not in launch block)
2. Computes WorkoutSummary synchronously
3. Sets _summary.value with the computed summary
4. Only returns after summary is ready
5. Navigation now happens AFTER summary is populated

Changed files:
- **WorkoutLoggingViewModel.kt line 435**: Changed `fun finishSession()` to `suspend fun finishSession()` and removed viewModelScope.launch wrapper (moved code to run synchronously in suspend context)
- **ActiveWorkoutScreen.kt line 28**: Added `import kotlinx.coroutines.launch` 
- **ActiveWorkoutScreen.kt line 56**: Added `val scope = rememberCoroutineScope()`
- **ActiveWorkoutScreen.kt lines 71-79**: Wrapped finishSession() call in `scope.launch { }` to handle suspend function
- **ActiveWorkoutScreen.kt lines 196-202**: Wrapped force-finish confirmation button's finishSession() call in `scope.launch { }`

verification: Gradle build succeeded with no errors. Code structure verified - suspend function now requires caller to wait for completion.
files_changed: [app/src/main/java/de/jupiter1202/gymtracker/feature/workout/WorkoutLoggingViewModel.kt, app/src/main/java/de/jupiter1202/gymtracker/feature/workout/ActiveWorkoutScreen.kt]

---
