# Phase 2: Exercise Library - Context

**Gathered:** 2026-04-04
**Status:** Ready for planning

<domain>
## Phase Boundary

Browse, search, and filter a pre-seeded library of 100–150 exercises. Users can create, edit, and delete custom exercises. The Exercises tab becomes fully functional. Workout plans and logging are separate phases.

</domain>

<decisions>
## Implementation Decisions

### List Layout
- Grouped by muscle group (section headers like "Chest", "Back", "Legs")
- Each row shows: exercise name (large) + muscle group · equipment type (small subtitle)
- Custom exercises show a subtle "Custom" badge/chip next to the name
- When a search query is active, section headers collapse → flat results list
- When no query, chip filter active: keep section headers, filter to matching group only

### Search & Filter UX
- Pinned search bar at top of screen (always visible, tap to type)
- Horizontal scrollable chip row below the search bar: [All] [Chest] [Back] [Legs] ...
- Search filters by exercise name; chip filters by muscle group — both can be active simultaneously
- Search query shows a clear (✕) button to reset; active chip is visually selected

### Custom Exercise Creation
- Floating Action Button (FAB) opens a modal bottom sheet
- Bottom sheet fields: Name (text field), Primary Muscle Group (dropdown), Equipment Type (dropdown)
- [Cancel] and [Save] buttons in the sheet footer
- Long-press on any custom exercise row shows context menu: [✏️ Edit exercise] [🗑️ Delete exercise]
- Edit reuses the same bottom sheet, pre-filled with existing values
- Delete: if exercise has been used in any workout session → block deletion with message "This exercise was used in X sessions. Remove it from all sessions first." No cascade delete.
- Delete: if exercise has never been used → simple confirm dialog, then delete

### Muscle Groups (fixed enum)
Chest, Back, Shoulders, Biceps, Triceps, Forearms, Quads, Hamstrings, Glutes, Calves, Core, Cardio

### Equipment Types (fixed enum)
Barbell, Dumbbell, Cable, Machine, Bodyweight, Kettlebell, Resistance Band, Other

### Seed Data Strategy
- Pre-built SQLite file bundled in `assets/gymtracker_seed.db`
- Room loads it via `createFromAsset("gymtracker_seed.db")` on first install
- All seeded exercises have `is_custom = false`; user-created exercises have `is_custom = true`

### Claude's Discretion
- Exact chip styling and selected state indicator
- Bottom sheet drag handle and height
- Loading state while Room query initializes
- Empty state copy when filters return no results

</decisions>

<specifics>
## Specific Ideas

- No specific design references provided — standard Material3 patterns are fine
- The "block deletion" approach protects workout history integrity (same philosophy as "always store in kg — never corrupt historical data" from Phase 1)

</specifics>

<code_context>
## Existing Code Insights

### Reusable Assets
- `Exercise.kt` entity: already has `name`, `primaryMuscleGroup`, `equipmentType`, `isCustom` — schema matches decisions exactly, no migration needed
- `GymTrackerDatabase.kt`: DAOs declared in Phase 2 — `ExerciseDao` added here
- `AppModule.kt`: Koin module pattern established — `ExerciseRepository` and `ExerciseViewModel` follow the same `single { }` / `viewModel { }` pattern
- `SettingsScreen.kt`: Shows established Compose patterns (collectAsStateWithLifecycle, koinViewModel, Material3 components)

### Established Patterns
- ViewModel → Repository (no use-case layer) — applies to ExerciseViewModel → ExerciseRepository
- Koin DI with modern DSL (`org.koin.core.module.dsl.viewModel`)
- Material3 + Jetpack Compose throughout
- Feature package: `feature/exercises/` — ExercisesScreen.kt is a placeholder ready to be replaced

### Integration Points
- `GymTrackerDatabase.kt`: Add `abstract fun exerciseDao(): ExerciseDao`
- `AppModule.kt`: Register ExerciseRepository and ExerciseViewModel
- `AppNavHost.kt`: ExercisesScreen already wired to bottom nav — Phase 2 replaces the composable body only, no nav changes needed
- `Room.databaseBuilder(...)` in AppModule: add `.createFromAsset("gymtracker_seed.db")` call

</code_context>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 02-exercise-library*
*Context gathered: 2026-04-04*
