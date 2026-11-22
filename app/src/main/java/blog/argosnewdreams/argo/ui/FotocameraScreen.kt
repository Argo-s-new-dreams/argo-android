package blog.argosnewdreams.argo.ui

import blog.argosnewdreams.argo.R
import androidx.compose.ui.res.stringResource
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import blog.argosnewdreams.argo.ArgoApp
import blog.argosnewdreams.argo.modello.Formato
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fotocamera dentro Argo: solo foto, mai video o audio, e la registrazione continua mentre scatti.
 * Ogni scatto viene agganciato al secondo della conversazione.
 */
@Composable
fun FotocameraScreen(app: ArgoApp, chiudi: () -> Unit) {
    val context = LocalContext.current
    val proprietario = LocalLifecycleOwner.current
    val ambito = rememberCoroutineScope()
    val durata by app.registrazione.durata.collectAsState()
    val scatti by app.registrazione.foto.collectAsState()
    var permesso by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var scattando by remember { mutableStateOf(false) }
    var conferma by remember { mutableStateOf<String?>(null) }
    val richiesta = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { permesso = it }

    LaunchedEffect(Unit) { if (!permesso) richiesta.launch(Manifest.permission.CAMERA) }
    LaunchedEffect(conferma) { if (conferma != null) { delay(1600); conferma = null } }
    BackHandler(onBack = chiudi)

    val controllo = remember {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (permesso) {
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).apply {
                        controller = controllo
                        controllo.bindToLifecycle(proprietario)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Column(Modifier.align(Alignment.Center).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.serve_fotocamera), color = Color.White)
                Spacer(Modifier.size(16.dp))
                Button(onClick = { richiesta.launch(Manifest.permission.CAMERA) }) { Text(stringResource(R.string.consenti)) }
            }
        }

        Row(
            Modifier.fillMaxWidth().systemBarsPadding().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = chiudi) { Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.torna_alla_registrazione), tint = Color.White) }
            Surface(color = Color.Black.copy(alpha = 0.55f), shape = RoundedCornerShape(50)) {
                Text(
                    stringResource(R.string.durata_foto, Formato.durata(durata), scatti.size),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
            Spacer(Modifier.size(48.dp))
        }

        conferma?.let { testo ->
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.Center),
            ) { Text(testo, color = Color.White, modifier = Modifier.padding(16.dp)) }
        }

        if (permesso) {
            FilledTonalIconButton(
                onClick = {
                    if (scattando) return@FilledTonalIconButton
                    scattando = true
                    controllo.takePicture(
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(immagine: ImageProxy) {
                                val rotazione = immagine.imageInfo.rotationDegrees
                                val bitmap = immagine.toBitmap()
                                immagine.close()
                                ambito.launch {
                                    runCatching { app.registrazione.aggiungiFoto(bitmap, rotazione) }
                                        .onSuccess { conferma = context.getString(R.string.foto_agganciata, Formato.durata(app.registrazione.durata.value)) }
                                        .onFailure { conferma = context.getString(R.string.foto_non_salvata) }
                                    scattando = false
                                }
                            }

                            override fun onError(eccezione: ImageCaptureException) {
                                conferma = context.getString(R.string.scatto_non_riuscito)
                                scattando = false
                            }
                        },
                    )
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .systemBarsPadding()
                    .padding(bottom = 32.dp)
                    .size(80.dp)
                    .border(4.dp, Color.White, CircleShape),
            ) {
                if (scattando) CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
            }
        }
    }
}
