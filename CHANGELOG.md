# Changelog

All notable GridBlast changes are summarized here.

## Unreleased (alpha)

- Added an offline HOW TO PLAY guide under Options, explaining scoring, drag/tap placement, hints, Undo and refills, with an Android UI navigation/state regression test.

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
