# GridBlast

**An original, free, ad-free Android block puzzle game.** No ads, no tracking, no accounts, no subscriptions, and no network permission. Native Kotlin and Jetpack Compose.

## Play

Place shapes on an 8×8 grid. Fill a row or column to clear it and score more points. Use all three pieces to receive three new ones. New trays guarantee at least one legal placement when any shape can fit, so the next tray cannot end a round purely through bad luck. The round ends when no available piece can fit anywhere.

- **Drag a piece** from the tray onto the grid. The lifted piece and board preview share the same placement anchor; red means it won't fit.
- Alternatively, **tap a piece**, then **tap its intended top-left board cell**. Tap a selected piece again to deselect it.
- Your current round, most recent undo checkpoint, and best score save automatically on the device.
- **New Game** asks for confirmation when you've scored points.
- Cleared cells briefly flash and the earned points are displayed.
- **Options** independently controls sound, vibration, and clear animations. Original synthesized sound effects are disabled by default.
- **Hint** is always free and available offline. Press **HINT** to preview a legal move and its point value; then tap the highlighted board cell **or press PLACE** to confirm it. HINT alone never spends points or moves pieces.
- **Undo** reverts the most recent successful placement for free, including after game over. Only one turn is available at a time; it survives app restarts, and your all-time best score is never reduced.

## Accessibility

Every tray piece is selectable by tap, switch access or screen reader. Once a shape is selected, legal board locations are marked as actionable; illegal placements cannot be activated. **HINT → PLACE** offers a shorter, two-action route to a recommended move without navigating the entire board. Undo, New Game and Options are ordinary accessible buttons. Please report real-device TalkBack problems in [issue #5](https://github.com/ProhibitedTV/GridBlast/issues/5); automated semantic tests are not a substitute for human evaluation.

## Build from a clean checkout

Requirements: Android SDK 37.0 (with SDK platform tooling), Android Studio or JDK 17, and an internet connection for initial Gradle/Android dependency downloads. **You do not need Gradle installed**: this repository commits the official Gradle 9.8.1 wrapper and verifies the wrapper JAR against Gradle's official SHA-256 and pins the distribution ZIP checksum.

```bash
git clone https://github.com/ProhibitedTV/GridBlast.git
cd GridBlast
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

On Windows, run `gradlew.bat :app:testDebugUnitTest :app:assembleDebug`. Android Studio can open this directory as an existing project. On systems where Android SDK 37 is exposed as `android-37.0`, CI sets an `android-37` compatibility path; your local SDK may need the same symlink until tooling supports the renamed platform.

**Install an alpha:** In GitHub's **Actions → Android CI → successful run**, download `GridBlast-debug`, extract `app-debug.apk`, and install it on your phone. Alternatively use the output at `app/build/outputs/apk/debug/app-debug.apk`.

## Signed release candidates

The workflow at `.github/workflows/android-release.yml` is triggered by a `v*` tag or a manual dispatch. It **fails closed** until you configure these four GitHub Actions secrets:

| Secret | Meaning |
| --- | --- |
| `GRIDBLAST_RELEASE_KEYSTORE_B64` | Base64-encoded Android release signing keystore |
| `GRIDBLAST_RELEASE_STORE_PASSWORD` | Keystore password |
| `GRIDBLAST_RELEASE_KEY_ALIAS` | Signing key alias |
| `GRIDBLAST_RELEASE_KEY_PASSWORD` | Signing key password |

Keep an encrypted backup of your release keystore and passwords; losing your original signing identity can prevent future update distribution. **Do not commit a keystore or secrets.** After secrets exist and hardware QA is complete, you can create a version tag or run the release workflow manually. Successful runs produce signed APK and AAB artifacts with their APK signature verified. A build artifact is not automatically a public Play Store listing or GitHub Release.

Normal PR CI also checks unsigned release bundle compilation, validates the committed wrapper, and rejects APKs with internet/network/advertising permissions.

## Architecture

- `app/src/main/java/tv/prohibited/gridblast/game`: deterministic Android-independent game rules, placement geometry, one-move history and versioned save-state codecs
- `app/src/main/java/tv/prohibited/gridblast/ui`: Compose board, gesture controls, previews and scoring
- `app/src/test`: JVM gameplay and placement regression tests
- `app/src/androidTest`: instrumentation smoke tests for real UI interactions and persisted state
- `.github/workflows/android.yml`: debug CI and privacy gate
- `.github/workflows/android-release.yml`: guarded signed-release pipeline
- `PRIVACY.md`: privacy policy
- `CHANGELOG.md`: release history

## Privacy and independence

No `INTERNET`, advertising ID, location, analytics or tracking permission. Local saves are excluded from automatic backup and device transfer on supported Android versions, and the privacy policy is readable offline under **Options → Privacy Policy**. Board, score and preferences stay in private Android storage and are never uploaded. See [PRIVACY.md](PRIVACY.md).

GridBlast uses independent game code, visuals and naming. It is not affiliated with Block Blast or its publisher.

See [Google Play release checklist](docs/GOOGLE_PLAY_RELEASE_CHECKLIST.md) and [draft Play listing](docs/PLAY_STORE_LISTING.md) for publication prerequisites. Google Play approval is not guaranteed by a successful build.

## Remaining pre-1.0 work

- Verify drag placement, hints, compact toolbar, TalkBack, audio behavior and performance on physical phones and tablets.
- Production screenshots, store metadata and signing secrets.
- Automated emulator UI tests on Android 10, Android 15, and an Android 15 320dp-wide phone layout; deeper physical-device validation, feature polish and gameplay balance.

## License

MIT. See [LICENSE](LICENSE).
