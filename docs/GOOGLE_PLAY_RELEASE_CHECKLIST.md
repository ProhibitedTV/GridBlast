# Google Play readiness and submission checklist

Last audited: October 18, 2026. **Not a Google Play approval claim.** GridBlast currently produces a working Android alpha. Only Google can accept a release submitted through Play Console.

## Technical requirements

- [x] Target **Android 16 / API 36** in `app/build.gradle.kts`, as required for new phone apps and updates from **August 31, 2026** ([Google policy](https://support.google.com/googleplay/android-developer/answer/11926878)).
- [x] Google Play-ready **Android App Bundle** (`.aab`) task exists, with signing via environment secrets; no signed public upload yet.
- [x] Android 8+ `minSdk 26`; Android 10 and 15 emulator regression suites, including a compact 320dp layout.
- [x] No app network/advertising permissions. CI checks the **packaged debug and release APKs**, not just the source manifest. The only allowed permission is AndroidX Core's app-local signature-level permission for non-exported dynamic receivers.
- [x] `allowBackup=false`; disable legacy backups, Android 12+ cloud extraction, and device transfer of private game records.
- [x] Restrict exported components and cleartext networking via manifest and a CI security gate.
- [x] GitHub Actions pinned to reviewed commit SHAs, Gradle distribution SHA-256 verified; Dependabot configured for reviewable updates.
- [x] CI verifies APK ZIP alignment and 64-bit native ELF load-segment alignment for Android's 16 KB memory-page requirement ([Android guidance](https://developer.android.com/guide/practices/page-sizes)).
- [ ] Generate an **owner-controlled upload key**, store it securely offline, and configure the four GitHub Actions secrets in README. Never commit a keystore or use a debug key.
- [ ] Run the signed-release workflow and verify the resulting `.aab` in Play Console's App Bundle Explorer / testing track. Preserve the upload key, consider Google-generated app signing key, and enroll in **Play App Signing** ([Android signing](https://developer.android.com/studio/publish/app-signing)).
- [ ] Confirm the final versionCode is unique and increasing, package name `tv.prohibited.gridblast` is registered, and app signing identity matches its intended update strategy.
- [ ] Install the **signed** release build on physical phones, including Android 16; check app startup, gesture placement, offline save/undo, TalkBack, muted audio, performance and screen sizes.
- [ ] Confirm real behavior with Play Console pre-launch report (crashes, ANRs, permissions, device compatibility). No screenshot or automated test substitutes for hands-on QA.
- [ ] Run an independent dependency/license review and watch for platform, library and security updates before publication.

## Store / policy / account requirements — owner actions

- [ ] Verify developer identity, payments/contact details and package registration in Play Console as required ([verification](https://support.google.com/googleplay/android-developer/answer/10841920)).
- [ ] For a **personal Play developer account created after November 13, 2023**, complete closed testing with at least **12 opted-in testers for 14 consecutive days** before applying for production access ([test requirements](https://support.google.com/googleplay/android-developer/answer/14151465)). Other accounts may have different access requirements.
- [ ] Select **Games → Puzzle**, provide accurate description and honest metadata. The draft listing in [PLAY_STORE_LISTING.md](PLAY_STORE_LISTING.md) is not a submitted listing.
- [ ] Capture **at least two genuine in-game screenshots** and prepare original feature/icon graphics at Play Console's currently requested sizes. Never fake UI or use another game's art ([asset rules](https://support.google.com/googleplay/android-developer/answer/9866151)).
- [ ] Verify rights and distinctiveness of the `GridBlast` name, icon, shapes, sounds and all store materials. Do not imply an affiliation with **Block Blast** or another publisher.
- [ ] Use the publicly accessible [GridBlast privacy policy](https://github.com/ProhibitedTV/GridBlast/blob/main/PRIVACY.md) in the Play Console field. Privacy text also appears offline under **Options → Privacy Policy**. Ensure the hosting URL is publicly readable, not a PDF, and accessible to reviewers ([privacy policy](https://support.google.com/googleplay/android-developer/answer/18258653)).
- [ ] Complete the mandatory **Data safety form**, including an accurate review of all transitive libraries. Current source and zero-permission build indicate no personal-data collection or sharing; verify this before declaring "No data collected" ([Data safety guide](https://support.google.com/googleplay/android-developer/answer/10787469)).
- [ ] Complete **Ads declaration: No**, **in-app purchases: No**, **account creation: No**, and any relevant Play Console financial / health / special-app declarations truthfully. No artificial claims about app ranking, reviews or special certifications.
- [ ] Complete the **content rating (IARC)** and **Target audience and content** questionnaires. Do not claim the game is designed for children or enroll in Families without reviewing the additional obligations.
- [ ] Ensure the Play Console developer name, email/contact route, website/privacy address and support contact are consistent. The current public GitHub issue tracker is a general contact mechanism; before launch establish and verify any contact details Play Console requires.
- [ ] Confirm category, availability countries, accessibility and policy requirements, then test through **internal and closed tracks** before the deliberate production rollout.
- [ ] Read the current [Google Play Developer Program policies](https://support.google.com/googleplay/android-developer/answer/18258653) and [minimum functionality rules](https://support.google.com/googleplay/android-developer/answer/9898783) at submission time. Reviewers may still reject buggy, misleading, duplicative or low-quality games.

## Current release decision

**Hold production submission.** Source/build privacy hardening can be validated in GitHub CI, but owner-managed signing, real phone tests, policy declarations, original screenshots and actual Play Console pre-launch review are still necessary. There is no Play approval guarantee.
