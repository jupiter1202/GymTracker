# Project Research Summary

**Project:** GymTracker
**Domain:** Android gym/workout tracking app (FOSS, local-first)
**Researched:** 2026-04-04
**Confidence:** MEDIUM-HIGH

## Executive Summary

GymTracker is a free, open-source Android workout tracking app that competes with Strong and Hevy by removing artificial paywalls on routines and programs. The domain is well-understood: experts build these apps with Room for local persistence, Jetpack Compose for UI, and MVVM architecture -- all first-party Google-recommended technologies. The existing project scaffold already has Kotlin 2.2.10, AGP 9.1.0, and Compose in place, though dependencies need updating to current stable versions.

The recommended approach is a layered build starting from the database schema outward. The data model is the most consequential decision in the entire project: getting the 6-entity hierarchical schema right (Exercise, WorkoutPlan, PlanExercise, WorkoutSession, WorkoutSet, BodyMeasurement) prevents costly rewrites later. From there, the exercise library and workout templates form the foundation, followed by the active session logger (the core UX), then progress visualization. The competitive differentiator is straightforward: unlimited routines and pre-built programs (PPL, 5x5, nSuns) for free, where competitors charge $5-9/month.

The primary risks are data-layer mistakes that compound downstream: a flat database schema that cannot represent plans vs. sessions, losing active workout state on Android process death, and shipping without exercise seed data. All three are preventable with upfront design. Secondary risks include Compose recomposition jank on the workout logging screen (the most complex UI) and Room migration strategy not being established before real users have data. The FOSS constraint is a non-issue -- every recommended dependency is Apache 2.0 licensed.

## Key Findings

### Recommended Stack

The stack is conventional Android with no exotic choices. Everything is first-party Jetpack except Koin (DI) and Vico (charts), both Apache 2.0 licensed. The existing project needs its Compose BOM updated from 2024.09.00 to 2026.03.01 and several AndroidX dependencies brought to current stable.

**Core technologies:**
- **Kotlin 2.2.10 + Jetpack Compose (BOM 2026.03.01)**: UI toolkit -- already in project, needs version bump
- **Room 2.8.4 + KSP 2.3.4**: Local persistence -- first-party, compile-time SQL verification, Flow integration, migration support
- **Navigation Compose 2.9.7**: Screen navigation -- type-safe routes since 2.8+, ViewModel scoping per destination
- **Lifecycle ViewModel/Runtime Compose 2.10.0**: MVVM glue -- collectAsStateWithLifecycle for safe Flow collection
- **Koin 4.0.x**: Dependency injection -- lightweight, no code gen, simpler than Hilt for single-developer project
- **Vico 2.x**: Charts -- Compose-native, Material 3, handles line/bar charts for weight progression
- **DataStore Preferences 1.1.x**: Settings (units, theme, rest timer defaults)

**Version verification needed:** Koin 4.0.x and Vico 2.x exact patch versions could not be verified (web fetch was blocked). Confirm on Maven Central before adding to version catalog.

### Expected Features

**Must have (table stakes):**
- Log sets with weight + reps (kg/lbs toggle, decimal support)
- Pre-populated exercise library (100-150 exercises with muscle group tags)
- Exercise search and filter by muscle group
- Custom exercise creation
- Workout templates/routines (create once, reuse)
- Previous performance display during workout ("last time you did...")
- Workout history (list by date, tap for detail)
- Rest timer (configurable, auto-start after logging set, notification)
- Body weight tracking with chart
- Basic progress charts (weight over time per exercise)

**Should have (differentiators -- the competitive edge):**
- Unlimited routines for free (Strong limits to 3 free)
- Pre-built program templates (PPL, 5x5, nSuns, GZCLP) for free
- PR detection and celebration (crown/confetti on new records)
- Export/import (CSV + JSON) for data ownership
- Workout notes and per-set notes
- Estimated 1RM calculation
- Dark theme (Material 3 makes this near-free)
- Workout duration tracking

**Defer (v2+):**
- Cloud sync / account system (explicitly excluded in PROJECT.md)
- Social features, AI workout generation, video demos
- Superset/circuit logging (UI complexity)
- Wearable integration, Google Fit integration
- Nutrition tracking, cardio-specific tracking
- Plate calculator, extended body measurements

### Architecture Approach

MVVM with simplified Clean Architecture layers in a single-module project. Three layers with strict dependency direction: UI (Compose screens + ViewModels exposing StateFlow) -> Domain (plain Kotlin models + repository interfaces) -> Data (Room DAOs + repository implementations + entity mappers). Skip the domain use-case layer initially; extract UseCases only when logic is reused across multiple ViewModels. Data flows down as method calls and up as reactive Flow/StateFlow streams. The active workout session must be modeled as app-level state (not screen-level) that persists across navigation and survives process death by writing every action to Room immediately.

**Major components:**
1. **Room Database (6 entities)** -- Exercise, WorkoutPlan, PlanExercise, WorkoutSession, WorkoutSet, BodyMeasurement with proper FK relationships and indices
2. **Repository Layer** -- ExerciseRepository, WorkoutRepository, MeasurementRepository; maps entities to domain models; single source of truth
3. **ViewModels (per screen)** -- Hold UI state as StateFlow, handle user actions, never import Compose
4. **Compose Screens** -- ExerciseList, PlanList/Editor, ActiveSession, SessionHistory, Progress, Measurements
5. **Navigation** -- Type-safe sealed class routes, persistent active-session banner when workout in progress
6. **DI Container (Koin)** -- Wires repositories, database, ViewModels

### Critical Pitfalls

1. **Flat workout schema** -- Design 6+ entities from day one with the snapshot pattern (sessions copy from templates at creation time, never reference mutable template rows for historical data). Include planned vs. actual reps/weight fields and ordering columns everywhere.
2. **Process death kills active workout** -- Persist every set/action to Room immediately. Model session status as IN_PROGRESS/COMPLETED/ABANDONED. On app resume, detect and restore in-progress sessions. Never rely on ViewModel memory or SavedStateHandle for workout data.
3. **Room migration not planned** -- Never ship with fallbackToDestructiveMigration in release. Enable exportSchema=true from v1. Set up MigrationTestHelper infrastructure before any real user data exists. Add manual backup/export as a safety net.
4. **Empty exercise library on first launch** -- Ship 80-120 pre-seeded exercises with muscle group and equipment metadata. Mark as is_custom=false so app updates can refresh them without overwriting user-created exercises.
5. **Weight unit ambiguity** -- Store all weights in kilograms canonically. Convert for display only. Include unit in export formats.

## Implications for Roadmap

Based on combined research, here is the suggested phase structure. The ordering follows data dependencies: you cannot log sets without exercises, cannot show progress without logged data, cannot build programs without templates.

### Phase 1: Foundation and Data Layer
**Rationale:** Everything depends on the database schema. Getting it wrong here means rewriting every subsequent phase. Architecture and pitfall research both emphasize this is the highest-risk, highest-leverage decision.
**Delivers:** Room database with all 6 entities, DAOs, repository interfaces and implementations, DI container, domain models, type converters, schema export, migration test infrastructure
**Addresses:** Data persistence (table stakes), unit toggle (kg canonical storage)
**Avoids:** Pitfall 1 (flat schema), Pitfall 2 (no migration strategy), Pitfall 7 (unit ambiguity), Pitfall 9 (template-session coupling), Pitfall 14 (no exercise type support)

### Phase 2: Exercise Library
**Rationale:** Exercises are the atomic unit every other feature references. The first vertical slice through all layers (data -> repository -> ViewModel -> Compose screen) validates the architecture.
**Delivers:** Exercise list screen with search and muscle group filter, add/edit custom exercise screen, pre-seeded database of 100-150 exercises
**Addresses:** Exercise library, exercise search/filter, custom exercise creation (table stakes)
**Avoids:** Pitfall 4 (empty library), Pitfall 11 (poor search)

### Phase 3: Workout Plans and Templates
**Rationale:** Plans define what a workout session looks like. Must exist before the active session screen can pre-fill exercises and targets. Pre-built programs (the key differentiator) are specialized templates.
**Delivers:** Plan list and editor screens, create/edit routines with exercises and target sets/reps/weight, built-in program templates (PPL, 5x5, GZCLP)
**Addresses:** Workout templates/routines, pre-built programs (differentiator), unlimited routines for free (differentiator)
**Avoids:** Pitfall 9 (template-session coupling via snapshot pattern)

### Phase 4: Active Workout Session
**Rationale:** This is the core UX and the most complex screen. It requires exercises (Phase 2) and plans (Phase 3) to exist. Must handle process death from the first implementation.
**Delivers:** Active session screen (log sets in real-time), previous performance overlay, rest timer with notification, workout duration tracking, persistent "workout in progress" banner across navigation, session start/finish/discard flow
**Addresses:** Log sets with weight+reps, previous performance display, rest timer, workout duration (table stakes)
**Avoids:** Pitfall 3 (process death data loss), Pitfall 5 (LazyColumn recomposition jank), Pitfall 6 (navigation confusion), Pitfall 10 (timer bugs)

### Phase 5: History, Progress, and Records
**Rationale:** Requires logged session data from Phase 4. Charts and PR detection are the motivational layer that drives retention.
**Delivers:** Session history list and detail view, per-exercise progress charts (using Vico), estimated 1RM calculation, PR detection with celebration UI, body weight tracking with trend chart
**Addresses:** Workout history, progress charts, estimated 1RM, PR detection, body weight tracking (table stakes + differentiators)
**Avoids:** Pitfall 8 (slow chart queries -- use SQL aggregation), Pitfall 13 (no e1RM)

### Phase 6: Data Ownership and Polish
**Rationale:** Export/import is critical for open-source credibility but not a launch blocker. Settings, theming, and onboarding are final polish.
**Delivers:** CSV + JSON export/import, settings screen (units, theme, rest timer defaults), dark theme (Material 3), onboarding flow for first launch, workout/set notes
**Addresses:** Export/import, dark theme, workout notes (differentiators), unit toggle UI
**Avoids:** Pitfall 12 (no onboarding/empty states)

### Phase Ordering Rationale

- **Data layer first** because both architecture and pitfall research identify the schema as the highest-risk decision. Every subsequent phase builds on it.
- **Exercise library before plans before sessions** follows the entity dependency chain: sessions reference plans which reference exercises.
- **Active session is Phase 4 (not earlier)** because it is the most complex screen and needs exercises + plans to be functional. Attempting it too early leads to a God ViewModel anti-pattern.
- **History and progress after session logging** because charts need real data to display.
- **Export/polish last** because these are high value but not blocking the core logging loop.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 1 (Foundation):** The entity schema design deserves careful review. The snapshot pattern for template-to-session copying and the exercise type enum (weighted/bodyweight/timed) need detailed spec before implementation.
- **Phase 4 (Active Session):** Most complex screen. Process death handling, navigation architecture for persistent session state, and Compose performance optimization all need attention during phase planning.

Phases with standard patterns (skip research-phase):
- **Phase 2 (Exercise Library):** Standard CRUD + list/detail pattern. Well-documented.
- **Phase 3 (Workout Plans):** Standard CRUD with junction table. Well-documented.
- **Phase 5 (History/Progress):** Charting with Vico is straightforward. SQL aggregation for stats is well-documented.
- **Phase 6 (Polish):** Export/import and settings are standard patterns.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All core technologies verified against developer.android.com. Only Koin and Vico exact patch versions unverified. |
| Features | MEDIUM | Based on training data knowledge of competitor apps (Strong, Hevy, FitNotes). Feature sets may have shifted since May 2025. Core table-stakes list is reliable. |
| Architecture | HIGH | MVVM + Room + Compose is Google's official recommended pattern. Package structure and data flow are well-established. |
| Pitfalls | MEDIUM-HIGH | Pitfalls are well-known in Android dev community and fitness app domain. Process death and Room migration issues are extensively documented officially. Domain-specific pitfalls (schema, snapshot pattern) based on training data. |

**Overall confidence:** MEDIUM-HIGH

### Gaps to Address

- **Koin and Vico exact versions**: Verify latest stable on Maven Central before adding to version catalog. Architecture research also suggested manual DI as an alternative to Koin -- the team should decide during Phase 1 planning.
- **Exercise seed data source**: Need to curate the actual list of 100-150 exercises with muscle groups and equipment types. This is content work, not engineering.
- **Pre-built program definitions**: PPL, 5x5, nSuns, GZCLP program structures (sets, reps, progression rules) need to be specified. Consider sourcing from community wikis (r/fitness, etc.).
- **Competitor feature verification**: Feature matrix is from training data (May 2025 cutoff). Strong and Hevy may have changed pricing or features. Spot-check before finalizing differentiator strategy.
- **Compose BOM 2026.03.01 compatibility**: Verify that all recommended AndroidX libraries are compatible with this BOM version. BOM manages Compose artifacts but not Room, Navigation, or Lifecycle independently.

## Sources

### Primary (HIGH confidence)
- Android Architecture Guide -- developer.android.com/topic/architecture
- Room Persistence Library -- developer.android.com/training/data-storage/room
- Jetpack Compose BOM 2026.03.01 -- developer.android.com/jetpack/compose/bom/bom-mapping
- Navigation Compose -- developer.android.com/jetpack/compose/navigation
- Lifecycle 2.10.0 -- developer.android.com/jetpack/androidx/releases/lifecycle

### Secondary (MEDIUM confidence)
- Koin 4.0.x -- training data, Apache 2.0 license confirmed
- Vico charting library -- training data, Compose-native with M3 support
- Competitor feature analysis (Strong, Hevy, FitNotes) -- training data through May 2025
- Fitness app UX patterns and common pitfalls -- training data, consistent with community knowledge

### Tertiary (needs validation)
- Exact Koin patch version (verify on Maven Central)
- Exact Vico patch version (verify on GitHub releases)
- Current competitor pricing and feature gates (may have changed)

---
*Research completed: 2026-04-04*
*Ready for roadmap: yes*
