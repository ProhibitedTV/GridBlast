# GridBlast security assessment

Last reviewed: October 18, 2026. Applies to the source and checks in this repository. This is a developer audit, **not** a third-party security certification or a guarantee of Google Play approval.

## Threat model and applied controls

| Area | Current control | Verification |
| --- | --- | --- |
| Network/data exfiltration | No network or ad permissions; no analytics, account or cloud code; cleartext traffic disabled | Inspect dependencies; fail CI on any packaged permission |
| App components | Only the launcher activity is intentionally exported; no providers, receivers or services | Python manifest gate rejects new exported components pending review |
| Private game saves | Android private SharedPreferences; no automatic cloud backup or Android 12+ device-transfer extraction; no external storage | Manifest/XML audit plus emulator privacy screen test |
| Sensitive data | No personal or sensitive information is required to play; game saves only board, scores and preferences | Review local code and any future SDK additions |
| Input and save parsing | Versioned bounded game history and validated state codec | Unit and emulator regression suite |
| CI supply chain | Gradle wrapper JAR matches trusted upstream Git hash and distribution SHA-256; GitHub Actions pinned to explicit commits | CI wrapper check and reviewed Dependabot updates |
| Build artifacts | No packaged Android permissions, API 36 target, no unreviewed native libraries, non-debuggable release build | Both debug and unsigned release APK checks in CI |
| Signing | Keystore/passwords provided by GitHub secrets, never checked into source; upload AAB and APK signature checks | Signed workflow fails closed with missing secrets; **not yet exercised with production keys** |
| Signing trigger | Release signing restricted to the checked-out commit on the reviewed main history; manual dispatch rejects unapproved branches | Release workflow ancestry gate |
| Store data disclosures | Public privacy policy in repository and full offline in-app policy accessible via Options | Emulator UI test and policy checklist |

## Security boundaries and limitations

- **Do not treat a successful CI run as a penetration test.** There is no static dependency-vulnerability scan, independent security review or physical-device audit yet.
- GitHub Actions secrets are entrusted to the repository/workflow administrators and GitHub runner environment. Use a **dedicated upload key** for Google Play, not an irreplaceable app-signing key; restrict who can approve changes to release workflows, create tags and manage secrets.
- Enable required status checks, protected `main`, review requirements, tag restrictions, and private vulnerability reporting in repository settings. The connected GitHub integration cannot configure these settings automatically.
- GitHub Actions/Gradle changes should be reviewed before Dependabot PRs are merged. Pinned actions protect against moving tags, not against a vulnerable action at a pinned commit.
- Future networking/SDK/ads changes invalidate the 'no collection' assumption. Reassess privacy policy, Data safety, exported components and permissions before every store release.
- The native-library gate deliberately fails the release if a bundled `.so` appears; any such dependency needs 16 KB memory-page support verification.
- App content, original branding/IP rights, screenshots, age rating, real-device stability, and actual Google Play review remain human release requirements.

## Responsible vulnerability reports

Use GitHub's private vulnerability reporting / repository Security Advisories when enabled. **Do not publish exploitable security details in a public issue.** Repository owners should enable private vulnerability reporting in Settings → Security and maintain a private security contact channel before a public launch.

See [Google Play release checklist](GOOGLE_PLAY_RELEASE_CHECKLIST.md).
