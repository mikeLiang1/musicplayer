package com.example.budget.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext

val DarkColorScheme = darkColorScheme(
    primary = Violet80,           // #BDB2FF — main accent on dark
    onPrimary = DarkInk,            // text/icons on primary
    primaryContainer = Violet20,           // #3D2EAD — chip/button backgrounds
    onPrimaryContainer = Violet80,

    secondary = Lavender80,
    onSecondary = Color(0xFF1A1450),
    secondaryContainer = Lavender20,
    onSecondaryContainer = Lavender80,

    tertiary = Rose80,
    onTertiary = Color(0xFF3A0A1E),
    tertiaryContainer = Rose20,
    onTertiaryContainer = Rose80,

    background = Grey08,             // #0A0A0F
    onBackground = Grey95,

    surface = Grey10,             // #13131A
    onSurface = Grey95,
    surfaceVariant = Grey20,             // #2A2A38
    onSurfaceVariant = Color(0xFFCAC4D8),

    surfaceTint = Violet50,

    inverseSurface = Grey95,
    inverseOnSurface = Grey10,
    inversePrimary = Violet40,

    outline = Color(0xFF948FA8),
    outlineVariant = Color(0xFF2A2A38),

    scrim = Black,

    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
)

// ── Light Scheme ───────────────────────────────────────────────────────────

val LightColorScheme = lightColorScheme(
    primary = Violet40,           // #6B5AE0
    onPrimary = White,
    primaryContainer = Color(0xFFE8E0FF),
    onPrimaryContainer = Color(0xFF1A0080),

    secondary = Lavender40,
    onSecondary = White,
    secondaryContainer = Color(0xFFE8E0FF),
    onSecondaryContainer = Color(0xFF1A1254),

    tertiary = Rose40,
    onTertiary = White,
    tertiaryContainer = Color(0xFFFFD9E4),
    onTertiaryContainer = Color(0xFF3A0020),

    background = Grey98,
    onBackground = Color(0xFF1A1A2A),

    surface = White,
    onSurface = Color(0xFF1A1A2A),
    surfaceVariant = Color(0xFFE8E0F0),
    onSurfaceVariant = Color(0xFF4A4560),

    surfaceTint = Violet40,

    inverseSurface = Color(0xFF2F2D3C),
    inverseOnSurface = Color(0xFFF3EFF8),
    inversePrimary = Violet80,

    outline = Color(0xFF7A7490),
    outlineVariant = Color(0xFFCAC4D8),

    scrim = Black,

    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
)

@Composable
fun BudgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
