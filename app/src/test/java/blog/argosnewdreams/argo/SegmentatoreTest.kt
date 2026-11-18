package blog.argosnewdreams.argo

import blog.argosnewdreams.argo.trascrizione.Parola
import blog.argosnewdreams.argo.trascrizione.Segmentatore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SegmentatoreTest {

    @Test
    fun `tempi delle parole con offset della sessione`() {
        val parole = listOf(Parola("Buongiorno,", 800), Parola("vedo", 1400), Parola("che", 1700), Parola("fate", 2000), Parola("visione.", 2600))
        val segmenti = Segmentatore.daParole("Buongiorno, vedo che fate visione.", parole, offset = 30.0, adesso = 34.0, fineUltimo = 29.0)
        assertEquals(1, segmenti.size)
        assertEquals(30.8, segmenti[0].inizio, 0.001)
        assertTrue(segmenti[0].fine in 32.6..34.0)
        assertEquals("Buongiorno, vedo che fate visione.", segmenti[0].testo)
    }

    @Test
    fun `tempi fuori dall'audio registrato diventano una stima`() {
        // es. tempi dall'epoch o da un'origine diversa: non devono finire a minuti sbagliati
        val parole = listOf(Parola("ciao", 900_000), Parola("a", 900_500), Parola("tutti", 901_000))
        val segmenti = Segmentatore.daParole("ciao a tutti", parole, offset = 0.0, adesso = 12.0, fineUltimo = 10.0)
        assertEquals(1, segmenti.size)
        assertEquals(12.0, segmenti[0].fine, 0.001)
        assertTrue(segmenti[0].inizio >= 10.0)
    }

    @Test
    fun `senza tempi il segmento finisce adesso e non sovrappone il precedente`() {
        val s = Segmentatore.stimato("Sì, per il controllo qualità sulle linee.", adesso = 20.0, fineUltimo = 18.5)
        assertEquals(20.0, s.fine, 0.001)
        assertEquals(18.5, s.inizio, 0.001)
    }

    @Test
    fun `risultato lungo spezzato a fine frase`() {
        val testo = (1..40).joinToString(" ") { if (it % 10 == 0) "parola$it." else "parola$it" }
        val parole = testo.split(" ").mapIndexed { i, p -> Parola(p, i * 300L) }
        val segmenti = Segmentatore.daParole(testo, parole, offset = 0.0, adesso = 13.0, fineUltimo = 0.0)
        assertTrue("attesi più segmenti, trovati ${segmenti.size}", segmenti.size >= 2)
        assertTrue(segmenti.zipWithNext().all { (a, b) -> a.inizio < b.inizio })
        assertEquals(testo, segmenti.joinToString(" ") { it.testo })
    }

    @Test
    fun `distribuzione proporzionale per la trascrizione da file`() {
        val segmenti = Segmentatore.distribuisci(listOf("aaaa", "", "aaaaaaaaaaaa"), durata = 16.0)
        assertEquals(2, segmenti.size)
        assertEquals(0.0, segmenti[0].inizio, 0.001)
        assertEquals(4.0, segmenti[0].fine, 0.001)
        assertEquals(16.0, segmenti[1].fine, 0.001)
    }

    @Test
    fun `testo vuoto nessun segmento`() {
        assertTrue(Segmentatore.daParole("  ", emptyList(), 0.0, 5.0, 0.0).isEmpty())
    }
}
