#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
# Reuse the installed official SDK compiler when available, avoiding a redundant download.
tire_sdk="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-$HOME/Library/Android/sdk}}"
tire_aapt="$tire_sdk/build-tools/36.0.0/aapt2"
if [ -x "$tire_aapt" ]; then
    ./gradlew assembleDebug "-Pandroid.aapt2FromMavenOverride=$tire_aapt" "$@"
else
    ./gradlew assembleDebug "$@"
fi
