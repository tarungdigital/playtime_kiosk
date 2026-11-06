#!/bin/zsh
# Builds playtime.apk without Gradle, using the Android SDK's command-line build tools.
# Usage: ./build.sh            -> build only
#        ./build.sh install    -> build, install on $ANDROID_SERIAL (or the only connected device), grant auto-start, launch
set -euo pipefail
cd "${0:A:h}"

SDK="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
BT="$SDK/build-tools/35.0.0"
JAR="$SDK/platforms/android-34/android.jar"
PKG=playtime.app

rm -rf build && mkdir -p build/classes build/dex

"$BT/aapt2" compile --dir res -o build/res.zip
"$BT/aapt2" link -o build/unsigned.apk -I "$JAR" --manifest AndroidManifest.xml \
  --min-sdk-version 21 --target-sdk-version 34 build/res.zip
javac -source 8 -target 8 -bootclasspath "$JAR" -Xlint:-options -d build/classes $(find src -name '*.java')
"$BT/d8" --release --min-api 21 --lib "$JAR" --output build/dex $(find build/classes -name '*.class')
(cd build/dex && zip -q -u ../unsigned.apk classes.dex)
"$BT/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
"$BT/apksigner" sign --ks "$HOME/.android/debug.keystore" --ks-pass pass:android \
  --key-pass pass:android --ks-key-alias androiddebugkey --out playtime.apk build/aligned.apk
echo "Built $(pwd)/playtime.apk"

if [[ "${1:-}" == install ]]; then
  adb install -r playtime.apk
  # Allow the boot receiver to open the activity from the background (Android 10+).
  # The TV resets the appop on reboot, so also grant the permission itself, which persists.
  adb shell pm grant $PKG android.permission.SYSTEM_ALERT_WINDOW
  adb shell appops set $PKG SYSTEM_ALERT_WINDOW allow
  adb shell am start -n $PKG/.MainActivity
fi
