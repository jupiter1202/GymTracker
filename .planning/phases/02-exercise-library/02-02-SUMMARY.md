---
phase: 02-exercise-library
plan: 02
subsystem: database
tags: [room, kotlin, ksp, dao, repository, coroutines, flow, koin, seed-database]

# Dependency graph
requires:
  - phase: 02-exercise-library
    plan: 01
    provides: ExerciseDaoTest and ExerciseRepositoryTest Wave 0 scaffolds (behavior contracts)
  - phase: 01-foundation
    provides: Exercise entity, GymTrackerDatabase skeleton, Room/KSP setup, AppModule/Koin wiring
provides:
  - ExerciseDao @Dao with searchExercises Flow query, insert, update, delete, countUsagesInSessions
  - ExerciseRepository with deleteExercise delete-guard returning DeleteResult sealed class
  - GymTrackerDatabase.exerciseDao() abstract function (Wave 2 wiring)
  - AppModule wired with createFromAsset("gymtracker_seed.db"), exerciseDao singleton, ExerciseRepository singleton
  - PENDING: gymtracker_seed.db asset file (100-150 seeded exercises) — requires human step (Task 2 checkpoint)
affects:
  - 02-03-PLAN.md (ExercisesViewModel depends on ExerciseRepository)

# Tech tracking
tech-stack:
  added: []
  patterns:
    - Room @Dao with Flow for reactive search (named parameters :query, :muscleGroup)
    - DeleteResult sealed class pattern for encoding business rule results at type level
    - createFromAsset() for pre-seeded Room databases on first install
    - Repository delete-guard via countUsagesInSessions() before dao.delete()

key-files:
  created:
    - app/src/main/java/de/jupiter1202/gymtracker/core/database/dao/ExerciseDao.kt
    - app/src/main/java/de/jupiter1202/gymtracker/feature/exercises/ExerciseRepository.kt
  modified:
    - app/src/main/java/de/jupiter1202/gymtracker/core/database/GymTrackerDatabase.kt
    - app/src/main/java/de/jupiter1202/gymtracker/core/di/AppModule.kt
    - app/src/androidTest/java/de/jupiter1202/gymtracker/feature/exercises/ExerciseDaoTest.kt
    - app/src/test/java/de/jupiter1202/gymtracker/feature/exercises/ExerciseRepositoryTest.kt

key-decisions:
  - "ExerciseDao uses named parameters (:query, :muscleGroup) in @Query SQL — required by Room KSP, positional parameters are rejected"
  - "DeleteResult sealed class defined at file-level (not nested) in ExerciseRepository.kt: accessible to ViewModel without importing inner class"
  - "delete-guard implemented in application code via countUsagesInSessions() — WorkoutSet FK uses SET_NULL so Room won't throw; guard must be app-level"
  - "createFromAsset added to databaseBuilder in AppModule before .build() — Room validates identity_hash on first open, seed DB must be Room-generated"

patterns-established:
  - "DeleteResult sealed class: encode delete outcomes as typed return values (Deleted | Blocked(count)) instead of exceptions or boolean flags"
  - "Repository delete-guard: check countUsagesInSessions() before dao.delete() to enforce business rules not expressible as DB constraints"

requirements-completed: [EXER-01, EXER-02]

# Metrics
duration: 10min
completed: 2026-04-04
---

# Phase 2 Plan 02: Exercise Library Data Layer Summary

**ExerciseDao @Dao with Flow search, ExerciseRepository with delete-guard returning DeleteResult sealed class, Room database wired with createFromAsset() — pending seed database asset (human checkpoint)**

## Performance

- **Duration:** 10 min
- **Started:** 2026-04-04T14:00:00Z
- **Completed:** 2026-04-04T14:10:00Z
- **Tasks:** 1 of 2 complete (Task 2 is a blocking human-action checkpoint)
- **Files modified:** 6

## Accomplishments

- ExerciseDao @Dao compiled by Room KSP with searchExercises Flow query (LIKE + optional muscle group filter, ORDER BY muscle group ASC, name ASC), insert with REPLACE, update, delete, countUsagesInSessions
- ExerciseRepository with deleteExercise delete-guard: checks countUsagesInSessions() first, returns DeleteResult.Blocked(count) if count > 0, otherwise calls dao.delete() and returns DeleteResult.Deleted
- GymTrackerDatabase updated with abstract fun exerciseDao(): ExerciseDao; AppModule wired with createFromAsset("gymtracker_seed.db"), exerciseDao() singleton, and ExerciseRepository singleton
- Wave 0 placeholder stubs removed from ExerciseDaoTest and ExerciseRepositoryTest; class-level @Ignore removed; ExerciseDaoTest now uses db.exerciseDao(); all 3 ExerciseRepositoryTest unit tests passing

## Task Commits

Each task was committed atomically:

1. **Task 1: ExerciseDao, ExerciseRepository, GymTrackerDatabase, AppModule** - `68e656a` (feat)
2. **Task 2: Generate and populate seed database** - PENDING (human-action checkpoint)

**Plan metadata:** (pending — will be added after Task 2 and final commit)

## Files Created/Modified

- `app/src/main/java/de/jupiter1202/gymtracker/core/database/dao/ExerciseDao.kt` - Room @Dao with Flow searchExercises and CRUD ops
- `app/src/main/java/de/jupiter1202/gymtracker/feature/exercises/ExerciseRepository.kt` - Repository with DeleteResult sealed class and delete-guard logic
- `app/src/main/java/de/jupiter1202/gymtracker/core/database/GymTrackerDatabase.kt` - Added abstract fun exerciseDao(): ExerciseDao
- `app/src/main/java/de/jupiter1202/gymtracker/core/di/AppModule.kt` - Added createFromAsset, exerciseDao singleton, ExerciseRepository singleton
- `app/src/androidTest/java/de/jupiter1202/gymtracker/feature/exercises/ExerciseDaoTest.kt` - Removed Wave 0 stub, class-level @Ignore removed, uses real db.exerciseDao()
- `app/src/test/java/de/jupiter1202/gymtracker/feature/exercises/ExerciseRepositoryTest.kt` - Removed Wave 0 stubs, class-level @Ignore removed, uses real ExerciseDao/ExerciseRepository imports

## Decisions Made

- Named parameters (:query, :muscleGroup) used in @Query — Room KSP rejects positional parameters.
- DeleteResult defined at file scope (not nested class) so ViewModel can import it cleanly.
- Delete guard is app-level (not a DB constraint) because WorkoutSet FK uses SET_NULL action — Room won't throw on delete, so the business rule must live in application code.
- createFromAsset() added before .build() in AppModule — Room validates schema identity_hash on first open, meaning the seed .db file must be created by running Room itself (not hand-crafted).

## Deviations from Plan

None - plan executed exactly as written for Task 1.

## Issues Encountered

None.

## User Setup Required

Task 2 is a blocking human-action checkpoint. The user must:
1. Temporarily remove .createFromAsset() from AppModule, launch app on emulator to create the Room DB
2. Pull gymtracker.db from device via adb or Android Studio Database Inspector
3. Populate with 100-150 exercises using DB Browser for SQLite
4. Place the file at app/src/main/assets/gymtracker_seed.db
5. Re-enable .createFromAsset() in AppModule
6. Uninstall and reinstall to verify seed loads on fresh install

See Task 2 `<how-to-verify>` section in 02-02-PLAN.md for full step-by-step instructions.

## Next Phase Readiness

- Task 1 complete: ExerciseDao, ExerciseRepository, GymTrackerDatabase, AppModule all compile and unit tests pass
- Blocked on seed database asset (Task 2 human-action checkpoint) — app will crash on launch until gymtracker_seed.db is placed in assets/
- After seed db is in place: 02-03-PLAN.md (ExercisesViewModel + UI) can proceed

---
*Phase: 02-exercise-library*
*Completed: 2026-04-04 (partial — awaiting Task 2 human checkpoint)*

## Self-Check: PASSED
