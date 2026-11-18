package blog.argosnewdreams.argo

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.ZonedDateTime

/**
 * Se Argo si chiude per un errore, il dettaglio tecnico resta in un file privato dell'app
 * e al riavvio si può leggere e condividere: serve quando il telefono non è collegato ad Android Studio.
 * Contiene solo l'errore (classe, messaggio, righe di codice), mai testo degli incontri o audio.
 */
object RegistroCrash {

    private fun file(context: Context) = File(context.filesDir, "ultimo-errore.txt")

    fun installa(context: Context) {
        val precedente = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, errore ->
            runCatching {
                val traccia = StringWriter().also { errore.printStackTrace(PrintWriter(it)) }.toString()
                file(context).writeText(
                    buildString {
                        appendLine("Argo per Android ${BuildConfig.VERSION_NAME} · build ${BuildConfig.BUILD_ARGO}")
                        appendLine("${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                        appendLine("Quando: ${ZonedDateTime.now()} · thread ${thread.name}")
                        appendLine()
                        append(traccia.take(12_000))
                    }
                )
            }
            precedente?.uncaughtException(thread, errore)
        }
    }

    fun leggi(context: Context): String? = file(context).takeIf { it.exists() }?.readText()

    fun cancella(context: Context) {
        file(context).delete()
    }
}
