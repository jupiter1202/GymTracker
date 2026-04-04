---
phase: 03-workout-plans
plan: "05-gap-01"
type: execute
wave: 5.1
gap_closure: true
depends_on:
  - "03-05"
files_modified:
  - "app/src/main/java/de/jupiter1202/gymtracker/feature/plans/PlanDetailScreen.kt"
autonomous: true
requirements:
  - PLAN-03

must_haves:
  truths:
    - "Drag handle is visible on exercise rows"
    - "Long-press and drag on the handle reorders exercises"
    - "Reorder is persisted after navigation away and back"
  artifacts:
    - path: "app/src/main/java/de/jupiter1202/gymtracker/feature/plans/PlanDetailScreen.kt"
      provides: "Working drag-and-drop reordering"
---

<objective>
Fix drag-and-drop reordering in PlanDetailScreen. The reorderable library may not be detecting drag gestures properly due to interaction source conflicts or incorrect state binding.

**Problem:** Dragging exercises doesn't reorder them. The drag handle may be visible but gestures aren't working.

**Solution:** Review the ReorderableItem setup, ensure proper interaction sources, and verify the reorder callback is being triggered correctly.
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
</execution_context>

<tasks>

<task type="auto">
  <name>Task 1: Debug and fix drag-and-drop</name>
  <action>
1. Open PlanDetailScreen.kt, examine the ReorderableItem and reorder state setup
2. Check if the interaction source or pointerInput is conflicting with SwipeToDismissBox
3. Ensure the LazyColumn has proper longPressDraggable or pointer input
4. Verify the reorderState callback is correctly wired
5. Test by adding debug logging to the onReorder callback
6. Build and test drag-and-drop functionality
  </action>
</task>

<task type="auto">
  <name>Task 2: Verify persistence</name>
  <action>
1. Test reordering exercises
2. Navigate away from plan detail screen
3. Tap back into the plan
4. Verify order is preserved in the database
  </action>
</task>

</tasks>

<success_criteria>
- [ ] Drag handle is visible and draggable
- [ ] Exercises can be reordered by dragging
- [ ] Order persists after navigation
- [ ] Build successful, tests pass
</success_criteria>
