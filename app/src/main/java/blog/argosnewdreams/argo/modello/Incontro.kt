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
        return scelto.ifEmpty { Etichette.correnti.parlante.format(parlante.drop(1)) }
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
                        val letto = voce.foto.testoOCR.ifBlank { Etichette.correnti.nessunTesto }.replace("\n", " · ")
                        "[${Formato.durata(voce.foto.secondo)}] 📷 ${Etichette.correnti.foto} $numeroFoto: $letto"
                    }
                }
            }
        }

    val titoloMostrato: String get() = titolo.ifBlank { Etichette.correnti.incontroDel.format(Formato.dataBreve(creata)) }

    /** Relazione in Markdown, pronta da condividere (stessa struttura dell'iPhone). */
    fun markdown(nomeApp: String): String = buildString {
        val e = Etichette.correnti
        append("# $titoloMostrato\n\n")
        append("_${Formato.dataLunga(creata)} · ${Formato.durata(durata)}_\n\n")
        if (riassunto.isNotBlank()) append("## ${e.riassunto}\n\n$riassunto\n\n")
        if (contatti.isNotEmpty()) append("## ${e.contatti}\n\n" + contatti.joinToString("\n") { "- ${it.descrizione}" } + "\n\n")
        if (puntiChiave.isNotEmpty()) append("## ${e.puntiChiave}\n\n" + puntiChiave.joinToString("\n") { "- $it" } + "\n\n")
        if (azioni.isNotEmpty()) append("## ${e.azioni}\n\n" + azioni.joinToString("\n") { "- [ ] ${it.testoCompleto}" } + "\n\n")
        if (haTrascrizione || foto.isNotEmpty()) append("## ${e.trascrizione}\n\n$testoCompleto\n")
        val origine = motoreRelazione?.let { e.relazione.format(it) } ?: e.soloTrascrizione
        append("\n---\n" + e.generato.format(nomeApp, origine) + "\n")
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
            scadenza?.takeIf { it.isNotBlank() }?.let { add(Etichette.correnti.entro.format(it)) }
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

/**
 * Etichette dei testi generati dal modello (titolo, relazione, testo condiviso) nella lingua dell'app.
 * I valori predefiniti sono quelli italiani, usati anche nei test; l'app li sostituisce con [blog.argosnewdreams.argo.Testi].
 */
data class Etichette(
    val locale: Locale = Locale.ITALIAN,
    val parlante: String = "Parlante %s",
    val incontroDel: String = "Incontro del %s",
    val nessunTesto: String = "nessun testo leggibile",
    val foto: String = "FOTO",
    val riassunto: String = "Riassunto",
    val contatti: String = "Contatti",
    val puntiChiave: String = "Punti chiave",
    val azioni: String = "Azioni",
    val trascrizione: String = "Trascrizione",
    val relazione: String = "relazione: %s",
    val soloTrascrizione: String = "solo trascrizione",
    val generato: String = "Generato con %1\$s · trascrizione sul dispositivo · %2\$s",
    val entro: String = "entro %s",
    val formatoDataLunga: String = "d MMMM yyyy 'alle' HH:mm",
    val formatoDataBreve: String = "d MMM, HH:mm",
) {
    companion object {
        @Volatile var correnti = Etichette()
    }
}

object Formato {

    fun durata(secondi: Double): String {
        val totale = Math.round(secondi).toInt().coerceAtLeast(0)
        val h = totale / 3600
        val m = (totale % 3600) / 60
        val s = totale % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    fun dataLunga(millis: Long): String =
        DateTimeFormatter.ofPattern(Etichette.correnti.formatoDataLunga, Etichette.correnti.locale)
            .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))

    fun dataBreve(millis: Long): String =
        DateTimeFormatter.ofPattern(Etichette.correnti.formatoDataBreve, Etichette.correnti.locale)
            .format(Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()))
}
