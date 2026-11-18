package blog.argosnewdreams.argo

import blog.argosnewdreams.argo.trascrizione.Parola
import blog.argosnewdreams.argo.trascrizione.ParoleDaToken
import org.junit.Assert.assertEquals
import org.junit.Test

class ParoleDaTokenTest {

    @Test
    fun `pezzi di parola reali di Parakeet`() {
        // Dalla prova con FLEURS: «Il Buddhismo tibetano è»
        val token = listOf(" Il", " B", "ud", "d", "h", "is", "mo", " t", "ib", "et", "ano", " è")
        val tempi = listOf(0.0f, 0.16f, 0.24f, 0.4f, 0.48f, 0.56f, 0.64f, 0.8f, 0.96f, 1.2f, 1.44f, 1.76f)
        assertEquals(
            listOf(Parola("Il", 0), Parola("Buddhismo", 160), Parola("tibetano", 800), Parola("è", 1760)),
            ParoleDaToken.raggruppa(token, tempi),
        )
    }

    @Test
    fun `punteggiatura attaccata alla parola e marcatore SentencePiece`() {
        val token = listOf("▁Sì", ",", "▁per", "▁il", "▁controllo", ".")
        val tempi = listOf(0.1f, 0.3f, 0.4f, 0.6f, 0.8f, 1.3f)
        assertEquals(
            listOf("Sì,", "per", "il", "controllo."),
            ParoleDaToken.raggruppa(token, tempi).map { it.testo },
        )
    }

    @Test
    fun `primo token senza spazio e tempi mancanti`() {
        val parole = ParoleDaToken.raggruppa(listOf("Buon", "giorno", " a", " tutti"), listOf(0.5f, 0.7f))
        assertEquals(listOf("Buongiorno", "a", "tutti"), parole.map { it.testo })
        assertEquals(500L, parole[0].ms)
        assertEquals(700L, parole[2].ms)
    }
}
