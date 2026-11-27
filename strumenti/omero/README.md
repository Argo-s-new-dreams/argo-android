# Invio notturno di Argo Android su omero

Su omero (Ubuntu Server) l'invio al test interno di Google Play gira con systemd utente, al posto di launchd sul Mac:
ogni notte alle **03:15** `argo-android-notte.timer` lancia `strumenti/pubblica-se-serve.sh`, che pubblica solo se dall'ultimo
invio ci sono modifiche all'app con la loro voce nel CHANGELOG (come sul Mac).

| Cosa | Dove su omero |
|---|---|
| Repo | `~/Sviluppo/argo-android` (clonato con una deploy key con scrittura: lo script fa commit e push di `VERSIONE`) |
| Chiavi | `~/.argo-chiavi/` (`play-service-account.json`, `argo-upload.jks`), mai nel repo |
| Firma | `~/Sviluppo/argo-android/keystore.properties` con `storeFile` che punta a `~/.argo-chiavi/argo-upload.jks` |
| SDK Android | `~/android-sdk` (piattaforma e build-tools 37), `local.properties` con `sdk.dir` |
| Java | `openjdk-21-jdk-headless` |
| Log | `~/.local/state/argo/log/argo-android.log`; avvisi in `avvisi.log` nella stessa cartella |

Installazione delle unità:
```
cp ~/Sviluppo/argo-android/strumenti/omero/argo-android-notte.* ~/.config/systemd/user/
systemctl --user daemon-reload && systemctl --user enable --now argo-android-notte.timer
```
Prova a mano: `systemctl --user start argo-android-notte.service`, poi `tail -n 30 ~/.local/state/argo/log/argo-android.log`.

Sul Mac l'invio notturno di Android va tolto, per non pubblicare due volte: `strumenti/installa-notte.sh --togli`.
