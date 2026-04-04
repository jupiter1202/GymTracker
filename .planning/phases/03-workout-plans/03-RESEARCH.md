# Phase 3: Workout Plans - Research

**Researched:** 2026-04-04
**Domain:** Jetpack Compose / Room / Drag-and-Drop / JSON asset parsing
**Confidence:** HIGH

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Card-based layout for Plans list; each card shows plan name, exercise count, created date (or "Pre-built"), muscle group chips (top 3 + "+N more")
- Two-section layout: "My Plans" (user-created) and "Pre-built Programs" (bundled templates)
- Empty state: primary "Create plan" button + secondary "Browse templates" link
- FAB (+) opens modal bottom sheet: Plan name (required) + Description (optional); Save creates plan and navigates to detail
- Long-press on custom plan card → DropdownMenu: [Edit plan] [Delete plan] (same Phase 2 pattern)
- Templates are read-only; cannot be deleted
- Deleting a custom plan is always allowed; planId becomes null via SET_NULL — sessions and history preserved
- Pre-built programs bundled in `assets/templates.json` — no DB row created until user imports
- "Use this program" creates WorkoutPlan + PlanExercise rows — user owns the copy, JSON is never mutated
- Template programs: PPL (Push/Pull/Legs × 3 days), 5x5 (StrongLifts A/B), nSuns, GZCLP
- Template card tap → preview screen: name, description, exercise list, [Use this program] button
- Plan detail screen: plan name in top bar with [Edit] button (opens rename sheet)
- Scrollable ordered exercise list with drag handles (☰) per row, sets × reps on each row
- "+ Add exercise" at bottom opens exercise picker bottom sheet (same search + filter chip pattern as Phase 2)
- After selecting exercise: follow-up sheet prompts Target sets (default 3) + Target reps (default "8", supports "5", "8-12", "AMRAP")
- Tapping an existing exercise row: same target sheet pre-filled with current values for editing
- Reorder: drag handles (☰) enable drag-and-drop; orderIndex updated on drop
- Remove exercise: swipe-to-delete

### Claude's Discretion
- Exact drag-and-drop library choice (e.g. `sh.calvin.reorderable` or custom implementation)
- Sheet height and drag handle styling
- Muscle group chip truncation (show top 3, "+N more", etc.)
- Loading and error states for template JSON parsing
- Exact visual styling of "Template" badge on cards

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| PLAN-01 | User can create custom workout plans with unlimited routines | WorkoutPlanDao CRUD + WorkoutPlanRepository + ViewModel pattern mirrors ExerciseRepository exactly; no artificial limits |
| PLAN-02 | App includes pre-built program templates (PPL, 5x5, nSuns, GZCLP) available for free | templates.json in assets/ parsed via kotlinx.serialization; "Use this program" copies to DB as user-owned plan |
| PLAN-03 | User can edit existing workout plans (add/remove/reorder exercises) | PlanExerciseDao handles add/delete/update; reorderable library handles drag; swipe-to-delete via SwipeToDismissBox |
</phase_requirements>

---

## Summary

Phase 3 builds the Plans feature on top of the fully-wired Room schema from Phase 1 — `WorkoutPlan` and `PlanExercise` entities exist and compile, only the DAOs and UI are missing. The architecture exactly mirrors the Phase 2 Exercise Library: two DAOs → two repositories → one ViewModel → one feature package. No schema migration is needed.

The two technically novel areas vs. Phase 2 are (1) drag-and-drop reordering and (2) JSON asset parsing for bundled templates. Both have well-maintained library solutions: `sh.calvin.reorderable` for drag-and-drop (v3.0.0, active) and `kotlinx.serialization` for JSON (already ships with Kotlin compiler plugin; zero new annotation processors needed). Navigation gains one new non-bottom-nav destination for plan detail and one for template preview, using the existing `navigation-compose` setup.

The plan detail screen is the most complex screen in the app so far: it combines a multi-layer bottom sheet stack (exercise picker → target input), drag-and-drop, and swipe-to-dismiss. Each interaction is independently scoped to a known Compose pattern, so complexity is additive rather than multiplicative.

**Primary recommendation:** Use `sh.calvin.reorderable:reorderable:3.0.0` for drag-and-drop; use `kotlinx.serialization-json` for template JSON; follow the exact ExerciseRepository / ExerciseViewModel pattern for WorkoutPlanRepository / WorkoutPlanViewModel.

---

## Standard Stack

### Core (already in project)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Room (runtime + ktx + compiler) | 2.8.4 | WorkoutPlanDao, PlanExerciseDao | Already in use; entities declared in Phase 1 |
| Jetpack Compose Material3 | BOM 2024.09.00 | All UI: cards, sheets, chips, FAB | Already in use throughout app |
| navigation-compose | 2.9.7 | New plan detail + template preview routes | Already in use in AppNavHost |
| Koin (android + compose) | 4.1.1 | DI for new repositories + ViewModel | Already in use with modern DSL |
| kotlinx-coroutines | 1.9.0 | Flow, viewModelScope, suspend functions | Already in use |

### New additions
| Library | Version | Purpose | Why |
|---------|---------|---------|-----|
| sh.calvin.reorderable:reorderable | 3.0.0 | Drag-and-drop reorder in LazyColumn | Best maintained Compose-native drag library; uses `Modifier.animateItem` API; supports drag handles natively; no view-system dependencies |
| kotlinx.serialization-json | 1.7.x (via Kotlin BOM) | Parse templates.json from assets | Code-gen based (no reflection), Kotlin-first, already available via Kotlin plugin — no new annotation processor |

**Note on kotlinx.serialization:** The `org.jetbrains.kotlin.plugin.serialization` Gradle plugin must be added. Check whether it is already declared alongside `kotlin-compose`; if not, add it to `build.gradle.kts` plugins block. No KSP configuration needed — it is a compiler plugin.

**Installation (new dependencies only):**
```kotlin
// libs.versions.toml additions
[versions]
reorderable = "3.0.0"

[libraries]
reorderable = { module = "sh.calvin.reorderable:reorderable", version.ref = "reorderable" }

// if kotlinx.serialization not already present:
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version = "1.7.3" }

[plugins]
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

```kotlin
// app/build.gradle.kts
plugins {
    // ... existing plugins
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.reorderable)
    implementation(libs.kotlinx.serialization.json)
}
```

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| sh.calvin.reorderable | Custom drag-and-drop via `detectDragGesturesAfterLongPress` | Custom solution needs ~200 lines, no edge-scroll, no animation; not worth it when library solves it correctly |
| kotlinx.serialization | Gson or Moshi | Gson has no Kotlin null-safety; Moshi needs KSP/kapt — another annotation processor in a project already using KSP for Room. kotlinx.serialization uses the same Kotlin compiler pass, no extra processor |
| kotlinx.serialization | Manual `JSONObject` parsing | Fragile; no type safety; lots of boilerplate for 4 template programs |

---

## Architecture Patterns

### Recommended Project Structure
```
feature/plans/
├── PlansScreen.kt           # Plans list (My Plans + Pre-built sections)
├── PlanDetailScreen.kt      # Ordered exercise list with drag + swipe-delete
├── TemplatePreviewScreen.kt # Template preview before import
├── WorkoutPlanViewModel.kt  # Single ViewModel for all plans screens
└── WorkoutPlanRepository.kt # DB operations + template JSON loading

core/database/dao/
├── WorkoutPlanDao.kt        # CRUD for workout_plans table
└── PlanExerciseDao.kt       # CRUD + reorder for plan_exercises table

core/database/
└── GymTrackerDatabase.kt    # Add abstract fun workoutPlanDao() + planExerciseDao()

core/constants/
└── MuscleGroups.kt          # Move MUSCLE_GROUPS list here (shared between exercises + plans exercise picker)

assets/
└── templates.json           # PPL, 5x5, nSuns, GZCLP definitions
```

### Pattern 1: Two-DAO Repository (mirrors ExerciseRepository)
**What:** WorkoutPlanRepository wraps WorkoutPlanDao + PlanExerciseDao; sealed DeleteResult not needed (plans are always deleteable).
**When to use:** All plan and plan-exercise mutations go through the repository.

```kotlin
// Source: mirrors ExerciseRepository.kt pattern
class WorkoutPlanRepository(
    private val planDao: WorkoutPlanDao,
    private val planExerciseDao: PlanExerciseDao
) {
    fun getPlans(): Flow<List<WorkoutPlan>> = planDao.getAllPlans()

    suspend fun createPlan(name: String, description: String?): Long {
        val plan = WorkoutPlan(
            name = name,
            description = description,
            createdAt = System.currentTimeMillis()
        )
        return planDao.insert(plan)
    }

    suspend fun deletePlan(plan: WorkoutPlan) = planDao.delete(plan)

    fun getPlanExercises(planId: Long): Flow<List<PlanExerciseWithExercise>> =
        planExerciseDao.getExercisesForPlan(planId)

    suspend fun addExercise(planId: Long, exerciseId: Long, sets: Int, reps: String) {
        val nextIndex = planExerciseDao.getMaxOrderIndex(planId) + 1
        planExerciseDao.insert(
            PlanExercise(
                planId = planId, exerciseId = exerciseId,
                orderIndex = nextIndex, targetSets = sets, targetReps = reps
            )
        )
    }

    suspend fun reorderExercises(exercises: List<PlanExercise>) {
        exercises.forEachIndexed { idx, ex ->
            planExerciseDao.update(ex.copy(orderIndex = idx))
        }
    }

    suspend fun removeExercise(planExercise: PlanExercise) = planExerciseDao.delete(planExercise)
}
```

### Pattern 2: WorkoutPlanDao
```kotlin
// Source: Room docs + established ExerciseDao pattern
@Dao
interface WorkoutPlanDao {
    @Query("SELECT * FROM workout_plans ORDER BY created_at DESC")
    fun getAllPlans(): Flow<List<WorkoutPlan>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(plan: WorkoutPlan): Long

    @Update
    suspend fun update(plan: WorkoutPlan)

    @Delete
    suspend fun delete(plan: WorkoutPlan)
}
```

### Pattern 3: PlanExerciseDao with JOIN
```kotlin
// Source: Room JOIN + @Embedded/@Relation pattern
data class PlanExerciseWithExercise(
    @Embedded val planExercise: PlanExercise,
    @Relation(
        parentColumn = "exercise_id",
        entityColumn = "id"
    )
    val exercise: Exercise
)

@Dao
interface PlanExerciseDao {
    @Transaction
    @Query("""
        SELECT * FROM plan_exercises
        WHERE plan_id = :planId
        ORDER BY order_index ASC
    """)
    fun getExercisesForPlan(planId: Long): Flow<List<PlanExerciseWithExercise>>

    @Query("SELECT COALESCE(MAX(order_index), -1) FROM plan_exercises WHERE plan_id = :planId")
    suspend fun getMaxOrderIndex(planId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(planExercise: PlanExercise): Long

    @Update
    suspend fun update(planExercise: PlanExercise)

    @Delete
    suspend fun delete(planExercise: PlanExercise)
}
```

**Critical:** `@Transaction` is required on queries returning `@Relation` results to avoid race conditions when Room fetches the joined entity.

### Pattern 4: Drag-and-Drop with sh.calvin.reorderable
```kotlin
// Source: https://github.com/Calvin-LL/Reorderable (v3.0.0)
@Composable
fun PlanExerciseList(
    exercises: List<PlanExerciseWithExercise>,
    onReorder: (List<PlanExercise>) -> Unit
) {
    var localList by remember(exercises) { mutableStateOf(exercises) }
    val lazyListState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localList = localList.toMutableList().apply { add(to.index, removeAt(from.index)) }
        onReorder(localList.map { it.planExercise })
    }

    LazyColumn(state = lazyListState) {
        items(localList, key = { it.planExercise.id }) { item ->
            ReorderableItem(reorderState, key = item.planExercise.id) { isDragging ->
                ExerciseRow(
                    item = item,
                    dragHandle = {
                        IconButton(modifier = Modifier.draggableHandle()) {
                            Icon(Icons.Default.DragHandle, contentDescription = "Reorder")
                        }
                    }
                )
            }
        }
    }
}
```

**Key:** Pass `key` to both `items()` and `ReorderableItem()` — must match. Use a stable `Long` id, not position index.

### Pattern 5: Template JSON parsing from assets
```kotlin
// Source: kotlinx.serialization docs + Android assets pattern
@Serializable
data class TemplateProgram(
    val id: String,
    val name: String,
    val description: String,
    val days: List<TemplateDay>
)

@Serializable
data class TemplateDay(
    val name: String,
    val exercises: List<TemplateExercise>
)

@Serializable
data class TemplateExercise(
    @SerialName("exercise_name") val exerciseName: String,
    @SerialName("target_sets") val targetSets: Int,
    @SerialName("target_reps") val targetReps: String
)

// In Repository or ViewModel initialization:
suspend fun loadTemplates(context: Context): List<TemplateProgram> =
    withContext(Dispatchers.IO) {
        val json = context.assets.open("templates.json")
            .bufferedReader()
            .use { it.readText() }
        Json.decodeFromString<List<TemplateProgram>>(json)
    }
```

**Template import (PLAN-02):**
When user taps "Use this program", iterate template exercises, look up each exercise by name in the Exercise table (or create if missing), then bulk-insert PlanExercise rows with `@Transaction`.

### Pattern 6: Navigation to Plan Detail
The current `AppNavHost` only has bottom-nav routes. Plan detail and template preview need new routes with `planId` argument:

```kotlin
// AppNavHost.kt additions
composable(
    route = "plan_detail/{planId}",
    arguments = listOf(navArgument("planId") { type = NavType.LongType })
) { backStackEntry ->
    val planId = backStackEntry.arguments?.getLong("planId") ?: return@composable
    PlanDetailScreen(planId = planId, onNavigateBack = { navController.popBackStack() })
}

composable(
    route = "template_preview/{templateId}",
    arguments = listOf(navArgument("templateId") { type = NavType.StringType })
) { backStackEntry ->
    val templateId = backStackEntry.arguments?.getString("templateId") ?: return@composable
    TemplatePreviewScreen(templateId = templateId, onNavigateBack = { navController.popBackStack() })
}
```

`PlansScreen` needs to receive `navController` (or navigate callbacks) — currently it takes no parameters.

### Pattern 7: Swipe-to-Delete (SwipeToDismissBox)
```kotlin
// Source: Material3 SwipeToDismissBox (M3 1.2+, already available via BOM)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteExerciseRow(
    item: PlanExerciseWithExercise,
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        }
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = { /* Red delete background */ },
        content = { content() }
    )
}
```

**Combine with reorderable:** Wrap `SwipeToDismissBox` inside `ReorderableItem`, not the other way around. Drag handle intercepts touch before swipe gesture.

### Anti-Patterns to Avoid
- **Reorder by re-inserting rows:** Never delete and re-insert all PlanExercise rows to reorder. Update only the `order_index` column via `@Update`. Room will emit a new Flow only for changed rows.
- **Parsing templates.json on main thread:** Always parse in `Dispatchers.IO` inside a coroutine. File I/O on main thread triggers StrictMode violations.
- **Storing template data in DB upfront:** CONTEXT.md specifies no DB row until user imports. Do not seed templates into the database at startup.
- **Sharing ViewModel across nav destinations with koinViewModel():** `PlanDetailScreen` needs its own ViewModel instance scoped to the `planId`. Pass `planId` as a constructor parameter via Koin's `parametersOf`.
- **Keying LazyColumn items by position:** Always use stable entity IDs as keys for drag-and-drop. Position-based keys break reorder animations.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Drag-and-drop reorder | Custom `detectDragGesturesAfterLongPress` + position math | `sh.calvin.reorderable:reorderable:3.0.0` | Edge-scroll, animated item movement, haptics, stable key handling — ~5 lines vs ~200+ lines of fragile custom code |
| JSON parsing | Manual `JSONObject` / `JSONArray` traversal | `kotlinx.serialization` | Type-safe, Kotlin null-safe, code-gen (no reflection overhead) |
| Swipe-to-delete | Custom touch handling overlay | `SwipeToDismissBox` (Material3) | Already in the M3 BOM; handles gesture conflicts with scroll |
| JOIN queries | Multiple separate queries + in-memory join | Room `@Relation` + `@Transaction` | Room handles consistency; separate queries have TOCTOU gaps |

---

## Common Pitfalls

### Pitfall 1: Missing @Transaction on @Relation queries
**What goes wrong:** `PlanExerciseWithExercise` is fetched in two separate queries (parent + related entity). Without `@Transaction`, the exercise entity can be deleted between the two reads, causing a null child or data inconsistency.
**Why it happens:** Room's `@Relation` expands to two SQL queries at runtime.
**How to avoid:** Always annotate the DAO method returning `@Relation` types with `@Transaction`.
**Warning signs:** Random `null` exercise references in plan detail list.

### Pitfall 2: Navigation destination receives no planId
**What goes wrong:** `PlansScreen` taps a card but the `PlanDetailScreen` composable receives id=0 or crashes.
**Why it happens:** Current `AppNavHost.kt` has no parameterized routes. The Plans tab composable takes no arguments.
**How to avoid:** Add `"plan_detail/{planId}"` route with `NavType.LongType` argument. Pass `navController` down to `PlansScreen` or use a navigate callback. Verify `backStackEntry.arguments?.getLong("planId")` before using.
**Warning signs:** Crash on navigation or plan detail always shows empty.

### Pitfall 3: Reorderable + Swipe gesture conflict
**What goes wrong:** Swiping to delete accidentally triggers drag-and-drop (or vice versa).
**Why it happens:** Both gestures compete for the same horizontal/vertical touch events if stacked incorrectly.
**How to avoid:** Place `SwipeToDismissBox` as the content of `ReorderableItem`. The drag handle (`Modifier.draggableHandle()`) only captures touch on the handle icon, so swipe gestures on the rest of the row reach `SwipeToDismissBox` normally.
**Warning signs:** Swipe gesture starts a drag; dragging triggers dismissal.

### Pitfall 4: Template exercise name lookup fails
**What goes wrong:** "Use this program" silently creates PlanExercise rows with exerciseId=0 because the template exercise name doesn't exactly match the seeded exercise name.
**Why it happens:** Template JSON uses exercise names that differ by spacing, capitalization, or spelling from the seed database.
**How to avoid:** Design `templates.json` exercise names to exactly match seed DB names (case-sensitive). Alternatively, use exercise IDs in the JSON for a stable reference. Validate at template-import time and surface an error if an exercise is not found.
**Warning signs:** Plan detail screen shows exercises with empty names or missing muscle group chips.

### Pitfall 5: orderIndex gaps after delete
**What goes wrong:** After removing an exercise, orderIndex values have gaps (e.g. 0, 1, 3). Subsequent drag-and-drop behaves unexpectedly if positions are assumed contiguous.
**Why it happens:** Deleting a row doesn't compact orderIndex of remaining rows.
**How to avoid:** The reorderable library works from a local in-memory list reindexed 0..N on every drop, then batch-updates DB via `reorderExercises()`. Gaps in DB orderIndex don't matter as long as the UI always reads ORDER BY order_index ASC and reindexes on save.
**Warning signs:** Exercises jump to wrong positions after delete + reorder sequence.

### Pitfall 6: Koin parametersOf for planId-scoped ViewModel
**What goes wrong:** Two plan detail screens share the same ViewModel instance via default Koin scoping, causing state bleed between plans.
**Why it happens:** `koinViewModel()` without parameters returns the same ViewModel from the nearest scope.
**How to avoid:** Use `koinViewModel { parametersOf(planId) }` in `PlanDetailScreen` and accept `planId: Long` in `WorkoutPlanViewModel` constructor (or create a separate scoped ViewModel for plan detail).
**Warning signs:** Navigating back to plans list and opening a different plan shows the previous plan's data.

---

## Code Examples

### WorkoutPlan creation bottom sheet pattern
```kotlin
// Source: mirrors ExerciseFormSheet in ExercisesScreen.kt
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlanSheet(onSave: (name: String, description: String?) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Drag handle (same 32x4dp rounded box as ExerciseFormSheet)
        OutlinedTextField(
            value = name,
            onValueChange = { name = it; nameError = false },
            label = { Text("Plan name") },
            isError = nameError,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (optional)") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
            Button(
                onClick = { if (name.isBlank()) nameError = true else onSave(name.trim(), description.takeIf { it.isNotBlank() }) },
                modifier = Modifier.weight(1f)
            ) { Text("Save") }
        }
        Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.ime))
    }
}
```

### Muscle group chip derivation for plan cards
```kotlin
// Source: derived from ExercisesScreen.kt MUSCLE_GROUPS pattern
fun deriveTopMuscleGroups(exercises: List<PlanExerciseWithExercise>, limit: Int = 3): Pair<List<String>, Int> {
    val groups = exercises
        .map { it.exercise.primaryMuscleGroup }
        .distinct()
    return groups.take(limit) to maxOf(0, groups.size - limit)
}

// In card composable:
val (chips, overflow) = deriveTopMuscleGroups(exercises)
LazyRow { items(chips) { FilterChip(selected = false, onClick = {}, label = { Text(it) }) } }
if (overflow > 0) Text("+$overflow more", style = MaterialTheme.typography.labelSmall)
```

### templates.json structure
```json
[
  {
    "id": "ppl",
    "name": "Push Pull Legs",
    "description": "Push Pull Legs — 3 days/week. Classic hypertrophy split.",
    "days": [
      {
        "name": "Push",
        "exercises": [
          { "exercise_name": "Bench Press", "target_sets": 4, "target_reps": "8-12" },
          { "exercise_name": "Overhead Press", "target_sets": 3, "target_reps": "8-12" },
          { "exercise_name": "Tricep Pushdown", "target_sets": 3, "target_reps": "10-15" }
        ]
      }
    ]
  }
]
```

**Note:** Phase 3 models plans as flat exercise lists (single `PlanExercise` table with `orderIndex`). Multi-day structure from JSON templates must be flattened on import: day name can be prepended to exercise row order or ignored. The current schema has no "day" concept — confirm with planner whether flattened import is acceptable or if a note field should be added to PlanExercise.

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| ItemTouchHelper (View system) | `sh.calvin.reorderable` (Compose-native) | Compose GA 2022+ | No interop bridge needed; handles animateItem natively |
| Gson for JSON parsing | kotlinx.serialization | ~2021 (became stable) | Compile-time code gen, null-safe, no reflection |
| LiveData in ViewModels | StateFlow + collectAsStateWithLifecycle | ~2022 | Already used in this project (ExerciseViewModel) |
| Room LiveData return | Room Flow return | Room 2.2+ | Already used in ExerciseDao |
| `@Relation` with separate queries | `@Transaction` + `@Relation` | Room 2.1+ | Consistency guarantee; already available |

---

## Open Questions

1. **Multi-day template structure vs. flat PlanExercise schema**
   - What we know: `PlanExercise` has no `day` field; templates (PPL) are multi-day programs
   - What's unclear: Should import flatten all days into one sequential list, or is a note/label added per exercise?
   - Recommendation: Flatten on import with no day grouping — simplest approach, consistent with phase scope. Phase 4 (logging) may revisit if needed.

2. **Template exercise name matching**
   - What we know: Templates reference exercises by name string; seed DB has 124 exercises
   - What's unclear: Exact name mapping hasn't been validated
   - Recommendation: Author `templates.json` after Phase 3 implementation begins, when the seed DB can be queried for exact exercise names. Alternatively use exercise IDs as the stable reference key.

3. **Navigation: PlansScreen currently takes no parameters**
   - What we know: `AppNavHost.kt` passes `PlansScreen()` with no arguments; plan detail requires navigation
   - What's unclear: Whether to pass `navController` directly or use a callback lambda pattern
   - Recommendation: Use callback lambdas (`onPlanClick: (Long) -> Unit`) in `PlansScreen` — consistent with Compose navigation best practice of not passing NavController into composables below the NavHost level.

---

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 4 + kotlinx-coroutines-test 1.9.0 |
| Config file | none — standard Android test runner |
| Quick run command | `./gradlew :app:testDebugUnitTest` |
| Full suite command | `./gradlew :app:testDebugUnitTest :app:connectedDebugAndroidTest` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| PLAN-01 | createPlan inserts WorkoutPlan row and returns valid id | unit | `./gradlew :app:testDebugUnitTest --tests "*.WorkoutPlanRepositoryTest"` | ❌ Wave 0 |
| PLAN-01 | deletePlan removes plan without blocking (always allowed) | unit | `./gradlew :app:testDebugUnitTest --tests "*.WorkoutPlanRepositoryTest"` | ❌ Wave 0 |
| PLAN-02 | loadTemplates returns 4 programs (PPL, 5x5, nSuns, GZCLP) | unit | `./gradlew :app:testDebugUnitTest --tests "*.TemplateParserTest"` | ❌ Wave 0 |
| PLAN-02 | importTemplate creates WorkoutPlan + N PlanExercise rows | unit | `./gradlew :app:testDebugUnitTest --tests "*.WorkoutPlanRepositoryTest"` | ❌ Wave 0 |
| PLAN-03 | addExercise appends with correct orderIndex | unit | `./gradlew :app:testDebugUnitTest --tests "*.PlanExerciseRepositoryTest"` | ❌ Wave 0 |
| PLAN-03 | removeExercise deletes the correct PlanExercise row | unit | `./gradlew :app:testDebugUnitTest --tests "*.PlanExerciseRepositoryTest"` | ❌ Wave 0 |
| PLAN-03 | reorderExercises updates all orderIndex values | unit | `./gradlew :app:testDebugUnitTest --tests "*.PlanExerciseRepositoryTest"` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :app:testDebugUnitTest`
- **Per wave merge:** `./gradlew :app:testDebugUnitTest`
- **Phase gate:** Full suite green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `app/src/test/.../feature/plans/WorkoutPlanRepositoryTest.kt` — covers PLAN-01, PLAN-02 import
- [ ] `app/src/test/.../feature/plans/PlanExerciseRepositoryTest.kt` — covers PLAN-03
- [ ] `app/src/test/.../feature/plans/TemplateParserTest.kt` — covers PLAN-02 JSON parsing (uses fake Context or raw JSON string)
- [ ] `app/src/test/assets/templates.json` — test fixture (copy of real templates.json for unit tests)

*(No new framework install needed — JUnit 4 + coroutines-test already declared in build.gradle.kts)*

---

## Sources

### Primary (HIGH confidence)
- Existing codebase (`ExerciseDao.kt`, `ExerciseRepository.kt`, `ExerciseViewModel.kt`, `ExercisesScreen.kt`, `AppModule.kt`, `GymTrackerDatabase.kt`, `PlanExercise.kt`, `WorkoutPlan.kt`) — direct inspection confirms entity schema, DAO pattern, Koin DSL version, navigation structure
- [Calvin-LL/Reorderable GitHub README](https://github.com/Calvin-LL/Reorderable/blob/main/README.md) — v3.0.0, Maven coordinates, LazyColumn usage pattern, `draggableHandle` modifier
- [Kotlin Serialization docs](https://kotlinlang.org/docs/serialization.html) — `@Serializable`, `Json.decodeFromString`, compiler plugin setup

### Secondary (MEDIUM confidence)
- [Room JOIN queries - ProAndroidDev](https://proandroiddev.com/room-database-lessons-learnt-from-working-with-multiple-tables-d499c9be94ce) — `@Transaction` + `@Relation` pattern, confirmed against official Room docs
- [Android Developers - Room DAOs](https://developer.android.com/training/data-storage/room/accessing-data) — Flow return type, async query patterns

### Tertiary (LOW confidence)
- WebSearch results on kotlinx.serialization version 1.7.x being current — cross-verify exact version against Kotlin 2.2.10 compatibility matrix before adding to `libs.versions.toml`

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all new libraries verified against official sources; existing stack directly inspected
- Architecture: HIGH — directly mirrors Phase 2 patterns from live codebase
- Drag-and-drop (discretion area): HIGH — library verified at GitHub source, v3.0.0 confirmed
- JSON parsing: HIGH — kotlinx.serialization is the Kotlin standard; pattern well-documented
- Navigation changes: HIGH — AppNavHost.kt directly inspected; pattern is standard navigation-compose
- Pitfalls: MEDIUM — derived from Room docs + codebase patterns; some pitfalls are proactive (not yet encountered)
- Template exercise name matching: LOW — actual name alignment with seed DB not yet verified

**Research date:** 2026-04-04
**Valid until:** 2026-05-04 (stable libraries; sh.calvin.reorderable releases infrequently)
