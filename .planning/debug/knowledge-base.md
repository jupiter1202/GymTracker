# GSD Debug Knowledge Base

Resolved debug sessions. Used by `gsd-debugger` to surface known-pattern hypotheses at the start of new investigations.

---

## elapsed-timer-stuck — Elapsed timer stuck at 0:00:00 - delay placed after emit instead of before

- **Date:** 2026-04-05
- **Error patterns:** elapsed, timer, 0:00:00, frozen, stuck, delay, emit, coroutine, StateFlow, workout duration, inactive
- **Root cause:** In startElapsedTimer(), the delay(1_000L) was placed AFTER the emit(), causing a 1-second gap after each update. Timeline: emit at t=0, delay 1s, emit at t=1s, delay 1s, etc. This created frozen periods where the UI saw no updates for 1+ seconds, making the timer appear stuck at 0:00:00.
- **Fix:** Moved delay to BEGINNING of the loop and added initial emit before the loop. Now: emit immediately at t=0, delay 1s, emit at t=1s, repeat. Ensures smooth 1-second increments with no gaps.
- **Files changed:** app/src/main/java/de/jupiter1202/gymtracker/feature/workout/WorkoutLoggingViewModel.kt

---

## plan-detail-drag-reorder-broken — Drag handle missing longPressDraggableHandle modifier from reorderable library
- **Date:** 2026-04-05
- **Error patterns:** drag, reorder, drag-and-drop, long-press, gesture, ReorderableItem, sh.calvin.reorderable, drag handle, pointerInput, detectTapGestures
- **Root cause:** The drag handle Surface in ExerciseRowWithDelete was missing .longPressDraggableHandle() from sh.calvin.reorderable. The ReorderableCollectionItemScope (available as `this` inside the ReorderableItem lambda) was never passed down to ExerciseRowWithDelete or its drag handle, so the library had no way to intercept the long-press gesture and start a drag. Additionally, a diagnostic .pointerInput { detectTapGestures } was actively consuming long-press events, further blocking drag initiation.
- **Fix:** Added ReorderableCollectionItemScope parameter to ExerciseRowWithDelete, passed `this` from the ReorderableItem lambda into it, replaced the .pointerInput { detectTapGestures } on the drag handle Surface with .longPressDraggableHandle() called on the scope, and removed unused imports.
- **Files changed:** app/src/main/java/de/jupiter1202/gymtracker/feature/plans/PlanDetailScreen.kt
---
