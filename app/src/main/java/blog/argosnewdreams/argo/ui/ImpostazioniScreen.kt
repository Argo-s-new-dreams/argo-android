package blog.argosnewdreams.argo.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import blog.argosnewdreams.argo.ArgoApp
import blog.argosnewdreams.argo.BuildConfig
import blog.argosnewdreams.argo.trascrizione.Modalita
import blog.argosnewdreams.argo.trascrizione.MotoreTrascrizione
import blog.argosnewdreams.argo.trascrizione.StatoModello
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImpostazioniScreen(app: ArgoApp, indietro: () -> Unit) {
    val imp = app.impostazioni
    val lingua by imp.lingua.collectAsState()
    val modalita by imp.modalita.collectAsState()
    val eliminaAudio by imp.eliminaAudio.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni") },
                navigationIcon = { IconButton(onClick = indietro) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Indietro") } },
            )
        },
    ) { margini ->
        Column(
            Modifier.padding(margini).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Sezione("Lingua predefinita")
            MotoreTrascrizione.lingueProposte.forEach { (codice, nome) ->
                Scelta(nome, null, codice == lingua) { imp.impostaLingua(codice) }
            }

            Sezione("Trascrizione")
            Modalita.entries.forEach { m -> Scelta(m.etichetta, m.descrizione, m == modalita) { imp.impostaModalita(m) } }
            CartaMotore(app, rememberStatoMotore(app, lingua, modalita), contesto = "registrare")
            ModelloLocaleRiga(app)

            Sezione("Privacy")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Elimina l'audio appena trascritto")
                    Text(
                        "Restano solo testo e foto. Se la trascrizione non riesce, l'audio resta finché non la ripeti.",
                        style = MaterialTheme.typography.bodySmall, color = Argo.colori.tenue,
                    )
                }
                Switch(checked = eliminaAudio, onCheckedChange = imp::impostaEliminaAudio)
            }
            Text(
                "Argo non è un registratore: è una stenografa digitale locale. Ascolta solo mentre lo usi, trascrive sul telefono, " +
                    "non conserva la voce, non invia la conversazione a un cloud e lascia a te il controllo del testo. " +
                    "Gli incontri sono esclusi dal backup di Google.",
                style = MaterialTheme.typography.bodySmall, color = Argo.colori.tenue,
                modifier = Modifier.padding(top = 8.dp),
            )

            Sezione("Crediti")
            Text(
                "Argo locale usa NVIDIA Parakeet TDT 0.6B v3 (CC-BY-4.0, versione int8 per sherpa-onnx di k2-fsa), " +
                    "sherpa-onnx (Apache 2.0) e Silero VAD (MIT). Trascrizione e testo delle foto anche con Google ML Kit.",
                style = MaterialTheme.typography.bodySmall, color = Argo.colori.tenue,
            )
            LicenzeRiga()

            Sezione("Informazioni")
            Text("Argo per Android ${BuildConfig.VERSION_NAME} · build ${BuildConfig.BUILD_ARGO}", style = MaterialTheme.typography.bodyMedium)
            Text("Argo's New Dreams · argosnewdreams.blog", style = MaterialTheme.typography.bodySmall, color = Argo.colori.tenue)
            Spacer(Modifier.size(24.dp))
        }
    }
}

/** Testo completo delle licenze di terze parti (assets/licenze_terze_parti.txt, come THIRD_PARTY_LICENSES.md). */
@Composable
private fun LicenzeRiga() {
    val contesto = androidx.compose.ui.platform.LocalContext.current
    var aperto by remember { mutableStateOf(false) }
    androidx.compose.material3.TextButton(onClick = { aperto = true }) { Text("Licenze e riconoscimenti") }
    if (aperto) {
        val testo = remember {
            runCatching { contesto.assets.open("licenze_terze_parti.txt").bufferedReader().use { it.readText() } }
                .getOrDefault("Testo delle licenze non trovato: lo trovi su github.com/Argo-s-new-dreams/argo-android (THIRD_PARTY_LICENSES.md).")
        }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { aperto = false },
            title = { Text("Licenze e riconoscimenti") },
            text = {
                Text(
                    testo,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { aperto = false }) { Text("Chiudi") } },
        )
    }
}

/** Spazio occupato da Argo locale, con la possibilità di liberarlo. */
@Composable
private fun ModelloLocaleRiga(app: ArgoApp) {
    val stato by app.modelloLocale.stato.collectAsState()
    var conferma by remember { mutableStateOf(false) }
    if (stato != blog.argosnewdreams.argo.trascrizione.ModelloLocale.Stato.Pronto) return
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        Text(
            "Argo locale occupa circa ${blog.argosnewdreams.argo.trascrizione.ModelloLocale.MEGABYTE} MB sul telefono.",
            style = MaterialTheme.typography.bodySmall, color = Argo.colori.tenue, modifier = Modifier.weight(1f),
        )
        androidx.compose.material3.TextButton(onClick = { conferma = true }) { Text("Elimina modello") }
    }
    if (conferma) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { conferma = false },
            title = { Text("Eliminare il modello di Argo locale?") },
            text = { Text("Liberi spazio; per trascrivere di nuovo con Argo locale andrà riscaricato.") },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { conferma = false; app.modelloLocale.elimina() }) { Text("Elimina") } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { conferma = false }) { Text("Annulla") } },
        )
    }
}

@Composable
private fun Sezione(titolo: String) {
    Column(Modifier.padding(top = 16.dp)) {
        HorizontalDivider()
        Text(titolo.uppercase(), style = MaterialTheme.typography.labelMedium, color = Argo.colori.tenue, fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
    }
}

@Composable
private fun Scelta(titolo: String, descrizione: String?, scelto: Boolean, alClic: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().selectable(selected = scelto, onClick = alClic, role = Role.RadioButton).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = scelto, onClick = null)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(titolo)
            descrizione?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Argo.colori.tenue) }
        }
    }
}
