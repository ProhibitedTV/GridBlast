# Changelog

All notable GridBlast changes are summarized here.

## Unreleased (alpha)

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
