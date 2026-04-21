package org.example.project.features.library.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.budget.navigation.rememberNavigationState
import com.example.budget.navigation.toEntries
import org.example.project.features.library.ui.LibraryEffect
import org.example.project.features.library.ui.LibraryScreen
import org.example.project.features.library.ui.LibraryViewModel
import org.example.project.navigation.Navigator
import org.example.project.navigation.Route
import org.example.project.navigation.libraryAllRoutes
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LibraryNavigation(navigateToPlaylist: (String) -> Unit) {
    val navigationState = rememberNavigationState(
        startRoute = Route.DashboardRoutes.LibraryRoutes.Library,
        topLevelRoutes = libraryAllRoutes
    )

    val navigator = remember { Navigator(navigationState) }

    val entryProvider = entryProvider<NavKey> {
        entry<Route.DashboardRoutes.LibraryRoutes.Library> {
            val libraryViewModel = koinViewModel<LibraryViewModel>()
            val state by libraryViewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(Unit) {
                libraryViewModel.effect.collect { effect ->
                    when (effect) {
                        is LibraryEffect.NavigateToPlaylist -> {
                            navigateToPlaylist(effect.playlistId)
                        }
                    }
                }
            }
            LibraryScreen(state = state, onAction = libraryViewModel::handleAction)
        }

    }

    NavDisplay(
        modifier = Modifier.fillMaxSize(),
        entries = navigationState.toEntries(entryProvider),
        onBack = { navigator.goBack() }
    )

}
