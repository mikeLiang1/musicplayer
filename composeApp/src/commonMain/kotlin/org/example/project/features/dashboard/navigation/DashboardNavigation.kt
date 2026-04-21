package org.example.project.features.dashboard.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.example.budget.navigation.rememberNavigationState
import com.example.budget.navigation.toEntries
import kotlinx.coroutines.flow.map
import org.example.project.features.home.ui.HomeScreen
import org.example.project.features.library.navigation.LibraryNavigation
import org.example.project.features.musicPlayer.ui.MusicPlayerBar
import org.example.project.features.musicPlayer.ui.MusicPlayerScreen
import org.example.project.features.musicPlayer.ui.MusicPlayerViewModel
import org.example.project.features.playlist.ui.PlaylistScreen
import org.example.project.features.playlist.ui.PlaylistViewModel
import org.example.project.features.search.navigtion.SearchNavigation
import org.example.project.navigation.Navigator
import org.example.project.navigation.Route
import org.example.project.navigation.dashboardAllRoutes
import org.example.project.ui.theme.appColors
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun DashboardNavigation() {
    val navigationState = rememberNavigationState(
        startRoute = Route.DashboardRoutes.Home,
        topLevelRoutes = dashboardAllRoutes
    )

    val navigator = remember { Navigator(navigationState) }

    val isBottomBarVisible = navigationState.topLevelRoute in dashboardTopLevelDestinations.keys

    val musicPlayerViewModel = koinViewModel<MusicPlayerViewModel>()
    val isFullScreenVisible by remember {
        musicPlayerViewModel.uiState.map { it.isFullScreenVisible }
    }.collectAsStateWithLifecycle(initialValue = false)

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = appColors.backgroundPrimary,
            bottomBar = {
                Column {
                    if (isBottomBarVisible) {
                        BottomNavigationBar(
                            navigationState = navigationState,
                            navigator = navigator
                        )
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {

                val entryProvider = entryProvider<NavKey> {
                    entry<Route.DashboardRoutes.Home> { HomeScreen() }
                    entry<Route.DashboardRoutes.SearchRoutes> { SearchNavigation() }
                    entry<Route.DashboardRoutes.LibraryRoutes> {
                        LibraryNavigation(
                            navigateToPlaylist = { navigator.navigate(Route.DashboardRoutes.Playlist(it)) }
                        )
                    }
                    entry<Route.DashboardRoutes.Playlist> { key ->
                        val playlistViewModel: PlaylistViewModel = koinViewModel(
                            parameters = { parametersOf(key.playlistId) }
                        )
                        val state by playlistViewModel.uiState.collectAsStateWithLifecycle()
                        PlaylistScreen(state, onBackPressed = { navigator.goBack() })
                    }
                }

                NavDisplay(
                    modifier = Modifier
                        .fillMaxSize(),
                    entries = navigationState.toEntries(entryProvider),
                    onBack = { navigator.goBack() }
                )
                MusicPlayerBar(
                    viewModel = musicPlayerViewModel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(12.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isFullScreenVisible,
            enter = slideInVertically(
                initialOffsetY = { it }, // Start from bottom
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            ),
            exit = slideOutVertically(
                targetOffsetY = { it }, // Slide back down
                animationSpec = tween(durationMillis = 400)
            )
        ) {
            Surface(
                // padding at top to immitate bottomsheet
                modifier = Modifier.fillMaxSize(),
                color = appColors.backgroundSecondary,
            ) {
                MusicPlayerScreen(
                    viewModel = musicPlayerViewModel,
                    navigateBack = { musicPlayerViewModel.setFullScreen(false) }
                )
            }
        }
    }

}


