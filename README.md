# ቡቹ (Buchu)

A playable Android endless-runner prototype: an Ethiopian boy rolls an upright tire with both hands through a wooded village while a neighbor’s dog follows. A perspective chase camera sits behind him, follows lane changes smoothly, and turns with a continuous winding route. Original Canvas illustrations include highland silhouettes, tukul compounds, tall trees, undergrowth, wooden gates and tibeb-inspired shirt trim. This uses a 3D route projection with stylized 2D characters and scenery, rather than full 3D character models.

## Play

- A gold-and-green rolling-tire adaptive icon and matching ቡቹ title screen introduce the village, with a large start button, saved scores and swipe hints. The layout adapts to shorter portrait screens.
- The title and “Back to village” screens use the same winding route, scenery and characters as gameplay. A gentle camera tour starts from the last run's location with the current day/night lighting; the preview does not change distance or coins.
- Swipe left/right anywhere on the play area to change lanes. There are no on-screen movement buttons.
- The camera follows the trail’s left and right bends automatically. Steering changes lanes relative to the path; coins and hazards follow the same route geometry.
- The street climbs rolling hills and dips into grassy valleys. The chase camera follows the slope, scenery sits on the terrain, and hill crests hide distant scenery while keeping nearby hazards visible.
- The dog has a contrasting charcoal-and-white coat, white paws and a blue collar so it stays visible against the dirt road, including at night.
- Homes mix round thatched tukuls and plaster cottages with corrugated roofs, shaded side walls, stone foundations, recessed doors, shutters, roof vents, warm interiors, flower pots and water barrels. Chimney smoke drifts, vendors sort fruit, neighbors wave or carry baskets, laundry sways, and yard chickens peck beside the path.
- Roadside courtyards include striped produce stalls, neighbors, laundry lines, clay pots, woven baskets, woodpiles and wells. Garden beds, hedges, stone verges and woven fences fill the spaces between compounds, with all scenery placed outside the running lanes.
- Swipe up to jump over rocks and logs.
- Runs start at a brisk 18 metres/second and build to a base pace of 32 metres/second over roughly the first minute. Slow frames catch up in small simulation steps (up to 250 ms per frame) to keep phone gameplay at its intended pace.
- Downhill slopes smoothly add up to 6 metres/second (38 maximum), with a boost badge, wind streaks and dust. The extra speed fades as the slope levels out.
- Late dodges earn 25 skill points; clearing a rock or log near the top of a hop earns 40. Score combines metres travelled and skill points, with a saved best score and a breakdown at the end of each run. Safe early lane changes do not count as near misses.
- Each run has one mission: collect coins, hop obstacles without stumbling, or reach a distance goal. Completion immediately banks 25 coins and unlocks the next mission for the next run. Initial goals are 12 coins, 3 clean hops and 350 metres; later goals rise to a maximum of 24 coins, 6 clean hops and 650 metres. Unfinished progress resets on retry, and a stumble resets an unfinished hop streak.
- Village surprises rotate through crossing chickens, a rolling basket and an entering produce cart. A sound cue, a named-lane banner and a gold lane marker warn before contact; even at maximum speed, the warning lasts at least 2.5 seconds. Chickens and baskets can be hopped or avoided; carts require a lane change. Reserved gaps around each surprise keep escape lanes clear.
- An original 112 BPM instrumental loop combines a bouncy pentatonic melody, plucked tones, soft bass and hand-percussion sounds. Coins, hops, skill rewards, missions, surprise warnings and stumbles have distinct musical cues. The title/pause sound toggle controls both music and effects; music pauses with gameplay, stops in the background, and respects audio focus. All audio is bundled for offline play.
- Rocks, logs and mud remain visible after passing the boy and leave through the screen edge; collision checks still happen only once as they reach him.
- Mud traps the tire and ends the run, including during a hop. Steer around it.
- Mud uses a bundled wet-soil texture, ground-perspective mapping, subtle ripples and an impact splash before the result screen.
- The boy uses a smooth rear-view stride with a capped cadence (1.35–1.85 cycles/second), independent of tire rotation. The tire rolls low on the trail ahead of him. A slight rear-quarter projection reveals the spinning rim beside his silhouette while retaining overlap with his body. Small coordinated hand strokes and elbow flexion show him pushing; both hands stay on the rubber, and a tight contact shadow grounds the wheel. The dog uses coordinated diagonal paw pairs and a low four-legged posture.
- The first obstacle hit brings the dog closer. A second hit within eight seconds ends the run. Eight seconds without a hit clear the first strike.
- A moving sun brings sunset, night with a moon and stars, and sunrise over a ten-minute cycle (about five minutes each for day and night, including gradual transitions). At night, warm interiors glow through house windows and open doorways, spilling light onto the courtyards. World lighting changes while controls remain readable. The world clock advances in play and on the title screen, freezes when paused/backgrounded, and continues across retries.
- Collected coins disappear on pickup and reward once. Missed coins continue past the boy and leave through the bottom of the screen. Distance and coin totals are saved on game over or when returning to the village through the pause screen.
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
- `VillageArt.java` / `VillageHomes.java`: animated roadside life, detailed houses and moving village surprise illustrations.
- `GameAudio.java`: looping music, loaded sound effects, mute, audio focus and lifecycle.
- `scripts/compose-audio.py`: reproducible original music and sound-cue composition; writes the bundled WAV files under `app/src/main/res/raw/`.
- `TrailCamera.java`: continuous curved route, rolling elevation, raised valley banks, perspective projection, smooth rear camera following and district names.
- `RunCycle.java`: smoothly interpolated rear-view poses, bounded cadence, two-hand contact with the tire directly ahead.
- `MainActivity.java`: Android lifecycle, portrait display and immersive mode.
- `GameEngineTest.java`: executable checks for rewards, hazards, recovery, lane limits, jumping, pause/resume, restart and bounded world generation.
- `VillageSurprisesTest.java`: warning duration at maximum speed, announced-lane alignment, clear escape routes, cart versus hop behavior, pause and one-time collision.
- `RunRewardsTest.java`: verifies downhill speed bounds, late dodges, perfect hops, scoring, mission progression, pause and one-time rewards.
- `TrailCameraTest.java`: checks both-direction bends, lane visibility/order, approaching-hazard sightlines, perspective depth, hill sightlines, stable runner height, continuous elevation, lane following and camera pause.
- `RunCycleTest.java`: verifies bounded cadence, smooth loop/foot motion, leg separation, planted/lifted feet, tire alignment ahead of the boy and both hands’ tire contact.
- `DayNightCycle.java` / `DayNightCycleTest.java`: continuous sun/moon positions, sunset/night/sunrise colors and visibility limits.
- `VisualCheck.java` (Android test APK only): renders lighting states, village districts, hill climbs/crests/descents/valleys, a 16-frame stride proof sheet, obstacles and missed coins passing off-screen, the adaptive icon, and the title screen by day/night and at a compact size. It also checks audio loading/play/pause/mute/background behavior, renders all three surprise warnings and house details, and checks mission payouts, saved progression, score persistence, reward duplication and the start/retry/return touch targets. Proof runs restore the player’s saved records afterward. Run `sh scripts/check-visuals.sh --offline` with an emulator connected; images are saved under `build/screenshots/day-night/`.

Current scope: an endless winding wooded-village route with denser compound sections, procedural obstacles, downhill boosts, skill rewards, rotating run missions, sound cues and local scores. Bends follow automatically; there are no player-selected junctions yet. No shop, character selection, cloud saves or full 3D models yet. Cultural details are a starting art direction for review rather than a representation of every Ethiopian community.
