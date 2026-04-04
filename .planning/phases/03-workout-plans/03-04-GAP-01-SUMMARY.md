---
phase: 03-workout-plans
plan: 04-gap-01
subsystem: feature/plans
type: gap-closure
completed_date: 2026-04-04
duration_minutes: 5
tasks_completed: 2
files_modified: 1
key_commits:
  - hash: 32dd23c
    message: "feat(03-04-gap-01): move add exercise button into lazycolumn"
dependencies:
  requires: [03-04]
  provides: [visible-add-exercise-button]
  affects: [PlanDetailScreen]
tech_stack:
  - Jetpack Compose
  - LazyColumn
  - Material3
---

# Phase 03 Plan 04-GAP-01: Fix Missing "Add exercise" Button

**Summary:** Fixed the missing "Add exercise" button in PlanDetailScreen by moving it from the outer Column into the LazyColumn as the last item, ensuring it's always visible and scrollable with the exercise list.

## What Was Built

- Modified `PlanExerciseList` composable to accept `onAddExercise` callback parameter
- Moved the "Add exercise" ListItem from outer Column into LazyColumn's items block
- Updated parent Column to remove duplicate button and pass callback to PlanExerciseList
- Button now appears at the bottom of the scrollable exercise list

## Key Changes

### File: `app/src/main/java/de/jupiter1202/gymtracker/feature/plans/PlanDetailScreen.kt`

**Modified:**
1. `PlanExerciseList()` function signature: Added `onAddExercise: () -> Unit` parameter
2. Inside LazyColumn's items block: Added final item containing the "Add exercise" ListItem with clickable modifier calling `onAddExercise()`
3. Parent Column: Removed the static "Add exercise" ListItem (lines 97-104)
4. Updated call to `PlanExerciseList()` to pass `onAddExercise = { showExercisePicker = true }`

## Problem Solved

**Original Problem:** The LazyColumn had `Modifier.fillMaxSize()` which caused it to fill all available vertical space in its parent Column, completely hiding the "Add exercise" ListItem placed after it.

**Solution:** By moving the "Add exercise" button inside the LazyColumn as the final item, it's now part of the scrollable content and will always be accessible at the bottom of the exercise list, never hidden.

## Verification

✅ **Build:** `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL  
✅ **Tests:** `./gradlew :app:testDebugUnitTest` → BUILD SUCCESSFUL (all tests pass)  
✅ **Compilation:** No errors or warnings  
✅ **Layout:** Button is now the last item in LazyColumn, always accessible by scrolling

## Deviations from Plan

None - plan executed exactly as written. The solution was straightforward: modify the function signature, add callback, move button into LazyColumn, remove duplicate button from parent Column.

## Self-Check

✅ File exists: `app/src/main/java/de/jupiter1202/gymtracker/feature/plans/PlanDetailScreen.kt`  
✅ Commit exists: `32dd23c` (verified: git log shows commit)  
✅ Build passes: assembleDebug successful  
✅ Tests pass: testDebugUnitTest successful
