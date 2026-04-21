package org.example.project.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf


private val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

val appColors: AppColors
    @Composable
    get() = LocalAppColors.current

@Composable
fun BudgetTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val customColors = if (darkTheme) DarkAppColors else LightAppColors

    // 1. Map your custom colors to the Material 3 ColorScheme
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = customColors.accentPrimary,
            onPrimary = customColors.onAccent,
            primaryContainer = customColors.accentContainer,
            onPrimaryContainer = customColors.onAccentContainer,

            secondary = customColors.accentDark,

            tertiary = customColors.rose,
            onTertiary = customColors.onRose,
            tertiaryContainer = customColors.roseContainer,

            background = customColors.backgroundPrimary,
            onBackground = customColors.textPrimary,

            surface = customColors.backgroundSurface,
            onSurface = customColors.textPrimary,
            surfaceVariant = customColors.backgroundSecondary,
            onSurfaceVariant = customColors.textSecondary,

            outline = customColors.divider,
            error = customColors.error,
            onError = customColors.onError
        )
    } else {
        lightColorScheme(
            primary = customColors.accentPrimary,
            onPrimary = customColors.onAccent,
            primaryContainer = customColors.accentContainer,
            onPrimaryContainer = customColors.onAccentContainer,

            secondary = customColors.accentDark,

            tertiary = customColors.rose,
            onTertiary = customColors.onRose,
            tertiaryContainer = customColors.roseContainer,

            background = customColors.backgroundPrimary,
            onBackground = customColors.textPrimary,

            surface = customColors.backgroundSurface,
            onSurface = customColors.textPrimary,
            surfaceVariant = customColors.backgroundSecondary,
            onSurfaceVariant = customColors.textSecondary,

            outline = customColors.divider,
            error = customColors.error,
            onError = customColors.onError
        )
    }

    // 2. Provide both your custom locals AND the MaterialTheme
    CompositionLocalProvider(
        LocalAppColors provides customColors
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            // You can also add your Typography here if you have one
            content = content
        )
    }
}
