# Phase 2: Exercise Library - Research

**Researched:** 2026-04-04
**Domain:** Android Room DAO + Jetpack Compose (LazyColumn, ModalBottomSheet, FilterChip, ExposedDropdownMenuBox)
**Confidence:** HIGH

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

**List Layout**
- Grouped by muscle group (section headers like "Chest", "Back", "Legs")
- Each row shows: exercise name (large) + muscle group · equipment type (small subtitle)
- Custom exercises show a subtle "Custom" badge/chip next to the name
- When a search query is active, section headers collapse → flat results list
- When no query, chip filter active: keep section headers, filter to matching group only

**Search & Filter UX**
- Pinned search bar at top of screen (always visible, tap to type)
- Horizontal scrollable chip row below the search bar: [All] [Chest] [Back] [Legs] ...
- Search filters by exercise name; chip filters by muscle group — both can be active simultaneously
- Search query shows a clear (X) button to reset; active chip is visually selected

**Custom Exercise Creation**
- Floating Action Button (FAB) opens a modal bottom sheet
- Bottom sheet fields: Name (text field), Primary Muscle Group (dropdown), Equipment Type (dropdown)
- [Cancel] and [Save] buttons in the sheet footer
- Long-press on any custom exercise row shows context menu: [Edit exercise] [Delete exercise]
- Edit reuses the same bottom sheet, pre-filled with existing values
- Delete: if exercise has been used in any workout session → block deletion with message "This exercise was used in X sessions. Remove it from all sessions first." No cascade delete.
- Delete: if exercise has never been used → simple confirm dialog, then delete

**Muscle Groups (fixed enum)**
Chest, Back, Shoulders, Biceps, Triceps, Forearms, Quads, Hamstrings, Glutes, Calves, Core, Cardio

**Equipment Types (fixed enum)**
Barbell, Dumbbell, Cable, Machine, Bodyweight, Kettlebell, Resistance Band, Other

**Seed Data Strategy**
- Pre-built SQLite file bundled in `assets/gymtracker_seed.db`
- Room loads it via `createFromAsset("gymtracker_seed.db")` on first install
- All seeded exercises have `is_custom = false`; user-created exercises have `is_custom = true`

### Claude's Discretion
- Exact chip styling and selected state indicator
- Bottom sheet drag handle and height
- Loading state while Room query initializes
- Empty state copy when filters return no results

### Deferred Ideas (OUT OF SCOPE)
None — discussion stayed within phase scope.
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| EXER-01 | App ships with a pre-seeded library of 100-150 exercises tagged with muscle groups, available on first launch | Room `createFromAsset()` API (Room 2.2+); seed SQLite file in `assets/`; schema must match exported Room schema |
| EXER-02 | User can create custom exercises with a name, primary muscle group, and equipment type | `ExerciseDao.insert()` / `update()` / `delete()`; ModalBottomSheet for creation/edit UI; usage-count check before delete |
| EXER-03 | User can search exercises by name and filter by muscle group | DAO `LIKE` query + optional muscle group param; `combine()` of two StateFlows; `stickyHeader` for grouped/flat display toggling |
</phase_requirements>

---

## Summary

Phase 2 is a pure data-layer + UI phase built entirely on the stack already established in Phase 1. No new dependencies are required beyond what is already declared in `libs.versions.toml`. The three technical pillars are: (1) Room 2.8.4 `createFromAsset()` for seed data, (2) a single DAO that returns `Flow<List<Exercise>>` and accepts optional search/filter parameters, and (3) three Compose components new to this project — `stickyHeader` (experimental, Foundation API), `ModalBottomSheet` (Material 3, stable), and `FilterChip` (Material 3, stable).

The trickiest integration point is the seed database file. Room validates the pre-packaged `.db` file's schema identity hash against the compiled Room schema. The `.db` must be created by Room itself (or WAL-checkpointed from a Room-opened database), not hand-crafted. The correct workflow is: build the app → open the database → copy the `.db` file from the emulator → VACUUM it → place in `assets/`.

The second subtlety is the dual-mode list: when a search query is active the list switches from grouped-with-headers to a flat list. This is a ViewModel concern — the ViewModel groups the list only when `searchQuery.isBlank()`, and the Compose layer receives either a `Map<String, List<Exercise>>` (grouped) or a `List<Exercise>` (flat), switching between two rendering modes.

**Primary recommendation:** Implement the DAO with a single `@Query` that uses `LIKE '%:query%'` and an optional muscle group parameter (null = all groups). Combine two `StateFlow`s in the ViewModel using `combine()` with `flatMapLatest`. Use `@OptIn(ExperimentalFoundationApi::class)` on the screen composable for `stickyHeader`.

---

## Standard Stack

### Core (no new dependencies needed)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| androidx.room:room-runtime | 2.8.4 | Database + DAO + `createFromAsset` | Already in project; native Android persistence |
| androidx.room:room-ktx | 2.8.4 | Flow-returning DAO queries, coroutine support | Already in project; enables `Flow<List<T>>` DAO returns |
| androidx.compose.material3 | BOM 2024.09.00 | `ModalBottomSheet`, `FilterChip`, `ExposedDropdownMenuBox`, `FloatingActionButton` | Already in project; all required components are stable in this version |
| androidx.compose.foundation | BOM 2024.09.00 | `LazyColumn`, `stickyHeader` (experimental) | Already in project |
| io.insert-koin:koin-androidx-compose | 4.1.1 | `koinViewModel()` in `ExercisesScreen` | Already in project; established pattern |
| org.jetbrains.kotlinx:kotlinx-coroutines | (via BOM) | `combine()`, `flatMapLatest`, `stateIn` | Already in project |

**No new `implementation()` lines needed in `build.gradle.kts`.** All required APIs exist in the current dependency graph.

### Seed Data Tooling (dev-time only)

| Tool | Purpose |
|------|---------|
| DB Browser for SQLite (desktop) | Inspect and VACUUM the seed `.db` before placing in `assets/` |
| Android emulator + `adb pull` | Extract the Room-created `.db` to use as seed template |

---

## Architecture Patterns

### Recommended Package Structure

```
feature/exercises/
├── ExercisesScreen.kt          # Replace placeholder body; @OptIn for stickyHeader
├── ExerciseViewModel.kt        # combine() search + filter StateFlows
├── ExerciseRepository.kt       # Delegates to ExerciseDao; no use-case layer
│
core/database/
├── dao/
│   └── ExerciseDao.kt          # @Dao interface with Flow queries
├── entities/
│   └── Exercise.kt             # Already exists — no changes needed
├── GymTrackerDatabase.kt       # Add: abstract fun exerciseDao(): ExerciseDao
│
assets/
└── gymtracker_seed.db          # Pre-built SQLite file (100-150 exercises)
```

### Pattern 1: DAO with Combined Search + Filter

**What:** A single `@Query` that accepts both a `LIKE` search string and an optional muscle group. The "optional" muscle group is handled by `OR :muscleGroup IS NULL`, which returns all rows when null is passed.

**When to use:** Whenever both search and category filter must work simultaneously without multiple DAO methods.

```kotlin
// Source: developer.android.com/training/data-storage/room/accessing-data
@Dao
interface ExerciseDao {
    @Query("""
        SELECT * FROM exercises
        WHERE name LIKE '%' || :query || '%'
        AND (:muscleGroup IS NULL OR primary_muscle_group = :muscleGroup)
        ORDER BY primary_muscle_group ASC, name ASC
    """)
    fun searchExercises(query: String, muscleGroup: String?): Flow<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: Exercise): Long

    @Update
    suspend fun update(exercise: Exercise)

    @Delete
    suspend fun delete(exercise: Exercise)

    @Query("SELECT COUNT(*) FROM workout_sets WHERE exercise_id = :exerciseId")
    suspend fun countUsagesInSessions(exerciseId: Long): Int
}
```

**Key point:** Passing `query = ""` returns all exercises (empty LIKE matches everything via `'%' || '' || '%'` = `'%%'`). Passing `muscleGroup = null` returns all muscle groups.

### Pattern 2: ViewModel with `combine()` for Dual StateFlow Inputs

**What:** Two independent `MutableStateFlow`s (search text, selected muscle group) combined via `combine()` + `flatMapLatest` to produce one display-ready `StateFlow`.

**When to use:** Any screen where multiple independent filter controls feed a single reactive data stream.

```kotlin
// Source: developer.android.com + kotlinx.coroutines docs
class ExerciseViewModel(private val repository: ExerciseRepository) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedMuscleGroup = MutableStateFlow<String?>(null)
    val selectedMuscleGroup: StateFlow<String?> = _selectedMuscleGroup.asStateFlow()

    val exercises: StateFlow<List<Exercise>> = combine(
        _searchQuery, _selectedMuscleGroup
    ) { query, muscleGroup -> Pair(query, muscleGroup) }
        .flatMapLatest { (query, muscleGroup) ->
            repository.searchExercises(query, muscleGroup)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // Grouped map for display when query is blank; flat list otherwise
    val groupedExercises: StateFlow<Map<String, List<Exercise>>> = exercises
        .combine(_searchQuery) { list, query ->
            if (query.isBlank()) list.groupBy { it.primaryMuscleGroup }
            else emptyMap()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun onSearchQueryChange(query: String) { _searchQuery.value = query }
    fun onMuscleGroupSelected(group: String?) { _selectedMuscleGroup.value = group }
    fun clearSearch() { _searchQuery.value = "" }
    // insert/update/delete delegate to repository via viewModelScope.launch
}
```

### Pattern 3: LazyColumn with stickyHeader

**What:** Grouped list when no search active; flat list when search is active. Uses `@OptIn(ExperimentalFoundationApi::class)`.

**When to use:** Any categorized list where the display mode switches based on state.

```kotlin
// Source: developer.android.com/develop/ui/compose/lists
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ExerciseList(
    grouped: Map<String, List<Exercise>>,
    flatList: List<Exercise>,
    isSearchActive: Boolean,
    onLongPress: (Exercise) -> Unit,
) {
    LazyColumn {
        if (isSearchActive || grouped.isEmpty()) {
            // Flat mode — no sticky headers
            items(flatList, key = { it.id }) { exercise ->
                ExerciseRow(exercise, onLongPress)
            }
        } else {
            // Grouped mode — sticky section headers
            grouped.forEach { (muscleGroup, exercises) ->
                stickyHeader(key = muscleGroup) {
                    MuscleGroupHeader(muscleGroup)
                }
                items(exercises, key = { it.id }) { exercise ->
                    ExerciseRow(exercise, onLongPress)
                }
            }
        }
    }
}
```

### Pattern 4: ModalBottomSheet for Create/Edit

**What:** A single bottom sheet composable reused for both create and edit. Controlled by `showBottomSheet: Boolean` and an optional `exerciseToEdit: Exercise?`.

**When to use:** Consistent with Material 3; avoids a separate screen navigation stack for simple forms.

```kotlin
// Source: developer.android.com/develop/ui/compose/components/bottom-sheets
val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
val scope = rememberCoroutineScope()
var showBottomSheet by remember { mutableStateOf(false) }
var exerciseToEdit by remember { mutableStateOf<Exercise?>(null) }

if (showBottomSheet) {
    ModalBottomSheet(
        onDismissRequest = { showBottomSheet = false },
        sheetState = sheetState
    ) {
        ExerciseFormSheet(
            exercise = exerciseToEdit,
            onSave = { name, muscle, equipment ->
                scope.launch {
                    viewModel.saveExercise(name, muscle, equipment, exerciseToEdit)
                    sheetState.hide()
                }.invokeOnCompletion { showBottomSheet = false }
            },
            onCancel = {
                scope.launch { sheetState.hide() }
                    .invokeOnCompletion { showBottomSheet = false }
            }
        )
    }
}
```

### Pattern 5: createFromAsset for Seed Data

**What:** Room copies the pre-built `.db` from `assets/` on first install.

```kotlin
// Source: developer.android.com/training/data-storage/room/prepopulate
Room.databaseBuilder(
    androidContext(),
    GymTrackerDatabase::class.java,
    "gymtracker.db"
)
.createFromAsset("gymtracker_seed.db")
.build()
```

**Critical setup steps:**
1. Build the app and open the DB once (Room creates internal schema hash).
2. `adb pull /data/data/de.jupiter1202.gymtracker/databases/gymtracker.db gymtracker_seed.db`
3. Open in DB Browser for SQLite → insert all 100-150 exercises → File → Write Changes.
4. Run VACUUM on the file (DB Browser: Execute SQL → `VACUUM;`).
5. Place resulting `.db` file in `app/src/main/assets/gymtracker_seed.db`.
6. Do NOT include `-wal` or `-shm` sidecar files; the main `.db` must be fully checkpointed.

### Pattern 6: ExposedDropdownMenuBox for Enum Selection

**What:** Material 3 dropdown for fixed-list enum selection in the bottom sheet form.

```kotlin
// Source: composables.com/material3/exposeddropdownmenubox
var expanded by remember { mutableStateOf(false) }
var selectedMuscle by remember { mutableStateOf(muscleGroups[0]) }

ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded }
) {
    OutlinedTextField(
        value = selectedMuscle,
        onValueChange = {},
        readOnly = true,
        label = { Text("Primary Muscle Group") },
        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        modifier = Modifier.menuAnchor()
    )
    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        muscleGroups.forEach { group ->
            DropdownMenuItem(
                text = { Text(group) },
                onClick = { selectedMuscle = group; expanded = false }
            )
        }
    }
}
```

### Anti-Patterns to Avoid

- **Returning `List<Exercise>` from DAO instead of `Flow<List<Exercise>>`:** You lose reactivity; UI won't update when data changes.
- **Using `collectAsState()` instead of `collectAsStateWithLifecycle()`:** Leaks collection during background/stopped lifecycle states. The project pattern (per SettingsScreen) is `collectAsStateWithLifecycle`.
- **Building the seed `.db` by hand (without Room opening it first):** Room validates the `identity_hash` in `room_master_table`. A manually created schema will not match and will cause an `IllegalStateException` crash.
- **Calling `stickyHeader` without `@OptIn(ExperimentalFoundationApi::class)`:** Compile error; must opt in at the call site or the enclosing composable.
- **Cascade-deleting exercises:** Explicitly blocked by user decision. Always check `countUsagesInSessions()` first and block with an error message if > 0.
- **Storing muscle group / equipment type as Room enums or typed constants:** The entity uses `String` for both fields; keep them as plain strings matching the fixed enum lists. Avoids a schema migration when adding values later.

---

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Reactive filtered list | Manual list filtering in UI layer | Room `Flow<List<T>>` + `combine()` + `flatMapLatest` | Room re-emits on every DB write automatically; manual filter misses concurrent writes |
| "Optional" filter parameter in SQL | Multiple DAO methods (one per filter combo) | `OR :param IS NULL` in a single `@Query` | Combinatorial explosion with 2+ filters; Room compiles and validates the single query |
| Seed data insertion | Insert rows at app startup in code | `createFromAsset()` with pre-built `.db` | Startup insert blocks main thread; asset copy is atomic and fast |
| Custom bottom sheet | `Dialog` + manual slide animation | `ModalBottomSheet` (Material 3) | Handles drag-to-dismiss, scrim, keyboard insets, and accessibility out of the box |
| Sticky headers | `nestedScroll` + `Box` offset tricks | `LazyColumn.stickyHeader {}` | The experimental API handles z-index, scroll tracking, and key stability correctly |
| CRUD validation | Try-catch in UI | `suspend fun` in repository + `viewModelScope.launch` with result callbacks | Keeps error state in ViewModel, not scattered in composables |

---

## Common Pitfalls

### Pitfall 1: Room identity_hash mismatch with seed database
**What goes wrong:** App crashes at startup with `IllegalStateException: Pre-packaged database has an invalid schema`.
**Why it happens:** Room computes a hash of the entire schema at compile time and stores it in `room_master_table`. If the seed `.db` was built without Room (or with a stale schema), the hashes differ.
**How to avoid:** Always generate the seed file from an actual Room-opened database. Export schema first (`exportSchema = true`, already set), use that as reference to populate the `.db`, then open the `.db` with Room once on an emulator before copying it.
**Warning signs:** Crash only on fresh install, passes on existing installs.

### Pitfall 2: WAL mode seed file leaves data invisible
**What goes wrong:** Exercises are missing on first launch even though the `.db` file is present.
**Why it happens:** SQLite in WAL mode keeps recent writes in a `-wal` sidecar file. If you copy the `.db` without checkpointing, the asset is missing those rows.
**How to avoid:** Run `PRAGMA wal_checkpoint(FULL);` or `VACUUM;` on the seed file before placing it in assets. DB Browser for SQLite does this when you "Write Changes".
**Warning signs:** Exercise count is 0 or lower than expected on first launch; rows appear after a second app start.

### Pitfall 3: `stickyHeader` missing `@OptIn` causes compile error
**What goes wrong:** Build fails: `This declaration is experimental and its usage must be marked with '@OptIn(ExperimentalFoundationApi::class)'`.
**Why it happens:** `stickyHeader` is still marked `@ExperimentalFoundationApi` as of Compose BOM 2024.09.00.
**How to avoid:** Add `@OptIn(ExperimentalFoundationApi::class)` to the composable that calls `stickyHeader`.
**Warning signs:** Compile-time error, not runtime.

### Pitfall 4: `flatMapLatest` not cancelling stale DB queries
**What goes wrong:** Rapid search-query changes cause multiple concurrent Room queries; results arrive out-of-order.
**Why it happens:** Using `switchMap` or `map` instead of `flatMapLatest`. `flatMapLatest` cancels the previous upstream Flow when a new value arrives.
**How to avoid:** Use `flatMapLatest` (not `flatMap`, not `map`) in the ViewModel when the input StateFlow drives a new DAO call.
**Warning signs:** Search results flicker or show stale data while typing.

### Pitfall 5: Delete without usage check
**What goes wrong:** Deleting a custom exercise that has been used in a past `WorkoutSet` silently removes it, corrupting history display in Phase 5.
**Why it happens:** Standard `@Delete` has no awareness of foreign key references.
**How to avoid:** Always call `countUsagesInSessions(exercise.id)` before deleting. Block deletion and show the error message if count > 0. `workout_sets` has a foreign key on `exercise_id` — but the FK action is `SET_NULL` (per Phase 1 schema), so Room won't throw an error; the business rule must be enforced in application code.
**Warning signs:** Past workout sessions show null/missing exercise names.

### Pitfall 6: `ExposedDropdownMenuBox` missing `menuAnchor` modifier
**What goes wrong:** Dropdown does not open, or opens at the wrong position.
**Why it happens:** The `OutlinedTextField` inside `ExposedDropdownMenuBox` must have `.menuAnchor()` modifier applied so the box knows where to anchor the dropdown.
**How to avoid:** Always add `modifier = Modifier.menuAnchor()` to the text field inside `ExposedDropdownMenuBox`.
**Warning signs:** Dropdown appears at (0,0) or never appears.

---

## Code Examples

### ExerciseDao — Complete Interface
```kotlin
// Source: developer.android.com/training/data-storage/room/accessing-data
@Dao
interface ExerciseDao {
    @Query("""
        SELECT * FROM exercises
        WHERE name LIKE '%' || :query || '%'
        AND (:muscleGroup IS NULL OR primary_muscle_group = :muscleGroup)
        ORDER BY primary_muscle_group ASC, name ASC
    """)
    fun searchExercises(query: String, muscleGroup: String?): Flow<List<Exercise>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(exercise: Exercise): Long

    @Update
    suspend fun update(exercise: Exercise)

    @Delete
    suspend fun delete(exercise: Exercise)

    @Query("SELECT COUNT(*) FROM workout_sets WHERE exercise_id = :exerciseId")
    suspend fun countUsagesInSessions(exerciseId: Long): Int
}
```

### GymTrackerDatabase — Adding the DAO
```kotlin
// Source: existing GymTrackerDatabase.kt + developer.android.com/training/data-storage/room
@Database(
    entities = [Exercise::class, WorkoutPlan::class, PlanExercise::class,
                WorkoutSession::class, WorkoutSet::class, BodyMeasurement::class],
    version = 1,
    exportSchema = true
)
abstract class GymTrackerDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
}
```

### AppModule — Adding createFromAsset + DI registrations
```kotlin
// Source: existing AppModule.kt + developer.android.com/training/data-storage/room/prepopulate
val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            GymTrackerDatabase::class.java,
            "gymtracker.db"
        )
        .createFromAsset("gymtracker_seed.db")
        .build()
    }
    single { get<GymTrackerDatabase>().exerciseDao() }
    single { androidContext().dataStore }
    single { SettingsRepository(get()) }
    single { ExerciseRepository(get()) }
    viewModel { SettingsViewModel(get()) }
    viewModel { ExerciseViewModel(get()) }
}
```

### FilterChip Row
```kotlin
// Source: composables.com/material3/filterchip
val muscleGroups = listOf("All", "Chest", "Back", "Shoulders", /* … */)

LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    items(muscleGroups) { group ->
        val isSelected = if (group == "All") selectedGroup == null
                         else selectedGroup == group
        FilterChip(
            selected = isSelected,
            onClick = {
                viewModel.onMuscleGroupSelected(if (group == "All") null else group)
            },
            label = { Text(group) }
        )
    }
}
```

---

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| LiveData-returning DAO | Flow-returning DAO | Room 2.2 (2019), Flow stable | No LiveData wrapper needed; works directly with `collectAsStateWithLifecycle` |
| `RoomAsset` third-party library | Built-in `createFromAsset()` | Room 2.2 (2019) | No extra dependency; schema validation built in |
| `ModalBottomSheetLayout` (Material 2) | `ModalBottomSheet` (Material 3) | Compose Material 3 1.1 (2023) | Different API, state management changed — use M3 version throughout |
| `LazyListState` + manual offset for sticky headers | `stickyHeader {}` in `LazyListScope` | Compose 1.1 (2022), still experimental | Correct z-index handling, key-based recomposition; still needs `@OptIn` |
| `collectAsState()` | `collectAsStateWithLifecycle()` | lifecycle-runtime-compose 2.6 | Stops collection when UI is backgrounded; reduces battery/CPU |

**Deprecated/outdated:**
- `org.koin.androidx.viewmodel.dsl.viewModel`: Deprecated in Koin 4.x; already using modern DSL `org.koin.core.module.dsl.viewModel` (Phase 1 decision).
- `KAPT`: Blocked by AGP 9.x for KSP-capable libraries; already using `ksp()` (Phase 1 decision).

---

## Open Questions

1. **Seed database identity hash**
   - What we know: Room validates `room_master_table.identity_hash` in the seed file. The hash is derived from the compiled schema.
   - What's unclear: The exact identity hash value is only known after building the app once. It cannot be pre-computed.
   - Recommendation: Wave 0 of Phase 2 should include a task to build, run, and extract the identity hash before inserting seed data. The seed-building task must come after the DAO and database compile successfully.

2. **`workout_sets.exercise_id` FK action on delete**
   - What we know: Phase 1 schema uses `SET_NULL` on `exercise_id` in `workout_sets` (per STATE.md decisions).
   - What's unclear: The exact FK definition needs verification in the WorkoutSet entity before the delete-guard logic is written.
   - Recommendation: Read `WorkoutSet.kt` at plan-time to confirm the FK action is `SET_NULL`; the business-rule guard must be application-level since Room will not error on delete.

3. **`LIKE '%' || :query || '%'` with empty string**
   - What we know: SQLite `'%%'` matches all rows. Passing `query = ""` returns the full list.
   - What's unclear: Whether Room's parameter binding handles this identically across all Android API levels (min SDK 29 in this project).
   - Recommendation: HIGH confidence this works correctly on API 29+ (SQLite 3.28+). The DAO test should include a case for `query = ""`.

---

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit4 + AndroidJUnit4 (instrumented), JUnit4 (unit) |
| Config file | None — standard Android test runner via `testInstrumentationRunner` in `build.gradle.kts` |
| Quick run command | `./gradlew testDebugUnitTest` (unit) |
| Instrumented command | `./gradlew connectedDebugAndroidTest` (requires emulator/device) |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|--------------|
| EXER-01 | `searchExercises("", null)` returns >= 100 results on first launch | Instrumented (androidTest) | `./gradlew connectedDebugAndroidTest` | ❌ Wave 0 |
| EXER-01 | All seeded exercises have `isCustom = false` | Instrumented (androidTest) | `./gradlew connectedDebugAndroidTest` | ❌ Wave 0 |
| EXER-02 | Insert custom exercise, appears in `searchExercises("", null)` | Instrumented (androidTest) | `./gradlew connectedDebugAndroidTest` | ❌ Wave 0 |
| EXER-02 | Delete blocked when `countUsagesInSessions > 0` | Unit (ExerciseRepository logic) | `./gradlew testDebugUnitTest` | ❌ Wave 0 |
| EXER-03 | `searchExercises("bench", null)` returns only bench-related exercises | Instrumented (androidTest) | `./gradlew connectedDebugAndroidTest` | ❌ Wave 0 |
| EXER-03 | `searchExercises("", "Chest")` returns only Chest exercises | Instrumented (androidTest) | `./gradlew connectedDebugAndroidTest` | ❌ Wave 0 |
| EXER-03 | `searchExercises("press", "Chest")` combined filter works | Instrumented (androidTest) | `./gradlew connectedDebugAndroidTest` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew testDebugUnitTest` (unit tests only, < 30 seconds)
- **Per wave merge:** `./gradlew connectedDebugAndroidTest` (requires connected device)
- **Phase gate:** Full instrumented suite green before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `app/src/androidTest/java/de/jupiter1202/gymtracker/feature/exercises/ExerciseDaoTest.kt` — covers EXER-01, EXER-02, EXER-03 DAO layer
- [ ] `app/src/test/java/de/jupiter1202/gymtracker/feature/exercises/ExerciseRepositoryTest.kt` — covers delete-guard logic (EXER-02)

*(Existing `GymTrackerDatabaseTest.kt` and `SettingsRepositoryTest.kt` demonstrate the established patterns for both test types.)*

---

## Sources

### Primary (HIGH confidence)
- [developer.android.com/training/data-storage/room/prepopulate](https://developer.android.com/training/data-storage/room/prepopulate) — `createFromAsset()` API, schema validation, WAL/checkpoint requirements
- [developer.android.com/training/data-storage/room/accessing-data](https://developer.android.com/training/data-storage/room/accessing-data) — DAO `@Query` patterns, `LIKE` parameter binding, `Flow` return types
- [developer.android.com/develop/ui/compose/components/bottom-sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets) — `ModalBottomSheet`, `rememberModalBottomSheetState`, `SheetState.hide()` pattern
- [developer.android.com/develop/ui/compose/lists](https://developer.android.com/develop/ui/compose/lists) — `stickyHeader`, `@OptIn(ExperimentalFoundationApi::class)`, grouped list pattern

### Secondary (MEDIUM confidence)
- [composables.com/material3/exposeddropdownmenubox](https://composables.com/material3/exposeddropdownmenubox) — `ExposedDropdownMenuBox` + `menuAnchor()` modifier usage
- [composables.com/material3/filterchip](https://composables.com/docs/androidx.compose.material3/material3/components/FilterChip) — `FilterChip` `selected` / `onClick` API
- [medium.com/androiddevelopers/room-flow](https://medium.com/androiddevelopers/room-flow-273acffe5b57) — `Flow` from Room DAO, `distinctUntilChanged` pattern

### Tertiary (LOW confidence)
- Community articles on `combine()` + `flatMapLatest` ViewModel pattern — matches official coroutines docs but not directly cited from an authoritative page

---

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries are already in the project; no version uncertainty
- Architecture (DAO pattern, ViewModel combine): HIGH — verified against official Android docs
- Seed data workflow: HIGH — official `createFromAsset` docs, pitfalls verified against multiple sources
- `stickyHeader` experimental status: HIGH — confirmed still experimental in Compose BOM 2024.09.00
- `ExposedDropdownMenuBox` `menuAnchor()` pitfall: MEDIUM — from composables.com (not official Android docs), cross-referenced with common issue reports

**Research date:** 2026-04-04
**Valid until:** 2026-07-04 (stable APIs; Compose BOM and Room are updated frequently but API surface is stable)
