#!/usr/bin/env bash
set -euo pipefail
mode="${1:?debug or release}"
apk="${2:?APK path}"
aapt="${ANDROID_HOME:?Set ANDROID_HOME}/build-tools/36.0.0/aapt"
test -f "$apk"
[[ "$mode" == "debug" || "$mode" == "release" ]] || exit 2
permissions="$("$aapt" dump permissions "$apk")"
printf '%s\n' "$permissions"
# GridBlast is an offline game. Any packaged permission requires explicit audit;
# this blocks transitive SDK additions as well as intentional manifest changes.
if grep -Ei '(uses-permission|android\.permission\.|com\.google\.android\.gms\.permission\.AD_ID)' <<< "$permissions"; then
  echo "Unexpected packaged permission. Fail closed pending privacy review." >&2
  exit 1
fi
badging="$("$aapt" dump badging "$apk")"
grep -Fq "targetSdkVersion:'36'" <<< "$badging" || {
  echo "Google Play requires Android 16 / API 36 targeting for phone apps in 2026" >&2
  exit 1
}
if [[ "$mode" == "release" ]]; then
  if grep -q 'application-debuggable' <<< "$badging"; then
    echo "Release APK is debuggable." >&2
    exit 1
  fi
  # All-current Kotlin/Compose sources are Java bytecode. Native additions
  # require explicit 16KB-page-alignment review for Google Play acceptance.
  if unzip -Z1 "$apk" | grep -Eq '^lib/[^/]+/[^/]+\.so$'; then
    echo "Native libraries require 16 KB page-size verification before release." >&2
    exit 1
  fi
fi
echo "Validated $mode artifact: API 36, no permissions, no unreviewed native code."
