package hu.petrik.filcapp.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hu.petrik.filcapp.settings.AppSettings
import hu.petrik.filcapp.settings.AppThemeMode
import hu.petrik.filcapp.settings.DEFAULT_PRIMARY_COLOR_ARGB

// Filc webes színpaletta
// Forrás: filcdev/filc
// packages/ui/src/styles/globals.css

// Világos téma

val FilcLightBackground = Color(0xFFFFFFFF)
val FilcLightSurface = Color(0xFFFFFFFF)
val FilcLightSurfaceAlt = Color(0xFFF4F4F5)
val FilcLightText = Color(0xFF09090B)
val FilcLightMuted = Color(0xFF71717B)
val FilcLightOutline = Color(0xFFE4E4E7)

// Sötét téma

val FilcDarkBackground = Color(0xFF09090B)
val FilcDarkSurface = Color(0xFF18181B)
val FilcDarkSurfaceAlt = Color(0xFF27272A)
val FilcDarkText = Color(0xFFFAFAFA)
val FilcDarkMuted = Color(0xFF9F9FA9)
val FilcDarkOutline = Color(0xFF3F3F46)

// Kiemelőszínek

private val FilcLightPrimary = Color(0xFF009869)
private val FilcDarkPrimary = Color(0xFF15BA81)

// Lekerekítések

private val FilcShapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(20.dp),
    )

// Tipográfia

private val FilcTypography =
    Typography(
        headlineSmall = TextStyle(
            fontSize = 30.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.75).sp,
        ),

        titleLarge = TextStyle(
            fontSize = 22.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.25).sp,
        ),

        titleMedium = TextStyle(
            fontSize = 17.sp,
            lineHeight = 22.sp,
            fontWeight = FontWeight.SemiBold,
        ),

        titleSmall = TextStyle(
            fontSize = 15.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.SemiBold,
        ),

        bodyLarge = TextStyle(
            fontSize = 16.sp,
            lineHeight = 23.sp,
        ),

        bodyMedium = TextStyle(
            fontSize = 15.sp,
            lineHeight = 21.sp,
        ),

        bodySmall = TextStyle(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
        ),

        labelLarge = TextStyle(
            fontSize = 15.sp,
            lineHeight = 21.sp,
            fontWeight = FontWeight.Bold,
        ),

        labelMedium = TextStyle(
            fontSize = 13.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
        ),

        labelSmall = TextStyle(
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.Medium,
        ),
    )

@Composable
fun FilcTheme(content: @Composable () -> Unit) {

    // Világos / sötét / rendszer téma

    val darkTheme =
        when (AppSettings.themeMode.value) {
            AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            AppThemeMode.LIGHT -> false
            AppThemeMode.DARK -> true
        }

    // Felhasználó által választott szín

    val selectedColor = AppSettings.primaryColorArgb.value

    // Ha nincs egyedi választás, a Filc színeit használjuk.
    // Egyedi szín esetén a választott szín lesz az elsődleges.

    val primary =
        if (selectedColor == DEFAULT_PRIMARY_COLOR_ARGB) {
            if (darkTheme) {
                FilcDarkPrimary
            } else {
                FilcLightPrimary
            }
        } else {
            Color(selectedColor.toInt())
        }

    val onPrimary = readableOnColor(primary)

    // Material 3 színséma

    val colors =
        if (darkTheme) {

            darkColorScheme(
                primary = primary,
                onPrimary = onPrimary,

                primaryContainer =
                    blend(primary, FilcDarkSurface, 0.72f),
                onPrimaryContainer = FilcDarkText,

                secondary = FilcDarkSurfaceAlt,
                onSecondary = FilcDarkText,
                secondaryContainer = FilcDarkSurfaceAlt,
                onSecondaryContainer = FilcDarkText,

                tertiary = Color(0xFF3AD198),
                onTertiary = Color(0xFF002C22),
                tertiaryContainer = Color(0xFF147859),
                onTertiaryContainer = Color.White,

                background = FilcDarkBackground,
                onBackground = FilcDarkText,

                surface = FilcDarkSurface,
                onSurface = FilcDarkText,

                surfaceVariant = FilcDarkSurfaceAlt,
                onSurfaceVariant = FilcDarkMuted,

                surfaceContainerLowest = FilcDarkBackground,
                surfaceContainerLow = FilcDarkSurface,
                surfaceContainer = FilcDarkSurface,
                surfaceContainerHigh = FilcDarkSurfaceAlt,
                surfaceContainerHighest = FilcDarkSurfaceAlt,

                outline = FilcDarkOutline,
                outlineVariant = FilcDarkOutline,

                error = Color(0xFFFF6467),
                onError = Color(0xFF450A0A),

                inverseSurface = FilcLightSurface,
                inverseOnSurface = FilcLightText,
                inversePrimary = FilcLightPrimary,
            )

        } else {

            lightColorScheme(
                primary = primary,
                onPrimary = onPrimary,

                primaryContainer =
                    blend(primary, Color.White, 0.82f),
                onPrimaryContainer = FilcLightText,

                secondary = FilcLightSurfaceAlt,
                onSecondary = FilcLightText,
                secondaryContainer = FilcLightSurfaceAlt,
                onSecondaryContainer = FilcLightText,

                tertiary = Color(0xFF3AD198),
                onTertiary = Color(0xFF002C22),
                tertiaryContainer = Color(0xFF70E9B9),
                onTertiaryContainer = Color(0xFF002C22),

                background = FilcLightBackground,
                onBackground = FilcLightText,

                surface = FilcLightSurface,
                onSurface = FilcLightText,

                surfaceVariant = FilcLightSurfaceAlt,
                onSurfaceVariant = FilcLightMuted,

                surfaceContainerLowest = FilcLightSurface,
                surfaceContainerLow = FilcLightSurface,
                surfaceContainer = FilcLightSurfaceAlt,
                surfaceContainerHigh = FilcLightSurfaceAlt,
                surfaceContainerHighest = FilcLightSurfaceAlt,

                outline = FilcLightOutline,
                outlineVariant = FilcLightOutline,

                error = Color(0xFFCD5B60),
                onError = Color.White,

                inverseSurface = FilcDarkSurface,
                inverseOnSurface = FilcDarkText,
                inversePrimary = FilcDarkPrimary,
            )
        }

    // Android / iOS rendszersávok

    ApplyPlatformSystemBars(darkTheme = darkTheme)

    // Téma alkalmazása

    MaterialTheme(
        colorScheme = colors,
        typography = FilcTypography,
        shapes = FilcShapes,
        content = content,
    )
}

// Két szín összekeverése

private fun blend(
    start: Color,
    end: Color,
    fraction: Float,
): Color {

    val amount = fraction.coerceIn(0f, 1f)

    return Color(
        red = start.red + (end.red - start.red) * amount,
        green = start.green + (end.green - start.green) * amount,
        blue = start.blue + (end.blue - start.blue) * amount,
        alpha = 1f,
    )
}

// Olvasható szövegszín választása

private fun readableOnColor(color: Color): Color {

    val luminance =
        color.red * 0.299f +
            color.green * 0.587f +
            color.blue * 0.114f

    return if (luminance > 0.62f) {
        Color(0xFF09090B)
    } else {
        Color.White
    }
}
