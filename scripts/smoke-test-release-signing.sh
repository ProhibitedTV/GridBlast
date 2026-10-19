#!/usr/bin/env bash
# NOT A PRODUCTION RELEASE. A fresh disposable private key is created and
# removed inside this single CI step. No signed artifact is uploaded.
set -euo pipefail

: "${RUNNER_TEMP:?Must run on an isolated GitHub Actions runner}"
: "${ANDROID_HOME:?Android SDK must be set}"
umask 077

tmpdir="$(mktemp -d "$RUNNER_TEMP/gridblast-ci-signing-XXXXXXXX")"
cleanup() {
  # Keystore was generated only for this ephemeral verification step.
  rm -rf -- "$tmpdir"
}
trap cleanup EXIT

password="$(openssl rand -hex 24)"
echo "::add-mask::$password"
keystore="$tmpdir/ci-only-upload-key.p12"
keytool -genkeypair \
  -keystore "$keystore" -storetype PKCS12 \
  -alias gridblast-ci-only \
  -keyalg RSA -keysize 3072 -sigalg SHA256withRSA \
  -validity 1 \
  -storepass "$password" -keypass "$password" \
  -dname "CN=Disposable CI Signing Test, O=GridBlast, C=US" \
  -noprompt >/dev/null 2>&1

export GRIDBLAST_RELEASE_KEYSTORE_FILE="$keystore"
export GRIDBLAST_RELEASE_STORE_PASSWORD="$password"
export GRIDBLAST_RELEASE_KEY_ALIAS="gridblast-ci-only"
export GRIDBLAST_RELEASE_KEY_PASSWORD="$password"

./gradlew --no-daemon :app:assembleRelease :app:bundleRelease

apk="app/build/outputs/apk/release/app-release.apk"
aab="app/build/outputs/bundle/release/app-release.aab"
test -s "$apk"
test -s "$aab"

"$ANDROID_HOME/build-tools/36.0.0/apksigner" verify --print-certs "$apk" > "$tmpdir/apk-signature.txt"
grep -Fq "CN=Disposable CI Signing Test" "$tmpdir/apk-signature.txt" || {
  echo "APK was not signed with the expected disposable CI key" >&2
  exit 1
}
jarsigner -verify "$aab" > "$tmpdir/bundle-signature.txt" 2>&1
grep -Fq "jar verified" "$tmpdir/bundle-signature.txt" || {
  echo "AAB archive signature did not verify" >&2
  exit 1
}

bash scripts/verify-apk-security.sh release "$apk"
echo "CI-only APK and AAB signatures valid; production upload key was never accessed."
echo "DO NOT UPLOAD THIS DISPOSABLE-KEY BUILD TO GOOGLE PLAY."
