package blog.argosnewdreams.argo.trascrizione

import blog.argosnewdreams.argo.Testi
import blog.argosnewdreams.argo.R
import android.util.Log
import blog.argosnewdreams.argo.audio.Audio
import blog.argosnewdreams.argo.modello.Segmento
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.Vad
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * «Argo locale» mentre registri: Silero VAD taglia l'audio nelle pause, Parakeet trascrive ogni pezzo.
 * Il VAD conta i campioni ricevuti, e il registratore manda solo audio registrato (non le pause),
 * quindi l'inizio di ogni pezzo è già il secondo giusto della registrazione: foto e voci restano agganciate.
 *
 * Se il telefono è più lento del parlato, l'audio si accoda e viene trascritto lo stesso:
 * alla fine [chiudi] aspetta che la coda sia vuota.
 */
class TrascrittoreLocale(
    private val motore: Motore.Locale,
    private val ambito: CoroutineScope,
    private val orologio: () -> Double,
    private val alSegmento: (Segmento) -> Unit,
) : TrascrizioneDalVivo {

    private val _parziale = MutableStateFlow("")
    override val parziale: StateFlow<String> = _parziale.asStateFlow()
    private val _errore = MutableStateFlow<String?>(null)
    override val errore: StateFlow<String?> = _errore.asStateFlow()
    override var segmentiRicevuti = 0
        private set

    private val coda = Channel<FloatArray>(Channel.UNLIMITED)
    private var lavoro: Job? = null
    private var fineUltimo = 0.0

    override fun avvia() {
        if (lavoro != null) return
        lavoro = ambito.launch(Dispatchers.Default) {
            try {
                _parziale.value = Testi.t(R.string.preparo_modello)
                val riconoscitore = motore.riconoscitore()
                val vad = motore.nuovoVad()
                _parziale.value = ""
                try {
                    val finestra = Motore.Locale.FINESTRA_VAD
                    var resto = FloatArray(0)
                    for (blocco in coda) {
                        // Il VAD vuole finestre esatte di 512 campioni
                        var dati = if (resto.isEmpty()) blocco else resto + blocco
                        var i = 0
                        while (i + finestra <= dati.size) {
                            vad.acceptWaveform(dati.copyOfRange(i, i + finestra))
                            i += finestra
                            svuota(vad, riconoscitore)
                        }
                        resto = dati.copyOfRange(i, dati.size)
                        _parziale.value = if (vad.isSpeechDetected()) "…" else ""
                    }
                    if (resto.isNotEmpty()) vad.acceptWaveform(resto)
                    vad.flush()
                    svuota(vad, riconoscitore)
                } finally {
                    vad.release()
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Trascrizione locale interrotta", e)
                _errore.value = e.message ?: e.javaClass.simpleName
            } finally {
                _parziale.value = ""
            }
        }
    }

    override fun scrivi(blocco: ByteArray) {
        coda.trySend(pcmInFloat(blocco, blocco.size))
    }

    /** In pausa il registratore non manda audio: non serve fare nulla, il VAD chiude la frase da sé. */
    override suspend fun sospendi() = Unit

    override fun riprendi() = Unit

    override suspend fun chiudi() {
        coda.close()
        if (segmentiRicevuti >= 0) _parziale.value = Testi.t(R.string.trascrivo_ultimi_secondi)
        lavoro?.join()
    }

    override suspend fun interrompi() {
        coda.close()
        lavoro?.cancel()
        lavoro?.join()
    }

    private fun svuota(vad: Vad, riconoscitore: OfflineRecognizer) {
        while (!vad.empty()) {
            val pezzo = vad.front()
            vad.pop()
            val inizio = pezzo.start.toDouble() / Audio.CAMPIONAMENTO
            val durata = pezzo.samples.size.toDouble() / Audio.CAMPIONAMENTO
            trascriviPezzo(riconoscitore, pezzo.samples, inizio, durata, fineUltimo).forEach {
                fineUltimo = it.fine
                segmentiRicevuti++
                alSegmento(it)
            }
        }
    }

    companion object {
        private const val TAG = "Argo"

        /** Un pezzo di audio → segmenti con i tempi delle parole (assoluti nella registrazione). */
        internal fun trascriviPezzo(
            riconoscitore: OfflineRecognizer,
            campioni: FloatArray,
            inizio: Double,
            durata: Double,
            fineUltimo: Double,
        ): List<Segmento> {
            val flusso = riconoscitore.createStream()
            try {
                flusso.acceptWaveform(campioni, Audio.CAMPIONAMENTO)
                riconoscitore.decode(flusso)
                val risultato = riconoscitore.getResult(flusso)
                val parole = ParoleDaToken.raggruppa(risultato.tokens.toList(), risultato.timestamps.toList())
                return Segmentatore.daParole(risultato.text, parole, offset = inizio, adesso = inizio + durata, fineUltimo = fineUltimo)
            } finally {
                flusso.release()
            }
        }

        fun pcmInFloat(dati: ByteArray, lunghezza: Int): FloatArray {
            val campioni = ByteBuffer.wrap(dati, 0, lunghezza).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            return FloatArray(campioni.remaining()) { campioni.get(it) / 32768f }
        }

        /**
         * Piano B con Argo locale: trascrive il WAV intero (stessa pipeline VAD → Parakeet),
         * quindi **con i tempi veri**, a differenza del piano B di ML Kit.
         */
        suspend fun trascriviFile(motore: Motore.Locale, wav: File, avanzamento: (Int) -> Unit = {}): List<Segmento> =
            withContext(Dispatchers.Default) {
                val riconoscitore = motore.riconoscitore()
                val vad = motore.nuovoVad()
                val segmenti = mutableListOf<Segmento>()
                try {
                    RandomAccessFile(wav, "r").use { file ->
                        val totale = (file.length() - 44).coerceAtLeast(0)
                        file.seek(44)
                        val buffer = ByteArray(Motore.Locale.FINESTRA_VAD * 2 * 32)
                        var letti = 0L
                        var resto = FloatArray(0)
                        fun svuotaFile() {
                            while (!vad.empty()) {
                                val pezzo = vad.front(); vad.pop()
                                segmenti += trascriviPezzo(
                                    riconoscitore, pezzo.samples,
                                    pezzo.start.toDouble() / Audio.CAMPIONAMENTO,
                                    pezzo.samples.size.toDouble() / Audio.CAMPIONAMENTO,
                                    segmenti.lastOrNull()?.fine ?: 0.0,
                                )
                            }
                        }
                        while (true) {
                            val n = file.read(buffer)
                            if (n <= 0) break
                            letti += n
                            val dati = resto + pcmInFloat(buffer, n - n % 2)
                            var i = 0
                            while (i + Motore.Locale.FINESTRA_VAD <= dati.size) {
                                vad.acceptWaveform(dati.copyOfRange(i, i + Motore.Locale.FINESTRA_VAD))
                                i += Motore.Locale.FINESTRA_VAD
                            }
                            resto = dati.copyOfRange(i, dati.size)
                            svuotaFile()
                            if (totale > 0) avanzamento(((letti * 100) / totale).toInt())
                        }
                        if (resto.isNotEmpty()) vad.acceptWaveform(resto)
                        vad.flush()
                        svuotaFile()
                    }
                } finally {
                    vad.release()
                }
                if (segmenti.isEmpty()) throw ErroreTrascrizione(Testi.t(R.string.nessun_parlato))
                segmenti
            }
    }
}
