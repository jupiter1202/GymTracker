# Technology Stack

**Project:** GymTracker
**Researched:** 2026-04-04

## Existing Project Baseline

The Android Studio project already has a scaffold with:
- AGP 9.1.0, Kotlin 2.2.10, Compose BOM 2024.09.00 (outdated)
- compileSdk 36, minSdk 29, targetSdk 36
- Material 3, basic Compose UI, lifecycle-runtime-ktx
- Version catalog (`libs.versions.toml`) in place

The existing Compose BOM (2024.09.00) and several AndroidX dependencies are significantly behind current stable. The version catalog should be updated as part of the first milestone.

## Recommended Stack

### Core Framework

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| Kotlin | 2.2.10 | Language | Already in project, current stable | HIGH |
| Jetpack Compose BOM | 2026.03.01 | UI toolkit | Latest stable BOM, includes Compose 1.10.x and Material3 1.4.0. The BOM manages all Compose artifact versions in lockstep | HIGH (verified: developer.android.com) |
| Material 3 | via BOM | Design system | Standard for new Android apps, included in BOM | HIGH |
| AGP | 9.1.0 | Build system | Already in project, current | HIGH |

### Database

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| Room | 2.8.4 | Local persistence | The only serious choice for structured local data on Android. First-party Jetpack library. Compile-time SQL verification, coroutines/Flow integration, migration support. Zero reason to use anything else for this use case | HIGH (verified: developer.android.com) |
| KSP plugin | 2.3.4 | Annotation processing | Required for Room code generation. Replaces legacy kapt -- faster, better Kotlin support | HIGH (verified: developer.android.com) |

**Why not alternatives:**
- **SQLDelight**: Good library, but Room is first-party, better documented, and has deeper AndroidX integration. SQLDelight shines in KMP projects; this is Android-only.
- **Realm**: Proprietary (MongoDB), violates FOSS constraint. Also heavier than needed.
- **Raw SQLite**: Room provides compile-time checks, migration helpers, and Flow integration that would take weeks to replicate.

### Architecture & Lifecycle

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| lifecycle-viewmodel-compose | 2.10.0 | ViewModel in Compose | Provides `viewModel()` composable function, scopes VMs to navigation graph | HIGH (verified: developer.android.com) |
| lifecycle-runtime-compose | 2.10.0 | Lifecycle-aware Compose | `collectAsStateWithLifecycle()` for safe Flow collection | HIGH (verified: developer.android.com) |
| Navigation Compose | 2.9.7 | Screen navigation | First-party, type-safe routes (since 2.8+), deep link support, ViewModel scoping per destination | HIGH (verified: developer.android.com) |

### Dependency Injection

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| Koin | 4.0.x | Dependency injection | Lightweight, pure Kotlin, no code generation, no Gradle plugin required. Apache 2.0 license (FOSS-compatible). Simpler setup than Hilt for a single-developer project | MEDIUM |

**Why Koin over Hilt:**
- **Hilt** is Google's official recommendation and is Apache 2.0 licensed (FOSS-compatible). It provides compile-time DI graph validation.
- **Koin** is runtime-based (no annotation processing), which means faster builds and simpler configuration. For a single-module, single-developer FOSS app, Koin's simplicity wins. The compile-time safety of Hilt matters more on large teams.
- **Trade-off**: Koin catches DI errors at runtime, not compile time. For a small app with good test coverage, this is acceptable.
- If you prefer compile-time safety, switch to Hilt -- both are FOSS-compatible.

**Why not manual DI**: For this app's complexity (multiple repositories, ViewModels, a database), manual DI would become boilerplate-heavy by phase 2-3.

### Charting / Progress Visualization

| Technology | Version | Purpose | Why | Confidence |
|------------|---------|---------|-----|------------|
| Vico | 2.x (latest stable) | Charts for progress | Purpose-built for Jetpack Compose and Material 3. Supports line charts, bar charts, and combined charts -- exactly what a gym tracker needs for weight/rep progression. Apache 2.0 license | MEDIUM (version unverified -- WebFetch blocked on GitHub/Maven Central) |

**Why Vico:**
- Native Compose support with Material 3 theming out of the box
- Lightweight -- does not pull in a massive view-based charting engine
- Active development, Kotlin-first API
- Handles the exact chart types needed: line charts (weight over time), bar charts (volume per session)

**Why not alternatives:**
- **MPAndroidChart**: View-based, not Compose-native. Requires interop wrappers. Last meaningful update was years ago.
- **YCharts (YourChart)**: Less mature, smaller community.
- **Custom Canvas drawing**: Possible but significant effort for smooth animations, axes, legends, and touch interactions.
- **Compose Multiplatform Charts**: Overkill for Android-only.

### Supporting Libraries

| Library | Version | Purpose | When to Use | Confidence |
|---------|---------|---------|-------------|------------|
| androidx.core:core-ktx | 1.15.x | Kotlin extensions for Android | Always -- quality-of-life Kotlin APIs | HIGH |
| kotlinx-coroutines-android | 1.9.x | Coroutine dispatchers | Always -- async work, Room queries | HIGH |
| kotlinx-serialization-json | 1.7.x | JSON serialization | If import/export of workout data is needed | HIGH |
| androidx.datastore:datastore-preferences | 1.1.x | Key-value preferences | User settings (units, theme, rest timer defaults) | HIGH |

### Testing

| Library | Version | Purpose | Confidence |
|---------|---------|---------|------------|
| JUnit 4 | 4.13.2 | Unit tests | HIGH (already in project) |
| kotlinx-coroutines-test | 1.9.x | Testing coroutines | HIGH |
| Room testing | 2.8.4 | In-memory DB for tests | HIGH |
| Compose UI Test | via BOM | UI/integration tests | HIGH (already in project) |
| Turbine | 1.2.x | Flow testing | MEDIUM |

## Architecture Pattern: MVVM with Unidirectional Data Flow

**Use MVVM**, not MVI. Here is why:

- Google's official architecture guide is built around MVVM (ViewModel + StateFlow/LiveData + Repository)
- For a gym tracker, UI state is straightforward: lists of workouts, form inputs, chart data. This does not need MVI's action/reducer ceremony.
- MVVM + `StateFlow` with `collectAsStateWithLifecycle()` gives you unidirectional data flow without the boilerplate of an MVI intent/action/state machine.
- Every official Android sample and codelab uses this pattern.

**Layer structure (per Google's architecture guide):**
```
UI Layer:  Composables -> ViewModel (exposes StateFlow<UiState>)
Data Layer: Repository -> Room DAO (+ DataStore for preferences)
Domain Layer: Optional UseCase classes (add only when logic is reused across ViewModels)
```

**Do NOT add a Domain layer upfront.** Start with UI + Data. Extract UseCases only when you find duplicated logic across multiple ViewModels (e.g., calculating 1RM, aggregating weekly volume).

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| Database | Room 2.8.4 | SQLDelight | KMP focus; Room has better AndroidX integration for Android-only apps |
| Database | Room 2.8.4 | Realm | Proprietary (MongoDB). Violates FOSS constraint |
| DI | Koin 4.0.x | Hilt/Dagger | More setup (Gradle plugin, kapt/KSP), slower builds. Valid alternative if compile-time safety preferred |
| DI | Koin 4.0.x | Manual DI | Too much boilerplate once you have 5+ ViewModels and repositories |
| Charts | Vico 2.x | MPAndroidChart | View-based, not Compose-native, essentially unmaintained |
| Charts | Vico 2.x | Custom Canvas | Weeks of work for axes, legends, animations, touch handling |
| Architecture | MVVM | MVI (Orbit/etc.) | Overkill for this app's state complexity. Adds unnecessary abstraction |
| Navigation | Navigation Compose 2.9.7 | Voyager / Decompose | Third-party, less documentation, not needed for Android-only |
| Preferences | DataStore | SharedPreferences | SharedPreferences is deprecated-in-spirit; DataStore is the replacement with coroutine support |

## FOSS / License Compatibility

All recommended dependencies are open-source and compatible with MIT/GPL distribution:

| Dependency | License |
|------------|---------|
| Jetpack Compose / AndroidX | Apache 2.0 |
| Room | Apache 2.0 |
| Navigation Compose | Apache 2.0 |
| Koin | Apache 2.0 |
| Vico | Apache 2.0 |
| Kotlin / Coroutines | Apache 2.0 |
| DataStore | Apache 2.0 |

No proprietary or restrictively-licensed dependencies in the stack.

## Version Catalog Updates

The existing `libs.versions.toml` needs these updates and additions:

```toml
[versions]
agp = "9.1.0"                    # keep
kotlin = "2.2.10"                # keep
ksp = "2.3.4"                    # ADD
composeBom = "2026.03.01"        # UPDATE from 2024.09.00
coreKtx = "1.15.0"              # UPDATE from 1.10.1
lifecycleRuntimeKtx = "2.10.0"  # UPDATE from 2.6.1
activityCompose = "1.10.0"      # UPDATE from 1.8.0
navigationCompose = "2.9.7"      # ADD
room = "2.8.4"                   # ADD
koin = "4.0.2"                   # ADD (verify latest on Maven Central)
vico = "2.1.0"                   # ADD (verify latest on GitHub releases)
datastorePreferences = "1.1.4"   # ADD
junit = "4.13.2"                 # keep
junitVersion = "1.2.1"          # UPDATE from 1.1.5
espressoCore = "3.6.1"          # UPDATE from 3.5.1
```

**NOTE on versions marked "verify"**: WebFetch was rate-limited for Maven Central and GitHub. Koin 4.0.x and Vico 2.x are the correct major versions, but the exact patch should be verified against Maven Central before adding to the version catalog.

## Installation

```kotlin
// In app/build.gradle.kts plugins block, add:
// alias(libs.plugins.ksp)

// Core (already present, update versions)
implementation(platform(libs.androidx.compose.bom))
implementation(libs.androidx.compose.material3)
implementation(libs.androidx.core.ktx)
implementation(libs.androidx.lifecycle.runtime.ktx)
implementation(libs.androidx.activity.compose)

// Navigation
implementation(libs.androidx.navigation.compose)

// ViewModel Compose
implementation(libs.androidx.lifecycle.viewmodel.compose)
implementation(libs.androidx.lifecycle.runtime.compose)

// Room
implementation(libs.androidx.room.runtime)
implementation(libs.androidx.room.ktx)
ksp(libs.androidx.room.compiler)

// DI - Koin
implementation(libs.koin.android)
implementation(libs.koin.androidx.compose)

// Charts
implementation(libs.vico.compose.m3)

// Preferences
implementation(libs.androidx.datastore.preferences)
```

## Sources

- Compose BOM 2026.03.01: https://developer.android.com/jetpack/compose/bom/bom-mapping (verified 2026-04-04)
- Room 2.8.4: https://developer.android.com/jetpack/androidx/releases/room (verified 2026-04-04)
- Navigation 2.9.7: https://developer.android.com/jetpack/androidx/releases/navigation (verified 2026-04-04)
- Lifecycle 2.10.0: https://developer.android.com/jetpack/androidx/releases/lifecycle (verified 2026-04-04)
- KSP 2.3.4: https://developer.android.com/build/migrate-to-ksp (verified 2026-04-04)
- Architecture guide: https://developer.android.com/topic/architecture (verified 2026-04-04)
- Compose library integration: https://developer.android.com/develop/ui/compose/libraries (verified 2026-04-04)
- Koin version: Training data (MEDIUM confidence -- verify on Maven Central)
- Vico version: Training data (MEDIUM confidence -- verify on GitHub releases)
