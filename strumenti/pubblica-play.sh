#!/bin/bash
# Crea l'AAB firmato e lo carica su Google Play nel test interno, con note dal CHANGELOG.
# Uso: strumenti/pubblica-play.sh
set -euo pipefail
cd "$(dirname "$0")/.."
# Sul Mac il Java di Android Studio; su omero quello di sistema (openjdk-21)
STUDIO_JBR="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
if [ -d "$STUDIO_JBR" ]; then export JAVA_HOME="$STUDIO_JBR"; fi
test -f keystore.properties || { echo "Manca keystore.properties: senza chiave di caricamento Play rifiuta l'AAB."; exit 1; }
./gradlew --quiet bundleRelease
uv run --quiet --with google-api-python-client --with google-auth strumenti/play.py carica internal
