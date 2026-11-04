#!/bin/sh
# Emulator runner invokes one script command, not a multiline shell block.
# These are software-emulated configurations, not physical hardware acceptance.
set -eu

case "${1:-}" in
  compact-320dp)
    adb shell wm density 160
    adb shell wm size 320x720
    adb shell settings put system font_scale 1.0
    ;;
  large-font-360dp)
    # 360dp-wide phone with enlarged accessibility text.
    adb shell wm density 160
    adb shell wm size 360x800
    adb shell settings put system font_scale 1.4
    ;;
  short-large-font-360dp)
    # Tight 360x640 portrait viewport with enlarged Android text.
    # Stress that options, score and tray stay reachable without clipping.
    adb shell wm density 160
    adb shell wm size 360x640
    adb shell settings put system font_scale 1.3
    ;;
  offline-dark-360dp)
    adb shell wm density 160
    adb shell wm size 360x800
    adb shell settings put system font_scale 1.0
    adb shell cmd uimode night yes
    # The shell must actually switch to airplane mode; fail closed if unsupported.
    adb shell cmd connectivity airplane-mode enable
    offline="$(adb shell settings get global airplane_mode_on | tr -d '\r')"
    [ "$offline" = "1" ] || { echo "Airplane mode did not activate" >&2; exit 1; }
    ;;
  three-button-360dp)
    adb shell wm density 160
    adb shell wm size 360x800
    adb shell settings put system font_scale 1.0
    overlay=com.android.internal.systemui.navbar.threebutton
    adb shell cmd overlay enable --user 0 "$overlay"
    adb shell cmd overlay list | grep -F "[x] $overlay" >/dev/null || {
      echo "Three-button nav overlay could not be verified" >&2; exit 1;
    }
    ;;
  motion-enabled-360dp)
    adb shell wm density 160
    adb shell wm size 360x800
    adb shell settings put system font_scale 1.0
    # The GitHub Actions emulator runner keeps animation scales at 1.0 here.
    for setting in window_animation_scale transition_animation_scale animator_duration_scale; do
      adb shell settings put global "$setting" 1.0
    done
    ;;
  tablet-600dp)
    # Portrait tablet-sized viewport; not a substitute for real tablet input.
    adb shell wm density 160
    adb shell wm size 600x960
    adb shell settings put system font_scale 1.15
    ;;
  standard)
    adb shell settings put system font_scale 1.0
    ;;
  *)
    echo "Unsupported emulator form factor: ${1:-missing}" >&2
    exit 2
    ;;
esac

echo "Android emulator profile: ${1}"
adb shell wm size
adb shell wm density
printf 'Font scale: '
adb shell settings get system font_scale

sh scripts/verify-gradle-wrapper.sh
./gradlew --no-daemon :app:connectedDebugAndroidTest

# Capture actual rendered app pixels for manual layout review. This is an
# emulator screenshot, not a fabricated UI or a physical-device/store capture.
# Reinstall because Gradle instrumentation can uninstall the tested APK.
pkg=tv.prohibited.gridblast
apk=app/build/outputs/apk/debug/app-debug.apk
test -s "$apk"
adb install -r "$apk" >/dev/null
mkdir -p app/build/emulator-screenshots

# Install the real test APK and run two named tests in separate instrumentation
# invocations. The Android package is killed BETWEEN them; the second test
# asserts exact persisted board, tray, score, best score and Undo checkpoint.
test_apk="$(find app/build/outputs/apk/androidTest -type f -name '*-androidTest.apk' -print -quit)"
[ -n "$test_apk" ] && [ -s "$test_apk" ] || {
  echo "Instrumentation APK not found; cannot test process-death recovery" >&2
  exit 1
}
adb install -r "$test_apk" >/dev/null

recovery_test=tv.prohibited.gridblast.GameProcessRecoveryTest
runner="$pkg.test/androidx.test.runner.AndroidJUnitRunner"
run_recovery_phase() {
  phase="$1"
  method="$2"
  logfile="app/build/emulator-screenshots/GridBlast-process-recovery-${phase}-${ANDROID_API_LEVEL:-unknown}-${PROFILE}.txt"
  adb shell am instrument -w -e recovery_phase "$phase" -e class "$recovery_test#$method" "$runner" > "$logfile" 2>&1 || {
    cat "$logfile"
    exit 1
  }
  if ! grep -F 'OK (1 test)' "$logfile" >/dev/null; then
    echo "Android instrumentation phase '$phase' did not pass exactly one test" >&2
    cat "$logfile"
    exit 1
  fi
  echo "Real process recovery phase '$phase' passed"
}

PROFILE="$1"
adb shell pm clear "$pkg" >/dev/null

# The prepare test intentionally DOES NOT finish: its Compose activity stays
# alive until the host force-stops the Android app process. Only its on-device
# readiness marker proves that a real UI placement and persistence assertions
# were completed. A normal prepare test exit is a failure, not evidence.
prepare_log="app/build/emulator-screenshots/GridBlast-process-recovery-prepare-${ANDROID_API_LEVEL:-unknown}-${PROFILE}.txt"
adb shell am instrument -w -e recovery_phase prepare -e class "$recovery_test#prepareSavedTurnForForceStop" "$runner" > "$prepare_log" 2>&1 &
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
    echo "Prepare instrumentation exited before signal; expected a live app" >&2
    cat "$prepare_log"
    exit 1
  fi
  poll=$((poll + 1))
  sleep 1
done
if [ "$ready" != yes ]; then
  echo "Prepare phase never produced its on-device readiness marker" >&2
  cat "$prepare_log"
  kill "$prepare_host_pid" 2>/dev/null || true
  exit 1
fi
before_pid="$(adb shell pidof "$pkg" | tr -d '\r')"
[ -n "$before_pid" ] || {
  echo "App was not running at moment of attempted force-stop" >&2
  exit 1
}
echo "Force-stopping live GridBlast process PID $before_pid"
adb shell am force-stop "$pkg"
# Do not wait for success from the intentionally aborted prepare invocation.
kill "$prepare_host_pid" 2>/dev/null || true
wait "$prepare_host_pid" 2>/dev/null || true
after_pid="$(adb shell pidof "$pkg" 2>/dev/null | tr -d '\r' || true)"
[ -z "$after_pid" ] || {
  echo "Force-stop did not kill GridBlast process: $after_pid" >&2
  exit 1
}
run_recovery_phase verify verifySavedTurnAfterForceStopAndUndo

# Reset the now-verified test fixture before capturing the normal launch screenshot.
adb shell pm clear "$pkg" >/dev/null
adb shell am start -W -n "$pkg/.MainActivity"
sleep 2

# A reproducible, real input-event stress case with system animations enabled.
# It is not a substitute for human gameplay/balance testing.
if [ "${1}" = "motion-enabled-360dp" ]; then
  monkey_log="app/build/emulator-screenshots/GridBlast-monkey-motion-enabled.log"
  adb shell monkey -p "$pkg" -s 20261104 --monitor-native-crashes --throttle 25 --pct-touch 70 --pct-motion 20 --pct-nav 10 250 > "$monkey_log" 2>&1 || { cat "$monkey_log"; exit 1; }
  grep -F "Events injected: 250" "$monkey_log" >/dev/null || {
    echo "Random touch run finished without recording 250 events" >&2
    cat "$monkey_log"; exit 1;
  }
  echo "250 deterministic touch/motion/navigation events completed"
fi

# Test an Android OS force-stop followed by a true cold process launch.
# This checks startup survival, not that a completed turn survives process death;
# instrumentation separately checks lifecycle/save/Undo behavior.
adb shell am force-stop "$pkg"
adb shell am start -W -n "$pkg/.MainActivity" > "app/build/emulator-screenshots/GridBlast-relaunch-${1}.txt"
sleep 2
pid="$(adb shell pidof "$pkg" | tr -d '\r')"
[ -n "$pid" ] || {
  echo "App process did not survive force-stop/relaunch" >&2
  cat "app/build/emulator-screenshots/GridBlast-relaunch-${1}.txt"
  exit 1
}
echo "Cold process relaunch survived on ${1}: PID ${pid}"
screenshot="app/build/emulator-screenshots/GridBlast-api${ANDROID_API_LEVEL:-unknown}-${1}.png"
adb exec-out screencap -p > "$screenshot"
python3 - "$screenshot" <<'PY'
import struct
import sys
from pathlib import Path

path = Path(sys.argv[1])
data = path.read_bytes()
assert len(data) > 1000, "Empty/incomplete emulator screenshot"
assert data[:8] == bytes((137, 80, 78, 71, 13, 10, 26, 10)), "Invalid PNG signature"
width, height = struct.unpack(">II", data[16:24])
assert width >= 320 and height >= 600, f"Unexpected screenshot geometry: {width}x{height}"
print(f"Captured real emulator UI: {width}x{height} ({len(data)} bytes) at {path}")
PY
