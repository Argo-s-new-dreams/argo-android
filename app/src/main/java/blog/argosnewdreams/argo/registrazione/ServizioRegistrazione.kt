package blog.argosnewdreams.argo.registrazione

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import blog.argosnewdreams.argo.ArgoApp
import blog.argosnewdreams.argo.MainActivity
import blog.argosnewdreams.argo.R

/**
 * Servizio in primo piano di tipo «microfono»: Android lo pretende per registrare a schermo spento
 * o con un'altra app davanti. Non registra nulla da sé: mostra l'avviso fisso e tiene vivo il processo.
 */
class ServizioRegistrazione : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val apri = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notifica = NotificationCompat.Builder(this, ArgoApp.CANALE_REGISTRAZIONE)
            .setSmallIcon(R.drawable.ic_stat_argo)
            .setContentTitle("Argo sta registrando l'incontro")
            .setContentText("Trascrizione sul telefono · l'audio viene eliminato dopo")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(apri)
            .build()
        ServiceCompat.startForeground(this, ID_NOTIFICA, notifica, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE)
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val ID_NOTIFICA = 1

        fun avvia(context: Context) =
            ContextCompat.startForegroundService(context, Intent(context, ServizioRegistrazione::class.java))

        fun ferma(context: Context) {
            context.stopService(Intent(context, ServizioRegistrazione::class.java))
        }
    }
}
