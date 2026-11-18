package blog.argosnewdreams.argo.trascrizione

/**
 * Parakeet restituisce pezzi di parola (SentencePiece) con il loro istante: « Il», « B», «ud», «d», «h», «is», «mo».
 * Un pezzo che inizia con uno spazio (o con «▁») apre una parola nuova. Logica pura, provata nei test.
 */
object ParoleDaToken {

    /** @param secondi istante di ogni token, in secondi dall'inizio del pezzo di audio */
    fun raggruppa(token: List<String>, secondi: List<Float>): List<Parola> {
        val parole = mutableListOf<Parola>()
        var testo = StringBuilder()
        var inizio = 0f
        token.forEachIndexed { i, grezzo ->
            val pezzo = grezzo.replace('▁', ' ')
            val tempo = secondi.getOrElse(i) { secondi.lastOrNull() ?: 0f }
            val nuova = pezzo.startsWith(" ") || testo.isEmpty()
            // La punteggiatura staccata (« ,» o «.») resta attaccata alla parola precedente
            val soloPunteggiatura = pezzo.isNotBlank() && pezzo.trim().all { !it.isLetterOrDigit() }
            if (nuova && !(soloPunteggiatura && testo.isNotEmpty())) {
                if (testo.isNotBlank()) parole += Parola(testo.toString().trim(), (inizio * 1000).toLong())
                testo = StringBuilder()
                inizio = tempo
            }
            testo.append(if (soloPunteggiatura) pezzo.trim() else pezzo)
        }
        if (testo.isNotBlank()) parole += Parola(testo.toString().trim(), (inizio * 1000).toLong())
        return parole
    }
}
