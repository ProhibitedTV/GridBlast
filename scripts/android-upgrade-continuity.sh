#!/usr/bin/env bash
# Test-only two-version install-over-install acceptance on a REAL Android
# emulator. Never use owner signing secrets or upload these disposable APKs.
set -euo pipefail

: "${RUNNER_TEMP:?This smoke test needs an isolated Actions runner}"
: "${ANDROID_HOME:?Android SDK is required}"
umask 077

pkg=tv.prohibited.gridblast
test_class=tv.prohibited.gridblast.AppUpgradeContinuityTest
instrument_runner="$pkg.test/androidx.test.runner.AndroidJUnitRunner"
apk="app/build/outputs/apk/debug/app-debug.apk"
report_dir="app/build/upgrade-continuity"
mkdir -p "$report_dir"
private_dir="$(mktemp -d "$RUNNER_TEMP/gridblast-upgrade-XXXXXXXX")"
cleanup() {
  rm -rf -- "$private_dir"
}
trap cleanup EXIT

# Create one isolated signer that is reused only for version A and version B.
password="$(openssl rand -hex 24)"
echo "::add-mask::$password"
export GRIDBLAST_UPGRADE_CI_KEYSTORE_FILE="$private_dir/disposable-upgrade-ci.p12"
export GRIDBLAST_UPGRADE_CI_STORE_PASSWORD="$password"
export GRIDBLAST_UPGRADE_CI_KEY_ALIAS=gridblast-upgrade-ci
export GRIDBLAST_UPGRADE_CI_KEY_PASSWORD="$password"
keytool -genkeypair \
  -keystore "$GRIDBLAST_UPGRADE_CI_KEYSTORE_FILE" -storetype PKCS12 \
  -alias "$GRIDBLAST_UPGRADE_CI_KEY_ALIAS" -keyalg RSA -keysize 3072 \
  -sigalg SHA256withRSA -validity 2 \
  -storepass "$password" -keypass "$password" \
  -dname "CN=Disposable Upgrade Test, O=GridBlast, C=US" \
  -noprompt >/dev/null 2>&1

sh scripts/verify-gradle-wrapper.sh
python3 scripts/verify-android-manifest.py

aapt="$ANDROID_HOME/build-tools/36.0.0/aapt"
apksigner="$ANDROID_HOME/build-tools/36.0.0/apksigner"
get_cert() {
  "$apksigner" verify --print-certs "$1" \
    | awk '/Signer #1 certificate SHA-256 digest:/ {print $NF}'
}
check_apk() {
  local name="$1" expected_code="$2" output="$3"
  test -s "$apk"
  "$aapt" dump badging "$apk" > "$report_dir/$name-badging.txt"
  grep -Fq "package: name='$pkg' versionCode='$expected_code'" \
    "$report_dir/$name-badging.txt" || {
      echo "$name: expected app ID/versionCode $pkg/$expected_code" >&2
      exit 1
    }
  bash scripts/verify-apk-security.sh debug "$apk" \
    > "$report_dir/$name-permission-check.txt"
  "$apksigner" verify --print-certs "$apk" \
    > "$report_dir/$name-signature.txt"
  local sha
  sha="$(get_cert "$apk")"
  [[ "$sha" =~ ^[a-f0-9]{64}$ ]] || {
    echo "$name: unable to parse signing certificate" >&2
    exit 1
  }
  printf -v "$output" '%s' "$sha"
}
phase() {
  local name="$1" method="$2" log="$report_dir/$1-instrumentation.txt"
  adb shell am instrument -w \
    -e upgrade_phase "$name" -e class "$test_class#$method" \
    "$instrument_runner" > "$log" 2>&1 || {
      cat "$log"
      exit 1
    }
  grep -Fq 'OK (1 test)' "$log" || {
    echo "Upgrade test '$name' failed or was skipped" >&2
    cat "$log"
    exit 1
  }
  echo "Version $name: 1 real Android UI instrumentation test passed"
}

# A. Fresh install, play, and persist an exact known board + settings.
export GRIDBLAST_VERSION_CODE=100101
export GRIDBLAST_VERSION_NAME=0.1-upgrade-a
./gradlew --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest
check_apk version-a "$GRIDBLAST_VERSION_CODE" cert_a

test_apk="$(find app/build/outputs/apk/androidTest -type f -name '*-androidTest.apk' -print -quit)"
test -n "$test_apk" && test -s "$test_apk" || {
  echo "Missing instrumentation APK" >&2
  exit 1
}
test_cert="$(get_cert "$test_apk")"
[[ "$cert_a" == "$test_cert" ]] || {
  echo "Instrumentation and app must use the same disposable signing key" >&2
  exit 1
}
# Only the initial setup may remove previous data; never clear after prepare.
adb uninstall "$pkg" >/dev/null 2>&1 || true
adb uninstall "$pkg.test" >/dev/null 2>&1 || true
adb install "$apk" > "$report_dir/install-a.txt"
adb install "$test_apk" > "$report_dir/test-install.txt"
phase prepare prepareGameAndPreferencesOnVersionA

# B. Compile a distinct APK, confirm cert continuity and a strictly larger
# versionCode, then upgrade with -r (without uninstall, backup, or pm clear).
export GRIDBLAST_VERSION_CODE=100102
export GRIDBLAST_VERSION_NAME=0.1-upgrade-b
./gradlew --no-daemon :app:assembleDebug
check_apk version-b "$GRIDBLAST_VERSION_CODE" cert_b
[[ "$cert_a" == "$cert_b" ]] || {
  echo "Signing identity changed between versions A and B" >&2
  exit 1
}
adb install -r "$apk" > "$report_dir/install-b-upgrade.txt"
grep -Fq 'Success' "$report_dir/install-b-upgrade.txt" || {
  echo "Android did not report a successful in-place APK upgrade" >&2
  exit 1
}
adb shell dumpsys package "$pkg" > "$report_dir/installed-package.txt"
grep -Fq 'versionCode=100102' "$report_dir/installed-package.txt" || {
  echo "The installed package is not version B" >&2
  exit 1
}
phase verify verifyProgressAndUndoOnVersionB

echo "REAL emulator install-over-install PASSED: same certificate; 100101 -> 100102; board/score/tray/Undo/best/preferences verified."
echo "These throwaway debug APKs are NEVER uploaded or treated as owner-signed betas."
