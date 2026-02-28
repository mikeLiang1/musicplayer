package org.example.project

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import org.example.project.ui.theme.BudgetTheme
import org.example.project.navigation.AppNavigation

@Composable
fun App() {
    BudgetTheme {
        Surface {
            AppNavigation()
        }
    }
}
