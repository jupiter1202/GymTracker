# Feature Landscape

**Domain:** Android gym/workout tracking app
**Researched:** 2026-04-04
**Overall confidence:** MEDIUM (based on training data knowledge of Strong, Hevy, FitNotes, JEFIT, GymRun, OpenBarbell ecosystem; no live web verification available)

## Table Stakes

Features users expect from any gym tracker. Missing any of these and users uninstall within the first session.

| Feature | Why Expected | Complexity | Notes |
|---------|--------------|------------|-------|
| **Log sets with weight + reps** | The entire point of the app. Every competitor does this. | Low | Must support kg/lbs toggle. Decimal weight support (e.g., 2.5kg plates). |
| **Exercise library (pre-populated)** | Users will not manually create 50+ exercises before their first workout. Strong ships ~300, Hevy ~800+. | Medium | Need at minimum 100-150 common exercises with muscle group tags. Can start with curated list, not encyclopedic. |
| **Exercise search and filter** | Users need to find exercises fast mid-workout. Search by name + filter by muscle group is baseline. | Low | Muscle group filter is more important than keyword search for discovery. |
| **Custom exercise creation** | No library covers every exercise. Users WILL have niche movements. Strong and Hevy both support this. | Low | Name + target muscle group at minimum. |
| **Workout history** | Users need to see what they did last Tuesday. This is how they decide today's weights. | Low | List view sorted by date. Tapping a date shows full session detail. |
| **Rest timer** | Every serious gym app has one. Strong made this a signature feature. Users time rest between sets. | Low | Configurable default duration (60s, 90s, 120s, etc.). Auto-start after logging a set. Notification when timer expires. |
| **Previous performance display** | During a workout, show what the user did last time for the same exercise. This is the #1 workflow for progressive overload. | Medium | Must match by exercise within the same workout template/routine, or fall back to last time that exercise was performed globally. |
| **Workout templates/routines** | Users repeat the same workouts weekly. Nobody wants to add 6 exercises from scratch every session. | Medium | Create once, reuse many times. Pre-fill with last session's weights. |
| **Body weight tracking** | Nearly universal in the category. Simple weight log over time. | Low | Date + weight entry. Chart over time. |
| **Basic progress charts** | Users want to see strength going up. At minimum: weight lifted over time per exercise. | Medium | Line chart of estimated 1RM or max weight per exercise over time. Date range selector. |
| **Data persistence (local)** | Losing workout data is unforgivable. This is months/years of training history. | Low | Room database. Reliable, no data loss. |
| **Unit toggle (kg/lbs)** | International user base. Strong defaults to user's locale but allows override. | Low | Global setting + per-exercise override is nice-to-have. Global-only is acceptable for v1. |

## Differentiators

Features that set the app apart. Not expected on day one, but create competitive advantage and retention.

| Feature | Value Proposition | Complexity | Notes |
|---------|-------------------|------------|-------|
| **Pre-built program templates (PPL, 5x5, nSuns, GZCLP)** | Strong charges $5/mo for more than 3 routines. Hevy paywalls custom routines beyond a limit. Shipping popular programs for free is a direct competitive advantage. | Medium | Requires a data model that supports periodization (weight progression rules, deload logic). Start with 3-5 popular programs. |
| **Unlimited routines (free)** | Strong limits to 3 free routines. This is the #1 complaint in app store reviews. Making routines unlimited and free is the core value prop per PROJECT.md. | Low | Just don't add the artificial limit. The differentiator is the absence of paywall, not a feature. |
| **Superset / circuit logging** | Hevy supports supersets well. Strong added it later. Users doing supersets, drop sets, or circuits need grouped exercise logging. | Medium | UI challenge: how to visually group exercises and track rest between groups vs. between sets. |
| **Workout notes and set notes** | Per-workout and per-set text notes. "Left shoulder felt tight", "Used belt for last 2 sets". Strong and Hevy both support this. | Low | Simple text field. High value, low effort. |
| **Personal records (PR) detection and celebration** | Automatic detection when user hits a new max weight, max reps, or max volume for an exercise. Strong shows a crown icon. Hevy shows confetti. | Medium | Requires comparison logic against all historical data for that exercise. Great for motivation. |
| **Body measurements beyond weight** | Chest, waist, arms, thighs, etc. FitNotes does this well. Most apps only track body weight. | Low | Simple date + measurement entries per body part. Chart per measurement. |
| **Export / import data** | CSV export is table stakes for power users. Strong exports to CSV. This matters hugely for an open-source app -- users want data ownership. | Medium | CSV export of workout history. CSV/JSON import for migration from other apps. |
| **Plate calculator** | Shows which plates to load on each side of the bar. Nice gym-floor utility. | Low | Input target weight, output plate breakdown per side. Configurable available plates. |
| **Workout duration tracking** | Auto-track start/end time of workout. Show duration in history. | Low | Start timer when workout begins, stop when finished. Display in history. |
| **Estimated 1RM calculation** | Calculate one-rep max from submaximal sets using Epley/Brzycki formula. Show on progress charts. | Low | Formula is simple. Display alongside actual performance data. |
| **Dark theme** | Android users expect dark mode support. Material You makes this straightforward with Jetpack Compose. | Low | Use Material 3 dynamic theming. Support system-level dark mode toggle. |

## Anti-Features

Features to explicitly NOT build in v1. Either too complex, not core, or scope-creeping.

| Anti-Feature | Why Avoid | What to Do Instead |
|--------------|-----------|-------------------|
| **Cloud sync / account system** | Massive infrastructure scope. Requires backend, auth, conflict resolution. PROJECT.md explicitly excludes this. | Local-only with robust export/import so users can manually back up. |
| **Social features (feed, sharing, followers)** | Hevy's social feed is polarizing -- many users find it distracting. Not the use case per PROJECT.md. | Focus on personal tracking. |
| **AI workout generation** | Requires ML pipeline or API integration. Scope explosion. PROJECT.md excludes. | Ship good pre-built templates instead. |
| **Video/image exercise demonstrations** | Massive storage requirement. Licensing issues. Every video becomes stale. | Text descriptions of exercises + target muscle group tags. Link to external resources if needed. |
| **Cardio / running / cycling tracking** | Different domain entirely. GPS tracking, heart rate zones, distance -- all complex. Dilutes the strength training focus. | Keep scope to resistance training. Add a generic "cardio" exercise type with duration + calories fields if needed, but don't build a running tracker. |
| **Meal / nutrition tracking** | Completely separate domain. MyFitnessPal, Cronometer own this space. | Out of scope. Don't even add calorie fields. |
| **Wearable / smartwatch integration** | Wear OS companion app is a full second app. Bluetooth device pairing is complex. | Not in v1. Could be a future milestone. |
| **Gamification (badges, streaks, leaderboards)** | Adds complexity without core value. Streaks create guilt, not motivation, for many users. | Simple PR detection covers the motivational aspect without gamification overhead. |
| **Built-in workout timer / interval timer** | Different from rest timer. A full interval timer (HIIT/Tabata) is a separate app. | Rest timer between sets only. |
| **Apple Health / Google Fit integration** | Platform API integration adds testing burden and is not core to logging. | Defer to future milestone. Nice-to-have, not essential. |

## Feature Dependencies

```
Exercise Library --> Custom Exercise Creation (library is the base, custom extends it)
Exercise Library --> Workout Templates (templates reference exercises)
Workout Templates --> Pre-built Programs (programs are specialized templates with progression)
Log Sets (weight + reps) --> Previous Performance Display (needs historical data)
Log Sets (weight + reps) --> Progress Charts (needs historical data)
Log Sets (weight + reps) --> PR Detection (needs historical data to compare)
Log Sets (weight + reps) --> Estimated 1RM (calculated from set data)
Body Weight Tracking --> Body Measurements (same pattern, extended)
Workout Templates --> Superset Logging (supersets exist within template structure)
Data Persistence --> Export/Import (need stored data to export)
Rest Timer --> (independent, no dependencies)
Unit Toggle --> Log Sets (unit system affects how weights are displayed/entered)
```

### Dependency Layers (build order)

```
Layer 0 (Foundation):  Data Persistence, Unit Toggle, Exercise Library
Layer 1 (Core Loop):   Log Sets, Custom Exercises, Rest Timer, Workout Duration
Layer 2 (Templates):   Workout Templates, Previous Performance Display, Workout History
Layer 3 (Insights):    Progress Charts, PR Detection, Estimated 1RM, Body Weight Tracking
Layer 4 (Programs):    Pre-built Programs (PPL, 5x5, etc.), Body Measurements, Export/Import
Layer 5 (Polish):      Superset Logging, Plate Calculator, Workout Notes, Dark Theme
```

## MVP Recommendation

### Must ship in v1 (without these, the app is not usable):

1. **Exercise library** with 100+ pre-populated exercises, search, and muscle group filter
2. **Log sets with weight + reps** -- the core interaction
3. **Workout templates/routines** -- nobody wants to build workouts from scratch every session
4. **Previous performance display** -- this is how users do progressive overload
5. **Workout history** -- users need to review past sessions
6. **Rest timer** -- expected by every gym-goer
7. **Body weight tracking** -- simple, high value
8. **Basic progress charts** -- users need to see gains

### Ship shortly after MVP (high value, moderate effort):

9. **Pre-built program templates** (PPL, 5x5) -- this is the differentiator vs. paywalled competitors
10. **PR detection** -- motivational, medium complexity
11. **Export/import** -- critical for open-source credibility and data ownership
12. **Unit toggle (kg/lbs)** -- technically should be in MVP, but can ship with user's locale default initially

### Defer to later milestones:

- Superset logging (UI complexity)
- Plate calculator (nice-to-have utility)
- Extended body measurements (body weight alone covers 80% of use case)
- Dark theme can be deferred if using Material 3 defaults (which support it out of the box)

## Competitor Feature Matrix

| Feature | Strong (Free) | Strong (Pro) | Hevy (Free) | Hevy (Pro) | FitNotes | GymTracker (Target) |
|---------|--------------|-------------|-------------|------------|----------|-------------------|
| Log sets (weight/reps) | Yes | Yes | Yes | Yes | Yes | Yes |
| Exercise library | ~300 | ~300 | ~800+ | ~800+ | ~100 | 100-150 |
| Custom exercises | Yes | Yes | Yes | Yes | Yes | Yes |
| Workout routines | 3 limit | Unlimited | Limited | Unlimited | Unlimited | **Unlimited (free)** |
| Rest timer | Yes | Yes | Yes | Yes | Yes | Yes |
| Previous performance | Yes | Yes | Yes | Yes | Yes | Yes |
| Progress charts | Basic | Advanced | Basic | Advanced | Yes | Yes |
| Body weight | Yes | Yes | Yes | Yes | Yes | Yes |
| Body measurements | No | No | No | Yes | Yes | Yes (later) |
| Pre-built programs | No | Yes | No | Yes | No | **Yes (free)** |
| Supersets | Yes | Yes | Yes | Yes | No | Later |
| PR detection | Yes | Yes | Yes | Yes | No | Yes |
| Export data | CSV | CSV | CSV | CSV | CSV | CSV + JSON |
| Cloud sync | No | Yes | Yes | Yes | No | No (local-first) |
| Social features | No | No | Yes | Yes | No | No |
| Price | Free | ~$5/mo | Free | ~$9/mo | Free | **Free (OSS)** |

## Key Insight: Where the Opportunity Is

The competitive gap is clear: **Strong and Hevy lock routines and programs behind paywalls**. The single most impactful differentiator for GymTracker is:

1. **Unlimited workout routines for free** (Strong's #1 complaint in reviews is the 3-routine limit)
2. **Pre-built popular programs for free** (5x5, PPL, nSuns, GZCLP -- these are community-created programs being paywalled by commercial apps)
3. **Full data ownership** via open source + export -- appeals to privacy-conscious and data-ownership-minded users

The app does NOT need to compete on exercise library size, social features, or polish. It needs to nail the core logging loop and give away what competitors charge for.

## Sources

- Training data knowledge of Strong, Hevy, FitNotes, JEFIT, GymRun feature sets (MEDIUM confidence -- based on app analysis up to early 2025, features may have changed)
- Strong App Store reviews and common complaints (MEDIUM confidence -- from training data)
- PROJECT.md requirements and constraints (HIGH confidence -- direct project input)
- General gym/fitness app UX patterns (MEDIUM confidence -- from training data)

**Note:** Web search was unavailable during this research session. All findings are based on training data (cutoff: May 2025). Feature sets of competing apps may have changed since then. Recommend spot-checking competitor features before finalizing the roadmap.
