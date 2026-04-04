---
phase: 1
slug: foundation
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-04
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 (junit:junit:4.13.2) — already in project |
| **Config file** | No separate config file; standard Android Gradle test source sets |
| **Quick run command** | `./gradlew testDebugUnitTest` |
| **Full suite command** | `./gradlew connectedDebugAndroidTest` |
| **Estimated runtime** | ~10s (unit) / ~2-3min (instrumented, requires emulator) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew testDebugUnitTest`
- **After every plan wave:** Run `./gradlew connectedDebugAndroidTest`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 10 seconds (unit tests)

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 1-UnitConverter | 01 | 1 | LOG-02 | unit | `./gradlew testDebugUnitTest --tests "*.UnitConverterTest"` | ❌ W0 | ⬜ pending |
| 1-DataStore | 01 | 2 | LOG-02 | instrumented | `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=de.jupiter1202.gymtracker.SettingsRepositoryTest` | ❌ W0 | ⬜ pending |
| 1-Database | 02 | 1 | GEN-01 (schema) | instrumented | `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=de.jupiter1202.gymtracker.GymTrackerDatabaseTest` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/java/de/jupiter1202/gymtracker/core/UnitConverterTest.kt` — stubs for LOG-02 (conversion math)
- [ ] `app/src/androidTest/java/de/jupiter1202/gymtracker/core/database/GymTrackerDatabaseTest.kt` — stubs for database schema (all 6 tables exist)
- [ ] `app/src/androidTest/java/de/jupiter1202/gymtracker/feature/settings/SettingsRepositoryTest.kt` — stubs for LOG-02 (persistence across process restart)
- [ ] `testImplementation(libs.androidx.room.testing)` added to app/build.gradle.kts — needed for in-memory DB in instrumented tests

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| LICENSE file exists in repo root | GEN-01 | File existence check, not a runtime behavior | Run `ls ./LICENSE` in repo root; verify MIT or GPL-3.0 content |
| App builds and launches on emulator | Phase goal | Requires physical device/emulator launch | Run app on emulator; verify placeholder home screen appears |
| App schema JSON committed to git | Phase hygiene | CI check, not a unit test | Run `git status app/schemas/`; verify 1.json is committed |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 10s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
