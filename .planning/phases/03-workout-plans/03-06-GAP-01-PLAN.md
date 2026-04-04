---
phase: 03-workout-plans
plan: "06-gap-01"
type: execute
wave: 6.1
gap_closure: true
depends_on:
  - "03-06"
files_modified:
  - "app/src/main/java/de/jupiter1202/gymtracker/feature/plans/WorkoutPlanRepository.kt"
autonomous: true
requirements:
  - PLAN-02

must_haves:
  truths:
    - "Template preview shows all exercises from the template JSON"
    - "Importing a template creates a plan with all exercises"
    - "No exercises are skipped due to name mismatch"
  artifacts:
    - path: "app/src/main/java/de/julius1202/gymtracker/feature/plans/WorkoutPlanRepository.kt"
      provides: "Fixed template import with exercise name matching"
---

<objective>
Fix template exercise loading so all exercises from templates appear in the preview and are imported correctly.

**Problem:** Template preview shows incomplete exercise lists, and importing a template loses some exercises.

**Solution:** Debug exercise name matching to ensure template exercise names are found in the exercise database. Add logging and fix any name mismatches.
</objective>

<execution_context>
@$HOME/.config/opencode/get-shit-done/workflows/execute-plan.md
</execution_context>

<tasks>

<task type="auto">
  <name>Task 1: Debug exercise name matching</name>
  <action>
1. Open WorkoutPlanRepository.importTemplate()
2. Add detailed logging for each template exercise being matched
3. Query the exercise database to list all exercise names
4. Compare template exercise names with database names (case-insensitive)
5. Identify any mismatches
6. Build and test template import with logging enabled
  </action>
</task>

<task type="auto">
  <name>Task 2: Fix mismatches</name>
  <action>
1. If exercise names don't match due to case or formatting, implement fuzzy matching
2. Or update template JSON to use exact exercise names from the database
3. Test that all template exercises are now imported correctly
4. Run build and tests
  </action>
</task>

</tasks>

<success_criteria>
- [ ] All template exercises appear in preview
- [ ] Template import includes all exercises
- [ ] No silent failures (all exercises matched)
- [ ] Build successful
</success_criteria>
