package blog.argosnewdreams.argo

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ArgoApp
        // Riaperta dalla notifica durante una registrazione: si va dritti alla registrazione.
        val partenza = if (app.registrazione.attiva.value != null) "registra" else "elenco"

        setContent {
            TemaArgo {
                var errorePrecedente by remember { mutableStateOf(RegistroCrash.leggi(this)) }
                errorePrecedente?.let { testo ->
                    AlertDialog(
                        onDismissRequest = {},
                        title = { Text("Argo si è chiuso per un errore") },
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
                                    putExtra(Intent.EXTRA_SUBJECT, "Errore di Argo per Android")
                                    putExtra(Intent.EXTRA_TEXT, testo)
                                }, "Condividi l'errore"))
                            }) { Text("Condividi") }
                        },
                        dismissButton = {
                            TextButton(onClick = { RegistroCrash.cancella(this); errorePrecedente = null }) { Text("Chiudi") }
                        },
                    )
                }
                val nav = rememberNavController()
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
