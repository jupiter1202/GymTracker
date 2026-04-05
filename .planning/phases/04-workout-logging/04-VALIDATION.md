---
phase: 4
slug: workout-logging
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-05
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 / Robolectric via androidTest |
| **Config file** | app/build.gradle.kts |
| **Quick run command** | `./gradlew :app:testDebugUnitTest` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest` |
| **Estimated runtime** | ~30 seconds (unit) / ~3 minutes (full) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDebugUnitTest`
- **After every plan wave:** Run `./gradlew :app:testDebugUnitTest`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 4-01-01 | 01 | 1 | LOG-01 | — | N/A | unit | `./gradlew :app:testDebugUnitTest` | ❌ W0 | ⬜ pending |
| 4-01-02 | 01 | 1 | LOG-01 | — | N/A | unit | `./gradlew :app:testDebugUnitTest` | ❌ W0 | ⬜ pending |
| 4-02-01 | 02 | 1 | LOG-03 | — | N/A | unit | `./gradlew :app:testDebugUnitTest` | ❌ W0 | ⬜ pending |
| 4-03-01 | 03 | 2 | LOG-04 | — | N/A | unit | `./gradlew :app:testDebugUnitTest` | ❌ W0 | ⬜ pending |
| 4-04-01 | 04 | 2 | LOG-05 | — | N/A | unit | `./gradlew :app:testDebugUnitTest` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/com/github/jupiter1202/gymtracker/WorkoutSessionTest.kt` — stubs for LOG-01
- [ ] `app/src/test/java/com/github/jupiter1202/gymtracker/RestTimerTest.kt` — stubs for LOG-03
- [ ] `app/src/test/java/com/github/jupiter1202/gymtracker/PreviousPerformanceTest.kt` — stubs for LOG-04

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Rest timer vibration on completion | LOG-03 | Requires physical device vibration hardware | Run on device, complete rest timer, verify haptic feedback |
| Crash recovery resume | LOG-05 | Requires process kill simulation | Start workout, force-stop app, reopen and verify session resumes |
| Real-time duration display | LOG-01 | Requires time-bound UI observation | Start workout, observe duration counter updates every second |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
