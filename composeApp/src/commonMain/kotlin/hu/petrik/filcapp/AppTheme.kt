package hu.petrik.filcapp

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.Color
import kotlin.math.abs
import kotlin.math.floor

object AppSettings {
    private val _accentHue = mutableFloatStateOf(AppPreferences.getAccentHue())
    private val _themeMode = mutableFloatStateOf(AppPreferences.getThemeMode().toFloat())

    var accentHue: Float
        get() = _accentHue.floatValue
        set(value) {
            _accentHue.floatValue = value
            AppPreferences.setAccentHue(value)
        }

    // 0 = system, 1 = light, 2 = dark
    var themeMode: Int
        get() = _themeMode.floatValue.toInt()
        set(value) {
            _themeMode.floatValue = value.toFloat()
            AppPreferences.setThemeMode(value)
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
    background = hslColor(hue, 0.06f, 0.99f),
    onBackground = hslColor(hue, 0.08f, 0.10f),
    surface = hslColor(hue, 0.06f, 0.99f),
    onSurface = hslColor(hue, 0.08f, 0.10f),
    surfaceVariant = hslColor(hue, 0.18f, 0.88f),
    onSurfaceVariant = hslColor(hue, 0.10f, 0.30f),
    surfaceBright = hslColor(hue, 0.06f, 0.99f),
    surfaceDim = hslColor(hue, 0.10f, 0.87f),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = hslColor(hue, 0.12f, 0.96f),
    surfaceContainer = hslColor(hue, 0.14f, 0.94f),
    surfaceContainerHigh = hslColor(hue, 0.16f, 0.91f),
    surfaceContainerHighest = hslColor(hue, 0.18f, 0.88f),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    outline = hslColor(hue, 0.10f, 0.52f),
    outlineVariant = hslColor(hue, 0.14f, 0.80f),
    scrim = Color(0xFF000000),
    inverseSurface = hslColor(hue, 0.08f, 0.12f),
    inverseOnSurface = hslColor(hue, 0.06f, 0.95f),
    inversePrimary = hslColor(hue, 0.75f, 0.78f),
)

fun buildDarkColorScheme(hue: Float): ColorScheme = darkColorScheme(
    primary = hslColor(hue, 0.75f, 0.75f),
    onPrimary = hslColor(hue, 0.65f, 0.18f),
    primaryContainer = hslColor(hue, 0.60f, 0.28f),
    onPrimaryContainer = hslColor(hue, 0.75f, 0.90f),
    secondary = hslColor((hue + 40f) % 360f, 0.35f, 0.70f),
    onSecondary = hslColor((hue + 40f) % 360f, 0.30f, 0.15f),
    secondaryContainer = hslColor((hue + 40f) % 360f, 0.25f, 0.28f),
    onSecondaryContainer = hslColor((hue + 40f) % 360f, 0.35f, 0.88f),
    tertiary = hslColor((hue + 80f) % 360f, 0.40f, 0.72f),
    onTertiary = hslColor((hue + 80f) % 360f, 0.35f, 0.15f),
    tertiaryContainer = hslColor((hue + 80f) % 360f, 0.30f, 0.26f),
    onTertiaryContainer = hslColor((hue + 80f) % 360f, 0.40f, 0.88f),
    background = hslColor(hue, 0.08f, 0.08f),
    onBackground = hslColor(hue, 0.08f, 0.90f),
    surface = hslColor(hue, 0.08f, 0.08f),
    onSurface = hslColor(hue, 0.08f, 0.90f),
    surfaceVariant = hslColor(hue, 0.12f, 0.18f),
    onSurfaceVariant = hslColor(hue, 0.10f, 0.72f),
    surfaceBright = hslColor(hue, 0.10f, 0.26f),
    surfaceDim = hslColor(hue, 0.08f, 0.06f),
    surfaceContainerLowest = hslColor(hue, 0.06f, 0.04f),
    surfaceContainerLow = hslColor(hue, 0.08f, 0.10f),
    surfaceContainer = hslColor(hue, 0.10f, 0.14f),
    surfaceContainerHigh = hslColor(hue, 0.12f, 0.18f),
    surfaceContainerHighest = hslColor(hue, 0.14f, 0.22f),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    outline = hslColor(hue, 0.10f, 0.50f),
    outlineVariant = hslColor(hue, 0.12f, 0.28f),
    scrim = Color(0xFF000000),
    inverseSurface = hslColor(hue, 0.08f, 0.90f),
    inverseOnSurface = hslColor(hue, 0.06f, 0.14f),
    inversePrimary = hslColor(hue, 0.65f, 0.38f),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (AppSettings.themeMode) {
        1 -> false
        2 -> true
        else -> systemDark
    }
    val colorScheme = if (isDark) buildDarkColorScheme(AppSettings.accentHue) else buildColorScheme(AppSettings.accentHue)
    MaterialTheme(colorScheme = colorScheme, content = content)
}
