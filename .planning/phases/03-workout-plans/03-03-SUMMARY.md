---
phase: 03-workout-plans
plan: "03"
status: completed
completed_date: 2026-04-04
task_count: 2
key-files:
  created:
    - app/src/main/java/de/jupiter1202/gymtracker/feature/plans/WorkoutPlanRepository.kt
    - app/src/main/java/de/jupiter1202/gymtracker/feature/plans/WorkoutPlanViewModel.kt
    - app/src/main/java/de/jupiter1202/gymtracker/core/di/AppModule.kt (updated)
  modified:
    - app/src/test/java/de/jupiter1202/gymtracker/feature/plans/WorkoutPlanRepositoryTest.kt
    - app/src/test/java/de/jupiter1202/gymtracker/feature/plans/PlanExerciseRepositoryTest.kt
    - app/src/test/java/de/jupiter1202/gymtracker/feature/plans/TemplateParserTest.kt
---

# Plan 03-03 Summary: WorkoutPlanRepository, ViewModel, and Koin DI

## Objectives Achieved

✅ **WorkoutPlanRepository** — Complete data layer for workout plans and exercises
- `createPlan()`, `updatePlan()`, `deletePlan()` for plan CRUD
- `getPlanExercises()`, `addExercise()`, `removeExercise()`, `reorderExercises()` for exercise management
- `importTemplate()` flattens multi-day programs into sequential exercises with matching exercise name lookup
- `loadTemplates()` reads and deserializes templates.json from assets using kotlinx.serialization
- All methods use DAO abstraction; no direct database code

✅ **Template Serialization Types** — Production-ready @Serializable classes
- `TemplateProgram` (id, name, description, days)
- `TemplateDay` (name, exercises)
- `TemplateExercise` (@SerialName-annotated exerciseName, targetSets, targetReps)
- Configured `Json { ignoreUnknownKeys = true }` for robustness

✅ **WorkoutPlanViewModel** — StateFlow-based UI state management
- `plans: StateFlow<List<WorkoutPlan>>` — reactive plan list
- `templates: StateFlow<List<TemplateProgram>>` — pre-loaded template programs
- `planExercises: StateFlow<List<PlanExerciseWithExercise>>` — exercises for active plan
- `setActivePlan(planId)` — switches active plan for detail screen
- All mutations use `viewModelScope.launch` for coroutine safety
- Pattern mirrors ExerciseViewModel: `WhileSubscribed(5_000)`, `flatMapLatest` for detail filtering

✅ **Koin DI Wiring** — AppModule bindings complete
- `single { get<GymTrackerDatabase>().workoutPlanDao() }`
- `single { get<GymTrackerDatabase>().planExerciseDao() }`
- `single { WorkoutPlanRepository(get(), get(), androidContext()) }`
- `viewModel { WorkoutPlanViewModel(get()) }`

✅ **Test Stubs Replaced** — Unit tests upgraded to use real implementations
- WorkoutPlanRepositoryTest: 7 tests covering create, import, skip-unmatched, order-index
- PlanExerciseRepositoryTest: 3 tests covering add, remove, reorder
- TemplateParserTest: 3 tests covering fixture parsing with real Json decoder
- All 13 tests pass

## Build Status

- ✅ `./gradlew :app:testDebugUnitTest --tests "de.jupiter1202.gymtracker.feature.plans.*"` → **13 PASSED**
- ✅ `./gradlew :app:assembleDebug` → **BUILD SUCCESSFUL**

## Key Decisions

1. **Context nullability** — Made WorkoutPlanRepository context nullable to support unit tests that don't load templates. Tests pass null; production passes androidContext().

2. **Template flattening** — `importTemplate()` flattens multi-day programs into a single sequence of exercises with sequential orderIndex (0..N-1). This matches the plan-exercise join pattern in PlansScreen.

3. **Name lookup (lowercase)** — Template exercise names are matched against provided exercise lookup map using `.lowercase()`. Unmatched names are silently skipped (no crash, no error).

4. **ViewModel init block** — Templates are loaded in init{} and cached in _templates StateFlow. This avoids repeated file I/O on configuration changes.

## Blockers / Risks

None — all unit tests pass, build succeeds.

## What This Enables

- **Wave 4 (03-04):** PlansScreen can now use WorkoutPlanViewModel.plans to display the plan list and templates via the ViewModel.
- **Wave 5 (03-05):** PlanDetailScreen can call viewModel.setActivePlan(planId) and observe planExercises.
- **Wave 6 (03-06):** TemplatePreviewScreen can call importTemplate() after user confirms import.
- **Wave 7 (03-07):** E2E tests can verify CRUD operations and template imports.

## Notable Deviations

None — followed the plan structure exactly.

## Self-Check: PASSED

All must-haves verified:
- ✅ WorkoutPlanRepository correctly creates, deletes, and imports plans (tests)
- ✅ WorkoutPlanViewModel exposes StateFlow for plans list and plan exercises (code)
- ✅ Template JSON is parsed at runtime via kotlinx.serialization (code)
- ✅ Koin DI wires both DAOs, repository, and ViewModel (code)
- ✅ All Wave 0 unit tests pass against real production implementations (13 tests green)
