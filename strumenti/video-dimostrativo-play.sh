#!/bin/bash
# Registra sull'emulatore il video dimostrativo dei servizi in primo piano chiesto da Google Play
# (microphone, mediaProcessing, dataSync): registrazione di un incontro, Stop, avviso
# «Argo sta elaborando l'incontro» con l'app in background, incontro pronto.
# Uso: emulatore acceso con Argo Lab installato (./gradlew installLab), poi
#   strumenti/video-dimostrativo-play.sh [secondi di registrazione, default 20]
# Il video finisce sulla Scrivania: argo-servizi-primo-piano.mp4
set -euo pipefail

ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"
PACCHETTO="blog.argosnewdreams.argo.lab"
SECONDI="${1:-20}"
VIDEO_TELEFONO="/sdcard/argo-servizi-primo-piano.mp4"
VIDEO_MAC="$HOME/Desktop/argo-servizi-primo-piano.mp4"

"$ADB" wait-for-device

centro() {
    "$ADB" shell uiautomator dump /sdcard/argo-ui.xml >/dev/null 2>&1 || true
    "$ADB" exec-out cat /sdcard/argo-ui.xml | python3 -c '
import re, sys
xml, cerca = sys.stdin.read(), sys.argv[1]
for nodo in re.findall(r"<node [^>]*>", xml):
    testo = re.search(r" text=\"([^\"]*)\"", nodo).group(1)
    descr = re.search(r" content-desc=\"([^\"]*)\"", nodo).group(1)
    if cerca in testo or cerca in descr:
        x1, y1, x2, y2 = map(int, re.search(r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", nodo).groups())
        print((x1 + x2) // 2, (y1 + y2) // 2)
        break
' "$1"
}

tocca() {
    local punto=""
    for _ in 1 2 3 4 5 6 7 8 9 10; do
        punto=$(centro "$1")
        [ -n "$punto" ] && break
        sleep 1
    done
    if [ -z "$punto" ]; then
        [ "${2:-}" = "facoltativo" ] && return 0
        echo "❌ Non trovo «$1» sullo schermo dell'emulatore."
        exit 1
    fi
    "$ADB" shell input tap $punto
    echo "· «$1»"
}

echo "Preparo l'emulatore…"
"$ADB" shell input keyevent KEYCODE_WAKEUP
"$ADB" shell am force-stop "$PACCHETTO"
"$ADB" shell monkey -p "$PACCHETTO" 1 >/dev/null 2>&1
sleep 3
tocca "Chiudi" facoltativo
sleep 1

echo "Registro lo schermo (al massimo 3 minuti)…"
"$ADB" shell rm -f "$VIDEO_TELEFONO"
"$ADB" shell screenrecord --bit-rate 8000000 --time-limit 180 "$VIDEO_TELEFONO" &
REGISTRATORE=$!
sleep 3

tocca "Registra"
sleep 2
tocca "Ho informato i partecipanti"
sleep 4

echo "Avviso del microfono nella tendina…"
"$ADB" shell cmd statusbar expand-notifications
sleep 4
"$ADB" shell cmd statusbar collapse
sleep "$SECONDI"

tocca "Ferma e salva l'incontro"
sleep 2

echo "App in background durante l'elaborazione…"
"$ADB" shell input keyevent KEYCODE_HOME
sleep 2
"$ADB" shell cmd statusbar expand-notifications
sleep 6
"$ADB" shell cmd statusbar collapse
sleep 1

echo "Schermo spento e riacceso…"
"$ADB" shell input keyevent KEYCODE_SLEEP
sleep 3
"$ADB" shell input keyevent KEYCODE_WAKEUP
sleep 2
"$ADB" shell wm dismiss-keyguard >/dev/null 2>&1 || true
sleep 1

echo "Torno in Argo…"
"$ADB" shell monkey -p "$PACCHETTO" 1 >/dev/null 2>&1
sleep 8

echo "Chiudo il video…"
"$ADB" shell pkill -INT screenrecord || true
wait "$REGISTRATORE" 2>/dev/null || true
sleep 2
"$ADB" pull "$VIDEO_TELEFONO" "$VIDEO_MAC" >/dev/null
"$ADB" shell rm -f "$VIDEO_TELEFONO" /sdcard/argo-ui.xml

if "$ADB" shell pidof "$PACCHETTO" >/dev/null; then
    echo "✅ Video salvato: $VIDEO_MAC (Argo è rimasto aperto, nessuna chiusura)"
else
    echo "⚠️ Video salvato in $VIDEO_MAC, ma Argo non risulta in esecuzione: guardalo prima di caricarlo."
fi
open -R "$VIDEO_MAC"
