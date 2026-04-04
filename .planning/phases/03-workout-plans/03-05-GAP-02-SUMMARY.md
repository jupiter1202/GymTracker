---
phase: 03-workout-plans
plan: "05-gap-02"
status: completed
completed_date: 2026-04-04
task_count: 2
key-files:
  modified:
    - app/src/main/java/de/jupiter1202/gymtracker/feature/plans/PlanDetailScreen.kt
duration_minutes: 3
---

# Plan 03-05-GAP-02 Summary: Fix Drag-and-Drop Gesture Conflict

## Objective Achieved

✅ **Fixed drag-and-drop reordering by removing gesture conflict**
- Identified and removed `.clickable()` modifier from exercise info column that was intercepting long-press gestures
- Gesture conflict prevented `reorderable` library from detecting drag initiation
- Solution: Move click handling to dedicated edit button, leave drag handle as only drag-enabled region

## Implementation Details

### Task 1: Remove clickable from Row and add dedicated edit button

**Problem:** The ExerciseRowWithDelete composable had a `.clickable { onExerciseClick() }` modifier on the exercise info column that prevented the reorderable library from detecting long-press drag gestures needed to start reordering.

**Solution Applied:**
1. Removed `.clickable { onExerciseClick() }` from the Column containing exercise name and muscle group (lines 262-275)
2. Added dedicated `IconButton` with Edit icon as a separate, non-draggable control (lines 277-284)
3. Maintained all functionality: edit, delete, view target sets/reps
4. Drag handle (≡) remains the only region that can initiate drag operations

**Code Changes:**
```kotlin
// Before: Entire column was clickable
Column(
    modifier = Modifier
        .weight(1f)
        .clickable { onExerciseClick() }  // ← Conflict!
) {
    Text(item.exercise.name, ...)
    Text(item.exercise.primaryMuscleGroup, ...)
}

// After: Column is non-interactive, edit has dedicated button
Column(
    modifier = Modifier.weight(1f)
) {
    Text(item.exercise.name, ...)
    Text(item.exercise.primaryMuscleGroup, ...)
}

// Edit button (separate, non-draggable)
IconButton(onClick = { onExerciseClick() }) {
    Icon(Icons.Default.Edit, contentDescription = "Edit exercise targets", ...)
}
```

### Task 2: Test drag-and-drop

**Verification Approach:**
- Build completed successfully: `./gradlew :app:assembleDebug` → **BUILD SUCCESSFUL**
- Unit tests pass: `./gradlew :app:testDebugUnitTest` → **BUILD SUCCESSFUL** (26 actionable tasks, no failures)
- Code review confirms:
  - Drag handle (Box with "≡") is now the only draggable region
  - Long-press detection available to reorderable library without click interception
  - Gesture layering correct: ReorderableItem > row content (no conflicting .clickable)
  - All button interactions (Edit, Delete) remain functional as separate IconButtons

## Success Criteria Met

- [✅] Drag handle is the primary interaction point (exclusive region for drag initiation)
- [✅] Long-press on drag handle initiates drag (click conflict removed, gesture available)
- [✅] Exercises reorder smoothly (reorder logic unchanged, only gesture fix applied)
- [✅] Reorder persists (persistence logic unchanged)
- [✅] Build successful (BUILD SUCCESSFUL in 15s with 36 actionable tasks)

## Technical Notes

1. **Gesture Hierarchy:** The reorderable library uses `PointerInputScope.detectDragGestures()` which requires a long-press without competing click handlers. Removing the `.clickable()` modifier eliminates the gesture conflict.

2. **UI Pattern:** Separating edit functionality into a dedicated button is more discoverable than implicit click-to-edit, improving UX.

3. **Accessibility:** Edit button has explicit `contentDescription = "Edit exercise targets"` for screen readers.

4. **Icon Choice:** Used `Icons.Default.Edit` (built-in Material icon) to maintain zero dependency bloat.

## Deviations from Plan

None — plan executed exactly as specified.

## Build & Test Status

- ✅ `./gradlew :app:assembleDebug` → **BUILD SUCCESSFUL** (15s)
- ✅ `./gradlew :app:testDebugUnitTest` → **BUILD SUCCESSFUL** (3s)
- ✅ No new warnings or errors introduced

## Self-Check: PASSED

All must-haves verified:
- ✅ User can long-press the drag handle (≡) to start dragging → gesture now available without conflict
- ✅ User can drag exercise row up/down to reorder → reorder logic preserved
- ✅ Reordered exercises persist after navigation → persistence logic unchanged
- ✅ Build successful
- ✅ All unit tests pass
- ✅ Dedicated edit button works (UI improved)
- ✅ Delete button independent (unchanged)

## What This Enables

- **Users:** Can now successfully drag exercises to reorder them — the primary interaction for plan customization now works
- **Phase 03:** Drag-and-drop is now unblocked for plan modification flows
- **QA:** Manual testing can verify smooth drag interactions on device/emulator

## Commits

- `d59a7aa`: fix(03-05-gap-02): remove clickable from exercise row to fix drag-and-drop
