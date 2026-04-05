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

## summary-stuck-loading — Workout summary screen shows infinite loading spinner (race condition in finishSession)
- **Date:** 2026-04-05
- **Error patterns:** summary, loading, CircularProgressIndicator, stuck, infinite, spinner, finishSession, race condition, navigation, StateFlow, null, async
- **Root cause:** finishSession() was a regular function that scheduled summary computation on viewModelScope.launch (async) but returned true immediately before the coroutine executed. This caused navigation to WorkoutSummaryScreen to happen before _summary StateFlow was populated, leaving the UI displaying CircularProgressIndicator forever.
- **Fix:** Changed finishSession() to a suspend function that computes and sets the summary synchronously before returning. Wrapped all calls to finishSession() in scope.launch { } to properly await completion. Navigation now only happens after summary is ready.
- **Files changed:** app/src/main/java/de/jupiter1202/gymtracker/feature/workout/WorkoutLoggingViewModel.kt, app/src/main/java/de/jupiter1202/gymtracker/feature/workout/ActiveWorkoutScreen.kt
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
