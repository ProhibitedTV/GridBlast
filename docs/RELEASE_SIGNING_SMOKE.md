# Disposable-key Android release-signing smoke test

This is a continuous-integration **negative assurance / plumbing** test, not a
production Google Play release. It tests that the Gradle Android release
signing configuration can actually create and cryptographically verify a
signed `.apk` and `.aab`.

## How it works

- The GitHub Actions workflow checks out the source and validates the pinned
  Gradle 9.8.1 wrapper before doing any signing.
- A randomly generated, one-day-validity PKCS12 signing key is created under
  the runner's temporary directory. Its password never appears in logs.
- The release build uses the same `GRIDBLAST_RELEASE_*` environment wiring
  as the real release path, but with **disposable test values only**.
- Android `apksigner verify` validates the APK's signature and checks the
  expected CI-only signer identity; `jarsigner -verify` validates the AAB.
- The signed APK also passes the packaged permission, SDK-target, debuggable
  and 16 KiB native-library compatibility gates.
- A shell trap removes the keystore and password-containing temporary
  information on success **and** failure. CI does **not upload the APK,
  AAB, keystore or password as artifacts**.

The smoke key is not the real Play upload key, and **a smoke-passed AAB must
not be submitted to Google Play**. Do not call this a production signed release
or claim Play Console acceptance.

## Outstanding owner-held tasks

Use a persistent, securely backed-up developer-controlled **upload key**
for Play App Signing and configure the four repository Actions secrets
described in [README](../README.md#signed-release-candidates). Run the
actual gated `Android signed release candidate` workflow on reviewed `main`
code. Then verify the real signed bundle via Play Console internal testing,
followed by device QA, Data safety declarations, content rating and accurate
store screenshots.

See [the Google Play release checklist](GOOGLE_PLAY_RELEASE_CHECKLIST.md).
