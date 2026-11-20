# Argo per Android · Taccuino

Il fratello Android di [Argo per iPhone](https://github.com/Argo-s-new-dreams/argo-ios) e di [Argo · Cassetta degli attrezzi AI](https://github.com/Argo-s-new-dreams/argo-ai): **un taccuino per riunioni e fiere**. Registri la conversazione, fotografi biglietti da visita e documenti mentre parli, e Argo trascrive sul telefono, aggancia ogni foto al momento giusto e non lascia l'audio sul telefono. Niente cloud, niente account.

> *Non è un registratore. È una stenografa digitale locale.* Ascolta solo mentre la usi, trascrive sul telefono, non conserva la voce, non invia la conversazione a un cloud, non profila gli interlocutori e lascia a te il controllo del testo prodotto.

🔒 100% locale · Android 12+ · Kotlin + Jetpack Compose

## Cosa fa (0.1)

| Funzione | Come | Dove gira |
|---|---|---|
| **Registra** | Microfono PCM 16 kHz mono, continua a schermo spento (servizio in primo piano «microfono») | telefono |
| **Trascrive (Argo locale)** | NVIDIA Parakeet TDT 0.6B v3 con [sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx) e Silero VAD: ogni frase trascritta appena finisci di dirla, con i tempi delle parole. Funziona su **ogni** telefono Android 12+ | telefono |
| **Trascrive (Google)** | [ML Kit GenAI Speech Recognition](https://developers.google.com/ml-kit/genai/speech-recognition/android): Avanzata con Gemini Nano sui telefoni recenti, Base altrove | telefono |
| **Foto** | Fotocamera dentro Argo (solo foto, la registrazione continua), ogni scatto agganciato al secondo della conversazione | telefono |
| **Testo delle foto** | OCR ML Kit con modello incluso nell'app + email, siti e telefoni riconosciuti (P.IVA e CAP esclusi) | telefono |
| **Audio eliminato** | Appena trascritto resta solo testo e foto | telefono |
| **Pacchetto Argo** | .zip `argo.sessione` v1, **identico a quello dell'iPhone**: testo e foto, mai l'audio | — |
| **Invia ad Argo sul computer** | Condivisione Android (Quick Share, mail, Teams, Drive…) → *Incontri › Importa pacchetto…* su Argo desktop | — |

Prossime versioni: divisione delle voci e anagrafe voci (0.3), Ponte con Argo desktop (0.4), relazione sul telefono o dal Mac con Ollama (0.5). Vedi [Roadmap](#roadmap).

### I motori di trascrizione

| Modalità | Cosa usa | Dove funziona |
|---|---|---|
| **Automatica** (predefinita) | Avanzata se c'è → Argo locale se scaricato → Base se pronta → propone di scaricare Argo locale | ovunque |
| **Argo locale** | Parakeet TDT 0.6B v3 int8 (~670 MB da scaricare una volta, revisione fissa e SHA-256 verificati) | ogni Android 12+ ARM64; ~1,5 GB di memoria durante la trascrizione |
| **Avanzata (Google)** | Gemini Nano tramite ML Kit | Galaxy S26, Pixel 10 e pochi altri |
| **Base (Google)** | Modello di sistema tramite ML Kit; italiano in beta | dipende dal telefono (sul Galaxy S21 non c'è) |

Argo locale: il VAD taglia l'audio nelle pause (min 0,5 s di silenzio, pezzi di al massimo 20 s) e ogni pezzo va a Parakeet. Prova di riferimento sul server di sviluppo: italiano reale (Google FLEURS) con ~8,5% di parole sbagliate.

### Perché trascrive *dal vivo*

Con Argo locale i tempi arrivano sempre, anche trascrivendo un file. ML Kit invece dà i tempi delle parole solo in streaming: con un file intero la trascrizione arriva senza tempi. Senza tempi non si possono agganciare foto e voci al momento giusto, quindi Argo trascrive mentre registra (lo stesso audio va al file temporaneo e al riconoscimento). Se la trascrizione dal vivo non parte (modello non ancora scaricato, telefono non supportato), Argo registra comunque e trascrive il file a fine incontro, con tempi **stimati** (lo scrive nella scheda dell'incontro).

## Requisiti

- Android 12 (API 31) o successivo
- Modalità di trascrizione:
  - **Avanzata** (Gemini Nano): telefoni con Gemini Nano su chip recenti, per esempio Galaxy S26, Z Flip8, Z Fold8, Pixel 10; italiano non in beta.
  - **Base** (modello di sistema): Android 12+; l'italiano è **in beta** (più errori).
  - **Automatica** (predefinita): Avanzata se disponibile, altrimenti Base.
- Il modello della lingua si scarica una volta (serve la rete), poi tutto funziona offline. **Fai una prova prima della fiera.**

## Compilare

```bash
git clone https://github.com/Argo-s-new-dreams/argo-android.git
cd argo-android
./gradlew assembleLab          # APK «Argo Lab» ottimizzato (~48 MB) da installare a mano sul telefono
./gradlew assembleDebug        # come sopra ma per il debug da Android Studio (più grande, non ottimizzato)
./gradlew bundleRelease        # AAB per Google Play (serve keystore.properties, vedi docs/GOOGLE-PLAY.md)
./gradlew testDebugUnitTest    # test della logica (segmenti, OCR, pacchetto, WAV)
```

Con Android Studio: *Open* sulla cartella, collega il telefono con il debug USB attivo e premi ▶︎.

- **Canali come su desktop**: *lab* e *debug* si installano come **Argo Lab** (`blog.argosnewdreams.argo.lab`, accanto a quella di Play), la *release* è **Pro**.
- **Installare l'APK Lab sul Galaxy**: copialo sul telefono (Quick Share, cavo, Drive), aprilo dall'app *File* e consenti l'installazione da quella sorgente quando Android lo chiede. Oppure, con il debug USB: `adb install -r app/build/outputs/apk/lab/app-lab.apk`.
- **Versione**: `versioneMarketing` in `app/build.gradle.kts` (semver) + build `AAAAMMGG.HHMM` nel file `VERSIONE`, ora di Roma. `strumenti/nuova-build.sh` la aggiorna; il `versionCode` per Play si calcola da lì (minuti dal 1/1/2026).

## Distribuire con Google Play (test interno)

Vedi [docs/GOOGLE-PLAY.md](docs/GOOGLE-PLAY.md): account, chiave di caricamento, prima release, tester, sezione «Sicurezza dei dati».

## Privacy

- Informativa da confermare prima di ogni registrazione (GDPR / AI Act).
- Audio eliminato appena trascritto (opzione attiva di default). Una registrazione interrotta dal sistema viene cancellata al riavvio: nessun audio orfano.
- Incontri, foto e impostazioni **esclusi dal backup di Google** e dal trasferimento a un nuovo telefono.
- La conversazione non lascia il telefono: secondo i [termini di ML Kit](https://developers.google.com/ml-kit/terms) i dati in ingresso sono elaborati interamente sul dispositivo e non vengono inviati ai server di Google. Le librerie ML Kit però **aggiungono il permesso Internet e inviano a Google metriche di prestazioni e utilizzo** (informazioni sul dispositivo, per diagnostica: vedi la [dichiarazione dei dati di ML Kit](https://developers.google.com/ml-kit/android-data-disclosure)). Da dichiarare nella scheda Play; da verificare con un'analisi del traffico (vedi Da fare).
- Argo usa Internet di sua iniziativa solo per scaricare il modello di Argo locale da Hugging Face, quando lo chiedi tu. Servirà poi per il Ponte in rete locale.

## Struttura

```
app/src/main/java/blog/argosnewdreams/argo/
├── ArgoApp.kt                       contenitore: archivio, impostazioni, registrazione, elaborazione
├── MainActivity.kt                  navigazione Compose
├── modello/Incontro.kt              come Sessione dell'iPhone: segmenti, foto, cronologia, Markdown
├── modello/Archivio.kt              un incontro = una cartella (incontro.json, foto/, audio.wav temporaneo)
├── audio/                           AudioRecord → WAV + blocchi per la trascrizione; livello
├── trascrizione/
│   ├── MotoreTrascrizione.kt        scelta del motore (Automatica, Argo locale, Avanzata, Base), stato, download
│   ├── ModelloLocale.kt             download verificato di Parakeet (revisione fissa, SHA-256, ripresa)
│   ├── TrascrittoreLocale.kt        Argo locale: Silero VAD → Parakeet a pezzi, anche da file
│   ├── ParoleDaToken.kt             logica pura: pezzi di parola SentencePiece → parole con tempi
│   ├── TrascrittoreMlKit.kt         ML Kit: streaming da pipe, tempi delle parole, pausa = nuova sessione con offset
│   ├── TrascrizioneDaFile.kt        piano B a fine incontro (tempi stimati)
│   └── Segmentatore.kt              logica pura: parole → segmenti, controlli di plausibilità
├── foto/                            salvataggio, OCR ML Kit, riferimenti (email, siti, telefoni)
├── registrazione/                   registrazione in corso + servizio in primo piano
├── elaborazione/Elaborazione.kt     dopo lo stop: piano B, OCR, audio eliminato
├── esporta/Esportatore.kt           pacchetto argo.sessione v1
└── ui/                              Compose, Stile Argo (Notte/Carta, accento terracotta)
```

## Formato del pacchetto

Lo stesso dell'iPhone: [argo-ios › docs/FORMATO-PACCHETTO.md](https://github.com/Argo-s-new-dreams/argo-ios/blob/main/docs/FORMATO-PACCHETTO.md). Il campo `app` vale `Argo per Android <versione>`. Verificato con il codice di Argo desktop (`ponte.importa_pacchetto` e `relazione_incontro.cronologia`): importazione, reimportazione idempotente, cronologia di parole e foto.

## Roadmap

| Versione | Contenuto |
|---|---|
| 0.1 | Registrazione, trascrizione dal vivo (ML Kit), foto + OCR, pacchetto Argo, audio eliminato |
| **0.2** | Argo locale: Parakeet TDT v3 + sherpa-onnx, funziona su ogni Android 12+ |
| 0.3 | Divisione delle voci sul telefono (sherpa-onnx ha già la diarizzazione: pyannote segmentation + embedding), profilo «la mia voce», test di compatibilità delle impronte con il desktop |
| 0.4 | Ponte con Argo desktop: abbinamento QR, HTTPS in rete locale con pinning, anagrafe voci giù, incontri su |
| 0.5 | Relazione sul telefono con Gemini Nano (Prompt API) dove disponibile, altrimenti dal Mac con Ollama in rete locale |

## Da fare

- [ ] Prova su Galaxy S21: velocità di Argo locale (tiene il passo del parlato?), batteria e calore in un'ora, rumore da fiera, pausa/ripresa
- [ ] Se Argo locale è lento sui telefoni più vecchi: opzione con un modello più leggero
- [ ] Verificare l'origine dei tempi delle parole di ML Kit (la documentazione non è univoca; il codice si protegge con controlli di plausibilità)
- [ ] Analisi del traffico di rete durante una registrazione (cosa inviano le metriche ML Kit)
- [ ] Icona adattiva Android (primo piano + sfondo) dal logo originale Icona_AND
- [ ] Interruzioni: telefonata in arrivo durante la registrazione
- [ ] Licenza, allineata ad argo-ai
