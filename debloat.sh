#!/bin/zsh
# Turns off apps a kiosk TV doesn't need, so they stop running in the background.
# Uses `pm disable-user`: the apps stay on the TV and can be turned back on.
# Usage: ./debloat.sh            -> disable everything below on $ANDROID_SERIAL (or the only connected device)
#        ./debloat.sh restore    -> turn it all back on
# Apps the device doesn't have are skipped, so one list covers every TV.
# Kept on purpose: WebView (renders the kiosk), Bluetooth and remote pairing, keyboard, Settings,
# Google Play services, the launcher, and Xiaomi's TV/remote-key services.
set -uo pipefail

PKGS=(
  # Streaming and content apps
  com.netflix.ninja
  com.netflix.tokenmanager
  com.amazon.amazonvideo.livingroom
  com.google.android.youtube.tv
  com.google.android.youtube.tvmusic
  com.google.android.play.games
  com.iqiyi.i18n.tv
  com.wbd.stream
  in.startv.hotstar.dplus.tv
  com.mitv.videoplayer
  com.mitv.tvhome.mitvplus
  com.mitv.tvhome.michannel
  com.mitv.tvhome.oemtab
  com.xm.webcontent
  # Google extras: Assistant and voice search, Chromecast, Ambient mode, feedback
  com.google.android.katniss
  com.google.android.apps.mediashell
  com.google.android.apps.tv.dreamx
  com.google.android.feedback
  com.android.tv.feedbackconsent
  # Xiaomi analytics, phone casting and Miracast
  com.miui.tv.analytics
  com.xiaomi.statistic
  com.xiaomi.mi_connect_service
  com.xiaomi.mitv.smartshare
  com.xiaomi.wfdsinkhelperservice
  # Generic HiSilicon boxes (sold as "onn STICK"): streaming apps
  com.netflix.mediaclient
  com.amazon.avod.thirdpartyclient
  org.smarttube.stable
  com.github.tvbox.osc
  com.android.mgstv
  appnovatica.stbp
  net.emotn.screencast
  com.android.chrome
  com.hisilicon.android.videoplayer
  com.google.android.tvrecommendations
  com.google.android.backdrop
  # Generic HiSilicon boxes: vendor apps that can install apps silently, start at boot and go online
  com.master.accessibility
  com.master.ttyunos.superuser
  com.charon.rocketfly
  com.dyos.factorytest
  com.eusfu.appstore
)

if [[ "${1:-}" == restore ]]; then cmd=enable; else cmd=disable-user; fi
installed=$(adb shell pm list packages | tr -d '\r')
for p in $PKGS; do
  [[ $installed == *"package:$p"$'\n'* || $installed == *"package:$p" ]] || continue
  printf '%-40s ' $p
  adb shell pm $cmd --user 0 $p 2>&1 | tr -d '\r' | tail -1
done
