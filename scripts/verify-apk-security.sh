#!/usr/bin/env bash
set -euo pipefail
mode="${1:?debug or release}"
apk="${2:?APK path}"
aapt="${ANDROID_HOME:?Set ANDROID_HOME}/build-tools/36.0.0/aapt"
test -f "$apk"
[[ "$mode" == "debug" || "$mode" == "release" ]] || exit 2
permissions="$("$aapt" dump permissions "$apk")"
printf '%s\n' "$permissions"
# AndroidX Core declares an app-specific signature permission to protect
# not-exported dynamic broadcast receivers on pre-Android 13 devices.
# It does not grant network, ad, location or personal-data access.
# Fail closed for anything except the exact internal AndroidX permission.
allowed="tv.prohibited.gridblast.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION"
while IFS= read -r line; do
  [[ -z "$line" ]] && continue
  if [[ "$line" == "permission: $allowed" ||
        "$line" == "uses-permission: name='$allowed'" ]]; then
    continue
  fi
  echo "Unexpected packaged permission: $line" >&2
  exit 1
done < <(grep -E '^(uses-permission|permission):' <<< "$permissions" || true)
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
echo "Validated $mode artifact: API 36, only AndroidX private signature permission, no unreviewed native code."
