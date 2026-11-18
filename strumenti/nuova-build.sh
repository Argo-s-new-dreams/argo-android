#!/usr/bin/env bash
# Nuovo numero di build (AAAAMMGG.HHMM, ora di Roma) nel file VERSIONE.
# Google Play ricava da qui il versionCode (minuti dal 1/1/2026): deve crescere a ogni caricamento.
set -euo pipefail
cd "$(dirname "$0")/.."
build="$(TZ=Europe/Rome date +%Y%m%d.%H%M)"
echo "$build" > VERSIONE
echo "Build $build · ricordati il CHANGELOG"
