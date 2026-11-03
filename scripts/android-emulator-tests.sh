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
apk=app/build/outputs/apk/debug/app-debug.apk
test -s "$apk"
adb install -r "$apk" >/dev/null
adb shell pm clear tv.prohibited.gridblast >/dev/null
adb shell am start -W -n tv.prohibited.gridblast/.MainActivity
sleep 3
mkdir -p app/build/emulator-screenshots
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
