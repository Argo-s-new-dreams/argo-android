package blog.argosnewdreams.argo.modello

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Gli incontri sul telefono, uno per cartella nello spazio privato dell'app:
 *
 *     incontri/<id>/incontro.json
 *     incontri/<id>/foto/foto-….jpg
 *     incontri/<id>/audio.wav        solo finché serve alla trascrizione
 *
 * Niente database: file leggibili, cancellazione completa = cancellare la cartella.
 */
class Archivio(radice: File) {

    private val cartellaIncontri = File(radice, "incontri").apply { mkdirs() }
    private val blocco = Mutex()
    private val _incontri = MutableStateFlow<List<Incontro>>(emptyList())
    val incontri: StateFlow<List<Incontro>> = _incontri.asStateFlow()

    /** Da chiamare una volta all'avvio, fuori dal thread principale. */
    suspend fun carica() = blocco.withLock {
        val letti = cartellaIncontri.listFiles().orEmpty().mapNotNull { cartella ->
            val file = File(cartella, NOME_JSON)
            if (!file.exists()) {
                // Registrazione interrotta a metà (app chiusa dal sistema): niente audio orfano sul telefono.
                cartella.deleteRecursively()
                return@mapNotNull null
            }
            runCatching { json.decodeFromString<Incontro>(file.readText()) }.getOrNull()
        }
        _incontri.value = letti.sortedByDescending { it.creata }
    }

    fun cerca(id: String): Incontro? = _incontri.value.firstOrNull { it.id == id }

    suspend fun salva(incontro: Incontro) = blocco.withLock { scrivi(incontro) }

    /** Modifica atomica: legge l'ultima versione, applica la modifica, salva. */
    suspend fun aggiorna(id: String, modifica: (Incontro) -> Incontro): Incontro? = blocco.withLock {
        val attuale = _incontri.value.firstOrNull { it.id == id } ?: return@withLock null
        modifica(attuale).also { scrivi(it) }
    }

    suspend fun elimina(id: String) = blocco.withLock {
        cartella(id).deleteRecursively()
        _incontri.value = _incontri.value.filterNot { it.id == id }
    }

    fun cartella(id: String) = File(cartellaIncontri, id)
    fun cartellaFoto(id: String) = File(cartella(id), "foto").apply { mkdirs() }
    fun fileFoto(id: String, nome: String) = File(cartellaFoto(id), nome)
    fun fileAudio(id: String) = File(cartella(id), NOME_AUDIO)

    private fun scrivi(incontro: Incontro) {
        val cartella = cartella(incontro.id).apply { mkdirs() }
        // Scrittura atomica: mai un incontro.json a metà se il telefono si spegne.
        val provvisorio = File(cartella, "$NOME_JSON.tmp")
        provvisorio.writeText(json.encodeToString(Incontro.serializer(), incontro))
        provvisorio.renameTo(File(cartella, NOME_JSON))
        _incontri.value = (_incontri.value.filterNot { it.id == incontro.id } + incontro)
            .sortedByDescending { it.creata }
    }

    companion object {
        const val NOME_JSON = "incontro.json"
        const val NOME_AUDIO = "audio.wav"
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }
    }
}
