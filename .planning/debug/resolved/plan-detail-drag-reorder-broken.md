---
status: resolved
trigger: "Drag-and-drop reordering of exercises in PlanDetailScreen doesn't work on device"
created: 2026-04-05T00:00:00Z
updated: 2026-04-05T12:00:00Z
---

## Current Focus

hypothesis: The drag handle Surface has NO draggableHandle/longPressDraggableHandle modifier from the reorderable library — it only has pointerInput(detectTapGestures) for logging, which actively consumes pointer events and prevents the library from initiating drag
test: Read PlanDetailScreen.kt drag handle implementation, check against sh.calvin.reorderable v3.0.0 API
expecting: Drag handle must use ReorderableCollectionItemScope.draggableHandle() or .longPressDraggableHandle() — without this modifier the library never knows a drag started
next_action: Human verification — install debug APK and test long-press drag on exercise rows

## Symptoms

expected: Long-pressing the drag handle (☰) on an exercise row initiates drag; user can drag the row up/down to reorder; reorder persists after navigation
actual: Reordering doesn't work — drag gesture is not detected or drag does not complete successfully
errors: None (no crash)
reproduction: Open PlanDetailScreen with exercises → try to long-press drag handle (≡) and drag
started: Was never confirmed working. 03-05-GAP-02 attempted to fix gesture conflict by removing .clickable() from row. Diagnostic logging added in commit 3dd672a.

## Eliminated

- hypothesis: .clickable() on exercise info column intercepting long-press gestures
  evidence: Fixed in 03-05-GAP-02 (d59a7aa) — .clickable() removed from Column. But drag still doesn't work.
  timestamp: 2026-04-05T00:00:00Z

## Evidence

- timestamp: 2026-04-05T00:01:00Z
  checked: PlanDetailScreen.kt drag handle implementation (lines 251-277)
  found: |
    The drag handle Surface has TWO modifiers:
    1. .pointerHoverIcon(PointerIcon.Hand) — desktop-only hover effect, does nothing on Android
    2. .pointerInput(Unit) { detectTapGestures(onPress=log, onLongPress=log) } — only logs, does not call library drag APIs
    MISSING: NO .draggableHandle() or .longPressDraggableHandle() modifier from ReorderableCollectionItemScope
  implication: |
    The reorderable library has NO way to detect that a drag was initiated. Without the library's own
    modifier on the handle, long-press is consumed by detectTapGestures (just logs it) and the
    drag never starts. The diagnostic logging confirmed touches reach the handle but the library
    is never notified.

- timestamp: 2026-04-05T00:02:00Z
  checked: sh.calvin.reorderable v3.0.0 API — ReorderableItem lambda signature
  found: |
    ReorderableItem lambda provides a ReorderableCollectionItemScope (referred to as `this` or
    named parameter). Inside that scope, you can call:
      - Modifier.draggableHandle() — immediate drag on touch
      - Modifier.longPressDraggableHandle() — drag after long press
    The drag handle composable MUST receive this scope and apply one of these modifiers.
    Without it, the library never intercepts the gesture.
  implication: |
    ExerciseRowWithDelete is called from inside ReorderableItem { isDragging -> ... } but the
    ReorderableCollectionItemScope is not passed into ExerciseRowWithDelete or its drag handle.
    The scope is effectively discarded.

- timestamp: 2026-04-05T00:03:00Z
  checked: ReorderableItem call site (PlanDetailScreen.kt lines 186-196)
  found: |
    ReorderableItem(state = reorderState, key = item.planExercise.id) { isDragging ->
        ExerciseRowWithDelete(item = item, ..., isDragging = isDragging)
    }
    The lambda `{ isDragging -> }` is actually `ReorderableCollectionItemScope.(Boolean) -> Unit`
    so `this` inside the lambda IS the scope. But `this` is never passed to ExerciseRowWithDelete.
  implication: |
    The fix must: pass `this` (the ReorderableCollectionItemScope) into ExerciseRowWithDelete,
    then pass it further into the drag handle so it can call .longPressDraggableHandle() on the
    Surface modifier.

- timestamp: 2026-04-05T00:04:00Z
  checked: .pointerInput(Unit) { detectTapGestures } on the drag handle
  found: |
    detectTapGestures installs a pointer input consumer that eagerly consumes tap/long-press
    gestures. Even after the reorderable modifier is added correctly, this pointerInput WILL
    compete with the library's gesture detection. The logging was diagnostic-only; it must be
    removed in the fix (or it will interfere with drag initiation).
  implication: |
    Two changes needed:
    1. Remove .pointerInput(Unit) { detectTapGestures } from the drag handle
    2. Add .longPressDraggableHandle() from the ReorderableCollectionItemScope

## Resolution

root_cause: |
  The drag handle Surface in ExerciseRowWithDelete is missing the .longPressDraggableHandle()
  modifier from sh.calvin.reorderable. The ReorderableCollectionItemScope (available inside the
  ReorderableItem lambda) was never passed down to the handle. Without this modifier the library
  has no way to intercept the long-press and start a drag. Additionally, the diagnostic
  .pointerInput { detectTapGestures } added in commit 3dd672a actively consumes the long-press
  event, further preventing any downstream gesture detection.

fix: |
  1. Add ReorderableCollectionItemScope parameter to ExerciseRowWithDelete
  2. Pass `this` (the scope) from the ReorderableItem lambda into ExerciseRowWithDelete
  3. Replace .pointerInput(detectTapGestures) on the drag handle with .longPressDraggableHandle()
     called on the scope
  4. Remove unused imports (detectTapGestures, PointerIcon, pointerHoverIcon, pointerInput)

verification: Build successful (19s, 36 tasks). Confirmed fixed on device — long-press drag now initiates reordering correctly.
files_changed:
  - app/src/main/java/de/jupiter1202/gymtracker/feature/plans/PlanDetailScreen.kt
