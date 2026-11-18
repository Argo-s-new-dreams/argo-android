package blog.argosnewdreams.argo.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import blog.argosnewdreams.argo.ArgoApp
import blog.argosnewdreams.argo.modello.Formato
import blog.argosnewdreams.argo.modello.Incontro
import blog.argosnewdreams.argo.modello.StatoIncontro

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElencoScreen(app: ArgoApp, apriIncontro: (String) -> Unit, registra: () -> Unit, impostazioni: () -> Unit) {
    val incontri by app.archivio.incontri.collectAsState()
    val fasi by app.elaborazione.fasi.collectAsState()
    val attiva by app.registrazione.attiva.collectAsState()
    val durata by app.registrazione.durata.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Incontri") },
                actions = {
                    IconButton(onClick = impostazioni) { Icon(Icons.Outlined.Settings, contentDescription = "Impostazioni") }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = registra,
                icon = { Icon(Icons.Outlined.Mic, contentDescription = null) },
                text = { Text(if (attiva != null) "Torna alla registrazione" else "Registra") },
            )
        },
    ) { margini ->
        Column(Modifier.padding(margini).fillMaxSize()) {
            if (attiva != null) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth().clickable(onClick = registra),
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape, modifier = Modifier.size(10.dp)) {}
                        Spacer(Modifier.width(10.dp))
                        Text("Registrazione in corso · ${Formato.durata(durata)}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            if (incontri.isEmpty()) {
                Column(
                    Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Outlined.Mic, contentDescription = null, tint = Argo.colori.tenue, modifier = Modifier.size(48.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Nessun incontro", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tocca Registra: Argo trascrive mentre parli, aggancia le foto al momento giusto " +
                            "e non lascia l'audio sul telefono.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Argo.colori.tenue,
                        textAlign = TextAlign.Center,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(incontri, key = { it.id }) { incontro ->
                        RigaIncontro(incontro, fasi[incontro.id]) { apriIncontro(incontro.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun RigaIncontro(incontro: Incontro, fase: String?, apri: () -> Unit) {
    Card(
        onClick = apri,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    incontro.titoloMostrato,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(4.dp))
                val dettagli = buildList {
                    add(Formato.dataBreve(incontro.creata))
                    add(Formato.durata(incontro.durata))
                    if (incontro.foto.isNotEmpty()) add("${incontro.foto.size} foto")
                }.joinToString(" · ")
                Text(dettagli, style = MaterialTheme.typography.bodySmall, color = Argo.colori.tenue)
                if (fase != null) {
                    Text(fase, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(12.dp))
            IconaStato(incontro, fase != null)
        }
    }
}

@Composable
private fun IconaStato(incontro: Incontro, inElaborazione: Boolean) {
    val (icona, colore, descrizione) = when {
        inElaborazione -> Triple(null, MaterialTheme.colorScheme.primary, "In elaborazione")
        incontro.stato == StatoIncontro.ERRORE -> Triple(Icons.Outlined.ErrorOutline, Argo.colori.errore, "Da sistemare")
        incontro.stato == StatoIncontro.TRASCRITTO || incontro.stato == StatoIncontro.COMPLETATO ->
            Triple(Icons.Outlined.TaskAlt, Argo.colori.ok, "Trascritto")
        else -> Triple(Icons.Outlined.Timelapse, Argo.colori.avviso, "In attesa")
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics { contentDescription = descrizione }) {
        if (incontro.foto.isNotEmpty()) {
            Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = Argo.colori.tenue, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        if (icona == null) CircularProgressIndicator(Modifier.size(20.dp).clip(CircleShape), strokeWidth = 2.dp)
        else Icon(icona, contentDescription = null, tint = colore)
    }
}
