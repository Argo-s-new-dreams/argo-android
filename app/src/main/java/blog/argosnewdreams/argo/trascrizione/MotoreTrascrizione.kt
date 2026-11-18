package blog.argosnewdreams.argo.trascrizione

import android.util.Log
import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.speechrecognition.SpeechRecognition
import com.google.mlkit.genai.speechrecognition.SpeechRecognizer
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerOptions
import com.google.mlkit.genai.speechrecognition.speechRecognizerOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale

/** Quale modello usare per trascrivere: tutti girano sul telefono. */
enum class Modalita(val etichetta: String, val descrizione: String) {
    AUTOMATICA("Automatica", "Avanzata se il telefono la supporta, altrimenti Base"),
    AVANZATA("Avanzata", "Gemini Nano: più precisa, solo telefoni recenti (es. Galaxy S26)"),
    BASE("Base", "Modello vocale di sistema, Android 12 e successivi; l'italiano è in beta"),
}

/** Lo stato del modello per una lingua. */
sealed interface StatoModello {
    data class Pronto(val motore: Motore) : StatoModello
    data class DaScaricare(val motore: Motore) : StatoModello
    data class InDownload(val motore: Motore) : StatoModello
    data class NonDisponibile(val motivo: String) : StatoModello
}

/** Un client ML Kit già configurato, con il nome da mostrare e da scrivere nel pacchetto. */
class Motore(val client: SpeechRecognizer, val nome: String) : AutoCloseable {
    override fun close() = client.close()
}

/**
 * Sceglie e prepara il modello di trascrizione di ML Kit GenAI (API in alpha: tutto ciò che la
 * riguarda passa da qui, così un cambio di API tocca un solo file).
 */
object MotoreTrascrizione {

    private const val TAG = "Argo"

    val lingueProposte = listOf(
        "it-IT" to "Italiano",
        "en-US" to "Inglese (USA)",
        "en-GB" to "Inglese (UK)",
        "de-DE" to "Tedesco",
        "fr-FR" to "Francese",
        "es-ES" to "Spagnolo",
    )

    fun nomeLingua(codice: String) = lingueProposte.firstOrNull { it.first == codice }?.second ?: codice

    /** Prova le modalità in ordine e restituisce la prima utilizzabile (o da scaricare). */
    suspend fun prepara(lingua: String, preferenza: Modalita): StatoModello {
        val tentativi = when (preferenza) {
            Modalita.AUTOMATICA -> listOf(Modalita.AVANZATA, Modalita.BASE)
            else -> listOf(preferenza)
        }
        // Diagnosi per modalità: cosa ha risposto ML Kit, anche quando l'errore non ha un messaggio.
        val diagnosi = mutableListOf<String>()
        for (modalita in tentativi) {
            val client = runCatching { crea(lingua, modalita) }.getOrElse {
                Log.w(TAG, "Creazione client ${modalita.name} ($lingua) fallita", it)
                diagnosi += "${modalita.etichetta}: ${descrivi(it)}"
                null
            } ?: continue
            val motore = Motore(client, "Android · ${modalita.etichetta.lowercase()}")
            val stato = runCatching { client.checkStatus() }.getOrElse {
                Log.w(TAG, "checkStatus ${modalita.name} ($lingua) fallito", it)
                diagnosi += "${modalita.etichetta}: ${descrivi(it)}"
                null
            }
            Log.i(TAG, "Modalità ${modalita.name} ($lingua): stato $stato")
            when (stato) {
                FeatureStatus.AVAILABLE -> return StatoModello.Pronto(motore)
                FeatureStatus.DOWNLOADABLE -> return StatoModello.DaScaricare(motore)
                FeatureStatus.DOWNLOADING -> return StatoModello.InDownload(motore)
                null -> motore.close()
                else -> {
                    motore.close()
                    diagnosi += "${modalita.etichetta}: non supportata per ${nomeLingua(lingua)} su questo telefono"
                }
            }
        }
        val dettaglio = diagnosi.joinToString("\n").ifEmpty { "nessuna risposta da ML Kit" }
        return StatoModello.NonDisponibile("La trascrizione sul dispositivo non è disponibile.\n$dettaglio")
    }

    /** Tipo, messaggio e codici ML Kit di un errore: serve a capire, non solo a dire «non va». */
    private fun descrivi(errore: Throwable): String {
        val radice = generateSequence(errore) { it.cause }.last()
        val codice = (errore as? GenAiException)?.let { " [codice ${it.errorCode}, AICore ${it.aiCoreErrorCode}]" }.orEmpty()
        val messaggio = errore.message ?: radice.message ?: "senza messaggio"
        val tipo = if (radice !== errore) "${errore.javaClass.simpleName} ← ${radice.javaClass.simpleName}" else errore.javaClass.simpleName
        return "$tipo: $messaggio$codice"
    }

    /** Scarica il modello: emette la percentuale (0…100) o null se il totale è ignoto; termina a download finito. */
    fun scarica(motore: Motore): Flow<Int?> {
        var totale = 0L
        return motore.client.download().map { stato ->
            when (stato) {
                is DownloadStatus.DownloadStarted -> { totale = stato.bytesToDownload; 0 }
                is DownloadStatus.DownloadProgress ->
                    if (totale > 0) ((stato.totalBytesDownloaded * 100) / totale).toInt().coerceIn(0, 100) else null
                is DownloadStatus.DownloadCompleted -> 100
                is DownloadStatus.DownloadFailed -> throw ErroreTrascrizione(
                    "Download del modello non riuscito (${stato.e.message ?: "errore ${stato.e.errorCode}"}). Serve una connessione: riprova dalle Impostazioni."
                )
            }
        }
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
