# Scorigami — Disc Golf Scoring App

A native Android + Wear OS disc golf scoring app for tracking rounds in real time across your phone and watch.

## Target Devices

| Device | Model |
|---|---|
| Phone | Google Pixel 8 Pro |
| Watch | Google Pixel Watch 2 (Wear OS 4) |

The app also runs on other Android devices (min Android 11 / API 30). A Pixel 4a or similar works fine for testing the phone app without a paired watch.

---

## Features

- Create and manage courses with per-hole par values and distances
- Add players before a round — previously used names are suggested automatically
- Add or remove players mid-round via the scorecard overflow menu
- Enter scores on the phone or the watch in real time, kept in sync via the Wearable Data Layer
- **Smart first-press scoring:** tapping `−` from 0 enters birdie (par − 1); tapping `+` enters par — no need to tap up from zero every hole
- Navigate between holes by swiping left/right on the phone, or via the scrollable hole-jump picker on both phone and watch
- Holes with missing scores are flagged with an amber dot in the hole-jump picker (phone and watch)
- Animated slide transition and hole-number spring bounce on the phone when changing holes
- View the full live scorecard mid-round via the table icon in the phone top bar
- Player order on each hole reflects the honor system (lowest score on the previous hole goes first; ties broken by the hole before that, cascading)
- Cancel an in-progress round without saving it to history
- Review the full scorecard before finalizing a round
- Browse round history with per-hole breakdowns and standings

---

## Project Structure

```
Scorigami/
├── shared/   # Room DB, entities, DAOs, phone↔watch sync contracts
├── app/      # Phone app — Jetpack Compose, MVVM, Hilt
└── wear/     # Wear OS app — Compose for Wear OS, stateless (driven by phone)
```

**Pattern:** MVVM with `StateFlow` / `collectAsStateWithLifecycle`. Hilt for dependency injection throughout.

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

Both courses include per-hole distances (meters and feet).

---

## Key Libraries

| Library | Purpose |
|---|---|
| Jetpack Compose BOM 2024.12.01 | Phone UI |
| Wear Compose 1.4.0 | Watch UI |
| Room 2.6.1 | Local database (phone only) |
| Hilt 2.51.1 | Dependency injection |
| play-services-wearable 18.2.0 | Phone ↔ Watch Data Layer |
| kotlinx.serialization 1.7.3 | JSON for sync messages |
| Navigation Compose 2.8.5 | Phone navigation |

Min SDK: 30 · Compile SDK: 35 · Kotlin: 2.0.21 · AGP: 8.7.0

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
