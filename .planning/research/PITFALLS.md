# Domain Pitfalls

**Domain:** Android Gym/Workout Tracking App
**Researched:** 2026-04-04
**Confidence:** MEDIUM (based on training data; web search unavailable for verification)

---

## Critical Pitfalls

Mistakes that cause rewrites or major data loss.

### Pitfall 1: Flat Workout Logging Schema

**What goes wrong:** Developers model workout data as a single table or two tables (workouts + sets), missing the hierarchical nature of gym logging: Program > Workout Template > Session > Exercise Instance > Set. This leads to duplicated data, inability to reorder exercises within a session, and no clean way to distinguish "the plan" from "what actually happened."

**Why it happens:** The domain looks simple on the surface. A workout is just exercises with sets, right? But real usage requires: planned vs actual reps, supersets/circuits, rest timers per exercise, exercise ordering, and the ability to modify a template mid-session without corrupting the template.

**Consequences:**
- Cannot implement "show last session's performance" without ugly queries
- Template edits retroactively change historical sessions (if sharing rows)
- Supersets and circuit ordering become impossible to represent
- Migration to a correct schema requires rewriting every query and DAO

**Prevention:**
Design at minimum 6 core entities from day one:
```
Exercise (library) -- the exercise definition
ProgramTemplate -- a named program (e.g., "PPL")
WorkoutTemplate -- a day within a program (e.g., "Push Day")
WorkoutTemplateExercise -- exercise + ordering within template
WorkoutSession -- an actual logged session (with start/end time)
WorkoutSet -- individual set within a session (linked to session + exercise)
```

Key rules:
- Sessions COPY from templates at creation time (snapshot pattern). Never point to live template rows for historical data.
- `WorkoutSet` stores `planned_reps`, `planned_weight`, `actual_reps`, `actual_weight` so users can see plan vs reality.
- Include an `order` integer column on every entity that needs ordering (exercises within a session, sets within an exercise).

**Detection:** If you cannot answer "what did I bench press 3 weeks ago?" with a single simple query joining Session > Set > Exercise, your schema is wrong.

**Phase relevance:** Phase 1 (data layer). Getting this wrong in Phase 1 means rewriting in Phase 2.

---

### Pitfall 2: Room Migration Strategy Not Planned From Day One

**What goes wrong:** Room requires explicit Migration objects for every schema change between versions. Developers ship v1 without a migration strategy, then discover that adding a column, changing a type, or restructuring tables in v2 either crashes the app or silently wipes user data (fallbackToDestructiveMigration).

**Why it happens:** During development, `fallbackToDestructiveMigration()` is convenient -- schema changes "just work" by nuking the database. This masks the problem until real users have data they care about.

**Consequences:**
- Users lose all workout history on app update (unrecoverable if no backup)
- 1-star reviews and uninstalls
- Panic-fixing migrations after the damage is done

**Prevention:**
1. **Never ship with `fallbackToDestructiveMigration()`** in release builds. Use it only in debug builds via a BuildConfig check.
2. **Export schemas from Room** by setting `exportSchema = true` in `@Database` and configuring the schema output directory in build.gradle. This creates JSON schema files per version that serve as migration contracts.
3. **Write migration tests from version 1.** Use `MigrationTestHelper` to verify every migration path. Even if v1 has no migration yet, set up the test infrastructure.
4. **Use AutoMigration** (Room 2.4+) for simple changes (adding columns, adding tables). It handles `ALTER TABLE ADD COLUMN` automatically. But understand its limits: it cannot handle column renames, type changes, or table restructuring without a manual `AutoMigrationSpec`.
5. **Add a manual backup/restore feature** early (export to JSON or CSV). This is your safety net regardless of migration correctness.

**Detection:** If `exportSchema` is false or not set, migration problems are coming.

**Phase relevance:** Phase 1 (database setup). The migration infrastructure must exist before any data is persisted by real users.

---

### Pitfall 3: Workout Session State Lost on Process Death

**What goes wrong:** Active workout session state (current exercise, set in progress, rest timer) is held only in ViewModel memory. Android kills the process (low memory, user switches to music app), and the entire in-progress workout is gone.

**Why it happens:** Developers test on modern devices with plenty of RAM. Process death is invisible during development because Android Studio keeps the debugger attached, which prevents the OS from killing the process.

**Consequences:**
- User is mid-workout, switches to Spotify for 2 minutes, comes back to find their session gone
- This is the single most complained-about bug in gym tracking apps
- Workarounds after the fact require significant rearchitecting

**Prevention:**
1. **Persist session state to Room immediately** on every meaningful action (start session, complete set, skip exercise). The database is the source of truth, not the ViewModel.
2. **Model session state as a database entity** with a status field: `IN_PROGRESS`, `COMPLETED`, `ABANDONED`. On app start, check for `IN_PROGRESS` sessions and resume them.
3. **Do NOT rely on `SavedStateHandle`** for workout data. It has a size limit (~1MB) and is meant for UI state (scroll position, selected tab), not domain data. Use it only for transient UI state like "which tab was selected."
4. **Test process death explicitly** during development: In developer options, enable "Don't keep activities" or use `adb shell am kill <package>` after backgrounding.

**Detection:** If your "start workout" action does not write to the database, this pitfall is active.

**Phase relevance:** Phase 2 (workout logging). Must be correct from the first implementation of live session tracking.

---

### Pitfall 4: Exercise Library Designed as User-Editable Only (No Seed Data)

**What goes wrong:** The app launches with an empty exercise library. Users must manually type "Bench Press", "Squat", "Deadlift" etc. before they can log a single workout. This creates massive friction in the first-use experience.

**Why it happens:** Developers think "users will add their own exercises" and skip the effort of curating a seed database. Or they worry about maintaining a large exercise dataset.

**Consequences:**
- First-use experience is terrible: user opens app, sees empty list, has to create everything from scratch before logging anything
- Inconsistent naming (some users type "Bench", others "Bench Press", others "Flat Barbell Bench Press") -- this breaks exercise-level progress tracking
- No muscle group / category metadata means no auto-generated push/pull/legs splits

**Prevention:**
1. **Ship a pre-seeded exercise database** with 80-120 common exercises. Cover: chest, back, shoulders, biceps, triceps, quads, hamstrings, glutes, calves, abs, cardio. Include barbell, dumbbell, cable, machine, bodyweight variants of major movements.
2. **Mark seeded exercises as `is_custom = false`** so they can be updated in future app versions without overwriting user modifications.
3. **Include metadata:** muscle group (primary + secondary), equipment type, exercise type (compound/isolation/cardio). This enables filtering, template suggestions, and muscle group analytics.
4. **Use Room's `createFromAsset()` or `RoomDatabase.Callback.onCreate()`** to seed from a bundled SQLite file or run INSERT statements on first launch.
5. **Allow users to create custom exercises** that coexist with seeded ones. Custom exercises should have `is_custom = true` and never be modified by app updates.

**Detection:** If your Exercise table is empty after first install, this pitfall is active.

**Phase relevance:** Phase 1 (data layer + exercise library). Must be populated before workout template creation makes sense.

---

## Moderate Pitfalls

### Pitfall 5: Jetpack Compose LazyColumn Recomposition in Workout Logging

**What goes wrong:** The workout logging screen -- a list of exercises, each with a list of sets, each with editable weight/rep fields -- causes severe jank and dropped frames. Every keystroke in a weight field triggers recomposition of the entire list.

**Why it happens:** Compose recomposes any composable whose parameters change. If the workout state is a single `List<ExerciseWithSets>` object passed down, changing one set's weight creates a new list, which recomposes every item.

**Prevention:**
1. **Use stable, immutable data classes** for list items. Ensure they have correct `equals()` implementations (data classes get this automatically, but verify nested objects do too).
2. **Provide `key` to `LazyColumn` items** -- use the database ID, not the list index: `items(sets, key = { it.setId })`.
3. **Hoist text field state to the ViewModel per-set**, not per-screen. Each set's weight/reps should be an individual `StateFlow<String>` or use `TextFieldValue` state scoped to that set. This ensures editing set #3 does not recompose sets #1, #2, #4.
4. **Use `derivedStateOf`** for computed values (e.g., total volume) so they only recompose when the underlying data actually changes.
5. **Avoid passing lambdas that capture mutable state** directly in `LazyColumn` item blocks. Use `remember` with stable keys or pass the ViewModel action directly.
6. **Profile with Layout Inspector** (recomposition counts) early and often. Do not wait until the screen is "done."

**Detection:** Open Layout Inspector during workout logging, type in a weight field, watch recomposition counts. If items not being edited show recompositions > 0, this is active.

**Phase relevance:** Phase 2-3 (workout logging UI). Must be addressed when building the set-entry interface.

---

### Pitfall 6: No Concept of "Workout in Progress" in Navigation Architecture

**What goes wrong:** The app treats an active workout session as just another screen. User navigates to settings, exercise library, or progress charts and loses context. Or worse, they can start a second workout while one is already active.

**Why it happens:** Standard navigation patterns (NavHost with screens) do not inherently model "persistent background state." A workout session is more like a media player -- it runs in the background regardless of which screen you are on.

**Prevention:**
1. **Model the active session as app-level state**, not screen-level. Use a singleton `ActiveSessionManager` (or equivalent) scoped to the Application/ViewModel that persists across navigation.
2. **Show a persistent banner or bottom bar indicator** when a workout is in progress ("Push Day -- 23:45 elapsed -- Tap to return"). This is standard UX in Strong, Hevy, and every good fitness app.
3. **Prevent starting a second session** while one is active. Show a dialog: "You have an active workout. Finish or discard it first?"
4. **Back button from workout screen should NOT end the session.** It should navigate away but keep the session active. Only an explicit "Finish Workout" or "Discard Workout" action should end it.

**Detection:** If pressing Back during a workout destroys session state, this is active.

**Phase relevance:** Phase 2 (navigation + session management).

---

### Pitfall 7: Weight Unit Handling as an Afterthought

**What goes wrong:** All weights are stored as raw numbers with no unit indicator. When a user switches between kg and lbs (or the app adds unit support later), historical data becomes ambiguous. Was that "100" in kg or lbs?

**Why it happens:** Developer uses one unit system and does not think about the other.

**Prevention:**
1. **Store all weights in a canonical unit (kilograms)** in the database. Always. No exceptions.
2. **Convert for display only** based on user preference. The preference is a UI concern, not a data concern.
3. **Store the user's preferred unit in a preferences table** or DataStore, not hardcoded.
4. **Handle precision correctly:** kg uses 2.5kg increments, lbs uses 5lb increments. Display formatting should respect this (no "45.359237 kg" when converting from 100 lbs).
5. **Include unit in export formats** (CSV, JSON backup) to avoid ambiguity.

**Detection:** If your `WorkoutSet` entity has a `weight: Double` with no unit column or documented convention, this is active.

**Phase relevance:** Phase 1 (data layer). Must be decided before any weight is stored.

---

### Pitfall 8: Charts and Progress Tracking Built on Raw Queries

**What goes wrong:** Progress charts query every set ever logged for an exercise, aggregate in Kotlin code, and render. As the dataset grows (months of training), chart loading becomes slow and the UI freezes.

**Why it happens:** Small datasets during development mask the O(n) growth problem.

**Prevention:**
1. **Use Room `@Query` with SQL aggregation** (MAX, GROUP BY date) rather than loading all rows and aggregating in Kotlin. SQL is orders of magnitude faster for this.
2. **Consider a pre-computed "personal records" table** or a summary/stats table that updates on session completion. Query the summary for charts, not raw sets.
3. **Paginate or window chart data** -- show last 3 months by default, let user expand. Do not load 2 years of data at once.
4. **Use `Flow` from Room** for reactive updates, but combine with `distinctUntilChanged()` to avoid chart re-renders on unrelated database changes.

**Detection:** If your chart query contains `SELECT * FROM workout_sets WHERE exercise_id = ?` without aggregation, this will become a problem.

**Phase relevance:** Phase 3 (progress visualization).

---

### Pitfall 9: Template-Session Coupling (Editing Templates Alters History)

**What goes wrong:** Sessions reference template exercises via foreign key. When the user edits a template (reorders exercises, removes an exercise, changes target reps), historical sessions that reference those template rows now show incorrect data or crash on missing references.

**Why it happens:** Normalization instinct -- "don't duplicate data." But in this domain, a session is a historical record that must be immutable.

**Prevention:**
1. **Snapshot on session start.** When a user starts a workout from a template, copy the template's exercises and target sets into session-specific rows. The session owns its data from that moment forward.
2. **Template edits only affect future sessions.** Past sessions are frozen in time.
3. **Use soft deletes for exercises in templates** (`is_archived = true`) rather than hard deletes, to avoid dangling references in sessions that were created before the edit.
4. **Foreign keys from sessions should point to the Exercise library (immutable definitions), NOT to template exercise rows (mutable plans).**

**Detection:** If deleting an exercise from a template causes a foreign key violation or changes how a past session displays, this is active.

**Phase relevance:** Phase 1-2 (schema design, template/session logic).

---

## Minor Pitfalls

### Pitfall 10: Rest Timer Not Surviving Configuration Changes

**What goes wrong:** A countdown rest timer between sets resets to its initial value on screen rotation or recomposition. Users see "90 seconds" jump back to "90 seconds" mid-countdown.

**Prevention:**
1. Store timer start timestamp (not remaining seconds) in the ViewModel or persisted state.
2. Calculate remaining time as `targetDuration - (now - startTimestamp)` on every tick.
3. Use `SystemClock.elapsedRealtime()` (not `System.currentTimeMillis()`) to avoid clock-change issues.
4. Persist the timer's start timestamp to the database as part of the active session state (survives process death).

**Phase relevance:** Phase 2 (workout logging with rest timer).

---

### Pitfall 11: Exercise Search Implemented as SQL LIKE '%query%'

**What goes wrong:** As the exercise library grows (especially with custom exercises), search becomes slow and does not handle typos or partial matches well. `LIKE '%bench%'` cannot find "Barbell Bench Press" if the user types "bech."

**Prevention:**
1. For a library of <500 exercises, `LIKE` is fine performance-wise but poor for fuzzy matching.
2. Implement a simple in-memory fuzzy search (filter the full list in Kotlin) since the dataset is small. Libraries like `fuzzywuzzy` or a simple Levenshtein-based filter work well.
3. Add category/muscle group filters as a complement to text search. Users can tap "Chest" to narrow results before searching.
4. Room FTS (Full Text Search) is overkill for this dataset size but is an option if you anticipate thousands of custom exercises.

**Phase relevance:** Phase 1-2 (exercise library UI).

---

### Pitfall 12: No Onboarding or Empty State Design

**What goes wrong:** New user opens the app, sees an empty "Your Workouts" screen with no guidance. They do not know they need to create a program first, or that templates exist, or how to start logging.

**Prevention:**
1. Design an explicit first-launch flow: "Start with a popular program" (PPL, 5x5, etc.) or "Create your own."
2. Every empty screen should have a clear call-to-action, not just blank space. "No workouts yet -- Start your first workout" with a prominent button.
3. Pre-built templates should be available from day one (ties to Pitfall 4 -- seeded exercise library).
4. Consider a quick "What equipment do you have?" onboarding question to filter templates and exercises to relevant ones.

**Phase relevance:** Phase 3 (UX polish, onboarding).

---

### Pitfall 13: Ignoring One-Rep Max and Volume Calculations

**What goes wrong:** The app stores raw weight x reps but never calculates estimated one-rep max (e1RM) or total volume. Users cannot compare progress across different rep ranges (e.g., 100kg x 5 vs 80kg x 10 -- which is stronger?).

**Prevention:**
1. Implement e1RM calculation early using a standard formula (Epley: `weight * (1 + reps/30)` or Brzycki).
2. Store or compute total session volume (`SUM(weight * reps)`) for trend tracking.
3. Use e1RM as the basis for progress charts, not raw weight (which only tells part of the story for varying rep ranges).

**Phase relevance:** Phase 3 (progress tracking and charts).

---

### Pitfall 14: Not Handling Bodyweight and Timed Exercises

**What goes wrong:** Schema assumes every exercise has weight + reps. Bodyweight exercises (pull-ups, push-ups) and timed exercises (planks, cardio) do not fit this model. Developers bolt on special cases later, creating inconsistent data.

**Prevention:**
1. Add an `exercise_type` enum to the Exercise entity: `WEIGHTED`, `BODYWEIGHT`, `TIMED`, `DISTANCE` (for cardio).
2. Make `weight` nullable in the set entity. Bodyweight exercises log reps only. Timed exercises log duration. Distance exercises log distance + optional time.
3. For weighted bodyweight exercises (e.g., weighted pull-ups with a belt), the weight field represents added weight, not total body weight. Document this convention.
4. Design the set-entry UI to adapt based on exercise type: show weight+reps fields for weighted, reps-only for bodyweight, duration for timed.

**Phase relevance:** Phase 1 (schema design) and Phase 2 (adaptive set-entry UI).

---

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Mitigation |
|-------------|---------------|------------|
| Database schema design | Flat schema (P1), Template-Session coupling (P9), No unit convention (P7), No exercise type support (P14) | Design 6+ entity schema with snapshot pattern, canonical kg storage, exercise type enum from day one |
| Exercise library | Empty library (P4), Poor search (P11) | Ship pre-seeded database with 80-120 exercises, include metadata, add fuzzy search |
| Workout session logging | Process death data loss (P3), Navigation confusion (P6), Timer bugs (P10) | Persist every action to DB immediately, model session as app-level state, use elapsed-time-based timers |
| Workout logging UI (Compose) | LazyColumn recomposition jank (P5) | Per-set state hoisting, stable keys, profile with Layout Inspector |
| Progress visualization | Slow chart queries (P8), No e1RM (P13) | SQL aggregation, summary tables, implement e1RM formula |
| Data migration | Destructive migration in release (P2) | exportSchema=true, migration tests, manual backup feature |
| UX and onboarding | Empty state confusion (P12) | First-launch flow, pre-built templates, clear CTAs on empty screens |

## Sources

- Training knowledge of Room database, Jetpack Compose, and Android architecture patterns (MEDIUM confidence)
- Common patterns observed in open-source fitness apps (Strong, FitNotes, JEFIT design analysis) (MEDIUM confidence)
- Web search was unavailable for verification; all findings based on established Android development patterns and fitness app domain knowledge

**Confidence note:** These pitfalls are well-established patterns in Android development and fitness app design. The Room migration, process death, and Compose recomposition pitfalls are extensively documented in official Android documentation. The domain-specific pitfalls (schema design, template-session coupling, unit handling) are consistent across fitness app development discussions. However, specific version numbers and API details should be verified against current Room and Compose documentation before implementation.
