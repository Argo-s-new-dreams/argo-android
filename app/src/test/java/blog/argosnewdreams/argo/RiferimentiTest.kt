package blog.argosnewdreams.argo

import blog.argosnewdreams.argo.foto.Riferimenti
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RiferimentiTest {

    private val biglietto = """
        Mario Rossi
        Sales Manager
        Rossi Automazione S.r.l.
        Via Roma 12 · 12100 Cuneo
        Tel. +39 0171 123 456 · Cell. 333-1234567
        mario.rossi@rossi-automazione.it
        www.rossi-automazione.it
        P.IVA 01234567890
    """.trimIndent()

    @Test
    fun `biglietto da visita italiano`() {
        val trovati = Riferimenti.trova(biglietto)
        assertTrue(trovati.toString(), "mario.rossi@rossi-automazione.it" in trovati)
        assertTrue(trovati.toString(), "www.rossi-automazione.it" in trovati)
        assertTrue(trovati.toString(), "+39 0171 123 456" in trovati)
        assertTrue(trovati.toString(), "333 1234567" in trovati)
    }

    @Test
    fun `partita IVA e CAP non sono telefoni`() {
        val trovati = Riferimenti.trova(biglietto)
        assertFalse(trovati.toString(), trovati.any { it.replace(" ", "") == "01234567890" })
        assertFalse(trovati.toString(), trovati.any { it.contains("12100") })
    }

    @Test
    fun `prefisso internazionale 00 normalizzato`() {
        assertEquals(listOf("+49 89 1234 5678"), Riferimenti.trova("Telefon 0049 89 1234 5678"))
    }

    @Test
    fun `sito con https`() {
        assertTrue("https://esempio.it/prodotti" in Riferimenti.trova("Catalogo: https://esempio.it/prodotti"))
    }

    @Test
    fun `testo senza riferimenti`() {
        assertTrue(Riferimenti.trova("Linea di confezionamento modello X200").isEmpty())
    }
}
