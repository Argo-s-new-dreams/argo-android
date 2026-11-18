package blog.argosnewdreams.argo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import blog.argosnewdreams.argo.ArgoApp
import blog.argosnewdreams.argo.trascrizione.Modalita
import blog.argosnewdreams.argo.trascrizione.ModelloLocale
import blog.argosnewdreams.argo.trascrizione.Motore
import blog.argosnewdreams.argo.trascrizione.MotoreTrascrizione
import blog.argosnewdreams.argo.trascrizione.StatoModello
import kotlinx.coroutines.launch

/** Stato del motore per lingua e modalità; il motore va chiuso se non viene [consegna]to alla registrazione. */
@Stable
class StatoMotore {
    var stato by mutableStateOf<StatoModello?>(null)
        internal set
    internal var tentativo by mutableIntStateOf(0)
    internal var consegnato = false

    fun ricarica() { tentativo++ }

    /** Il motore pronto passa alla registrazione, che lo chiuderà. */
    fun consegna(): Motore? = (stato as? StatoModello.Pronto)?.motore?.also { consegnato = true }
}

@Composable
fun rememberStatoMotore(app: ArgoApp, lingua: String, modalita: Modalita): StatoMotore {
    val statoMotore = remember { StatoMotore() }
    val locale by app.modelloLocale.stato.collectAsState()
    // A download di Argo locale finito si riprepara da sé
    val prontoLocale = locale == ModelloLocale.Stato.Pronto
    LaunchedEffect(lingua, modalita, statoMotore.tentativo, prontoLocale) {
        statoMotore.stato = null
        statoMotore.stato = MotoreTrascrizione.prepara(lingua, modalita, app.modelloLocale, app.assets)
    }
    DisposableEffect(statoMotore.stato) {
        val corrente = statoMotore.stato
        onDispose { if (!statoMotore.consegnato) motoreDi(corrente)?.close() }
    }
    return statoMotore
}

fun motoreDi(stato: StatoModello?): Motore? = when (stato) {
    is StatoModello.Pronto -> stato.motore
    is StatoModello.DaScaricare -> stato.motore
    is StatoModello.InDownload -> stato.motore
    else -> null
}

/** Cosa può fare la trascrizione su questo telefono, con il download quando serve. */
@Composable
fun CartaMotore(app: ArgoApp, statoMotore: StatoMotore, contesto: String = "registrare") {
    val ambito = rememberCoroutineScope()
    val locale by app.modelloLocale.stato.collectAsState()
    var downloadGoogle by remember { mutableStateOf<Int?>(null) }
    var scaricandoGoogle by remember { mutableStateOf(false) }
    var confermaDati by remember { mutableStateOf(false) }
    var errore by remember { mutableStateOf<String?>(null) }

    fun avviaDownload(motore: Motore) {
        errore = null
        when (motore) {
            is Motore.Locale -> app.modelloLocale.scarica()   // continua anche lasciando la schermata
            is Motore.MlKit -> {
                scaricandoGoogle = true
                ambito.launch {
                    try { MotoreTrascrizione.scarica(motore).collect { downloadGoogle = it } }
                    catch (e: Exception) { errore = e.message }
                    finally { scaricandoGoogle = false; downloadGoogle = null; statoMotore.ricarica() }
                }
            }
        }
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (val s = statoMotore.stato) {
                null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Controllo la trascrizione sul telefono…")
                }
                is StatoModello.Pronto -> {
                    Text("Trascrizione dal vivo pronta", fontWeight = FontWeight.Medium, color = Argo.colori.ok)
                    Text("${s.motore.nome} · vedrai il testo mentre parli.", style = MaterialTheme.typography.bodySmall, color = Argo.colori.tenue)
                }
                is StatoModello.DaScaricare, is StatoModello.InDownload -> {
                    val motore = motoreDi(s)!!
                    val isLocale = motore is Motore.Locale
                    Text(
                        if (isLocale) "Argo locale: modello da scaricare" else "Il modello della lingua non è ancora sul telefono",
                        fontWeight = FontWeight.Medium, color = Argo.colori.avviso,
                    )
                    Text(
                        if (isLocale) "${ModelloLocale.NOME}, circa ${ModelloLocale.MEGABYTE} MB, una volta sola (meglio in Wi-Fi). " +
                            "Poi trascrive offline, anche in fiera. Puoi $contesto subito: la trascrizione partirà a fine incontro, quando il modello c'è."
                        else "Scaricalo ora (serve la rete, una volta sola). Puoi $contesto subito: la trascrizione partirà a fine incontro.",
                        style = MaterialTheme.typography.bodySmall, color = Argo.colori.tenue,
                    )
                    val inCorsoLocale = isLocale && locale is ModelloLocale.Stato.InDownload
                    when {
                        inCorsoLocale -> {
                            val p = (locale as ModelloLocale.Stato.InDownload)
                            LinearProgressIndicator(progress = { p.percentuale / 100f }, modifier = Modifier.fillMaxWidth())
                            Text(
                                "${p.byteScaricati / 1_000_000} di ${ModelloLocale.MEGABYTE} MB · continua anche se chiudi questa schermata",
                                style = MaterialTheme.typography.bodySmall, color = Argo.colori.tenue,
                            )
                        }
                        scaricandoGoogle -> {
                            downloadGoogle?.let { LinearProgressIndicator(progress = { it / 100f }, modifier = Modifier.fillMaxWidth()) }
                                ?: LinearProgressIndicator(Modifier.fillMaxWidth())
                        }
                        else -> OutlinedButton(onClick = {
                            if (isLocale && app.modelloLocale.reteAConsumo()) confermaDati = true else avviaDownload(motore)
                        }) {
                            Icon(Icons.Outlined.Download, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (isLocale) "Scarica Argo locale (${ModelloLocale.MEGABYTE} MB)" else "Scarica il modello")
                        }
                    }
                    if (isLocale && locale is ModelloLocale.Stato.Errore) {
                        Text((locale as ModelloLocale.Stato.Errore).messaggio, color = Argo.colori.errore, style = MaterialTheme.typography.bodySmall)
                    }
                    if (isLocale && confermaDati) {
                        AlertDialog(
                            onDismissRequest = { confermaDati = false },
                            title = { Text("Stai usando i dati mobili") },
                            text = { Text("Il modello pesa circa ${ModelloLocale.MEGABYTE} MB. Meglio aspettare il Wi-Fi, ma se vuoi si scarica anche adesso.") },
                            confirmButton = { TextButton(onClick = { confermaDati = false; avviaDownload(motore) }) { Text("Scarica comunque") } },
                            dismissButton = { TextButton(onClick = { confermaDati = false }) { Text("Aspetto il Wi-Fi") } },
                        )
                    }
                }
                is StatoModello.NonDisponibile -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = Argo.colori.avviso)
                        Spacer(Modifier.width(8.dp))
                        Text("Trascrizione non disponibile", fontWeight = FontWeight.Medium, color = Argo.colori.avviso)
                    }
                    Text(s.motivo, style = MaterialTheme.typography.bodySmall, color = Argo.colori.tenue)
                }
            }
            errore?.let { Text(it, color = Argo.colori.errore, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
