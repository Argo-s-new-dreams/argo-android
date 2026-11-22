# Argo su Google Play · test interno

L'equivalente di TestFlight: fino a 100 tester per app, invitati per email, installano da Google Play e ricevono da soli gli aggiornamenti. Il test interno non passa dalla revisione di Google ed è disponibile subito (la prima volta con nome e scheda provvisori fino a 48 ore).

## Una volta sola

### 1. Account sviluppatore
[Play Console](https://play.google.com/console): registrazione (25 $ una tantum), verifica dell'identità. Valuta un account **organizzazione** intestato ad Argo's New Dreams invece che personale: gli account personali recenti, per passare in produzione, devono prima fare un test chiuso con almeno 12 tester per 14 giorni (il test interno no).

### 2. Chiave di caricamento
Google firma l'app per gli utenti (*Play App Signing*); tu firmi solo i caricamenti. Sul Mac, **fuori dal repository**:

```bash
mkdir -p ~/.argo-chiavi && cd ~/.argo-chiavi
keytool -genkeypair -v -keystore argo-upload.jks -alias argo-upload \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Argo's New Dreams, O=Argo's New Dreams, L=Cuneo, C=IT"
```

Poi nella cartella del progetto crea `keystore.properties` (è in `.gitignore`):

```properties
storeFile=/Users/<tu>/.argo-chiavi/argo-upload.jks
storePassword=…
keyAlias=argo-upload
keyPassword=…
```

> Conserva `argo-upload.jks` e le password in un posto sicuro (es. portachiavi/1Password). Se la perdi, la chiave di caricamento si può reimpostare dal supporto Play, ma serve tempo.

### 3. Crea l'app
*Crea app* › nome **Argo · Taccuino**, lingua italiano, App, Gratuita. Il pacchetto `blog.argosnewdreams.argo` si fissa al primo caricamento.

### 4. Schede obbligatorie anche per il test interno
- **Accesso all'app**: tutte le funzioni disponibili senza accesso.
- **Annunci**: nessun annuncio.
- **Classificazione dei contenuti**: questionario (utilità/produttività, nessun contenuto sensibile).
- **Pubblico di destinazione**: 18+.
- **Norme sulla privacy**: URL di una pagina del blog (es. `argosnewdreams.blog/argo-privacy`) con il testo della sezione Privacy del README.
- **Servizi in primo piano**: dichiarare il tipo *microfono* — «registrare un incontro su richiesta dell'utente, con notifica sempre visibile» — e, se richiesto, un breve video della registrazione.
- **Sicurezza dei dati**: vedi sotto.

## A ogni rilascio (automatico)

Dal Terminale del Mac, nella cartella del progetto, dopo aver aggiornato `CHANGELOG.md`:

```bash
strumenti/nuova-build.sh && strumenti/pubblica-play.sh
```

Lo script crea l'AAB firmato, lo carica nel **test interno** con le note di rilascio prese dalla prima voce del CHANGELOG (max 500 caratteri) e il file `mapping.txt`. Per portare la stessa versione al **test chiuso** (revisione di Google):

```bash
uv run --with google-api-python-client --with google-auth strumenti/play.py promuovi internal alpha
```

Serve una volta sola: account di servizio Google Cloud con *Google Play Android Developer API* abilitata, chiave JSON in `~/.argo-chiavi/play-service-account.json`, account invitato in Play Console › *Utenti e autorizzazioni* con il permesso di rilasciare nelle tracce di test.


### Di notte, da solo, o quando vuoi tu

`strumenti/pubblica-se-serve.sh` pubblica **solo se** su `main` ci sono modifiche dall'ultimo invio **e** una voce nuova nel `CHANGELOG.md` (altrimenti non fa nulla, o avvisa che manca la voce). Scrive il log in `~/Library/Logs/Argo/` e manda una notifica macOS.

Una volta sola, per programmarlo ogni notte (se il Mac dorme, parte al risveglio):

```bash
strumenti/installa-notte.sh 03:15
```

Quando vuoi tu: `strumenti/pubblica-se-serve.sh` (oppure `--forza` per ripubblicare comunque). Per togliere la programmazione: `strumenti/installa-notte.sh --togli`.

## A ogni rilascio (a mano, piano B)

1. `strumenti/nuova-build.sh` → nuovo `VERSIONE` (AAAAMMGG.HHMM, ora di Roma) e quindi nuovo `versionCode`.
2. Aggiorna `CHANGELOG.md` (e `versioneMarketing` in `app/build.gradle.kts` se cambia la versione).
3. `./gradlew bundleRelease` → `app/build/outputs/bundle/release/app-release.aab`.
4. Play Console › *Test e rilascio › Test › Test interno* › **Crea nuova release** › carica l'AAB › note di rilascio (dal CHANGELOG) › *Salva* › *Revisiona release* › **Avvia l'implementazione**.
5. Scheda **Tester**: crea un elenco email (account Google o Workspace) e copia il **link di partecipazione**; chi lo apre dal telefono accetta e installa da Play.

## Sicurezza dei dati · bozza

Da rivedere prima dell'invio: la responsabilità della dichiarazione è dello sviluppatore.

| Domanda | Risposta proposta | Perché |
|---|---|---|
| L'app raccoglie o condivide dati utente? | **Sì** | Le librerie ML Kit inviano a Google metriche di diagnostica |
| Dati raccolti | *Informazioni e prestazioni dell'app › Diagnostica* (raccolti, non condivisi; non facoltativi; scopo: analisi/diagnostica) | [Dichiarazione dati ML Kit](https://developers.google.com/ml-kit/android-data-disclosure): informazioni sul dispositivo e acceleratori ML, per diagnostica e analisi d'uso |
| Audio, foto, testo degli incontri | **Non raccolti**: restano sul telefono; l'invio al computer avviene solo per scelta dell'utente, tramite la condivisione di Android verso un'app che sceglie lui | [Termini ML Kit](https://developers.google.com/ml-kit/terms): input elaborati interamente sul dispositivo |
| Dati criptati in transito | Sì | ML Kit usa HTTPS |
| L'utente può chiedere la cancellazione | Sì: eliminando l'incontro o l'app | Nessun dato su server di Argo |

## Scheda dello Store in quattro lingue

Titolo, descrizione breve (max 80) e descrizione completa (max 4000) stanno in `docs/play/scheda/<lingua>.json`: `it-IT`, `en-US`, `de-DE`, `es-ES`. Il titolo è «Argo · Taccuino» in tutte le lingue.

```bash
uv run --with google-api-python-client --with google-auth strumenti/play.py scheda leggi
uv run --with google-api-python-client --with google-auth strumenti/play.py scheda scrivi
```

`leggi` salva la scheda attuale di Play in `docs/play/scheda/da-play/` per confrontarla con i file; `scrivi` aggiorna tutte le lingue e pubblica la modifica. All'account di servizio serve il permesso **Gestisci la presenza nello Store** (Play Console › Utenti e autorizzazioni). Gli screenshot restano quelli italiani finché non se ne caricano altri per lingua.
