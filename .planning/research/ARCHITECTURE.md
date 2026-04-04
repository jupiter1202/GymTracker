
# Architecture Patterns

**Domain:** Android gym/workout tracking app
**Researched:** 2026-04-04
**Confidence:** HIGH (well-established Android architecture patterns; Room + Compose + MVVM is the official Google-recommended stack)

## Recommended Architecture

**Pattern: MVVM with Clean Architecture layers (simplified)**

Three layers, strict dependency direction: UI -> Domain -> Data. No framework imports in domain layer. Single-module project (multi-module is overkill for this scope).

```
+---------------------------------------------------------+
|                    UI Layer (Compose)                     |
|  Screens / Composables / Navigation / ViewModels         |
+---------------------------------------------------------+
           |                              ^
           | calls                        | exposes StateFlow
           v                              |
+---------------------------------------------------------+
|                   Domain Layer                           |
|  Use Cases (optional) / Repository Interfaces / Models   |
+---------------------------------------------------------+
           |                              ^
           | implements                   | returns Flow<T>
           v                              |
+---------------------------------------------------------+
|                    Data Layer                             |
|  Repository Impls / Room DAOs / Entities / Mappers       |
+---------------------------------------------------------+
           |
           v
+---------------------------------------------------------+
|                  Room SQLite Database                     |
+---------------------------------------------------------+
```

**Why this pattern:**
- Google's officially recommended architecture for Android apps
- ViewModel survives configuration changes (screen rotation)
- Room + Flow gives reactive, offline-first data by default
- Clean separation means each layer is independently testable
- For a single-developer FOSS project, the simplified version (skip use cases until you need them) keeps velocity high without sacrificing structure

### Package Structure

```
de.jupiter1202.gymtracker/
  |-- data/
  |     |-- local/
  |     |     |-- GymDatabase.kt           (Room database)
  |     |     |-- dao/
  |     |     |     |-- ExerciseDao.kt
  |     |     |     |-- WorkoutPlanDao.kt
  |     |     |     |-- WorkoutSessionDao.kt
  |     |     |     |-- BodyMeasurementDao.kt
  |     |     |-- entity/
  |     |     |     |-- ExerciseEntity.kt
  |     |     |     |-- WorkoutPlanEntity.kt
  |     |     |     |-- PlanExerciseEntity.kt
  |     |     |     |-- WorkoutSessionEntity.kt
  |     |     |     |-- WorkoutSetEntity.kt
  |     |     |     |-- BodyMeasurementEntity.kt
  |     |     |-- converter/
  |     |           |-- Converters.kt       (TypeConverters)
  |     |-- repository/
  |           |-- ExerciseRepository.kt
  |           |-- WorkoutRepository.kt
  |           |-- MeasurementRepository.kt
  |
  |-- domain/
  |     |-- model/
  |     |     |-- Exercise.kt
  |     |     |-- MuscleGroup.kt
  |     |     |-- WorkoutPlan.kt
  |     |     |-- WorkoutSession.kt
  |     |     |-- WorkoutSet.kt
  |     |     |-- BodyMeasurement.kt
  |     |-- repository/                     (interfaces)
  |           |-- ExerciseRepository.kt
  |           |-- WorkoutRepository.kt
  |           |-- MeasurementRepository.kt
  |
  |-- ui/
  |     |-- navigation/
  |     |     |-- NavGraph.kt
  |     |     |-- Screen.kt                (sealed class of routes)
  |     |-- theme/
  |     |     |-- Theme.kt / Color.kt / Type.kt
  |     |-- screens/
  |     |     |-- exercises/
  |     |     |     |-- ExerciseListScreen.kt
  |     |     |     |-- ExerciseListViewModel.kt
  |     |     |     |-- ExerciseDetailScreen.kt
  |     |     |-- plans/
  |     |     |     |-- PlanListScreen.kt
  |     |     |     |-- PlanListViewModel.kt
  |     |     |     |-- PlanEditorScreen.kt
  |     |     |     |-- PlanEditorViewModel.kt
  |     |     |-- session/
  |     |     |     |-- ActiveSessionScreen.kt
  |     |     |     |-- ActiveSessionViewModel.kt
  |     |     |     |-- SessionHistoryScreen.kt
  |     |     |-- progress/
  |     |     |     |-- ProgressScreen.kt
  |     |     |     |-- ProgressViewModel.kt
  |     |     |-- measurements/
  |     |           |-- MeasurementScreen.kt
  |     |           |-- MeasurementViewModel.kt
  |     |-- components/
  |           |-- SetInputRow.kt
  |           |-- ExerciseCard.kt
  |           |-- ProgressChart.kt
  |           |-- RestTimerDialog.kt
  |
  |-- di/
  |     |-- AppModule.kt                   (manual DI or Hilt)
  |
  |-- GymTrackerApplication.kt
  |-- MainActivity.kt
```

### Component Boundaries

| Component | Responsibility | Communicates With |
|-----------|---------------|-------------------|
| **Room Database** | Persists all data locally as SQLite | DAOs (accessed by repositories only) |
| **DAOs** | Define SQL queries, return `Flow<List<T>>` | Room Database (reads/writes), Repository impls (called by) |
| **Entities** | Room table definitions with annotations | DAOs, Mappers |
| **Repositories (impl)** | Coordinate data access, map entities to domain models | DAOs (calls), Domain models (returns) |
| **Repository interfaces** | Define contracts for data access | Domain layer (owns), Data layer (implements) |
| **Domain Models** | Plain Kotlin data classes, no annotations | Used by ViewModels, Repositories |
| **ViewModels** | Hold UI state as StateFlow, handle user actions | Repositories (calls), UI (exposes state to) |
| **Screens (Composables)** | Render UI, collect state, forward user events | ViewModels (observes/calls), Navigation (navigates) |
| **Navigation** | Manages screen transitions and back stack | Screens (hosts), MainActivity (entry point) |

**Boundary rules:**
- Screens never touch repositories or DAOs directly
- ViewModels never import `androidx.compose.*`
- Repositories never import `androidx.lifecycle.*`
- Domain models are plain data classes -- no Room annotations, no Compose annotations
- Entity-to-model mapping happens in the repository layer

## Data Models and Relationships

### Entity-Relationship Diagram

```
+------------------+       +---------------------+
|    Exercise      |       |    MuscleGroup      |
|------------------|       |---------------------|
| id (PK)          |       | (enum in domain)    |
| name             |       +---------------------+
| muscleGroup      |
| equipmentType    |
| notes            |
| isCustom         |
+--------+---------+
         |
         | 1:N (via PlanExercise)
         v
+---------------------+       +------------------+
|   PlanExercise      |------>|   WorkoutPlan    |
|---------------------|       |------------------|
| id (PK)             |       | id (PK)          |
| planId (FK)         |       | name             |
| exerciseId (FK)     |       | description      |
| orderIndex          |       | createdAt        |
| targetSets          |       | isTemplate       |
| targetReps          |       +--------+---------+
| targetWeight        |                |
+---------------------+                |
                                       | 1:N
                                       v
                              +--------------------+
                              |  WorkoutSession    |
                              |--------------------|
                              | id (PK)            |
                              | planId (FK, null)  |
                              | startedAt          |
                              | completedAt (null) |
                              | notes              |
                              | durationSeconds    |
                              +--------+-----------+
                                       |
                                       | 1:N
                                       v
                              +--------------------+
                              |    WorkoutSet      |
                              |--------------------|
                              | id (PK)            |
                              | sessionId (FK)     |
                              | exerciseId (FK)    |
                              | orderIndex         |
                              | weight             |
                              | reps               |
                              | rpe (nullable)     |
                              | isWarmup           |
                              | completedAt        |
                              +--------------------+

+------------------------+
|   BodyMeasurement      |
|------------------------|
| id (PK)                |
| date                   |
| bodyWeight (nullable)  |
| bodyFat (nullable)     |
| notes                  |
+------------------------+
```

### Key Relationships

| Relationship | Type | Implementation |
|-------------|------|----------------|
| Exercise <-> WorkoutPlan | Many-to-Many | Via `PlanExercise` junction table |
| WorkoutPlan -> WorkoutSession | One-to-Many | `session.planId` FK (nullable for ad-hoc sessions) |
| WorkoutSession -> WorkoutSet | One-to-Many | `set.sessionId` FK |
| Exercise -> WorkoutSet | One-to-Many | `set.exerciseId` FK |
| BodyMeasurement | Standalone | No FK relationships, tracked independently by date |

### Domain Model Details

```kotlin
// -- Exercise --
data class Exercise(
    val id: Long = 0,
    val name: String,
    val muscleGroup: MuscleGroup,
    val equipmentType: EquipmentType,
    val notes: String = "",
    val isCustom: Boolean = true
)

enum class MuscleGroup {
    CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS,
    QUADRICEPS, HAMSTRINGS, GLUTES, CALVES,
    ABS, FULL_BODY, CARDIO, OTHER
}

enum class EquipmentType {
    BARBELL, DUMBBELL, MACHINE, CABLE, BODYWEIGHT,
    KETTLEBELL, BAND, OTHER
}

// -- Workout Plan --
data class WorkoutPlan(
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Instant,
    val isTemplate: Boolean = false  // true for built-in PPL/5x5/etc.
)

data class PlanExercise(
    val id: Long = 0,
    val planId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val targetSets: Int,
    val targetReps: Int,
    val targetWeight: Double? = null
)

// -- Workout Session --
data class WorkoutSession(
    val id: Long = 0,
    val planId: Long? = null,       // null = ad-hoc session
    val startedAt: Instant,
    val completedAt: Instant? = null,
    val notes: String = "",
    val durationSeconds: Long? = null
)

data class WorkoutSet(
    val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val orderIndex: Int,
    val weight: Double,
    val reps: Int,
    val rpe: Float? = null,         // Rate of Perceived Exertion (1-10)
    val isWarmup: Boolean = false,
    val completedAt: Instant
)

// -- Body Measurement --
data class BodyMeasurement(
    val id: Long = 0,
    val date: LocalDate,
    val bodyWeight: Double? = null,  // in user's preferred unit
    val bodyFatPercent: Double? = null,
    val notes: String = ""
)
```

### Data Flow

**Writing data (user logs a set):**
```
User taps "Complete Set" in ActiveSessionScreen
  -> ActiveSessionViewModel.completeSet(weight, reps)
    -> WorkoutRepository.addSet(set)
      -> WorkoutSetDao.insert(setEntity)
        -> Room SQLite write
```

**Reading data (reactive, via Flow):**
```
Room SQLite
  -> WorkoutSetDao.getSetsForSession(sessionId): Flow<List<WorkoutSetEntity>>
    -> WorkoutRepository: maps entities to domain models
      -> ActiveSessionViewModel: collects Flow into StateFlow
        -> ActiveSessionScreen: collectAsState() renders UI
```

**Key principle:** Data flows DOWN as method calls, and UP as reactive `Flow`/`StateFlow` streams. The UI never polls; it observes.

## Patterns to Follow

### Pattern 1: Repository as Single Source of Truth
**What:** All data access goes through repositories. No DAO calls from ViewModels.
**When:** Always.
**Example:**
```kotlin
class WorkoutRepositoryImpl(
    private val sessionDao: WorkoutSessionDao,
    private val setDao: WorkoutSetDao
) : WorkoutRepository {

    override fun getSessionWithSets(sessionId: Long): Flow<SessionWithSets> =
        sessionDao.getSessionWithSets(sessionId).map { it.toDomain() }

    override suspend fun addSet(set: WorkoutSet) {
        setDao.insert(set.toEntity())
    }
}
```

### Pattern 2: UiState Sealed Class per Screen
**What:** Each screen has a sealed interface representing all possible UI states.
**When:** For every screen with async data loading.
**Example:**
```kotlin
sealed interface SessionUiState {
    data object Loading : SessionUiState
    data class Active(
        val session: WorkoutSession,
        val sets: List<WorkoutSet>,
        val currentExercise: Exercise?
    ) : SessionUiState
    data class Error(val message: String) : SessionUiState
}

class ActiveSessionViewModel(
    private val workoutRepo: WorkoutRepository
) : ViewModel() {
    val uiState: StateFlow<SessionUiState> = ...
}
```

### Pattern 3: Room Relations for Complex Queries
**What:** Use `@Embedded` and `@Relation` for queries that span multiple tables.
**When:** Fetching a session with all its sets, or a plan with all its exercises.
**Example:**
```kotlin
data class SessionWithSets(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val sets: List<WorkoutSetEntity>
)

@Dao
interface WorkoutSessionDao {
    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId")
    fun getSessionWithSets(sessionId: Long): Flow<SessionWithSets>
}
```

### Pattern 4: Manual Dependency Injection (No Hilt)
**What:** Use a simple `AppContainer` object to provide dependencies, created in `Application` class.
**When:** Always for this project.
**Why not Hilt:** Hilt adds annotation processing overhead, KSP configuration complexity, and a learning curve. For a single-module app with 5-6 repositories and 5-6 ViewModels, constructor injection via a manual container is simpler, faster to build, and easier to understand. OSS contributors can onboard without learning Dagger/Hilt.
**Example:**
```kotlin
class AppContainer(context: Context) {
    private val database = GymDatabase.create(context)

    private val exerciseRepository: ExerciseRepository =
        ExerciseRepositoryImpl(database.exerciseDao())

    private val workoutRepository: WorkoutRepository =
        WorkoutRepositoryImpl(database.sessionDao(), database.setDao())

    fun exerciseListViewModel() = ExerciseListViewModel(exerciseRepository)
    fun activeSessionViewModel(sessionId: Long) =
        ActiveSessionViewModel(workoutRepository, sessionId)
}
```

### Pattern 5: Navigation with Type-Safe Routes
**What:** Use Kotlin sealed class or object for navigation routes with Compose Navigation.
**When:** Always.
**Example:**
```kotlin
sealed class Screen(val route: String) {
    data object ExerciseList : Screen("exercises")
    data object PlanList : Screen("plans")
    data class PlanEditor(val planId: Long? = null) : Screen("plan/{planId}")
    data class ActiveSession(val sessionId: Long) : Screen("session/{sessionId}")
    data object Progress : Screen("progress")
    data object Measurements : Screen("measurements")
}
```

## Anti-Patterns to Avoid

### Anti-Pattern 1: God ViewModel
**What:** A single ViewModel handling state for the entire active session including timer, sets, exercise selection, and navigation.
**Why bad:** Becomes unmaintainable quickly; the active workout screen is the most complex screen.
**Instead:** Break into focused concerns. One ViewModel per screen. If the active session screen grows, extract a `RestTimerState` helper class that the ViewModel delegates to.

### Anti-Pattern 2: Storing Computed Data
**What:** Storing "total volume" or "1RM estimate" in the database.
**Why bad:** Denormalized data gets out of sync. If a set is edited, all computed values must be recalculated.
**Instead:** Compute derived values in the repository or ViewModel layer from raw set data. Room `@Query` can do aggregation (`SUM(weight * reps)`) directly in SQL for performance.

### Anti-Pattern 3: Entity Classes as Domain Models
**What:** Passing Room entity classes (with `@Entity`, `@PrimaryKey` annotations) directly to the UI layer.
**Why bad:** Couples UI to database schema. Schema changes ripple through every screen. Room annotations pollute domain logic.
**Instead:** Separate entity and domain model classes with mappers in the repository. Yes, it is more code. It pays off at the first schema migration.

### Anti-Pattern 4: Passing Database IDs Through Navigation as Strings
**What:** Converting Long IDs to strings for navigation arguments, parsing back.
**Why bad:** Error-prone, no type safety.
**Instead:** Use Compose Navigation's support for typed arguments (Long, Int) or pass IDs as Long from the start.

## Suggested Build Order

Build order is driven by data dependencies. You cannot log a workout set without an exercise to assign it to, and you cannot start a session without exercises existing.

```
Phase 1: Foundation
  Room Database + Entities + DAOs (all tables)
  DI Container
  Exercise domain models + repository
  -> Validates: database schema, basic CRUD, project compiles

Phase 2: Exercise Library
  Exercise list screen + ViewModel
  Add/edit exercise screen
  Seed data (pre-populate common exercises)
  -> Validates: full vertical slice through all layers

Phase 3: Workout Plans
  Plan domain models + repository
  Plan list screen
  Plan editor screen (add exercises to plan, set targets)
  Built-in templates (PPL, 5x5) as seed data
  -> Depends on: Exercise library (Phase 2)

Phase 4: Active Workout Session
  Session domain models + repository
  Active session screen (log sets in real-time)
  Session timer
  Previous session overlay ("last time you did...")
  -> Depends on: Exercises (Phase 2), Plans (Phase 3, but sessions can be ad-hoc)

Phase 5: History and Progress
  Session history list
  Session detail view
  Progress charts (per-exercise strength over time)
  -> Depends on: logged session data (Phase 4)

Phase 6: Body Measurements
  Measurement models + repository + screen
  Body weight trend chart
  -> Independent of workout features; can be built in parallel with Phase 4/5

Phase 7: Polish
  Rest timer with notification
  Data export (CSV/JSON)
  Settings (units, theme)
  App icon and theming
```

**Why this order:**
1. Database first because everything depends on it and mistakes here are costly to fix later
2. Exercises first because they are the atomic unit every other feature references
3. Plans before sessions because plans define what a session looks like (but sessions should support ad-hoc mode from day one)
4. Active session is the core UX -- most complex screen, most value, needs exercises and plans to exist
5. History/progress only makes sense with existing data
6. Body measurements are decoupled and can slide earlier or later

## Scalability Considerations

| Concern | At personal use (1 user) | At 1000+ sessions | Notes |
|---------|--------------------------|--------------------|----|
| Query performance | No issue | Index on `sessionId`, `exerciseId`, `date` columns | Add indices to entities from the start |
| Database size | Negligible | ~5-20MB for years of data | SQLite handles this trivially |
| UI rendering | No issue | Lazy lists + pagination for history | Use `LazyColumn` from the start; add paging only if needed |
| Schema migration | N/A | Room auto-migration or manual Migration objects | Plan for migrations from v1: never delete columns, always add |
| Chart rendering | Simple | 1000+ data points per exercise | Use Vico or a compose-native charting library; avoid loading all points at once |

## Sources

- Android Architecture Guide (developer.android.com/topic/architecture) -- HIGH confidence, official Google guidance
- Room Persistence Library documentation (developer.android.com/training/data-storage/room) -- HIGH confidence
- Jetpack Compose Navigation documentation (developer.android.com/jetpack/compose/navigation) -- HIGH confidence
- Training data knowledge of common patterns in open-source workout trackers (FitNotes, OpenWorkout, GymRoutines) -- MEDIUM confidence, patterns are well-established but specific implementations may vary
