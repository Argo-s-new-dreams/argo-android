package blog.argosnewdreams.argo.elaborazione

import blog.argosnewdreams.argo.Impostazioni
import blog.argosnewdreams.argo.foto.LettoreFoto
import blog.argosnewdreams.argo.modello.Archivio
import blog.argosnewdreams.argo.modello.OrigineTempi
import blog.argosnewdreams.argo.modello.StatoIncontro
import android.content.res.AssetManager
import blog.argosnewdreams.argo.trascrizione.ModelloLocale
import blog.argosnewdreams.argo.trascrizione.Motore
import blog.argosnewdreams.argo.trascrizione.MotoreTrascrizione
import blog.argosnewdreams.argo.trascrizione.TrascrittoreLocale
import blog.argosnewdreams.argo.trascrizione.StatoModello
import blog.argosnewdreams.argo.trascrizione.TrascrizioneDaFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Dopo lo stop, tutto sul telefono:
 * trascrizione dal file se quella dal vivo non c'è → testo delle foto → audio eliminato.
 * La relazione la scrive Argo sul computer (pacchetto o, dalla 0.2, Ponte).
 */
class Elaborazione(
    private val archivio: Archivio,
    private val impostazioni: Impostazioni,
    private val modelloLocale: ModelloLocale,
    private val assets: AssetManager,
    private val ambito: CoroutineScope,
) {
    private val _fasi = MutableStateFlow<Map<String, String>>(emptyMap())
    /** Incontro → cosa sta facendo adesso, per la UI. */
    val fasi: StateFlow<Map<String, String>> = _fasi.asStateFlow()

    fun avvia(id: String) {
        ambito.launch { esegui(id) }
    }

    suspend fun esegui(id: String) {
        if (_fasi.value.containsKey(id)) return
        fun fase(testo: String) = _fasi.update { it + (id to testo) }
        fase("Preparo…")
        try {
            archivio.aggiorna(id) { it.copy(stato = StatoIncontro.IN_ELABORAZIONE, messaggioErrore = null) }
            var incontro = archivio.cerca(id) ?: return
            val avvisi = mutableListOf<String>()
            incontro.avviso?.let { avvisi += it }

            // 1 · Piano B: trascrizione dal file
            val audio = archivio.fileAudio(id)
            if (!incontro.haTrascrizione && audio.exists()) {
                fase("Trascrivo sul telefono…")
                when (val stato = MotoreTrascrizione.prepara(incontro.lingua, impostazioni.modalita.value, modelloLocale, assets)) {
                    is StatoModello.Pronto -> stato.motore.use { motore ->
                        val esito = runCatching {
                            when (motore) {
                                // Argo locale: stessa pipeline del dal vivo, quindi tempi veri
                                is Motore.Locale -> TrascrittoreLocale.trascriviFile(motore, audio) { fase("Trascrivo sul telefono… $it%") } to OrigineTempi.PAROLE
                                is Motore.MlKit -> TrascrizioneDaFile.trascrivi(motore, audio, incontro.durata) to OrigineTempi.STIMATI
                            }
                        }.getOrElse { errore ->
                            archivio.aggiorna(id) {
                                it.copy(stato = StatoIncontro.ERRORE, messaggioErrore = errore.message)
                            }
                            return
                        }
                        val (segmenti, tempi) = esito
                        incontro = archivio.aggiorna(id) {
                            it.copy(segmenti = segmenti, tempi = tempi, motoreTrascrizione = motore.nome)
                        } ?: return
                        if (tempi == OrigineTempi.STIMATI) avvisi += "Trascritto dopo la registrazione: i tempi di frasi e foto sono stimati."
                    }
                    is StatoModello.DaScaricare, is StatoModello.InDownload -> {
                        archivio.aggiorna(id) {
                            it.copy(
                                stato = StatoIncontro.ERRORE,
                                messaggioErrore = "Il modello di trascrizione non è ancora sul telefono. " +
                                    "Scaricalo da Impostazioni › Trascrizione, poi tocca «Riprova». L'audio resta finché non è trascritto.",
                            )
                        }
                        return
                    }
                    is StatoModello.NonDisponibile -> {
                        archivio.aggiorna(id) { it.copy(stato = StatoIncontro.ERRORE, messaggioErrore = stato.motivo) }
                        return
                    }
                }
            }

            // 2 · Testo delle foto
            val daLeggere = incontro.foto.withIndex().filter { !it.value.letta }
            for ((posizione, voce) in daLeggere.withIndex()) {
                fase("Leggo la foto ${posizione + 1} di ${daLeggere.size}…")
                val (testo, riferimenti) = LettoreFoto.leggi(archivio.fileFoto(id, voce.value.nomeFile))
                incontro = archivio.aggiorna(id) { attuale ->
                    attuale.copy(foto = attuale.foto.map {
                        if (it.nomeFile == voce.value.nomeFile) it.copy(testoOCR = testo, riferimenti = riferimenti, letta = true) else it
                    })
                } ?: return
            }

            // 3 · Nessuna registrazione resta sul telefono
            if (incontro.haTrascrizione && impostazioni.eliminaAudio.value) {
                audio.delete()
                incontro = archivio.aggiorna(id) { it.copy(fileAudio = null) } ?: return
            }

            archivio.aggiorna(id) {
                it.copy(
                    stato = if (it.haTrascrizione || it.foto.isNotEmpty()) StatoIncontro.TRASCRITTO else StatoIncontro.ERRORE,
                    messaggioErrore = if (it.haTrascrizione || it.foto.isNotEmpty()) null else "Non ho trovato parlato nella registrazione.",
                    avviso = avvisi.distinct().joinToString("\n").ifEmpty { null },
                )
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Qualunque imprevisto resta nella scheda dell'incontro, con «Riprova»: mai un'app che si chiude.
            android.util.Log.e("Argo", "Elaborazione dell'incontro $id non riuscita", e)
            runCatching {
                archivio.aggiorna(id) {
                    it.copy(stato = StatoIncontro.ERRORE, messaggioErrore = "Elaborazione non riuscita: ${e.message ?: e.javaClass.simpleName}")
                }
            }
        } finally {
            _fasi.update { it - id }
        }
    }
}
