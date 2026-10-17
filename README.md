# GridBlast

A native Android block-puzzle game with **no advertising, analytics, accounts, tracking, or network permission**. Built independently with Kotlin and Jetpack Compose, using original visuals and gameplay code.

## Gameplay (first playable milestone)

- Place blocks on an 8×8 grid. Tap a piece in the tray, then tap its top-left placement cell.
- Clear full rows and columns simultaneously to score points.
- Three new pieces appear after you use all three current pieces.
- The game ends when none of the remaining pieces fits.
- Best score is stored locally on your device.

## Development

Use Android Studio with JDK 17 and Android SDK 37 (compile SDK). The build uses **AGP 9.4.0**, **Gradle 9.6.0**, and the stable **Compose BOM 2026.08.00**.

A conventional Gradle wrapper binary is not yet committed. With Gradle 9.6.0 installed, run:

```bash
gradle wrapper --gradle-version 9.6.0
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

On Windows use `gradlew.bat` after generating the wrapper. Alternatively, open the project in Android Studio and configure Gradle 9.6.0. GitHub Actions installs a pinned Gradle version and publishes a debug APK for each successful build.

## Architecture

- `app/src/main/java/tv/prohibited/gridblast/game` — deterministic, Android-independent game engine
- `app/src/main/java/tv/prohibited/gridblast/ui` — Compose game board and piece picker
- `app/src/test` — JVM tests for rules, scoring, line clearing, and game-over

## Privacy

No ads, no ad SDK, no analytics SDK, no network permission. The only persistent data in this milestone is the best score, stored with private Android preferences.

## Roadmap

1. Drag-and-drop placement with a translucent landing preview, invalid-placement feedback, and haptics.
2. Save and resume full game sessions, undo, and polished effects.
3. Accessibility, audio toggles, original artwork, and adaptive screens.
4. Release signing, versioning, and Play Store/F-Droid packaging.

GridBlast is an independent original project. It is not affiliated with Block Blast or its publisher.
