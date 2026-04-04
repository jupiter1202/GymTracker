# Phase 3: Workout Plans - Context

**Gathered:** 2026-04-04
**Status:** Ready for planning

<domain>
## Phase Boundary

Users can build their own workout routines or start from proven pre-built programs (PPL, 5x5, nSuns, GZCLP). The Plans tab becomes fully functional: browse plans, create custom plans, view/edit plan exercises, and import free templates. Workout logging (actually running a session) is Phase 4.

</domain>

<decisions>
## Implementation Decisions

### Plans List Screen
- Card-based layout; each card shows: plan name, exercise count · created date (or "Pre-built" for templates), and a row of muscle group chips (e.g. [Chest] [Shoulders] [Triceps]) derived from the plan's exercises
- Pre-built program cards display a "Template" badge
- Screen is split into two sections: "My Plans" (user-created) and "Pre-built Programs" (bundled templates)
- Empty state shows two CTAs: primary "Create plan" button + secondary "Browse templates" link
- FAB (+) opens a modal bottom sheet: Plan name (required) + Description (optional); Save creates the plan and navigates into its detail screen
- Long-press on a custom plan card → context menu: [Edit plan] [Delete plan] — same pattern as Phase 2 exercises
- Templates cannot be deleted (they are read-only until imported as a copy)
- Deleting a custom plan is always allowed, even if workout sessions reference it — planId becomes null via SET_NULL (already wired in the Phase 1 schema); sessions and their history are preserved

### Templates UX
- Pre-built programs bundled in `assets/templates.json` — parsed at runtime, no DB row created until user imports
- "Use this program" creates a new WorkoutPlan + PlanExercise rows in the database — a personal copy the user owns and can edit freely; original template data in JSON is never mutated
- Template programs to include: PPL (Push/Pull/Legs × 3 days), 5x5 (StrongLifts A/B), nSuns, GZCLP
- Tapping a template card opens a preview screen: program name, short description (e.g. "Push Pull Legs — 3 days/week"), scrollable exercise list with target sets × reps, and a [Use this program] button at the bottom

### Plan Detail & Editing
- Tapping a plan card navigates to the plan detail screen: plan name in top bar with an [Edit] button (opens rename sheet), scrollable ordered exercise list with drag handles (☰) per row, sets × reps shown on each row, and a "+ Add exercise" item at the bottom of the list
- "Edit" button in top bar opens a bottom sheet to rename the plan (name + optional description)
- "+ Add exercise" opens an exercise picker bottom sheet — same search bar + muscle filter chip pattern as Phase 2 ExercisesScreen
- After selecting an exercise from the picker, a follow-up sheet prompts for targets: Target sets (number field, default 3) + Target reps (text field, default "8", supports "5", "8-12", "AMRAP"); [Cancel] and [Add to plan] buttons
- Tapping an existing exercise row on the plan detail screen opens the same target sheet pre-filled with current values for editing; [Cancel] and [Save] buttons
- Reorder exercises: drag handles (☰) on each row enable drag-and-drop; orderIndex updated on drop
- Remove exercise: swipe-to-delete on a row

### Claude's Discretion
- Exact drag-and-drop library choice (e.g. `sh.calvin.reorderable` or custom implementation)
- Sheet height and drag handle styling
- Muscle group chip truncation if a plan covers many muscle groups (show top 3, "+N more", etc.)
- Loading and error states for template JSON parsing
- Exact visual styling of "Template" badge on cards

</decisions>

<specifics>
## Specific Ideas

- The template preview screen mirrors the plan detail screen's exercise list layout — consistent "what you'll get" preview before import
- Default sets/reps (3 × 8) reflects a standard hypertrophy entry point; pre-built templates override these with program-specific values
- The two-section layout (My Plans / Pre-built Programs) on the Plans tab makes templates persistently discoverable without a separate screen

</specifics>

<code_context>
## Existing Code Insights

### Reusable Assets
- `WorkoutPlan.kt` entity: id, name, description, createdAt — schema matches all card and sheet fields exactly, no migration needed
- `PlanExercise.kt` entity: id, planId, exerciseId, orderIndex, targetSets (Int), targetReps (String) — supports "5", "8-12", "AMRAP" natively; no migration needed
- `WorkoutSession.planId`: nullable Long with SET_NULL ForeignKey on delete — plan deletion already safe for session history
- `ExercisesScreen.kt`: MUSCLE_GROUPS and EQUIPMENT_TYPES enums, search + filter chip pattern, FAB, ModalBottomSheet, long-press DropdownMenu — all patterns can be reused for exercise picker sheet and plan list interactions
- `ExerciseRepository.kt` + `ExerciseViewModel.kt`: established ViewModel → Repository pattern to follow for WorkoutPlanRepository + WorkoutPlanViewModel
- `AppModule.kt`: Koin `single { }` and modern `viewModel { }` DSL pattern established

### Established Patterns
- Feature package: `feature/plans/` — `PlansScreen.kt` is a placeholder ready to replace
- ViewModel → Repository (no use-case layer), Koin DI, Material3 + Jetpack Compose throughout
- ModalBottomSheet for create/edit actions (Phase 2 pattern)
- LazyColumn with sticky section headers for grouped lists (Phase 2 muscle group grouping)

### Integration Points
- `GymTrackerDatabase.kt`: add `abstract fun workoutPlanDao(): WorkoutPlanDao` and `abstract fun planExerciseDao(): PlanExerciseDao`
- `AppModule.kt`: register WorkoutPlanRepository and WorkoutPlanViewModel
- `AppNavHost.kt`: PlansScreen already wired to bottom nav — replace composable body only, no nav changes needed
- `assets/`: add `templates.json` with PPL, 5x5, nSuns, GZCLP program definitions
- `ExercisesScreen.kt` MUSCLE_GROUPS list: may want to move to a shared `core/` constants file if referenced from the plans feature too

</code_context>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 03-workout-plans*
*Context gathered: 2026-04-04*
