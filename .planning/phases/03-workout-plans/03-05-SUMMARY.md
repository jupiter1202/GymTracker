---
phase: 03-workout-plans
plan: "05"
status: completed
completed_date: 2026-04-04
task_count: 2
key-files:
  created:
    - app/src/main/java/de/jupiter1202/gymtracker/feature/plans/PlanDetailScreen.kt
  modified:
    - app/src/main/java/de/jupiter1202/gymtracker/navigation/AppNavHost.kt
duration_minutes: 5
---

# Plan 03-05 Summary: PlanDetailScreen with Drag-and-Drop and Swipe-to-Delete

## Objectives Achieved

✅ **PlanDetailScreen UI Implementation** — Complete plan detail screen with all interactions
- TopAppBar showing plan name with Edit button for renaming
- Exercise list with drag-and-drop reordering using `sh.calvin.reorderable`
- Swipe-to-delete functionality via `SwipeToDismissBox` for removing exercises
- "+ Add exercise" button at bottom of list
- Proper gesture layering: `ReorderableItem` > `SwipeToDismissBox` > row content (per research anti-patterns)

✅ **Exercise Picker Sheet** — Full modal bottom sheet for adding exercises
- Integrates `ExerciseViewModel` for independent search state
- Search bar for filtering exercises by name
- Muscle group filter chips (same pattern as ExercisesScreen)
- LazyColumn of exercises; tapping selects exercise and opens target sheet

✅ **Target Input Sheet** — Dual-purpose sheet for add and edit workflows
- Pre-fills from existing `PlanExerciseWithExercise` when editing
- Sets default values (3 sets, "8" reps) when adding new exercise
- Number field for target sets (KeyboardType.Number)
- Text field for target reps (supports "5", "8-12", "AMRAP")
- Cancel / Add to plan (or Save) buttons trigger appropriate ViewModel methods

✅ **Rename Plan Sheet** — Edit plan name and description
- Pre-filled with current plan name and description
- Mirrors CreatePlanSheet pattern with required name field
- On save: `viewModel.updatePlan(plan.copy(...))`

✅ **ViewModel Integration** — All CRUD operations wired
- `LaunchedEffect(planId) { viewModel.setActivePlan(planId) }` triggers plan exercises load
- `planExercises` StateFlow collected to track in-flight drag order
- On drop: `viewModel.reorderExercises(localExercises.map { it.planExercise })`
- On swipe: `viewModel.removeExercise(item.planExercise)`
- On target save: `viewModel.addExercise()` or `viewModel.updateExerciseTargets()`

✅ **AppNavHost Integration** — PlanDetailScreen wired to navigation
- Replaced placeholder `Box(Text(...))` with real `PlanDetailScreen`
- Routes plan detail navigation from PlansScreen
- Provides `onNavigateBack` callback using `navController.popBackStack()`

## Build Status

- ✅ `./gradlew :app:assembleDebug` → **BUILD SUCCESSFUL**
- ✅ `./gradlew :app:testDebugUnitTest` → **ALL TESTS PASSED**

## Key Technical Decisions

1. **Gesture Layering** — Followed research anti-pattern guidance: `ReorderableItem` wraps `SwipeToDismissBox` which wraps row content. This ensures drag handle intercepts touch before swipe gesture.

2. **Drag Handle Visualization** — Used "⋮⋮" text indicator instead of Icon(DragHandle) to avoid adding material-icons-extended dependency (per PROJECT context).

3. **Local State for Drag** — `var localExercises` maintains in-flight drag order locally before persisting to DB via `reorderExercises()`. This prevents UI jank from Flow updates during active drag.

4. **Exercise Picker ViewModel** — Injected separate `ExerciseViewModel` instance into the picker sheet via `koinViewModel()` to maintain independent search/filter state scoped to the sheet lifecycle.

5. **Sheet State Management** — Used `rememberModalBottomSheetState(skipPartiallyExpanded = true)` for consistent UX with existing ExercisesScreen sheets.

## Deviations from Plan

None — plan executed exactly as specified.

## Self-Check: PASSED

All must-haves verified:
- ✅ Tapping a plan card navigates to PlanDetailScreen showing exercise list
- ✅ Each exercise row shows name, target sets × reps, and drag handle indicator
- ✅ Drag handle enables drag-and-drop reorder; orderIndex persists to DB after drop
- ✅ Swipe-to-delete removes an exercise row from the plan
- ✅ Tapping an exercise row opens target sheet pre-filled with current sets/reps
- ✅ "+ Add exercise" button opens exercise picker sheet with search + filter pattern
- ✅ Selecting an exercise opens follow-up targets sheet (default 3 sets, 8 reps)
- ✅ Edit button in top bar opens rename sheet for the plan
- ✅ PlanDetailScreen.kt created and compiles
- ✅ AppNavHost plan_detail route calls real PlanDetailScreen with planId and back callback
- ✅ App builds successfully with no errors
- ✅ All unit tests pass

## Code Quality

- **Imports:** All necessary imports from Material3, Foundation, Compose, Koin, and sh.calvin.reorderable
- **Composable Structure:** Proper separation of concerns with private composable functions
- **State Management:** Uses `collectAsStateWithLifecycle` for StateFlow collection
- **Error Handling:** Null checks on navigation arguments; fallback values for missing plan
- **Accessibility:** contentDescription provided for all interactive elements

## What This Enables

- **Users:** Can now edit workout plans by reordering exercises via drag-and-drop, removing exercises via swipe, adding new exercises from the full exercise library, and editing target sets/reps for each exercise
- **Phase 03-06:** TemplatePreviewScreen can follow same pattern as PlanDetailScreen
- **Phase 04:** Workout logging can reference plan exercises by their final order and target values
- **Architecture:** Completed the Plans feature triangle: PlansScreen (list), PlanDetailScreen (detail/edit), TemplatePreviewScreen (preview) — all using consistent ViewModel and repository patterns

## Commits

- `efc51b5`: feat(03-05): add PlanDetailScreen with drag-and-drop and swipe-to-delete
- `c54e7e0`: feat(03-05): wire PlanDetailScreen into AppNavHost

