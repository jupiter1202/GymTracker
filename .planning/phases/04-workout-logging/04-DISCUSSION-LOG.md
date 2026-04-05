# Phase 4: Workout Logging - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions captured in CONTEXT.md — this log preserves the discussion.

**Date:** 2026-04-05
**Phase:** 04-workout-logging
**Mode:** discuss
**Areas discussed:** Active workout screen layout, Session start flow, Rest timer display, Workout completion & resume

## Gray Areas Presented

All four areas were selected for discussion.

## Discussion Summary

### Active Workout Screen Layout
| Question | Choice | Notes |
|----------|--------|-------|
| Screen layout | Single scrollable screen | LazyColumn with all exercises visible at once |
| Set input method | Inline row with pre-filled fields | Weight + reps fields, checkmark to log — no bottom sheet |

### Session Start Flow
| Question | Choice | Notes |
|----------|--------|-------|
| Start entry point | Dashboard quick-start + Plans tab | Two entry points; Dashboard surfaces most recent plan |
| Ad-hoc exercise adding | Exercise picker bottom sheet | Reuses PlanDetailScreen component |

### Rest Timer
| Question | Choice | Notes |
|----------|--------|-------|
| Timer display | Sticky banner at top | Pinned below top app bar, always visible while scrolling |
| Timer alert | Vibration + sound | Works even with screen off; configurable in Settings |

### Workout Completion & Resume
| Question | Choice | Notes |
|----------|--------|-------|
| Finish workout | Summary screen | Duration, exercises, sets, volume — then to History tab |
| Reopen with active session | Auto-redirect | Direct to active workout, no prompt needed |

## Corrections Made

None — all recommended options confirmed.
