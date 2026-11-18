# Argo su Google Play · test interno

L'equivalente di TestFlight: fino a 100 tester per app, invitati per email, installano da Google Play e ricevono da soli gli aggiornamenti. Il test interno non passa dalla revisione di Google ed è disponibile subito (la prima volta con nome e scheda provvisori fino a 48 ore).

## Una volta sola

### 1. Account sviluppatore
[Play Console](https://play.google.com/console): registrazione (25 $ una tantum), verifica dell'identità. Valuta un account **organizzazione** intestato ad Argo's New Dreams invece che personale: gli account personali recenti, per passare in produzione, devono prima fare un test chiuso con almeno 12 tester per 14 giorni (il test interno no).

### 2. Chiave di caricamento
Google firma l'app per gli utenti (*Play App Signing*); tu firmi solo i caricamenti. Sul Mac, **fuori dal repository**:

```bash
mkdir -p ~/Documents/ArgoChiavi && cd ~/Documents/ArgoChiavi
keytool -genkeypair -v -keystore argo-upload.jks -alias argo-upload \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Argo's New Dreams, O=Argo's New Dreams, L=Cuneo, C=IT"
```

Poi nella cartella del progetto crea `keystore.properties` (è in `.gitignore`):

```properties
storeFile=/Users/<tu>/Documents/ArgoChiavi/argo-upload.jks
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

## A ogni rilascio

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
