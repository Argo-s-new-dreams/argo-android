package blog.argosnewdreams.argo

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import blog.argosnewdreams.argo.elaborazione.Elaborazione
import blog.argosnewdreams.argo.modello.Archivio
import blog.argosnewdreams.argo.modello.StatoIncontro
import blog.argosnewdreams.argo.registrazione.RegistrazioneInCorso
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Contenitore delle poche cose condivise: archivio, impostazioni, registrazione, elaborazione. */
class ArgoApp : Application() {

    val ambito = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    lateinit var archivio: Archivio
        private set
    lateinit var impostazioni: Impostazioni
        private set
    lateinit var elaborazione: Elaborazione
        private set
    lateinit var registrazione: RegistrazioneInCorso
        private set

    val nomeApp: String get() = "Argo per Android ${BuildConfig.VERSION_NAME}"

    override fun onCreate() {
        super.onCreate()
        archivio = Archivio(filesDir)
        impostazioni = Impostazioni(this)
        elaborazione = Elaborazione(archivio, impostazioni, ambito)
        registrazione = RegistrazioneInCorso(this, archivio, elaborazione, ambito)
        creaCanaleNotifiche()

        ambito.launch(Dispatchers.IO) {
            archivio.carica()
            // Un incontro rimasto «in elaborazione» (app chiusa a metà) riparte da dove era.
            archivio.incontri.value
                .filter { it.stato == StatoIncontro.IN_ELABORAZIONE || it.stato == StatoIncontro.REGISTRATO }
                .forEach { launch(Dispatchers.Main) { elaborazione.esegui(it.id) } }
        }
    }

    private fun creaCanaleNotifiche() {
        val canale = NotificationChannel(CANALE_REGISTRAZIONE, "Registrazione in corso", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Avviso fisso mentre Argo registra un incontro"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(canale)
    }

    companion object {
        const val CANALE_REGISTRAZIONE = "registrazione"
    }
}
