# Requirements: GymTracker

**Defined:** 2026-04-04
**Core Value:** Users can track their workouts and see their progress for free — no subscriptions, no paywalls, no nonsense.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Exercise Library

- [x] **EXER-01**: App ships with a pre-seeded library of 100-150 exercises tagged with muscle groups, available on first launch
- [x] **EXER-02**: User can create custom exercises with a name, primary muscle group, and equipment type
- [x] **EXER-03**: User can search exercises by name and filter by muscle group

### Workout Plans

- [ ] **PLAN-01**: User can create custom workout plans with unlimited routines
- [ ] **PLAN-02**: App includes pre-built program templates (PPL, 5x5, nSuns, GZCLP) available for free
- [ ] **PLAN-03**: User can edit existing workout plans (add/remove/reorder exercises)

### Workout Logging

- [ ] **LOG-01**: User can log a workout session by starting a plan or an ad-hoc session and recording sets with weight and reps
- [x] **LOG-02**: User can toggle between kg and lbs; preference persists across sessions
- [ ] **LOG-03**: App displays a configurable rest timer that auto-starts after each set is logged
- [ ] **LOG-04**: App shows previous performance for each exercise during a workout (e.g. "Last: 3×8 @ 75 kg")
- [ ] **LOG-05**: App tracks and displays total workout duration while a session is active

### Workout History

- [ ] **HIST-01**: User can view a chronological list of all past workout sessions
- [ ] **HIST-02**: User can view the full set-by-set breakdown of any past workout session

### Progress Tracking

- [ ] **PROG-01**: User can view a strength progress chart for any exercise (weight lifted over time)
- [ ] **PROG-02**: User can log body weight entries and view a trend chart over time
- [ ] **PROG-03**: User can log body measurements (chest, waist, hips, arms, thighs) and view history
- [ ] **PROG-04**: App detects personal records (PRs) and highlights them during and after workouts

### Data Ownership

- [ ] **DATA-01**: User can export all workout data to CSV and JSON files

### General

- [x] **GEN-01**: App source code is released under an open source license (MIT or GPL-3.0)

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Data Ownership

- **DATA-02**: User can import workout data from a previously exported backup

### Polish

- **PLSH-01**: User can add notes to a workout session
- **PLSH-02**: Estimated 1RM calculation displayed alongside set logging
- **PLSH-03**: Plate calculator (what plates to load for a given weight)

### Advanced Logging

- **ADV-01**: User can log superset/circuit combinations as a single exercise block

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Cloud sync / account system | Local-first by design; no backend needed for v1 |
| Social features | Not the use case; adds complexity without core value |
| AI-generated plans | Out of scope; pre-built programs cover the use case |
| Nutrition / calorie tracking | Different domain; scope explosion risk |
| Subscription / paywall | Antithetical to the project's reason for existing |
| Video exercise demos | Content hosting complexity; not essential |
| Wearable / fitness tracker integration | High complexity, low v1 priority |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| GEN-01 | Phase 1: Foundation | Complete |
| LOG-02 | Phase 1: Foundation | Complete |
| EXER-01 | Phase 2: Exercise Library | Complete |
| EXER-02 | Phase 2: Exercise Library | Complete |
| EXER-03 | Phase 2: Exercise Library | Complete |
| PLAN-01 | Phase 3: Workout Plans | Pending |
| PLAN-02 | Phase 3: Workout Plans | Pending |
| PLAN-03 | Phase 3: Workout Plans | Pending |
| LOG-01 | Phase 4: Workout Logging | Pending |
| LOG-03 | Phase 4: Workout Logging | Pending |
| LOG-04 | Phase 4: Workout Logging | Pending |
| LOG-05 | Phase 4: Workout Logging | Pending |
| HIST-01 | Phase 5: History and Progress | Pending |
| HIST-02 | Phase 5: History and Progress | Pending |
| PROG-01 | Phase 5: History and Progress | Pending |
| PROG-02 | Phase 5: History and Progress | Pending |
| PROG-03 | Phase 5: History and Progress | Pending |
| PROG-04 | Phase 5: History and Progress | Pending |
| DATA-01 | Phase 6: Data Export | Pending |

**Coverage:**
- v1 requirements: 19 total
- Mapped to phases: 19
- Unmapped: 0

---
*Requirements defined: 2026-04-04*
*Last updated: 2026-04-04 after roadmap creation*
