package blog.argosnewdreams.argo.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import blog.argosnewdreams.argo.ArgoApp
import blog.argosnewdreams.argo.foto.LettoreFoto
import blog.argosnewdreams.argo.modello.Formato
import blog.argosnewdreams.argo.trascrizione.MotoreTrascrizione
import blog.argosnewdreams.argo.trascrizione.StatoModello
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistraScreen(app: ArgoApp, indietro: () -> Unit, alTermine: (String) -> Unit) {
    val attiva by app.registrazione.attiva.collectAsState()
    var mostraFotocamera by remember { mutableStateOf(false) }

    if (attiva != null && mostraFotocamera) {
        FotocameraScreen(app, chiudi = { mostraFotocamera = false })
        return
    }
    if (attiva != null) {
        PannelloRegistrazione(app, indietro, alTermine, apriFotocamera = { mostraFotocamera = true })
    } else {
        Preparazione(app, indietro)
    }
}

// ------------------------------------------------------------------ prima di registrare

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Preparazione(app: ArgoApp, indietro: () -> Unit) {
    val context = LocalContext.current
    val linguaPredefinita by app.impostazioni.lingua.collectAsState()
    val modalita by app.impostazioni.modalita.collectAsState()
    var lingua by remember(linguaPredefinita) { mutableStateOf(linguaPredefinita) }
    var errore by remember { mutableStateOf<String?>(null) }
    val motore = rememberStatoMotore(app, lingua, modalita)

    fun avvia() {
        try {
            // Se il motore non è pronto si registra lo stesso: trascrizione a fine incontro.
            app.registrazione.avvia(lingua, motore.consegna())
        } catch (e: Exception) {
            motore.consegnato = false
            errore = e.message ?: "Non riesco ad avviare la registrazione."
        }
    }

    val permessi = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { esiti ->
        if (esiti[Manifest.permission.RECORD_AUDIO] == true) avvia()
        else errore = "Serve l'accesso al microfono: lo trovi in Impostazioni › App › Argo › Autorizzazioni."
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Prima di registrare") },
                navigationIcon = { IconButton(onClick = indietro) { Icon(Icons.Outlined.Close, contentDescription = "Chiudi") } },
            )
        },
    ) { margini ->
        Column(
            Modifier.padding(margini).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Punto(Icons.Outlined.Campaign, "Avvisa chi hai di fronte che la conversazione viene registrata e trascritta con l'AI, e chiedi il consenso.")
            Punto(Icons.Outlined.CloudOff, "Audio, trascrizione e foto restano su questo telefono. La relazione la scrive Argo sul tuo computer: mai servizi cloud.")
            Punto(Icons.Outlined.DeleteOutline, "L'audio viene eliminato appena trascritto: restano testo e foto.")
            Punto(Icons.Outlined.VerifiedUser, "La relazione generata dall'AI va riletta prima di condividerla.")

            Text("Lingua dell'incontro", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MotoreTrascrizione.lingueProposte) { (codice, nome) ->
                    FilterChip(selected = codice == lingua, onClick = { lingua = codice }, label = { Text(nome) })
                }
            }

            CartaMotore(app, motore)

            Button(
                onClick = {
                    val necessari = buildList {
                        add(Manifest.permission.RECORD_AUDIO)
                        if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    val mancano = necessari.filter {
                        ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                    }
                    if (mancano.isEmpty()) avvia() else permessi.launch(mancano.toTypedArray())
                },
                enabled = motore.stato != null,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp).height(52.dp),
            ) {
                Text("Ho informato i partecipanti · Registra")
            }
        }
    }

    errore?.let { messaggio ->
        AlertDialog(
            onDismissRequest = { errore = null },
            confirmButton = { TextButton(onClick = { errore = null }) { Text("OK") } },
            title = { Text("Attenzione") },
            text = { Text(messaggio) },
        )
    }
}

@Composable
private fun Punto(icona: androidx.compose.ui.graphics.vector.ImageVector, testo: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icona, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
        Spacer(Modifier.width(14.dp))
        Text(testo, style = MaterialTheme.typography.bodyLarge)
    }
}

// ------------------------------------------------------------------ durante la registrazione

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PannelloRegistrazione(app: ArgoApp, indietro: () -> Unit, alTermine: (String) -> Unit, apriFotocamera: () -> Unit) {
    val ambito = rememberCoroutineScope()
    val reg = app.registrazione
    val attiva by reg.attiva.collectAsState()
    val durata by reg.durata.collectAsState()
    val livello by reg.livello.collectAsState()
    val inPausa by reg.inPausa.collectAsState()
    val segmenti by reg.segmenti.collectAsState()
    val parziale by reg.parziale.collectAsState()
    val foto by reg.foto.collectAsState()
    var confermaScarta by remember { mutableStateOf(false) }
    var fermando by remember { mutableStateOf(false) }
    val elenco = rememberLazyListState()

    // Lo schermo resta acceso mentre guardi la trascrizione; se lo spegni, la registrazione continua.
    val vista = LocalView.current
    DisposableEffect(Unit) {
        vista.keepScreenOn = true
        onDispose { vista.keepScreenOn = false }
    }
    LaunchedEffect(segmenti.size, parziale) {
        val ultimo = segmenti.size + (if (parziale.isNotEmpty()) 1 else 0) - 1
        if (ultimo >= 0) elenco.animateScrollToItem(ultimo)
    }
    BackHandler { indietro() } // si torna all'elenco, la registrazione continua

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (inPausa) "In pausa" else "Sto registrando") },
                navigationIcon = {
                    IconButton(onClick = indietro) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Torna agli incontri, la registrazione continua") }
                },
                actions = {
                    TextButton(onClick = { confermaScarta = true }) { Text("Scarta") }
                },
            )
        },
    ) { margini ->
        Column(Modifier.padding(margini).fillMaxSize().padding(horizontal = 20.dp)) {
            Text(
                Formato.durata(durata),
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            MisuratoreLivello(if (inPausa) 0f else livello, Modifier.fillMaxWidth().height(48.dp))
            Text(
                if (attiva?.dalVivo == true) "Trascrivo sul telefono mentre parli · l'audio viene eliminato dopo"
                else "Registro · la trascrizione partirà a fine incontro",
                style = MaterialTheme.typography.bodySmall,
                color = Argo.colori.tenue,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp),
            )

            LazyColumn(
                state = elenco,
                modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(segmenti) { s ->
                    Row {
                        Text(Formato.durata(s.inizio), style = MaterialTheme.typography.labelSmall, color = Argo.colori.tenue, modifier = Modifier.width(48.dp).padding(top = 3.dp))
                        Text(s.testo, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (parziale.isNotEmpty()) item {
                    Row {
                        Spacer(Modifier.width(48.dp))
                        Text(parziale, style = MaterialTheme.typography.bodyMedium, color = Argo.colori.tenue)
                    }
                }
            }

            if (foto.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    items(foto, key = { it.nomeFile }) { f ->
                        val id = attiva?.id
                        val miniatura = remember(f.nomeFile) { id?.let { LettoreFoto.miniatura(app.archivio.fileFoto(it, f.nomeFile), 128) } }
                        Box(Modifier.size(56.dp).clip(RoundedCornerShape(10.dp))) {
                            miniatura?.let { Image(it.asImageBitmap(), contentDescription = "Foto al minuto ${Formato.durata(f.secondo)}", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(
                    onClick = { if (inPausa) reg.riprendi() else reg.pausa() },
                    modifier = Modifier.size(68.dp),
                ) {
                    Icon(
                        if (inPausa) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                        contentDescription = if (inPausa) "Riprendi la registrazione" else "Metti in pausa",
                        modifier = Modifier.size(30.dp),
                    )
                }
                FilledIconButton(
                    onClick = {
                        if (fermando) return@FilledIconButton
                        fermando = true
                        ambito.launch {
                            val id = reg.ferma()
                            if (id != null) alTermine(id) else indietro()
                        }
                    },
                    modifier = Modifier.size(84.dp),
                    shape = CircleShape,
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                ) {
                    if (fermando) CircularProgressIndicator(Modifier.size(28.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 3.dp)
                    else Icon(Icons.Outlined.Stop, contentDescription = "Ferma e salva l'incontro", modifier = Modifier.size(38.dp))
                }
                FilledTonalIconButton(onClick = apriFotocamera, modifier = Modifier.size(68.dp)) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = "Scatta una foto", modifier = Modifier.size(30.dp))
                }
            }
        }
    }

    if (confermaScarta) {
        AlertDialog(
            onDismissRequest = { confermaScarta = false },
            title = { Text("Scartare la registrazione?") },
            text = { Text("Audio, trascrizione e foto di questo incontro verranno eliminati.") },
            confirmButton = {
                TextButton(onClick = {
                    confermaScarta = false
                    ambito.launch { reg.scarta(); indietro() }
                }) { Text("Scarta", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confermaScarta = false }) { Text("Continua a registrare") } },
        )
    }
}

/** Barre a campana come su iPhone: quelle centrali reagiscono di più. */
@Composable
private fun MisuratoreLivello(livello: Float, modifier: Modifier) {
    val colore = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val barre = 32
        val spazio = 3.dp.toPx()
        val larghezza = (size.width - spazio * (barre - 1)) / barre
        for (i in 0 until barre) {
            val distanza = abs(i - (barre - 1) / 2f) / (barre / 2f)
            val altezza = maxOf(4.dp.toPx(), size.height * livello * (1 - distanza * 0.7f))
            drawRoundRect(
                color = colore,
                topLeft = Offset(i * (larghezza + spazio), (size.height - altezza) / 2),
                size = Size(larghezza, altezza),
                cornerRadius = CornerRadius(larghezza / 2),
            )
        }
    }
}
