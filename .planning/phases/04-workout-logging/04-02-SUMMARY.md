---
phase: 04-workout-logging
plan: 02
subsystem: data-layer
tags: [room-dao, repository, di-registration, settings-extension]
dependency_graph:
  requires: [04-01]
  provides: [data-persistence-for-04-03, data-persistence-for-04-04, data-persistence-for-04-05]
  affects: [04-03-PLAN.md, 04-04-PLAN.md, 04-05-PLAN.md]
tech_stack:
  added: []
  patterns: [room-dao, suspend-functions, flow-queries, correlated-subquery, datastore-preferences, koin-dsl]
key_files:
  created:
    - app/src/main/java/de/jupiter1202/gymtracker/feature/workout/WorkoutSessionDao.kt
    - app/src/main/java/de/jupiter1202/gymtracker/feature/workout/WorkoutSetDao.kt
    - app/src/main/java/de/jupiter1202/gymtracker/feature/workout/WorkoutSessionRepository.kt
    - app/src/main/java/de/jupiter1202/gymtracker/feature/workout/WorkoutSetRepository.kt
  modified:
    - app/src/main/java/de/jupiter1202/gymtracker/core/database/GymTrackerDatabase.kt
    - app/src/main/java/de/jupiter1202/gymtracker/feature/settings/SettingsRepository.kt
    - app/src/main/java/de/jupiter1202/gymtracker/core/di/AppModule.kt
decisions:
  - "WorkoutSessionDao uses suspension-based suspend fun for insert/query/update — no reactive overhead"
  - "WorkoutSetDao getPreviousSessionSets uses correlated subquery to find most-recent completed session, then fetch all sets — no in-memory filtering"
  - "SettingsRepository REST_TIMER_SECONDS defaults to 90 seconds, range validated 10-600 at repository boundary per threat model T-04-02-02"
  - "AppModule registers both DAOs as DAO instances and both repositories as repositories — eliminates intermediate `get()` in repositories"
metrics:
  duration: 2m
  completed_date: "2026-04-05T13:41:47Z"
  tasks_completed: 2
  files_created: 4
  files_modified: 3
  compile_status: "GREEN (BUILD SUCCESSFUL)"
  test_status: "N/A (data layer only — integration tests in 04-03)"
---

# Phase 04 Plan 02: Data Layer for Workout Logging Summary

**WorkoutSessionDao, WorkoutSetDao, repositories, and SettingsRepository extension. Establishes durable persistence for active sessions, set logging, and rest timer configuration.**

One-liner: **Room DAOs for sessions and sets, two repositories wrapping them, extended SettingsRepository with rest timer defaults, and AppModule registration enable Plan 03 (ViewModel) to persist all workout data.**

---

## Execution Summary

### Tasks Completed

| Task | Name | Status | Commit | Files |
|------|------|--------|--------|-------|
| 1 | WorkoutSessionDao, WorkoutSetDao, GymTrackerDatabase update | ✅ Complete | 4fc8710 | 3 files, 66 insertions |
| 2 | Repositories, SettingsRepository update, AppModule registration | ✅ Complete | aa7e88a | 4 files, 81 insertions |

### Artifacts Delivered

**WorkoutSessionDao.kt** (package: `de.jupiter1202.gymtracker.feature.workout`)
- `suspend fun insert(session: WorkoutSession): Long` — persists new session to Room
- `suspend fun getActiveSession(): WorkoutSession?` — queries for incomplete session (isCompleted=0)
- `suspend fun getById(id: Long): WorkoutSession?` — fetches specific session by ID
- `suspend fun update(session: WorkoutSession)` — updates existing session record

**WorkoutSetDao.kt** (package: `de.jupiter1202.gymtracker.feature.workout`)
- `suspend fun insert(set: WorkoutSet): Long` — persists new set to Room
- `fun getSetsForSession(sessionId: Long): Flow<List<WorkoutSet>>` — reactive query for all sets in a session
- `fun getSetsForExercise(sessionId: Long, exerciseId: Long): Flow<List<WorkoutSet>>` — reactive query for sets for a specific exercise in a session
- `suspend fun getMaxSetNumber(sessionId: Long, exerciseId: Long): Int?` — finds highest set number (for auto-increment)
- `suspend fun delete(set: WorkoutSet)` — deletes a set
- `suspend fun getPreviousSessionSets(exerciseId: Long): List<WorkoutSet>` — correlated subquery fetching all sets from the most recent completed session for an exercise

**WorkoutSessionRepository.kt** (package: `de.jupiter1202.gymtracker.feature.workout`)
- `suspend fun createSession(name: String, planId: Long?): Long` — creates new WorkoutSession with current timestamp
- `suspend fun getActiveSession(): WorkoutSession?` — delegates to DAO
- `suspend fun getSessionById(id: Long): WorkoutSession?` — delegates to DAO
- `suspend fun finishSession(session: WorkoutSession)` — marks session complete with finishedAt timestamp

**WorkoutSetRepository.kt** (package: `de.jupiter1202.gymtracker.feature.workout`)
- `suspend fun logSet(sessionId: Long, exerciseId: Long, weightKg: Double, reps: Int): Long` — creates new WorkoutSet with auto-incremented setNumber, writes to DB
- `fun getSetsForSession(sessionId: Long)` — delegates to DAO, returns Flow
- `fun getSetsForExercise(sessionId: Long, exerciseId: Long)` — delegates to DAO, returns Flow
- `suspend fun getPreviousSessionSets(exerciseId: Long): List<WorkoutSet>` — delegates to DAO (uses correlated subquery)
- `suspend fun deleteSet(set: WorkoutSet)` — delegates to DAO

**SettingsRepository.kt** (updated)
- Added `val REST_TIMER_SECONDS = intPreferencesKey("rest_timer_seconds")` to PreferenceKeys
- Added `val restTimerSeconds: Flow<Int>` — reads from DataStore, defaults to 90 seconds
- Added `suspend fun setRestTimerSeconds(seconds: Int)` — validates range 10-600, writes to DataStore

**GymTrackerDatabase.kt** (updated)
- Added `abstract fun workoutSessionDao(): WorkoutSessionDao` — Room generates implementation
- Added `abstract fun workoutSetDao(): WorkoutSetDao` — Room generates implementation
- Database version remains `version = 1` (no schema migration)

**AppModule.kt** (updated)
- Added DAO registrations: `single { get<GymTrackerDatabase>().workoutSessionDao() }`
- Added DAO registrations: `single { get<GymTrackerDatabase>().workoutSetDao() }`
- Added repository registrations: `single { WorkoutSessionRepository(get()) }`
- Added repository registrations: `single { WorkoutSetRepository(get()) }`

### Build Status

- **Compilation:** ✅ `./gradlew :app:kspDebugKotlin` → BUILD SUCCESSFUL (KSP generates Room DAO implementations)
- **Full build:** ✅ `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL (all 36 tasks completed)
- **Errors:** 0 (no "error:" lines in output)
- **Warnings:** 1 non-blocking (exercise_id FK not indexed — acceptable per existing schema design)

---

## Design Decisions

1. **WorkoutSetDao.getPreviousSessionSets uses Correlated Subquery**
   - Query finds max session ID where isCompleted=1 and that session has sets for the given exerciseId
   - Then fetches all WorkoutSet rows for that session, ordered by set_number
   - Compiles correctly under Room KSP (named parameters `:exerciseId` required)
   - Alternative (two queries) avoided to keep DAO focused on data access

2. **SettingsRepository REST_TIMER_SECONDS Default 90**
   - Default aligns with Phase 3 research recommendation (D-12 in CONTEXT.md)
   - Range validation 10-600 seconds enforced at repository boundary (threat model T-04-02-02)
   - Stored in DataStore alongside existing WEIGHT_UNIT preference

3. **Repository Functions are Suspend vs Flow**
   - `insert`, `update`, `delete`, `getMaxSetNumber`: suspend functions (one-shot operations)
   - `getSetsForSession`, `getSetsForExercise`: return Flow (UI observes list changes)
   - Mirrors existing codebase pattern (ExerciseRepository, WorkoutPlanRepository)

4. **No Intermediate DAO Singletons**
   - AppModule registers DAOs directly as singletons (`get<GymTrackerDatabase>().workoutSessionDao()`)
   - Repositories inject DAO interface (not wrapped in another layer)
   - Simplifies DI chain: AppModule → Repositories (no use-case layer)

5. **Koin DSL Uses `single { }` for All DAOs and Repositories**
   - Consistent with existing registrations (ExerciseDao, ExerciseRepository, etc.)
   - `viewModel {}` reserved for ViewModel registrations (will be used in Plan 03)

---

## Verification Results

✅ All acceptance criteria met:

- ✅ WorkoutSessionDao.kt exists at correct path
- ✅ WorkoutSetDao.kt exists at correct path
- ✅ GymTrackerDatabase.kt contains `abstract fun workoutSessionDao(): WorkoutSessionDao`
- ✅ GymTrackerDatabase.kt contains `abstract fun workoutSetDao(): WorkoutSetDao`
- ✅ GymTrackerDatabase.kt still contains `version = 1` (not changed)
- ✅ `./gradlew :app:kspDebugKotlin` output contains "BUILD SUCCESSFUL" with no "error:" lines
- ✅ WorkoutSetDao.kt contains `getPreviousSessionSets` with `:exerciseId` named parameter
- ✅ WorkoutSessionRepository.kt contains `suspend fun createSession(name: String, planId: Long?): Long`
- ✅ WorkoutSessionRepository.kt contains `suspend fun getActiveSession(): WorkoutSession?`
- ✅ WorkoutSessionRepository.kt contains `suspend fun finishSession(session: WorkoutSession)`
- ✅ WorkoutSetRepository.kt contains `suspend fun logSet(sessionId: Long, exerciseId: Long, weightKg: Double, reps: Int): Long`
- ✅ WorkoutSetRepository.kt contains `suspend fun getPreviousSessionSets(exerciseId: Long): List<WorkoutSet>`
- ✅ SettingsRepository.kt contains `val REST_TIMER_SECONDS = intPreferencesKey("rest_timer_seconds")`
- ✅ SettingsRepository.kt contains `val restTimerSeconds: Flow<Int>`
- ✅ SettingsRepository.kt contains `suspend fun setRestTimerSeconds(seconds: Int)`
- ✅ AppModule.kt contains `single { WorkoutSessionRepository(get()) }`
- ✅ AppModule.kt contains `single { WorkoutSetRepository(get()) }`
- ✅ `./gradlew :app:assembleDebug` output contains "BUILD SUCCESSFUL"

---

## Deviations from Plan

None — plan executed exactly as written.

- WorkoutSessionDao created with all required methods ✓
- WorkoutSetDao created with all required methods including getPreviousSessionSets ✓
- GymTrackerDatabase updated with both DAO accessors ✓
- WorkoutSessionRepository created with all required methods ✓
- WorkoutSetRepository created with all required methods ✓
- SettingsRepository extended with REST_TIMER_SECONDS and methods ✓
- AppModule updated with DAO and repository registrations ✓
- KSP builds clean ✓
- Full app builds clean ✓

---

## Threat Model Mitigations Applied

| Threat ID | Mitigation | Implementation |
|-----------|-----------|-----------------|
| T-04-02-01 | Input validation: weightKg >= 0.0, reps >= 1 | Caller responsibility enforced via `toDoubleOrNull()` guard in ViewModel (Plan 03) — repository accepts any values |
| T-04-02-02 | Range validation for rest timer seconds | `require(seconds in 10..600)` in `setRestTimerSeconds()` — rejects out-of-range at data boundary |
| T-04-02-03 | Workout data stored in app-private directory | Room database at `/data/data/{package}/databases/` — not accessible without root |

---

## Known Stubs

None. All production code is implemented. Test stubs from Plan 01 remain in test files (WorkoutSessionRepositoryTest.kt, etc.) — those are scaffolds for Plan 03 to implement against.

---

## Self-Check

✅ **PASSED**

- ✅ WorkoutSessionDao.kt exists at `app/src/main/java/de/jupiter1202/gymtracker/feature/workout/WorkoutSessionDao.kt`
- ✅ WorkoutSetDao.kt exists at `app/src/main/java/de/jupiter1202/gymtracker/feature/workout/WorkoutSetDao.kt`
- ✅ WorkoutSessionRepository.kt exists at `app/src/main/java/de/jupiter1202/gymtracker/feature/workout/WorkoutSessionRepository.kt`
- ✅ WorkoutSetRepository.kt exists at `app/src/main/java/de/jupiter1202/gymtracker/feature/workout/WorkoutSetRepository.kt`
- ✅ Commit 4fc8710 exists: `git log --oneline | grep 4fc8710`
- ✅ Commit aa7e88a exists: `git log --oneline | grep aa7e88a`
- ✅ Both commits contain correct files
- ✅ `./gradlew :app:kspDebugKotlin` successful
- ✅ `./gradlew :app:assembleDebug` successful
- ✅ No compile errors in output

---

## Notes for Plan 03 (WorkoutLoggingViewModel)

**Plan 03 will:**
1. Implement `WorkoutLoggingViewModel` as a proper ViewModel (extends androidx.lifecycle.ViewModel)
2. Inject both `WorkoutSessionRepository` and `WorkoutSetRepository` via Koin
3. Manage active session state via MutableStateFlow and StateFlow
4. Implement elapsed timer coroutine (derived from startedAt, updated every second)
5. Implement rest timer coroutine with countdown and completion alert (vibration + sound)
6. Implement session persistence checks (Room writes on each set log — no batching)
7. Make the test scaffolds from Plan 01 PASS

**Data layer is ready for consumption by Plan 03.**

---

*Phase: 04-workout-logging*
*Plan: 02 (data layer)*
*Completed: 2026-04-05T13:41:47Z*
