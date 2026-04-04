---
phase: 03-workout-plans
plan: "02"
subsystem: database
tags: [room, kotlin, dao, android, serialization, reorderable]

# Dependency graph
requires:
  - phase: 03-01
    provides: WorkoutPlan, PlanExercise, Exercise entities; stub DAO interfaces in test files
provides:
  - WorkoutPlanDao: getAllPlans(), insert(), update(), delete() with Flow
  - PlanExerciseDao: getExercisesForPlan(@Transaction+@Relation), getMaxOrderIndex(), insert(), update(), delete(), countExercisesInPlan()
  - PlanExerciseWithExercise: @Embedded + @Relation data class for JOIN results
  - GymTrackerDatabase registered workoutPlanDao() and planExerciseDao()
  - templates.json with 4 complete program definitions (PPL, StrongLifts 5x5, nSuns 5/3/1, GZCLP)
  - core/constants/MuscleGroups.kt shared constants extracted from ExercisesScreen
affects:
  - 03-03 (WorkoutPlanRepository, TemplateImporter, WorkoutPlanViewModel will consume these DAOs)

# Tech tracking
tech-stack:
  added:
    - sh.calvin.reorderable:reorderable 3.0.0
    - org.jetbrains.kotlinx:kotlinx-serialization-json 1.7.3
    - org.jetbrains.kotlin.plugin.serialization plugin
  patterns:
    - Room @Transaction + @Relation pattern for JOIN queries (PlanExerciseWithExercise)
    - Shared constants in core/constants/ package (vs. file-level declarations in feature screens)

key-files:
  created:
    - app/src/main/java/de/jupiter1202/gymtracker/core/database/dao/WorkoutPlanDao.kt
    - app/src/main/java/de/jupiter1202/gymtracker/core/database/dao/PlanExerciseDao.kt
    - app/src/main/java/de/jupiter1202/gymtracker/core/constants/MuscleGroups.kt
    - app/src/main/assets/templates.json
  modified:
    - gradle/libs.versions.toml
    - app/build.gradle.kts
    - app/src/main/java/de/jupiter1202/gymtracker/core/database/GymTrackerDatabase.kt
    - app/src/main/java/de/jupiter1202/gymtracker/feature/exercises/ExercisesScreen.kt

key-decisions:
  - "PlanExerciseWithExercise defined at file scope in PlanExerciseDao.kt — accessible to repository and ViewModel without inner-class import"
  - "kotlin-serialization plugin version tied to kotlin = 2.2.10 via version.ref — ensures plugin and compiler match"
  - "templates.json uses exercise names matching seed DB title case convention — unmatched names skipped with warning in repository (plan 03-03)"

patterns-established:
  - "Room JOIN pattern: @Transaction + @Query returning @Embedded+@Relation data class"
  - "Shared constants extracted to core/constants/ — prevents duplication across feature screens"

requirements-completed: [PLAN-01, PLAN-02, PLAN-03]

# Metrics
duration: 3min
completed: 2026-04-04
---

# Phase 3 Plan 02: Data Layer Foundation Summary

**Room DAOs for workout_plans and plan_exercises with Flow/JOIN, templates.json with 4 programs (PPL, 5x5, nSuns, GZCLP), and reorderable + kotlinx.serialization dependencies added**

## Performance

- **Duration:** 3 min
- **Started:** 2026-04-04T15:27:18Z
- **Completed:** 2026-04-04T15:31:03Z
- **Tasks:** 2
- **Files modified:** 8

## Accomplishments
- Created WorkoutPlanDao and PlanExerciseDao as valid Room interfaces consumed by GymTrackerDatabase
- PlanExerciseWithExercise @Relation JOIN data class enables exercises to be fetched with their Exercise details in a single query
- Authored templates.json with 4 complete training programs ready for repository import in 03-03
- Extracted MUSCLE_GROUPS/EQUIPMENT_TYPES to core/constants/MuscleGroups.kt, eliminating duplication

## Task Commits

Each task was committed atomically:

1. **Task 1: Add Gradle deps and extract MuscleGroups constant** - `0bb21d7` (feat)
2. **Task 2: WorkoutPlanDao, PlanExerciseDao, DB update, templates.json** - `4a6d25d` (feat)

**Plan metadata:** (docs commit — see below)

## Files Created/Modified
- `app/src/main/java/de/jupiter1202/gymtracker/core/database/dao/WorkoutPlanDao.kt` - CRUD + Flow for workout_plans
- `app/src/main/java/de/jupiter1202/gymtracker/core/database/dao/PlanExerciseDao.kt` - JOIN, reorder, CRUD for plan_exercises
- `app/src/main/java/de/jupiter1202/gymtracker/core/constants/MuscleGroups.kt` - Shared MUSCLE_GROUPS and EQUIPMENT_TYPES
- `app/src/main/assets/templates.json` - 4 full program definitions
- `app/src/main/java/de/jupiter1202/gymtracker/core/database/GymTrackerDatabase.kt` - Added workoutPlanDao() and planExerciseDao()
- `app/src/main/java/de/jupiter1202/gymtracker/feature/exercises/ExercisesScreen.kt` - Import from core/constants
- `gradle/libs.versions.toml` - Added reorderable, kotlinx-serialization-json, kotlin-serialization plugin
- `app/build.gradle.kts` - Applied kotlin.serialization plugin, added implementation deps

## Decisions Made
- PlanExerciseWithExercise defined at file scope in PlanExerciseDao.kt so the repository and ViewModel can import it cleanly without inner-class syntax
- kotlin-serialization plugin version bound to `kotlin = 2.2.10` via `version.ref = "kotlin"` — keeps plugin and compiler versions in lockstep
- templates.json exercise names use seed DB title case convention; unmatched names will be skipped with a warning logged (handled in 03-03 repository)

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- KSP emitted a warning about missing index on `exercise_id` in `plan_exercises` (covers FK but not indexed). This is a pre-existing schema concern from 03-01 entities; deferred to `deferred-items.md` as out-of-scope for this plan.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Both DAOs are ready for consumption by WorkoutPlanRepository and TemplateImporter in 03-03
- GymTrackerDatabase now exposes workoutPlanDao() and planExerciseDao() abstract funs
- templates.json asset is in place; repository only needs to parse and import it
- kotlinx.serialization plugin and library are configured; repository can use @Serializable models immediately

---
*Phase: 03-workout-plans*
*Completed: 2026-04-04*
