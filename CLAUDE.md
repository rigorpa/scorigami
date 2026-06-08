# Scorigami — Disc Golf Scoring App

## What this project is
A native Android + Wear OS disc golf scoring app built for:
- **Phone:** Google Pixel 8 Pro
- **Watch:** Google Pixel Watch 2 (Wear OS 4)

Players are added before a round. Scores are entered on the phone **and** the watch in real time. Courses store per-hole par values. When a round ends, the phone shows a full review screen before finalizing — the watch cannot finalize a round (screen too small).

---

## Architecture

**Multi-module Kotlin project** (Gradle Kotlin DSL, `libs.versions.toml` version catalog):

```
iThrow/
├── shared/   # Room DB, entities, DAOs, sync message contracts
├── app/      # Phone app — Jetpack Compose, MVVM, Hilt
└── wear/     # Wear OS app — Compose for Wear OS, stateless (driven by phone)
```

**Pattern:** MVVM with `StateFlow` / `collectAsStateWithLifecycle`. Hilt for DI throughout.

**Storage:** Room DB lives on the phone only. The watch has no local DB.

**Phone ↔ Watch sync** via Wearable Data Layer API:
- Phone → Watch: `WearSyncManager.pushRoundState()` delivers the `RoundState` via `DataClient.putDataItem` (persistent, survives reconnect) on every score change or hole navigation
- Watch → Phone: `MessageClient.sendMessage` sends a JSON `ScoreUpdateMessage` to `/score/update` when the user taps −/+
- Watch polls `DataClient` every 2 s while foregrounded (`WearViewModel.startPolling()`) as a fallback for missed push events
- `WearSyncManager` (`app/sync/`) handles all phone-side pushing
- `WearListenerService` (`wear/service/`) receives data-item changes and writes to `RoundStateHolder` (a singleton `StateFlow`)
- `PhoneWearableListenerService` (`app/service/`) receives score messages from the watch, writes to Room, then re-pushes updated state

---

## Key Libraries

| Library | Version | Purpose |
|---|---|---|
| Jetpack Compose BOM | 2024.12.01 | Phone UI |
| Wear Compose | 1.4.0 | Watch UI |
| Room | 2.6.1 | Local DB (phone only) |
| Hilt | 2.51.1 | Dependency injection |
| play-services-wearable | 18.2.0 | Phone ↔ Watch Data Layer |
| kotlinx.serialization | 1.7.3 | JSON for sync messages |
| Navigation Compose | 2.8.5 | Phone navigation |
| Wear Compose Navigation | 1.4.0 | Watch navigation |

Min SDK: 30 · Compile SDK: 35 · Kotlin: 2.0.21 · AGP: 8.7.0

---

## Data Model (shared module)

```
courses     id, name, holeCount
holes       id, courseId, number, par, distanceFeet?     (FK → courses CASCADE)
players     id, name, createdAt
rounds      id, courseId, startedAt, completedAt?        (completedAt=null = active)
round_players  roundId, playerId, order                  (composite PK)
scores      roundId, playerId, holeNumber, throws        (composite PK, upsert on change)
```

**DB version: 3**
- Migration 1→2: adds `distanceMeters INTEGER` nullable column to `holes`
- Migration 2→3: renames `distanceMeters` → `distanceFeet` (values were always stored in feet)

**Sync types** (`shared/sync/`):
- `RoundState` — full snapshot pushed phone→watch (roundId, courseName, currentHole, totalHoles, players[], **holePars: Map<Int,Int>**). `holePars` maps every hole number to its par value so the watch can apply the first-press scoring logic for any hole it navigates to independently.
- `PlayerState` — per-player data inside RoundState (playerId, name, **holeScores: Map<Int,Int>**, totalThrows, totalVsPar). `holeScores` maps every hole number the player has scored to their throw count, allowing the watch to display the correct score for whichever hole it is viewing independently of the phone.
- `ScoreUpdateMessage` — watch→phone message (roundId, playerId, holeNumber, throws, viewingHole)

---

## Pre-Seeded Courses

Inserted on first launch by `DatabaseSeeder.seedIfEmpty()` (called from `DatabaseModule`):

| Course | Holes | Par | Notes |
|---|---|---|---|
| Los Colomos | 18 | 56 | H2 and H13 are Par 4; all others Par 3 |
| El Centinela | 18 | 54 | All holes Par 3 |

Both courses include per-hole `distanceFeet` values. Displayed on the scorecard as "xxx ft / xxx m".

---

## Phone App Screens

| Screen | Route | ViewModel |
|---|---|---|
| HomeScreen | `home` | RoundViewModel |
| CourseListScreen | `course_list` | CourseViewModel |
| CourseEditorScreen | `course_editor?courseId={id}` | CourseViewModel |
| RoundSetupScreen | `round_setup` | RoundViewModel + CourseViewModel |
| ScorecardScreen | `scorecard` | RoundViewModel |
| RoundReviewScreen | `round_review` | RoundViewModel |
| HistoryScreen | `history` | HistoryViewModel |
| RoundDetailScreen | `round_detail/{roundId}` | HistoryViewModel |

Navigation is in `app/navigation/AppNavigation.kt`.

### ScorecardScreen layout
- **Top bar:** course name in `FontFamily.Cursive`; `TableChart` icon button (opens full scorecard sheet); "End Round" button; ⋮ overflow menu
- **Full scorecard sheet:** `ModalBottomSheet` opened via the `TableChart` icon — same per-player 18-hole breakdown shown in `RoundReviewScreen` (hole number + vs-par grid, totals)
- **Hole card:** large hole number (yellow), par, distance; ◀/▶ arrow buttons
- **Player cards:** 3-letter uppercase abbreviation (40sp, bold, white) on the left; round vs-par centered; −/+ score controls on the right with the current throw count displayed between them (0 shown as `"—"`)
- **Hole transitions:** `AnimatedContent` slides player cards left/right matching navigation direction (250 ms); hole number springs from 82 % → 100 % with `Spring.DampingRatioMediumBouncy` on each navigation (`Animatable` + `LaunchedEffect`)
- **Hole jump:** `GolfCourse` icon + `DropdownMenu` at bottom-right; max height 480 dp (~10 visible items); scrollable via inner `Column` + `verticalScroll`; `ExpandLess`/`ExpandMore` overlay arrows appear via `derivedStateOf` when content exists above/below the visible window; holes with any missing player score show a small amber dot (`0xFFFFB300`, 8 dp, `CircleShape`) in the `trailingIcon` slot

---

## Watch App Screens

| Screen | Route | Notes |
|---|---|---|
| NoRoundScreen | `no_round` | Shown when no active round |
| WearScorecardScreen | `scorecard` | Current hole, −/+ per player, hole nav |
| EndRoundPromptScreen | `end_round_prompt` | Tells user to finalize on phone |

Navigation is in `wear/navigation/WearNavigation.kt`. State comes from `RoundStateHolder.state` (a singleton `StateFlow` updated by `WearListenerService`).

### WearScorecardScreen layout — Sequential score entry (branch: `Before-Major-Wear-App-UI-Score-Entry`)

The watch scorecard uses a **one-player-at-a-time** flow instead of showing all players simultaneously:

1. Screen shows the current player (honor-sorted) with hole number, their name, −/+ controls, and an **Enter** button
2. Tapping **Enter** commits the score to the phone and advances to the next player; if `pendingScore` is 0 the advance happens without writing a score
3. The last player's button reads **"Next Hole ▶"** — tapping it commits their score (if > 0) and navigates to the next hole automatically

**Layout per player:**
- "Hole X / 18" in yellow `title2` (ExtraBold) at top — tappable to open the hole-jump picker
- Player's full name in white `title1` (SemiBold), centered — tappable to open the tee-order popup
- −/+ `CompactButton` (36 dp, primary-color fill, 18sp) with score between them
- Enter / Next Hole ▶ `Chip` centered below (slim 36 dp height)

**Hole-jump picker:** `ScalingLazyColumn` of hole chips. Holes with any missing player score show a small amber dot (`0xFFFFB300`, 6 dp, `CircleShape`) to the right of the hole number text.

**Honor system on watch:** Players are sorted locally using a cascading comparator — primary key is `holeScores[currentHole - 1]`, ties broken by `holeScores[currentHole - 2]`, continuing back to hole 1, then DB registration order. No phone re-push needed when moving to a new hole.

**`pendingScore` state:** keyed on `(currentPlayer.playerId, currentHole)` so it resets correctly per player per hole, even if the phone pushes a reordered `roundState.players` mid-entry.

**Navigation:** Uses standard `NavHost` (from `androidx.navigation.compose`) instead of `SwipeDismissableNavHost`. Horizontal swipe gestures are **not** used on the watch — they conflict with the Pixel Watch 2 system back gesture (right-edge swipe → watch face). Hole navigation is via the hole-jump picker and the Next Hole ▶ button.

**End Round chip** is present in code but commented out — uncomment the `Chip` block in `WearScorecardScreen` to re-enable.

---

## How Scoring Works

**First-press scoring:** When a hole score is 0 (not yet entered), the first button press jumps to a smart default rather than incrementing from 0:
- Tapping `−` → `maxOf(1, par − 1)` (birdie assumption — Par 3 enters 2)
- Tapping `+` → `par` (even par assumption — Par 3 enters 3)

Subsequent presses increment/decrement normally. Pressing `−` from any score > 0 decrements by 1 (reaching 0 clears the score back to "not entered"). Applies on both phone and watch. Par is looked up from `holes` (phone) or `roundState.holePars[currentHole]` (watch).

1. User taps −/+ on a player row (phone or watch)
2. Score is written to Room (`ScoreEntity` upsert)
3. `RoundViewModel` observes both `scoreDao.getScoresForRound()` and `playerDao.getPlayersForRoundFlow()` via `combine` inside `flatMapLatest` — picks up DB changes automatically
4. Players are re-sorted for the current hole (honor system — see below)
5. `pushStateToWatch()` is called (150 ms debounce) → `WearSyncManager.pushRoundState()` → DataClient put + MessageClient send
6. Watch receives new `RoundState` in `WearListenerService` → updates `RoundStateHolder` → `WearViewModel` recomposes

When update originates from watch: step 2 happens in `PhoneWearableListenerService` instead, then steps 3-6 follow.

---

## Round Finalization Flow

1. "End Round" on **phone** → navigates to `RoundReviewScreen`
   - Full scrollable scorecard per player (per-hole vs-par grid, totals)
   - Final standings card (sorted by score, medal emojis for top 3)
   - Bottom bar: side-by-side row — **"Edit Scores"** (left) pops back to ScorecardScreen; **"Confirm & Finish"** (right, red) → `AlertDialog` → `roundDao.completeRound()` → `wearSyncManager.clearRoundState()` → navigate to History
2. "End Round" on **watch** → navigates to `EndRoundPromptScreen`
   - Tells user to open phone; no finalize action available

## Cancelling a Round

The ⋮ overflow menu on `ScorecardScreen` has two options:

- **Add / Remove Players** — opens a `ModalBottomSheet` to add new players or remove existing ones mid-round. Removing a player also deletes their scores for the round. The last remaining player cannot be removed.
- **Cancel Round** — shows an `AlertDialog` confirming the action. On confirm: `roundDao.deleteRound()` (CASCADE deletes all scores and round_players) → `wearSyncManager.clearRoundState()` → navigate to Home. The round is not saved to history.

## Honor System Player Ordering

On hole N, players are sorted by a cascading comparator:
- **Primary key:** score on hole N−1 (ascending — lowest = best = goes first)
- **Tiebreaker:** score on hole N−2, then N−3, cascading back to hole 1
- **Last resort:** DB registration order (`round_players.order`) via Kotlin's stable sort
- Players who have not scored a given hole are treated as `Int.MAX_VALUE` for that key (sorted last)
- Re-sort fires on every hole navigation and on every score/player DB change
- Hole 1 always uses the original round order

The sort input is always `basePlayers` (DB order); the cascading keys make the tiebreaker deterministic without needing to sort from the previously displayed list. Phone (`RoundViewModel.sortPlayersForHole`) and watch (`WearScorecardScreen`) use identical logic.

---

## Setup (on a new machine)

1. Install Android Studio: `sudo snap install android-studio --classic` (Ubuntu)
2. Open Android Studio → **Open** → select this folder
3. Let Gradle sync (first sync downloads ~500 MB)
4. Enable USB Debugging on Pixel 8 Pro (Settings → About → tap Build Number 7×, then Developer Options → USB Debugging)
5. Run `:app` on the phone
6. Run `:wear` on the watch (required during development; on physical devices via Play Store the wear APK auto-installs because it is embedded via `wearApp(project(":wear"))`)

---

## Things to Know

- **Active round detection:** `roundDao.getActiveRound()` returns any round where `completedAt IS NULL`. Only one active round is expected at a time.
- **Player reuse:** When starting a round, typing a name that already exists in the `players` table reuses that player (matched by name, case-sensitive). Same logic applies when adding a player mid-round.
- **Score default:** A score of 0 means "not yet entered" — displayed as `"—"` in the UI. The minimum recorded score is 1.
- **vs Par display:** Under par = green (primary color), even = neutral, over par = red (error color). Only the round total vs-par is shown on the phone scorecard; per-hole vs-par was intentionally removed.
- **Hole distances:** `distanceFeet` is nullable on `HoleEntity`. Values are stored in feet; the scorecard converts to meters for display ("xxx ft / xxx m"). Courses created via the editor have no distance; the distance line is only shown when the value is non-null.
- **holeScores in PlayerState:** The watch displays `player.holeScores[currentHole] ?: 0` for the score, not a pre-computed field. This lets the watch navigate holes independently without requesting a re-push from the phone. Both `RoundViewModel.doPushStateToWatch()` and `PhoneWearableListenerService.pushUpdatedState()` must populate this map when building `PlayerState`.
- **Sync delivery:** `WearSyncManager.pushRoundState()` uses only `DataClient.putDataItem` (persistent, reconnect-safe). `WearListenerService` handles `onDataChanged`. The watch-side `MessageClient` path was removed — `WearListenerService` has no `onMessageReceived`, so the send was dead code.
- **Watch polling fallback:** `WearViewModel.startPolling()` reads `DataClient` every 2 s while the watch is in the foreground (started in `onResume`, stopped in `onPause`). It calls `RoundStateHolder.update(null)` if no item is found, which navigates the watch to `NoRoundScreen` — this is the intended clear-state behavior.
- **Player list reactivity:** `RoundViewModel.init` uses `combine(scoreFlow, playerFlow)` so adding/removing players updates the UI immediately without needing a score change to trigger it.
- **No Gradle wrapper JAR** is included. Android Studio handles this automatically; for CLI use, run `gradle wrapper --gradle-version 8.9` once.

---

## Branch History

### Branch `main` — Bug fixes & polish (2026-06-08)

**Bug fixes:**
- Score of 0 ("not yet entered") now renders as `"—"` on the phone scorecard player card (`PlayerScoreCard`) — was incorrectly showing `"0"`
- Watch `commitAndAdvance()` now skips `onScoreChange` when `pendingScore == 0` — previously sent a zero throw to Room, corrupting parSoFar calculations
- Removed dead `MessageClient` send from `WearSyncManager.pushRoundState()` — `WearListenerService` has no `onMessageReceived`, so the send was never consumed; also removed `nodeClient` and `messageClient` fields

**Honor system fix:**
- `sortPlayersForHole` (phone) and the watch's `players` sort now use a **cascading comparator**: primary = prevHole score, tie → prevPrevHole score, cascading to hole 1. Previously, ties always fell back to DB registration order, causing the "wrong" player to appear first after any equal-score hole

**Watch swipe removed:**
- Removed both `detectHorizontalDragGestures` blocks from `WearScorecardScreen` — they conflicted with the Pixel Watch 2 system back gesture (right-edge swipe → watch face). Hole navigation is now exclusively via the hole-jump picker and the Next Hole ▶ button

**Incomplete hole indicators:**
- Watch hole-jump picker: holes with any missing player score show an amber dot (6 dp, `CircleShape`, `0xFFFFB300`) to the right of the hole number text
- Phone hole-jump dropdown: same indicator as a `trailingIcon` on each `DropdownMenuItem` (8 dp dot). `hasMissingScores` now derives from the same pre-computed `incompleteHoles` set instead of a separate scan

---

### Branch `Before-Major-Wear-App-UI-Score-Entry` (2026-06-07)

Major watch UX redesign. Created as a safety branch off `fedxps` before changes — revert here if needed.

**Watch changes:**
- Sequential score entry: one player at a time with Enter / Next Hole ▶ buttons
- Honor system applied locally on the watch (`holeScores[currentHole - 1]` sort) — no phone re-push needed
- Replaced `SwipeDismissableNavHost` → `NavHost` to fix right-swipe system dismiss conflict
- Added `androidx.navigation.compose` to `wear/build.gradle.kts`
- `pendingScore` keyed on player ID + hole (not player index) for correct reset on reorder

---

## Code Cleanup History

### Branch `Code-Cleanup-on-Fedxps` (2026-06-04)

Dead code audit and removal. All changes are deletions only — no behavior change.

**Removed:**
- `import android.content.Context` from `RoundViewModel.kt` — imported but never used as a type
- `WAKE_LOCK` permission from `app/AndroidManifest.xml` — no `PowerManager` or `WakeLock` usage anywhere in the app
- `deleteHolesForCourse(courseId: Long)` from `CourseDao.kt` — Room `@Query` method defined but never called
- `implementation(libs.androidx.compose.ui.tooling.preview)` from `app/build.gradle.kts` — no `@Preview` composable functions exist in the codebase
- `debugImplementation(libs.androidx.compose.ui.tooling)` from both `app/build.gradle.kts` and `wear/build.gradle.kts` — same reason
- `androidx-compose-ui-tooling` and `androidx-compose-ui-tooling-preview` library entries from `gradle/libs.versions.toml` — orphaned after above removals

**Already clean on this branch (not added here):**
- `WearSyncManager` MessageClient send path — the `Alpha` branch had a `messageClient.sendMessage()` call in `pushRoundState()` targeting `/round/state/push`, but `WearListenerService` on the watch only implements `onDataChanged()` (DataClient) and has no `onMessageReceived()`, making that path dead. This branch predates that addition. The working delivery path is `DataClient.putDataItem()` only.

**Remaining known cleanup candidates (deferred):**
- `PlayerEntity.createdAt` field — stored in DB but never read anywhere; requires a Room migration (3 → 4) to drop the column
- `formatVsPar()` / `vsParColor()` — duplicated identically in `ScorecardScreen.kt`, `RoundReviewScreen.kt`, and `RoundDetailScreen.kt`; candidate for consolidation into a shared util
