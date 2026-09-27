package hu.petrik.filcapp.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val FilcAccent = Color(0xFF818CF8)
val FilcAccentStrong = Color(0xFF4F46E5)
val FilcAccentSoft = Color(0xFFA5B4FC)
val FilcSuccess = Color(0xFF8FD4C1)

val FilcDarkBackground = Color(0xFF0B1120)
val FilcDarkSurface = Color(0xFF1E293B)
val FilcDarkSurfaceAlt = Color(0xFF18213A)
val FilcDarkText = Color(0xFFF1F5F9)
val FilcDarkMuted = Color(0xFF94A3B8)
val FilcDarkOutline = Color(0xFF334155)

private val FilcV3Colors =
    darkColorScheme(
        primary = FilcAccent,
        onPrimary = Color.White,
        primaryContainer = FilcAccentStrong,
        onPrimaryContainer = Color.White,
        secondary = FilcAccentSoft,
        onSecondary = FilcDarkBackground,
        secondaryContainer = Color(0xFF28345A),
        onSecondaryContainer = FilcDarkText,
        tertiary = FilcSuccess,
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
    // The Figma V3 design is intentionally dark-only.
    ApplyPlatformSystemBars(darkTheme = true)

    MaterialTheme(
        colorScheme = FilcV3Colors,
        typography = FilcV3Typography,
        shapes = FilcV3Shapes,
        content = content,
    )
}
