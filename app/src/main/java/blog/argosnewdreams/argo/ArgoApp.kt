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

    val nomeApp: String get() = "Argo per Android ${BuildConfig.VERSION_NAME}"

    override fun onCreate() {
        super.onCreate()
        RegistroCrash.installa(this)   // per primo: anche un errore qui sotto resta leggibile al riavvio
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

    private fun creaCanaleNotifiche() {
        val canale = NotificationChannel(CANALE_REGISTRAZIONE, "Registrazione in corso", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Avviso fisso mentre Argo registra un incontro"
            setShowBadge(false)
        }
        val canaleElaborazione = NotificationChannel(CANALE_ELABORAZIONE, "Elaborazione degli incontri", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Avviso fisso mentre Argo trascrive un incontro sul telefono"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannels(listOf(canale, canaleElaborazione))
    }

    companion object {
        const val CANALE_REGISTRAZIONE = "registrazione"
        const val CANALE_ELABORAZIONE = "elaborazione"
    }
}
