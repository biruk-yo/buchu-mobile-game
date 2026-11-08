#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
sh scripts/build-android.sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.tirerun.game/.MainActivity
