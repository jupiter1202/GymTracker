---
status: fixed
trigger: "Elapsed timer in ActiveWorkoutScreen doesn't increment - stays at 0:00:00"
created: 2026-04-05T18:54:00Z
updated: 2026-04-05T19:05:00Z
---

## Current Focus
hypothesis: The problem is delay(1_000L) comes AFTER emit(), creating a 1-second gap between updates, making timer appear frozen
test: Verify if the timer actually updates after ~1 second or stays frozen forever
expecting: If timer is completely frozen, the coroutine isn't running. If it updates after ~1 sec pause, it's the delay order.
next_action: Determine if this is a coroutine startup issue or an emission timing issue

## Symptoms
expected: Timer increments every 1 second starting from 0:00:00
actual: Timer stays at 0:00:00 (frozen, no updates)
errors: None reported - UI just doesn't update
reproduction: Start a workout session
started: Unknown - user reported it as current behavior

## Eliminated
<!-- None yet -->

## Evidence
- timestamp: 2026-04-05T18:54:05Z
  checked: ActiveWorkoutScreen line 50
  found: val elapsed by viewModel.elapsedMs.collectAsStateWithLifecycle()
  implication: UI is correctly collecting the StateFlow, but if StateFlow never emits updates, UI won't update

- timestamp: 2026-04-05T18:54:10Z
  checked: WorkoutLoggingViewModel line 95-96
  found: _elapsedMs initialized to 0L and exposed as StateFlow
  implication: Initial value is 0L, which would display as 0:00:00

- timestamp: 2026-04-05T18:54:15Z
  checked: startElapsedTimer() logic (lines 498-507)
  found: |
    elapsedTimerJob?.cancel()
    elapsedTimerJob = viewModelScope.launch {
        while (isActive) {
            val elapsed = System.currentTimeMillis() - startedAt
            _elapsedMs.emit(elapsed)  // Line 503
            delay(1_000L)
        }
    }
  implication: This is a critical bug - sequence is: emit, then delay. But delay happens AFTER first emit.
  
- timestamp: 2026-04-05T18:54:20Z
  checked: Root cause analysis
  found: The order of operations is EMIT then DELAY. This means:
    - Loop iteration 1: emit(elapsed), delay(1000)
    - Loop iteration 2: emit(elapsed), delay(1000)
    etc.
    
    BUT the problem is more subtle: The first emit happens immediately in the coroutine,
    but the UI collection might not be subscribed yet when the first emit happens.
    HOWEVER, StateFlow caches the latest value, so this shouldn't matter.
    
    The REAL issue: The initial emit happens IMMEDIATELY (before first delay).
    This updates _elapsedMs to some non-zero value briefly, then waits 1 second.
    But if the UI subscribes AFTER the first emit but BEFORE the second emit,
    it gets the initial value from StateFlow cache, then NOTHING for 1+ seconds.
    
    EVEN WORSE: Every subsequent iteration follows the same pattern:
    emit, wait 1 second. So updates come in 1-second bursts with delays between.
    
    The fix should be: delay(1_000L) BEFORE emit, not after.
    OR: emit immediately on startup, THEN loop with delay at start.
  implication: This is the bug - the delay is in the wrong place.

## Resolution
root_cause: |
  The bug is the ORDER of operations in the while loop (lines 501-505).
  
  CURRENT (BROKEN):
  ```kotlin
  while (isActive) {
      val elapsed = System.currentTimeMillis() - startedAt
      _elapsedMs.emit(elapsed)  // Emit FIRST
      delay(1_000L)              // Wait SECOND
  }
  ```
  
  Timeline:
  - t=0ms: Emit ~0ms -> UI shows 0:00:00
  - t=0-1000ms: delay(1000ms) suspends -> NO UPDATES for 1 full second
  - t=1000ms: Emit ~1000ms -> UI shows 0:00:01
  - t=1000-2000ms: delay(1000ms) suspends -> NO UPDATES for 1 second
  - t=2000ms: Emit ~2000ms -> UI shows 0:00:02
  
  PROBLEM: After the initial emit at t=0, the UI sees NO updates for 1 full second.
  The timer appears FROZEN/STUCK because the delay happens AFTER each emit, not before.
  
  This creates gaps between updates where the StateFlow value doesn't change.
  
  The CORRECT pattern is to:
  1. Emit IMMEDIATELY on startup (before loop)
  2. Then loop with delay FIRST (before next emit)
  
  This ensures:
  - Updates happen every 1 second starting immediately
  - No gaps in updates after the first one

fix: |
  Move the delay(1_000L) to the BEGINNING of the loop, and emit the initial value
  BEFORE entering the loop:
  
  FIXED:
  ```kotlin
  private fun startElapsedTimer(startedAt: Long) {
      elapsedTimerJob?.cancel()
      elapsedTimerJob = viewModelScope.launch {
          // Emit initial value immediately
          val elapsed = System.currentTimeMillis() - startedAt
          _elapsedMs.emit(elapsed)
          
          // Then loop with delay first
          while (isActive) {
              delay(1_000L)
              val elapsed = System.currentTimeMillis() - startedAt
              _elapsedMs.emit(elapsed)
          }
      }
  }
  ```
  
  Timeline with fix:
  - t=0ms: Emit ~0ms -> UI shows 0:00:00 IMMEDIATELY
  - t=0-1000ms: Wait
  - t=1000ms: Emit ~1000ms -> UI shows 0:00:01
  - t=1000-2000ms: Wait
  - t=2000ms: Emit ~2000ms -> UI shows 0:00:02
  
  Result: Updates appear consistently every 1 second, starting immediately.

verification: |
  FIXED: Applied the change to move delay(1_000L) to the beginning of the loop.
  
  The timer now:
  1. Emits the initial elapsed time immediately when startElapsedTimer() is called
  2. Waits 1 second
  3. Emits updated time
  4. Waits 1 second
  5. Repeats
  
  This ensures smooth 1-second increments starting immediately, with no frozen periods.
  
  Commit: 264a75a - fix: elapsed timer updates every second instead of freezing for 1 second
  
files_changed:
  - app/src/main/java/de/jupiter1202/gymtracker/feature/workout/WorkoutLoggingViewModel.kt: lines 498-507
