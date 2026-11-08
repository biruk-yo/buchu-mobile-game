#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
mkdir -p build/engine-tests
javac -d build/engine-tests app/src/main/java/com/tirerun/game/GameEngine.java app/src/main/java/com/tirerun/game/TrailCamera.java app/src/main/java/com/tirerun/game/RunCycle.java app/src/main/java/com/tirerun/game/DayNightCycle.java app/src/test/java/com/tirerun/game/GameEngineTest.java app/src/test/java/com/tirerun/game/TrailCameraTest.java app/src/test/java/com/tirerun/game/RunCycleTest.java app/src/test/java/com/tirerun/game/DayNightCycleTest.java
java -cp build/engine-tests com.tirerun.game.GameEngineTest
java -cp build/engine-tests com.tirerun.game.TrailCameraTest
java -cp build/engine-tests com.tirerun.game.RunCycleTest
java -cp build/engine-tests com.tirerun.game.DayNightCycleTest
