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
adb shell pm clear "$pkg" >/dev/null
mkdir -p app/build/emulator-screenshots
adb shell am start -W -n "$pkg/.MainActivity"
sleep 2

# A reproducible, real input-event stress case with system animations enabled.
# It is not a substitute for human gameplay/balance testing.
if [ "${1}" = "motion-enabled-360dp" ]; then
  monkey_log="app/build/emulator-screenshots/GridBlast-monkey-motion-enabled.log"
  adb shell monkey -p "$pkg" -s 20261104 --monitor-native-crashes \\
    --throttle 25 --pct-touch 70 --pct-motion 20 --pct-nav 10 250 \\
    > "$monkey_log" 2>&1 || { cat "$monkey_log"; exit 1; }
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
adb shell am start -W -n "$pkg/.MainActivity" \\
  > "app/build/emulator-screenshots/GridBlast-relaunch-${1}.txt"
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
