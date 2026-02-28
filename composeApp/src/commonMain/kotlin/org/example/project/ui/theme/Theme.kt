package org.example.project.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
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
    CompositionLocalProvider(
        LocalAppColors provides if (darkTheme) DarkAppColors else LightAppColors
    ) {
        content()
    }

}
