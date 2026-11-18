# Changelog

Tutte le modifiche rilevanti di Argo per Android.
Versioni: `versionName` (semver) + build `AAAAMMGG.HHMM` (ora di Roma).

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
