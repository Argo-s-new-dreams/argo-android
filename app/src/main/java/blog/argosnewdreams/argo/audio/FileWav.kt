package blog.argosnewdreams.argo.audio

import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** WAV PCM 16 bit mono: intestazione provvisoria all'apertura, dimensioni corrette alla chiusura. */
class FileWav(file: File, private val campionamento: Int = Audio.CAMPIONAMENTO) : AutoCloseable {

    private val uscita = RandomAccessFile(file, "rw").apply {
        setLength(0)
        write(intestazione(0))
    }
    var byteDati = 0L
        private set

    fun scrivi(dati: ByteArray, lunghezza: Int = dati.size) {
        uscita.write(dati, 0, lunghezza)
        byteDati += lunghezza
    }

    override fun close() {
        uscita.seek(0)
        uscita.write(intestazione(byteDati))
        uscita.close()
    }

    private fun intestazione(byteDati: Long): ByteArray {
        val canali = 1
        val bit = 16
        val byteAlSecondo = campionamento * canali * bit / 8
        return ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray()); putInt((36 + byteDati).toInt())
            put("WAVE".toByteArray())
            put("fmt ".toByteArray()); putInt(16); putShort(1); putShort(canali.toShort())
            putInt(campionamento); putInt(byteAlSecondo); putShort((canali * bit / 8).toShort()); putShort(bit.toShort())
            put("data".toByteArray()); putInt(byteDati.toInt())
        }.array()
    }
}

object Audio {
    /** Formato richiesto dalla trascrizione ML Kit in streaming: PCM 16 bit, mono, 16 kHz. */
    const val CAMPIONAMENTO = 16_000
    const val BYTE_AL_SECONDO = CAMPIONAMENTO * 2

    /** Livello 0…1 da un blocco PCM 16 bit (RMS in dB, -60…0 come su iPhone). */
    fun livello(dati: ByteArray, lunghezza: Int): Float {
        if (lunghezza < 2) return 0f
        var somma = 0.0
        var i = 0
        while (i + 1 < lunghezza) {
            val campione = ((dati[i + 1].toInt() shl 8) or (dati[i].toInt() and 0xFF)).toShort().toDouble()
            somma += campione * campione
            i += 2
        }
        val rms = kotlin.math.sqrt(somma / (lunghezza / 2)) / Short.MAX_VALUE
        val db = if (rms <= 0) -60.0 else (20 * kotlin.math.log10(rms)).coerceAtLeast(-60.0)
        return ((db + 60) / 60).toFloat().coerceIn(0f, 1f)
    }
}
