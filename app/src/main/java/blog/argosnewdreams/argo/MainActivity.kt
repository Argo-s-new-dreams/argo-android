package blog.argosnewdreams.argo

import android.os.Bundle
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
