#!/bin/sh
# Android package-manager update regression. Uses TWO real APKs built with
# the SAME runner-local disposable debug signing identity, not an owner key.
# A playing app is updated in place; a new instrumentation process then
# asserts exact board/tray/score/Undo and persistent best score.
set -eu

pkg=tv.prohibited.gridblast
runner="$pkg.test/androidx.test.runner.AndroidJUnitRunner"
test_class=tv.prohibited.gridblast.GameProcessRecoveryTest
out=app/build/emulator-screenshots
mkdir -p "$out"
# APKs are deliberately retained only in the runner's private temp dir.
# Artifact upload includes logs and genuine screenshot, NEVER APKs or signing keys.
: "${RUNNER_TEMP:?Required isolated GitHub Actions runner}"
umask 077
workdir="$(mktemp -d "$RUNNER_TEMP/gridblast-upgrade-XXXXXXXX")"
trap 'rm -rf -- "$workdir"' EXIT HUP INT TERM
sh scripts/verify-gradle-wrapper.sh
adb shell wm density 160
adb shell wm size 360x800
adb shell settings put system font_scale 1.0

# Both APKs are built on the same CI runner, and Android's debug signing key
# remains constant only *within this job*. Never distribute these APKs as beta.
export GRIDBLAST_VERSION_CODE=10001
export GRIDBLAST_VERSION_NAME=0.1-beta-ci-a
./gradlew --no-daemon :app:assembleDebug :app:assembleDebugAndroidTest
apk_path=app/build/outputs/apk/debug/app-debug.apk
test_path="$(find app/build/outputs/apk/androidTest -type f -name '*-androidTest.apk' -print -quit)"
[ -s "$apk_path" ] && [ -n "$test_path" ] && [ -s "$test_path" ] || {
  echo "Missing A APK or instrumentation APK" >&2
  exit 1
}
cp "$apk_path" "$workdir/GridBlast-upgrade-A-debug.apk"
cp "$test_path" "$workdir/GridBlast-upgrade-A-tests.apk"

export GRIDBLAST_VERSION_CODE=10002
export GRIDBLAST_VERSION_NAME=0.1-beta-ci-b
# Force repackaging even if Gradle caches a build that used another env value.
./gradlew --no-daemon --rerun-tasks :app:assembleDebug
[ -s "$apk_path" ] || { echo "Missing B APK" >&2; exit 1; }
cp "$apk_path" "$workdir/GridBlast-upgrade-B-debug.apk"

a="$workdir/GridBlast-upgrade-A-debug.apk"
b="$workdir/GridBlast-upgrade-B-debug.apk"
aapt="$ANDROID_HOME/build-tools/36.0.0/aapt"
apksigner="$ANDROID_HOME/build-tools/36.0.0/apksigner"
"$apksigner" verify --print-certs "$a" > "$out/GridBlast-upgrade-A-cert.txt"
"$apksigner" verify --print-certs "$b" > "$out/GridBlast-upgrade-B-cert.txt"
sha_a="$(sed -n 's/.*Signer #1 certificate SHA-256 digest: //p' "$out/GridBlast-upgrade-A-cert.txt" | head -n 1)"
sha_b="$(sed -n 's/.*Signer #1 certificate SHA-256 digest: //p' "$out/GridBlast-upgrade-B-cert.txt" | head -n 1)"
[ -n "$sha_a" ] && [ "$sha_a" = "$sha_b" ] || {
  echo "Test APK A and B must have the same signing certificate" >&2
  exit 1
}
"$aapt" dump badging "$a" | grep -F "package: name='$pkg' versionCode='10001'" >/dev/null || {
  echo "Wrong versionCode/package in A" >&2; exit 1;
}
"$aapt" dump badging "$b" | grep -F "package: name='$pkg' versionCode='10002'" >/dev/null || {
  echo "Wrong versionCode/package in B" >&2; exit 1;
}
echo "Two APKs verified: same CI-only signer, versions 10001 -> 10002."

# Begin from A with a clean fixture. This is the ONLY pm clear call: no data
# clear, uninstall or backup restore is allowed across the A -> B upgrade.
adb install -r "$a" > "$out/GridBlast-upgrade-install-A.log"
adb install -r "$workdir/GridBlast-upgrade-A-tests.apk" > "$out/GridBlast-upgrade-test-install.log"
adb shell pm clear "$pkg" > "$out/GridBlast-upgrade-initial-clear.log"

# The prepare phase exercises the app UI to PLACE a real move, checks on-disk
# session and Undo via its real codecs, writes an fsynced readiness marker and
# intentionally keeps the Activity running. We UPDATE that live app.
prepare_log="$out/GridBlast-upgrade-prepare.txt"
adb shell am instrument -w -e recovery_phase prepare -e class "$test_class#prepareSavedTurnForForceStop" "$runner" > "$prepare_log" 2>&1 &
prepare_host_pid=$!
ready=no
poll=0
while [ "$poll" -lt 65 ]; do
  marker="$(adb shell run-as "$pkg" cat files/gridblast-recovery-ready-v1 2>/dev/null | tr -d '\r' || true)"
  if [ "$marker" = "ready-v1" ]; then
    ready=yes
    break
  fi
  if ! kill -0 "$prepare_host_pid" 2>/dev/null; then
    echo "Prepare instrumentation exited before a live game was ready" >&2
    cat "$prepare_log"
    exit 1
  fi
  poll=$((poll + 1))
  sleep 1
done
if [ "$ready" != yes ]; then
  echo "Prepare did not confirm live persisted progress" >&2
  cat "$prepare_log"
  kill "$prepare_host_pid" 2>/dev/null || true
  exit 1
fi

old_pid="$(adb shell pidof "$pkg" | tr -d '\r')"
[ -n "$old_pid" ] || { echo "Game not running before APK upgrade" >&2; exit 1; }
echo "Installing B over ACTIVE A PID $old_pid, without clearing app data"
adb install -r "$b" > "$out/GridBlast-upgrade-install-B.log"
grep -Fx 'Success' "$out/GridBlast-upgrade-install-B.log" >/dev/null || {
  cat "$out/GridBlast-upgrade-install-B.log"
  echo "Android package manager refused in-place update" >&2
  exit 1
}
kill "$prepare_host_pid" 2>/dev/null || true
wait "$prepare_host_pid" 2>/dev/null || true

# Android must now be running new code, under the same package/data identity.
adb shell dumpsys package "$pkg" > "$out/GridBlast-upgrade-installed-package.txt"
grep -E 'versionCode=10002([[:space:]]|$)' "$out/GridBlast-upgrade-installed-package.txt" >/dev/null || {
  echo "Installed app does not report versionCode 10002" >&2
  exit 1
}
verify_log="$out/GridBlast-upgrade-verify.txt"
adb shell am instrument -w -e recovery_phase verify -e class "$test_class#verifySavedTurnAfterForceStopAndUndo" "$runner" > "$verify_log" 2>&1 || {
  cat "$verify_log"
  exit 1
}
grep -F 'OK (1 test)' "$verify_log" >/dev/null || {
  echo "Exact board, score, tray or Undo did NOT survive version upgrade" >&2
  cat "$verify_log"
  exit 1
}
# Actual running upgraded APK screenshot (not a marketing image).
adb shell am start -W -n "$pkg/.MainActivity" > "$out/GridBlast-upgrade-launch-B.txt"
sleep 2
adb exec-out screencap -p > "$out/GridBlast-upgrade-B-live.png"
python3 - "$out/GridBlast-upgrade-B-live.png" <<'PY'
import struct
import sys
from pathlib import Path
data=Path(sys.argv[1]).read_bytes()
assert len(data)>1000 and data[:8]==bytes((137,80,78,71,13,10,26,10))
width,height=struct.unpack(">II", data[16:24])
assert (width,height)==(360,800), f"Unexpected emulator screenshot: {width}x{height}"
print(f"Captured upgraded GridBlast app at {width}x{height}")
PY
echo "PASS: in-place same-certificate APK upgrade with exact saved turn, best score and Undo continuity."
