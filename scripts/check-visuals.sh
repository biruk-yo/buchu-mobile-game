#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
sh scripts/build-android.sh assembleDebugAndroidTest "$@"
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
tire_proof=$(adb shell am instrument -w com.tirerun.game.test/com.tirerun.game.VisualCheck)
printf '%s\n' "$tire_proof"
case "$tire_proof" in
    *"Visual proof: village art, surprises, audio lifecycle, hills, rewards and persistence passed."*) ;;
    *) exit 1 ;;
esac
mkdir -p build/screenshots/day-night
for name in day sunset night sunrise stride-sheet woodland compounds grove hill-climb hill-crest hill-descent valley hill-return obstacle-passing start-screen start-screen-night start-screen-compact back-to-village app-icon downhill-boost close-call perfect-hop mission-complete rewards-result next-mission surprise-chickens surprise-basket surprise-cart surprise-warning surprise-night village-homes; do
    adb exec-out run-as com.tirerun.game cat "files/visual-check/$name.png" > "build/screenshots/day-night/$name.png"
done
