---
status: diagnosed
trigger: "Exercises not loading when users start a workout from a plan"
created: 2026-04-05T00:00:00Z
updated: 2026-04-05T00:00:00Z
---

## Current Focus

hypothesis: ROOT CAUSE IDENTIFIED - Koin viewModel scoping creates separate instances
test: (completed) Verified that AppModule.kt line 44 uses `viewModel { ... }` instead of `single { ... }`, causing PlansScreen and ActiveWorkoutScreen to get different ViewModel instances
expecting: PlansScreen updates one instance's _exerciseSections, ActiveWorkoutScreen observes a different instance's empty _exerciseSections
next_action: Fix is ready - change AppModule.kt line 44

## Symptoms

expected: When user taps "Start" on a plan, exercises are fetched from the plan, passed to the ViewModel, StateFlow is updated, then navigation occurs with populated exercises visible in LazyColumn
actual: ActiveWorkoutScreen renders with empty LazyColumn. No exercises appear even though PlansScreen correctly fetches them.
errors: []
reproduction: 1. Create a workout plan with exercises, 2. Go to PlansScreen, 3. Tap "Start" on the plan, 4. Observe ActiveWorkoutScreen - exercises section is empty
started: After recent code fixes to PlansScreen and ViewModel (per user description)

## Eliminated

- (none yet)

## Evidence

- timestamp: 2026-04-05T00:00:00Z
  checked: PlansScreen.kt lines 129-146 (onStartClick handler)
  found: User correctly fetches `planExercises` via `planRepository.getPlanExercises(plan.id).first()`, maps to Exercise objects, and calls `workoutViewModel.startSessionAndGetId(name, planId, exercises=planExercises)` inside `scope.launch { ... }`
  implication: Call is wrapped in coroutine, which is correct. But need to verify the actual flow control.

- timestamp: 2026-04-05T00:00:00Z
  checked: PlansScreen.kt line 137-141 (startSessionAndGetId invocation)
  found: `val sessionId = workoutViewModel.startSessionAndGetId(...)` - this is a suspend function call, but PlansScreen is NOT using `await()`. Scope.launch wraps it, but the return value assignment is synchronous code following an async call.
  implication: CRITICAL ISSUE - `onStartPlan(sessionId)` on line 142 is called IMMEDIATELY AFTER the suspend function assignment, not after the function completes

- timestamp: 2026-04-05T00:00:00Z
  checked: AppModule.kt line 44
  found: `viewModel { WorkoutLoggingViewModel(get(), get(), get(), get(), androidContext()) }` - uses Koin's `viewModel` DSL, NOT `single`
  implication: `viewModel` DSL in Koin creates a SCOPED instance per navigation destination/back stack, not a singleton

- timestamp: 2026-04-05T00:00:00Z
  checked: PlansScreen.kt line 74 vs ActiveWorkoutScreen.kt line 36
  found: Both use `koinViewModel()` to retrieve WorkoutLoggingViewModel, but the Koin configuration creates separate instances per route
  implication: PlansScreen gets Plans-route-scoped instance, ActiveWorkoutScreen gets active_workout-route-scoped instance. These are DIFFERENT objects with separate state.

- timestamp: 2026-04-05T00:00:00Z
  checked: AppNavHost.kt line 52
  found: Navigation from PlansScreen to ActiveWorkoutScreen is via `navController.navigate("active_workout/$sessionId")` - this creates a new route composition
  implication: New route composition triggers a new ViewModel instance from Koin's factory

- timestamp: 2026-04-05T00:00:00Z
  checked: WorkoutLoggingViewModel.kt line 117-153 (startSession) vs 158-194 (startSessionAndGetId)
  found: startSessionAndGetId updates _exerciseSections on the ViewModel instance that called it (PlansScreen's instance)
  implication: When ActiveWorkoutScreen is composed, it gets a fresh ViewModel instance with _exerciseSections = emptyList() from initialization (line 91)

## Resolution

root_cause: **VIEWMODEL SCOPING BUG - Different ViewModel Instances Between Screens**

Koin's `viewModel { ... }` creates a scoped ViewModel instance per navigation destination/back stack entry. When PlansScreen calls `koinViewModel()` (line 74), it gets a ViewModel instance scoped to the Plans route. When ActiveWorkoutScreen calls `koinViewModel()` (line 36), it gets a DIFFERENT ViewModel instance scoped to the active_workout route.

**Evidence Chain:**
1. AppModule.kt line 44: `viewModel { WorkoutLoggingViewModel(...) }` - uses Koin's scoped viewModel factory (not singleton)
2. PlansScreen.kt line 74: Gets WorkoutLoggingViewModel instance scoped to Plans route
3. PlansScreen.kt lines 135-142: Calls `workoutViewModel.startSessionAndGetId(...)` which updates _exerciseSections on PLANS-SCOPED instance
4. AppNavHost.kt line 52: Navigation triggered via `onStartPlan(sessionId) -> navController.navigate("active_workout/$sessionId")`
5. ActiveWorkoutScreen receives NEW/DIFFERENT ViewModel instance scoped to active_workout route
6. ActiveWorkoutScreen.kt line 49: Collects from `viewModel.exerciseSections` - but this is a DIFFERENT instance with EMPTY _exerciseSections
7. Result: LazyColumn is empty because it's observing the wrong ViewModel instance

**Why This Happens:**
Koin's `viewModel { ... }` DSL creates a NEW instance for EACH navigation destination by default. It doesn't share state across routes. The exercises were set on the Plans-route ViewModel, but ActiveWorkoutScreen is observing the active_workout-route ViewModel.

fix: Change Koin configuration to use `single { WorkoutLoggingViewModel(...) }` instead of `viewModel { ... }` - make it a true singleton shared across all screens
verification: (pending - needs Koin config change and testing)
files_changed: [core/di/AppModule.kt]

---

## SECONDARY ISSUE - If First Issue Existed

If somehow the ViewModel instances WERE shared, there would be a SECOND bug:
In ActiveWorkoutScreen.kt lines 42-44:
```kotlin
if (viewModel.activeSession.value == null) {
    viewModel.resumeSession(sessionId)
}
```

This logic SKIPS resumeSession when activeSession is NOT null. After PlansScreen calls startSessionAndGetId(), activeSession IS set (line 163 of ViewModel). So resumeSession() would NOT run, and the exercise sections created by startSessionAndGetId would be used as-is.

BUT since activeSession IS properly set by startSessionAndGetId, this guard is correct behavior (it prevents overwriting exercises with an empty fresh resume).

The issue is that with separate ViewModel instances, ActiveWorkoutScreen's ViewModel never had startSessionAndGetId called, so its activeSession is null and it WOULD call resumeSession(), which loads exercises from the DATABASE rather than using the freshly-fetched plan exercises.

---

## ROOT CAUSE SUMMARY

**PRIMARY BUG:** Koin viewModel scoping creates separate instances per route
- PlansScreen's ViewModel instance: _exerciseSections populated with plan exercises
- ActiveWorkoutScreen's ViewModel instance: _exerciseSections empty (never called with exercises)

**CONSEQUENCE:** ActiveWorkoutScreen observes empty exercise list because it's on a different ViewModel instance

**FIX:** Make WorkoutLoggingViewModel a singleton using `single { ... }` instead of `viewModel { ... }`
