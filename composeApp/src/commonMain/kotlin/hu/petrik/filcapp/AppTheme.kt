package hu.petrik.filcapp

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.floor

object AppSettings {
    private val _accentHue = mutableFloatStateOf(AppPreferences.getAccentHue())

    var accentHue: Float
        get() = _accentHue.floatValue
        set(value) {
            _accentHue.floatValue = value
            AppPreferences.setAccentHue(value)
        }
}

fun hslColor(hue: Float, saturation: Float, lightness: Float): Color {
    val c = (1f - abs(2f * lightness - 1f)) * saturation
    val h = hue / 60f
    val x = c * (1f - abs(h % 2f - 1f))
    val m = lightness - c / 2f
    val (r1, g1, b1) = when (floor(h).toInt().coerceIn(0, 5)) {
        0 -> Triple(c, x, 0f)
        1 -> Triple(x, c, 0f)
        2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c)
        4 -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(
        red = (r1 + m).coerceIn(0f, 1f),
        green = (g1 + m).coerceIn(0f, 1f),
        blue = (b1 + m).coerceIn(0f, 1f),
    )
}

fun buildColorScheme(hue: Float): ColorScheme = lightColorScheme(
    primary = hslColor(hue, 0.65f, 0.38f),
    onPrimary = Color.White,
    primaryContainer = hslColor(hue, 0.75f, 0.92f),
    onPrimaryContainer = hslColor(hue, 0.65f, 0.15f),
    secondary = hslColor((hue + 40f) % 360f, 0.30f, 0.40f),
    onSecondary = Color.White,
    secondaryContainer = hslColor((hue + 40f) % 360f, 0.35f, 0.90f),
    onSecondaryContainer = hslColor((hue + 40f) % 360f, 0.30f, 0.15f),
    tertiary = hslColor((hue + 80f) % 360f, 0.35f, 0.40f),
    onTertiary = Color.White,
    tertiaryContainer = hslColor((hue + 80f) % 360f, 0.40f, 0.90f),
    onTertiaryContainer = hslColor((hue + 80f) % 360f, 0.35f, 0.15f),
    background = Color(0xFFFDFCFF),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFDFCFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = hslColor(hue, 0.18f, 0.88f),
    onSurfaceVariant = hslColor(hue, 0.10f, 0.30f),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = hslColor(hue, 0.10f, 0.52f),
    outlineVariant = hslColor(hue, 0.14f, 0.80f),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = hslColor(hue, 0.75f, 0.78f),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = buildColorScheme(AppSettings.accentHue), content = content)
}
