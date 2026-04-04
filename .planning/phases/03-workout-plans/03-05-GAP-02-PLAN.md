---
phase: 03-workout-plans
plan: "05-gap-02"
type: execute
wave: 5.2
gap_closure: true
depends_on:
  - "03-05-GAP-01"
files_modified:
  - "app/src/main/java/de/julius1202/gymtracker/feature/plans/PlanDetailScreen.kt"
autonomous: true
requirements:
  - PLAN-03

must_haves:
  truths:
    - "User can long-press the drag handle (≡) to start dragging"
    - "User can drag exercise row up/down to reorder"
    - "Reordered exercises persist after navigation"
  artifacts:
    - path: "app/src/main/java/de/julius1202/gymtracker/feature/plans/PlanDetailScreen.kt"
      provides: "Fixed drag-and-drop without gesture conflict"
---

<objective>
Fix drag-and-drop reordering by removing the `.clickable()` modifier that's consuming long-press gestures needed by the reorderable library.

**Problem:** The ExerciseRowWithDelete composable has a `.clickable()` modifier that prevents the reorderable library from detecting long-press drag gestures. Only the drag handle should be draggable, not the entire row.

**Solution:** Remove the `.clickable()` from the Row and move the click handler to a separate, non-draggable region (like a dedicated edit button) so drag gestures work properly.
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
</execution_context>

<tasks>

<task type="auto">
  <name>Task 1: Remove clickable from Row and add dedicated edit button</name>
  <action>
1. Open PlanDetailScreen.kt, find ExerciseRowWithDelete composable
2. Remove the `.clickable { onExerciseClick() }` modifier from the Row
3. Add a dedicated edit/info icon button (or use long-press on the text instead)
4. Ensure only the drag handle region is involved in the reorderable logic
5. Keep the delete button working independently
  </action>
</task>

<task type="auto">
  <name>Task 2: Test drag-and-drop</name>
  <action>
1. Build the app
2. Navigate to a plan detail screen
3. Try to drag an exercise by the drag handle (≡)
4. Verify you can reorder exercises
5. Verify reorder is persisted after navigation
  </action>
</task>

</tasks>

<success_criteria>
- [ ] Drag handle is the primary interaction point
- [ ] Long-press on drag handle initiates drag
- [ ] Exercises reorder smoothly
- [ ] Reorder persists
- [ ] Build successful
</success_criteria>
