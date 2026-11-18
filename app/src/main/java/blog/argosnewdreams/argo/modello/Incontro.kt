package blog.argosnewdreams.argo.modello

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/**
 * Un incontro: registrazione, foto scattate durante, trascrizione con i parlanti e (più avanti) relazione.
 * Specchio di `Sessione` di Argo per iPhone: stessi concetti, stessi nomi, stesso pacchetto in uscita.
 */
@Serializable
data class Incontro(
    val id: String = UUID.randomUUID().toString().uppercase(),
    /** Inizio della registrazione, millisecondi epoch. */
    val creata: Long = System.currentTimeMillis(),
    val titolo: String = "",
    /** BCP-47, es. it-IT. */
    val lingua: String = "it-IT",
    /** Secondi registrati (le pause non contano). */
    val durata: Double = 0.0,
    val segmenti: List<Segmento> = emptyList(),
    val foto: List<FotoIncontro> = emptyList(),
    /** Id parlante (P1, P2…) → nome scelto dall'utente. Si riempie dalla 0.2 con la divisione delle voci. */
    val nomiParlanti: Map<String, String> = emptyMap(),
    val riassunto: String = "",
    val puntiChiave: List<String> = emptyList(),
    val azioni: List<Azione> = emptyList(),
    val contatti: List<Contatto> = emptyList(),
    val motoreRelazione: String? = null,
    val stato: StatoIncontro = StatoIncontro.REGISTRATO,
    /** Come sono stati calcolati i tempi dei segmenti: dal vivo (parola per parola) o stimati dopo. */
    val tempi: OrigineTempi = OrigineTempi.PAROLE,
    /** Motore che ha trascritto, es. «Android · avanzata». */
    val motoreTrascrizione: String? = null,
    val messaggioErrore: String? = null,
    /** Informazione non bloccante. */
    val avviso: String? = null,
    /** Nome del file audio ancora presente (solo finché serve all'elaborazione). */
    val fileAudio: String? = null,
    val inviatoAdArgo: Long? = null,
) {
    val haTrascrizione: Boolean get() = segmenti.isNotEmpty()

    /** Parlanti in ordine di prima apparizione. */
    val parlanti: List<String> get() = segmenti.mapNotNull { it.parlante }.distinct()

    fun nome(parlante: String?): String? {
        if (parlante == null) return null
        val scelto = nomiParlanti[parlante]?.trim().orEmpty()
        return scelto.ifEmpty { "Parlante ${parlante.drop(1)}" }
    }

    /** Parole e foto in ordine di tempo. */
    val cronologia: List<VoceCronologia>
        get() = (segmenti.map { VoceCronologia.Parole(it) } + foto.map { VoceCronologia.Scatto(it) })
            .sortedBy { it.secondo }

    /** Testo per il modello e per l'esportazione: parlanti con i loro nomi, foto con il testo letto. */
    val testoCompleto: String
        get() {
            var numeroFoto = 0
            return cronologia.joinToString("\n") { voce ->
                when (voce) {
                    is VoceCronologia.Parole -> {
                        val chi = nome(voce.segmento.parlante)?.let { "$it: " }.orEmpty()
                        "[${Formato.durata(voce.segmento.inizio)}] $chi${voce.segmento.testo}"
                    }
                    is VoceCronologia.Scatto -> {
                        numeroFoto++
                        val letto = voce.foto.testoOCR.ifBlank { "nessun testo leggibile" }.replace("\n", " · ")
                        "[${Formato.durata(voce.foto.secondo)}] 📷 FOTO $numeroFoto: $letto"
                    }
                }
            }
        }

    val titoloMostrato: String get() = titolo.ifBlank { "Incontro del ${Formato.dataBreve(creata)}" }

    /** Relazione in Markdown, pronta da condividere (stessa struttura dell'iPhone). */
    fun markdown(nomeApp: String): String = buildString {
        append("# $titoloMostrato\n\n")
        append("_${Formato.dataLunga(creata)} · ${Formato.durata(durata)}_\n\n")
        if (riassunto.isNotBlank()) append("## Riassunto\n\n$riassunto\n\n")
        if (contatti.isNotEmpty()) append("## Contatti\n\n" + contatti.joinToString("\n") { "- ${it.descrizione}" } + "\n\n")
        if (puntiChiave.isNotEmpty()) append("## Punti chiave\n\n" + puntiChiave.joinToString("\n") { "- $it" } + "\n\n")
        if (azioni.isNotEmpty()) append("## Azioni\n\n" + azioni.joinToString("\n") { "- [ ] ${it.testoCompleto}" } + "\n\n")
        if (haTrascrizione || foto.isNotEmpty()) append("## Trascrizione\n\n$testoCompleto\n")
        val origine = motoreRelazione?.let { "relazione: $it" } ?: "solo trascrizione"
        append("\n---\nGenerato con $nomeApp · trascrizione sul dispositivo · $origine\n")
    }
}

@Serializable
data class Segmento(
    val inizio: Double,
    val fine: Double,
    val testo: String,
    val parlante: String? = null,
)

@Serializable
data class FotoIncontro(
    val nomeFile: String,
    /** Secondo della registrazione in cui è stata scattata. */
    val secondo: Double,
    val testoOCR: String = "",
    /** Telefoni, email, siti riconosciuti nel testo. */
    val riferimenti: List<String> = emptyList(),
    val letta: Boolean = false,
)

sealed interface VoceCronologia {
    val secondo: Double

    data class Parole(val segmento: Segmento) : VoceCronologia {
        override val secondo get() = segmento.inizio
    }

    data class Scatto(val foto: FotoIncontro) : VoceCronologia {
        override val secondo get() = foto.secondo
    }
}

@Serializable
data class Azione(val descrizione: String, val responsabile: String? = null, val scadenza: String? = null) {
    val testoCompleto: String
        get() = buildList {
            add(descrizione)
            responsabile?.takeIf { it.isNotBlank() }?.let { add("($it)") }
            scadenza?.takeIf { it.isNotBlank() }?.let { add("entro $it") }
        }.joinToString(" ")
}

@Serializable
data class Contatto(
    val nome: String,
    val azienda: String? = null,
    val ruolo: String? = null,
    val email: String? = null,
    val telefono: String? = null,
    val note: String? = null,
) {
    val descrizione: String
        get() {
            val dettagli = listOf(ruolo, azienda, email, telefono, note).filterNot { it.isNullOrBlank() }
            return if (dettagli.isEmpty()) nome else "$nome · " + dettagli.joinToString(" · ")
        }
}

@Serializable
enum class StatoIncontro { REGISTRATO, IN_ELABORAZIONE, TRASCRITTO, COMPLETATO, ERRORE }

@Serializable
enum class OrigineTempi { PAROLE, STIMATI }

object Formato {
    private val italiano = Locale.ITALIAN

    fun durata(secondi: Double): String {
        val totale = Math.round(secondi).toInt().coerceAtLeast(0)
        val h = totale / 3600
        val m = (totale % 3600) / 60
        val s = totale % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    fun dataLunga(millis: Long): String =
        DateTimeFormatter.ofPattern("d MMMM yyyy 'alle' HH:mm", italiano)
            .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

    fun dataBreve(millis: Long): String =
        DateTimeFormatter.ofPattern("d MMM, HH:mm", italiano)
            .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
}
