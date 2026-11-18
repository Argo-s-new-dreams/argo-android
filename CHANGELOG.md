# Changelog

Tutte le modifiche rilevanti di Argo per Android.
Versioni: `versionName` (semver) + build `AAAAMMGG.HHMM` (ora di Roma).

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
