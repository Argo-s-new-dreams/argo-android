package blog.argosnewdreams.argo.registrazione

import blog.argosnewdreams.argo.Testi
import blog.argosnewdreams.argo.R
import android.content.Context
import android.graphics.Bitmap
import blog.argosnewdreams.argo.audio.Registratore
import blog.argosnewdreams.argo.elaborazione.Elaborazione
import blog.argosnewdreams.argo.foto.LettoreFoto
import blog.argosnewdreams.argo.modello.Archivio
import blog.argosnewdreams.argo.modello.FotoIncontro
import blog.argosnewdreams.argo.modello.Incontro
import blog.argosnewdreams.argo.modello.OrigineTempi
import blog.argosnewdreams.argo.modello.Segmento
import blog.argosnewdreams.argo.modello.StatoIncontro
import blog.argosnewdreams.argo.trascrizione.Motore
import blog.argosnewdreams.argo.trascrizione.TrascrittoreLocale
import blog.argosnewdreams.argo.trascrizione.TrascrittoreMlKit
import blog.argosnewdreams.argo.trascrizione.TrascrizioneDalVivo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * La registrazione in corso: una sola alla volta, sopravvive alla chiusura della schermata.
 * Microfono → file WAV + trascrizione dal vivo; foto agganciate al secondo; allo stop
 * l'incontro passa all'[Elaborazione] (testo delle foto, eventuale piano B, audio eliminato).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegistrazioneInCorso(
    private val context: Context,
    private val archivio: Archivio,
    private val elaborazione: Elaborazione,
    private val ambito: CoroutineScope,
) {
    private var registratore: Registratore? = null
    private var trascrittore: TrascrizioneDalVivo? = null
    private var motore: Motore? = null

    private val _attiva = MutableStateFlow<Attiva?>(null)
    /** null se non si sta registrando. */
    val attiva: StateFlow<Attiva?> = _attiva.asStateFlow()

    private val _segmenti = MutableStateFlow<List<Segmento>>(emptyList())
    val segmenti: StateFlow<List<Segmento>> = _segmenti.asStateFlow()
    private val _foto = MutableStateFlow<List<FotoIncontro>>(emptyList())
    val foto: StateFlow<List<FotoIncontro>> = _foto.asStateFlow()

    private val registratoreCorrente = MutableStateFlow<Registratore?>(null)
    private val trascrittoreCorrente = MutableStateFlow<TrascrizioneDalVivo?>(null)

    val durata: StateFlow<Double> = registratoreCorrente
        .flatMapLatest { it?.durata ?: flowOf(0.0) }.stateIn(ambito, SharingStarted.Eagerly, 0.0)
    val livello: StateFlow<Float> = registratoreCorrente
        .flatMapLatest { it?.livello ?: flowOf(0f) }.stateIn(ambito, SharingStarted.Eagerly, 0f)
    val inPausa: StateFlow<Boolean> = registratoreCorrente
        .flatMapLatest { it?.inPausa ?: flowOf(false) }.stateIn(ambito, SharingStarted.Eagerly, false)
    val parziale: StateFlow<String> = trascrittoreCorrente
        .flatMapLatest { it?.parziale ?: flowOf("") }.stateIn(ambito, SharingStarted.Eagerly, "")
    val erroreTrascrizione: StateFlow<String?> = trascrittoreCorrente
        .flatMapLatest { it?.errore ?: flowOf(null) }.stateIn(ambito, SharingStarted.Eagerly, null)

    data class Attiva(val id: String, val lingua: String, val creata: Long, val dalVivo: Boolean)

    /**
     * Avvia. Con [motore] null (modello non pronto) si registra lo stesso e si trascrive a fine incontro.
     * Il permesso del microfono va chiesto prima.
     */
    fun avvia(lingua: String, motore: Motore?) {
        check(_attiva.value == null) { Testi.t(R.string.registrazione_gia_in_corso) }
        val id = UUID.randomUUID().toString().uppercase()
        archivio.cartella(id).mkdirs()
        _segmenti.value = emptyList()
        _foto.value = emptyList()

        ServizioRegistrazione.avvia(context)
        val nuovoRegistratore = Registratore { blocco, _ -> trascrittore?.scrivi(blocco) }
        this.motore = motore
        val alSegmento: (Segmento) -> Unit = { segmento -> _segmenti.update { (it + segmento).sortedBy { s -> s.inizio } } }
        trascrittore = when (motore) {
            is Motore.MlKit -> TrascrittoreMlKit(motore, ambito, orologio = { nuovoRegistratore.secondi }, alSegmento = alSegmento)
            is Motore.Locale -> TrascrittoreLocale(motore, ambito, orologio = { nuovoRegistratore.secondi }, alSegmento = alSegmento)
            null -> null
        }
        try {
            nuovoRegistratore.avvia(archivio.fileAudio(id), ambito)
        } catch (e: Exception) {
            ServizioRegistrazione.ferma(context)
            archivio.cartella(id).deleteRecursively()
            motore?.close()
            trascrittore = null
            throw e
        }
        registratore = nuovoRegistratore
        trascrittore?.avvia()
        registratoreCorrente.value = nuovoRegistratore
        trascrittoreCorrente.value = trascrittore
        _attiva.value = Attiva(id, lingua, System.currentTimeMillis(), dalVivo = trascrittore != null)
    }

    fun pausa() {
        registratore?.pausa()
        ambito.launch { trascrittore?.sospendi() }
    }

    fun riprendi() {
        trascrittore?.riprendi()
        registratore?.riprendi()
    }

    /** Foto scattata durante l'incontro: salvata subito, letta (OCR) a fine registrazione. */
    suspend fun aggiungiFoto(immagine: Bitmap, rotazione: Int) {
        val attiva = _attiva.value ?: return
        val secondo = registratore?.secondi ?: 0.0
        val nome = LettoreFoto.salva(immagine, rotazione, archivio.cartellaFoto(attiva.id))
        _foto.update { it + FotoIncontro(nomeFile = nome, secondo = Math.round(secondo * 10) / 10.0) }
    }

    /** Ferma, salva l'incontro e avvia l'elaborazione. Restituisce l'id dell'incontro. */
    suspend fun ferma(): String? {
        val attiva = _attiva.value ?: return null
        val durataFinale = registratore?.ferma() ?: 0.0
        trascrittore?.chiudi()
        val motoreUsato = motore?.nome
        val dalVivoRiuscito = (trascrittore?.segmentiRicevuti ?: 0) > 0
        val avviso = erroreTrascrizione.value?.takeIf { dalVivoRiuscito }
            ?.let { Testi.t(R.string.problema_dal_vivo, it) }
        pulisci()

        archivio.salva(
            Incontro(
                id = attiva.id,
                creata = attiva.creata,
                lingua = attiva.lingua,
                durata = durataFinale,
                segmenti = if (dalVivoRiuscito) _segmenti.value else emptyList(),
                foto = _foto.value,
                stato = StatoIncontro.IN_ELABORAZIONE,
                tempi = OrigineTempi.PAROLE,
                motoreTrascrizione = motoreUsato.takeIf { dalVivoRiuscito },
                fileAudio = archivio.fileAudio(attiva.id).name,
                avviso = avviso,
            )
        )
        elaborazione.avvia(attiva.id)
        return attiva.id
    }

    /** Scarta tutto: audio, foto, cartella. */
    suspend fun scarta() {
        val attiva = _attiva.value ?: return
        registratore?.ferma()
        trascrittore?.interrompi()
        pulisci()
        archivio.cartella(attiva.id).deleteRecursively()
    }

    private fun pulisci() {
        ServizioRegistrazione.ferma(context)
        motore?.close()
        motore = null
        registratore = null
        trascrittore = null
        registratoreCorrente.value = null
        trascrittoreCorrente.value = null
        _attiva.value = null
    }
}
