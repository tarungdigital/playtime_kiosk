# Playtime Kiosk

A tiny Android TV app that turns a TV into a dedicated display for
[Playtime](https://playtime.tarung.space/d/lounge). It opens the Playtime page
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

### Without adb

If Wireless debugging won't turn on, download `playtime.apk` on the TV and
install it. Then turn on auto-start by hand:

1. Open **Playtime** once from the Apps row.
2. Go to **Settings → System → Accessibility → Playtime**, switch it on, and
   accept the warning. The kiosk opens straight away.

If the switch is greyed out with a *Restricted setting* message, go to
**Settings → Apps → Playtime**, choose **Allow restricted settings**, and try
again.

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
[`src/playtime/app/MainActivity.java`](src/playtime/app/MainActivity.java),
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

Without adb, the app's accessibility service does the same job. Android lets an
app with a switched-on accessibility service open screens from the background,
and the service opens the kiosk as soon as it starts, which happens at boot. It
declares no event types and no window access, so it can't read the screen or
key presses.

## Notes

- **Wireless debugging turns off after a reboot** on this TV, and sometimes on
  its own after a while. You only need it to
  install updates, not for the kiosk to run. The pairing is remembered, so just
  switch it back on; no new code needed.
- **Don't force-stop the app** from the TV's app settings. Android won't start a
  force-stopped app at boot until it's opened by hand once.
- The package ID is `playtime.app`, matching the **Playtime** app name.
  Devices with the old package ID (`space.tarung.lounge`) will treat this as a
  separate app; uninstall the old app after installing this one if desired.

## Project layout

```
AndroidManifest.xml                          app name, permissions, boot receiver, auto-start service
src/playtime/app/MainActivity.java           fullscreen WebView, retry, keep-awake, station settings
src/playtime/app/BootReceiver.java           opens MainActivity after boot
src/playtime/app/AutoStartService.java       accessibility service for auto-start without adb
res/xml/autostart.xml                        accessibility service config (no events, no window access)
build.sh                                     build (and optionally install) without Gradle
```
