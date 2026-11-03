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
