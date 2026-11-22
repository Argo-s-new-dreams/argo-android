package blog.argosnewdreams.argo.trascrizione

import android.content.res.AssetManager
import android.util.Log
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizer
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.speechRecognizerOptions
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Locale
import androidx.annotation.StringRes
import blog.argosnewdreams.argo.R
import blog.argosnewdreams.argo.Testi

/** Quale motore usare per trascrivere: tutti girano sul telefono. */
enum class Modalita(@StringRes val idEtichetta: Int, @StringRes val idDescrizione: Int) {
    AUTOMATICA(R.string.modalita_automatica, R.string.modalita_automatica_desc),
    LOCALE(R.string.modalita_locale, R.string.modalita_locale_desc),
    AVANZATA(R.string.modalita_avanzata, R.string.modalita_avanzata_desc),
    BASE(R.string.modalita_base, R.string.modalita_base_desc);

    val etichetta: String get() = Testi.t(idEtichetta)
}

/** Lo stato del motore scelto per una lingua. */
sealed interface StatoModello {
    data class Pronto(val motore: Motore) : StatoModello
    data class DaScaricare(val motore: Motore) : StatoModello
    data class InDownload(val motore: Motore) : StatoModello
    data class NonDisponibile(val motivo: String) : StatoModello
}

/** Un motore pronto all'uso, con il nome da mostrare e da scrivere nel pacchetto. */
sealed class Motore(val nome: String) : AutoCloseable {

    /** ML Kit GenAI Speech Recognition (alpha): Gemini Nano o modello di sistema. */
    class MlKit(val client: SpeechRecognizer, nome: String) : Motore(nome) {
        override fun close() = client.close()
    }

    /**
     * Argo locale: Parakeet TDT 0.6B v3 con sherpa-onnx + Silero VAD (negli asset).
     * Il modello (~1,5 GB in memoria) si carica alla prima richiesta e si libera con [close].
     */
    class Locale(val modello: ModelloLocale, private val assets: AssetManager) : Motore(Testi.t(R.string.motore_locale, ModelloLocale.NOME)) {
        private val blocco = Mutex()
        @Volatile private var riconoscitore: OfflineRecognizer? = null

        suspend fun riconoscitore(): OfflineRecognizer = blocco.withLock {
            riconoscitore ?: withContext(Dispatchers.Default) {
                val m = modello
                OfflineRecognizer(
                    null,
                    OfflineRecognizerConfig().apply {
                        featConfig = FeatureConfig().apply { sampleRate = 16_000; featureDim = 80 }
                        modelConfig = OfflineModelConfig().apply {
                            transducer = OfflineTransducerModelConfig().apply {
                                encoder = m.file("encoder.int8.onnx").path
                                decoder = m.file("decoder.int8.onnx").path
                                joiner = m.file("joiner.int8.onnx").path
                            }
                            tokens = m.file("tokens.txt").path
                            modelType = "nemo_transducer"
                            numThreads = 4
                        }
                        decodingMethod = "greedy_search"
                    },
                )
            }.also { riconoscitore = it }
        }

        /** Un VAD nuovo per ogni registrazione o file: i tempi partono da zero. */
        fun nuovoVad(): Vad = Vad(
            assets,
            VadModelConfig().apply {
                sileroVadModelConfig = SileroVadModelConfig().apply {
                    model = "silero_vad.onnx"
                    threshold = 0.5f
                    minSilenceDuration = 0.5f
                    minSpeechDuration = 0.25f
                    windowSize = FINESTRA_VAD
                    maxSpeechDuration = 20f
                }
                sampleRate = 16_000
                numThreads = 1
            },
        )

        override fun close() {
            riconoscitore?.release()
            riconoscitore = null
        }

        companion object {
            const val FINESTRA_VAD = 512
        }
    }
}

/**
 * Sceglie e prepara il motore di trascrizione. Tutto ciò che riguarda le API (ML Kit in alpha, sherpa-onnx)
 * passa da qui o dalle classi [Motore], così un cambio di API tocca pochi file.
 */
object MotoreTrascrizione {

    private const val TAG = "Argo"

    /** Lingue in cui si può registrare un incontro (indipendenti dalla lingua dell'interfaccia). */
    val lingueProposte = listOf("it-IT", "en-US", "en-GB", "de-DE", "fr-FR", "es-ES")

    /** Nome della lingua nella lingua dell'app, es. «Inglese (Regno Unito)», «English (United Kingdom)». */
    fun nomeLingua(codice: String): String {
        val dove = Testi.locale
        return Locale.forLanguageTag(codice).getDisplayName(dove)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(dove) else it.toString() }
            .ifEmpty { codice }
    }

    /**
     * Automatica: Avanzata (Google) se c'è → Argo locale se già scaricato → Base (Google) se pronta →
     * Argo locale da scaricare. Parakeet riconosce da solo la lingua tra le 25 europee.
     */
    suspend fun prepara(lingua: String, preferenza: Modalita, modello: ModelloLocale, assets: AssetManager): StatoModello {
        val diagnosi = mutableListOf<String>()
        fun locale(): StatoModello {
            val motore = Motore.Locale(modello, assets)
            return when (modello.stato.value) {
                ModelloLocale.Stato.Pronto -> StatoModello.Pronto(motore)
                is ModelloLocale.Stato.InDownload -> StatoModello.InDownload(motore)
                else -> StatoModello.DaScaricare(motore)
            }
        }

        when (preferenza) {
            Modalita.LOCALE -> return locale()
            Modalita.AVANZATA, Modalita.BASE -> mlKit(lingua, preferenza, diagnosi)?.let { return it }
            Modalita.AUTOMATICA -> {
                mlKit(lingua, Modalita.AVANZATA, diagnosi)?.let { return it }
                if (modello.pronto) return locale()
                mlKit(lingua, Modalita.BASE, diagnosi, soloSePronto = true)?.let { return it }
                return locale()
            }
        }
        val dettaglio = diagnosi.joinToString("\n").ifEmpty { Testi.t(R.string.nessuna_risposta_mlkit) }
        return StatoModello.NonDisponibile(
            Testi.t(R.string.google_non_disponibile, dettaglio)
        )
    }

    /** null se la modalità non è utilizzabile (il motivo finisce in [diagnosi] e nel Logcat). */
    private suspend fun mlKit(
        lingua: String,
        modalita: Modalita,
        diagnosi: MutableList<String>,
        soloSePronto: Boolean = false,
    ): StatoModello? {
        val client = runCatching { crea(lingua, modalita) }.getOrElse {
            Log.w(TAG, "Creazione client ${modalita.name} ($lingua) fallita", it)
            diagnosi += "${modalita.etichetta}: ${descrivi(it)}"
            return null
        }
        val motore = Motore.MlKit(client, Testi.t(R.string.motore_android, modalita.etichetta.lowercase(Testi.locale)))
        val stato = runCatching { client.checkStatus() }.getOrElse {
            Log.w(TAG, "checkStatus ${modalita.name} ($lingua) fallito", it)
            diagnosi += "${modalita.etichetta}: ${descrivi(it)}"
            null
        }
        Log.i(TAG, "Modalità ${modalita.name} ($lingua): stato $stato")
        val risultato = when (stato) {
            FeatureStatus.AVAILABLE -> StatoModello.Pronto(motore)
            FeatureStatus.DOWNLOADABLE -> StatoModello.DaScaricare(motore).takeUnless { soloSePronto }
            FeatureStatus.DOWNLOADING -> StatoModello.InDownload(motore).takeUnless { soloSePronto }
            null -> null
            else -> {
                diagnosi += Testi.t(R.string.non_supportata_lingua, modalita.etichetta, nomeLingua(lingua))
                null
            }
        }
        if (risultato == null) motore.close()
        return risultato
    }

    /** Scarica il modello del motore: emette la percentuale (0…100) o null se ignota; termina a download finito. */
    fun scarica(motore: Motore): Flow<Int?> = when (motore) {
        is Motore.MlKit -> {
            var totale = 0L
            motore.client.download().map { stato ->
                when (stato) {
                    is DownloadStatus.DownloadStarted -> { totale = stato.bytesToDownload; 0 }
                    is DownloadStatus.DownloadProgress ->
                        if (totale > 0) ((stato.totalBytesDownloaded * 100) / totale).toInt().coerceIn(0, 100) else null
                    is DownloadStatus.DownloadCompleted -> 100
                    is DownloadStatus.DownloadFailed -> throw ErroreTrascrizione(
                        Testi.t(R.string.download_google_fallito, stato.e.message ?: Testi.t(R.string.errore_codice, stato.e.errorCode))
                    )
                }
            }
        }
        is Motore.Locale -> {
            motore.modello.scarica()
            motore.modello.stato.transformWhile { stato ->
                when (stato) {
                    is ModelloLocale.Stato.InDownload -> { emit(stato.percentuale); true }
                    ModelloLocale.Stato.Pronto -> { emit(100); false }
                    is ModelloLocale.Stato.Errore -> throw ErroreTrascrizione(stato.messaggio)
                    ModelloLocale.Stato.Assente -> { emit(null); true }
                }
            }
        }
    }

    /** Tipo, messaggio e codici ML Kit di un errore: serve a capire, non solo a dire «non va». */
    private fun descrivi(errore: Throwable): String {
        val radice = generateSequence(errore) { it.cause }.last()
        val codice = (errore as? GenAiException)?.let { " [codice ${it.errorCode}, AICore ${it.aiCoreErrorCode}]" }.orEmpty()
        val messaggio = errore.message ?: radice.message ?: "senza messaggio"
        val tipo = if (radice !== errore) "${errore.javaClass.simpleName} ← ${radice.javaClass.simpleName}" else errore.javaClass.simpleName
        return "$tipo: $messaggio$codice"
    }

    private fun crea(lingua: String, modalita: Modalita): SpeechRecognizer =
        SpeechRecognition.getClient(speechRecognizerOptions {
            locale = Locale.forLanguageTag(lingua)
            preferredMode = when (modalita) {
                Modalita.BASE -> SpeechRecognizerOptions.Mode.MODE_BASIC
                else -> SpeechRecognizerOptions.Mode.MODE_ADVANCED
            }
        })
}

class ErroreTrascrizione(messaggio: String) : Exception(messaggio)
