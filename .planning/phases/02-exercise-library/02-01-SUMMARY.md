---
phase: 02-exercise-library
plan: 01
subsystem: testing
tags: [room, kotlin, coroutines, junit4, androidtest, wave0-scaffold]

# Dependency graph
requires:
  - phase: 01-foundation
    provides: Exercise entity, GymTrackerDatabase skeleton, established test patterns (GymTrackerDatabaseTest)
provides:
  - ExerciseDaoTest instrumented scaffold with 6 behavior contracts (EXER-01/02/03)
  - ExerciseRepositoryTest unit scaffold with 3 delete-guard behavior contracts (EXER-02)
  - ExerciseDao placeholder interface (contract for Wave 2 @Dao implementation)
  - DeleteResult sealed class stub (contract for Wave 2 implementation)
  - ExerciseRepository placeholder stub (contract for Wave 2 implementation)
affects:
  - 02-02-PLAN.md (Wave 2 implementation must satisfy all test contracts defined here)

# Tech tracking
tech-stack:
  added:
    - kotlinx-coroutines-test 1.9.0 (testImplementation + androidTestImplementation)
  patterns:
    - Wave 0 scaffold pattern: local stubs in test files allow contracts to compile before implementations exist
    - Class-level @Ignore for wave scaffolds — tests skip at runtime, run after Wave 2 implementation
    - Individual @Ignore("seed db not yet present") for tests blocked on later assets
    - FakeExerciseDao hand-written inner class (no mockk) with configurable usageCount + deleteCalled flag
    - runBlocking for Flow.first() collection in instrumented tests
    - runTest { } for suspend function calls in unit tests

key-files:
  created:
    - app/src/androidTest/java/de/jupiter1202/gymtracker/feature/exercises/ExerciseDaoTest.kt
    - app/src/test/java/de/jupiter1202/gymtracker/feature/exercises/ExerciseRepositoryTest.kt
  modified:
    - gradle/libs.versions.toml (added kotlinx-coroutines-test version + library entry)
    - app/build.gradle.kts (added coroutines-test to testImplementation + androidTestImplementation)

key-decisions:
  - "Wave 0 scaffold pattern: placeholder types (ExerciseDao, DeleteResult, ExerciseRepository) defined in test files so tests compile cleanly before Wave 2 implementations exist; stubs removed when real classes are added in 02-02"
  - "kotlinx-coroutines-test added as explicit dependency: required for runTest in unit tests (not provided transitively at compile scope)"

patterns-established:
  - "Wave 0 scaffold: define behavior contracts as compiling, @Ignored tests before implementations exist"
  - "FakeExerciseDao pattern: hand-written fake with configurable fields instead of mockk for unit tests"

requirements-completed: [EXER-01, EXER-02, EXER-03]

# Metrics
duration: 4min
completed: 2026-04-04
---

# Phase 2 Plan 01: Exercise Library Test Scaffolds Summary

**Wave 0 skeleton tests for ExerciseDao and ExerciseRepository behavior contracts — 9 test stubs covering EXER-01/02/03 search, filter, insert, and delete-guard logic, compiled against Wave 2 placeholder interfaces**

## Performance

- **Duration:** 4 min
- **Started:** 2026-04-04T13:43:27Z
- **Completed:** 2026-04-04T13:47:42Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments

- ExerciseDaoTest instrumented scaffold with 6 test methods covering: empty-query all-results, name search, muscle group filter, combined filter, insert visibility, countUsagesInSessions zero-guard, and seeded-exercise stub (individually @Ignored)
- ExerciseRepositoryTest unit scaffold with 3 test methods covering: Deleted result on 0 usages, Blocked(sessionCount) result on >0 usages, and guard enforcement that dao.delete() is NOT called when blocked
- Added kotlinx-coroutines-test 1.9.0 dependency to enable runTest in unit tests; Wave 0 stubs compile cleanly with ./gradlew assembleDebug and ./gradlew testDebugUnitTest both passing

## Task Commits

Each task was committed atomically:

1. **Task 1: ExerciseDaoTest scaffold (instrumented)** - `4716d2a` (test)
2. **Task 2: ExerciseRepositoryTest scaffold (unit)** - `00e2e53` (test)

**Plan metadata:** (pending)

_Note: TDD tasks may have multiple commits (test → feat → refactor)_

## Files Created/Modified

- `app/src/androidTest/java/de/jupiter1202/gymtracker/feature/exercises/ExerciseDaoTest.kt` - Instrumented Wave 0 test scaffold for ExerciseDao; class-level @Ignore; contains ExerciseDao placeholder interface
- `app/src/test/java/de/jupiter1202/gymtracker/feature/exercises/ExerciseRepositoryTest.kt` - Unit Wave 0 test scaffold for ExerciseRepository; class-level @Ignore; contains ExerciseDao, DeleteResult, and ExerciseRepository placeholder stubs
- `gradle/libs.versions.toml` - Added coroutinesTest = "1.9.0" version and kotlinx-coroutines-test library entry
- `app/build.gradle.kts` - Added kotlinx.coroutines.test to testImplementation and androidTestImplementation

## Decisions Made

- **Wave 0 scaffold pattern with local stubs:** Since ExerciseDao, DeleteResult, and ExerciseRepository don't exist yet (Wave 2), placeholder types were defined directly in the test files. This lets the tests compile cleanly against the contracts they define. In Wave 2, these stubs are deleted and replaced by the real @Dao and classes in the feature package.
- **kotlinx-coroutines-test added explicitly:** The plan calls for runTest in the unit test. While room-ktx provides coroutines transitively, kotlinx-coroutines-test is not available at test compile scope without explicit declaration.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Wave 0 stubs added to test files for compilability**
- **Found during:** Task 1 and Task 2 (both)
- **Issue:** The plan said `testDebugUnitTest` should pass, but referencing non-existent ExerciseDao, ExerciseRepository, and DeleteResult caused compile errors in both test files
- **Fix:** Added minimal placeholder interfaces/classes directly in the test files (ExerciseDao interface, DeleteResult sealed class, ExerciseRepository stub). For ExerciseDaoTest, used `TODO("db.exerciseDao() added in Wave 2")` as the DAO assignment since db.exerciseDao() is an instrumented-only concern that doesn't affect unit test compilation
- **Files modified:** Both test files
- **Verification:** `./gradlew testDebugUnitTest` BUILD SUCCESSFUL; `./gradlew assembleDebug` BUILD SUCCESSFUL
- **Committed in:** `4716d2a` (Task 1), `00e2e53` (Task 2)

**2. [Rule 3 - Blocking] kotlinx-coroutines-test dependency added**
- **Found during:** Task 2 (ExerciseRepositoryTest)
- **Issue:** Plan specifies `runTest { }` for all suspend calls but `kotlinx-coroutines-test` was not in libs.versions.toml or build.gradle.kts
- **Fix:** Added `coroutinesTest = "1.9.0"` to versions catalog and `kotlinx.coroutines.test` to both testImplementation and androidTestImplementation
- **Files modified:** `gradle/libs.versions.toml`, `app/build.gradle.kts`
- **Verification:** `./gradlew testDebugUnitTest` BUILD SUCCESSFUL; runTest imports resolve cleanly
- **Committed in:** `4716d2a` (Task 1 commit, added ahead of time for Task 2)

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Both auto-fixes required to meet the plan's stated success criterion of `testDebugUnitTest` passing. No scope creep — stubs are minimal contracts that Wave 2 will replace.

## Issues Encountered

None beyond the auto-fixed blocking issues above.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness

- Wave 0 contracts locked in: ExerciseDao (search/filter/insert/countUsagesInSessions) and ExerciseRepository (deleteExercise with DeleteResult) behaviors are defined as code
- 02-02 (Wave 2 implementation) can proceed: implement the real ExerciseDao @Dao, ExerciseRepository, and DeleteResult, then remove the placeholder stubs from the test files
- connectedDebugAndroidTest (ExerciseDaoTest) will compile and run after GymTrackerDatabase.exerciseDao() is added in 02-02

---
*Phase: 02-exercise-library*
*Completed: 2026-04-04*

## Self-Check: PASSED
