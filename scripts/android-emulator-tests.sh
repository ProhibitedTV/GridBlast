#!/bin/sh
# Emulator runner invokes one script command, not a multiline shell block.
set -eu

case "${1:-}" in
  compact-320dp)
    adb shell wm density 160
    adb shell wm size 320x720
    adb shell settings put system font_scale 1.0
    echo "Running UI tests on compact 320dp emulator"
    adb shell wm size
    adb shell wm density
    ;;
  standard)
    echo "Running UI tests on standard emulator"
    ;;
  *)
    echo "Unsupported emulator form factor: ${1:-missing}" >&2
    exit 2
    ;;
esac

sh scripts/verify-gradle-wrapper.sh
./gradlew --no-daemon :app:connectedDebugAndroidTest
