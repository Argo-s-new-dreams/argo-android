package blog.argosnewdreams.argo.trascrizione

import blog.argosnewdreams.argo.modello.Segmento

/** Una parola riconosciuta con il suo istante (ms dall'inizio della sessione di riconoscimento). */
data class Parola(val testo: String, val ms: Long)

/**
 * Dai risultati del riconoscimento ai segmenti sulla linea del tempo della registrazione.
 * Logica pura, senza Android: si prova con i test.
 */
object Segmentatore {

    /** Oltre queste parole un risultato lungo viene spezzato a fine frase. */
    private const val PAROLE_MASSIME = 28
    private const val SECONDI_PER_CARATTERE = 0.065

    /**
     * Segmenti da un risultato finale con i tempi delle parole.
     *
     * @param offset secondi della registrazione in cui è partita la sessione di riconoscimento
     * @param adesso secondi registrati al momento del risultato (limite superiore plausibile)
     * @param fineUltimo fine dell'ultimo segmento già scritto (limite inferiore plausibile)
     */
    fun daParole(
        testo: String,
        parole: List<Parola>,
        offset: Double,
        adesso: Double,
        fineUltimo: Double,
    ): List<Segmento> {
        val pulito = testo.trim()
        if (pulito.isEmpty()) return emptyList()
        if (parole.isEmpty()) return listOf(stimato(pulito, adesso, fineUltimo))

        val primo = offset + parole.first().ms / 1000.0
        val ultimo = offset + parole.last().ms / 1000.0
        // La documentazione non chiarisce se i tempi partono dalla sessione o da altro:
        // se cadono fuori dall'audio registrato, meglio una stima onesta che un tempo sbagliato.
        val plausibile = primo >= fineUltimo - 2.0 && ultimo <= adesso + 2.0 && ultimo >= primo
        if (!plausibile) return listOf(stimato(pulito, adesso, fineUltimo))

        return spezza(parole).map { gruppo ->
            val inizio = offset + gruppo.first().ms / 1000.0
            val coda = gruppo.last()
            val fine = (offset + coda.ms / 1000.0 + durataParola(coda.testo)).coerceAtMost(adesso)
            Segmento(
                inizio = arrotonda(inizio),
                fine = arrotonda(maxOf(fine, inizio)),
                testo = gruppo.joinToString(" ") { it.testo.trim() }.replace(Regex("\\s+([,.;:!?])"), "$1"),
            )
        }.ifEmpty { listOf(stimato(pulito, adesso, fineUltimo)) }
    }

    /** Senza tempi delle parole: il testo finisce adesso e dura quanto serve a pronunciarlo. */
    fun stimato(testo: String, adesso: Double, fineUltimo: Double): Segmento {
        val durata = testo.length * SECONDI_PER_CARATTERE
        val inizio = maxOf(fineUltimo, adesso - durata).coerceAtMost(adesso)
        return Segmento(arrotonda(inizio), arrotonda(adesso), testo.trim())
    }

    /**
     * Trascrizione fatta dopo, dal file, senza tempi: distribuisce i testi sulla durata
     * in proporzione alla lunghezza. Serve solo a tenere l'ordine con le foto.
     */
    fun distribuisci(testi: List<String>, durata: Double): List<Segmento> {
        val validi = testi.map { it.trim() }.filter { it.isNotEmpty() }
        val totale = validi.sumOf { it.length }.takeIf { it > 0 } ?: return emptyList()
        var cursore = 0.0
        return validi.map { testo ->
            val quota = durata * testo.length / totale
            Segmento(arrotonda(cursore), arrotonda(cursore + quota), testo).also { cursore += quota }
        }
    }

    /** Gruppi di parole: si spezza a fine frase quando il risultato è lungo. */
    internal fun spezza(parole: List<Parola>): List<List<Parola>> {
        val validi = parole.filter { it.testo.isNotBlank() }
        if (validi.size <= PAROLE_MASSIME) return listOf(validi).filter { it.isNotEmpty() }
        val gruppi = mutableListOf<MutableList<Parola>>(mutableListOf())
        for (parola in validi) {
            gruppi.last().add(parola)
            val fineFrase = parola.testo.trimEnd().lastOrNull() in setOf('.', '?', '!')
            if (fineFrase && gruppi.last().size >= 6) gruppi.add(mutableListOf())
            else if (gruppi.last().size >= PAROLE_MASSIME * 2) gruppi.add(mutableListOf())
        }
        return gruppi.filter { it.isNotEmpty() }
    }

    private fun durataParola(parola: String) = (parola.length * SECONDI_PER_CARATTERE).coerceIn(0.2, 1.0)
    private fun arrotonda(secondi: Double) = Math.round(secondi * 100) / 100.0
}
