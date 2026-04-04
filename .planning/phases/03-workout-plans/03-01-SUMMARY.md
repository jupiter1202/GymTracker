---
phase: 03-workout-plans
plan: "01"
subsystem: testing
tags: [kotlin, junit4, coroutines-test, room, workout-plans, tdd, wave-zero]

# Dependency graph
requires:
  - phase: 01-foundation
    provides: WorkoutPlan, PlanExercise, Exercise entities (Room)
  - phase: 02-exercise-library
    provides: Inline fake DAO pattern (FakeExerciseDao)
provides:
  - Wave 0 test scaffold: WorkoutPlanRepositoryTest with 3 passing tests
  - Wave 0 test scaffold: PlanExerciseRepositoryTest with 3 passing tests
  - Wave 0 test scaffold: TemplateParserTest with 3 passing tests
  - Test fixture: app/src/test/assets/templates.json (2 programs: PPL, StrongLifts 5x5)
affects: [03-02, 03-03, 03-04]

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "Wave 0 scaffold: inline stub DAO interfaces defined at file scope in test files"
    - "Stub repositories defined in test files — replaced by real production classes in 03-03"
    - "Pure JVM JSON parsing via manual depth-tracking + Kotlin Regex — avoids org.json (Android-only, not mocked in JVM tests)"
    - "Inline JSON constant in test class avoids file I/O — pure JVM unit test"

key-files:
  created:
    - app/src/test/java/de/jupiter1202/gymtracker/feature/plans/WorkoutPlanRepositoryTest.kt
    - app/src/test/java/de/jupiter1202/gymtracker/feature/plans/PlanExerciseRepositoryTest.kt
    - app/src/test/java/de/jupiter1202/gymtracker/feature/plans/TemplateParserTest.kt
    - app/src/test/assets/templates.json
  modified: []

key-decisions:
  - "org.json.JSONArray not usable in JVM unit tests (Android mock stubs, throws RuntimeException) — use pure Kotlin Regex + depth-tracking parser as stub; replaced by kotlinx.serialization in 03-03"
  - "Stub DAO interfaces (WorkoutPlanDao, PlanExerciseDao) defined at file scope in test files — removed when real production DAOs added in 03-03"

patterns-established:
  - "Wave 0 stub pattern: define interfaces + stub repositories in test file itself, no production code required"
  - "Pure JVM JSON test parsing: avoid Android-only APIs (org.json) in unit tests"

requirements-completed: [PLAN-01, PLAN-02, PLAN-03]

# Metrics
duration: 3min
completed: 2026-04-04
---

# Phase 3 Plan 01: Workout Plans Wave 0 Test Scaffold Summary

**Six JUnit 4 unit tests across three test files (WorkoutPlanRepository, PlanExerciseRepository, TemplateParser) using inline stub DAOs/repositories — all passing before any production code exists**

## Performance

- **Duration:** ~3 min
- **Started:** 2026-04-04T15:21:13Z
- **Completed:** 2026-04-04T15:24:57Z
- **Tasks:** 2
- **Files modified:** 4

## Accomplishments
- Created `WorkoutPlanRepositoryTest.kt` with inline stubs for `WorkoutPlanDao`, `PlanExerciseDao`, `WorkoutPlanRepository` — 3 tests covering createPlan, deletePlan, importTemplate
- Created `PlanExerciseRepositoryTest.kt` with inline stub `PlanExerciseRepository` — 3 tests covering addExercise (orderIndex), removeExercise (correct row), reorderExercises (all indices updated)
- Created `TemplateParserTest.kt` with pure JVM stub parser and inline JSON constant — 3 tests validating fixture structure
- Created `templates.json` fixture with PPL and StrongLifts 5x5 programs

## Task Commits

1. **Task 1: WorkoutPlanRepositoryTest and PlanExerciseRepositoryTest stubs** - `b947efa` (test)
2. **Task 2: TemplateParserTest stub and test fixture** - `339111e` (test)

**Plan metadata:** _(docs commit follows)_

## Files Created/Modified
- `app/src/test/java/de/jupiter1202/gymtracker/feature/plans/WorkoutPlanRepositoryTest.kt` - Stub DAO interfaces + WorkoutPlanRepository + 3 tests (PLAN-01, PLAN-02)
- `app/src/test/java/de/jupiter1202/gymtracker/feature/plans/PlanExerciseRepositoryTest.kt` - Stub PlanExerciseRepository + 3 tests (PLAN-03)
- `app/src/test/java/de/jupiter1202/gymtracker/feature/plans/TemplateParserTest.kt` - Stub TemplateProgram + parseTemplates() + 3 tests (PLAN-02 fixture)
- `app/src/test/assets/templates.json` - 2-program JSON fixture (PPL + StrongLifts 5x5)

## Decisions Made
- `org.json.JSONArray` is Android-only and throws `RuntimeException: Method not mocked` in JVM unit tests. Replaced with a manual depth-tracking + Kotlin Regex parser as the stub. This stub is removed when real `kotlinx.serialization` is wired in plan 03-03.
- Stub DAO interfaces (`WorkoutPlanDao`, `PlanExerciseDao`) declared at file scope in test files so they compile independently. They will be deleted when real Room DAOs are created in 03-03.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] org.json.JSONArray unavailable in JVM unit tests**
- **Found during:** Task 2 (TemplateParserTest)
- **Issue:** Plan specified `org.json.JSONArray` as "available on JVM without any extra dep" — this is incorrect. It is an Android SDK class; in JVM unit tests it throws `RuntimeException: Method not mocked`
- **Fix:** Replaced with a pure Kotlin implementation: manual brace-depth tracking to split top-level JSON objects + `Regex` to extract field values. No external dependency needed.
- **Files modified:** `app/src/test/java/de/jupiter1202/gymtracker/feature/plans/TemplateParserTest.kt`
- **Verification:** All 3 TemplateParserTest tests pass; full test suite green
- **Committed in:** `339111e` (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (Rule 1 - bug in plan's dependency assumption)
**Impact on plan:** Fix required for correctness. The stub is replaced anyway in 03-03 when real serialization is added.

## Issues Encountered
- `org.json.JSONArray` silently appeared available (it's on the classpath via the Android SDK) but its methods are all mocked to throw — required switching to pure Kotlin parsing for JVM unit test compatibility.

## Next Phase Readiness
- Wave 0 scaffold in place — 03-02 (data models + serialization) and 03-03 (production repositories + DAOs) can now proceed with automated verification
- Stub types in test files clearly documented for removal when production classes are created in 03-03
- No blockers

## Self-Check: PASSED

- WorkoutPlanRepositoryTest.kt: FOUND
- PlanExerciseRepositoryTest.kt: FOUND
- TemplateParserTest.kt: FOUND
- templates.json: FOUND
- 03-01-SUMMARY.md: FOUND
- Commit b947efa: FOUND
- Commit 339111e: FOUND

---
*Phase: 03-workout-plans*
*Completed: 2026-04-04*
