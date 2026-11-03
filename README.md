# Playtime Kiosk

A tiny Android TV app that turns a TV into a dedicated display for
[Playtime](https://playtime.tarung.space/d/lounge). It opens the lounge page
fullscreen, keeps it there, and comes back on its own after a reboot.

Built for a Mi TV (Google TV, Android 14), but it works on any Android TV or
Android device running Android 5.0+ with the system WebView.

## What it does

- Shows `https://playtime.tarung.space/d/{station}` (default `lounge`) in a
  fullscreen WebView with no browser UI.
- Starts automatically when the TV boots, and comes back to the front when the
  TV wakes from standby.
- Keeps the screen on while it's showing, so the screensaver and screen-off
  timer don't kick in.
- If the network isn't up yet (common right after boot), shows a dark screen and
  retries every 10 seconds.
- Back does nothing on the page. Press **Home** on the remote to leave the kiosk.
- Press **OK** on the remote 5 times quickly (or tap the screen 5 times) to open
  settings and switch to another station.

No browser needs to be installed on the TV.

## Requirements

On your computer:

- Android SDK with `build-tools/35.0.0` and `platforms/android-34`
  (set `ANDROID_HOME`, or it defaults to `~/Library/Android/sdk`)
- JDK 17 (`javac` on `PATH`)
- `adb`
- A debug keystore at `~/.android/debug.keystore` (Android Studio creates one)

No Gradle; `build.sh` calls the SDK tools directly.

## Build

```bash
./build.sh
```

This produces `playtime.apk`.

## Install on a TV

1. On the TV, enable **Developer options** (Settings → System → About → click
   *Android TV OS build* 7 times), then turn on **Wireless debugging**
   (Settings → System → Developer options).
2. Pair once. In *Wireless debugging* choose **Pair device with pairing code**,
   then on your computer:

   ```bash
   adb pair <tv-ip>:<pairing-port> <6-digit-code>
   ```

   The pairing port is the one shown in the pairing dialog, not the one on the
   main Wireless debugging screen. `adb mdns services` lists both.
3. Connect, using the port on the main Wireless debugging screen:

   ```bash
   adb connect <tv-ip>:<port>
   ```

4. Build, install, grant auto-start, and launch:

   ```bash
   ./build.sh install
   ```

   If more than one device is connected, set `ANDROID_SERIAL=<tv-ip>:<port>` first.

## Change the station

The app shows `https://playtime.tarung.space/d/{station}`, starting with `lounge`.

1. Press **OK** on the remote 5 times within 3 seconds (or tap 5 times on a
   touchscreen). A **Station** dialog opens.
2. Type the station name, for example `lobby`. Pasting `/d/lobby` or the full
   link also works; only the last part is kept.
3. Choose **Save**. The page reloads, and the station is remembered across
   reboots. **Cancel** or Back leaves it unchanged.

To change the site itself, edit `BASE_URL` (and `DEFAULT_STATION` for fresh
installs) in
[`src/space/tarung/lounge/MainActivity.java`](src/space/tarung/lounge/MainActivity.java),
then run `./build.sh install` again. Installing over the existing app keeps its
permissions and the saved station.

## How auto-start works

Android 10+ blocks apps from opening a screen from the background, which is what
a boot receiver needs to do. The app gets around this with the *Display over
other apps* permission (`SYSTEM_ALERT_WINDOW`), which `build.sh install` grants
over adb in two ways:

- `pm grant`: the permission itself. This survives reboots.
- `appops set ... allow`: the app-op. Google TV resets this one on reboot, which
  is why the `pm grant` is needed.

## Notes

- **Wireless debugging turns off after a reboot** on this TV, and sometimes on
  its own after a while. You only need it to
  install updates, not for the kiosk to run. The pairing is remembered, so just
  switch it back on; no new code needed.
- **Don't force-stop the app** from the TV's app settings. Android won't start a
  force-stopped app at boot until it's opened by hand once.
- The package ID is `space.tarung.lounge` (the app's first name). The TV shows
  it as **Playtime**. Changing the ID would install it as a separate app.

## Project layout

```
AndroidManifest.xml                          app name, permissions, boot receiver
src/space/tarung/lounge/MainActivity.java    fullscreen WebView, retry, keep-awake, station settings
src/space/tarung/lounge/BootReceiver.java    opens MainActivity after boot
build.sh                                     build (and optionally install) without Gradle
```
