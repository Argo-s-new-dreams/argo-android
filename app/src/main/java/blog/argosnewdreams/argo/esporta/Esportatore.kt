package blog.argosnewdreams.argo.esporta

import blog.argosnewdreams.argo.modello.Incontro
import blog.argosnewdreams.argo.modello.StatoIncontro
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.text.Normalizer
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Il «pacchetto Argo» (formato `argo.sessione` v1, lo stesso di Argo per iPhone):
 *
 *     Argo-2026-11-18-0958-Incontro-stand-Rossi.zip
 *     └── Argo-2026-11-18-0958-Incontro-stand-Rossi/
 *         ├── sessione.json
 *         ├── relazione.md
 *         └── foto/01.jpg, 02.jpg…
 *
 * L'audio non c'è mai. Lo leggono Argo desktop (Incontri › Importa pacchetto, o il Ponte) e,
 * più avanti, il connettore per Dynamics 365 Sales.
 */
object Esportatore {

    const val VERSIONE_FORMATO = 1

    // Come Swift (Codable): i campi nulli si omettono, es. `parlante` senza divisione delle voci.
    private val json = Json { prettyPrint = true; encodeDefaults = true; explicitNulls = false }

    /**
     * Crea lo zip in [destinazione] (cartella) e lo restituisce.
     * @param cartellaFoto dove si trovano i file elencati in `incontro.foto`
     * @param nomeApp es. «Argo per Android 0.1.0»
     */
    fun pacchetto(incontro: Incontro, cartellaFoto: File, destinazione: File, nomeApp: String): File {
        val nome = nomeCartella(incontro)
        destinazione.mkdirs()
        val zip = File(destinazione, "$nome.zip")

        val fotoOrdinate = incontro.foto.sortedBy { it.secondo }
        val fotoEsportate = fotoOrdinate.mapIndexed { indice, foto ->
            PacchettoArgo.Foto(
                file = "foto/%02d.jpg".format(indice + 1),
                secondo = foto.secondo,
                testo = foto.testoOCR,
                riferimenti = foto.riferimenti,
            )
        }

        val contenuto = PacchettoArgo(
            formato = "argo.sessione",
            versioneFormato = VERSIONE_FORMATO,
            app = nomeApp,
            id = incontro.id,
            creata = iso8601(incontro.creata),
            titolo = incontro.titoloMostrato,
            lingua = incontro.lingua,
            durata = incontro.durata,
            partecipanti = incontro.parlanti.map { PacchettoArgo.Partecipante(it, incontro.nome(it) ?: it) },
            segmenti = incontro.segmenti.map { PacchettoArgo.Segmento(it.inizio, it.fine, it.parlante, it.testo) },
            foto = fotoEsportate,
            relazione = if (incontro.stato == StatoIncontro.COMPLETATO) PacchettoArgo.Relazione(
                motore = incontro.motoreRelazione,
                riassunto = incontro.riassunto,
                puntiChiave = incontro.puntiChiave,
                azioni = incontro.azioni.map { PacchettoArgo.Azione(it.descrizione, it.responsabile, it.scadenza) },
                contatti = incontro.contatti.map {
                    PacchettoArgo.Contatto(it.nome, it.azienda, it.ruolo, it.email, it.telefono, it.note)
                },
            ) else null,
            trascrizioneLegacy = null,
        )

        ZipOutputStream(zip.outputStream().buffered()).use { uscita ->
            fun voce(percorso: String, dati: ByteArray) {
                uscita.putNextEntry(ZipEntry("$nome/$percorso"))
                uscita.write(dati)
                uscita.closeEntry()
            }
            voce("sessione.json", json.encodeToString(PacchettoArgo.serializer(), contenuto).toByteArray())
            voce("relazione.md", incontro.markdown(nomeApp).toByteArray())
            fotoOrdinate.zip(fotoEsportate).forEach { (foto, esportata) ->
                val file = File(cartellaFoto, foto.nomeFile)
                if (file.exists()) voce(esportata.file, file.readBytes())
            }
        }
        return zip
    }

    /** «Argo-AAAA-MM-GG-HHMM-Titolo-in-poche-parole», senza accenti né simboli. */
    fun nomeCartella(incontro: Incontro, zona: ZoneId = ZoneId.systemDefault()): String {
        val data = DateTimeFormatter.ofPattern("yyyy-MM-dd-HHmm").format(Instant.ofEpochMilli(incontro.creata).atZone(zona))
        val parole = Normalizer.normalize(incontro.titolo, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .split(Regex("[^A-Za-z0-9]+"))
            .filter { it.isNotEmpty() }
            .take(6)
        return "Argo-$data" + if (parole.isEmpty()) "" else "-" + parole.joinToString("-")
    }

    /** Come `JSONEncoder.dateEncodingStrategy = .iso8601` su iPhone: UTC, secondi, «Z». */
    fun iso8601(millis: Long): String =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").format(Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC))
}

/** Contenuto di sessione.json (versioneFormato 1): nomi dei campi identici ad Argo per iPhone. */
@Serializable
data class PacchettoArgo(
    val formato: String,
    val versioneFormato: Int,
    val app: String,
    val id: String,
    val creata: String,
    val titolo: String,
    val lingua: String,
    val durata: Double,
    val partecipanti: List<Partecipante>,
    val segmenti: List<Segmento>,
    val foto: List<Foto>,
    val relazione: Relazione?,
    val trascrizioneLegacy: String?,
) {
    @Serializable data class Partecipante(val id: String, val nome: String)
    @Serializable data class Segmento(val inizio: Double, val fine: Double, val parlante: String?, val testo: String)
    @Serializable data class Foto(val file: String, val secondo: Double, val testo: String, val riferimenti: List<String>)
    @Serializable data class Azione(val descrizione: String, val responsabile: String?, val scadenza: String?)
    @Serializable data class Contatto(
        val nome: String, val azienda: String?, val ruolo: String?,
        val email: String?, val telefono: String?, val note: String?,
    )
    @Serializable data class Relazione(
        val motore: String?, val riassunto: String, val puntiChiave: List<String>,
        val azioni: List<Azione>, val contatti: List<Contatto>,
    )
}
