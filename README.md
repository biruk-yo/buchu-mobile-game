# ቡቹ (Buchu)

A playable Android endless-runner prototype: an Ethiopian boy rolls an upright tire with both hands through a wooded village while a neighbor’s dog follows. A perspective chase camera sits behind him, follows lane changes smoothly, and turns with a continuous winding route. Original Canvas illustrations include highland silhouettes, tukul compounds, tall trees, undergrowth, wooden gates and tibeb-inspired shirt trim. This uses a 3D route projection with stylized 2D characters and scenery, rather than full 3D character models.

## Play

- Swipe left/right anywhere on the play area to change lanes. There are no on-screen movement buttons.
- The camera follows the trail’s left and right bends automatically. Steering changes lanes relative to the path; coins and hazards follow the same route geometry.
- Swipe up to jump over rocks and logs.
- Mud traps the tire and ends the run, including during a hop. Steer around it.
- Mud uses a bundled wet-soil texture, ground-perspective mapping, subtle ripples and an impact splash before the result screen.
- The boy uses a smooth rear-view stride with a capped cadence (1.35–1.85 cycles/second), independent of tire rotation. Both hands stay anchored to the tire. The dog uses coordinated diagonal paw pairs and a low four-legged posture.
- The first obstacle hit brings the dog closer. A second hit within eight seconds ends the run. Eight seconds without a hit clear the first strike.
- A moving sun brings sunset, night with a moon and stars, and sunrise over a 90-second cycle. World lighting changes while controls remain readable. The world clock advances in play and on the title screen, freezes when paused/backgrounded, and continues across retries.
- Collect coins; distance and coin totals are saved on game over or when returning to the village through the pause screen.
- Tap pause at the upper right. Backgrounding the app automatically pauses the run.
- Emulator keyboard: arrows or A/D to steer; Up, W or Space to hop; P/Escape to pause; Enter to start/resume.

## Build and test

Open this folder in Android Studio. Use Java 17 or newer (Android Studio’s bundled Java works) and Android SDK 36. Select an emulator or a USB-debugging-enabled phone, then Run.

This Mac already has a `Pixel_7` virtual phone. Start it from Android Studio’s Device Manager, or run `emulator -avd Pixel_7` in a terminal before using the install script.

```sh
sh scripts/build-android.sh
sh scripts/test-engine.sh
sh scripts/run-android.sh
```

The first Gradle build may download build tools/dependencies. Gameplay runs entirely offline and requires no permissions, account, advertising SDK or asset downloads. The generated mud texture is bundled in the APK; its [source prompt and notes](app/src/main/assets/README.md) are included.

The build script reuses the official SDK 36 resource compiler when installed, avoiding a redundant Maven download on this Mac. A standard `./gradlew assembleDebug` also works when Maven downloads are available. After dependencies have been cached, use `sh scripts/build-android.sh --offline` to build without the network.

APK: `app/build/outputs/apk/debug/app-debug.apk`. Install this APK on an Android 8.0+ device; it is a debug build for testing, not a Play Store release.

The local `local.properties` points to this Mac’s Android SDK and is excluded from version control. On another computer, Android Studio can recreate it.

## Project

- `GameEngine.java`: deterministic, platform-independent game rules and world generation.
- `GameView.java`: original vector-style artwork, animation, gestures, HUD and menus.
- `TrailCamera.java`: continuous curved route, perspective projection, smooth rear camera following and district names.
- `RunCycle.java`: smoothly interpolated rear-view poses, bounded cadence and fixed hand contacts on the tire.
- `MainActivity.java`: Android lifecycle, portrait display and immersive mode.
- `GameEngineTest.java`: executable checks for rewards, hazards, recovery, lane limits, jumping, pause/resume, restart and bounded world generation.
- `TrailCameraTest.java`: checks both-direction bends, lane visibility/order, approaching-hazard sightlines, perspective depth, lane following and camera pause.
- `RunCycleTest.java`: verifies bounded cadence, smooth loop/foot motion, leg separation, planted/lifted feet and fixed tire contact.
- `DayNightCycle.java` / `DayNightCycleTest.java`: continuous sun/moon positions, sunset/night/sunrise colors and visibility limits.
- `VisualCheck.java` (Android test APK only): renders four lighting states and a 16-frame stride proof sheet from the real Android view.

Current scope: an endless winding wooded-village route with denser compound sections, procedural obstacles, sound cues and local scores. Bends follow automatically; there are no player-selected junctions yet. No shop, missions, character selection, cloud saves or full 3D models yet. Cultural details are a starting art direction for review rather than a representation of every Ethiopian community.
