package blog.argosnewdreams.argo

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.annotation.StringRes
import blog.argosnewdreams.argo.modello.Etichette
import java.util.Locale

/**
 * Testi dell'interfaccia fuori dalle schermate (errori, fasi, avvisi di sistema), nella lingua dell'app.
 * Le schermate usano `stringResource`; qui si passa dalle risorse dell'applicazione, con la lingua
 * scelta in Argo (Android 13+) anche se il processo non è ancora stato riavviato.
 */
object Testi {

    /** Lingue dell'interfaccia: tag BCP 47, nello stesso ordine del selettore in Impostazioni. */
    val lingueApp = listOf("it", "en", "es", "de")

    private lateinit var app: Context
    @Volatile private var cache: Pair<LocaleList, Resources>? = null

    fun installa(context: Context) {
        app = context.applicationContext
        aggiornaEtichette()
    }

    fun t(@StringRes id: Int, vararg argomenti: Any): String = risorse().getString(id, *argomenti)

    /** Lingua in cui mostrare date e nomi delle lingue. */
    val locale: Locale get() = risorse().configuration.locales[0]

    /** Lingua scelta dentro Argo, o null se segue il telefono (o su Android 12). */
    fun linguaScelta(): String? {
        if (Build.VERSION.SDK_INT < 33) return null
        val scelte = app.getSystemService(LocaleManager::class.java).applicationLocales
        return if (scelte.isEmpty) null else scelte[0].language
    }

    /** Cambia la lingua dell'app (Android 13+): Android ricrea le schermate da solo. null = come il telefono. */
    fun impostaLingua(tag: String?) {
        if (Build.VERSION.SDK_INT < 33) return
        app.getSystemService(LocaleManager::class.java).applicationLocales =
            if (tag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        cache = null
        aggiornaEtichette()
    }

    /** Da chiamare quando cambia la configurazione (lingua del telefono). */
    fun configurazioneCambiata() {
        cache = null
        aggiornaEtichette()
    }

    private fun risorse(): Resources {
        if (Build.VERSION.SDK_INT >= 33) {
            val scelte = app.getSystemService(LocaleManager::class.java).applicationLocales
            if (!scelte.isEmpty) {
                cache?.let { (lingue, risorse) -> if (lingue == scelte) return risorse }
                val configurazione = Configuration(app.resources.configuration).apply { setLocales(scelte) }
                val risorse = app.createConfigurationContext(configurazione).resources
                cache = scelte to risorse
                return risorse
            }
        }
        return app.resources
    }

    /** Il modello (relazione, testo condiviso, titoli) non conosce Android: riceve qui le sue etichette. */
    private fun aggiornaEtichette() {
        Etichette.correnti = Etichette(
            locale = locale,
            parlante = t(R.string.parlante_n, "%s"),
            incontroDel = t(R.string.incontro_del, "%s"),
            nessunTesto = t(R.string.testo_nessun_testo),
            foto = t(R.string.testo_foto),
            riassunto = t(R.string.md_riassunto),
            contatti = t(R.string.md_contatti),
            puntiChiave = t(R.string.md_punti_chiave),
            azioni = t(R.string.md_azioni),
            trascrizione = t(R.string.md_trascrizione),
            relazione = t(R.string.md_relazione, "%s"),
            soloTrascrizione = t(R.string.md_solo_trascrizione),
            generato = t(R.string.md_generato, "%1\$s", "%2\$s"),
            entro = t(R.string.md_entro, "%s"),
            formatoDataLunga = t(R.string.formato_data_lunga),
            formatoDataBreve = t(R.string.formato_data_breve),
        )
    }
}
