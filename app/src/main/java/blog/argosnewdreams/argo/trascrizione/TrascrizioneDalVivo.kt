package blog.argosnewdreams.argo.trascrizione

import kotlinx.coroutines.flow.StateFlow

/**
 * Trascrizione mentre si registra: riceve lo stesso PCM 16 kHz mono del file WAV e produce segmenti
 * sulla linea del tempo della registrazione. Due implementazioni: [TrascrittoreMlKit] e [TrascrittoreLocale].
 */
interface TrascrizioneDalVivo {
    /** Testo provvisorio della frase in corso, da mostrare in grigio. */
    val parziale: StateFlow<String>
    val errore: StateFlow<String?>
    /** Quanti segmenti sono arrivati: se resta 0 si ritrascrive dal file a fine registrazione. */
    val segmentiRicevuti: Int

    fun avvia()
    /** Blocco PCM 16 bit little-endian, 16 kHz mono. Non deve mai bloccare il registratore. */
    fun scrivi(blocco: ByteArray)
    suspend fun sospendi()
    fun riprendi()
    /** Fine registrazione: aspetta che l'ultimo audio sia trascritto. */
    suspend fun chiudi()
    /** Registrazione scartata: si ferma subito, senza finire la coda. */
    suspend fun interrompi() = chiudi()
}
