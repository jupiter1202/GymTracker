# GSD Debug Knowledge Base

Resolved debug sessions. Used by `gsd-debugger` to surface known-pattern hypotheses at the start of new investigations.

---

## plan-detail-drag-reorder-broken — Drag handle missing longPressDraggableHandle modifier from reorderable library
- **Date:** 2026-04-05
- **Error patterns:** drag, reorder, drag-and-drop, long-press, gesture, ReorderableItem, sh.calvin.reorderable, drag handle, pointerInput, detectTapGestures
- **Root cause:** The drag handle Surface in ExerciseRowWithDelete was missing .longPressDraggableHandle() from sh.calvin.reorderable. The ReorderableCollectionItemScope (available as `this` inside the ReorderableItem lambda) was never passed down to ExerciseRowWithDelete or its drag handle, so the library had no way to intercept the long-press gesture and start a drag. Additionally, a diagnostic .pointerInput { detectTapGestures } was actively consuming long-press events, further blocking drag initiation.
- **Fix:** Added ReorderableCollectionItemScope parameter to ExerciseRowWithDelete, passed `this` from the ReorderableItem lambda into it, replaced the .pointerInput { detectTapGestures } on the drag handle Surface with .longPressDraggableHandle() called on the scope, and removed unused imports.
- **Files changed:** app/src/main/java/de/jupiter1202/gymtracker/feature/plans/PlanDetailScreen.kt
---
