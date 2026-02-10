package org.example.project.features.dashboard.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.budget.navigation.Navigator
import com.example.budget.navigation.rememberNavigationState
import com.example.budget.navigation.toEntries
import org.example.project.features.home.ui.HomeScreen
import org.example.project.features.musicPlayer.ui.MusicPlayerBar
import org.example.project.features.musicPlayer.ui.MusicPlayerScreen
import org.example.project.features.musicPlayer.ui.MusicPlayerViewModel
import org.example.project.features.search.navigtion.SearchNavigation
import org.example.project.navigation.Route
import org.example.project.navigation.dashboardAllRoutes
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DashboardNavigation() {
    val navigationState = rememberNavigationState(
        startRoute = Route.DashboardRoutes.Home,
        topLevelRoutes = dashboardAllRoutes
    )

    val navigator = remember { Navigator(navigationState) }

    val isBottomBarVisible = navigationState.topLevelRoute in dashboardTopLevelDestinations.keys

    val musicPlayerViewModel = koinViewModel<MusicPlayerViewModel>()


    Scaffold(
        bottomBar = {
            Column {
                // TODO: music player bar probably has different logic
                if (isBottomBarVisible) {
                    MusicPlayerBar(
                        viewModel = musicPlayerViewModel,
                        onBarClicked = { navigator.navigate(Route.MusicPlayerRoute) })
                    BottomNavigationBar(
                        navigationState = navigationState,
                        navigator = navigator
                    )
                }
            }
        },
    ) { innerPadding ->
        val entryProvider = entryProvider<NavKey> {
            entry<Route.DashboardRoutes.Home> { HomeScreen() }
            entry<Route.DashboardRoutes.Profile> { Text("Profile") }
            entry<Route.DashboardRoutes.SearchRoutes> { SearchNavigation() }
            entry<Route.MusicPlayerRoute> {
                MusicPlayerScreen(
                    viewModel = musicPlayerViewModel,
                    navigateBack = { navigator.goBack() })
            }
        }

        NavDisplay(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding),
            entries = navigationState.toEntries(entryProvider),
            onBack = { navigator.goBack() }
        )
    }
}


