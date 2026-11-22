package blog.argosnewdreams.argo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Ogni testo esiste in tutte le lingue, con gli stessi segnaposto (%1$s, %2$d…). */
class TraduzioniTest {

    private val res = File("src/main/res")

    private fun leggi(cartella: String): Map<String, String> {
        val documento = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(File(res, "$cartella/strings.xml"))
        val voci = documento.getElementsByTagName("string")
        return (0 until voci.length).map { voci.item(it) }
            .filter { it.attributes.getNamedItem("translatable")?.nodeValue != "false" }
            .associate { it.attributes.getNamedItem("name").nodeValue to it.textContent }
    }

    private fun segnaposto(testo: String) = Regex("""%\d+\$[sd]""").findAll(testo).map { it.value }.sorted().toList()

    @Test
    fun `tutte le lingue hanno tutte le chiavi e gli stessi segnaposto`() {
        val base = leggi("values")
        assertTrue(base.size > 100)
        for (lingua in listOf("values-en", "values-es", "values-de")) {
            val tradotte = leggi(lingua)
            assertEquals("chiavi di $lingua", base.keys.sorted(), tradotte.keys.sorted())
            base.forEach { (chiave, testo) ->
                assertEquals("segnaposto di $chiave in $lingua", segnaposto(testo), segnaposto(tradotte.getValue(chiave)))
            }
        }
    }
}
