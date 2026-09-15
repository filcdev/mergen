package hu.petrik.filcapp.theme

import androidx.compose.foundation.isSystemInDarkTheme
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

val FilcAccent = Color(0xFF20AC9B)
val FilcLightBackground = Color(0xFFF4F9FF)
val FilcLightSurface = Color(0xFFFFFFFF)
val FilcDarkBackground = Color(0xFF000000)
val FilcDarkSurface = Color(0xFF141516)

private val FilcLightColors =
    lightColorScheme(
        primary = FilcAccent,
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDDF5F1),
        onPrimaryContainer = Color(0xFF123D37),
        secondary = FilcAccent,
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFDDF5F1),
        onSecondaryContainer = Color(0xFF123D37),
        tertiary = Color(0xFF247665),
        onTertiary = Color.White,
        tertiaryContainer = Color(0xFFE6F3F0),
        onTertiaryContainer = Color(0xFF173C35),
        background = FilcLightBackground,
        onBackground = Color(0xFF111315),
        surface = FilcLightSurface,
        onSurface = Color(0xFF111315),
        surfaceVariant = Color(0xFFEDF3F7),
        onSurfaceVariant = Color(0xFF657078),
        outline = Color(0xFFD4DEE5),
        error = Color(0xFFFF3B30),
        onError = Color.White,
    )

private val FilcDarkColors =
    darkColorScheme(
        primary = FilcAccent,
        onPrimary = Color(0xFF00201A),
        primaryContainer = Color(0xFF174D43),
        onPrimaryContainer = Color(0xFFD8FFF6),
        secondary = FilcAccent,
        onSecondary = Color(0xFF00201A),
        secondaryContainer = Color(0xFF174D43),
        onSecondaryContainer = Color(0xFFD8FFF6),
        tertiary = Color(0xFF64D2C3),
        onTertiary = Color(0xFF00201A),
        tertiaryContainer = Color(0xFF173D37),
        onTertiaryContainer = Color(0xFFD8FFF6),
        background = FilcDarkBackground,
        onBackground = Color(0xFFF2F4F5),
        surface = FilcDarkSurface,
        onSurface = Color(0xFFF2F4F5),
        surfaceVariant = Color(0xFF202326),
        onSurfaceVariant = Color(0xFFB6BEC3),
        outline = Color(0xFF3A4145),
        error = Color(0xFFFF453A),
        onError = Color(0xFF2B0000),
    )

private val FilcShapes =
    Shapes(
        extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
        small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        large = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    )

private val FilcTypography =
    Typography(
        headlineSmall =
            TextStyle(
                fontSize = 32.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Bold,
            ),
        titleLarge =
            TextStyle(
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
            ),
        titleMedium =
            TextStyle(
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        titleSmall =
            TextStyle(
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        bodyLarge =
            TextStyle(
                fontSize = 16.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.Normal,
            ),
        bodyMedium =
            TextStyle(
                fontSize = 14.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Normal,
            ),
        bodySmall =
            TextStyle(
                fontSize = 12.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Normal,
            ),
        labelLarge =
            TextStyle(
                fontSize = 14.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.SemiBold,
            ),
        labelMedium =
            TextStyle(
                fontSize = 12.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.Medium,
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

    ApplyPlatformSystemBars(darkTheme)

    MaterialTheme(
        colorScheme = if (darkTheme) FilcDarkColors else FilcLightColors,
        typography = FilcTypography,
        shapes = FilcShapes,
        content = content,
    )
}
