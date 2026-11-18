package blog.argosnewdreams.argo.trascrizione

import android.os.ParcelFileDescriptor
import blog.argosnewdreams.argo.modello.Segmento
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.google.mlkit.genai.speechrecognition.speechRecognizerRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Trascrive mentre registri. Lo stesso PCM del file WAV arriva qui con [scrivi] e passa a ML Kit
 * da una pipe, in streaming: è l'unico modo per avere i tempi delle parole (con un file intero
 * ML Kit non li dà), quindi l'unico per agganciare foto e voci al momento giusto.
 *
 * Ogni pausa chiude la sessione di riconoscimento e la ripresa ne apre una nuova con l'offset
 * della registrazione: così i tempi restano giusti qualunque sia l'origine che usa ML Kit.
 */
class TrascrittoreDalVivo(
    private val motore: Motore,
    private val ambito: CoroutineScope,
    /** Secondi registrati in questo momento. */
    private val orologio: () -> Double,
    private val alSegmento: (Segmento) -> Unit,
) {
    private val _parziale = MutableStateFlow("")
    /** Testo provvisorio della frase in corso, da mostrare in grigio. */
    val parziale: StateFlow<String> = _parziale.asStateFlow()
    private val _errore = MutableStateFlow<String?>(null)
    val errore: StateFlow<String?> = _errore.asStateFlow()

    /** Quanti segmenti sono arrivati: se resta 0 si ritrascrive dal file a fine registrazione. */
    var segmentiRicevuti = 0
        private set

    @Volatile private var sessione: Sessione? = null
    private var fineUltimo = 0.0

    private inner class Sessione(val offset: Double) {
        /** Chiusa da noi (pausa o stop): se il riconoscimento finisce, non va riaperto. */
        @Volatile var chiusa = false
        val canale = Channel<ByteArray>(Channel.UNLIMITED)
        val sorgente: AudioSource
        val scrittore: Job
        val ascoltatore: Job

        init {
            val (lettura, scrittura) = ParcelFileDescriptor.createPipe()
            sorgente = AudioSource.fromPfd(lettura, AudioSource.Mode.STREAMING)
            scrittore = ambito.launch(Dispatchers.IO) {
                ParcelFileDescriptor.AutoCloseOutputStream(scrittura).use { uscita ->
                    runCatching { for (blocco in canale) uscita.write(blocco) }
                }
            }
            val richiesta = speechRecognizerRequest {
                audioSource = sorgente
                recognitionOptions { enableWordTiming = true }
            }
            ascoltatore = ambito.launch {
                runCatching {
                    motore.client.startRecognition(richiesta).collect { risposta -> gestisci(risposta, offset) }
                }.onFailure { _errore.value = it.message ?: "Errore della trascrizione dal vivo." }
                // Il riconoscimento si è chiuso da solo mentre si registra ancora: si riparte.
                if (!chiusa && sessione === this@Sessione) {
                    canale.close()
                    sessione = Sessione(orologio())
                }
            }
        }

        suspend fun termina() {
            canale.close()                       // fine dell'audio: ML Kit chiude l'ultima frase
            scrittore.join()
            if (withTimeoutOrNull(10_000) { ascoltatore.join() } == null) {
                runCatching { motore.client.stopRecognition() }
                ascoltatore.cancel()
            }
            runCatching { sorgente.close() }
        }
    }

    fun avvia() {
        sessione = Sessione(orologio())
    }

    fun scrivi(blocco: ByteArray) {
        sessione?.canale?.trySend(blocco)
    }

    /** Pausa: chiude la frase in corso. */
    suspend fun sospendi() {
        val corrente = sessione ?: return
        sessione = null                 // una ripresa immediata apre la sua sessione senza toccare questa
        corrente.chiusa = true
        corrente.termina()
        _parziale.value = ""
    }

    fun riprendi() = avvia()

    suspend fun chiudi() {
        sospendi()
    }

    private fun gestisci(risposta: SpeechRecognizerResponse, offset: Double) {
        when (risposta) {
            is SpeechRecognizerResponse.PartialTextResponse -> _parziale.value = risposta.text
            is SpeechRecognizerResponse.FinalTextResponse -> {
                _parziale.value = ""
                val parole = risposta.wordTimings.orEmpty().map { Parola(it.text, it.timestampMs) }
                Segmentatore.daParole(risposta.text, parole, offset, orologio(), fineUltimo).forEach {
                    fineUltimo = it.fine
                    segmentiRicevuti++
                    alSegmento(it)
                }
            }
            is SpeechRecognizerResponse.ErrorResponse ->
                _errore.value = risposta.e.message ?: "Errore della trascrizione (codice ${risposta.e.errorCode})."
            else -> Unit // livello audio e completamento: niente da fare
        }
    }
}
