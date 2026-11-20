package blog.argosnewdreams.argo.elaborazione

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import blog.argosnewdreams.argo.ArgoApp
import blog.argosnewdreams.argo.MainActivity
import blog.argosnewdreams.argo.R

/**
 * Servizio in primo piano mentre Argo elabora un incontro (trascrizione dal file, foto): senza, con lo schermo
 * spento o un'altra app davanti Android può sospendere o chiudere Argo a metà di un incontro lungo.
 * Tipo «mediaProcessing» da Android 15 (lavori lunghi sui file audio, fino a 6 ore ogni 24), «dataSync» prima.
 * Tiene anche un wake lock parziale: lo schermo si può spegnere, il processore no.
 * Non fa nulla da sé: il lavoro resta in Elaborazione (ambito dell'app); qui solo avviso fisso e processo vivo.
 */
class ServizioElaborazione : Service() {

    private var risveglio: PowerManager.WakeLock? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val tipo = if (Build.VERSION.SDK_INT >= 35) ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROCESSING
        else ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        ServiceCompat.startForeground(this, ID_NOTIFICA, notifica(this, intent?.getStringExtra(EXTRA_FASE)), tipo)
        attivo = true
        if (risveglio == null) {
            risveglio = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Argo:elaborazione")
                .apply { setReferenceCounted(false); acquire(MASSIMO_MS) }
        }
        return START_NOT_STICKY
    }

    /** Android 15: finito il tempo concesso per i lavori lunghi, il servizio va chiuso subito. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        Log.w("Argo", "Tempo concesso da Android per l'elaborazione finito: il lavoro continua solo con l'app aperta")
        stopSelf()
    }

    override fun onDestroy() {
        risveglio?.let { if (it.isHeld) it.release() }
        risveglio = null
        attivo = false
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val ID_NOTIFICA = 2
        private const val EXTRA_FASE = "fase"
        private const val MASSIMO_MS = 4L * 60 * 60 * 1000
        @Volatile private var attivo = false

        private fun notifica(context: Context, fase: String?): Notification {
            val apri = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE,
            )
            return NotificationCompat.Builder(context, ArgoApp.CANALE_ELABORAZIONE)
                .setSmallIcon(R.drawable.ic_stat_argo)
                .setContentTitle("Argo sta elaborando l'incontro")
                .setContentText(fase ?: "Sul telefono, senza cloud")
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .setContentIntent(apri)
                .build()
        }

        /** Avvia il servizio o, se c'è già, aggiorna la fase nell'avviso. Mai un errore: senza servizio
         *  (avvio dal sottofondo vietato da Android) il lavoro continua finché l'app resta aperta. */
        @SuppressLint("MissingPermission")
        fun aggiorna(context: Context, fase: String?) {
            if (attivo) {
                val permesso = Build.VERSION.SDK_INT < 33 ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                if (permesso) runCatching { NotificationManagerCompat.from(context).notify(ID_NOTIFICA, notifica(context, fase)) }
                return
            }
            runCatching {
                ContextCompat.startForegroundService(
                    context, Intent(context, ServizioElaborazione::class.java).putExtra(EXTRA_FASE, fase),
                )
            }.onFailure { Log.w("Argo", "Servizio di elaborazione non avviato: ${it.message}") }
        }

        fun ferma(context: Context) {
            if (attivo) context.stopService(Intent(context, ServizioElaborazione::class.java))
        }
    }
}
