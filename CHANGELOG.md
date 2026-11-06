# Changelog

All notable GridBlast changes are summarized here.

## Unreleased (alpha)

- Make each board cell's full allocated square tappable, including the 2dp visual gutter on every side. The tile appearance and grid spacing are unchanged; an Android instrumentation regression now checks edge taps and adjoining hitboxes on all emulator profiles. Small-screen physical usability still needs human validation.

- Added a dedicated Android 8/16 emulator test that builds two real, same-signer debug APK versions, upgrades with `adb install -r` without clearing data, and verifies that board/tray/score/Undo/best and sound/motion/haptics settings survive. Signed beta and physical-device acceptance remain outstanding.

- Added a dedicated API 35 emulator regression that builds two APK versions under one temporary debug signing identity, performs an in-place update while a real saved game is active, and verifies exact board/tray/score, best score and Undo continuity after upgrading. Temporary APKs remain off artifact uploads; physical owner-signed beta upgrade acceptance is still pending.

- Added a fail-closed, manually dispatched owner-signed beta APK channel with pinned certificate fingerprint and increasing versionCode overrides. The permanent beta signing secret is not supplied by CI; actual beta upgrades and physical-device data continuity are still unverified.

- Verify exact completed-turn persistence across a real Android force-stop and separate test process on every emulator profile: board, tray, score, best score and one-step Undo must survive and remain usable. Test evidence is stored as per-phase CI logs; update/reinstall continuity remains a separate gate.

- Expanded emulator QA from eight to eleven profiles: airplane-mode dark theme, three-button navigation and animations-on interaction stress. All profiles now verify cold process relaunch; automated UI tests verify the visible Help BACK / Options DONE controls and offline placement; synthetic OS Back-event injection was removed after repeated emulator focus races. Physical system Back input remains a manual QA gate.

- Expanded emulator QA with Android 8.0 (the declared minimum supported OS) and a 360×640 short phone at 1.3× font scale; added a lifecycle background/resume regression test for board, score and Undo preservation.

- Expanded the Android emulator suite to Android 16, 360dp phones at 1.4x text size, and 600dp tablet viewports. Real emulator screenshots exposed a cramped tablet layout: reserve a minimum 8dp gap above the board, with UI regressions for board/score spacing, controls, and help visibility. Real-device QA remains an independent release gate.

- Added a repeatable seeded multi-round gameplay soak regression test covering random moves, hints, fair tray refills, scoring, one-turn Undo, persistence and game-over boundaries. Automated results do not replace hands-on device playtesting.

- Auto-size the full SCORE and BEST values in compact layouts so very large scores do not wrap or overlap; an Android UI regression test covers Int.MAX_VALUE, placement and Undo.

- Added an offline HOW TO PLAY guide under Options, explaining scoring, drag/tap placement, hints, Undo and refills, with an Android UI navigation/state regression test.

- Suppress optional sound effects while the device is in Silent/Vibrate mode or media audio is muted/at zero volume, without changing device settings. Sound resumes automatically when the system allows it; added JVM policy regression tests.

- Validate restored one-step Undo checkpoints against actual placement rules, line clears, score changes and tray consumption. Corrupt or stale Undo data is discarded while the current game is preserved; full random-tray refills remain supported.

- Outline legal tap-to-place board anchors when a shape is selected; remove outlines during drag/hint overlays and after placement, while announcing the visual cue through TalkBack semantics.

- Improved tray accessibility: screen readers now announce exact block geometry, including dimensions and occupied columns per row; horizontal/vertical and mirrored shapes are distinguishable without vision.

- Updated SHA-pinned GitHub Actions toolchain in all four CI/release workflows: checkout v7.0.1, Java setup v6.1.0, Gradle setup v6.4.0, artifact upload v7.0.2 (no production signing secrets changed).

- Explain invalid drag-and-drop attempts with a brief accessible message while retaining the selected piece, score and Undo checkpoint for retry. Compile Android instrumentation tests in the fast CI build gate to catch test code regressions before emulator launch.

- Highlight rows and columns that will clear before a drag is released or a hinted move is confirmed; announce the preview in board accessibility labels and show prospective drag-clear points without spending them.

- Updated verified Gradle wrapper distribution to 9.8.2 using independently cross-checked official SHA-256 checksums (wrapper JAR remains unchanged).

- Reduced HINT work by reusing its placement analysis instead of replaying each possible move and refilling a simulated tray; preserved survival priorities and tie-breaking with seeded equivalence tests.

- HINT now looks one move ahead and prefers a placement that keeps the round alive over an immediately terminal high score; ties remain deterministic, and forced-loss positions still get a hint.

- Updated Android Gradle Plugin 9.4.1, Kotlin Compose compiler 2.4.21 and Compose BOM 2026.09.00 together, validated against the Gradle 9.8.1 toolchain and Android emulator matrix.

- Upgraded the verified Gradle build toolchain to 9.8.1 to address three high-rated security advisories; wrapper and distribution SHA-256 checks are enforced in all build workflows.

- Manifest security: no app backups/device transfer, no cleartext networking, and explicit offline privacy policy in Options.
- Verified and pinned GitHub Actions revisions, packaged APK privacy/SDK checks, signed-release ancestry and AAB verification gates.

- HINT now offers an explicit PLACE confirmation action with a spoken position and point value, supporting switch and screen-reader navigation without traversing all 64 cells.

- New three-piece trays protect against random no-move draws: if any shape can fit, at least one of the new pieces is playable. Difficulty and occupied cells are otherwise unchanged.

- Free, persistent one-turn Undo, also available on the game-over dialog; never decreases the best score.
- Existing alpha saves migrate automatically from the legacy session format.

- Free, deterministic best-immediate-score Hint with a suggested move preview; hints never alter the board or score.
- Compact title and settings controls for narrower phone widths.

- Playable native Kotlin / Jetpack Compose 8×8 block puzzle.
- Drag-to-place and accessible tap-to-place controls with aligned landing preview.
- Simultaneous full-row/column clearing, local scoring, and game-over detection.
- Saved offline sessions, local best score, configurable sound/haptics/animations.
- Original synthesized sound effects and independent launcher artwork.
- Gradle 9.6 wrapper, automated JVM testing, APK permission checks, and guarded release-signing workflow.

## 0.1.0 — 2026-10-17

Initial Android alpha foundation. No advertisements, network access, telemetry, or accounts.
