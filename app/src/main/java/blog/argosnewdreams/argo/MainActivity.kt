package blog.argosnewdreams.argo

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Bundle
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import blog.argosnewdreams.argo.ui.ElencoScreen
import blog.argosnewdreams.argo.ui.ImpostazioniScreen
import blog.argosnewdreams.argo.ui.IncontroScreen
import blog.argosnewdreams.argo.ui.RegistraScreen
import blog.argosnewdreams.argo.ui.TemaArgo

class MainActivity : ComponentActivity() {

    /** Scorciatoia «Registra» (icona tenuta premuta o comando vocale che la apre): si va alla registrazione. */
    private var daScorciatoia by mutableStateOf(false)

    /** Scorciatoia «Registra» sull'icona (creata dal codice, così vale anche per le versioni Lab con un altro id). */
    private fun pubblicaScorciatoie() {
        runCatching {
            val registra = ShortcutInfo.Builder(this, "registra")
                .setShortLabel(getString(R.string.registra))
                .setLongLabel(getString(R.string.registra))
                .setIcon(Icon.createWithResource(this, R.mipmap.ic_launcher))
                .setIntent(Intent(AZIONE_REGISTRA).setClass(this, MainActivity::class.java))
                .build()
            getSystemService(ShortcutManager::class.java)?.dynamicShortcuts = listOf(registra)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == AZIONE_REGISTRA) daScorciatoia = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ArgoApp
        pubblicaScorciatoie()
        // Riaperta dalla notifica durante una registrazione: si va dritti alla registrazione.
        val partenza = if (app.registrazione.attiva.value != null || intent?.action == AZIONE_REGISTRA) "registra" else "elenco"

        setContent {
            TemaArgo {
                var errorePrecedente by remember { mutableStateOf(RegistroCrash.leggi(this)) }
                errorePrecedente?.let { testo ->
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text(stringResource(R.string.crash_titolo)) },
                        text = {
                            Text(
                                testo,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, getString(R.string.crash_oggetto))
                                    putExtra(Intent.EXTRA_TEXT, testo)
                                }, getString(R.string.crash_condividi)))
                            }) { Text(stringResource(R.string.condividi)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { RegistroCrash.cancella(this); errorePrecedente = null }) { Text(stringResource(R.string.chiudi)) }
                        },
                    )
                }
                val nav = rememberNavController()
                LaunchedEffect(daScorciatoia) {
                    if (daScorciatoia) {
                        daScorciatoia = false
                        nav.navigate("registra") { launchSingleTop = true }
                    }
                }
                NavHost(navController = nav, startDestination = partenza) {
                    composable("elenco") {
                        ElencoScreen(
                            app,
                            apriIncontro = { nav.navigate("incontro/$it") },
                            registra = { nav.navigate("registra") { launchSingleTop = true } },
                            impostazioni = { nav.navigate("impostazioni") },
                        )
                    }
                    composable("registra") {
                        RegistraScreen(
                            app,
                            indietro = {
                                if (!nav.popBackStack()) nav.navigate("elenco") { popUpTo(0) }
                            },
                            alTermine = { id ->
                                nav.navigate("incontro/$id") { popUpTo("elenco") }
                            },
                        )
                    }
                    composable("incontro/{id}") { voce ->
                        IncontroScreen(app, voce.arguments?.getString("id").orEmpty(), indietro = {
                            if (!nav.popBackStack()) nav.navigate("elenco") { popUpTo(0) }
                        })
                    }
                    composable("impostazioni") {
                        ImpostazioniScreen(app, indietro = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}

/** Azione della scorciatoia «Registra» (res/xml/scorciatoie.xml). */
const val AZIONE_REGISTRA = "blog.argosnewdreams.argo.REGISTRA"
