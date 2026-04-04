---
phase: 02-exercise-library
plan: 04
subsystem: ui
tags: [kotlin, compose, viewmodel, stateflow, bottom-sheet, lazy-column, filter-chips, koin]

# Dependency graph
requires:
  - phase: 02-exercise-library
    plan: 03
    provides: ExerciseViewModel with exercises, groupedExercises, deleteResult StateFlows and 6 public methods
  - phase: 02-exercise-library
    plan: 02
    provides: ExerciseRepository, Exercise entity, DeleteResult sealed class
  - phase: 01-foundation
    provides: Koin AppModule, SettingsViewModel pattern, app navigation structure
provides:
  - ExercisesScreen: full exercise library UI replacing placeholder composable
  - Search bar filtering to flat list, chip row filtering by muscle group, grouped sticky-header list
  - ExerciseFormSheet ModalBottomSheet for create and edit with ExposedDropdownMenuBox fields
  - Long-press context menu for custom exercises (Edit / Delete)
  - AlertDialog for delete-blocked exercises (session count message)
affects:
  - Phase 03+ (nav graph — ExercisesScreen is already wired via tab navigation from Phase 01)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - ExposedDropdownMenuBox with menuAnchor(MenuAnchorType.PrimaryNotEditable) for M3 dropdown fields
    - ModalBottomSheet + rememberModalBottomSheetState(skipPartiallyExpanded=true) for create/edit forms
    - combinedClickable(onLongClick) for long-press context menu on list rows
    - stickyHeader in LazyColumn requires @OptIn(ExperimentalFoundationApi::class)
    - collectAsStateWithLifecycle() for all StateFlow consumption in composables
    - Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.ime)) for keyboard clearance in bottom sheets

key-files:
  created: []
  modified:
    - app/src/main/java/de/jupiter1202/gymtracker/feature/exercises/ExercisesScreen.kt

key-decisions:
  - "menuAnchor() deprecated — replaced with menuAnchor(MenuAnchorType.PrimaryNotEditable) for M3 ExposedDropdownMenuBox; avoids deprecation warning on build"
  - "ExerciseFormSheet hoisted into same file as ExercisesScreen (not a separate file) — single composable file pattern consistent with project structure"

# Metrics
duration: 4min
completed: 2026-04-04
---

# Phase 2 Plan 04: ExercisesScreen UI Summary

**Full exercise library UI: search bar, muscle-group filter chips, sticky-header grouped list, FAB bottom sheet for create/edit, and long-press context menu for custom exercises**

## Performance

- **Duration:** 4 min
- **Started:** 2026-04-04T14:10:28Z
- **Completed:** 2026-04-04T14:14:32Z
- **Tasks:** 2 of 2 complete
- **Files modified:** 1

## Accomplishments

- ExercisesScreen replaces "Coming soon" placeholder with complete exercise library UI
- Search bar (OutlinedTextField) with trailing clear icon resets via `viewModel::clearSearch`
- Filter chip LazyRow: "All" chip + 12 muscle group chips toggle `selectedMuscleGroup` StateFlow
- LazyColumn switches mode: flat `items(exercises)` when search active, grouped `stickyHeader + items` otherwise
- ExerciseRow: `combinedClickable` long-press opens DropdownMenu for custom exercises (Edit / Delete)
- SuggestionChip "Custom" badge rendered next to custom exercise names
- AlertDialog shown when `deleteResult` is `DeleteResult.Blocked` with exact session count
- FAB opens `ModalBottomSheet` (create mode); long-press Edit sets `exerciseToEdit` (edit mode)
- ExerciseFormSheet: drag handle, title ("New Exercise" / "Edit Exercise"), name field with error, two ExposedDropdownMenuBox fields, Cancel/Save row, IME spacer
- `menuAnchor(MenuAnchorType.PrimaryNotEditable)` used — no deprecation warnings on build

## Task Commits

Each task was committed atomically:

1. **Task 1: Search bar, filter chips, grouped list** — `aa61732` (feat)
2. **Task 2: ExerciseFormSheet bottom sheet** — `435d97d` (feat)

## Files Created/Modified

- `app/src/main/java/de/jupiter1202/gymtracker/feature/exercises/ExercisesScreen.kt` — 470-line file containing ExercisesScreen, ExerciseList, ExerciseRow, MuscleGroupHeader, ExerciseFormSheet, ExposedDropdownField composables; MUSCLE_GROUPS and EQUIPMENT_TYPES constants

## Decisions Made

- `menuAnchor()` (no-arg) deprecated in the project's M3 version — replaced with `menuAnchor(MenuAnchorType.PrimaryNotEditable)`. Correct anchor type for read-only dropdown text fields.
- ExerciseFormSheet kept in the same file as ExercisesScreen. No reason to split — all composables are private and tightly coupled to the screen.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Updated deprecated menuAnchor() to menuAnchor(MenuAnchorType.PrimaryNotEditable)**
- **Found during:** Task 2 (build verification)
- **Issue:** Compiler warning: `'fun Modifier.menuAnchor(): Modifier' is deprecated. Use overload that takes MenuAnchorType and enabled parameters.` Plan specified using `Modifier.menuAnchor()` but the M3 library requires the typed overload.
- **Fix:** Added `import androidx.compose.material3.MenuAnchorType` and replaced `.menuAnchor()` with `.menuAnchor(MenuAnchorType.PrimaryNotEditable)` in ExposedDropdownField.
- **Files modified:** ExercisesScreen.kt
- **Verification:** assembleDebug produces no warnings; testDebugUnitTest passes
- **Committed in:** 435d97d (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 deprecated API replacement)
**Impact on plan:** Single-line change. No scope creep.

## Issues Encountered

None.

## User Setup Required

None — no external service configuration required.

## Next Phase Readiness

- ExercisesScreen fully functional: seeded exercises visible grouped by muscle group
- Search, filter chips, FAB, long-press edit/delete all wired to ExerciseViewModel
- Phase 2 Exercise Library feature complete — all 4 ROADMAP requirements met
- Ready for Phase 3 (Workout Plans) or smoke test of the Exercises tab

---
*Phase: 02-exercise-library*
*Completed: 2026-04-04*

## Self-Check: PASSED
