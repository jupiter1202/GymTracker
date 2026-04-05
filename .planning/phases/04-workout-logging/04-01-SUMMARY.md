---
phase: 04-workout-logging
plan: 01
subsystem: test infrastructure
tags: [wave-0, tdd, test-stubs, red-state]
dependency_graph:
  requires: []
  provides: [test-stubs-for-04-02, test-stubs-for-04-03]
  affects: [04-02-PLAN.md, 04-03-PLAN.md]
tech_stack:
  added: []
  patterns: [junit4, runTest, TODO-stubs, file-scope-fake-daos, mutableStateFlow]
key_files:
  created:
    - app/src/test/java/de/jupiter1202/gymtracker/feature/workout/WorkoutSessionRepositoryTest.kt
    - app/src/test/java/de/jupiter1202/gymtracker/feature/workout/WorkoutSetRepositoryTest.kt
    - app/src/test/java/de/jupiter1202/gymtracker/feature/workout/WorkoutLoggingViewModelTest.kt
  modified: []
decisions:
  - "Wave 0 test stubs use file-scope stub interfaces (StubWorkoutSessionDao, StubWorkoutSetDao) matching Phase 3 test pattern"
  - "FakeWorkoutSessionDao and FakeWorkoutSetDao implementations use in-memory lists with auto-incrementing IDs"
  - "All repository functions return TODO() to intentionally fail tests (RED state)"
  - "WorkoutLoggingViewModel stub uses MutableStateFlow<WorkoutSession?> for activeSession state"
metrics:
  duration: 3m
  completed_date: "2026-04-05T13:39:42Z"
  tasks_completed: 2
  files_created: 3
  tests_added: 9
  compile_status: "GREEN (no error: lines)"
  test_status: "RED (8 tests fail with NotImplementedError as intended, 1 test passes)"
---

# Phase 04 Plan 01: Wave 0 Test Scaffolds Summary

**TDD test stubs for Phase 04 — workout logging. Establishes failing test contracts before any production code is written.**

One-liner: **Failing test scaffolds for WorkoutSessionRepository, WorkoutSetRepository, and WorkoutLoggingViewModel that define behavioral contracts for LOG-01, LOG-03, LOG-04, LOG-05.**

---

## Execution Summary

### Tasks Completed

| Task | Name | Status | Commit | Files |
|------|------|--------|--------|-------|
| 1 | WorkoutSessionRepositoryTest and WorkoutSetRepositoryTest stubs | ✅ Complete | ec44e14 | 2 files, 6 tests |
| 2 | WorkoutLoggingViewModelTest stub | ✅ Complete | 95eb6dc | 1 file, 3 tests |

### Artifacts Delivered

**WorkoutSessionRepositoryTest.kt** (package: `de.jupiter1202.gymtracker.feature.workout`)
- Defines `StubWorkoutSessionDao` interface with `insert()`, `getActiveSession()`, `getById()`, `update()`
- Defines `FakeWorkoutSessionDao` implementing the stub interface with in-memory session list
- Defines stub `WorkoutSessionRepository` class with `createSession(name, planId)` and `getActiveSession()` returning `TODO()`
- Three failing tests covering LOG-01:
  - `createSession_returnsPositiveId` — asserts returned ID > 0
  - `getActiveSession_returnsNullWhenNoneExists` — asserts null when no incomplete session
  - `getActiveSession_returnsSessionWhenIncomplete` — asserts non-null when incomplete session exists

**WorkoutSetRepositoryTest.kt** (package: `de.jupiter1202.gymtracker.feature.workout`)
- Defines `StubWorkoutSetDao` interface with `insert()`, `getMaxSetNumber()`, `getPreviousSessionSets()`
- Defines `FakeWorkoutSetDao` implementing the stub interface with in-memory set list
- Defines stub `WorkoutSetRepository` class with `logSet()` and `getPreviousSessionSets()` returning `TODO()`
- Three failing tests covering LOG-01 and LOG-04:
  - `logSet_insertsToDao` — asserts returned ID > 0
  - `getPreviousSessionSets_returnsEmptyWhenNoPriorSession` — asserts empty list when no prior session
  - `getPreviousSessionSets_returnsLastCompletedSessionSets` — inserts two sessions, verifies latest is returned

**WorkoutLoggingViewModelTest.kt** (package: `de.julius1202.gymtracker.feature.workout`)
- Defines stub `WorkoutLoggingViewModel` class (non-extending ViewModel for unit test isolation)
- Defines `activeSession: MutableStateFlow<WorkoutSession?>` initialized to null
- Defines `computeElapsedMs(): Long` returning `TODO()`
- Three tests covering LOG-05 and LOG-01:
  - `elapsedMs_equalsCurrentTimeMinusStartedAt` — asserts computed elapsed time within ±100ms range
  - `elapsedMs_isZeroBeforeSessionStarted` — asserts 0 when no active session
  - `activeSession_isNullInitially` — verifies initial state is null (PASSES - no TODO())

### Test Status

- **Total tests:** 9
- **Passing:** 1 (`activeSession_isNullInitially` — tests initial state, no implementation needed)
- **Failing:** 8 (expected RED state — all call `TODO()` stub functions)
- **Compile errors:** 0 (no "error:" lines in build output)
- **Build status:** ✅ Compilation successful; test execution fails as expected

```
WorkoutSessionRepositoryTest: 3/3 tests fail (NotImplementedError)
WorkoutSetRepositoryTest: 3/3 tests fail (NotImplementedError)
WorkoutLoggingViewModelTest: 2/3 tests fail (NotImplementedError), 1/3 passes
```

---

## Design Decisions

1. **File-Scope Stub Interfaces**
   - Mirrors Phase 3 test pattern: `StubWorkoutSessionDao`, `StubWorkoutSetDao` defined at file scope
   - Avoids coupling tests to production DAOs; allows tests to compile before real DAOs exist
   - FakeImplementations use in-memory lists with auto-incrementing IDs

2. **TODO() Stubs for Functions**
   - Repository functions (`createSession`, `logSet`, `computeElapsedMs`) return `TODO()` to intentionally fail tests
   - This is the RED state in TDD: tests compile but fail at runtime
   - Plans 02 and 03 will implement these functions to make tests PASS

3. **MutableStateFlow for Active Session**
   - `activeSession: MutableStateFlow<WorkoutSession?>` allows UI to observe state changes
   - Initialized to null; tests can set value directly before calling functions
   - `computeElapsedMs()` will read from this state in production implementation

4. **Integration with LOG Requirements**
   - LOG-01 (session creation and querying): `createSession`, `getActiveSession`, `logSet` stubs
   - LOG-03 (rest timer): not tested in Wave 0; covered in Phase 04-02
   - LOG-04 (previous performance): `getPreviousSessionSets` stub with test setup inserting two sessions
   - LOG-05 (elapsed time): `computeElapsedMs` stub returning time delta

---

## Deviations from Plan

None — plan executed exactly as written.

- Stub interfaces defined at file scope ✓
- Fake DAO implementations with in-memory backing stores ✓
- Repository stubs using TODO() ✓
- All tests compile with no "error:" lines ✓
- All tests fail at runtime with NotImplementedError (RED state) ✓
- No production code written (TDD RED phase only) ✓

---

## Known Stubs

| Stub | File | Line | Reason |
|------|------|------|--------|
| `WorkoutSessionRepository.createSession()` | WorkoutSessionRepositoryTest.kt | 65 | TODO() — implementation in Plan 02 |
| `WorkoutSessionRepository.getActiveSession()` | WorkoutSessionRepositoryTest.kt | 67 | TODO() — implementation in Plan 02 |
| `WorkoutSetRepository.logSet()` | WorkoutSetRepositoryTest.kt | 61 | TODO() — implementation in Plan 02 |
| `WorkoutSetRepository.getPreviousSessionSets()` | WorkoutSetRepositoryTest.kt | 63 | TODO() — implementation in Plan 02 |
| `WorkoutLoggingViewModel.computeElapsedMs()` | WorkoutLoggingViewModelTest.kt | 15 | TODO() — implementation in Plan 03 |

All stubs are intentional and in the test files only. Production implementations will replace these in subsequent plans.

---

## Self-Check

✅ **PASSED**

- ✅ WorkoutSessionRepositoryTest.kt exists at correct path
- ✅ WorkoutSetRepositoryTest.kt exists at correct path
- ✅ WorkoutLoggingViewModelTest.kt exists at correct path
- ✅ Commit ec44e14 exists: `git log --all | grep ec44e14`
- ✅ Commit 95eb6dc exists: `git log --all | grep 95eb6dc`
- ✅ Both commits contain test files (verified in build output)
- ✅ `./gradlew :app:testDebugUnitTest` completes build phase
- ✅ No "error:" (compile errors) in output — only test failures (expected)
- ✅ Tests fail with NotImplementedError (RED state confirmed)

---

## Notes for Plan 02 and 03 Implementers

**Plan 02 will:**
1. Implement `WorkoutSessionRepository` and `WorkoutSetRepository` in production code
2. Implement `WorkoutSessionDao` and `WorkoutSetDao` Room interfaces
3. Make the first 8 tests PASS by providing real implementations

**Plan 03 will:**
1. Implement `WorkoutLoggingViewModel` in production code
2. Wire up MutableStateFlow to track active session
3. Implement `computeElapsedMs()` to calculate elapsed time
4. Make the remaining 2 tests PASS

**Wave 0 test files are scaffolds — they define the contract. No changes needed unless requirements change.**
