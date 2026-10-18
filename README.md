# GridBlast

**An original, free, ad-free Android block puzzle game.** No ads, no tracking, no accounts, no subscriptions, and no network permission. Native Kotlin and Jetpack Compose.

## Play

Place shapes on an 8×8 grid. Fill a row or column to clear it and score more points. Use all three pieces to receive three new ones. The round ends when no available piece can fit anywhere.

- **Drag a piece** from the tray onto the grid. The lifted piece and board preview share the same placement anchor; red means it won't fit.
- Alternatively, **tap a piece**, then **tap its intended top-left board cell**. Tap a selected piece again to deselect it.
- Your current round, most recent undo checkpoint, and best score save automatically on the device.
- **New Game** asks for confirmation when you've scored points.
- Cleared cells briefly flash and the earned points are displayed.
- **Options** independently controls sound, vibration, and clear animations. Original synthesized sound effects are disabled by default.
- **Hint** is always free and available offline. It previews a suggested legal placement with its immediate point value; tap the highlighted location to play it. Asking for a hint never spends points or moves pieces.
- **Undo** reverts the most recent successful placement for free, including after game over. Only one turn is available at a time; it survives app restarts, and your all-time best score is never reduced.

## Build from a clean checkout

Requirements: Android SDK 37.0 (with SDK platform tooling), Android Studio or JDK 17, and an internet connection for initial Gradle/Android dependency downloads. **You do not need Gradle installed**: this repository commits the official Gradle 9.6 wrapper and pins the distribution SHA-256.

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

No `INTERNET`, advertising ID, location, analytics or tracking permission. Board, score and preferences stay in private Android storage and are never uploaded. See [PRIVACY.md](PRIVACY.md).

GridBlast uses independent game code, visuals and naming. It is not affiliated with Block Blast or its publisher.

## Remaining pre-1.0 work

- Verify drag placement, hints, compact toolbar, TalkBack, audio behavior and performance on physical phones and tablets.
- Production screenshots, store metadata and signing secrets.
- Automated emulator UI tests on Android 10, Android 15, and an Android 15 320dp-wide phone layout; deeper physical-device validation, feature polish and gameplay balance.

## License

MIT. See [LICENSE](LICENSE).
