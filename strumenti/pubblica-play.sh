#!/bin/bash
# Crea l'AAB firmato e lo carica su Google Play nel test interno, con note dal CHANGELOG.
# Uso: strumenti/pubblica-play.sh
set -euo pipefail
cd "$(dirname "$0")/.."
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
test -f keystore.properties || { echo "Manca keystore.properties: senza chiave di caricamento Play rifiuta l'AAB."; exit 1; }
./gradlew --quiet bundleRelease
uv run --quiet --with google-api-python-client --with google-auth strumenti/play.py carica internal
