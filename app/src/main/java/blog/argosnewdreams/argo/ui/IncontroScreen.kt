package blog.argosnewdreams.argo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import blog.argosnewdreams.argo.ArgoApp
import blog.argosnewdreams.argo.foto.LettoreFoto
import blog.argosnewdreams.argo.modello.FotoIncontro
import blog.argosnewdreams.argo.modello.Formato
import blog.argosnewdreams.argo.modello.Incontro
import blog.argosnewdreams.argo.modello.OrigineTempi
import blog.argosnewdreams.argo.modello.StatoIncontro
import blog.argosnewdreams.argo.modello.VoceCronologia
import blog.argosnewdreams.argo.trascrizione.MotoreTrascrizione
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncontroScreen(app: ArgoApp, id: String, indietro: () -> Unit) {
    val context = LocalContext.current
    val ambito = rememberCoroutineScope()
    val incontri by app.archivio.incontri.collectAsState()
    val fasi by app.elaborazione.fasi.collectAsState()
    val incontro = incontri.firstOrNull { it.id == id }
    var menu by remember { mutableStateOf(false) }
    var rinomina by remember { mutableStateOf(false) }
    var confermaElimina by remember { mutableStateOf(false) }
    var fotoAperta by remember { mutableStateOf<FotoIncontro?>(null) }
    var errore by remember { mutableStateOf<String?>(null) }

    if (incontro == null) {
        // Eliminato (anche da un'altra schermata): si torna all'elenco, fuori dalla composizione.
        androidx.compose.runtime.LaunchedEffect(Unit) { indietro() }
        return
    }
    val fase = fasi[id]

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(incontro.titoloMostrato, maxLines = 1) },
                navigationIcon = { IconButton(onClick = indietro) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Indietro") } },
                actions = {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "Altre azioni") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(
                            text = { Text("Rinomina") },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                            onClick = { menu = false; rinomina = true },
                        )
                        DropdownMenuItem(
                            text = { Text("Condividi il testo") },
                            leadingIcon = { Icon(Icons.Outlined.Share, null) },
                            enabled = fase == null,
                            onClick = { menu = false; Condividi.testo(context, incontro) },
                        )
                        DropdownMenuItem(
                            text = { Text("Elimina incontro", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.DeleteOutline, null, tint = MaterialTheme.colorScheme.error) },
                            enabled = fase == null,
                            onClick = { menu = false; confermaElimina = true },
                        )
                    }
                },
            )
        },
    ) { margini ->
        LazyColumn(
            Modifier.padding(margini).fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Intestazione(incontro) }

            if (fase != null) item {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(fase, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (fase == null && incontro.stato == StatoIncontro.ERRORE) item {
                Avviso(Icons.Outlined.ErrorOutline, Argo.colori.errore, incontro.messaggioErrore ?: "Qualcosa non ha funzionato.") {
                    OutlinedButton(onClick = { app.elaborazione.avvia(id) }) {
                        Icon(Icons.Outlined.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Riprova")
                    }
                }
            }
            incontro.avviso?.let { testo -> item { Avviso(Icons.Outlined.Info, Argo.colori.avviso, testo) } }

            if (fase == null && (incontro.haTrascrizione || incontro.foto.isNotEmpty())) item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Relazione e CRM su Argo sul computer", fontWeight = FontWeight.Medium)
                        Text(
                            "Invia il pacchetto (testo e foto, mai l'audio) e importalo in Argo: Incontri › Importa pacchetto…. " +
                                "Lì nascono la relazione, il cliente in Dynamics 365 e i task.",
                            style = MaterialTheme.typography.bodySmall, color = Argo.colori.tenue,
                        )
                        Button(
                            onClick = {
                                ambito.launch {
                                    runCatching { Condividi.pacchetto(context, incontro) }
                                        .onFailure { errore = "Non riesco a preparare il pacchetto: ${it.message}" }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Outlined.Computer, null); Spacer(Modifier.width(8.dp)); Text("Invia ad Argo sul computer")
                        }
                    }
                }
            }

            if (incontro.cronologia.isNotEmpty()) item {
                Text("Trascrizione", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
            }
            items(incontro.cronologia, key = { voce ->
                when (voce) {
                    is VoceCronologia.Parole -> "p-${voce.segmento.inizio}-${voce.segmento.testo.hashCode()}"
                    is VoceCronologia.Scatto -> "f-${voce.foto.nomeFile}"
                }
            }) { voce ->
                when (voce) {
                    is VoceCronologia.Parole -> RigaParole(incontro, voce)
                    is VoceCronologia.Scatto -> RigaFoto(app, incontro.id, voce.foto) { fotoAperta = voce.foto }
                }
            }
        }
    }

    if (rinomina) {
        var titolo by remember { mutableStateOf(incontro.titolo) }
        AlertDialog(
            onDismissRequest = { rinomina = false },
            title = { Text("Titolo dell'incontro") },
            text = { OutlinedTextField(titolo, { titolo = it }, singleLine = true, placeholder = { Text("es. Stand Rossi Automazione") }) },
            confirmButton = {
                TextButton(onClick = {
                    rinomina = false
                    ambito.launch { app.archivio.aggiorna(id) { it.copy(titolo = titolo.trim()) } }
                }) { Text("Salva") }
            },
            dismissButton = { TextButton(onClick = { rinomina = false }) { Text("Annulla") } },
        )
    }

    if (confermaElimina) {
        AlertDialog(
            onDismissRequest = { confermaElimina = false },
            title = { Text("Eliminare l'incontro?") },
            text = { Text("Trascrizione e foto verranno cancellate da questo telefono. Ciò che hai già inviato ad Argo sul computer resta lì.") },
            confirmButton = {
                TextButton(onClick = {
                    confermaElimina = false
                    ambito.launch { app.archivio.elimina(id); indietro() }
                }) { Text("Elimina", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confermaElimina = false }) { Text("Annulla") } },
        )
    }

    fotoAperta?.let { foto -> FotoIntera(app, incontro.id, foto) { fotoAperta = null } }

    errore?.let { messaggio ->
        AlertDialog(
            onDismissRequest = { errore = null },
            confirmButton = { TextButton(onClick = { errore = null }) { Text("OK") } },
            text = { Text(messaggio) },
        )
    }
}

@Composable
private fun Intestazione(incontro: Incontro) {
    Column(Modifier.padding(top = 4.dp)) {
        val parti = buildList {
            add(Formato.dataLunga(incontro.creata))
            add(Formato.durata(incontro.durata))
            add(MotoreTrascrizione.nomeLingua(incontro.lingua))
        }
        Text(parti.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = Argo.colori.tenue)
        val origine = buildList {
            incontro.motoreTrascrizione?.let { add("trascritto con $it") }
            if (incontro.haTrascrizione && incontro.tempi == OrigineTempi.STIMATI) add("tempi stimati")
            if (incontro.fileAudio == null && incontro.haTrascrizione) add("audio eliminato")
        }
        if (origine.isNotEmpty()) Text(origine.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = Argo.colori.tenue)
    }
}

@Composable
private fun Avviso(
    icona: androidx.compose.ui.graphics.vector.ImageVector,
    colore: Color,
    testo: String,
    azione: (@Composable () -> Unit)? = null,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(icona, contentDescription = null, tint = colore)
                Spacer(Modifier.width(10.dp))
                Text(testo, style = MaterialTheme.typography.bodyMedium)
            }
            if (azione != null) { Spacer(Modifier.height(8.dp)); azione() }
        }
    }
}

@Composable
private fun RigaParole(incontro: Incontro, voce: VoceCronologia.Parole) {
    val s = voce.segmento
    Row {
        Text(Formato.durata(s.inizio), style = MaterialTheme.typography.labelSmall, color = Argo.colori.tenue, modifier = Modifier.width(52.dp).padding(top = 3.dp))
        Column {
            incontro.nome(s.parlante)?.let { chi ->
                val indice = incontro.parlanti.indexOf(s.parlante).coerceAtLeast(0)
                Text(chi, style = MaterialTheme.typography.labelLarge, color = Argo.colori.parlanti[indice % Argo.colori.parlanti.size])
            }
            Text(s.testo, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun RigaFoto(app: ArgoApp, id: String, foto: FotoIncontro, apri: () -> Unit) {
    val miniatura = remember(foto.nomeFile) { LettoreFoto.miniatura(app.archivio.fileFoto(id, foto.nomeFile), 320) }
    Card(
        onClick = apri,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp)) {
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(10.dp))) {
                miniatura?.let {
                    Image(it.asImageBitmap(), contentDescription = "Foto al minuto ${Formato.durata(foto.secondo)}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("📷 ${Formato.durata(foto.secondo)}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(
                    when {
                        !foto.letta -> "Testo da leggere…"
                        foto.testoOCR.isBlank() -> "Nessun testo leggibile"
                        else -> foto.testoOCR
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (foto.testoOCR.isBlank()) Argo.colori.tenue else MaterialTheme.colorScheme.onSurface,
                    maxLines = 6,
                )
                if (foto.riferimenti.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        foto.riferimenti.forEach { AssistChip(onClick = {}, label = { Text(it, style = MaterialTheme.typography.labelSmall) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FotoIntera(app: ArgoApp, id: String, foto: FotoIncontro, chiudi: () -> Unit) {
    val immagine = remember(foto.nomeFile) { LettoreFoto.miniatura(app.archivio.fileFoto(id, foto.nomeFile), 2048) }
    Dialog(onDismissRequest = chiudi, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().clickable(onClick = chiudi)) {
            immagine?.let {
                Image(it.asImageBitmap(), contentDescription = "Foto dell'incontro", contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            }
        }
    }
}
