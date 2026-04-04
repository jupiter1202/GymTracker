---
phase: 2
slug: exercise-library
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-04
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit4 + AndroidJUnit4 (instrumented), JUnit4 (unit) |
| **Config file** | None — standard Android test runner via `testInstrumentationRunner` in `build.gradle.kts` |
| **Quick run command** | `./gradlew testDebugUnitTest` |
| **Full suite command** | `./gradlew connectedDebugAndroidTest` |
| **Estimated runtime** | ~30s (unit) / ~3 min (instrumented, requires emulator/device) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew testDebugUnitTest`
- **After every plan wave:** Run `./gradlew connectedDebugAndroidTest`
- **Before `/gsd:verify-work`:** Full instrumented suite must be green
- **Max feedback latency:** 30 seconds (unit); ~3 min (instrumented per wave)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| seed-db | TBD | 0 | EXER-01 | instrumented | `./gradlew connectedDebugAndroidTest` | ❌ W0 | ⬜ pending |
| dao-queries | TBD | 1 | EXER-01, EXER-03 | instrumented | `./gradlew connectedDebugAndroidTest` | ❌ W0 | ⬜ pending |
| custom-exercise-create | TBD | 1 | EXER-02 | instrumented | `./gradlew connectedDebugAndroidTest` | ❌ W0 | ⬜ pending |
| delete-guard | TBD | 1 | EXER-02 | unit | `./gradlew testDebugUnitTest` | ❌ W0 | ⬜ pending |
| search-filter-combined | TBD | 1 | EXER-03 | instrumented | `./gradlew connectedDebugAndroidTest` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/androidTest/java/de/jupiter1202/gymtracker/feature/exercises/ExerciseDaoTest.kt` — covers EXER-01, EXER-02, EXER-03 DAO layer
- [ ] `app/src/test/java/de/jupiter1202/gymtracker/feature/exercises/ExerciseRepositoryTest.kt` — covers delete-guard logic (EXER-02)

*Existing `GymTrackerDatabaseTest.kt` and `SettingsRepositoryTest.kt` demonstrate the established patterns for both test types.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Exercise list shows sticky headers by muscle group when no search active | EXER-03 | UI layout behavior not testable via unit/instrumented | Launch app → Exercise Library → verify grouped headers visible with no search text |
| ModalBottomSheet for adding custom exercise opens and dismisses correctly | EXER-02 | Compose UI gesture/animation behavior | Tap Add Exercise → verify bottom sheet appears, fill form, submit, verify dismissed and list updated |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s (unit) / 3 min (instrumented)
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
