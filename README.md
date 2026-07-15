# ⛳ Scorigami — Disc Golf Scoring App

A disc golf scoring app built for your **Android phone** and **Pixel Watch**. Track your round in real time, right from your wrist or your pocket.

## Target Devices

| Device | Model |
|---|---|
| Phone | Google Pixel 8 Pro |
| Watch | Google Pixel Watch 2 (Wear OS 4) |

The app also runs on other Android devices (min Android 11 / API 30). A Pixel 4a or similar works fine for testing the phone app without a paired watch.

---

## Features

- Create and manage courses with per-hole par values, distances, and rules/notes — courses can have any number of holes (add/remove hole lines in the editor)
- **Share courses between users** as `.sgcourse` files — export from the My Courses share icon, import by opening the file from email, Drive, or a file manager (names are de-duplicated automatically)
- Add players before a round — previously used names are suggested automatically (long-unused names can be removed from suggestions); animated shuffle button randomises the tee order
- Add or remove players mid-round via the scorecard overflow menu
- Enter scores on the phone or the watch in real time, kept in sync via the Wearable Data Layer
- **Smart first-press scoring:** tapping `−` from 0 enters birdie (par − 1); tapping `+` enters par — no need to tap up from zero every hole
- **OB and C1x stat tracking:** per-hole out-of-bounds (red) and missed circle-1-putt (orange) counters on both phone and watch — tap to cycle 1 → 2 → 3+, long-press to step back; round totals and per-hole color-coded indicators appear on every scorecard view
- Navigate between holes via the ◀/▶ arrows, or tap the big hole number to open the hole-jump grid picker (phone and watch)
- Holes with missing scores are flagged with an amber dot in the hole-jump grid (phone and watch)
- Per-hole rules and notes — tap the info icon on the hole card to see OB lines, mandos, or any rule the course editor stored for that hole
- Hide player scores mid-round with the eye toggle (shows `•••`) — on the phone hole card and the watch tee-order view
- Animated slide transition and hole-number spring bounce on the phone when changing holes
- View the full live scorecard mid-round via the table icon in the phone top bar
- Player order on each hole reflects the honor system (lowest score on the previous hole goes first; ties broken by the hole before that, cascading)
- Cancel an in-progress round without saving it to history
- Review the full scorecard before finalizing a round
- Browse round history with per-hole breakdowns and standings
- **Share a finished round as a PNG** — branded scorecard image with per-player hole grids and OB/C1x stats, from the round-detail share icon

---

## Project Structure

```
Scorigami/
├── shared/   # Room DB, entities, DAOs, phone↔watch sync contracts
├── app/      # Phone app — Jetpack Compose, MVVM, Hilt
└── wear/     # Wear OS app — Compose for Wear OS, stateless (driven by phone)
```

**Pattern:** MVVM with `StateFlow` / `collectAsStateWithLifecycle`. Hilt for dependency injection throughout.

The phone scorecard UI is split into focused composables under `app/ui/round/` — `ScorecardScreen` orchestrates state and layout while `PlayerScoreCard`, `HoleInfoCard`, `HoleJumpGrid`, `FullScorecardSheet`, `AddRemovePlayersSheet`, and `ScorecardTopBar` each own a piece of the screen.

**Storage:** Room database lives on the phone only. The watch has no local DB — it receives state snapshots pushed from the phone.

**Phone ↔ Watch sync:**
- Phone → Watch: full `RoundState` pushed on every score change or hole navigation via `DataClient.putDataItem` (persistent, survives reconnect). The watch also polls `DataClient` every 2 s while foregrounded as a fallback
- Watch → Phone: lightweight `ScoreUpdateMessage` sent when the user taps −/+
- `RoundState` carries per-hole scores for every player (not just the current hole), so the watch always shows the correct score regardless of which hole it is viewing independently

---

## Pre-Seeded Courses

Inserted on first launch:

| Course | Holes | Par | Notes |
|---|---|---|---|
| Los Colomos | 18 | 56 | H2 and H13 are Par 4; all others Par 3 |
| El Centinela | 18 | 54 | All holes Par 3 |

Both courses include per-hole distances (meters and feet) and example hole rules/notes (OB lines, mandatory routes, etc.) to demonstrate the feature.

Creating your own course is quick — it takes about **2 minutes** to get a course set up and ready to play, and you can share it with other Scorigami users as a `.sgcourse` file.

---

## Key Libraries

| Library | Purpose |
|---|---|
| Jetpack Compose BOM 2024.12.01 | Phone UI |
| Wear Compose 1.4.0 | Watch UI |
| Room 2.8.4 | Local database (phone only) |
| Hilt 2.59.2 | Dependency injection |
| play-services-wearable 18.2.0 | Phone ↔ Watch Data Layer |
| kotlinx.serialization 1.7.3 | JSON for sync messages |
| Navigation Compose 2.8.5 | Phone navigation |
| Wear Compose Navigation 1.4.0 | Watch navigation |

Min SDK: 30 · Compile SDK: 35 · Kotlin: 2.2.10 · AGP: 9.2.1

---

## Setup

### Requirements

- Android Studio (install via AUR on Arch Linux: `yay -S android-studio`)
- A physical Android device with USB Debugging enabled, or an Android emulator

### Running the app

1. Open Android Studio → **Open** → select this folder
2. Let Gradle sync complete (first sync downloads ~500 MB)
3. Enable USB Debugging on your phone:
   - **Settings → About phone** → tap **Build number** 7 times
   - **Settings → System → Developer options** → enable **USB debugging**
4. Plug in your phone, approve the USB debugging prompt on the device
5. Select your device in the Android Studio toolbar and click **Run**

The `:wear` APK is embedded in the `:app` build and installs to the paired watch automatically.

---

## Version History

| Version | Notes |
|---|---|
| Unreleased | Shuffle button animates through 4 passes (~2 s) so the reorder reads as a visible randomization. Scorecard hole number painted with the top-bar blue gradient plus a white outline. C1x metric changed from red to orange (OB stays red) across phone, watch, and all scorecard views; per-hole red/orange underline indicators added beneath the throw count wherever scorecards are shown, including the shared PNG. (A light/dark theme toggle is parked on the `Light-Dark-Theme-Toggle` branch.) |
| 0.6.3 | Major bug fix: navigation callbacks are now guarded with `dropUnlessResumed` — an unguarded quick double-tap on a back arrow fired `popBackStack()` twice, popped the start destination, and left a blank navy screen. Round Setup course selection changed from a dropdown to a `ModalBottomSheet` for consistency with the app's other pickers. |
| 0.6.2 | UI updates on OB/C1x metrics. Shared round PNG shows total throws per hole instead of the over/under par score, matching the in-app scorecards. |
| 0.6.1 | UI color changes. Removed " - " separator in OB and C1x metric labels. |
| 0.6.0 | **OB / C1x stat tracking:** out-of-bounds and missed circle-1-putt counter buttons on the player card (phone) and score entry screen (watch), synced both ways; round totals shown in Review, Full-scorecard sheet, and History detail. Two new Room tables (`ob_counts`, `c1x_counts`, DB migrations 5→7). |
| 0.5.9 | Watch tee-order view rebuilt as an inline Scaffold branch instead of a `Dialog` — big performance gain (no second platform window or entrance animation). |
| 0.5.8 | Round sharing is now a branded PNG scorecard image (gradient header, logo, per-player hole grids) instead of plain text. |
| 0.5.7 | Player archiving: remove a name from the "Previous Golfers" suggestions with a confirm dialog; re-adding the name restores it. |
| 0.5.6 | **Course sharing:** export any course as a `.sgcourse` file via the share sheet, import by opening the file (names de-duplicated, malformed files validated). Course editor gained add/remove hole lines — courses are no longer fixed at 18 holes. |
| 0.5.5 | Cold-start fix on the watch: blank loading route until the Data Layer resolves, so `NoRoundScreen` no longer flashes when a round is active. |
| 0.5.4 | Score visibility (eye) toggle added to the watch tee-order view, mirroring the phone. |
| 0.5.3 | Audit fixes: foreign-key enforcement enabled (course edits no longer duplicate hole rows), decrement-to-zero deletes the score row instead of storing 0, seeding moved to DB creation. Screen orientation locked to portrait. |
| 0.5.2 | UI scorecard changes on the phone app; minor watch UI changes. |
| 0.5.1 | Hole jump moved from a bottom-bar icon to tapping the hole number itself; "Hole X" typography enlarged for visibility. |
| 0.5.0 | Internal refactor: the phone scorecard screen was split from one ~420-line file into focused composables (`PlayerScoreCard`, `HoleInfoCard`, `HoleJumpGrid`, `FullScorecardSheet`, `AddRemovePlayersSheet`, `ScorecardTopBar`) plus a shared `ScoreFormat` helper. No behavior change. |
| 0.4.9 | UI color cleanup on the phone app. Added a "hide score" (eye) icon on the hole card to toggle player round scores between visible and `•••`. |
| 0.4.8 | Gradient home screen buttons (each button has its own color scheme). Matching gradient top bars on New Round, Scorecard, My Courses, and Round History screens. Player full name shown on scorecard cards (was 4-letter abbreviation). Player cards stretch full screen width. Shuffle player order button on round setup. |
| 0.4.7 | Per-hole rules and notes for courses. OB lines, mandos, and other notes stored per hole in the course editor; info icon on the hole card opens a rules sheet. El Centinela and Los Colomos seeded with example hole rules. |
| 0.4.6 | Add/remove players icon in scorecard overflow menu. Various phone UI changes: color updates, font weight, button spacing. |
| 0.4.5 | Color system centralized into `AppColors.kt` for phone and watch — no more inline color literals in screen files. |
| 0.4.4 | New app logo: red S on black background with white circle ring. Watch: end-of-round dialog when pressing Next Hole ▶ on hole 18. Watch: enlarged score controls (48 dp), spread to screen edges, dark-grey colour. Watch & phone: hole-jump picker replaced with scrollable 3-column grid (eliminates scroll jank on Pixel Watch 2). Phone: hole-jump grid opens as a dialog in the lower screen half, dismissable by tapping outside. |
| 0.4.3 | Bug fixes: score 0 renders as "—", watch can no longer commit a zero score, dead `MessageClient` send removed. Watch swipe-to-change-hole removed (conflicted with Pixel Watch 2 system back gesture). Cascading honor-system sort fixes tie-breaking. Amber incomplete-hole dot added to hole-jump picker on both phone and watch. |
| 0.4.2 | Watch sequential score entry (one player at a time, Enter → Next Hole ▶). Honor system sort applied locally on watch. Fixed swipe-to-dismiss conflict by replacing `SwipeDismissableNavHost` with `NavHost`. Branch: `Before-Major-Wear-App-UI-Score-Entry`. |
| 0.4.1 | Smart first-press scoring (− = birdie, + = par). Watch swipe navigation, compact watch layout (no course name/arrows, tighter cards). Phone: live scorecard sheet, spring-bounce hole transition, scrollable hole-jump with indicators, side-by-side finalize buttons. `holePars` added to RoundState sync. |
| 0.3.6 | Sync reliability fix (per-hole scores in RoundState, dual delivery, watch polling fallback). Phone scorecard redesign: cursive course name, 3-letter player abbreviation, −/+ score entry, animated hole transitions, hole-jump dropdown. Watch scorecard matching redesign. |
| 0.3.5 | Fix distances, player priority after previous score |
| 0.3.4 | Reorder based on previous score, change player list in new round screen |
| 0.3.3 | Add ability to cancel round, add/remove players, swipe ability |
| 0.3.2 | Add distances to scorecard, fix names to default courses |
| 0.2 | Tomorrow Night Blue theme, larger fonts, player history suggestions, version display, ordinal dates in round detail |
| 0.1 | Initial release — full scoring, history, course editor, Wear OS sync |
