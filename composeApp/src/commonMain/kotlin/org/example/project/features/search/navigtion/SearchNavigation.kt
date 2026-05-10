package org.example.project.features.search.navigtion

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.budget.navigation.rememberNavigationState
import com.example.budget.navigation.toEntries
import org.example.project.features.search.ui.SearchScreen
import org.example.project.features.search.ui.SearchViewModel
import org.example.project.navigation.Navigator
import org.example.project.navigation.Route
import org.example.project.navigation.searchAllRoutes
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SearchNavigation() {
    val navigationState = rememberNavigationState(
        startRoute = Route.DashboardRoutes.SearchRoutes.Suggestions,
        topLevelRoutes = searchAllRoutes
    )

    val navigator = remember { Navigator(navigationState) }


    val entryProvider = entryProvider<NavKey> {
        entry<Route.DashboardRoutes.SearchRoutes.Suggestions> {
            val searchViewModel = koinViewModel<SearchViewModel>()
            val uiState by searchViewModel.uiState.collectAsStateWithLifecycle()
            SearchScreen(
                state = uiState, onAction = searchViewModel::handleAction
            )
        }
    }

    NavDisplay(
        modifier = Modifier.fillMaxSize(),
        entries = navigationState.toEntries(entryProvider),
        onBack = { navigator.goBack() }
    )

}


