# Roadmap: GymTracker

## Overview

GymTracker delivers a free, open-source Android workout tracker by building from the data layer outward along the entity dependency chain: exercises first (the atomic unit), then plans that reference exercises, then live session logging that uses plans and exercises, then history and progress visualization over logged data, and finally data export for ownership. Each phase delivers a complete, verifiable capability that unblocks the next.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [x] **Phase 1: Foundation** - Project scaffolding, database schema, architecture, and unit system (completed 2026-04-04)
- [x] **Phase 2: Exercise Library** - Pre-seeded exercise database with search, filter, and custom exercise creation (completed 2026-04-04)
- [x] **Phase 3: Workout Plans** - Custom workout plans and pre-built program templates (completed 2026-04-05)
- [ ] **Phase 4: Workout Logging** - Live and post-hoc session logging with rest timer and previous performance
- [ ] **Phase 5: History and Progress** - Workout history, strength charts, body tracking, and PR detection
- [ ] **Phase 6: Data Export** - CSV and JSON export for full data ownership

## Phase Details

### Phase 1: Foundation
**Goal**: The app builds, runs, and has a complete data layer with the correct 6-entity schema, DI wiring, and unit preference system -- ready for feature development
**Depends on**: Nothing (first phase)
**Requirements**: GEN-01, LOG-02
**Success Criteria** (what must be TRUE):
  1. App builds and launches on an Android device/emulator showing a placeholder home screen
  2. Source code repository has an open source license file (MIT or GPL-3.0)
  3. User can toggle between kg and lbs in a settings screen, and the preference persists after closing and reopening the app
  4. Room database with all 6 entities (Exercise, WorkoutPlan, PlanExercise, WorkoutSession, WorkoutSet, BodyMeasurement) is created on first launch with schema export enabled
**Plans**: 4 plans

Plans:
- [ ] 01-01-PLAN.md — Build scaffolding: add all dependencies, Wave 0 test stubs, MIT LICENSE
- [ ] 01-02-PLAN.md — Room database: 6 entity classes and GymTrackerDatabase
- [ ] 01-03-PLAN.md — DI and settings layer: Koin, DataStore, SettingsRepository, UnitConverter
- [ ] 01-04-PLAN.md — Navigation scaffold and UI: 5-tab nav, placeholder screens, Settings screen

### Phase 2: Exercise Library
**Goal**: Users have a fully populated exercise library they can browse, search, filter, and extend with their own exercises
**Depends on**: Phase 1
**Requirements**: EXER-01, EXER-02, EXER-03
**Success Criteria** (what must be TRUE):
  1. On first launch, the exercise list shows 100-150 pre-seeded exercises tagged with muscle groups
  2. User can search exercises by name and results update as they type
  3. User can filter exercises by muscle group and see only matching exercises
  4. User can create a custom exercise with a name, primary muscle group, and equipment type, and it appears in the library alongside pre-seeded exercises
**Plans**: 5 plans

Plans:
- [ ] 02-01-PLAN.md — Test scaffolds: ExerciseDaoTest and ExerciseRepositoryTest (Wave 0 stubs)
- [ ] 02-02-PLAN.md — Data layer: ExerciseDao, ExerciseRepository, GymTrackerDatabase, AppModule, seed database (human-action)
- [ ] 02-03-PLAN.md — ExerciseViewModel: combine() dual-StateFlow search + filter pattern
- [ ] 02-04-PLAN.md — ExercisesScreen UI: search bar, filter chips, grouped list, create/edit bottom sheet
- [ ] 02-05-PLAN.md — Human verification: end-to-end exercise library smoke test

### Phase 3: Workout Plans
**Goal**: Users can build their own workout routines or start from proven pre-built programs, giving them unlimited plans for free
**Depends on**: Phase 2
**Requirements**: PLAN-01, PLAN-02, PLAN-03
**Success Criteria** (what must be TRUE):
  1. User can create a custom workout plan with a name and add exercises with target sets and reps
  2. User can view and select from pre-built program templates (PPL, 5x5, nSuns, GZCLP) that come with the app for free
  3. User can edit an existing plan by adding, removing, or reordering exercises
  4. User can create unlimited plans with no paywall or artificial restriction
**Plans**: 7 plans

Plans:
- [x] 03-01-PLAN.md — Wave 0 test stubs: WorkoutPlanRepositoryTest, PlanExerciseRepositoryTest, TemplateParserTest
- [x] 03-02-PLAN.md — Dependencies + DAOs + GymTrackerDatabase + templates.json + MuscleGroups constant
- [x] 03-03-PLAN.md — WorkoutPlanRepository (with @Serializable template types) + WorkoutPlanViewModel + Koin DI
- [x] 03-04-PLAN.md — PlansScreen UI: two-section layout, FAB, plan cards, template cards, long-press menu, navigation routes
- [x] 03-05-PLAN.md — PlanDetailScreen: drag-and-drop reorder, swipe-to-delete, exercise picker, target sheet
- [x] 03-06-PLAN.md — TemplatePreviewScreen: exercise list preview, "Use this program" import flow
- [x] 03-06-GAP-01-PLAN.md — Fix template exercise matching with fuzzy algorithm and updated template names
- [x] 03-07-PLAN.md — Human verification: end-to-end Plans smoke test (7 scenarios)

### Phase 4: Workout Logging
**Goal**: Users can log a complete workout session in real time or after the fact, with rest timer support and visibility into previous performance
**Depends on**: Phase 3
**Requirements**: LOG-01, LOG-03, LOG-04, LOG-05
**Success Criteria** (what must be TRUE):
   1. User can start a workout from a plan or as an ad-hoc session and log sets with weight and reps for each exercise
   2. After logging a set, a configurable rest timer auto-starts and counts down visibly
   3. During a workout, each exercise shows what the user lifted last time (e.g., "Last: 3x8 @ 75 kg")
   4. While a session is active, the app displays total workout duration that updates in real time
   5. An in-progress workout survives app close and process death -- reopening the app resumes the session
**Plans**: 15 plans

Plans:
- [x] 04-01-PLAN.md — Wave 0 test stubs: WorkoutSessionRepositoryTest, WorkoutSetRepositoryTest, WorkoutLoggingViewModelTest
- [x] 04-02-PLAN.md — Data layer: WorkoutSessionDao, WorkoutSetDao, WorkoutSessionRepository, WorkoutSetRepository, SettingsRepository rest timer setting
- [x] 04-03-PLAN.md — WorkoutLoggingViewModel: session state, elapsed timer, rest timer, previous performance, input guards
- [x] 04-04-PLAN.md — ActiveWorkoutScreen: LazyColumn exercise sections, inline set input, sticky rest timer banner, exercise picker
- [x] 04-05-PLAN.md — WorkoutSummaryScreen, SettingsScreen rest timer row, VIBRATE permission, timer_beep.ogg
- [x] 04-06-PLAN.md — Navigation wiring: routes, crash recovery, Dashboard quick-start, Plans Start button, post-hoc date picker
- [ ] 04-07-PLAN.md — Human verification: end-to-end workout logging smoke test (8 scenarios)
- [x] 04-08-PLAN.md — Gap closure: Load plan exercises in PlansScreen before session start (GAPS-01)
- [x] 04-09-PLAN.md — Gap closure: Load Exercise entities in crash recovery (GAPS-02)
- [x] 04-10-PLAN.md — Gap closure: Fix elapsed timer updates in UI (GAPS-03)
- [ ] 04-11-PLAN.md — Gap closure verification: Re-verify all gaps fixed
- [x] 04-12-PLAN.md — Root cause fix: ActiveWorkoutScreen activeSession null check + resumeSession guard (Flow race condition)
- [x] 04-13-PLAN.md — Gap verification: Tests confirming all 3 gaps fixed
- [x] 04-14-PLAN.md — UI additions: DoneButton per exercise + markExerciseDone() auto-timer
- [ ] 04-15-PLAN.md — Final human verification: All gaps + Done button functionality

### Phase 5: History and Progress
**Goal**: Users can review past workouts, see their strength progression over time, track body composition, and get motivated by PR detection
**Depends on**: Phase 4
**Requirements**: HIST-01, HIST-02, PROG-01, PROG-02, PROG-03, PROG-04
**Success Criteria** (what must be TRUE):
  1. User can view a chronological list of all past workout sessions with date, name, and summary
  2. User can tap any past session to see the full set-by-set breakdown of that workout
  3. User can view a strength progress chart for any exercise showing weight lifted over time
  4. User can log body weight entries and view a trend line chart over time
  5. User can log body measurements (chest, waist, hips, arms, thighs) and view their history
  6. App detects personal records and highlights them with a visible indicator during and after workouts
**Plans**: TBD

Plans:
- [ ] 05-01: TBD
- [ ] 05-02: TBD

### Phase 6: Data Export
**Goal**: Users have full ownership of their data and can export everything the app has recorded
**Depends on**: Phase 5
**Requirements**: DATA-01
**Success Criteria** (what must be TRUE):
  1. User can export all workout data to a CSV file from a menu option
  2. User can export all workout data to a JSON file from a menu option
  3. Exported files contain all workout sessions, sets, body measurements, and exercise data
**Plans**: TBD

Plans:
- [ ] 06-01: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4 -> 5 -> 6

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation | 4/4 | Complete   | 2026-04-04 |
| 2. Exercise Library | 5/5 | Complete   | 2026-04-04 |
| 3. Workout Plans | 11/11 | Complete  | 2026-04-05 |
| 4. Workout Logging | 10/15 | Root Cause Gap Closure | - |
| 5. History and Progress | 0/? | Not started | - |
| 6. Data Export | 0/? | Not started | - |
