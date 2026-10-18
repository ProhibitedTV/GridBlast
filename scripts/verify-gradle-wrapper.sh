#!/bin/sh
# These digests are independently listed by Gradle at
# https://gradle.org/release-checksums/ (Gradle 9.8.1).
# Check both the locally committed wrapper and the download checksum before
# running build code or handling release-signing secrets.
set -eu
cd "$(dirname "$0")/.."

expected_wrapper="3b8a25775a69158b5ad2b1d17a80a88dc7b40a352a73eb27d06c612a7ce68e98"
expected_dist="dce76f55f8e251a3a1f130eb120f30b3d271de2b76c9b0729d316b5a1b6dc01f"
actual_wrapper="$(sha256sum gradle/wrapper/gradle-wrapper.jar | cut -d ' ' -f 1)"
if [ "$actual_wrapper" != "$expected_wrapper" ]; then
  echo "Untrusted gradle-wrapper.jar: SHA-256 mismatch" >&2
  exit 1
fi
if ! grep -Fxq 'distributionUrl=https\://services.gradle.org/distributions/gradle-9.8.1-bin.zip' gradle/wrapper/gradle-wrapper.properties; then
  echo "Unexpected Gradle distribution URL" >&2
  exit 1
fi
if ! grep -Fxq "distributionSha256Sum=$expected_dist" gradle/wrapper/gradle-wrapper.properties; then
  echo "Untrusted Gradle distribution checksum" >&2
  exit 1
fi
echo "Verified official Gradle 9.8.1 wrapper JAR and pinned distribution SHA-256"
