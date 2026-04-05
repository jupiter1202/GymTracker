# Phase 4: Workout Logging - Research

**Researched:** 2026-04-05
**Domain:** Jetpack Compose / Room DAOs / Coroutine timer / Android Vibrator / SoundPool
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**Active Workout Screen Layout**
- D-01: Single scrollable screen (LazyColumn). All exercises shown at once, each with its own section header, logged sets listed below, and an inline pending-set row at the bottom of each section.
- D-02: Previous performance displayed inline per exercise: "Last: 3×8 @ 80 kg" shown in the exercise section header row.
- D-03: Inline set input — each pending set shows a row with weight and reps fields pre-filled from the last session. User edits inline and taps a checkmark to log it. No bottom sheet per set.
- D-04: "+ Add set" button at the bottom of each exercise section adds another inline input row.
- D-05: Each exercise section has a dismiss/remove button (✕) to remove an exercise from the active session.

**Session Start Flow**
- D-06: Two entry points: Dashboard shows a quick-start card (most recent plan or "Start empty workout"), and each plan card on the Plans tab gets a "Start" button.
- D-07: Dashboard quick-start: if a plan was used recently, it surfaces as the suggested plan; a secondary option allows picking any plan or starting ad-hoc.
- D-08: Only one active session at a time — starting a new session while one is in progress prompts: "You have an active workout. Finish it first or discard it."

**Exercise Adding (Ad-hoc and Mid-Workout)**
- D-09: "+ Add exercise" button at the bottom of the active workout screen opens the exercise picker ModalBottomSheet — same search + filter pattern as PlanDetailScreen. Reuses existing component.

**Rest Timer**
- D-10: Sticky banner pinned below the top app bar showing countdown (e.g. "Rest · 1:23"). Always visible while scrolling through the workout. Tap to skip; long-press or tap to extend by 30 s.
- D-11: Timer auto-starts immediately after a set is logged (checkmark tapped).
- D-12: Configurable default rest duration via the Settings screen. Default: 90 seconds.
- D-13: Alert on timer completion: vibration + short sound. Works even if screen is off.

**Workout Duration**
- D-14: Elapsed time shown in the top app bar of the active workout screen (e.g. "0:42:15"). Updates in real time using a coroutine-based timer derived from `startedAt`.

**Workout Completion**
- D-15: "Finish workout" button saves the session, sets `finishedAt` and `isCompleted = true`, then navigates to a Summary screen.
- D-16: Summary screen: workout name, total duration, number of exercises, total sets, total volume (sum of weight × reps in user's preferred unit). Dismiss navigates to History tab.
- D-17: Confirmation dialog before finishing if any exercise section has no sets logged.

**Session Persistence**
- D-18: Active session written to Room immediately as each set is logged — no batching.
- D-19: On app launch, check for `WorkoutSession` where `isCompleted = false`. If found, auto-redirect to active workout screen (no prompt).
- D-20: DataStore snapshot not needed — Room is source of truth.

**Post-Hoc Logging**
- D-21: Post-hoc sessions use the same logging screen with a date/time picker for `startedAt` surfaced during session creation. Completed sessions set `isCompleted = true` immediately on save.

### Claude's Discretion
- Exact visual styling of the rest timer banner (height, color, progress bar vs countdown only)
- Whether to show a plate calculator hint next to weight fields
- Loading/transition animation when navigating into active workout
- Exact wording and icon choices throughout the logging screen
- `setNumber` auto-increment logic (next = max(setNumber) + 1 for that session+exercise)

### Deferred Ideas (OUT OF SCOPE)
- Plate calculator hint next to weight fields (PLSH-03 backlog)
- Estimated 1RM display during logging (PLSH-02 backlog)
- Superset/circuit logging blocks (ADV-01 backlog)
- Notes on a session (PLSH-01 backlog)
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| LOG-01 | User can log a workout session by starting a plan or an ad-hoc session and recording sets with weight and reps | WorkoutSessionDao (insert + query by isCompleted) + WorkoutSetDao (insert, query last session) + WorkoutLoggingViewModel; schema already exists in Room |
| LOG-03 | App displays a configurable rest timer that auto-starts after each set is logged | Coroutine-based countdown in ViewModel (`viewModelScope.launch` + `delay(1000)`); completion alert via Vibrator (VibrationEffect.createOneShot, API 26+) + SoundPool; default stored in DataStore via SettingsRepository |
| LOG-04 | App shows previous performance for each exercise during a workout (e.g. "Last: 3×8 @ 75 kg") | WorkoutSetDao query: most recent completed session's sets per exercise; JOIN or sub-query returning (weightKg, reps, setCount) grouped by prior sessionId |
| LOG-05 | App tracks and displays total workout duration while a session is active | Elapsed time = `System.currentTimeMillis() - session.startedAt`; updated every second by a `tickerFlow` or while-loop coroutine in ViewModel; displayed in TopAppBar |
</phase_requirements>

---

## Summary

Phase 4 is the most complex phase to date. It introduces three parallel concerns that must work together: (1) real-time UI state across a multi-section LazyColumn, (2) a coroutine-based countdown timer that fires an alert even when the screen is off, and (3) crash-safe session persistence where every set write is immediately durable in Room.

The good news is that the schema is fully built and version-locked at `version = 1` with no pending migration. The entities `WorkoutSession` and `WorkoutSet` exist and compile. The only database work is adding two new DAOs and wiring them into the database class and DI module — a copy-paste-adapt task that exactly mirrors the Phase 2/3 DAO pattern already in the codebase.

The novel areas are: (a) the sticky rest timer banner, which requires a `Box/Scaffold` composition trick to pin a composable between `TopAppBar` and the `LazyColumn` content; (b) the vibration+sound alert on timer completion working when the screen is off, which needs `VIBRATE` permission in the manifest and `SoundPool` (not `MediaPlayer`) for low-latency audio; and (c) the crash recovery check on startup, which needs a one-shot `LaunchedEffect` in `AppNavHost` or `MainActivity` that queries the DB for an unfinished session before rendering the start destination.

**Primary recommendation:** Build in this order — DAOs/repos first, then WorkoutLoggingViewModel with timer state, then ActiveWorkoutScreen UI, then crash-recovery navigation wiring, then Settings rest-timer row. Each step is testable before the next.

---

## Standard Stack

### Core (all already in project)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Room (room-ktx) | 2.8.4 [VERIFIED: libs.versions.toml] | WorkoutSessionDao, WorkoutSetDao persistence | Existing ORM; schema entities already defined |
| Koin | 4.1.1 [VERIFIED: libs.versions.toml] | DI for repositories and ViewModel | Existing DI setup; `single {}` + `viewModel {}` DSL |
| Jetpack Compose + Material3 | BOM 2024.09.00 [VERIFIED: libs.versions.toml] | All UI | Entire app is Compose-only |
| navigation-compose | 2.9.7 [VERIFIED: libs.versions.toml] | `active_workout/{sessionId}` and `workout_summary/{sessionId}` routes | Existing nav setup |
| datastore-preferences | 1.2.1 [VERIFIED: libs.versions.toml] | Rest timer default setting persisted across launches | Existing `SettingsRepository` uses this |
| kotlinx-coroutines | 1.9.0 (test) [VERIFIED: libs.versions.toml] | Ticker coroutine for elapsed time and rest timer countdown | Standard lifecycle-scoped coroutines |
| lifecycle-viewmodel-compose | 2.8.7 [VERIFIED: libs.versions.toml] | `collectAsStateWithLifecycle()` in all screens | Already used throughout app |

### Supporting (no new dependencies needed)

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Android Vibrator (framework) | API 26+ (`VibrationEffect`) [ASSUMED: verified API 26 minimum; project minSdk=29] | Haptic feedback on timer end | Always available, no import needed |
| SoundPool (framework) | API 1+ [ASSUMED] | Short sound playback on timer end (lower latency than MediaPlayer) | Timer completion alert only |

### New Dependencies Needed

None. All required libraries are already declared in `libs.versions.toml` and `build.gradle.kts`.

**No installation step required.**

---

## Architecture Patterns

### Recommended Project Structure

```
feature/workout/
├── WorkoutSessionDao.kt         # Room DAO: insert session, get active, finish
├── WorkoutSetDao.kt             # Room DAO: insert set, get by session+exercise, get last session sets
├── WorkoutSessionRepository.kt  # Wraps both DAOs; creates/finishes sessions
├── WorkoutSetRepository.kt      # Wraps WorkoutSetDao; logs sets, queries previous performance
└── WorkoutLoggingViewModel.kt   # All UI state: active session, exercise list, inline inputs, timer
feature/workout/ui/
├── ActiveWorkoutScreen.kt       # LazyColumn with exercise sections + sticky timer banner
└── WorkoutSummaryScreen.kt      # Post-completion summary
```

The context specifies `AppModule.kt` registers `WorkoutSessionRepository`, `WorkoutSetRepository`, and `WorkoutLoggingViewModel`. The feature package is `feature/workout/` by analogy with `feature/plans/` and `feature/exercises/`.

### Pattern 1: Room DAO — WorkoutSessionDao

**What:** Create session, find active (isCompleted=false), finish session
**When to use:** Start/end of every workout

```kotlin
// Source: [ASSUMED — mirrors PlanExerciseDao.kt pattern in codebase]
@Dao
interface WorkoutSessionDao {
    @Insert
    suspend fun insert(session: WorkoutSession): Long

    @Query("SELECT * FROM workout_sessions WHERE is_completed = 0 LIMIT 1")
    suspend fun getActiveSession(): WorkoutSession?

    @Query("SELECT * FROM workout_sessions WHERE id = :id")
    suspend fun getById(id: Long): WorkoutSession?

    @Update
    suspend fun update(session: WorkoutSession)
}
```

### Pattern 2: Room DAO — WorkoutSetDao

**What:** Log sets, query sets for current session, query previous performance per exercise

```kotlin
// Source: [ASSUMED — named parameters required by Room KSP per STATE.md decision]
@Dao
interface WorkoutSetDao {
    @Insert
    suspend fun insert(set: WorkoutSet): Long

    @Query("SELECT * FROM workout_sets WHERE session_id = :sessionId ORDER BY set_number ASC")
    fun getSetsForSession(sessionId: Long): Flow<List<WorkoutSet>>

    @Query("SELECT * FROM workout_sets WHERE session_id = :sessionId AND exercise_id = :exerciseId ORDER BY set_number ASC")
    fun getSetsForExercise(sessionId: Long, exerciseId: Long): Flow<List<WorkoutSet>>

    @Query("SELECT MAX(set_number) FROM workout_sets WHERE session_id = :sessionId AND exercise_id = :exerciseId")
    suspend fun getMaxSetNumber(sessionId: Long, exerciseId: Long): Int?

    @Delete
    suspend fun delete(set: WorkoutSet)

    // Previous performance: all sets from the most recent completed session for a given exercise
    @Query("""
        SELECT ws.* FROM workout_sets ws
        INNER JOIN workout_sessions s ON ws.session_id = s.id
        WHERE ws.exercise_id = :exerciseId
          AND s.is_completed = 1
          AND s.id = (
              SELECT MAX(s2.id) FROM workout_sessions s2
              INNER JOIN workout_sets ws2 ON ws2.session_id = s2.id
              WHERE ws2.exercise_id = :exerciseId AND s2.is_completed = 1
          )
        ORDER BY ws.set_number ASC
    """)
    suspend fun getPreviousSessionSets(exerciseId: Long): List<WorkoutSet>
}
```

**Note on previous performance query:** The `getPreviousSessionSets` query uses a correlated subquery. Room compiles this at build time — any SQL error surfaces as a KSP compile error, not a runtime crash. Verified approach for complex Room queries. [ASSUMED — pattern aligns with Room documentation approach]

### Pattern 3: Coroutine Elapsed-Time Ticker

**What:** Updates elapsed time every second in ViewModel, displayed in TopAppBar.
**When to use:** While `isCompleted == false` on the active session.

```kotlin
// Source: [ASSUMED — standard Kotlin coroutines ticker pattern]
private fun startElapsedTimer(startedAt: Long) {
    elapsedTimerJob?.cancel()
    elapsedTimerJob = viewModelScope.launch {
        while (isActive) {
            _elapsedMs.value = System.currentTimeMillis() - startedAt
            delay(1_000L)
        }
    }
}
```

The elapsed time is **derived from wall clock minus `startedAt`** (not an accumulator), so app process death and resume automatically shows the correct elapsed duration — no additional crash recovery needed for the timer itself.

### Pattern 4: Coroutine Rest Timer Countdown

**What:** Counts down from configured duration; fires completion alert.
**When to use:** Auto-starts after each set is logged.

```kotlin
// Source: [ASSUMED — standard coroutines countdown pattern]
private fun startRestTimer(durationSeconds: Int) {
    restTimerJob?.cancel()
    _restTimerState.value = RestTimerState.Running(remaining = durationSeconds)
    restTimerJob = viewModelScope.launch {
        var remaining = durationSeconds
        while (remaining > 0 && isActive) {
            delay(1_000L)
            remaining--
            _restTimerState.value = RestTimerState.Running(remaining = remaining)
        }
        if (isActive) {
            _restTimerState.value = RestTimerState.Idle
            triggerTimerAlert()
        }
    }
}
```

**Skip:** `restTimerJob?.cancel()` then set `_restTimerState.value = RestTimerState.Idle`.
**Extend by 30 s:** `(current remaining + 30)` → restart timer from new value.

### Pattern 5: Sticky Rest Timer Banner

**What:** Composable pinned between `TopAppBar` and the scrolling exercise list. Must not scroll away.
**How:** Use `Scaffold` + a custom `topBar` that stacks both `TopAppBar` and the timer banner, OR place the banner as a non-scrolling item before the `LazyColumn` inside a `Column`.

```kotlin
// Source: [ASSUMED — standard Compose layout pattern]
Scaffold(
    topBar = {
        Column {
            TopAppBar(title = { Text(elapsedFormatted) }, ...)
            // Banner only visible when timer is running
            AnimatedVisibility(visible = restTimerState is RestTimerState.Running) {
                RestTimerBanner(
                    state = restTimerState,
                    onSkip = { viewModel.skipRestTimer() },
                    onExtend = { viewModel.extendRestTimer(30) }
                )
            }
        }
    }
) { paddingValues ->
    LazyColumn(modifier = Modifier.padding(paddingValues)) { ... }
}
```

The `Scaffold` `topBar` slot accepts any composable, including a `Column`, so the banner will always sit above the scroll area regardless of scroll position. [ASSUMED — standard Compose behavior]

### Pattern 6: Vibration + Sound Alert (Screen-Off Safe)

**What:** On timer completion: vibrate + play short beep. Must work when screen is off.
**Key insight:** The ViewModel coroutine continues running when the screen turns off (it's process-alive). Vibration and sound calls from the main thread work. Only a full process death stops it — which is addressed by the elapsed timer recovery pattern (crash recovery reads the DB; the timer itself is not a service).

```kotlin
// Source: [ASSUMED — standard Android Vibrator API, minSdk=29 supports VibrationEffect]
// In a helper class or ViewModel (requires Context via Koin androidContext())
val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
vibrator.vibrate(VibrationEffect.createOneShot(300L, VibrationEffect.DEFAULT_AMPLITUDE))
```

**Manifest permission required:**
```xml
<uses-permission android:name="android.permission.VIBRATE" />
```

`VibrationEffect.createOneShot` is available from API 26. Project minSdk is 29 — no version check needed. [VERIFIED: minSdk=29 from build.gradle.kts; VibrationEffect API 26+ is ASSUMED from training knowledge]

**Sound with SoundPool** (preferred over MediaPlayer for short clips):
```kotlin
// Source: [ASSUMED — standard Android SoundPool pattern]
val soundPool = SoundPool.Builder().setMaxStreams(1).build()
val soundId = soundPool.load(context, R.raw.timer_beep, 1)
// On completion:
soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
```

Place a short beep audio file at `app/src/main/res/raw/timer_beep.ogg` (OGG Vorbis recommended for Android — smallest file, fast decode). [ASSUMED]

**Important:** SoundPool must be created once and reused (expensive to create). Create in ViewModel or a singleton helper, release in `onCleared()`.

### Pattern 7: Crash Recovery Navigation Check

**What:** On app launch, if an unfinished session exists in Room, redirect directly to the active workout screen.
**Where:** `LaunchedEffect` in `AppNavHost` composable before the `NavHost` renders, or in `MainActivity`'s `onCreate`.

```kotlin
// Source: [ASSUMED — standard Compose navigation pattern]
// In AppNavHost, before NavHost:
val scope = rememberCoroutineScope()
LaunchedEffect(Unit) {
    val activeSession = workoutSessionRepository.getActiveSession()
    if (activeSession != null) {
        navController.navigate("active_workout/${activeSession.id}") {
            popUpTo(BottomNavDestination.Dashboard.route) { inclusive = false }
        }
    }
}
```

**Gotcha:** This `LaunchedEffect` must run before the user sees the Dashboard. Since `LaunchedEffect` runs after the first composition, there may be a brief flash of the Dashboard. Mitigate by defaulting to a loading/splash state in `startDestination` and navigating once the check completes, or by accepting the brief Dashboard flash (acceptable for v1).

### Pattern 8: Inline Set Input Row State

**What:** Each pending-set row has local weight and reps state, pre-filled from previous performance.
**Where:** UI state held in `WorkoutLoggingViewModel` as a map `exerciseId -> PendingSetInput(weight, reps)`.

```kotlin
// Source: [ASSUMED]
data class PendingSetInput(
    val weightDisplay: String,  // in user's display unit
    val reps: String
)

// In ViewModel:
private val _pendingInputs = MutableStateFlow<Map<Long, PendingSetInput>>(emptyMap())
val pendingInputs: StateFlow<Map<Long, PendingSetInput>> = _pendingInputs.asStateFlow()
```

Pre-fill: when an exercise is added to the active session, look up `getPreviousSessionSets(exerciseId)` and use the last set's weight and reps as the initial input values.

### Anti-Patterns to Avoid

- **Batching set writes to Room:** D-18 explicitly forbids this. Write immediately on checkmark tap.
- **Using MediaPlayer for the timer beep:** MediaPlayer has significant startup latency for short clips; SoundPool is the correct choice. [ASSUMED]
- **Storing elapsed time as an accumulator in DataStore:** The correct approach is `currentTimeMillis() - startedAt` (D-14/D-20). Accumulator drift and write pressure make DataStore wrong for this.
- **Using a Foreground Service for the rest timer:** Not needed. The ViewModel coroutine survives screen-off. A foreground service would be needed only if the user wants the timer to continue after swiping the app away (process death) — which is out of scope per D-20.
- **Using DataStore for session recovery:** D-20 explicitly states Room is the source of truth. No DataStore snapshot needed.
- **Adding new Room schema entities:** Both `WorkoutSession` and `WorkoutSet` exist in `version = 1`. Do NOT bump schema version — there is no migration needed.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Session crash recovery | Custom SharedPreferences "active session ID" flag | Room `isCompleted = false` query | Already in schema; ACID; D-20 mandated |
| Weight unit conversion | Custom formula | `UnitConverter.kgToLbs()` / `UnitConverter.lbsToKg()` | Already exists at `core/UnitConverter.kt` |
| Previous performance query | In-memory session history cache | Room DAO `getPreviousSessionSets(exerciseId)` | Room compiles query at build time; correct by construction |
| Exercise picker in active workout | New search/filter component | `ExercisePickerSheet` / `ExercisePickerContent` from `PlanDetailScreen.kt` | D-09 mandated; already built and tested |
| Settings rest timer row | New settings infrastructure | Add row to existing `SettingsScreen.kt` + `SettingsRepository.kt` | `SettingsRow` composable already exists; `PreferenceKeys` pattern established |
| Rest timer persistence across restart | Separate timer state in DataStore | Not needed — timer does not survive process death (design decision, see D-20) | Per decisions: only session sets/completion persist |

**Key insight:** All persistence infrastructure (Room schema, DataStore, DI) was built in Phase 1 specifically for this phase. The only work is adding DAOs and repositories — no infrastructure is missing.

---

## Common Pitfalls

### Pitfall 1: Named Parameters in Room @Query

**What goes wrong:** Using positional parameters (e.g., `?`) in Room `@Query` SQL causes a KSP compile error.
**Why it happens:** Room KSP requires named parameters (`:paramName`).
**How to avoid:** Always use `:parameterName` syntax in all SQL queries.
**Warning signs:** KSP compile error mentioning "unnamed parameter" or "ambiguous column".
**Evidence:** Explicitly documented in STATE.md — "ExerciseDao uses named parameters (:query, :muscleGroup) in @Query SQL — required by Room KSP."

### Pitfall 2: Room Database Abstract Methods Not Registered

**What goes wrong:** Creating `WorkoutSessionDao` and `WorkoutSetDao` but forgetting to add `abstract fun workoutSessionDao(): WorkoutSessionDao` to `GymTrackerDatabase.kt`. Room compile error at KSP time.
**How to avoid:** Add abstract accessor methods to `GymTrackerDatabase.kt` as the first step after creating DAO interfaces.
**Warning signs:** Room KSP error about "cannot find implementation" for the DAO.
**Current state of `GymTrackerDatabase.kt`:** Has `exerciseDao()`, `workoutPlanDao()`, `planExerciseDao()` but NOT the two new workout DAOs [VERIFIED: read GymTrackerDatabase.kt].

### Pitfall 3: SoundPool Load Race

**What goes wrong:** Calling `soundPool.play(soundId, ...)` immediately after `soundPool.load(...)` before the audio asset is decoded. Result: silent playback.
**How to avoid:** Use `SoundPool.OnLoadCompleteListener` to set a `soundLoaded` flag; only call `play()` once loaded.
**Warning signs:** Timer completion is silent on first ring but works on subsequent rings.

### Pitfall 4: Weight Display Unit in Inline Inputs

**What goes wrong:** Storing user-entered weight directly in `weightKg` when user is in lbs mode. Silent data corruption.
**How to avoid:** The ViewModel must convert: if `weightUnit == "lbs"`, call `UnitConverter.lbsToKg(enteredValue)` before writing to Room. Display: read `weightKg` from DB and convert to display unit for pre-fill.
**Warning signs:** Weights appear correct in kg mode but show wildly wrong values after switching units.
**Evidence:** `WorkoutSet.weightKg` comment: "Always stored in kg — display layer converts to lbs via UnitConverter" [VERIFIED: read WorkoutSet.kt].

### Pitfall 5: DataStore Key Collision for Rest Timer Setting

**What goes wrong:** Adding rest timer default as `intPreferencesKey("rest_timer")` without checking existing keys. No collision currently, but the pattern must be followed in `SettingsRepository.kt`.
**How to avoid:** Add to the `PreferenceKeys` object in `SettingsRepository.kt` alongside `WEIGHT_UNIT`. Use `intPreferencesKey` (not `stringPreferencesKey`) for integer seconds.
**Evidence:** `PreferenceKeys` object in `SettingsRepository.kt` verified [VERIFIED: read SettingsRepository.kt].

### Pitfall 6: Compose Recomposition on Every Tick

**What goes wrong:** Elapsed timer ticks every second. If the ViewModel emits a new `UiState` object wrapping all session data every second, the entire active workout screen recomposes every second.
**How to avoid:** Keep `_elapsedMs` as a separate `StateFlow<Long>` (not inside a combined UiState). The `TopAppBar` text subscribes only to `elapsedMs`, not to the full session state.

### Pitfall 7: setNumber Auto-Increment Off-By-One

**What goes wrong:** Using `count(*)` instead of `MAX(set_number)` for next set number when sets can be deleted.
**How to avoid:** Use `getMaxSetNumber(sessionId, exerciseId)` — returns null if no sets yet, so `nextSetNumber = (maxSetNumber ?: 0) + 1`.
**Evidence:** CONTEXT.md Claude's Discretion note: "next = max(setNumber) + 1 for that session+exercise".

---

## Code Examples

### WorkoutSessionDao — Active Session Query

```kotlin
// Source: [ASSUMED — mirrors WorkoutPlanDao pattern; named params per STATE.md requirement]
@Query("SELECT * FROM workout_sessions WHERE is_completed = 0 LIMIT 1")
suspend fun getActiveSession(): WorkoutSession?
```

### SettingsRepository — Add Rest Timer Key

```kotlin
// Source: [VERIFIED: existing SettingsRepository.kt pattern]
object PreferenceKeys {
    val WEIGHT_UNIT = stringPreferencesKey("weight_unit")
    val REST_TIMER_SECONDS = intPreferencesKey("rest_timer_seconds") // ADD THIS
}

val restTimerSeconds: Flow<Int> = dataStore.data.map { prefs ->
    prefs[PreferenceKeys.REST_TIMER_SECONDS] ?: 90
}

suspend fun setRestTimerSeconds(seconds: Int) {
    dataStore.edit { prefs ->
        prefs[PreferenceKeys.REST_TIMER_SECONDS] = seconds
    }
}
```

### AppNavHost — Crash Recovery Check

```kotlin
// Source: [ASSUMED — standard navigation-compose LaunchedEffect pattern]
// Add BEFORE the NavHost block in AppNavHost.kt
LaunchedEffect(Unit) {
    val active = workoutSessionRepository.getActiveSession() // suspend, runs in composition scope
    if (active != null) {
        navController.navigate("active_workout/${active.id}") {
            popUpTo(BottomNavDestination.Dashboard.route) { inclusive = false }
        }
    }
}
```

**Important:** `workoutSessionRepository` must be injected into `AppNavHost` or retrieved via `koinInject()`.

### VIBRATE Permission (AndroidManifest.xml)

```xml
<!-- Add inside <manifest> before <application> -->
<uses-permission android:name="android.permission.VIBRATE" />
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| KAPT annotation processing | KSP for Room | AGP 9.x (Phase 1 established) | All Room DAOs use `ksp()`, not `kapt()` |
| Deprecated `menuAnchor()` | `menuAnchor(MenuAnchorType.PrimaryNotEditable)` | M3 update (Phase 2) | Use correct anchor for any dropdowns |
| Deprecated `org.koin.androidx.viewmodel.dsl.viewModel` | `org.koin.core.module.dsl.viewModel` | Koin 4.x (Phase 1 established) | Use modern DSL in AppModule.kt |

---

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `VibrationEffect.createOneShot` is available with no version check at minSdk 29 | Standard Stack + Code Examples | Low — VibrationEffect was added in API 26; minSdk=29 is higher |
| A2 | SoundPool is preferred over MediaPlayer for short timer alert audio | Don't Hand-Roll | Low — SoundPool lower latency for short clips is well-established Android guidance |
| A3 | ViewModel coroutine continues running when screen turns off (process alive) | Architecture Patterns P6 | Low — standard Android lifecycle; only process death stops the coroutine |
| A4 | Placing timer banner inside `Scaffold` `topBar` slot via nested `Column` correctly pins it above `LazyColumn` | Architecture Patterns P5 | Low — standard Compose behavior; if wrong, use `Column { banner; LazyColumn }` instead |
| A5 | The `getPreviousSessionSets` correlated subquery compiles correctly under Room KSP | Architecture Patterns P2 | Medium — complex SQL; if KSP rejects it, simplify to two queries (1: max session id, 2: sets for that session) |
| A6 | `intPreferencesKey` is available in `androidx.datastore.preferences.core` at version 1.2.1 | Common Pitfalls 5 | Low — standard DataStore API since 1.0.0 |
| A7 | `koinInject()` can be called inside a composable to retrieve a repository for crash recovery | Code Examples | Low — standard Koin Compose integration; alternative: pass repository as parameter from MainActivity |

---

## Open Questions

1. **Timer alert audio asset**
   - What we know: `SoundPool.load(context, R.raw.timer_beep, 1)` requires `res/raw/timer_beep.*` to exist
   - What's unclear: Is there a preference for audio file format (OGG vs MP3 vs WAV)?
   - Recommendation: Use a short OGG Vorbis file (~0.5s). If no asset is available, vibration-only is acceptable for v1 as the sound is a polish item.

2. **Crash recovery flash of Dashboard**
   - What we know: `LaunchedEffect` runs after first composition, so Dashboard renders briefly before redirect
   - What's unclear: Whether the brief flash is acceptable for v1
   - Recommendation: Accept for v1; a proper solution requires moving the check before NavHost renders (e.g., in MainActivity or a dedicated SplashScreen), which adds complexity.

---

## Environment Availability

Step 2.6: SKIPPED — no new external tools or services. All dependencies are already in the project's Gradle configuration. Vibrator and SoundPool are Android framework APIs requiring no additional installation.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4 (junit 4.13.2) + kotlinx-coroutines-test 1.9.0 |
| Config file | None — standard Android test runner |
| Quick run command | `./gradlew testDebugUnitTest` |
| Full suite command | `./gradlew testDebugUnitTest` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| LOG-01 | `createSession()` returns valid ID; `logSet()` writes to DAO | unit | `./gradlew testDebugUnitTest --tests "*.WorkoutSessionRepositoryTest"` | Wave 0 |
| LOG-01 | `getActiveSession()` returns null when no incomplete session | unit | `./gradlew testDebugUnitTest --tests "*.WorkoutSessionRepositoryTest"` | Wave 0 |
| LOG-03 | Rest timer default reads 90 from SettingsRepository | unit | `./gradlew testDebugUnitTest --tests "*.SettingsRepositoryTest"` | Wave 0 |
| LOG-03 | `setRestTimerSeconds()` persists to DataStore | unit | `./gradlew testDebugUnitTest --tests "*.SettingsRepositoryTest"` | Wave 0 |
| LOG-04 | `getPreviousSessionSets(exerciseId)` returns sets from most recent completed session | unit | `./gradlew testDebugUnitTest --tests "*.WorkoutSetRepositoryTest"` | Wave 0 |
| LOG-04 | Returns empty list when no prior session exists for that exercise | unit | `./gradlew testDebugUnitTest --tests "*.WorkoutSetRepositoryTest"` | Wave 0 |
| LOG-05 | Elapsed time = currentTimeMillis - startedAt (ViewModel logic) | unit | `./gradlew testDebugUnitTest --tests "*.WorkoutLoggingViewModelTest"` | Wave 0 |

### Sampling Rate

- **Per task commit:** `./gradlew testDebugUnitTest`
- **Per wave merge:** `./gradlew testDebugUnitTest`
- **Phase gate:** Full suite green before `/gsd-verify-work`

### Wave 0 Gaps

- [ ] `app/src/test/.../feature/workout/WorkoutSessionRepositoryTest.kt` — covers LOG-01 (start session, active check)
- [ ] `app/src/test/.../feature/workout/WorkoutSetRepositoryTest.kt` — covers LOG-01 (log set), LOG-04 (previous performance query)
- [ ] `app/src/test/.../feature/workout/WorkoutLoggingViewModelTest.kt` — covers LOG-05 (elapsed time computation)
- [ ] `app/src/test/.../feature/settings/SettingsRepositoryTest.kt` — covers LOG-03 (rest timer default setting) — NOTE: check if already exists from Phase 1 (SettingsRepository was created then)

---

## Security Domain

Phase 4 handles locally-stored workout data only. No network calls, no authentication, no user credentials, no sensitive data beyond workout records that are local-first by design. ASVS categories V2 (Authentication), V3 (Session Management as in user auth), V6 (Cryptography) do not apply.

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | No | N/A — local app, no accounts |
| V3 Session Management | No | N/A — "session" here is a workout session, not an auth session |
| V4 Access Control | No | N/A — single user, local device |
| V5 Input Validation | Yes | Weight input: `toDoubleOrNull()` guard; reps input: `toIntOrNull()` guard; reject null/negative values |
| V6 Cryptography | No | N/A — no encryption required for workout data |

---

## Sources

### Primary (HIGH confidence)
- `GymTrackerDatabase.kt` — verified entity list, version=1, existing DAO pattern
- `WorkoutSession.kt` — verified schema: id, planId (nullable), name, startedAt, finishedAt (nullable), isCompleted
- `WorkoutSet.kt` — verified schema: id, sessionId, exerciseId, setNumber, weightKg, reps, completedAt; comment confirms kg-always storage
- `libs.versions.toml` — verified all library versions in use
- `AppModule.kt` — verified DI registration pattern: `single {}` + `viewModel {}` DSL
- `SettingsRepository.kt` — verified `PreferenceKeys` object pattern and DataStore usage
- `PlanDetailScreen.kt` — verified `ExercisePickerSheet` / `ExercisePickerContent` composables available for reuse
- `build.gradle.kts` — verified minSdk=29, confirming VibrationEffect API 26+ is unconditionally available
- `STATE.md` — verified KSP named-parameter requirement, Koin 4.x modern DSL decision

### Secondary (MEDIUM confidence)
- Android developer documentation pattern for `VibrationEffect.createOneShot` (API 26+, below minSdk=29)
- Android developer documentation pattern for `SoundPool` for low-latency short audio

### Tertiary (LOW confidence — flagged in Assumptions Log)
- Correlated subquery acceptance by Room KSP (A5 — fallback strategy documented)
- `koinInject()` availability in composable scope for crash recovery (A7 — standard API)

---

## Metadata

**Confidence breakdown:**
- Standard Stack: HIGH — all libraries verified in libs.versions.toml; no new dependencies needed
- Architecture: HIGH — all patterns mirror established codebase patterns; novel areas (timer banner, vibration) are standard Android patterns
- Pitfalls: HIGH — pitfalls 1-3 are directly evidenced from STATE.md decisions; pitfalls 4-7 are standard Android/Compose patterns

**Research date:** 2026-04-05
**Valid until:** 2026-05-05 (stable library versions; no fast-moving dependencies)
