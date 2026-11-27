#!/bin/bash
# Crea l'AAB firmato e lo carica su Google Play nel test interno, con note dal CHANGELOG.
# Uso: strumenti/pubblica-play.sh
set -euo pipefail
cd "$(dirname "$0")/.."
# Serve Java 25: la chiave di caricamento ha caratteri accentati nella password e Java 21 non la apre.
# Sul Mac il Java di Android Studio (25); su omero openjdk-25.
STUDIO_JBR="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
if [ -d "$STUDIO_JBR" ]; then export JAVA_HOME="$STUDIO_JBR"
elif [ -d /usr/lib/jvm/java-25-openjdk-amd64 ]; then export JAVA_HOME=/usr/lib/jvm/java-25-openjdk-amd64; fi
test -f keystore.properties || { echo "Manca keystore.properties: senza chiave di caricamento Play rifiuta l'AAB."; exit 1; }
./gradlew --quiet bundleRelease
uv run --quiet --with google-api-python-client --with google-auth strumenti/play.py carica internal
