package blog.argosnewdreams.argo.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

/**
 * Microfono → PCM 16 kHz mono. Ogni blocco (100 ms) va nel file WAV e a [alBlocco]
 * (la trascrizione dal vivo). In pausa il microfono si spegne davvero: niente audio, niente indicatore.
 * La durata conta solo l'audio registrato, quindi è la stessa linea del tempo di segmenti e foto.
 */
class Registratore(private val alBlocco: (ByteArray, Int) -> Unit) {

    private val _durata = MutableStateFlow(0.0)
    val durata: StateFlow<Double> = _durata.asStateFlow()
    private val _livello = MutableStateFlow(0f)
    val livello: StateFlow<Float> = _livello.asStateFlow()
    private val _inPausa = MutableStateFlow(false)
    val inPausa: StateFlow<Boolean> = _inPausa.asStateFlow()

    private var registro: AudioRecord? = null
    private var wav: FileWav? = null
    private var lavoro: Job? = null

    /** Secondi registrati finora (le pause non contano); dopo lo stop resta il totale. */
    val secondi: Double get() = wav?.let { it.byteDati.toDouble() / Audio.BYTE_AL_SECONDO } ?: totaleFinale
    @Volatile private var totaleFinale = 0.0

    @SuppressLint("MissingPermission") // il permesso è chiesto dalla schermata prima di avviare
    fun avvia(file: File, ambito: CoroutineScope) {
        val minimo = AudioRecord.getMinBufferSize(Audio.CAMPIONAMENTO, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val dimensione = maxOf(minimo, Audio.BYTE_AL_SECONDO) // un secondo di margine
        // VOICE_RECOGNITION: niente controllo automatico del guadagno, pensato per il riconoscimento vocale.
        val nuovo = listOf(MediaRecorder.AudioSource.VOICE_RECOGNITION, MediaRecorder.AudioSource.MIC)
            .firstNotNullOfOrNull { sorgente ->
                AudioRecord(sorgente, Audio.CAMPIONAMENTO, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, dimensione)
                    .takeIf { it.state == AudioRecord.STATE_INITIALIZED }
            } ?: throw ErroreRegistrazione("Non riesco ad accedere al microfono.")

        registro = nuovo
        wav = FileWav(file)
        totaleFinale = 0.0
        _durata.value = 0.0
        _inPausa.value = false
        nuovo.startRecording()

        lavoro = ambito.launch(Dispatchers.IO) {
            val blocco = ByteArray(Audio.BYTE_AL_SECONDO / 10)
            var acceso = true
            while (isActive) {
                if (_inPausa.value) {
                    if (acceso) { nuovo.stop(); acceso = false; _livello.value = 0f }
                    delay(50)
                    continue
                }
                if (!acceso) { nuovo.startRecording(); acceso = true }
                val letti = nuovo.read(blocco, 0, blocco.size)
                if (letti <= 0) { delay(10); continue }
                wav?.scrivi(blocco, letti)
                alBlocco(blocco.copyOf(letti), letti)
                _durata.value = secondi
                _livello.value = Audio.livello(blocco, letti)
            }
        }
    }

    fun pausa() { _inPausa.value = true }
    fun riprendi() { _inPausa.value = false }

    /** Ferma tutto e restituisce i secondi registrati. */
    suspend fun ferma(): Double {
        lavoro?.cancel()
        lavoro?.join()
        lavoro = null
        val totale = secondi
        totaleFinale = totale
        registro?.run { runCatching { stop() }; release() }
        registro = null
        wav?.close()
        wav = null
        _livello.value = 0f
        _inPausa.value = false
        return totale
    }
}

class ErroreRegistrazione(messaggio: String) : Exception(messaggio)
