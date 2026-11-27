#!/bin/bash
# Manda ai tester una nuova build solo se su main ci sono modifiche dall'ultimo invio
# all'app (con la loro voce nel CHANGELOG). Strumenti, docs e README non contano. Lo lancia ogni notte launchd sul Mac
# oppure systemd su omero (strumenti/omero/); a mano:
#   strumenti/pubblica-se-serve.sh           pubblica se serve
#   strumenti/pubblica-se-serve.sh --forza   pubblica comunque (stesse note dell'ultima voce)
set -uo pipefail
cd "$(dirname "$0")/.."
REPO=$(basename "$PWD")
# Registro: ~/Library/Logs/Argo sul Mac, ~/.local/state/argo/log su Linux (omero)
if [ -d "$HOME/Library" ]; then LOGDIR="$HOME/Library/Logs/Argo"; else LOGDIR="$HOME/.local/state/argo/log"; fi
LOG="$LOGDIR/$REPO.log"
mkdir -p "$LOGDIR"
exec > >(tee -a "$LOG") 2>&1
# Avvisi: notifica di macOS sul Mac; su Linux una riga in avvisi.log (la legge il cruscotto di omero)
avvisa() {
  if command -v osascript >/dev/null 2>&1; then
    osascript -e "display notification \"$2\" with title \"Argo · $REPO\" subtitle \"$1\"" >/dev/null 2>&1 || true
  else
    echo "$(date '+%Y-%m-%d %H:%M') · $REPO · $1 · $2" >> "$LOGDIR/avvisi.log"
  fi
}
echo "=== $(date '+%Y-%m-%d %H:%M') · $REPO"

if [ -f strumenti/pubblica-testflight.sh ]; then
  PUBBLICA=strumenti/pubblica-testflight.sh; DOVE="TestFlight"; ESCLUSI=(':!VERSIONE' ':!project.yml' ':!strumenti' ':!docs' ':!README.md' ':!THIRD_PARTY_LICENSES.md')
else
  PUBBLICA=strumenti/pubblica-play.sh; DOVE="test interno di Google Play"; ESCLUSI=(':!VERSIONE' ':!strumenti' ':!docs' ':!README.md' ':!THIRD_PARTY_LICENSES.md')
fi

BLOCCO="/tmp/argo-$REPO.lock"
mkdir "$BLOCCO" 2>/dev/null || { echo "Una pubblicazione è già in corso."; exit 0; }
trap 'rmdir "$BLOCCO"' EXIT

if ! git pull --autostash --quiet; then
  echo "git pull non riuscito (rete, conflitto o permessi sulla cartella)."
  avvisa "Pubblicazione non partita" "git pull non riuscito: vedi $LOG"
  exit 1
fi

ULTIMO=$(git log -1 --format=%H --grep="inviata a")
if [ "${1:-}" != "--forza" ] && [ -n "$ULTIMO" ]; then
  CAMBIATI=$(git diff --name-only "$ULTIMO" HEAD -- . "${ESCLUSI[@]}")
  if [ -z "$CAMBIATI" ]; then
    echo "Niente di nuovo dall'ultimo invio ($(git log -1 --format=%s "$ULTIMO"))."
    exit 0
  fi
  if ! grep -qx "CHANGELOG.md" <<<"$CAMBIATI"; then
    echo "Ci sono modifiche ma nessuna voce nuova nel CHANGELOG: non pubblico."
    avvisa "In attesa" "Modifiche senza voce nel CHANGELOG"
    exit 0
  fi
  echo "Novità dall'ultimo invio:"; echo "$CAMBIATI" | sed 's/^/  /'
fi

if strumenti/nuova-build.sh && "$PUBBLICA" \
   && git commit -qam "Build $(cat VERSIONE) inviata a $DOVE" && git push -q; then
  echo "✅ Build $(cat VERSIONE) inviata a $DOVE."
  avvisa "Inviata ai tester" "Build $(cat VERSIONE) su $DOVE"
else
  echo "❌ Pubblicazione fallita."
  avvisa "Pubblicazione fallita" "Dettagli in $LOG"
  exit 1
fi
