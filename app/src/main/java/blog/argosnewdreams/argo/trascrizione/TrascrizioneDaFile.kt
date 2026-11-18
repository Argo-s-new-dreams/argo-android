package blog.argosnewdreams.argo.trascrizione

import blog.argosnewdreams.argo.modello.Segmento
import com.google.mlkit.genai.common.audio.AudioSource
import com.google.mlkit.genai.speechrecognition.SpeechRecognizerResponse
import com.google.mlkit.genai.speechrecognition.speechRecognizerRequest
import java.io.File

/**
 * Piano B: se la trascrizione dal vivo non ha prodotto nulla (modello non pronto all'avvio,
 * errore a metà), si trascrive il WAV intero a fine registrazione. ML Kit in questo caso
 * non dà i tempi delle parole: i segmenti vengono distribuiti sulla durata (tempi stimati).
 */
object TrascrizioneDaFile {

    suspend fun trascrivi(motore: Motore.MlKit, file: File, durata: Double): List<Segmento> {
        val sorgente = AudioSource.fromFile(file)
        val testi = mutableListOf<String>()
        var errore: Exception? = null
        try {
            motore.client.startRecognition(speechRecognizerRequest { audioSource = sorgente }).collect { risposta ->
                when (risposta) {
                    is SpeechRecognizerResponse.FinalTextResponse -> testi += risposta.text
                    is SpeechRecognizerResponse.ErrorResponse -> errore = risposta.e
                    else -> Unit
                }
            }
        } finally {
            runCatching { sorgente.close() }
        }
        val segmenti = Segmentatore.distribuisci(testi, durata)
        if (segmenti.isEmpty()) {
            throw ErroreTrascrizione(errore?.message?.let { "Trascrizione non riuscita: $it" } ?: "Non ho trovato parlato nella registrazione.")
        }
        return segmenti
    }
}
