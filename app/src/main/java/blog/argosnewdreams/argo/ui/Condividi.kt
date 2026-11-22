package blog.argosnewdreams.argo.ui

import blog.argosnewdreams.argo.R
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import blog.argosnewdreams.argo.ArgoApp
import blog.argosnewdreams.argo.esporta.Esportatore
import blog.argosnewdreams.argo.modello.Incontro
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Condivisione con le app del telefono (Quick Share, Gmail, Outlook, Teams, Drive…). */
object Condividi {

    /** Il pacchetto Argo, da importare in Argo sul computer (Incontri › Importa pacchetto…). */
    suspend fun pacchetto(context: Context, incontro: Incontro) {
        val app = context.applicationContext as ArgoApp
        val zip = withContext(Dispatchers.IO) {
            val cartella = cartellaCondivisi(context)
            Esportatore.pacchetto(incontro, app.archivio.cartellaFoto(incontro.id), cartella, app.nomeApp)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.file", zip)
        val invio = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.pacchetto_oggetto, incontro.titoloMostrato))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(invio, context.getString(R.string.invia_ad_argo)))
    }

    /** Relazione e trascrizione in Markdown, come testo. */
    fun testo(context: Context, incontro: Incontro) {
        val app = context.applicationContext as ArgoApp
        val invio = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, incontro.titoloMostrato)
            putExtra(Intent.EXTRA_TEXT, incontro.markdown(app.nomeApp))
        }
        context.startActivity(Intent.createChooser(invio, context.getString(R.string.condividi_testo)))
    }

    /** Svuotata a ogni condivisione: nessun pacchetto vecchio resta in giro sul telefono. */
    private fun cartellaCondivisi(context: Context): File =
        File(context.cacheDir, "condivisi").apply { deleteRecursively(); mkdirs() }
}
