# GymTracker

## What This Is

A free, open-source Android app for tracking gym workouts and measuring fitness progress over time. Users can create their own workout plans or start from pre-built templates (PPL, 5x5, etc.), log sets during or after sessions, and visualize strength and body composition trends. Built because existing apps lock core functionality behind paywalls.

## Core Value

Users can track their workouts and see their progress for free — no subscriptions, no paywalls, no nonsense.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] User can create and manage workout plans
- [ ] User can start from pre-built program templates (PPL, 5x5, etc.)
- [ ] User can log a workout session live or after the fact
- [ ] User can log sets with weight and reps per exercise
- [ ] User can track body weight and measurements over time
- [ ] User can view strength progress charts for exercises over time
- [ ] User can see previous session details before/during a workout
- [ ] App is released under an open source license

### Out of Scope

- Subscription / paywall — core reason the app exists
- Cloud sync / account system — local-first, no backend required for v1
- Social features — not the use case
- AI-generated plans — out of scope for v1

## Context

- Android-native app (Kotlin + Jetpack Compose expected)
- Local data storage — no server required
- User is building this for personal use; FOSS license matters for distribution
- Competing with apps like Strong, Hevy, FitNotes — all have paywalled features

## Constraints

- **Platform**: Android only — no iOS, no web
- **License**: Open source (MIT or GPL) — no proprietary dependencies that would block OSS release
- **Storage**: Local-first — no mandatory account or cloud backend in v1

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Android-only for v1 | User's stated platform, reduces scope | — Pending |
| Local storage only | No backend needed, simpler, privacy-friendly | — Pending |
| Open source license | FOSS requirement from user | — Pending |

---
*Last updated: 2026-04-04 after initialization*
