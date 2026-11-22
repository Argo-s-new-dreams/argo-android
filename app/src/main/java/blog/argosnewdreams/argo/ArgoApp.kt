package blog.argosnewdreams.argo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import blog.argosnewdreams.argo.elaborazione.Elaborazione
import blog.argosnewdreams.argo.elaborazione.ServizioElaborazione
import blog.argosnewdreams.argo.modello.Archivio
import blog.argosnewdreams.argo.modello.StatoIncontro
import blog.argosnewdreams.argo.registrazione.RegistrazioneInCorso
import blog.argosnewdreams.argo.trascrizione.ModelloLocale
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Contenitore delle poche cose condivise: archivio, impostazioni, registrazione, elaborazione. */
class ArgoApp : Application() {

    /** Un errore in un lavoro di sottofondo finisce nel Logcat (tag Argo), non chiude l'app. */
    private val rete = CoroutineExceptionHandler { _, errore -> Log.e("Argo", "Errore in un lavoro di sottofondo", errore) }
    val ambito = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + rete)
    lateinit var archivio: Archivio
        private set
    lateinit var impostazioni: Impostazioni
        private set
    lateinit var elaborazione: Elaborazione
        private set
    lateinit var registrazione: RegistrazioneInCorso
        private set
    lateinit var modelloLocale: ModelloLocale
        private set

    val nomeApp: String get() = Testi.t(R.string.nome_app_versione, BuildConfig.VERSION_NAME)

    override fun onCreate() {
        super.onCreate()
        RegistroCrash.installa(this)   // per primo: anche un errore qui sotto resta leggibile al riavvio
        Testi.installa(this)
        archivio = Archivio(filesDir)
        impostazioni = Impostazioni(this)
        modelloLocale = ModelloLocale(this, ambito)
        elaborazione = Elaborazione(archivio, impostazioni, modelloLocale, assets, ambito)
        registrazione = RegistrazioneInCorso(this, archivio, elaborazione, ambito)
        creaCanaleNotifiche()
        // Elaborazione in corso → servizio in primo piano con la fase nell'avviso; finita → via.
        ambito.launch {
            elaborazione.fasi.collect { fasi ->
                if (fasi.isEmpty()) ServizioElaborazione.ferma(this@ArgoApp)
                else ServizioElaborazione.aggiorna(this@ArgoApp, fasi.values.first())
            }
        }

        ambito.launch(Dispatchers.IO) {
            runCatching { archivio.carica() }.onFailure { Log.e("Argo", "Archivio non leggibile", it) }
            // Un incontro rimasto «in elaborazione» (app chiusa a metà) riparte da dove era.
            archivio.incontri.value
                .filter { it.stato == StatoIncontro.IN_ELABORAZIONE || it.stato == StatoIncontro.REGISTRATO }
                .forEach { elaborazione.avvia(it.id) }
        }
    }

    /** Cambio della lingua del telefono o di Argo: testi fuori dalle schermate e nomi degli avvisi nella nuova lingua. */
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        Testi.configurazioneCambiata()
        creaCanaleNotifiche()
    }

    /** Chiamata anche quando cambia la lingua: ricreare un canale esistente ne aggiorna solo nome e descrizione. */
    fun creaCanaleNotifiche() {
        val canale = NotificationChannel(CANALE_REGISTRAZIONE, Testi.t(R.string.canale_registrazione), NotificationManager.IMPORTANCE_LOW).apply {
            description = Testi.t(R.string.canale_registrazione_desc)
            setShowBadge(false)
        }
        val canaleElaborazione = NotificationChannel(CANALE_ELABORAZIONE, Testi.t(R.string.canale_elaborazione), NotificationManager.IMPORTANCE_LOW).apply {
            description = Testi.t(R.string.canale_elaborazione_desc)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(canale, canaleElaborazione))
    }

    companion object {
        const val CANALE_REGISTRAZIONE = "registrazione"
        const val CANALE_ELABORAZIONE = "elaborazione"
    }
}
