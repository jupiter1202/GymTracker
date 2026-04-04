# Phase 1: Foundation - Research

**Researched:** 2026-04-04
**Domain:** Android Jetpack (Room, Navigation Compose, DataStore, Koin DI)
**Confidence:** HIGH

## Summary

Phase 1 establishes the complete technical skeleton for GymTracker: project structure, dependency injection, a 6-entity Room database with schema export, a DataStore-backed unit preference system, and a bottom navigation scaffold with 5 placeholder tabs. All four key libraries (Room 2.8.4, Koin 4.1.1 BOM, Navigation Compose 2.9.7, DataStore 1.2.1) are at known stable versions verified from official Android developer sources. KSP 2.2.10-2.0.2 is the correct annotation processor version for Kotlin 2.2.10 and AGP 9.1.0.

The existing project uses AGP 9.1.0 and Kotlin 2.2.10 — both are important constraints. AGP 9.x requires KSP 2.2.10+ and enforces KSP2 (not KAPT). The Room Gradle Plugin (`androidx.room`) is the current recommended way to configure schema export rather than the legacy `ksp { arg("room.schemaLocation", ...) }` approach. Koin replaces Hilt as DI — this is a deliberate project decision.

**Primary recommendation:** Use the Room Gradle Plugin for schema export, Koin BOM 4.1.1 for DI, Navigation Compose 2.9.7 for the bottom nav scaffold, and DataStore Preferences 1.2.1 for the kg/lbs preference. All wired together with KSP 2.2.10-2.0.2.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

- Feature-based package structure: `feature/exercises`, `feature/plans`, `feature/logging`, `feature/history`, `feature/export`
- Shared infrastructure in `core/`: `core/database`, `core/di`, `core/ui`
- No domain/use-case layer — ViewModel calls repository methods directly (repository → ViewModel)
- Base package: `de.jupiter1202.gymtracker`
- Full bottom navigation scaffold set up in Phase 1 with 5 tabs: Dashboard, Exercises, Plans, History, Settings
- Phases 2–5 fill in the real screens; Phase 1 shows placeholder screens for each tab
- Placeholder screens: simple centered text with tab name + "Coming soon" subtitle
- Full settings screen foundation with sections and a reusable row composable
- Phase 1 exposes only the kg/lbs unit toggle
- Unit toggle uses Material3 `SingleChoiceSegmentedButtonRow` showing "kg" and "lbs" side by side
- Settings accessible as the 5th bottom nav tab
- kg/lbs preference persisted via DataStore (Preferences)
- UnitConverter utility built in Phase 1 under `core/` — handles kg↔lbs conversion
- Database always stores weight values in kg; display layer converts to lbs when preference is set

### Claude's Discretion

- Exact DataStore key naming and structure
- Room schema export directory path
- Loading skeleton or splash screen (if any)
- Koin module organization within `core/di/`

### Deferred Ideas (OUT OF SCOPE)

None — discussion stayed within phase scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| GEN-01 | App source code is released under an open source license (MIT or GPL-3.0) | Requires adding a LICENSE file to the repo root — no library dependency needed |
| LOG-02 | User can toggle between kg and lbs; preference persists across sessions | DataStore Preferences 1.2.1 + UnitConverter utility in `core/` |
</phase_requirements>

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Room | 2.8.4 | SQLite ORM — entities, DAOs, migrations | Official Jetpack persistence layer; 2.8.4 is current stable |
| Room Gradle Plugin | 2.8.4 | Schema export directory config | Replaces legacy `ksp arg()` approach; required for clean schema export |
| KSP | 2.2.10-2.0.2 | Annotation processing for Room | Required by AGP 9.x; KAPT is deprecated and blocked |
| Koin BOM | 4.1.1 | Dependency injection | Project-chosen DI; lightweight, no code generation, Kotlin-first |
| koin-android | (via BOM) | Android lifecycle integration | startKoin + androidContext setup |
| koin-androidx-compose | (via BOM) | Compose ViewModel injection | koinViewModel() in composables |
| Navigation Compose | 2.9.7 | In-app navigation, NavHost | Official Jetpack nav for Compose; typed routes |
| DataStore Preferences | 1.2.1 | Persistent key-value storage | Modern SharedPreferences replacement; Flow-based |
| Lifecycle ViewModel Compose | (already in BOM scope) | ViewModel scoping in Compose | collectAsStateWithLifecycle for Flow |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| room-testing | 2.8.4 | In-memory DB for unit tests | Verifying DAO queries and schema correctness |
| androidx-test-runner | 1.6.x | Android instrumented tests | Running Room instrumented tests on device/emulator |

### Alternatives Considered

| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Koin | Hilt | Hilt requires KAPT/KSP codegen; Koin is annotation-free and simpler for this project size |
| Navigation Compose 2.9.7 | Navigation3 (1.1.0-rc01) | Navigation3 is stable but brand-new; 2.9.7 has ecosystem maturity and more community examples |
| DataStore Preferences | Room for preferences | Overkill; DataStore is purpose-built for simple key-value persistence |

**Installation (additions to existing project):**

```bash
# These go into libs.versions.toml + app/build.gradle.kts — not a shell command
# Listed here for reference
```

`libs.versions.toml` additions:
```toml
[versions]
ksp = "2.2.10-2.0.2"
room = "2.8.4"
koin = "4.1.1"
navigationCompose = "2.9.7"
datastorePreferences = "1.2.1"

[libraries]
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
androidx-room-testing = { group = "androidx.room", name = "room-testing", version.ref = "room" }
koin-bom = { group = "io.insert-koin", name = "koin-bom", version.ref = "koin" }
koin-android = { group = "io.insert-koin", name = "koin-android" }
koin-androidx-compose = { group = "io.insert-koin", name = "koin-androidx-compose" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastorePreferences" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
androidx-room = { id = "androidx.room", version.ref = "room" }
```

## Architecture Patterns

### Recommended Project Structure

```
app/src/main/java/de/jupiter1202/gymtracker/
├── core/
│   ├── database/
│   │   ├── GymTrackerDatabase.kt        # RoomDatabase with all 6 entities
│   │   ├── entities/
│   │   │   ├── Exercise.kt
│   │   │   ├── WorkoutPlan.kt
│   │   │   ├── PlanExercise.kt
│   │   │   ├── WorkoutSession.kt
│   │   │   ├── WorkoutSet.kt
│   │   │   └── BodyMeasurement.kt
│   │   └── converters/                  # TypeConverters if needed
│   ├── di/
│   │   ├── AppModule.kt                 # Database, DataStore providers
│   │   └── FeatureModules.kt            # Or split per feature — discretion area
│   └── ui/
│       └── theme/                       # Already exists — ui/theme/ stays put
├── feature/
│   ├── dashboard/
│   │   └── DashboardScreen.kt           # Placeholder
│   ├── exercises/
│   │   └── ExercisesScreen.kt           # Placeholder
│   ├── plans/
│   │   └── PlansScreen.kt               # Placeholder
│   ├── history/
│   │   └── HistoryScreen.kt             # Placeholder
│   └── settings/
│       ├── SettingsScreen.kt            # Full settings scaffold
│       └── SettingsViewModel.kt         # Reads/writes unit preference
├── navigation/
│   ├── AppNavHost.kt                    # NavHost wiring
│   └── BottomNavDestination.kt          # Sealed class/enum for 5 tabs
└── MainActivity.kt                      # Hosts Scaffold + BottomNav + AppNavHost
```

Schema export:
```
app/schemas/
└── de.jupiter1202.gymtracker.core.database.GymTrackerDatabase/
    └── 1.json                           # Auto-generated on first compile
```

### Pattern 1: Room Gradle Plugin for Schema Export

**What:** The `androidx.room` Gradle plugin handles schema export directory configuration declaratively, replacing the legacy `ksp { arg("room.schemaLocation", ...) }` approach.
**When to use:** Always — it is the current standard for Room 2.6.0+.

`app/build.gradle.kts`:
```kotlin
// Source: https://developer.android.com/training/data-storage/room/migrating-db-versions
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
}

android {
    // ...existing config...
    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    // ...
}
```

Database declaration:
```kotlin
// Source: https://developer.android.com/jetpack/androidx/releases/room
@Database(
    entities = [
        Exercise::class,
        WorkoutPlan::class,
        PlanExercise::class,
        WorkoutSession::class,
        WorkoutSet::class,
        BodyMeasurement::class
    ],
    version = 1,
    exportSchema = true
)
abstract class GymTrackerDatabase : RoomDatabase() {
    // DAOs declared here in later phases
}
```

### Pattern 2: Koin Initialization with BOM

**What:** Start Koin in Application.onCreate() with androidContext and module list. Use BOM so individual library versions are managed centrally.
**When to use:** Always for this project — single Application class wires everything.

```kotlin
// Source: https://insert-koin.io/docs/setup/koin/
class GymTrackerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger()
            androidContext(this@GymTrackerApp)
            modules(appModule)
        }
    }
}
```

Koin module providing Room + DataStore:
```kotlin
val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            GymTrackerDatabase::class.java,
            "gymtracker.db"
        ).build()
    }
    single {
        androidContext().dataStore  // property delegate defined at top of DataStore file
    }
}
```

### Pattern 3: Navigation Compose with Bottom Nav

**What:** NavController + NavHost + Scaffold with `bottomBar` using Material3 NavigationBar.
**When to use:** This is the permanent scaffold — phases 2–5 replace composable bodies, not this wiring.

```kotlin
// Source: https://developer.android.com/develop/ui/compose/components/navigation-bar
@Composable
fun AppNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") { DashboardScreen() }
        composable("exercises") { ExercisesScreen() }
        composable("plans") { PlansScreen() }
        composable("history") { HistoryScreen() }
        composable("settings") { SettingsScreen() }
    }
}

// In MainActivity or top-level composable:
val navController = rememberNavController()
Scaffold(
    bottomBar = {
        NavigationBar {
            BottomNavDestination.entries.forEach { dest ->
                NavigationBarItem(
                    selected = currentRoute == dest.route,
                    onClick = { navController.navigate(dest.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }},
                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                    label = { Text(dest.label) }
                )
            }
        }
    }
) { padding ->
    AppNavHost(navController, Modifier.padding(padding))
}
```

**Critical navigation flags:** `popUpTo`, `launchSingleTop = true`, `restoreState = true` prevent back-stack accumulation when switching tabs.

### Pattern 4: DataStore Preferences for Unit Toggle

**What:** Single `preferencesDataStore` property delegate at file top level, read via Flow, write via `edit {}` in a coroutine.
**When to use:** Always — creating multiple instances for the same file corrupts DataStore.

```kotlin
// Source: https://developer.android.com/topic/libraries/architecture/datastore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PreferenceKeys {
    val WEIGHT_UNIT = stringPreferencesKey("weight_unit")  // "kg" or "lbs"
}

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    val weightUnit: Flow<String> = dataStore.data.map { prefs ->
        prefs[PreferenceKeys.WEIGHT_UNIT] ?: "kg"
    }

    suspend fun setWeightUnit(unit: String) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.WEIGHT_UNIT] = unit
        }
    }
}
```

### Pattern 5: UnitConverter Utility

**What:** Pure Kotlin object in `core/` with two conversion functions. No dependencies.
**When to use:** Called exclusively in the display/ViewModel layer — database always stores kg.

```kotlin
// core/UnitConverter.kt
object UnitConverter {
    fun kgToLbs(kg: Double): Double = kg * 2.20462
    fun lbsToKg(lbs: Double): Double = lbs / 2.20462
}
```

### Anti-Patterns to Avoid

- **Multiple DataStore instances for same file:** Creating `preferencesDataStore("settings")` more than once in the process causes corruption. Use a singleton via Koin injection.
- **KAPT for Room:** KAPT is deprecated in AGP 9.x. Use `ksp(libs.androidx.room.compiler)` exclusively.
- **Storing lbs in database:** The canonical rule is always-kg storage. Converting on write corrupts historical data when users toggle preference.
- **Tab back-stack accumulation:** Without `launchSingleTop = true` and `restoreState = true`, pressing a bottom nav tab multiple times creates duplicate back-stack entries.
- **Missing `exportSchema = true` on `@Database`:** Even with the Room Gradle Plugin configured, the annotation must be `exportSchema = true` (it is true by default, but should be explicit).
- **Declaring Koin modules inside Activity/Fragment:** Modules belong in the Application class startup, not per-screen.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Persistent preferences | Custom SharedPreferences wrapper | DataStore Preferences | Thread-safe, Flow-based, handles migration, no corruption on ANR |
| DI container | Manual singleton registry | Koin | Lifecycle-aware, testable, handles Android context scoping |
| Type-safe navigation | String route constants | Typed routes in Navigation Compose 2.9.7 | Compile-time safety, no stringly-typed bugs |
| SQL schema management | Manual CREATE TABLE statements | Room entities + @Database | Auto-migration support, schema versioning, query validation at compile time |
| kg/lbs conversion | Inline math at call sites | UnitConverter utility | Centralized, testable, prevents precision drift from repeated conversions |

**Key insight:** Room's real value in Phase 1 is not just persistence — it's the compile-time SQL validation and schema versioning that prevents costly Phase 4/5 rewrites.

## Common Pitfalls

### Pitfall 1: KSP Version Mismatch with Kotlin Version

**What goes wrong:** Build fails with "KSP processor not found" or "incompatible KSP version."
**Why it happens:** KSP versions are tightly coupled to the Kotlin version. The KSP version must start with the same Kotlin version prefix (e.g., Kotlin 2.2.10 requires KSP `2.2.10-X.Y.Z`).
**How to avoid:** Use KSP `2.2.10-2.0.2` for Kotlin `2.2.10`. AGP 9.x will upgrade KSP automatically if a lower version is specified, but explicit declaration avoids surprises.
**Warning signs:** Gradle sync error mentioning "ksp" and version mismatch; Room entities not generating DAOs.

### Pitfall 2: Missing Application Class Registration in AndroidManifest

**What goes wrong:** Koin fails at runtime with "KoinApplication is not started" even though startKoin is called.
**Why it happens:** The custom `Application` subclass is not declared in `AndroidManifest.xml` with `android:name=".GymTrackerApp"`.
**How to avoid:** Always add `android:name` to `<application>` tag in `AndroidManifest.xml` after creating a custom Application class.
**Warning signs:** `KoinNotStartedException` at first ViewModel creation.

### Pitfall 3: Room Database on Main Thread

**What goes wrong:** `IllegalStateException: Cannot access database on the main thread` at runtime.
**Why it happens:** DAOs are called directly without `suspend`, or a repository method is invoked from the main thread without a coroutine.
**How to avoid:** All DAO methods must be `suspend` functions. Repository methods are `suspend`. ViewModels use `viewModelScope.launch` to call repository. Room-ktx handles the coroutine dispatcher automatically.
**Warning signs:** App crashes on first DB interaction; the error message is explicit.

### Pitfall 4: Schema Export Not Committed to Source Control

**What goes wrong:** Future auto-migrations reference schema files that don't exist, causing build failures in CI or on other machines.
**Why it happens:** `app/schemas/` directory is often absent from `.gitignore` but developers forget to `git add` the exported JSON files.
**How to avoid:** After first successful build, commit the generated `app/schemas/.../*.json` file. Add a note in the plan's success criteria to verify this file exists.
**Warning signs:** Build works locally but fails on a fresh clone; auto-migration build errors.

### Pitfall 5: Navigation Back Stack on Tab Switches

**What goes wrong:** Pressing the back button from a bottom nav tab goes back through all previously visited tabs instead of exiting the app.
**Why it happens:** `navController.navigate(route)` without `popUpTo` and `launchSingleTop` stacks destinations.
**How to avoid:** Always use the navigation options shown in Pattern 3: `popUpTo(startDestination) { saveState = true }`, `launchSingleTop = true`, `restoreState = true`.
**Warning signs:** Multiple taps on same nav item creates duplicates; back button cycles through tabs.

### Pitfall 6: DataStore Initialized Multiple Times

**What goes wrong:** `IllegalStateException: There are multiple DataStores active for the same file` at runtime.
**Why it happens:** `preferencesDataStore("settings")` delegate is called from multiple files or the DataStore is re-instantiated.
**How to avoid:** Define the `Context.dataStore` extension property exactly once in a dedicated file (e.g., `core/di/DataStoreProvider.kt`), then inject it via Koin.
**Warning signs:** Exception on second app launch or after hot-reload; intermittent crashes.

## Code Examples

Verified patterns from official sources:

### Room Database Declaration (6 entities)

```kotlin
// Source: https://developer.android.com/jetpack/androidx/releases/room
@Database(
    entities = [
        Exercise::class,
        WorkoutPlan::class,
        PlanExercise::class,
        WorkoutSession::class,
        WorkoutSet::class,
        BodyMeasurement::class
    ],
    version = 1,
    exportSchema = true
)
abstract class GymTrackerDatabase : RoomDatabase()
```

### Koin Module with Room + DataStore

```kotlin
// Source: https://insert-koin.io/docs/4.0/quickstart/android-compose/
val appModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            GymTrackerDatabase::class.java,
            "gymtracker.db"
        ).build()
    }
    single<DataStore<Preferences>> { androidContext().dataStore }
    single { SettingsRepository(get()) }
}
```

### ViewModel with Koin in Compose

```kotlin
// Source: https://insert-koin.io/docs/reference/koin-compose/compose/
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel()
) {
    val unit by viewModel.weightUnit.collectAsStateWithLifecycle()
    // ...
}
```

### SingleChoiceSegmentedButtonRow for kg/lbs

```kotlin
// Source: https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary
val options = listOf("kg", "lbs")
SingleChoiceSegmentedButtonRow {
    options.forEachIndexed { index, option ->
        SegmentedButton(
            selected = currentUnit == option,
            onClick = { onUnitChange(option) },
            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
        ) {
            Text(option)
        }
    }
}
```

### Placeholder Screen Pattern

```kotlin
@Composable
fun DashboardScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Dashboard", style = MaterialTheme.typography.headlineMedium)
            Text("Coming soon", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| KAPT for Room | KSP (ksp2) | Room 2.6+, required by AGP 9.x | KSP is ~2x faster build times; KAPT broken in AGP 9.x |
| `ksp { arg("room.schemaLocation", ...) }` | `room { schemaDirectory(...) }` via Room Gradle Plugin | Room 2.6.0 | Cleaner config, variant-aware schema export |
| SharedPreferences | DataStore Preferences | AndroidX 1.0+ (stable now at 1.2.1) | Thread-safe, coroutine-native, no ANR risk |
| Navigation 2.x stringly-typed routes | Navigation Compose 2.9.7 typed routes | 2.7.0+ | Compile-time safety via serializable destination classes |
| Hilt for DI | Koin (project decision) | N/A — project choice | No code generation, simpler setup for this app size |

**Deprecated/outdated:**
- KAPT: Broken in AGP 9.x for KSP-capable libraries; never use for this project
- `kapt("androidx.room:room-compiler:...")`: Replace with `ksp(...)` always
- `Room.allowMainThreadQueries()`: Never use; exists only for tests with in-memory DB

## Open Questions

1. **KSP 2.2.10-2.0.2 is listed as the correct version but AGP 9.1.0 may have a preferred KSP version**
   - What we know: AGP 9.0 ships with a runtime dependency on KGP 2.2.10 and will auto-upgrade KSP below `2.2.10-2.0.2`
   - What's unclear: Whether AGP 9.1.0 prefers a higher KSP patch (e.g., 2.2.20-x)
   - Recommendation: Use `2.2.10-2.0.2` as the floor; if Gradle sync warns about upgrading, follow the suggested version

2. **Koin module organization (discretion area)**
   - What we know: Koin supports any module decomposition — one flat module or per-feature modules
   - What's unclear: Whether Phase 1 should pre-create empty feature modules or one `appModule` covering everything
   - Recommendation: One `appModule` in Phase 1 covering DB + DataStore + Settings. Feature modules added as Phase 2+ introduces ViewModels and repositories.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 4 (junit:junit:4.13.2) — already in project |
| Android Instrumented | AndroidJUnitRunner (androidx.test.ext:junit:1.1.5) — already in project |
| Config file | No separate config file; standard Android Gradle test source sets |
| Quick run command | `./gradlew testDebugUnitTest` (host JVM — UnitConverter tests) |
| Full suite command | `./gradlew connectedDebugAndroidTest` (requires device/emulator — Room DAO tests) |

### Phase Requirements to Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| GEN-01 | LICENSE file exists in repo root | Manual verification | `ls ./LICENSE` | Wave 0 gap |
| LOG-02 (unit pref persists) | Setting "lbs" survives app restart | Instrumented (DataStore) | `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=de.jupiter1202.gymtracker.SettingsRepositoryTest` | Wave 0 gap |
| LOG-02 (converter) | `UnitConverter.kgToLbs(100.0)` returns 220.462 ± 0.001 | Unit test | `./gradlew testDebugUnitTest --tests "*.UnitConverterTest"` | Wave 0 gap |
| Database schema | GymTrackerDatabase creates all 6 tables on first launch | Instrumented (Room testing) | `./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=de.jupiter1202.gymtracker.GymTrackerDatabaseTest` | Wave 0 gap |

### Sampling Rate

- **Per task commit:** `./gradlew testDebugUnitTest` (fast — JVM only, ~10 seconds)
- **Per wave merge:** `./gradlew connectedDebugAndroidTest` (requires emulator — ~2-3 minutes)
- **Phase gate:** Full instrumented suite green before `/gsd:verify-work`

### Wave 0 Gaps

- [ ] `app/src/test/java/de/jupiter1202/gymtracker/core/UnitConverterTest.kt` — covers LOG-02 (conversion math)
- [ ] `app/src/androidTest/java/de/jupiter1202/gymtracker/core/database/GymTrackerDatabaseTest.kt` — covers database schema (all 6 tables exist)
- [ ] `app/src/androidTest/java/de/jupiter1202/gymtracker/feature/settings/SettingsRepositoryTest.kt` — covers LOG-02 (persistence across process restart via DataStore)
- [ ] `app/schemas/` directory committed to git after first compile — covers GEN-01 indirectly (build integrity)
- [ ] `room-testing` dependency addition: `testImplementation(libs.androidx.room.testing)` — needed for in-memory DB in instrumented tests

## Sources

### Primary (HIGH confidence)

- https://developer.android.com/jetpack/androidx/releases/room — Room 2.8.4 stable, KSP setup, Room Gradle Plugin
- https://developer.android.com/jetpack/androidx/releases/navigation — Navigation Compose 2.9.7 stable (January 28, 2026)
- https://developer.android.com/jetpack/androidx/releases/datastore — DataStore Preferences 1.2.1 stable (March 11, 2026)
- https://developer.android.com/topic/libraries/architecture/datastore — DataStore API patterns, singleton requirement
- https://developer.android.com/develop/ui/compose/components/navigation-bar — NavigationBar + NavigationBarItem composable API
- https://insert-koin.io/docs/setup/koin/ — Koin BOM 4.1.1 stable, Android Compose dependency declarations

### Secondary (MEDIUM confidence)

- https://mvnrepository.com/artifact/com.google.devtools.ksp/com.google.devtools.ksp.gradle.plugin/2.2.10-2.0.2 — KSP 2.2.10-2.0.2 confirmed on Maven Central
- https://developer.android.com/build/releases/agp-9-0-0-release-notes — AGP 9.x KSP 2.2.10 requirement
- https://blog.insert-koin.io/koin-4-1-safer-configurations-stronger-integrations-support-0d68a691b30f — Koin 4.1 release notes

### Tertiary (LOW confidence)

- https://android-developers.googleblog.com/2026/03/room-30-modernizing-room.html — Room 3.0 in alpha (not used here; 2.8.4 is correct choice)

## Metadata

**Confidence breakdown:**
- Standard stack (library versions): HIGH — all four libraries verified from official release pages with exact dates
- Architecture patterns: HIGH — code patterns from official docs and verified Koin docs
- KSP version compatibility: MEDIUM — primary source is Maven Central listing + AGP release notes; one open question remains on 9.1.0 exact preference
- Pitfalls: HIGH — Room main-thread and DataStore singleton pitfalls are officially documented; nav back-stack pattern is from official sample code
- Test infrastructure: MEDIUM — framework is standard JUnit4/AndroidJUnitRunner already in project; specific test files don't exist yet (Wave 0 gaps)

**Research date:** 2026-04-04
**Valid until:** 2026-07-04 (stable libraries; Room 3.0 alpha may go stable, but 2.8.4 will remain supported in maintenance mode)
