# GridBlast Android alpha — playtest guide

GridBlast is an **original, free, ad-free, offline Android block puzzle** in active alpha testing. It has **not** been approved or published on Google Play. This page is for voluntary testers who want to help improve the game.

## Download the Android alpha

1. Sign into GitHub and open [Android CI](https://github.com/ProhibitedTV/GridBlast/actions/workflows/android.yml).
2. Select a **successful run on `main`** (not an unreviewed PR or failed run).
3. Find **Artifacts → GridBlast-debug** at the bottom of that run and download the ZIP.
4. Extract `app-debug.apk` from the ZIP. On Android, open that APK in your Files app and follow the install prompt. Android may ask you to allow **Install unknown apps** for Files or your browser; you can turn that permission off again immediately afterward.
5. Open **GridBlast**. No sign-in or internet connection is required to play.

**Security and compatibility:** Only use artifacts linked from this official repository. This is a development-signed sideload build, not a Google Play download; Android may display a sideload warning. Don't disable Play Protect or other system protections to install it. Android 8.0+ is the configured minimum, but physical compatibility is **not yet verified** across all devices. Existing installs signed with a different key may require uninstalling before installing this debug build; uninstalling clears local game progress.

Artifacts expire automatically. If the latest run has no artifact, use an earlier **successful `main`** Android CI run with an unexpired `GridBlast-debug` artifact.

## Try these five things

- Play a round using **drag**, then try **tap-to-select and tap-to-place**. Do the outlines make sense?
- Complete rows and columns, try **HINT → PLACE**, and use **UNDO** after placing a shape.
- Close and reopen the app after making moves. Do your board, score and Undo checkpoint restore?
- Test **Options → How to Play**, optional audio, vibration and clear animations. Check accessibility with TalkBack if you use it.
- Try at least a few rounds and tell us whether the controls feel good, whether the piece distribution seems fair, and whether you would play again.

## Send feedback

Use the [**Alpha Playtest Feedback** GitHub issue form](https://github.com/ProhibitedTV/GridBlast/issues/new?template=alpha-playtest.yml) for bugs, usability issues and constructive suggestions. For a general discussion or to signal interest in further tests, join the [early-tester discussion](https://github.com/ProhibitedTV/GridBlast/issues?q=is%3Aissue+is%3Aopen+alpha+playtest). 

Please include your **device model, Android version, APK commit/run, what you did, what you expected, and what happened** when reporting a bug. Screenshots should come from the *real running game* and contain no personal data. Do **not** post personal email addresses, phone numbers, private logs, passwords or device identifiers in public issues. A public GitHub account is needed to comment; Play Console's future closed tests will use a separate, private enrollment flow.

## What happens to feedback?

The maintainer will review reproducible issues, prioritize crashes, inaccessible interactions, data loss, release blockers and misleading UI first, and document fixes in pull requests. Suggestions may become roadmap work, but no feature or launch date is promised.

For release readiness, see [physical-device QA](DEVICE_QA_PLAYBOOK.md) and the [Google Play checklist](GOOGLE_PLAY_RELEASE_CHECKLIST.md).
