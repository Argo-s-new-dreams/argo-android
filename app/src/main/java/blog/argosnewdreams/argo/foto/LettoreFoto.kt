package blog.argosnewdreams.argo.foto

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/** Salvataggio e lettura (OCR) delle foto, tutto sul telefono: il modello latino è dentro l'app. */
object LettoreFoto {

    private const val LATO_LUNGO = 2048

    /** Raddrizza, ridimensiona (lato lungo 2.048 px, come su iPhone) e salva in JPEG. Restituisce il nome del file. */
    suspend fun salva(immagine: Bitmap, rotazione: Int, cartella: File): String = withContext(Dispatchers.IO) {
        val scala = minOf(1f, LATO_LUNGO.toFloat() / maxOf(immagine.width, immagine.height))
        val matrice = Matrix().apply {
            postScale(scala, scala)
            postRotate(rotazione.toFloat())
        }
        val pronta = Bitmap.createBitmap(immagine, 0, 0, immagine.width, immagine.height, matrice, true)
        val nome = "foto-${UUID.randomUUID()}.jpg"
        File(cartella, nome).outputStream().use { pronta.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        if (pronta !== immagine) pronta.recycle()
        nome
    }

    /** Testo della foto e riferimenti (email, siti, telefoni). Se qualcosa va storto: testo vuoto, mai un errore. */
    suspend fun leggi(file: File): Pair<String, List<String>> = withContext(Dispatchers.Default) {
        val bitmap = BitmapFactory.decodeFile(file.path) ?: return@withContext "" to emptyList()
        val lettore = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val risultato = lettore.process(InputImage.fromBitmap(bitmap, 0)).await()
            val testo = risultato.textBlocks.flatMap { blocco -> blocco.lines.map { it.text } }.joinToString("\n")
            testo to Riferimenti.trova(testo)
        } catch (e: Exception) {
            "" to emptyList()
        } finally {
            lettore.close()
            bitmap.recycle()
        }
    }

    /** Miniatura leggera per elenchi e strisce di foto. */
    fun miniatura(file: File, lato: Int = 256): Bitmap? {
        val limiti = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, limiti)
        if (limiti.outWidth <= 0) return null
        var campione = 1
        while (maxOf(limiti.outWidth, limiti.outHeight) / (campione * 2) >= lato) campione *= 2
        return BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = campione })
    }
}
