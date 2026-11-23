package blog.argosnewdreams.argo.ui

import blog.argosnewdreams.argo.R
import androidx.compose.ui.res.stringResource
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
import blog.argosnewdreams.argo.trascrizione.ModelloLocale
import blog.argosnewdreams.argo.Testi
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
                title = { Text(stringResource(R.string.impostazioni)) },
                navigationIcon = { IconButton(onClick = indietro) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.indietro)) } },
            )
        },
    ) { margini ->
        Column(
            Modifier.padding(margini).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LinguaAppRiga()

            Sezione(stringResource(R.string.lingua_predefinita))
            MotoreTrascrizione.lingueProposte.forEach { codice ->
                Scelta(MotoreTrascrizione.nomeLingua(codice), null, codice == lingua) { imp.impostaLingua(codice) }
            }

            Sezione(stringResource(R.string.trascrizione))
            Modalita.entries.forEach { m ->
                Scelta(stringResource(m.idEtichetta), stringResource(m.idDescrizione, ModelloLocale.MEGABYTE), m == modalita) { imp.impostaModalita(m) }
            }
            CartaMotore(app, rememberStatoMotore(app, lingua, modalita))
            ModelloLocaleRiga(app)

            Sezione(stringResource(R.string.privacy))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.elimina_audio))
                    Text(
                        stringResource(R.string.elimina_audio_spiegazione),
                        style = MaterialTheme.typography.bodySmall, color = Argo.colori.tenue,
                    )
                }
                Switch(checked = eliminaAudio, onCheckedChange = imp::impostaEliminaAudio)
            }
            Text(
                stringResource(R.string.promessa),
                style = MaterialTheme.typography.bodySmall, color = Argo.colori.tenue,
                modifier = Modifier.padding(top = 8.dp),
            )
            InformativaRiga()

            Sezione(stringResource(R.string.crediti))
            Text(
                stringResource(R.string.crediti_testo),
                style = MaterialTheme.typography.bodySmall, color = Argo.colori.tenue,
            )
            LicenzeRiga()

            Sezione(stringResource(R.string.informazioni))
            Text(stringResource(R.string.versione_build, BuildConfig.VERSION_NAME, BuildConfig.BUILD_ARGO), style = MaterialTheme.typography.bodyMedium)
            Text("Argo's New Dreams · argosnewdreams.blog", style = MaterialTheme.typography.bodySmall, color = Argo.colori.tenue)
            Spacer(Modifier.size(24.dp))
        }
    }
}

/** Link all'informativa sulla privacy sul blog, nella lingua dell'app (richiesto da Google Play). */
@Composable
private fun InformativaRiga() {
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    val indirizzo = stringResource(R.string.url_informativa)
    androidx.compose.material3.TextButton(onClick = { runCatching { uri.openUri(indirizzo) } }) {
        Text(stringResource(R.string.informativa_privacy))
    }
}

/** Testo completo delle licenze di terze parti (assets/licenze_terze_parti.txt, come THIRD_PARTY_LICENSES.md). */
@Composable
private fun LicenzeRiga() {
    val contesto = androidx.compose.ui.platform.LocalContext.current
    var aperto by remember { mutableStateOf(false) }
    androidx.compose.material3.TextButton(onClick = { aperto = true }) { Text(stringResource(R.string.licenze)) }
    if (aperto) {
        val nonTrovate = stringResource(R.string.licenze_non_trovate)
        val testo = remember {
            runCatching { contesto.assets.open("licenze_terze_parti.txt").bufferedReader().use { it.readText() } }
                .getOrDefault(nonTrovate)
        }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { aperto = false },
            title = { Text(stringResource(R.string.licenze)) },
            text = {
                Text(
                    testo,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { aperto = false }) { Text(stringResource(R.string.chiudi)) } },
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
            stringResource(R.string.spazio_modello, ModelloLocale.MEGABYTE),
            style = MaterialTheme.typography.bodySmall, color = Argo.colori.tenue, modifier = Modifier.weight(1f),
        )
        androidx.compose.material3.TextButton(onClick = { conferma = true }) { Text(stringResource(R.string.elimina_modello)) }
    }
    if (conferma) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { conferma = false },
            title = { Text(stringResource(R.string.eliminare_modello)) },
            text = { Text(stringResource(R.string.eliminare_modello_spiegazione)) },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { conferma = false; app.modelloLocale.elimina() }) { Text(stringResource(R.string.elimina)) } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { conferma = false }) { Text(stringResource(R.string.annulla)) } },
        )
    }
}

/**
 * Lingua dell'interfaccia: italiano, inglese, spagnolo, tedesco o come il telefono.
 * Da Android 13 si sceglie qui (o nelle impostazioni di sistema dell'app); su Android 12 segue il telefono.
 */
@Composable
private fun LinguaAppRiga() {
    Sezione(stringResource(R.string.lingua_app))
    if (android.os.Build.VERSION.SDK_INT < 33) {
        Text(stringResource(R.string.lingua_app_android12), style = MaterialTheme.typography.bodySmall, color = Argo.colori.tenue)
        return
    }
    var scelta by remember { mutableStateOf(Testi.linguaScelta()) }
    Scelta(stringResource(R.string.lingua_app_sistema), null, scelta == null) { scelta = null; Testi.impostaLingua(null) }
    Testi.lingueApp.forEach { tag ->
        // Ogni lingua scritta nella sua lingua: chi non capisce l'interfaccia ritrova la propria
        val locale = java.util.Locale.forLanguageTag(tag)
        val nome = locale.getDisplayLanguage(locale).replaceFirstChar { it.titlecase(locale) }
        Scelta(nome, null, scelta == tag) { scelta = tag; Testi.impostaLingua(tag) }
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
