#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
sh scripts/build-android.sh assembleDebugAndroidTest "$@"
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w com.tirerun.game.test/com.tirerun.game.VisualCheck
mkdir -p build/screenshots/day-night
for name in day sunset night sunrise stride-sheet; do
    adb exec-out run-as com.tirerun.game cat "files/visual-check/$name.png" > "build/screenshots/day-night/$name.png"
done
