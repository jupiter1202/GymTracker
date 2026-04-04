---
phase: 03-workout-plans
plan: "04-gap-01"
type: execute
wave: 4.1
gap_closure: true
depends_on:
  - "03-04"
files_modified:
  - "app/src/main/java/de/jupiter1202/gymtracker/feature/plans/PlanDetailScreen.kt"
autonomous: true
requirements:
  - PLAN-01

must_haves:
  truths:
    - "Add exercise button is visible at bottom of exercise list"
    - "Button is clickable and opens exercise picker"
    - "Layout is clean without visual clipping"
  artifacts:
    - path: "app/src/main/java/de/julius1202/gymtracker/feature/plans/PlanDetailScreen.kt"
      provides: "Fixed layout with visible add button"
---

<objective>
Fix the missing "Add exercise" button in PlanDetailScreen by moving it inside the LazyColumn so it appears at the bottom of the exercise list and is always visible.

**Problem:** The PlanExerciseList LazyColumn uses Modifier.fillMaxSize(), which causes it to fill all available space in the parent Column. This hides the "Add exercise" ListItem that's placed below the LazyColumn.

**Solution:** Move the "Add exercise" button from the outer Column into the LazyColumn as the last item, so it appears at the end of the scrollable exercise list.
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
@$HOME/.config/opencode/get-shit-done/templates/summary.md
</execution_context>

<tasks>

<task type="auto">
  <name>Task 1: Move Add button into LazyColumn</name>
  <files>
    - app/src/main/java/de/jupiter1202/gymtracker/feature/plans/PlanDetailScreen.kt
  </files>
  <action>
1. Open PlanDetailScreen.kt
2. Find the PlanExerciseList() call (around line 82)
3. Modify PlanExerciseList to accept an onAddExercise callback
4. Inside PlanExerciseList LazyColumn, add a stickyHeader or item at the end with the "Add exercise" ListItem
5. Pass the showExercisePicker trigger from parent through the callback
6. Update the parent Column to remove the old "Add exercise" ListItem (lines 97-104)
7. Build and verify: ./gradlew :app:assembleDebug
  </action>
  <verify>
    <manual>The Add exercise button should appear at the bottom of the exercise list, after all exercises. It should be clickable without scrolling issues.</manual>
  </verify>
  <done>LazyColumn now includes the Add button as its final item. Parent Column no longer has the duplicate button.</done>
</task>

<task type="auto">
  <name>Task 2: Test and verify</name>
  <files></files>
  <action>
1. Run full build: ./gradlew :app:assembleDebug
2. Run tests: ./gradlew :app:testDebugUnitTest
3. Verify no new compilation errors
  </action>
  <verify>
    <automated>./gradlew :app:assembleDebug 2>&1 | tail -5</automated>
    <automated>./gradlew :app:testDebugUnitTest 2>&1 | tail -5</automated>
  </verify>
  <done>Build successful, all tests pass, no new errors or warnings.</done>
</task>

</tasks>

<success_criteria>
- [ ] Add exercise button visible at bottom of exercise list in PlanDetailScreen
- [ ] Button is clickable and opens exercise picker
- [ ] No layout clipping or overflow issues
- [ ] Build successful with no new errors
- [ ] All tests pass
</success_criteria>

<output>
Create SUMMARY.md documenting the fix.
</output>
