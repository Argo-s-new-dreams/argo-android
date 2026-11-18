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
            StatoModelloRiga(lingua, modalita)

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

            Sezione("Informazioni")
            Text("Argo per Android ${BuildConfig.VERSION_NAME} · build ${BuildConfig.BUILD_ARGO}", style = MaterialTheme.typography.bodyMedium)
            Text("Argo's New Dreams · argosnewdreams.blog", style = MaterialTheme.typography.bodySmall, color = Argo.colori.tenue)
            Spacer(Modifier.size(24.dp))
        }
    }
}

@Composable
private fun StatoModelloRiga(lingua: String, modalita: Modalita) {
    val ambito = rememberCoroutineScope()
    var stato by remember { mutableStateOf<StatoModello?>(null) }
    var tentativo by remember { mutableIntStateOf(0) }
    var download by remember { mutableStateOf<Int?>(null) }
    var scaricando by remember { mutableStateOf(false) }
    var errore by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lingua, modalita, tentativo) {
        stato = null
        stato = MotoreTrascrizione.prepara(lingua, modalita)
    }
    DisposableEffect(stato) {
        val corrente = stato
        onDispose {
            when (corrente) {
                is StatoModello.Pronto -> corrente.motore.close()
                is StatoModello.DaScaricare -> corrente.motore.close()
                is StatoModello.InDownload -> corrente.motore.close()
                else -> Unit
            }
        }
    }

    Column(Modifier.fillMaxWidth().padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val nome = MotoreTrascrizione.nomeLingua(lingua)
        when (val s = stato) {
            null -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp)); Text("Controllo il modello per $nome…", style = MaterialTheme.typography.bodySmall)
            }
            is StatoModello.Pronto -> Text("$nome: pronto sul telefono (${s.motore.nome})", color = Argo.colori.ok, style = MaterialTheme.typography.bodyMedium)
            is StatoModello.NonDisponibile -> Text(s.motivo, color = Argo.colori.avviso, style = MaterialTheme.typography.bodyMedium)
            is StatoModello.DaScaricare, is StatoModello.InDownload -> {
                Text("$nome: modello da scaricare (una volta sola, poi funziona offline)", color = Argo.colori.avviso, style = MaterialTheme.typography.bodyMedium)
                if (scaricando || s is StatoModello.InDownload) {
                    if (download != null) LinearProgressIndicator(progress = { download!! / 100f }, modifier = Modifier.fillMaxWidth())
                    else LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                if (!scaricando) OutlinedButton(onClick = {
                    val motore = (s as? StatoModello.DaScaricare)?.motore ?: (s as StatoModello.InDownload).motore
                    scaricando = true
                    ambito.launch {
                        try { MotoreTrascrizione.scarica(motore).collect { download = it } }
                        catch (e: Exception) { errore = e.message }
                        finally { scaricando = false; download = null; tentativo++ }
                    }
                }) {
                    Icon(Icons.Outlined.Download, null); Spacer(Modifier.width(8.dp))
                    Text(if (s is StatoModello.InDownload) "Segui il download" else "Scarica il modello")
                }
            }
        }
        errore?.let { Text(it, color = Argo.colori.errore, style = MaterialTheme.typography.bodySmall) }
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
