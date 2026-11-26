# Changelog

Tutte le modifiche rilevanti di Argo per Android.
Versioni: `versionName` (semver) + build `AAAAMMGG.HHMM` (ora di Roma).

## [0.3.1] · build (al prossimo invio)

Un link che Google Play chiedeva e un'icona che finalmente si adatta al telefono.

### Aggiunto
- **Informativa sulla privacy dentro l'app.** In Impostazioni › Privacy c'è il link all'informativa sul blog, nella lingua dell'app (italiano, inglese, tedesco o spagnolo). Google Play lo richiede.
- **Scorciatoia «Registra».** Tieni premuta l'icona di Argo e scegli *Registra*: si apre direttamente la registrazione, con l'informativa da confermare. Puoi anche trascinarla sulla schermata Home. Utile in auto: Android Auto non ammette app che registrano o conversano, quindi Argo resta sul telefono, ma si avvia in un attimo.
- **Icona adattiva.** Il logo di Argo's New Dreams su fondo carta: il telefono la ritaglia nella forma che usa per tutte le icone (cerchio, quadrato arrotondato, goccia), senza bordi strani.

### Modificato
- Nel pacchetto per Google Play c'è anche la tabella dei simboli delle librerie native, così la Console può leggere un eventuale crash nativo (sherpa-onnx, ONNX Runtime).

### Da provare
- Impostazioni › Privacy › «Informativa sulla privacy»: si apre la pagina giusta; con l'app in inglese si apre quella in inglese
- Home del telefono: l'icona di Argo ha la stessa forma delle altre e il logo non è tagliato

## [0.3.0] · build 20261123.0319

Argo parla quattro lingue: italiano, inglese, spagnolo e tedesco. E da Android 15 non si chiude più quando premi Stop.

### Aggiunto
- **Interfaccia in italiano, inglese, spagnolo e tedesco.** Tutti i testi delle schermate, degli avvisi, degli errori e del testo condiviso. Argo segue la lingua del telefono; da Android 13 la scegli anche in Impostazioni › Lingua dell'app (o nelle impostazioni di sistema dell'app). Su Android 12 segue sempre il telefono. Se il telefono è in un'altra lingua, Argo resta in italiano.
- La **lingua dell'incontro** resta una scelta a parte: puoi registrare in tedesco con l'app in italiano. I nomi delle lingue ora sono scritti nella lingua dell'app.
- Date, titoli predefiniti («Incontro del…», «Meeting of…»), nomi dei parlanti e intestazioni della relazione nella lingua dell'app.
- Test `TraduzioniTest`: ogni testo esiste in tutte le lingue con gli stessi segnaposto.

### Note
- Le traduzioni in inglese, spagnolo e tedesco sono una prima versione da rileggere.
- I nomi dei menu di Argo sul computer citati nei testi inglese, spagnolo e tedesco («Meetings › Import package…») saranno giusti quando anche Argo desktop avrà le quattro lingue.
- Avvisi ed errori già salvati in un incontro restano nella lingua in cui sono nati.

### Corretto
- **Chiusura dell'app al termine della registrazione (Android 15 e successivi).** Il servizio «Argo sta elaborando l'incontro» partiva senza tipo, e Android chiudeva l'app. Ora parte come *mediaProcessing*, come previsto. Trovato registrando il video dimostrativo per Google Play sull'emulatore Android 17.

### Da provare
- Impostazioni › Lingua dell'app › English (poi Español, Deutsch): tutte le schermate cambiano lingua senza riavviare; torna a «Come il telefono»
- Con l'app in inglese registra un incontro breve: avvisi, fasi dell'elaborazione e testo condiviso sono in inglese
- Registra un incontro breve e premi Stop: Argo non si chiude, compare l'avviso «Argo sta elaborando l'incontro» e poi l'incontro pronto
- Durante l'elaborazione torna alla schermata Home: l'avviso resta nella tendina

## [0.2.4] · build 20261120.2308

Gli incontri lunghi si elaborano anche a schermo spento, e in Impostazioni trovi il testo completo delle licenze.

### Aggiunto
- **Licenze e riconoscimenti** in Impostazioni › Crediti: testo completo di Apache 2.0 (sherpa-onnx, AndroidX), MIT (ONNX Runtime, Silero VAD) e attribuzione CC BY 4.0 di NVIDIA Parakeet, con link alla licenza.

### Corretto
- **Elaborazione a schermo spento o con un'altra app davanti.** Durante la trascrizione dal file e la lettura delle foto, Argo ora tiene un servizio in primo piano, con l'avviso fisso «Argo sta elaborando l'incontro» e la fase in corso. Prima, con lo schermo spento, Android poteva sospendere o chiudere Argo a metà di un incontro lungo. Il servizio è di tipo *mediaProcessing* da Android 15 (fino a 6 ore ogni 24) e *dataSync* prima; tiene il processore acceso, lo schermo no. Se Android non lo lascia partire, il lavoro continua finché l'app resta aperta. Lo stesso problema era stato trovato sull'iPhone; su Android l'elaborazione era già indipendente dalla schermata.

### Da provare
- Impostazioni › Crediti › Licenze e riconoscimenti: il testo si apre e si scorre fino in fondo
- Registra un incontro di almeno 10 minuti con la trascrizione dopo la registrazione, spegni lo schermo: l'avviso «Argo sta elaborando l'incontro» resta e alla riaccensione l'incontro è pronto

## [0.2.3] · build 20261118.1159

Prima prova di Argo locale sul Galaxy S21: trascrizione riuscita, elaborazione ferma sulla lettura delle foto.

### Corretto
- Build ottimizzata (Lab e Play): R8 toglieva a ML Kit Text Recognition un pezzo usato per riflessione → `NullPointerException` in `com.google.mlkit.vision.text.internal` durante la lettura delle foto. Regole di protezione per ML Kit e R8 in modalità compatibile (non «full»)
- Se la lettura di una foto fallisce, la foto resta senza testo e l'incontro si completa lo stesso (il computer può rileggerla con il modello vision)

## [0.2.2] · build 20261118.1115

### Aggiunto
- Se Argo si chiude per un errore, al riavvio mostra il dettaglio tecnico con **Condividi** (solo l'errore: niente testo degli incontri né audio). Serve quando il telefono non è collegato ad Android Studio

## [0.2.1] · build 20261118.1057

### Corretto
- All'apertura Argo riprende gli incontri rimasti a metà: un errore in quella ripresa chiudeva l'app. Ora finisce nella scheda dell'incontro (con «Riprova») e nel Logcat con il tag `Argo`
- Qualunque errore nei lavori di sottofondo viene registrato invece di chiudere l'app

## [0.2.0] · build 20261118.1048

**Argo locale**: un motore di trascrizione dentro Argo, che funziona su ogni telefono Android 12+ anche quando la trascrizione di Google non c'è (es. Galaxy S21).

### Aggiunto
- Motore **Argo locale**: NVIDIA Parakeet TDT 0.6B v3 (25 lingue europee, italiano compreso, punteggiatura e tempi delle parole) con sherpa-onnx 1.12.21 e Silero VAD
- Trascrizione **quasi dal vivo**: il VAD taglia l'audio nelle pause e ogni pezzo viene trascritto mentre registri; se il telefono è più lento del parlato l'audio si accoda e allo stop Argo finisce gli ultimi secondi
- **Download del modello** (~670 MB) una volta sola: revisione fissa del repository Hugging Face, impronta SHA-256 per ogni file, ripresa dopo interruzioni, avviso se sei sui dati mobili, continua anche se lasci la schermata; Impostazioni › «Elimina modello» per liberare spazio
- Piano B con Argo locale: trascrizione del file a fine incontro **con i tempi veri** (non più stimati)
- Impostazioni › Crediti (Parakeet CC-BY-4.0, sherpa-onnx Apache 2.0, Silero VAD MIT) e `THIRD_PARTY_LICENSES.md`

### Modificato
- Modalità **Automatica**: Avanzata (Google) se c'è → Argo locale se scaricato → Base (Google) se pronta → proposta di scaricare Argo locale
- Schermata Registra e Impostazioni usano lo stesso pannello del motore, con avanzamento del download in MB
- APK solo per ARM a 64 bit (tutti i telefoni Android 12+ di fascia media e alta)

### Verificato
- Parakeet v3 int8 con lo stesso VAD su 192 s di italiano reale (Google FLEURS, 10 frasi di parlanti diversi): ~8,5% di parole sbagliate, calcolo ~0,04× il tempo reale su 2 core x86 del server di sviluppo; picco di memoria ~1,5 GB. Velocità sul telefono da misurare

## [0.1.1] · build 20261118.1028

Diagnosi della trascrizione (prima prova su Galaxy S21).

### Modificato
- Se la trascrizione sul dispositivo non è disponibile, Argo mostra **cosa ha risposto ML Kit per ogni modalità** (tipo di errore, messaggio, codici ML Kit/AICore) invece di un messaggio generico
- Le stesse informazioni finiscono nel Logcat con il tag `Argo`

## [0.1.0] · build 20261118.0830

Prima versione: il taccuino da fiera su Android, allineato ad Argo per iPhone 0.6.

### Aggiunto
- **Registrazione** dal microfono (PCM 16 kHz mono) con servizio in primo piano: continua a schermo spento; pausa che spegne davvero il microfono
- **Trascrizione dal vivo sul telefono** con ML Kit GenAI Speech Recognition (alpha), modalità Automatica / Avanzata (Gemini Nano) / Base, tempi parola per parola; frasi lunghe spezzate a fine frase
- **Piano B**: se la trascrizione dal vivo non parte, si trascrive il file a fine incontro (tempi stimati, segnalati)
- **Download del modello** della lingua dalla schermata di registrazione o dalle Impostazioni, con avanzamento
- **Foto durante la registrazione** con la fotocamera dentro Argo, agganciate al secondo; **OCR** sul telefono con modello incluso; email, siti e telefoni riconosciuti (Partita IVA, codice fiscale, REA esclusi)
- **Audio eliminato** appena trascritto (opzione attiva di default); registrazioni interrotte cancellate al riavvio
- **Pacchetto Argo** `argo.sessione` v1 identico all'iPhone, inviato con la condivisione di Android; verificato con l'importazione reale di Argo desktop
- **Condividi il testo** in Markdown (relazione + trascrizione)
- Informativa prima di ogni registrazione, come su iPhone e desktop
- **Stile Argo**: palette Notte e Carta del desktop, accento terracotta, stessi colori dei parlanti
- Canali: build **Argo Lab** (`.lab`, ottimizzata, firmata con la chiave di debug) installabile a mano accanto alla versione di Google Play
- Incontri esclusi dal backup di Google e dal trasferimento dati
- Test: segmenti, riferimenti, pacchetto, archivio, WAV (16)
