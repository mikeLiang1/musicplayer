package org.example.project

import androidx.compose.runtime.Composable
import org.example.project.navigation.AppNavigation
import org.example.project.ui.theme.BudgetTheme

@Composable
fun App() {
    BudgetTheme {
        AppNavigation()
    }
}
