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

## Color System

Named color constants are centralized in `AppColors.kt` files — no inline `Color(0xFF…)` literals in screen files.

**`app/ui/theme/AppColors.kt`** (phone):
| Constant | Value | Usage |
|---|---|---|
| `CardBackground` | `#37474F` | Hole-jump grid non-selected cell background |
| `ScaleGrey1` | `#354045` | `HoleInfoCard` card background |
| `ScaleGrey2` | `#5A6164` | `PlayerScoreCard` card background |
| `ContentWhite` | `Color.White` | Primary text/icon color on dark surfaces — text, icons, top bar chrome |
| `ScreenBackground` | `Color.Black` | App background — list `LazyColumn`, `ListItem` containers |
| `HoleNumberColor` | `#FFD60A` | Yellow hole number on scorecard and hole-jump grid |
| `HoleJumpSelectedColor` | `#7A7A7A` | Selected hole cell highlight in hole-jump grid (phone and watch) |
| `IncompleteHoleDotColor` | `#FFB300` | Amber dot on holes with missing scores |
| `ScoreUnderParColor` | `#81C784` | Green — under par score display |
| `NewRoundGradientStart` | `#1C2E42` | Deep grey-blue — left edge of New Round / Scorecard / Round Setup top bar and home button gradient |
| `NewRoundGradientEnd` | `#474B50` | Slate grey — right edge of New Round / Scorecard / Round Setup top bar and home button gradient |
| `CoursesGradientStart` | `#24534B` | Dark jungle green — left edge of My Courses top bar and home button gradient |
| `CoursesGradientEnd` | `#506B67` | Muted green — right edge of My Courses top bar and home button gradient |
| `HistoryGradientStart` | `#2D0C00` | Espresso brown — left edge of Round History top bar and home button gradient |
| `HistoryGradientEnd` | `#CC6B0A` | Warm amber — right edge of Round History top bar and home button gradient |
| `ResumeGradientStart` | `#4527A0` | Deep violet — left edge of Resume Round home button gradient |
| `ResumeGradientEnd` | `#7E57C2` | Soft lavender — right edge of Resume Round home button gradient |
| `DisabledButtonGradientStart` | `#3A3A3A` | Dark grey — left edge of a disabled `HomeActionButton` |
| `DisabledButtonGradientEnd` | `#5A5A5A` | Mid grey — right edge of a disabled `HomeActionButton` |

**`wear/ui/theme/AppColors.kt`** (watch):
| Constant | Value | Usage |
|---|---|---|
| `HoleNumberColor` | `#FFD60A` | Yellow hole number on the scorecard |
| `HoleJumpSelectedColor` | `#7A7A7A` | Selected hole cell highlight in hole-jump grid (matches phone) |
| `WearButtonBackground` | `#2A2A2A` | Dark grey for −/+ buttons, Enter/Next Hole chip, non-current hole cells |
| `IncompleteHoleDotColor` | `#FFB300` | Amber dot on holes with missing scores |
| `ScoreUnderParColor` | `#81C784` | Green — under par score display |
| `ContentWhite` | `Color.White` | Primary text/icon color on dark surfaces (mirrors phone) |

At-par and unscored use `ContentWhite`. Over-par uses `MaterialTheme.colorScheme.error` (phone) / `MaterialTheme.colors.error` (wear).

---

## Key Libraries

| Library | Version | Purpose |
|---|---|---|
| Jetpack Compose BOM | 2024.12.01 | Phone UI |
| Wear Compose | 1.4.0 | Watch UI |
| Room | 2.8.4 | Local DB (phone only) |
| Hilt | 2.59.2 | Dependency injection |
| play-services-wearable | 18.2.0 | Phone ↔ Watch Data Layer |
| kotlinx.serialization | 1.7.3 | JSON for sync messages |
| Navigation Compose | 2.8.5 | Phone navigation |
| Wear Compose Navigation | 1.4.0 | Watch navigation |

Min SDK: 30 · Compile SDK: 35 · Kotlin: 2.2.10 · AGP: 9.2.1

---

## Data Model (shared module)

```
courses     id, name, holeCount
holes       id, courseId, number, par, distanceFeet?, notes?   (FK → courses CASCADE)
players     id, name, createdAt, isArchived
rounds      id, courseId, startedAt, completedAt?              (completedAt=null = active)
round_players  roundId, playerId, order                        (composite PK)
scores      roundId, playerId, holeNumber, throws              (composite PK, upsert on change)
```

**DB version: 5**
- Migration 1→2: adds `distanceMeters INTEGER` nullable column to `holes`
- Migration 2→3: renames `distanceMeters` → `distanceFeet` (values were always stored in feet)
- Migration 3→4: adds `notes TEXT` nullable column to `holes`
- Migration 4→5: adds `isArchived INTEGER NOT NULL DEFAULT 0` column to `players` — archived players are hidden from the "Previous Golfers" suggestions (`PlayerDao.getAllPlayers()` filters `isArchived = 0`); archiving happens from Round Setup, and adding an archived name to a round auto-unarchives it (`RoundViewModel`)

**Foreign key enforcement:** `DatabaseModule.provideDatabase()` enables FK enforcement via a `RoomDatabase.Callback` whose `onOpen(connection)` runs `connection.execSQL("PRAGMA foreign_keys = ON")`. Room does NOT enable `PRAGMA foreign_keys` by default, and Room 2.8 **removed** the old `Builder.setForeignKeyConstraintsEnabled()` method (the KMP rewrite — the `Callback` now receives an `androidx.sqlite.SQLiteConnection`, and `execSQL` is the `androidx.sqlite.execSQL` extension). The PRAGMA runs on every connection open, outside a transaction, so it takes effect. This is required for the declared `onDelete = CASCADE` constraints to fire: deleting a course cascades to its `holes`; deleting a round cascades to its `scores` and `round_players`; and editing a course (`insertCourse` with `OnConflictStrategy.REPLACE` on an existing id) cascade-deletes the old holes before `insertHoles` re-adds them — without this, course edits silently duplicated every hole row.

**Sync types** (`shared/sync/`):
- `RoundState` — full snapshot pushed phone→watch (roundId, courseName, currentHole, totalHoles, players[], **holePars: Map<Int,Int>**). `holePars` maps every hole number to its par value so the watch can apply the first-press scoring logic for any hole it navigates to independently.
- `PlayerState` — per-player data inside RoundState (playerId, name, **holeScores: Map<Int,Int>**, totalThrows, totalVsPar). `holeScores` maps every hole number the player has scored to their throw count, allowing the watch to display the correct score for whichever hole it is viewing independently of the phone.
- `ScoreUpdateMessage` — watch→phone message (roundId, playerId, holeNumber, throws, viewingHole)

---

## Pre-Seeded Courses

Inserted once when the DB file is first created, via `DatabaseSeeder.seedIfEmpty()` called from the Room `Callback.onCreate` in `DatabaseModule` (launched on a bounded, self-cancelling coroutine scope). Note: because seeding is tied to DB creation rather than "any time the courses table is empty," deleting all courses no longer re-seeds them on the next launch:

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
- **Top bar:** blue gradient (`NewRoundGradientStart` → `NewRoundGradientEnd`) wrapping a transparent `TopAppBar`; course name in `FontFamily.Cursive`; `TableChart` icon button (opens full scorecard sheet); "End Round" button; ⋮ overflow menu
- **Full scorecard sheet:** `ModalBottomSheet` with `skipPartiallyExpanded = true` (opens full height) — per-player 18-hole breakdown with hole numbers (`labelMedium`) and vs-par scores (`bodyMedium`, bold, colored)
- **Hole card** (`HoleInfoCard`): `ScaleGrey1` background; `Box` overlay hosts four corner icons — `Info` at `TopStart` (notes, only when `hole.notes` non-null), `Group` at `TopEnd` (add/remove players), `Visibility`/`VisibilityOff` at `BottomStart` (score hide toggle); ◀/▶ arrow buttons; hole label is stacked — small "Hole" (`titleMedium`, 20sp, `FontWeight.Normal`) above a large bold tappable hole number (`displayMedium`, 124sp, `FontWeight.ExtraBold`), both yellow (`HoleNumberColor`); the spring-bounce scale animates the number; tapping the number opens the hole-jump grid dialog (subtle ripple via `clip` + `clickable` on a `RoundedCornerShape(12.dp)` `Box`)
- **Player cards:** left `Column` (`weight(1f)`) stacks player name (28sp, `FontWeight.Bold`, `ContentWhite`) above round vs-par score (`titleSmall`, `ContentWhite`, hidden as `"•••"` when scores hidden); plain `IconButton` −/+ controls on the right; cards are full screen width (no horizontal padding on the `LazyColumn`)
- **Hole transitions:** `AnimatedContent` slides player cards left/right matching navigation direction (250 ms); hole number springs from 82 % → 100 % with `Spring.DampingRatioMediumBouncy` on each navigation (`Animatable` + `LaunchedEffect`)
- **Hole jump dialog** (inlined in `HoleInfoCard`): opened by tapping the hole number; `Dialog` (`usePlatformDefaultWidth = false`) positioned in the lower screen half; `Surface` with `RoundedCornerShape(16.dp)` and `tonalElevation = 8.dp` contains the grid; 3-column grid of `Box` cells (`60 dp` tall, `RoundedCornerShape(8.dp)`); current hole `HoleJumpSelectedColor`, others `CardBackground`; amber `IncompleteHoleDotColor` dot (6 dp, `CircleShape`) top-right on cells with missing scores; tap outside to dismiss. `HoleJumpGrid.kt` (the old standalone composable with its own `OutlinedButton` trigger) is **kept in the codebase as a revert fallback** but is no longer used in any screen.

### CourseListScreen layout
- **Top bar:** green gradient (`CoursesGradientStart` → `CoursesGradientEnd`) wrapping a transparent `TopAppBar`; title and nav icon use `ContentWhite`
- **List items:** `ListItem` with `containerColor = ScreenBackground`; `HorizontalDivider` between entries; `LazyColumn` background is `ScreenBackground`

### HistoryScreen layout
- **Top bar:** amber/brown gradient (`HistoryGradientStart` → `HistoryGradientEnd`) wrapping a transparent `TopAppBar`; title and nav icon use `ContentWhite`
- **List items:** `ListItem` with `containerColor = ScreenBackground`; `HorizontalDivider` between entries; `LazyColumn` background is `ScreenBackground`

### RoundSetupScreen layout
- **Top bar:** blue gradient (`NewRoundGradientStart` → `NewRoundGradientEnd`) matching ScorecardScreen; title and nav icon use `ContentWhite`
- **Players section order:** (1) "Players" heading + shuffle `IconButton` (enabled when `players.size > 1`); (2) current players list with × remove buttons + `HorizontalDivider`s; (3) "Previous Golfers" `SuggestionChip`s (label text `ContentWhite`, only shown when un-added players exist); (4) "Add Player" `OutlinedTextField` + add `IconButton`
- **Start Round button:** in `Scaffold` `bottomBar` — full-width, 56 dp height, 16 dp horizontal padding, matching `RoundReviewScreen` bottom bar style

### CourseEditorScreen layout
- **Top bar:** green gradient (`CoursesGradientStart` → `CoursesGradientEnd`) matching `CourseListScreen`; title and nav icon use `ContentWhite`
- **Per-hole rows:** par −/+ stepper (2–6) with a remove-hole × (disabled when only 1 hole remains); "Add Hole" `OutlinedButton` appends a Par 3; each hole also has a single-line **"Distance ft (optional)"** field (number keyboard, digits-only filter, max 5 chars, blank = null) and a multiline "Hole rules / notes (optional)" field. On save, all three lists (par / distance / notes) are rebuilt into fresh `HoleEntity` rows — the FK cascade removes the old ones

### RoundDetailScreen layout
- **Top bar:** amber/brown gradient (`HistoryGradientStart` → `HistoryGradientEnd`) matching `HistoryScreen`; course name as title, "Played on …" subtitle at 75 % alpha `ContentWhite`; nav icon uses `ContentWhite`; **Share icon** (`Icons.Default.Share`) in `actions` — fires `Intent.ACTION_SEND` with a plain-text scorecard (course name, date, par, per-player totals + per-hole vs-par grid split in rows of 9); disabled until data loads
- **Hole grid:** hole numbers `labelLarge`, vs-par scores `bodyMedium` + `FontWeight.Bold` (colored by par relationship)

### HomeScreen layout
- **Buttons:** `HomeActionButton` composable — `Brush.horizontalGradient` applied via `Modifier.background(brush, RoundedCornerShape(percent = 50))`; `containerColor = Color.Transparent` so gradient shows through; `contentColor = ContentWhite`; disabled state falls back to a dark-grey gradient; all buttons full-width 56 dp height
- **Themes:** `app/res/values/themes.xml` sets `windowBackground`, `statusBarColor`, `navigationBarColor` to `#002451` (dark navy) for pre-API 31 devices. `app/res/values-v31/themes.xml` inherits the same three and additionally sets `windowSplashScreenBackground="#FF000000"` (black) — forces Android 12+ system splash to use black instead of the navy windowBackground

---

## Watch App Screens

| Screen | Route | Notes |
|---|---|---|
| (Loading) | `loading` | Blank background shown on cold start until the Data Layer is read once (`uiState.loaded`) — prevents a `NoRoundScreen` flash before the active round resolves |
| NoRoundScreen | `no_round` | Shown when no active round |
| WearScorecardScreen | `scorecard` | Current hole, −/+ per player, hole nav |
| EndRoundPromptScreen | `end_round_prompt` | Tells user to finalize on phone |

Navigation is in `wear/navigation/WearNavigation.kt`. State comes from `RoundStateHolder.state` (a singleton `StateFlow` updated by `WearListenerService` and by `WearViewModel`'s Data Layer reads).

**Cold-start flash fix:** `WearUiState.loaded` starts `false` and flips `true` after `WearViewModel.refreshFromDataLayer()` reads `DataClient` once (kicked off in `init`, before `onResume` polling). The NavHost's `startDestination` is the blank `loading` route while `!loaded`; a `LaunchedEffect(loaded, roundState != null)` then routes to `scorecard`/`no_round` once the state is known. This stops `NoRoundScreen` from rendering for a frame on cold start when a round is actually active.

### WearScorecardScreen layout — Sequential score entry (branch: `Before-Major-Wear-App-UI-Score-Entry`)

The watch scorecard uses a **one-player-at-a-time** flow instead of showing all players simultaneously:

1. Screen shows the current player (honor-sorted) with hole number, their name, −/+ controls, and an **Enter** button
2. Tapping **Enter** commits the score to the phone and advances to the next player; if `pendingScore` is 0 the advance happens without writing a score
3. The last player's button reads **"Next Hole ▶"** — tapping it commits their score (if > 0) and navigates to the next hole automatically

**Layout per player:**
- Hole number (42sp, `FontWeight.ExtraBold`, `HoleNumberColor`) at top — tappable to open the hole-jump picker
- Player's full name in white `title1` (SemiBold), centered — tappable to open the tee-order popup
- −/+ `CompactButton` (48 dp, `#2A2A2A` dark-grey fill, 22sp) spread to screen edges via `Arrangement.SpaceBetween` on a `fillMaxWidth` row; score centered between them
- Enter / Next Hole ▶ `Chip` centered below (36 dp height, `#2A2A2A` fill)
- Tapping **Next Hole ▶ on the final hole** (instead of navigating) shows a centered `Dialog` with the message "End round on the phone app" — score is still committed first if `pendingScore > 0`

**Hole-jump picker (`WearHoleJumpGrid`):** Static 3-column grid rendered as a `Column`/`Row` layout with `verticalScroll(rememberScrollState())`. Each hole is a `Box` (44 dp tall, `RoundedCornerShape(8.dp)`): current hole `HoleJumpSelectedColor` (`#7A7A7A`, matches phone), others `WearButtonBackground` (`#2A2A2A`); all text white. Amber dot (`0xFFFFB300`, 5 dp) in the top-right corner of cells with any missing score. No `ScalingLazyColumn` or fling physics — eliminates scroll jank on physical hardware. Tapping a cell jumps to that hole.

**Tee-order popup (`TeeOrderDialog`):** All players listed in uniform white — no current-player highlight.

**End-of-round dialog (`EndRoundPromptDialog`):** "End round on the phone app" message in a dismissable `Card`.

**Honor system on watch:** Players are sorted locally using a cascading comparator — primary key is `holeScores[currentHole - 1]`, ties broken by `holeScores[currentHole - 2]`, continuing back to hole 1, then DB registration order. No phone re-push needed when moving to a new hole.

**`pendingScore` state:** keyed on `(currentPlayer.playerId, currentHole)` so it resets correctly per player per hole, even if the phone pushes a reordered `roundState.players` mid-entry.

**Navigation:** Uses standard `NavHost` (from `androidx.navigation.compose`) instead of `SwipeDismissableNavHost`. Horizontal swipe gestures are **not** used on the watch — they conflict with the Pixel Watch 2 system back gesture (right-edge swipe → watch face). Hole navigation is via the hole-jump picker and the Next Hole ▶ button.

**End Round chip** is present in code but commented out. To re-enable: uncomment the `Chip` block in `WearPlayerScoreEntry` and re-add the `onEndRound` callback param through `WearScorecardScreen` → `WearNavigation` (the unused params were removed in the 2026-07-02 review; the `EndRoundPrompt` route/screen still exist).

**WearScorecardScreen component split (2026-06-19):**
`ScorecardScreen.kt` decomposed from ~354 lines into focused `internal fun`s in `com.scorigami.wear.ui`. `ScorecardScreen.kt` is now ~97 lines of orchestration.

| File | Responsibility |
|---|---|
| `WearHoleJumpGrid.kt` | Scrollable 3-column hole picker grid |
| `WearPlayerScoreEntry.kt` | Hole number + player name + −/+ controls + Enter/Next Hole chip |
| `EndRoundPromptDialog.kt` | "End round on the phone app" dialog |
| `TeeOrderDialog.kt` | Tee order player list dialog |

---

## How Scoring Works

**First-press scoring:** When a hole score is 0 (not yet entered), the first button press jumps to a smart default rather than incrementing from 0:
- Tapping `−` → `maxOf(1, par − 1)` (birdie assumption — Par 3 enters 2)
- Tapping `+` → `par` (even par assumption — Par 3 enters 3)

Subsequent presses increment/decrement normally. Pressing `−` from any score > 0 decrements by 1 (reaching 0 clears the score back to "not entered"). Applies on both phone and watch. Par is looked up from `holes` (phone) or `roundState.holePars[currentHole]` (watch).

When a score is cleared to 0, `RoundViewModel.updateScore()` calls `scoreDao.deleteScore(roundId, playerId, holeNumber)` rather than upserting a `throws = 0` row. A stored 0 would otherwise be counted by `parSoFar` (which keys off map presence), falsely showing the player under par. The watch's `commitAndAdvance()` already guards this with `if (pendingScore > 0)`.

1. User taps −/+ on a player row (phone or watch)
2. Score is written to Room (`ScoreEntity` upsert)
3. `RoundViewModel` observes both `scoreDao.getScoresForRound()` and `playerDao.getPlayersForRoundFlow()` via `combine` inside `flatMapLatest` — picks up DB changes automatically
4. Players are re-sorted for the current hole (honor system — see below)
5. `pushStateToWatch()` is called (150 ms debounce) → `WearSyncManager.pushRoundState()` → `DataClient.putDataItem` only
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
- **Hole distances:** `distanceFeet` is nullable on `HoleEntity`. Values are stored in feet; the scorecard converts to meters for display ("xxx ft / xxx m"). The course editor exposes a per-hole "Distance ft (optional)" field (digits only, blank = null); the distance line is only shown when the value is non-null.
- **Hole notes:** `notes` is nullable on `HoleEntity` (added in migration 3→4). When non-null/non-blank, an `Info` icon appears on the hole card in `ScorecardScreen`; tapping it opens a `ModalBottomSheet` titled "Hole [number] Rules". The course editor exposes a multiline "Hole rules / notes (optional)" field per hole. Notes are not yet surfaced on the watch.
- **holeScores in PlayerState:** The watch displays `player.holeScores[currentHole] ?: 0` for the score, not a pre-computed field. This lets the watch navigate holes independently without requesting a re-push from the phone. This map (and the rest of `PlayerState`) is populated by `RoundStateBuilder.build(...)` in `shared/sync/`, called by both `RoundViewModel.doPushStateToWatch()` (in-memory state) and `PhoneWearableListenerService.pushUpdatedState()` (fresh DB queries) — so the per-player math lives in one place.
- **Sync delivery:** `WearSyncManager.pushRoundState()` uses only `DataClient.putDataItem` (persistent, reconnect-safe). `WearListenerService` handles `onDataChanged`. The watch-side `MessageClient` path was removed — `WearListenerService` has no `onMessageReceived`, so the send was dead code.
- **Watch polling fallback:** `WearViewModel.startPolling()` calls `refreshFromDataLayer()` every 2 s while the watch is in the foreground (started in `onResume`, stopped in `onPause`). `refreshFromDataLayer()` reads `DataClient` and calls `RoundStateHolder.update(round-or-null)`; a null (no item found) navigates the watch to `NoRoundScreen` — the intended clear-state behavior. The same helper runs once in `init` for the cold-start `loaded` flag (see BUG-7 fix).
- **Player list reactivity:** `RoundViewModel.init` uses `combine(scoreFlow, playerFlow)` so adding/removing players updates the UI immediately without needing a score change to trigger it.
- **Gradle wrapper is committed** (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`) — CLI builds work via `./gradlew`.

---

## Branch History

### Branch `cachyai` — UI polish, hole-jump redesign & hole notes (2026-06-10)

**Color system centralized:**
- `app/ui/theme/AppColors.kt` and `wear/ui/theme/AppColors.kt` introduced — all named color constants moved here; no more inline `Color(0xFF…)` literals in screen files

**Hole rules / notes feature (phone):**
- `HoleEntity` gains a nullable `notes: String?` field (migration 3→4)
- `CourseEditorScreen`: multiline "Hole rules / notes (optional)" text field added per hole; values trimmed and saved as null when blank
- `ScorecardScreen`: `Info` icon appears on the hole card when notes are present; tapping opens a `ModalBottomSheet` titled "Hole [number] Rules"
- `DatabaseSeeder`: Los Colomos and El Centinela seeded with example OB/mando notes to demonstrate the feature

**Logo refresh (both `app` and `wear`):**
- `ic_launcher_background.xml`: dark blue → pure black
- `ic_launcher_foreground.xml`: flying-disc graphic → white circle ring + red bold S (cubic-bezier stroke path, `#FFCC0000`)
- `ic_logo.xml`: same combined design (black background + ring + S) for standalone use on the phone home screen

**Watch UX improvements (`ScorecardScreen.kt`):**
- **End-of-round prompt:** tapping "Next Hole ▶" on the last hole now shows a centered `Dialog` ("End round on the phone app") instead of silently doing nothing; score is committed first if `pendingScore > 0`
- **Score controls:** − / + `CompactButton` enlarged from 36 dp → 48 dp; symbol font 18sp → 22sp; spread to screen edges (`Arrangement.SpaceBetween`, `fillMaxWidth`); color changed from primary blue → `#2A2A2A` dark grey; Enter / Next Hole ▶ `Chip` also changed to `#2A2A2A`
- **Hole-jump picker replaced:** `ScalingLazyColumn` removed; replaced with a static 3-column `Column`/`Row` grid + `verticalScroll`. Eliminates scroll jank on Pixel Watch 2 (no fling physics, no per-item scale transform). Current hole highlighted yellow; others `#2A2A2A`; amber dot on incomplete holes
- **Tee-order popup:** removed current-player blue/bold highlight — all players shown in uniform white

**Phone UX improvement (`ScorecardScreen.kt`):**
- **Hole-jump picker replaced:** `DropdownMenu` + scroll arrows removed; replaced with `HoleJumpGrid` composable that opens a `Dialog` (`usePlatformDefaultWidth = false`) positioned in the lower screen half. Same 3-column grid (60 dp cells, `CardBackground` / `HoleNumberColor`). Tap outside to dismiss (backdrop `clickable` + Surface no-op `clickable` to consume inner touches)

---

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

## Code Audit Report (2026-06-21, branch `fedxps`)

Full multi-module audit. The four highest-priority bugs are **fixed and verified** (UI + DB inspection via adb); the rest are tracked below as open follow-ups.

**Fixed & verified:**
- **BUG-1 — Course edits duplicated all hole rows.** Editing a course (`CourseViewModel.saveCourse` → `insertCourse` with `OnConflictStrategy.REPLACE` + `insertHoles`) re-inserted holes with `id = 0` without removing the old ones, so a course grew 18 → 36 → 54 rows per edit. Fixed by enabling FK enforcement (BUG-2), so the REPLACE cascade-deletes the old holes first. Verified: both seeded courses show exactly 18 holes.
- **BUG-2 — FK constraints were inert.** Room never enabled `PRAGMA foreign_keys`, so every `onDelete = CASCADE` was a no-op. Fixed in `DatabaseModule` via a `RoomDatabase.Callback.onOpen { execSQL("PRAGMA foreign_keys = ON") }` (Room 2.8 removed `Builder.setForeignKeyConstraintsEnabled()`). Verified: cancelling a round dropped `scores` 2→0 and `round_players` 2→0 even though `RoundDao.deleteRound` only runs `DELETE FROM rounds` — proof the cascade fires.
- **BUG-3 — Pressing `−` from score 1 stored `throws = 0`.** A stored 0 was counted by `parSoFar` (which keys off map presence), falsely showing the player under par. `RoundViewModel.updateScore` now calls the new `ScoreDao.deleteScore(roundId, playerId, holeNumber)` when `throws <= 0` instead of upserting. Verified: zero `throws = 0` rows in the DB after decrementing to clear.
- **BUG-4 — Unscored holes rendered `"0"` instead of `"—"`** in `PlayerScoreCard`. Now shows `"—"`, matching `FullScorecardSheet` / `RoundReviewScreen`. Verified in UI.
- **Bonus — Course editor opened blank when editing.** `CourseEditorScreen`'s init `LaunchedEffect` treated the transient `existing == null` (DB load not finished) as "new course" and set `initialized = true` before real data arrived, so fields never populated. Fixed by adding `CourseViewModel.isEditing` and gating: new course → init blank immediately; editing → wait for `existing != null` before populating.

**Open follow-ups (not yet addressed), by priority:**
- BUG-5: deleting a course with an **active** round silently clears `RoundUiState` (the round vanishes from the UI with no cancellation/cleanup). `CourseListScreen` / `CourseViewModel`.
- ~~BUG-6~~: **Rejected (false positive).** The audit wrongly claimed the wear `applicationId` (`com.scorigami.app`, same as phone) should differ. In fact the Wearable Data Layer **requires** the phone and wear apps to share the same `applicationId` and signing key — that shared id is why sync works. Play Store delivery also uses the same `applicationId`, distinguished by the wear manifest's `<uses-feature android:name="android.hardware.type.watch">`. The audit conflated `applicationId` with `namespace` (wear already uses `namespace = "com.scorigami.wear"` for its code/R-class package, which is correct). No change made.
- ~~BUG-7~~: **Done.** Added `WearUiState.loaded` + a blank `loading` start destination; `WearViewModel.refreshFromDataLayer()` reads the Data Layer once in `init` and flips `loaded`, then a `LaunchedEffect` routes to `scorecard`/`no_round`. `NoRoundScreen` no longer flashes on cold start. Also de-duplicated the poll loop to reuse `refreshFromDataLayer()`.
- ~~ARCH-1~~: **Done.** Extracted `RoundStateBuilder.build(...)` in `shared/sync/` — both `RoundViewModel.doPushStateToWatch` and `PhoneWearableListenerService.pushUpdatedState` now call it. `PlayerState` math lives in one place.
- ~~ARCH-2~~: **Done.** `SyncKeys.ROUND_STATE_MSG` deleted.
- ~~ARCH-3~~: **Done.** Seeding moved into Room `Callback.onCreate` (fires once on DB creation) on a bounded, self-cancelling scope, via an injected `Provider<CourseDao>`. Replaces the inline never-cancelled scope that launched on every app start. Behavior change: deleting all courses no longer re-seeds them on next launch.
- ~~ARCH-4~~: **Done.** `HistoryViewModel.detail` converted from a `flow{}` (only scores reactive) to `combine(scoresFlow, playersFlow)`; round + course fetched once inside the suspend transform. Players are now reactive too. (`toSummary`'s one-shot queries are the separate N+1 QUALITY item, left as-is.)
- ~~DEAD~~: **Done.** Removed unused `@ApplicationContext` import in `RoundViewModel`; `wear.compose.navigation` dep (+ orphaned toml lib entry); root `kotlin.android` plugin alias (+ orphaned toml plugin entry); all four `ic_*_vector.xml` drawables.
- QUALITY: **done.** ✅ par editor now allows Par 2 (`CourseEditorScreen`, min `par > 2`); ✅ `HomeScreen` disabled-button gradient moved to `DisabledButtonGradientStart/End` in `AppColors.kt`; ✅ wear `ContentWhite` alias added and the scattered `Color.White` replaced across the 4 wear UI files; ✅ `values-v31/themes.xml` added with `windowSplashScreenBackground = #FF000000`; ✅ CLAUDE.md color table reconciled with `AppColors.kt`; ✅ N+1 in the history list replaced — `HistoryViewModel.toSummary` removed in favor of a single `RoundDao.getCompletedRoundSummaryRows()` JOIN (one row per round+player, grouped in memory), cutting `M×(2+N)` queries to one. Dead `ScoreDao.getTotalThrowsForPlayer` removed.

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
- `PlayerEntity.createdAt` field — stored in DB but never read anywhere; requires a Room migration (5 → 6) to drop the column
- ~~`formatVsPar()` / `vsParColor()` in `RoundDetailScreen.kt`~~ — **Done (2026-07-02 review).** Private copies removed; now imports the `internal` helpers from `ui.round/ScoreFormat.kt`

---

### Branch `fedxps` — UI theming, scorecard improvements & color cleanup (2026-06-16)

**Gradient top bars:**
- `RoundSetupScreen`, `ScorecardScreen`: blue gradient (`NewRoundGradientStart` → `NewRoundGradientEnd`) top bar — transparent `TopAppBar` inside a `Box` with gradient `Modifier.background`
- `CourseListScreen`: green gradient (`CoursesGradientStart` → `CoursesGradientEnd`) top bar
- `HistoryScreen`: amber/brown gradient (`HistoryGradientStart` → `HistoryGradientEnd`) top bar

**Home screen gradient buttons:**
- `HomeActionButton` composable: `Brush.horizontalGradient` via `Modifier.background(brush, RoundedCornerShape(percent=50))`; `containerColor = Color.Transparent`; `contentColor = ContentWhite`; disabled grey fallback; 56 dp height full-width
- Gradient pairs: New Round navy→sky, My Courses jungle→green, History espresso→amber, Resume violet→lavender

**List screen list items:**
- `CourseListScreen` and `HistoryScreen`: `ListItem` containerColor and `LazyColumn` background use `ScreenBackground`; `HorizontalDivider` as separator

**ScorecardScreen player cards:**
- Removed 4-letter abbreviation — now shows full player name with `Modifier.weight(1f)`
- Removed horizontal padding from `LazyColumn` — cards now full screen width

**ScorecardScreen hole card:**
- "Hole X" uses `buildAnnotatedString` — "Hole " `FontWeight.Normal`, number `FontWeight.ExtraBold`
- Score visibility toggle: `Visibility`/`VisibilityOff` icon at `BottomStart`; "Round" label always visible; score shows `formatVsPar` or `"•••"` when hidden; state lives outside `AnimatedContent` so it persists across holes

**Full scorecard sheet:**
- `skipPartiallyExpanded = true` — opens to full screen height immediately
- Hole grid text bumped from `labelSmall`/`bodySmall` → `labelMedium`/`bodyMedium`

**RoundSetupScreen:**
- "Start Round" moved to `Scaffold` `bottomBar` — full-width, 56 dp height, matches `RoundReviewScreen` style
- Shuffle `IconButton` next to "Players" heading; enabled when `players.size > 1`

**Color system cleanup:**
- `ContentWhite = Color.White` added to `AppColors.kt` — replaces all `Color.White` across UI files; single change flips all white content
- `ScreenBackground = Color.Black` added to `AppColors.kt` — replaces all `Color.Black` across UI files; single change flips all dark backgrounds
- `Color.Transparent` left as-is — structural rendering technique, not a design color

**Splash screen:**
- `android:windowSplashScreenBackground="#FF000000"` added to `app/res/values/themes.xml` — fixes Android 12+ splash defaulting to white

**Hole card color:**
- Changed from `MaterialTheme.colorScheme.primaryContainer` to `ScaleGrey1` (`#354045`) via `HoleInfoCard`
- `CardGrey` (`#42413C`) was added to `AppColors.kt` during this work but was never used — removed in the 2026-07-02 review

**DatabaseSeeder notes update:**
- El Centinela H1–H2 seeded with OB notes; Los Colomos H2 note trimmed

**ScorecardScreen component split (v0.5.0, 2026-06-16):**
`ScorecardScreen.kt` was decomposed from a single ~420-line file into focused composables, all in the `com.scorigami.app.ui.round` package as `internal fun`s. `ScorecardScreen.kt` itself is now ~190 lines of orchestration (state, dialogs, layout) that composes the pieces. Pure refactor — no behavior change; state hoisted to the screen and passed down via callbacks.

| File | Responsibility |
|---|---|
| `PlayerScoreCard.kt` | One player's row — name + vs-par stacked in left column, −/+ score controls on right (no "Round" label) |
| `HoleInfoCard.kt` | Hole card: ◀/▶ nav, animated yellow hole number, par/distance, the three corner icons (Info / Group / Visibility) **and** its own hole-notes `ModalBottomSheet` (visibility state internal, keyed on `hole`) |
| `HoleJumpGrid.kt` | Hole-jump button + 3-column grid `Dialog` |
| `FullScorecardSheet.kt` | Per-player 18-hole breakdown shown in the table-icon `ModalBottomSheet` |
| `AddRemovePlayersSheet.kt` | Add/remove-players `ModalBottomSheet` body |
| `ScorecardTopBar.kt` | Gradient top bar — scorecard/end-round actions + ⋮ overflow menu (`menuExpanded` state internal) |
| `ScoreFormat.kt` | Shared `formatVsPar()` / `vsParColor()` helpers for the `ui.round` package |

- `formatVsPar()` / `vsParColor()` were previously duplicated as `private` copies in each screen file (private copies don't collide). Extracting `PlayerScoreCard` as `internal` exposed them package-wide and clashed with `RoundReviewScreen`'s copies → consolidated into `ScoreFormat.kt`; the copies in `ScorecardScreen.kt` and `RoundReviewScreen.kt` were removed

**Additional screen polish (2026-06-19):**
- `RoundDetailScreen`: gradient top bar (espresso→amber, matching `HistoryScreen`); hole/score font sizes increased (`labelLarge` / `bodyMedium + Bold`)
- `CourseEditorScreen`: gradient top bar (jungle→green, matching `CourseListScreen`)
- `RoundSetupScreen`: "Previous Golfers" chips repositioned to appear between current player list and "Add Player" field; chip label text set to `ContentWhite`
- `PlayerScoreCard`: removed separate center "Round" label column; vs-par score now stacked below player name in the left column

**Wear ScorecardScreen component split (2026-06-19):**
`ScorecardScreen.kt` decomposed from ~354 lines into focused `internal fun`s — see WearScorecardScreen layout section above for the component table.
