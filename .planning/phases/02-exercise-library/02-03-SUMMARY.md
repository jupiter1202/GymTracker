---
phase: 02-exercise-library
plan: 03
subsystem: viewmodel
tags: [kotlin, viewmodel, stateflow, coroutines, koin, flow, flatMapLatest, combine]

# Dependency graph
requires:
  - phase: 02-exercise-library
    plan: 02
    provides: ExerciseRepository with searchExercises Flow, deleteExercise returning DeleteResult, Koin registration
  - phase: 01-foundation
    provides: Exercise entity, Koin AppModule with modern DSL, SettingsViewModel pattern
provides:
  - ExerciseViewModel with combine + flatMapLatest dual-StateFlow pattern for search + muscle group filter
  - groupedExercises StateFlow (non-empty only when searchQuery is blank)
  - deleteResult StateFlow exposing DeleteResult for UI snackbar/dialog
  - saveExercise method handling insert (isCustom=true) and update via repository
  - viewModel { ExerciseViewModel(get()) } registered in AppModule
affects:
  - 02-04-PLAN.md (ExercisesScreen collects from exercises, groupedExercises, deleteResult StateFlows)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - combine() + flatMapLatest dual-StateFlow pattern for reacting to two independent filter inputs
    - groupedExercises derived from exercises + searchQuery: blank query -> grouped map, non-blank -> emptyMap()
    - DeleteResult exposed via StateFlow for UI-layer consumption (snackbar/dialog dispatch)
    - @OptIn(ExperimentalCoroutinesApi::class) on ViewModel class for flatMapLatest usage

key-files:
  created:
    - app/src/main/java/de/jupiter1202/gymtracker/feature/exercises/ExerciseViewModel.kt
  modified:
    - app/src/main/java/de/jupiter1202/gymtracker/core/di/AppModule.kt

key-decisions:
  - "@OptIn(ExperimentalCoroutinesApi::class) applied at class level to suppress flatMapLatest warning cleanly — stable pattern, opt-in is boilerplate only"

patterns-established:
  - "Dual-StateFlow combine: combine(flow1, flow2) { a, b -> Pair(a,b) }.flatMapLatest { ... } drives a single reactive output from two independent inputs"
  - "Display mode switching: groupedExercises emits emptyMap() when search is active — composable switches between flat list and grouped list based on map emptiness"

requirements-completed: [EXER-03]

# Metrics
duration: 3min
completed: 2026-04-04
---

# Phase 2 Plan 03: ExerciseViewModel Summary

**ExerciseViewModel with combine + flatMapLatest dual-StateFlow pattern bridging ExerciseRepository to ExercisesScreen, exposing exercises, groupedExercises, and DeleteResult state**

## Performance

- **Duration:** 3 min
- **Started:** 2026-04-04T14:05:06Z
- **Completed:** 2026-04-04T14:08:09Z
- **Tasks:** 1 of 1 complete
- **Files modified:** 2

## Accomplishments

- ExerciseViewModel with `combine(_searchQuery, _selectedMuscleGroup).flatMapLatest` drives `exercises` StateFlow — both filters react simultaneously
- `groupedExercises` StateFlow non-empty only when searchQuery is blank, enabling flat vs. grouped display mode switch in composable
- `deleteResult` StateFlow surfaces `DeleteResult.Deleted` / `DeleteResult.Blocked(count)` for UI snackbar/dialog without coupling repo logic to screen
- `saveExercise` handles insert (new exercise with `isCustom=true`) and update via `existing.copy(...)` in single method
- `viewModel { ExerciseViewModel(get()) }` added to AppModule using modern Koin DSL; `assembleDebug` and `testDebugUnitTest` both pass

## Task Commits

Each task was committed atomically:

1. **Task 1: ExerciseViewModel and Koin registration** - `e1a5428` (feat)

## Files Created/Modified

- `app/src/main/java/de/jupiter1202/gymtracker/feature/exercises/ExerciseViewModel.kt` - ViewModel with dual-StateFlow combine, groupedExercises, deleteResult, and 6 public methods
- `app/src/main/java/de/jupiter1202/gymtracker/core/di/AppModule.kt` - Added ExerciseViewModel import and `viewModel { ExerciseViewModel(get()) }` registration

## Decisions Made

- `@OptIn(ExperimentalCoroutinesApi::class)` applied at class level to suppress the `flatMapLatest` opt-in warning cleanly. This is a stable API in practice; the annotation is boilerplate only.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added @OptIn(ExperimentalCoroutinesApi::class) for flatMapLatest**
- **Found during:** Task 1 (build verification)
- **Issue:** Kotlin compiler warned "this declaration needs opt-in" for `flatMapLatest` — build succeeded but warning indicated incomplete adoption of API contract
- **Fix:** Added `import kotlinx.coroutines.ExperimentalCoroutinesApi` and `@OptIn(ExperimentalCoroutinesApi::class)` annotation on class
- **Files modified:** ExerciseViewModel.kt
- **Verification:** assembleDebug produces no warnings; testDebugUnitTest passes
- **Committed in:** e1a5428 (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 missing critical)
**Impact on plan:** Annotation addition required for clean build output. No scope creep.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- ExerciseViewModel complete and Koin-registered; `koinViewModel<ExerciseViewModel>()` resolves in ExercisesScreen
- All 5 StateFlows (`exercises`, `groupedExercises`, `searchQuery`, `selectedMuscleGroup`, `deleteResult`) ready for collection
- Ready for: 02-04-PLAN.md (ExercisesScreen composable UI)

---
*Phase: 02-exercise-library*
*Completed: 2026-04-04*

## Self-Check: PASSED
