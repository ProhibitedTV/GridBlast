# GridBlast: practical audience and Google Play launch plan

**Status:** Preparation, not evidence of a recruited audience or an approved release. All participation and feedback numbers must come from actual people. We do not buy reviews, script fake engagement, mass-message strangers, or track users inside the app.

## Positioning

**GridBlast** — an original offline block puzzle game with no ads, accounts, tracking or subscriptions. A free hint and one-step Undo emphasize thoughtful, low-pressure play.

**Initial audience:** Android puzzle players who prefer ad-free games, accessibility-minded players, and technically inclined early adopters willing to test an indie build. Do not present GridBlast as affiliated with Block Blast or as a clone's official release.

## Phase 1: recruit genuine alpha testers (now)

1. Point visitors to [install instructions](ALPHA_PLAYTEST.md) from README and the public early-tester issue.
2. Invite **10–20 voluntary Android testers** from existing opt-in communities: personal developer updates/newsletter, GitHub followers and relevant indie-game or Android community spaces **where promotion is permitted**. Be transparent that this is a sideloaded debug alpha.
3. Invite feedback after actual play, not just downloads. Give testers five tasks and one lightweight [feedback form](../.github/ISSUE_TEMPLATE/alpha-playtest.yml).
4. Reply to each actionable report and cross-link the fix/PR so testers know their feedback mattered. Keep a short, opt-in changelog; do not harvest emails.
5. Encourage people who want updates to **Watch** the repo or opt into an existing newsletter; do not promise automatic notifications for GitHub issue comments.

**Suggested internal measures** (targets, not current totals): ≥10 unique testers confirming a real install; feedback from ≥5 Android/device combinations; ≥8 useful feedback reports; ≥3 reports from repeat players. Count unique humans, not issue comments or GitHub stars.

## Phase 2: prepare a polished preview

- Export the original brand feature graphic as a **1024×500, 24-bit nontransparent PNG** and create a separate **512×512, 32-bit PNG** listing icon. Verify both files and brand rights before submission.
- Record **at least two authentic, unaltered screenshots** of the actual build; never pass a concept graphic or mockup off as an in-game screenshot.
- Capture a short honest gameplay clip from a device. A caption may explain controls, but must not imply a feature that is not in the submitted app.
- Refresh [listing copy](PLAY_STORE_LISTING.md) based on what real testers value.
- Obtain private, opt-in distribution for any closed Play test; GitHub stars, downloads and issue commenters **do not count** as Play Console opted-in testers.

## Phase 3: Play testing and launch decision

- Complete [hardware QA](DEVICE_QA_PLAYBOOK.md), test owner-signed release APK/AAB, confirm crash-free operation, Android 16 compatibility, accessibility, policy forms, screenshots, privacy policy and support contact.
- If the developer uses a **personal Play account created after November 13, 2023**, Google currently requires **12 opted-in closed testers for at least 14 continuous days** before requesting production access; verify account-specific requirements in Play Console. Recruit more than 12 interested people because opt-outs and dropouts can happen. An alpha APK download does not start this clock.
- Keep an explicit release gate: no serious crashes, data loss, inaccessible controls or known policy violations; sufficient real-device reports; owner-controlled key backed up, Play listing assets reviewed; successful Play pre-launch report.
- Prepare the launch post only when production access is actually available. Don't promise a release date until approved.

## Ready-to-adapt invitation (do not spam)

> I'm building **GridBlast**, a free, original Android block puzzle: fully offline, no ads, no accounts, just drag/tap gameplay, hints and Undo. I'm inviting a small group to try the early APK and tell me what actually works or feels frustrating. It's a sideloaded alpha, not a Play Store app yet. Download instructions: https://github.com/ProhibitedTV/GridBlast/blob/main/docs/ALPHA_PLAYTEST.md — constructive feedback is welcome.

## Launch post outline

Show **real** gameplay first, then the offline/ad-free promise, the actual feature list, a real screenshot or clip, the official testing/Play link, and a specific invitation to send feedback. Where communities prohibit promotion, do not post; participate normally and request moderator approval when needed.

## Public feedback hygiene

- Never publish beta invitees' emails or enrollment records on GitHub.
- Do not collect information that the app does not need. Report Android version, device model, reproduction steps and test build only.
- Do not offer money, prizes, favorable treatment or access in exchange for ratings/reviews. Ask for honest feedback instead.
- Separate organic interest from target metrics; do not claim a community exists until real participants arrive.
