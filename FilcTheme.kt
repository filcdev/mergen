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

val FilcDarkBackground = Color(0xFF0B1120)
val FilcDarkSurface = Color(0xFF1E293B)
val FilcDarkSurfaceAlt = Color(0xFF18213A)
val FilcDarkText = Color(0xFFF1F5F9)
val FilcDarkMuted = Color(0xFF94A3B8)
val FilcDarkOutline = Color(0xFF334155)

private val FilcLightBackground = Color(0xFFF6F7FB)
private val FilcLightSurface = Color(0xFFFFFFFF)
private val FilcLightSurfaceAlt = Color(0xFFF0F2F8)
private val FilcLightText = Color(0xFF151826)
private val FilcLightMuted = Color(0xFF64748B)
private val FilcLightOutline = Color(0xFFD8DCE7)

private val FilcV3Shapes =
    Shapes(
        extraSmall = RoundedCornerShape(4.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(20.dp),
    )

private val FilcV3Typography =
    Typography(
        headlineSmall =
            TextStyle(
                fontSize = 30.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.75).sp,
            ),
        titleLarge =
            TextStyle(
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.25).sp,
            ),
        titleMedium =
            TextStyle(
                fontSize = 17.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.085).sp,
            ),
        titleSmall =
            TextStyle(
                fontSize = 15.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        bodyLarge =
            TextStyle(
                fontSize = 16.sp,
                lineHeight = 23.sp,
            ),
        bodyMedium =
            TextStyle(
                fontSize = 15.sp,
                lineHeight = 21.sp,
            ),
        bodySmall =
            TextStyle(
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.12.sp,
            ),
        labelLarge =
            TextStyle(
                fontSize = 15.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
            ),
        labelMedium =
            TextStyle(
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        labelSmall =
            TextStyle(
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.12.sp,
            ),
    )

@Composable
fun FilcTheme(content: @Composable () -> Unit) {
    val darkTheme =
        when (AppSettings.themeMode.value) {
            AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            AppThemeMode.LIGHT -> false
            AppThemeMode.DARK -> true
        }

    val primary = Color(AppSettings.primaryColorArgb.value.toInt())
    val onPrimary = readableOnColor(primary)

    val colors =
        if (darkTheme) {
            darkColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = blend(primary, Color.Black, 0.16f),
                onPrimaryContainer = readableOnColor(blend(primary, Color.Black, 0.16f)),
                secondary = blend(primary, Color.White, 0.28f),
                onSecondary = readableOnColor(blend(primary, Color.White, 0.28f)),
                secondaryContainer = blend(primary, FilcDarkSurface, 0.62f),
                onSecondaryContainer = FilcDarkText,
                tertiary = Color(0xFF8FD4C1),
                onTertiary = FilcDarkBackground,
                tertiaryContainer = Color(0xFF123C38),
                onTertiaryContainer = Color(0xFFB8F4E5),
                background = FilcDarkBackground,
                onBackground = FilcDarkText,
                surface = FilcDarkSurface,
                onSurface = FilcDarkText,
                surfaceVariant = FilcDarkSurfaceAlt,
                onSurfaceVariant = FilcDarkMuted,
                outline = FilcDarkOutline,
                error = Color(0xFFF87171),
                onError = Color(0xFF450A0A),
            )
        } else {
            lightColorScheme(
                primary = primary,
                onPrimary = onPrimary,
                primaryContainer = blend(primary, Color.White, 0.78f),
                onPrimaryContainer = readableOnColor(blend(primary, Color.White, 0.78f)),
                secondary = blend(primary, Color.Black, 0.10f),
                onSecondary = readableOnColor(blend(primary, Color.Black, 0.10f)),
                secondaryContainer = blend(primary, Color.White, 0.86f),
                onSecondaryContainer = FilcLightText,
                tertiary = Color(0xFF218B72),
                onTertiary = Color.White,
                tertiaryContainer = Color(0xFFD7F5EC),
                onTertiaryContainer = Color(0xFF103B32),
                background = FilcLightBackground,
                onBackground = FilcLightText,
                surface = FilcLightSurface,
                onSurface = FilcLightText,
                surfaceVariant = FilcLightSurfaceAlt,
                onSurfaceVariant = FilcLightMuted,
                outline = FilcLightOutline,
                error = Color(0xFFB42318),
                onError = Color.White,
            )
        }

    ApplyPlatformSystemBars(darkTheme = darkTheme)

    MaterialTheme(
        colorScheme = colors,
        typography = FilcV3Typography,
        shapes = FilcV3Shapes,
        content = content,
    )
}

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

private fun readableOnColor(color: Color): Color {
    val luminance =
        color.red * 0.299f +
            color.green * 0.587f +
            color.blue * 0.114f

    return if (luminance > 0.62f) {
        Color(0xFF111827)
    } else {
        Color.White
    }
}
