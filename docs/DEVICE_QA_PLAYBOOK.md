# GridBlast physical Android QA playbook

**Status: unexecuted acceptance plan.** Seeded JVM gameplay soak tests cover many simulated turns, but they cannot validate human enjoyment, real touch performance, Android process death or hardware accessibility. Automated CI is valuable, but it does not establish actual touch comfort, TalkBack usability, real-device sound behavior, power consumption, or Play Store acceptance. Record **observed** results, never mark a scenario passed from code review alone. See [issue #5](https://github.com/ProhibitedTV/GridBlast/issues/5) and the [Play release gate](GOOGLE_PLAY_RELEASE_CHECKLIST.md).

## Automated Android emulator coverage

The [GitHub Actions emulator matrix](https://github.com/ProhibitedTV/GridBlast/actions/workflows/android-emulator.yml) runs the same real Compose instrumentation tests across these *software-emulated* configurations. The suite also simulates background/foreground lifecycle transitions to check saved score and one-turn Undo:

| Android level | Emulated viewport | Font scale | Purpose |
| --- | --- | --- | --- |
| API 26 / Android 8.0 | Standard Pixel-style phone | 1.0 | Verify the declared minimum supported Android version |
| API 29 / Android 10 | Standard Pixel-style phone | 1.0 | Older-platform usability |
| API 35 / Android 15 | Standard Pixel-style phone | 1.0 | Common phone baseline |
| API 35 / Android 15 | 320×720 at 160dpi (320dp wide) | 1.0 | Small-phone clipping and gestures |
| API 35 / Android 15 | 360×800 at 160dpi (360dp wide) | 1.4 | Enlarged text and dialog accessibility |
| API 35 / Android 15 | 360×640 at 160dpi (360dp wide) | 1.3 | Short screen + large text; fit board, controls and modal actions |
| API 35 / Android 15 | 360×800 at 160dpi | 1.0 | **Airplane mode + system dark theme**, no network entitlement |
| API 35 / Android 15 | 360×800 at 160dpi | 1.0 | **Three-button system navigation**, different usable insets; physical system Back behavior remains a manual QA gate |
| API 35 / Android 15 | 360×800 at 160dpi | 1.0 | **System animations enabled**; 250 reproducible random input events |
| API 35 / Android 15 | 360×800 at 160dpi | 1.0 | **Real A→B APK update-in-place** with identical temporary signer, versionCode 10001→10002, preserved active board/tray/score/Undo |
| API 36 / Android 16 | Standard Pixel-style phone | 1.0 | Modern platform/target-SDK regression |
| API 36 / Android 16 | 600×960 at 160dpi (600dp wide) | 1.15 | Tablet-sized portrait layout |

For **each emulator profile**, the smoke script now performs a real **two-process save/recovery check** after the ordinary Compose suite: one instrumentation invocation seeds a deterministic game, places a piece via the visible UI, verifies the on-device encoded board, score, tray, best score and valid one-step Undo, then **holds the active activity open** and writes a private readiness marker. The host waits for the marker, confirms a live app PID, force-stops that active process and confirms it disappeared; the prepare test must never finish normally. A second instrumentation invocation then launches a new process, checks the exact persisted state, uses Undo and verifies best score stays unchanged. The test methods are skipped in the ordinary suite unless CI supplies a phase-specific argument, preventing a false pass from one invocation alone. Separate per-phase logs are uploaded with the screenshots; a missing test APK, skipped test or failing assertion blocks the emulator job. This checks a concrete completed-turn recovery path, **not** power loss during a disk write, OS-wide data loss, reinstall/update, or every possible in-flight animation. The active-motion profile adds seeded random input; it cannot establish difficulty, fairness, ergonomics or sustained user satisfaction.

The dedicated **upgrade-in-place-360dp** profile is different from the other 11: it builds APK A and APK B on the same CI runner with its one **disposable debug signing identity** and increasing version codes; no owner or Google Play keystores are loaded or uploaded. It makes a real placement on A, leaves the activity live, performs `adb install -r` of B without uninstalling, clearing data, or restoring from backup, then starts a second instrumentation process to check the exact persisted score, best score, board, tray and one-step Undo. The script requires matching certificate fingerprints, version metadata, successful package manager update and a true passing verification phase. It uploads test logs and a **genuine emulator screenshot**; it deliberately keeps all ephemeral APKs in private runner temporary storage, outside artifacts. This is an *emulator regression for same-signer upgrades*, **not** proof of progress continuity between two owner-signed APKs on real hardware or across a signing-key change.

Changing an emulator's resolution/density is **not** equivalent to physical tablet hardware or a particular vendor's phone. This matrix can detect many startup, Compose layout, input, and persistence regressions, but it cannot validate thermal behavior, ergonomics, real audio routing, TalkBack speech usability, screen brightness, battery life, or store acceptance. An emulator report passes only when all jobs finish successfully.

After instrumented tests pass, each emulator launches a fresh installed build and records a **real screenshot of its rendered screen** in the `GridBlast-emulator-report-*` artifact. The screenshots are for manual clipping/layout review only. They are **not** proof of a physical-device test and must not be mislabeled as hardware captures or final Google Play store screenshots.

## Before starting

Use an installable APK from a successful [Android CI](https://github.com/ProhibitedTV/GridBlast/actions/workflows/android.yml) run on `main`, or a locally built debug APK. Capture the exact Git commit SHA and APK filename; avoid testing an unknown older build. For the signed-release gate, separately test the owner-signed release APK rather than treating debug QA as equivalent. Never publish production keystores, passwords, contacts, or private device logs.

For **every device** record:

| Field | Value |
| --- | --- |
| Date / tester | |
| Git commit / APK hash (SHA-256) | |
| Phone/tablet model and Android version | |
| Screen size / smallest width (dp) / font scale | |
| Orientation / gesture or 3-button navigation | |
| TalkBack / switch access / reduced motion settings | |
| Results: PASS / FAIL / NOT TESTED per section | |
| Repro steps / issue link for failures | |

Aim for a **320dp-width phone, a common midrange phone, a modern Android 16 device, and a tablet** if hardware is available. If a device is unavailable, mark the row **NOT TESTED** rather than claiming compatibility.

## A. Core mechanics, placement, and visual feedback

1. Start a new round; verify score is 0 and the three shapes are visible. Drag small and large pieces to each board corner, including near system gesture edges. The highlight and actual placed cells must match.
2. Release a piece partly outside the board, over an occupied cell, and back inside the tray. **Expected:** board, score, and available piece remain unchanged; a short readable failure message appears; retry works with the same piece.
3. Select a piece by tapping it, then place it with a board-cell tap. Compare the result with dragging the same shape in a different round. Illegal board cells must not be executable.
4. Complete a row, column, and crossed row/column. **Expected:** pre-placement preview marks exactly the cells that will clear; the score increases once; completed cells become empty; no overlapping intersection is double-counted.
5. Drag around a nearly complete row to verify live preview updates or disappears as the landing target changes. Move off-board and release; no score, tray, or saved-session change should occur.
6. Use HINT, inspect the highlighted move, then press PLACE. HINT itself must not change the score or consume a block. Repeat using the previewed board anchor instead of PLACE. Also tap **a different legal anchor** within a highlighted line: its accessibility description must not promise the preview's clear.
7. Use all tray pieces and confirm a new tray arrives. Continue to a board with very few spaces; a new random tray must not lose immediately when at least one shape fits.

## B. Recovery and durable local storage

1. Place a piece, press UNDO, and confirm board, tray and score return to the immediately preceding state; best score stays at its historical maximum.
2. Place again, force-stop GridBlast in Android settings, then reopen. Confirm current board and one-move Undo checkpoint survive process death.
3. Reach the game-over dialog. Use UNDO MOVE, verify the previous playable position returns, then play again. If no undo is available, PLAY AGAIN must still work.
4. Attempt NEW after scoring, cancel, then repeat and confirm. Cancel must retain the session; confirm starts a fresh round and preserves best score.
5. Turn sound off and on and toggle vibration and motion separately, recreate the activity, then force-stop/reopen; all three settings must persist.
6. Repeatedly tap/drag while clear animation is finishing. Check that no taps are lost, score is consistent, and game state remains valid after restart.

## C. Accessibility and device behavior

1. Enable **TalkBack** and navigate the title, SCORE, BEST, OPTIONS, NEW, UNDO, HINT and the three pieces. Each actionable control must have an understandable name and focus order.
2. Select a piece and explore legal vs illegal board locations. They must accurately describe whether placement is allowed; a **previewed clear** must not be mistaken for a clear from another anchor. Complete HINT → PLACE with TalkBack and, if available, switch access.
3. Increase Android font scale and display size; test 320dp and 360dp widths plus a tablet. Confirm score, board, toolbar, tray and modal buttons are visible, legible, and not clipped. Test portrait and landscape only where orientation is supported.
4. With media volume muted, device in silent mode and DND enabled, test sounds with **Sound effects** toggled on and off. Document the **actual** sound-routing behavior; do not assume ringer-silent implies media-silent. Also test headphones if available.
5. Disable vibration and animation; verify haptics and flashes respect in-app settings. Check Android system reduced-motion behavior separately and record any mismatch.
6. Run in airplane mode, open PRIVACY POLICY from OPTIONS, play and reopen. Check that no account, network access, ads, analytics or sign-in are required.

## D. Sustained play and release gate

- Play **10 or more complete rounds** on at least one midrange phone. Record crashes, freezes, ANRs, frame drops, battery drain, warmth, surprising difficulty spikes, unfair-feeling trays, or inaccessible interactions, with reproduction steps.
- Capture **two or more genuine, unaltered in-game screenshots** for store preparation; never use another game's artwork or claim screenshots were captured when they were not.
- On test devices with Android developer tools available, inspect relevant crash output and application responsiveness. Redact personal or unrelated log data before sharing.
- Repeat the core smoke test using the **actual signed release APK**, then inspect its signature, version code, target SDK and packaged permissions using the release workflow gate.

### Acceptance record

| Section | PASS / FAIL / NOT TESTED | Evidence or GitHub issue |
| --- | --- | --- |
| A. Touch, rules and hint | | |
| B. Undo, restart and persistence | | |
| C. Accessibility and settings | | |
| D. Long sessions and signed APK | | |
| Google Play owner-policy checklist | | |

**Do not close [issue #5](https://github.com/ProhibitedTV/GridBlast/issues/5) or declare GridBlast production-ready until device results and the remaining Play Console owner actions are recorded.** Report observations rather than predictions.
