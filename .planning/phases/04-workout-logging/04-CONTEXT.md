# Phase 4: Workout Logging - Context

**Gathered:** 2026-04-05
**Status:** Ready for planning

<domain>
## Phase Boundary

Users can log a complete workout session live or after the fact — start from a plan or ad-hoc, record sets with weight and reps for each exercise, get an auto-starting rest timer after each set, see previous performance inline, and track elapsed time. An in-progress session survives app close and process death. Workout history viewing and progress charts are Phase 5.

</domain>

<decisions>
## Implementation Decisions

### Active Workout Screen Layout
- **D-01:** Single scrollable screen (LazyColumn). All exercises shown at once, each with its own section header, logged sets listed below, and an inline pending-set row at the bottom of each section.
- **D-02:** Previous performance displayed inline per exercise: "Last: 3×8 @ 80 kg" shown in the exercise section header row.
- **D-03:** Inline set input — each pending set shows a row with weight and reps fields pre-filled from the last session. User edits inline and taps a checkmark to log it. No bottom sheet per set.
- **D-04:** "+ Add set" button at the bottom of each exercise section adds another inline input row.
- **D-05:** Each exercise section has a dismiss/remove button (✕) to remove an exercise from the active session.

### Session Start Flow
- **D-06:** Two entry points: Dashboard shows a quick-start card (most recent plan or "Start empty workout"), and each plan card on the Plans tab gets a "Start" button.
- **D-07:** Dashboard quick-start: if a plan was used recently, it surfaces as the suggested plan; a secondary option allows picking any plan or starting ad-hoc.
- **D-08:** Only one active session at a time — starting a new session while one is in progress prompts: "You have an active workout. Finish it first or discard it."

### Exercise Adding (Ad-hoc and Mid-Workout)
- **D-09:** "+ Add exercise" button at the bottom of the active workout screen opens the exercise picker ModalBottomSheet — same search + filter pattern as PlanDetailScreen. Reuses existing component.

### Rest Timer
- **D-10:** Sticky banner pinned below the top app bar showing countdown (e.g. "Rest · 1:23"). Always visible while scrolling through the workout. Tap to skip; long-press or tap to extend by 30 s.
- **D-11:** Timer auto-starts immediately after a set is logged (checkmark tapped).
- **D-12:** Configurable default rest duration via the Settings screen (Phase 1 settings foundation already has a slot for this). Default: 90 seconds.
- **D-13:** Alert on timer completion: vibration + short sound. Works even if screen is off.

### Workout Duration
- **D-14:** Elapsed time shown in the top app bar of the active workout screen (e.g. "0:42:15"). Updates in real time using a coroutine-based timer derived from `startedAt`.

### Workout Completion
- **D-15:** "Finish workout" button (in top bar or bottom of screen) saves the session, sets `finishedAt` and `isCompleted = true`, then navigates to a Summary screen.
- **D-16:** Summary screen shows: workout name, total duration, number of exercises, total sets, and total volume (sum of weight × reps, displayed in user's preferred unit). Dismiss navigates to History tab.
- **D-17:** Confirmation dialog before finishing if any exercise section has no sets logged ("You have exercises with no sets logged. Finish anyway?").

### Session Persistence (Crash/Close Recovery)
- **D-18:** Active session is written to Room immediately as each set is logged — no batching. `WorkoutSession` row with `isCompleted = false` is the authoritative in-progress marker.
- **D-19:** On app launch, check for any `WorkoutSession` where `isCompleted = false`. If found, auto-redirect directly to the active workout screen (no prompt — only one active session possible).
- **D-20:** DataStore snapshot not needed — Room is the source of truth. Recovery reads live DB rows.

### Post-Hoc Logging
- **D-21:** Post-hoc sessions use the same logging screen with a date/time picker for `startedAt` surfaced during session creation ("When did you work out?"). Completed sessions entered post-hoc set `isCompleted = true` immediately on save.

### Claude's Discretion
- Exact visual styling of the rest timer banner (height, color, progress bar vs countdown only)
- Whether to show a plate calculator hint next to weight fields
- Loading/transition animation when navigating into active workout
- Exact wording and icon choices throughout the logging screen
- `setNumber` auto-increment logic (next = max(setNumber) + 1 for that session+exercise)

</decisions>

<specifics>
## Specific Ideas

- The inline set row pattern (weight × reps + checkmark) mirrors Strong/Hevy — most familiar to gym users
- Pre-filling from last session is critical: user taps checkmark with no edits if weight/reps are the same — fastest path for routine workouts
- The sticky timer banner must not cover content; it pins above the exercise list, not as an overlay

</specifics>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

No external specs — requirements are fully captured in decisions above.

### Schema
- `app/src/main/java/de/jupiter1202/gymtracker/core/database/entities/WorkoutSession.kt` — Session entity: id, planId (nullable), name, startedAt, finishedAt (nullable), isCompleted
- `app/src/main/java/de/jupiter1202/gymtracker/core/database/entities/WorkoutSet.kt` — Set entity: id, sessionId, exerciseId, setNumber, weightKg, reps, completedAt
- `app/src/main/java/de/jupiter1202/gymtracker/core/database/GymTrackerDatabase.kt` — Needs WorkoutSessionDao and WorkoutSetDao abstract methods added

### Reusable Screens / Components
- `app/src/main/java/de/jupiter1202/gymtracker/feature/plans/PlanDetailScreen.kt` — Exercise picker bottom sheet pattern to reuse in active workout
- `app/src/main/java/de/jupiter1202/gymtracker/feature/exercises/ExercisesScreen.kt` — Search + filter chip pattern reference
- `app/src/main/java/de/jupiter1202/gymtracker/navigation/AppNavHost.kt` — Navigation wiring; new routes needed: active_workout/{sessionId}, workout_summary/{sessionId}
- `app/src/main/java/de/jupiter1202/gymtracker/feature/settings/SettingsScreen.kt` — Add rest timer default setting row here

### DI / Architecture Reference
- `app/src/main/java/de/jupiter1202/gymtracker/core/di/AppModule.kt` — Register WorkoutSessionRepository, WorkoutSetRepository, WorkoutLoggingViewModel
- `app/src/main/java/de/jupiter1202/gymtracker/core/data/UnitConverter.kt` — Use for weight display (kg ↔ lbs)

### Requirements
- `LOG-01`: Start plan or ad-hoc session, log sets with weight and reps
- `LOG-03`: Configurable rest timer auto-starts after each set
- `LOG-04`: Previous performance shown per exercise during workout
- `LOG-05`: Total workout duration displayed in real time

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `WorkoutSession.kt` + `WorkoutSet.kt`: Schema fully defined in Phase 1 — no migration needed. `isCompleted` flag is the crash-recovery marker.
- `PlanDetailScreen.kt`: Exercise picker ModalBottomSheet (search + filter) ready to reuse for "+ Add exercise" in the active workout.
- `UnitConverter.kt` (`core/`): kg ↔ lbs for weight display — use in set input fields and summary screen volume calc.
- `SettingsScreen.kt`: Settings row composable ready — add rest timer default duration setting.
- `PlanExerciseDao.kt` / `WorkoutPlanRepository.kt`: Pattern for DAO + Repository to follow for WorkoutSessionDao + WorkoutSetRepository.

### Established Patterns
- ViewModel → Repository (no use-case layer), Koin `single {}` + `viewModel {}` DSL
- `ModalBottomSheet` for contextual inputs (Phase 2/3 pattern) — reuse for exercise picker
- `LazyColumn` with section headers (Phase 2 muscle group grouping) — adapt for exercise sections in active workout
- Named parameters in `@Query` SQL (`:sessionId`, `:exerciseId`)
- Weight always stored in kg; display via UnitConverter

### Integration Points
- `GymTrackerDatabase.kt`: add `abstract fun workoutSessionDao(): WorkoutSessionDao` and `abstract fun workoutSetDao(): WorkoutSetDao`
- `AppModule.kt`: register WorkoutSessionRepository, WorkoutSetRepository, WorkoutLoggingViewModel
- `AppNavHost.kt`: add `active_workout/{sessionId}` and `workout_summary/{sessionId}` composable routes; add crash-recovery check on launch
- `DashboardScreen.kt`: add quick-start workout card; check for active session on init
- `PlansScreen.kt`: add "Start" button per plan card

</code_context>

<deferred>
## Deferred Ideas

- Plate calculator hint next to weight fields — v2 (PLSH-03 already in backlog)
- Estimated 1RM display during logging — v2 (PLSH-02 already in backlog)
- Superset/circuit logging blocks — v2 (ADV-01 already in backlog)
- Notes on a session — v2 (PLSH-01 already in backlog)

</deferred>

---

*Phase: 04-workout-logging*
*Context gathered: 2026-04-05*
