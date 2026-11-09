#!/usr/bin/env bash
set -euo pipefail
mkdir -p device-results
adb install test-apks/github/debug/app-github-debug.apk
adb install test-apks/androidTest/github/debug/app-github-debug-androidTest.apk
adb shell am instrument -w -r de.haberland.meitraumgeschichten.test/androidx.test.runner.AndroidJUnitRunner | tee device-results/instrumentation.txt
if ! grep -Eq 'OK \([0-9]+ tests?\)' device-results/instrumentation.txt; then
  adb logcat -d > device-results/logcat.txt
  exit 1
fi
adb shell am start -n de.haberland.meitraumgeschichten/.MainActivity
adb shell screencap -p /sdcard/library.png
adb pull /sdcard/library.png device-results/library.png
