package blog.argosnewdreams.argo.trascrizione

import blog.argosnewdreams.argo.Testi
import blog.argosnewdreams.argo.R
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * Il modello di «Argo locale»: NVIDIA Parakeet TDT 0.6B v3 (25 lingue europee, italiano compreso),
 * quantizzato int8 per sherpa-onnx. Licenza CC-BY-4.0.
 *
 * Si scarica una volta da Hugging Face, **a una revisione fissa** e con l'impronta SHA-256 di ogni file:
 * se un file non corrisponde viene cancellato e non si usa. Il download riprende da dove si era fermato.
 * Dopo, la trascrizione funziona offline.
 */
class ModelloLocale(context: Context, private val ambito: CoroutineScope) {

    data class FileModello(val nome: String, val byte: Long, val sha256: String)

    companion object {
        const val NOME = "Parakeet TDT 0.6B v3"
        private const val TAG = "Argo"
        private const val REPO = "csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-v3-int8"
        private const val REVISIONE = "ed5d745623472f97cfc2e6c2916ee248798fc5a8"
        private const val CARTELLA = "parakeet-tdt-0.6b-v3-int8"

        val FILE = listOf(
            FileModello("encoder.int8.onnx", 652_282_294, "1afbe69c1ffdabee94755f42e4daf1d93549131a3da11356bda84fcef0f2c479"),
            FileModello("decoder.int8.onnx", 11_845_274, "0222a86147299d4dee90edf95b6d13827edb42ad8b7c9cf0b2582f030937c0b5"),
            FileModello("joiner.int8.onnx", 6_355_277, "cbd02f3696e9944970111fb439d9c3fd365a80374565f13f2b9c7007d253fc98"),
            FileModello("tokens.txt", 93_939, "d58544679ea4bc6ac563d1f545eb7d474bd6cfa467f0a6e2c1dc1c7d37e3c35d"),
        )
        val BYTE_TOTALI = FILE.sumOf { it.byte }
        val MEGABYTE: Long get() = BYTE_TOTALI / 1_000_000

        fun url(file: FileModello) = "https://huggingface.co/$REPO/resolve/$REVISIONE/${file.nome}"
    }

    sealed interface Stato {
        data object Assente : Stato
        data class InDownload(val byteScaricati: Long) : Stato {
            val percentuale: Int get() = ((byteScaricati * 100) / BYTE_TOTALI).toInt().coerceIn(0, 100)
        }
        data object Pronto : Stato
        data class Errore(val messaggio: String) : Stato
    }

    private val appContext = context.applicationContext
    val cartella = File(context.filesDir, "modelli/$CARTELLA").apply { mkdirs() }
    private val segnaleVerificato = File(cartella, ".verificato")
    private val _stato = MutableStateFlow<Stato>(if (verificato()) Stato.Pronto else Stato.Assente)
    val stato: StateFlow<Stato> = _stato.asStateFlow()
    private var lavoro: Job? = null

    fun file(nome: String) = File(cartella, nome)
    val pronto: Boolean get() = _stato.value == Stato.Pronto

    /** Controllo rapido all'avvio: segnale di verifica presente e dimensioni giuste (lo SHA-256 si fa al download). */
    private fun verificato(): Boolean =
        segnaleVerificato.exists() && FILE.all { file(it.nome).length() == it.byte }

    /** Rete a consumo (dati mobili, hotspot): la UI chiede conferma prima di 670 MB. */
    fun reteAConsumo(): Boolean {
        val cm = appContext.getSystemService(ConnectivityManager::class.java)
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return true
        return !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    /** Avvia (o riprende) il download nell'ambito dell'app: continua anche se chiudi la schermata. */
    fun scarica() {
        if (lavoro?.isActive == true || pronto) return
        _stato.value = Stato.InDownload(0)   // subito: chi osserva non deve vedere un vecchio errore
        lavoro = ambito.launch {
            try {
                withContext(Dispatchers.IO) { scaricaTutto() }
                _stato.value = Stato.Pronto
            } catch (e: Exception) {
                Log.w(TAG, "Download del modello locale non riuscito", e)
                _stato.value = Stato.Errore(e.message ?: Testi.t(R.string.download_non_riuscito))
            }
        }
    }

    fun annulla() {
        lavoro?.cancel()
        _stato.value = Stato.Assente
    }

    /** Cancella il modello (libera ~${MEGABYTE} MB). */
    fun elimina() {
        lavoro?.cancel()
        cartella.listFiles()?.forEach { it.delete() }
        _stato.value = Stato.Assente
    }

    private suspend fun scaricaTutto() {
        val spazio = cartella.usableSpace
        val mancanti = FILE.sumOf { maxOf(0L, it.byte - file(it.nome).length()) }
        if (spazio < mancanti + 200_000_000) {
            throw ErroreTrascrizione(Testi.t(R.string.spazio_insufficiente, mancanti / 1_000_000 + 200, spazio / 1_000_000))
        }
        segnaleVerificato.delete()
        var giaScaricati = 0L
        for (voce in FILE) {
            val destinazione = file(voce.nome)
            if (destinazione.length() == voce.byte && sha256(destinazione) == voce.sha256) {
                giaScaricati += voce.byte
                _stato.value = Stato.InDownload(giaScaricati)
                continue
            }
            destinazione.delete()
            val parziale = File(cartella, "${voce.nome}.part")
            scaricaFile(voce, parziale) { byteFile -> _stato.value = Stato.InDownload(giaScaricati + byteFile) }
            if (sha256(parziale) != voce.sha256) {
                parziale.delete()
                throw ErroreTrascrizione(Testi.t(R.string.impronta_errata, voce.nome))
            }
            parziale.renameTo(destinazione)
            giaScaricati += voce.byte
        }
        segnaleVerificato.writeText(REVISIONE)
    }

    /** Scarica con ripresa (header Range): se il .part c'è già, continua da lì. */
    private suspend fun scaricaFile(voce: FileModello, parziale: File, avanzamento: (Long) -> Unit) {
        var tentativi = 0
        while (true) {
            if (parziale.length() > voce.byte) parziale.delete()   // file parziale corrotto
            val giaPresenti = parziale.length()
            if (giaPresenti == voce.byte) return
            try {
                val connessione = (URL(url(voce)).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 20_000
                    readTimeout = 60_000
                    instanceFollowRedirects = true
                    if (giaPresenti > 0) setRequestProperty("Range", "bytes=$giaPresenti-")
                }
                val codice = connessione.responseCode
                val riprende = codice == HttpURLConnection.HTTP_PARTIAL
                if (codice != HttpURLConnection.HTTP_OK && !riprende) {
                    throw ErroreTrascrizione(Testi.t(R.string.server_risponde, codice))
                }
                var scritti = if (riprende) giaPresenti else 0L
                connessione.inputStream.use { ingresso ->
                    FileOutputStream(parziale, riprende).use { uscita ->
                        val buffer = ByteArray(256 * 1024)
                        while (true) {
                            kotlin.coroutines.coroutineContext.ensureActive()
                            val letti = ingresso.read(buffer)
                            if (letti < 0) break
                            uscita.write(buffer, 0, letti)
                            scritti += letti
                            avanzamento(scritti)
                        }
                    }
                }
                connessione.disconnect()
                if (parziale.length() == voce.byte) return
                throw ErroreTrascrizione(Testi.t(R.string.download_interrotto, voce.nome))
            } catch (e: java.io.IOException) {
                // Rete instabile (fiera, hotel): qualche nuovo tentativo, ripartendo da dove si era arrivati
                if (++tentativi >= 5) throw ErroreTrascrizione(Testi.t(R.string.rete_instabile, voce.nome, e.message.orEmpty()))
                kotlinx.coroutines.delay(3_000L * tentativi)
            }
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered(1 shl 20).use { ingresso ->
            val buffer = ByteArray(1 shl 20)
            while (true) {
                val letti = ingresso.read(buffer)
                if (letti < 0) break
                digest.update(buffer, 0, letti)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
