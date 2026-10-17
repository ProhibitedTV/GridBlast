# GridBlast

**An original, free, ad-free Android block puzzle game.** No ads, no tracking, no accounts, no subscriptions, and no network permission. Native Kotlin and Jetpack Compose.

## Play

Place shapes on an 8×8 grid. Fill a row or column to clear it and score more points. Use all three pieces to receive three new ones. The round ends when no available piece can fit anywhere.

- **Drag a piece** from the tray onto the grid. A preview shows exactly which cells will fill; red means it won't fit.
- Alternatively, **tap a piece**, then **tap its intended top-left board cell**. Tap a selected piece again to deselect it.
- Your current round and best score save automatically on the device, including if you close the app.
- **New Game** asks for confirmation when you've scored points, protecting accidental taps.
- Device vibration provides a small acknowledgement of successful moves.

## Build

Open the repository in Android Studio with JDK 17, Android SDK 37.0, and Gradle 9.6.0. The build uses AGP 9.4.0 and the Compose BOM 2026.08.00.

The Gradle wrapper binary is not yet checked in. With Gradle 9.6.0 installed:

```bash
gradle wrapper --gradle-version 9.6.0
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

On Windows use `gradlew.bat` after generating the wrapper. CI builds the APK for every pull request; successful runs have a **GridBlast-debug** artifact on the Actions tab. Download the ZIP, extract `app-debug.apk`, then install it on your Android device.

## Structure

- `app/src/main/java/tv/prohibited/gridblast/game`: deterministic Android-independent puzzle rules and versioned save-state codec
- `app/src/main/java/tv/prohibited/gridblast/ui`: Jetpack Compose board, gesture controls, previews and scoring
- `app/src/test`: JVM tests for gameplay and saved sessions
- `.github/workflows/android.yml`: build/test and debug APK pipeline

## Privacy and independence

The Android manifest intentionally has no `INTERNET`, advertising ID, or tracking permissions. No ad or analytics dependencies are included. Scores and in-progress game sessions are stored in private app preferences and never uploaded. Clearing app data also clears saved scores.

GridBlast uses independent game code, visuals and naming. It is not affiliated with Block Blast or its publisher.

## Next milestones

- Line-clear animations and optional sound/haptic settings
- Better accessibility and larger-screen layouts
- Gameplay balance and playtesting on different Android devices
- App icon, release signing and publication-ready screenshots
- Conventional Gradle wrapper and reproducible release pipeline

## License

MIT. See [LICENSE](LICENSE).
