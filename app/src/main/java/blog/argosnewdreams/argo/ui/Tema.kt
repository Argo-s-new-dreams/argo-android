package blog.argosnewdreams.argo.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * «Stile Argo», gli stessi colori di Argo sul computer (`app/tema.py`, Brand Book di Argo's New Dreams):
 * Notte (scuro) e Carta (chiaro), terracotta come accento, salvia per «fatto», ocra per gli avvisi.
 * Il colore non è mai l'unico segnale: c'è sempre anche un'icona o un testo.
 */
data class ColoriArgo(
    val ok: Color,
    val avviso: Color,
    val errore: Color,
    val tenue: Color,
    val parlanti: List<Color>,
)

private val notte = ColoriArgo(
    ok = Color(0xFF9CC9A6), avviso = Color(0xFFEEC29B), errore = Color(0xFFF19A8E), tenue = Color(0xFFA99C8E),
    parlanti = listOf(0xFFE58074, 0xFFEEC29B, 0xFF8FB3C4, 0xFFA9C3A0, 0xFFC7A2C0, 0xFFD9B88F, 0xFFF0A8A0, 0xFF9FC6BE).map { Color(it) },
)
private val carta = ColoriArgo(
    ok = Color(0xFF356B4C), avviso = Color(0xFF8A5413), errore = Color(0xFFA63426), tenue = Color(0xFF5E554C),
    parlanti = listOf(0xFFAE4E39, 0xFF8A5413, 0xFF3F6F86, 0xFF4E7A4A, 0xFF8A4E80, 0xFF8C6A3E, 0xFFA5503C, 0xFF2F7A70).map { Color(it) },
)

private val schemaNotte = darkColorScheme(
    primary = Color(0xFFE58074), onPrimary = Color(0xFF1C2124),
    primaryContainer = Color(0xFF3A2C28), onPrimaryContainer = Color(0xFFF1E8DC),
    secondary = Color(0xFFEEC29B), onSecondary = Color(0xFF1C2124),
    secondaryContainer = Color(0xFF2B3236), onSecondaryContainer = Color(0xFFF1E8DC),
    tertiary = Color(0xFF9CC9A6),
    background = Color(0xFF1C2124), onBackground = Color(0xFFF1E8DC),
    surface = Color(0xFF1C2124), onSurface = Color(0xFFF1E8DC),
    surfaceVariant = Color(0xFF2B3236), onSurfaceVariant = Color(0xFFA99C8E),
    surfaceContainerLowest = Color(0xFF14181A), surfaceContainerLow = Color(0xFF1F2528),
    surfaceContainer = Color(0xFF23292C), surfaceContainerHigh = Color(0xFF2B3236), surfaceContainerHighest = Color(0xFF323A3E),
    outline = Color(0xFF7A848A), outlineVariant = Color(0xFF323A3E),
    error = Color(0xFFF19A8E), onError = Color(0xFF1C2124),
)

private val schemaCarta = lightColorScheme(
    primary = Color(0xFFAE4E39), onPrimary = Color.White,
    primaryContainer = Color(0xFFF5DDD4), onPrimaryContainer = Color(0xFF1A1E20),
    secondary = Color(0xFF8A5413), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6EFE5), onSecondaryContainer = Color(0xFF1A1E20),
    tertiary = Color(0xFF356B4C),
    background = Color(0xFFEDE4D6), onBackground = Color(0xFF1A1E20),
    surface = Color(0xFFEDE4D6), onSurface = Color(0xFF1A1E20),
    surfaceVariant = Color(0xFFF6EFE5), onSurfaceVariant = Color(0xFF5E554C),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFFFFDF9),
    surfaceContainer = Color(0xFFFFFDF9), surfaceContainerHigh = Color(0xFFF6EFE5), surfaceContainerHighest = Color(0xFFE9DFD1),
    outline = Color(0xFF8C7963), outlineVariant = Color(0xFFD9CAB6),
    error = Color(0xFFA63426), onError = Color.White,
)

val LocalColoriArgo = staticCompositionLocalOf { notte }

@Composable
fun TemaArgo(contenuto: @Composable () -> Unit) {
    val scuro = isSystemInDarkTheme()
    CompositionLocalProvider(LocalColoriArgo provides if (scuro) notte else carta) {
        MaterialTheme(colorScheme = if (scuro) schemaNotte else schemaCarta, content = contenuto)
    }
}

object Argo {
    val colori: ColoriArgo @Composable get() = LocalColoriArgo.current
}
