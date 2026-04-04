---
phase: 3
slug: workout-plans
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-04-04
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 4 + kotlinx-coroutines-test 1.9.0 |
| **Config file** | none — standard Android test runner |
| **Quick run command** | `./gradlew :app:testDebugUnitTest` |
| **Full suite command** | `./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest` |
| **Estimated runtime** | ~30 seconds (unit tests only) |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :app:testDebugUnitTest`
- **After every plan wave:** Run `./gradlew :app:testDebugUnitTest`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 3-01-01 | 01 | 1 | PLAN-01 | unit | `./gradlew :app:testDebugUnitTest --tests "*.WorkoutPlanRepositoryTest"` | ❌ W0 | ⬜ pending |
| 3-01-02 | 01 | 1 | PLAN-01 | unit | `./gradlew :app:testDebugUnitTest --tests "*.WorkoutPlanRepositoryTest"` | ❌ W0 | ⬜ pending |
| 3-01-03 | 01 | 1 | PLAN-02 | unit | `./gradlew :app:testDebugUnitTest --tests "*.TemplateParserTest"` | ❌ W0 | ⬜ pending |
| 3-01-04 | 01 | 1 | PLAN-02 | unit | `./gradlew :app:testDebugUnitTest --tests "*.WorkoutPlanRepositoryTest"` | ❌ W0 | ⬜ pending |
| 3-02-01 | 02 | 2 | PLAN-03 | unit | `./gradlew :app:testDebugUnitTest --tests "*.PlanExerciseRepositoryTest"` | ❌ W0 | ⬜ pending |
| 3-02-02 | 02 | 2 | PLAN-03 | unit | `./gradlew :app:testDebugUnitTest --tests "*.PlanExerciseRepositoryTest"` | ❌ W0 | ⬜ pending |
| 3-02-03 | 02 | 2 | PLAN-03 | unit | `./gradlew :app:testDebugUnitTest --tests "*.PlanExerciseRepositoryTest"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `app/src/test/.../feature/plans/WorkoutPlanRepositoryTest.kt` — stubs for PLAN-01, PLAN-02 import
- [ ] `app/src/test/.../feature/plans/PlanExerciseRepositoryTest.kt` — stubs for PLAN-03
- [ ] `app/src/test/.../feature/plans/TemplateParserTest.kt` — stubs for PLAN-02 JSON parsing
- [ ] `app/src/test/assets/templates.json` — test fixture (copy of real templates.json for unit tests)

*No new framework install needed — JUnit 4 + coroutines-test already declared in build.gradle.kts*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Drag-and-drop reorder in UI | PLAN-03 | Compose instrumented drag tests are flaky; gesture simulation unreliable | Open plan detail → long-press drag handle → drag row to new position → verify orderIndex persists after back/re-open |
| Template preview screen shows correct exercise list | PLAN-02 | UI navigation state; screenshot testing overkill | Tap PPL template card → verify 3 days of exercises shown → tap "Use this program" → verify plan appears in My Plans |
| FAB bottom sheet creates plan and navigates to detail | PLAN-01 | Navigation + animation; hard to automate reliably | Tap FAB → enter plan name → Save → verify PlansScreen → verify new card → tap card → verify detail screen |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
