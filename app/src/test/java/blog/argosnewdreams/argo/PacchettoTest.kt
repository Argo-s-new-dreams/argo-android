package blog.argosnewdreams.argo

import blog.argosnewdreams.argo.audio.Audio
import blog.argosnewdreams.argo.audio.FileWav
import blog.argosnewdreams.argo.esporta.Esportatore
import blog.argosnewdreams.argo.modello.Archivio
import blog.argosnewdreams.argo.modello.FotoIncontro
import blog.argosnewdreams.argo.modello.Incontro
import blog.argosnewdreams.argo.modello.Segmento
import blog.argosnewdreams.argo.modello.StatoIncontro
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.ZoneId
import java.util.zip.ZipFile

class PacchettoTest {

    @get:Rule val cartella = TemporaryFolder()

    private val creata = 1_763_456_280_000L // 2025-11-18T08:58:00Z

    private fun incontro() = Incontro(
        id = "6F1C2D7E-0B1A-4C55-9A0E-2B4D1E9F7A10",
        creata = creata,
        titolo = "Incontro stand Rossi Automazione · è più facile",
        lingua = "it-IT",
        durata = 412.5,
        segmenti = listOf(
            Segmento(0.8, 6.2, "Buongiorno, vedo che fate visione artificiale."),
            Segmento(6.9, 14.0, "Sì, per il controllo qualità sulle linee di confezionamento."),
        ),
        foto = listOf(
            FotoIncontro("foto-b.jpg", 40.0, "Brochure X200", emptyList(), letta = true),
            FotoIncontro("foto-a.jpg", 15.3, "Mario Rossi\nmario.rossi@esempio.it", listOf("mario.rossi@esempio.it"), letta = true),
        ),
        stato = StatoIncontro.TRASCRITTO,
    )

    @Test
    fun `pacchetto argo sessione v1 come iPhone`() {
        val fotoDir = cartella.newFolder("foto")
        fotoDir.resolve("foto-a.jpg").writeBytes(byteArrayOf(1, 2, 3))
        fotoDir.resolve("foto-b.jpg").writeBytes(byteArrayOf(4, 5))
        val zip = Esportatore.pacchetto(incontro(), fotoDir, cartella.newFolder("out"), "Argo per Android 0.1.0")
        // Copia d'esempio per la prova incrociata con Argo desktop (strumenti/prova-desktop.sh)
        System.getenv("ARGO_PACCHETTO_ESEMPIO")?.let { zip.copyTo(java.io.File(it), overwrite = true) }

        ZipFile(zip).use { z ->
            val nomi = z.entries().toList().map { it.name }
            val radice = nomi.first().substringBefore("/")
            assertTrue(radice.startsWith("Argo-") && radice.endsWith("-Incontro-stand-Rossi-Automazione-e-piu"))
            assertEquals(setOf("$radice/sessione.json", "$radice/relazione.md", "$radice/foto/01.jpg", "$radice/foto/02.jpg"), nomi.toSet())
            // foto rinumerate in ordine di scatto: 01 è la prima scattata (secondo 15.3)
            assertEquals(listOf<Byte>(1, 2, 3), z.getInputStream(z.getEntry("$radice/foto/01.jpg")).readBytes().toList())

            val json = Json.parseToJsonElement(z.getInputStream(z.getEntry("$radice/sessione.json")).readBytes().decodeToString()).jsonObject
            assertEquals("argo.sessione", json["formato"]!!.jsonPrimitive.content)
            assertEquals(1, json["versioneFormato"]!!.jsonPrimitive.int)
            assertEquals("6F1C2D7E-0B1A-4C55-9A0E-2B4D1E9F7A10", json["id"]!!.jsonPrimitive.content)
            assertEquals("2025-11-18T08:58:00Z", json["creata"]!!.jsonPrimitive.content)
            assertEquals("Argo per Android 0.1.0", json["app"]!!.jsonPrimitive.content)
            val foto = json["foto"]!!.jsonArray.map { it.jsonObject }
            assertEquals("foto/01.jpg", foto[0]["file"]!!.jsonPrimitive.content)
            assertEquals(15.3, foto[0]["secondo"]!!.jsonPrimitive.content.toDouble(), 0.001)
            assertEquals("mario.rossi@esempio.it", foto[0]["riferimenti"]!!.jsonArray[0].jsonPrimitive.content)
            // Come Swift: campi nulli omessi (niente relazione, niente parlante senza voci)
            assertFalse("relazione" in json)
            assertFalse("parlante" in (json["segmenti"]!!.jsonArray[0] as JsonObject))
            assertEquals(2, json["segmenti"]!!.jsonArray.size)
            assertTrue(json["partecipanti"]!!.jsonArray.isEmpty())

            val md = z.getInputStream(z.getEntry("$radice/relazione.md")).readBytes().decodeToString()
            assertTrue(md.contains("📷 FOTO 1: Mario Rossi · mario.rossi@esempio.it"))
            assertTrue(md.contains("Generato con Argo per Android 0.1.0"))
        }
    }

    @Test
    fun `nome cartella senza accenti`() {
        val nome = Esportatore.nomeCartella(incontro(), ZoneId.of("Europe/Rome"))
        assertEquals("Argo-2025-11-18-0958-Incontro-stand-Rossi-Automazione-e-piu", nome)
    }

    @Test
    fun `archivio salva rilegge ed elimina`() = runTest {
        val radice = cartella.newFolder("dati")
        val archivio = Archivio(radice)
        archivio.salva(incontro())
        archivio.cartella(incontro().id).resolve("foto").mkdirs()
        radice.resolve("incontri/ORFANO").mkdirs() // registrazione interrotta: senza incontro.json

        val riletto = Archivio(radice).apply { carica() }
        assertEquals(1, riletto.incontri.value.size)
        assertEquals(incontro(), riletto.incontri.value.first())
        assertFalse(radice.resolve("incontri/ORFANO").exists())

        riletto.aggiorna(incontro().id) { it.copy(titolo = "Nuovo") }
        assertEquals("Nuovo", Archivio(radice).apply { carica() }.incontri.value.first().titolo)

        riletto.elimina(incontro().id)
        assertFalse(archivio.cartella(incontro().id).exists())
    }

    @Test
    fun `wav con intestazione corretta`() {
        val file = cartella.newFile("prova.wav")
        FileWav(file).use { wav -> repeat(10) { wav.scrivi(ByteArray(3200)) } }
        val byte = file.readBytes()
        val buf = ByteBuffer.wrap(byte).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals("RIFF", String(byte, 0, 4))
        assertEquals(36 + 32000, buf.getInt(4))
        assertEquals(Audio.CAMPIONAMENTO, buf.getInt(24))
        assertEquals(32000, buf.getInt(40))
        assertEquals(44 + 32000, byte.size)
    }

    @Test
    fun `livello del microfono`() {
        assertEquals(0f, Audio.livello(ByteArray(3200), 3200), 0.001f)
        val forte = ByteArray(3200) { if (it % 2 == 1) 0x7F else 0xFF.toByte() }
        assertTrue(Audio.livello(forte, 3200) > 0.95f)
    }
}
