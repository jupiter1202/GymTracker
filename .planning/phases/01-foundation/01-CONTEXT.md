# Phase 1: Foundation - Context

**Gathered:** 2026-04-04
**Status:** Ready for planning

<domain>
## Phase Boundary

Project scaffolding, Room database with all 6 entities, Koin DI wiring, and unit preference system — app builds, launches, and is ready for feature development. Exercise library, workout plans, and logging are separate phases.

</domain>

<decisions>
## Implementation Decisions

### App Architecture
- Feature-based package structure: `feature/exercises`, `feature/plans`, `feature/logging`, `feature/history`, `feature/export`
- Shared infrastructure in `core/`: `core/database`, `core/di`, `core/ui`
- No domain/use-case layer — ViewModel calls repository methods directly (repository → ViewModel)
- Base package: `de.jupiter1202.gymtracker`

### Navigation Skeleton
- Full bottom navigation scaffold set up in Phase 1 with 5 tabs: Dashboard, Exercises, Plans, History, Settings
- Phases 2–5 fill in the real screens; Phase 1 shows placeholder screens for each tab
- Placeholder screens: simple centered text with tab name + "Coming soon" subtitle

### Settings Screen
- Full settings screen foundation with sections and a reusable row composable — future settings (rest timer default, theme, etc.) drop in without restructuring
- Phase 1 exposes only the kg/lbs unit toggle
- Unit toggle uses Material3 `SingleChoiceSegmentedButtonRow` showing "kg" and "lbs" side by side
- Settings accessible as the 5th bottom nav tab

### Unit System
- kg/lbs preference persisted via DataStore (Preferences)
- UnitConverter utility built in Phase 1 under `core/` — handles kg↔lbs conversion for use by Phases 4 and 5
- Database always stores weight values in kg; display layer converts to lbs when preference is set

### Claude's Discretion
- Exact DataStore key naming and structure
- Room schema export directory path
- Loading skeleton or splash screen (if any)
- Koin module organization within `core/di/`

</decisions>

<specifics>
## Specific Ideas

- Storage: always-in-kg is the canonical decision — switching user preference must never corrupt historical data
- The bottom nav scaffold is meant to be the permanent navigation structure; Phases 2–5 replace placeholder composables, not the nav wiring

</specifics>

<code_context>
## Existing Code Insights

### Reusable Assets
- `MainActivity.kt`: Exists as a standard Compose entry point — will be refactored to host NavHost + bottom nav scaffold
- `ui/theme/` (Color.kt, Type.kt, Theme.kt): GymTrackerTheme already set up — use as-is, no changes needed in Phase 1

### Established Patterns
- Jetpack Compose + Material3: already in build config, no additional setup needed
- `compileSdk 36`, `minSdk 29` — confirmed target range
- Version catalog (`libs.versions.toml`): no Room, Koin, Navigation, or DataStore yet — all need adding

### Integration Points
- `libs.versions.toml` + `app/build.gradle.kts`: all new dependencies (Room, Koin, Navigation Compose, DataStore) go here
- `MainActivity.kt`: entry point for NavHost wiring
- `settings.gradle.kts`: single-module app — no multi-module changes needed in Phase 1

</code_context>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 01-foundation*
*Context gathered: 2026-04-04*
