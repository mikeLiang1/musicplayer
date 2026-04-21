package org.example.project.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {

    @Serializable
    data object DashboardRoutes : Route {

        @Serializable
        data object Home : Route

        @Serializable
        data object SearchRoutes : Route {
            @Serializable
            data object Suggestions : Route
        }

        @Serializable
        data object LibraryRoutes : Route {
            @Serializable
            data object Library : Route
        }

        // Shared route — reachable from both Search and Library
        @Serializable
        data class Playlist(val playlistId: String) : Route
    }
}

val appTopLevelRoutes = setOf(Route.DashboardRoutes)

val dashboardAllRoutes = setOf(
    Route.DashboardRoutes.Home,
    Route.DashboardRoutes.SearchRoutes,
    Route.DashboardRoutes.LibraryRoutes
)

val searchAllRoutes = setOf(
    Route.DashboardRoutes.SearchRoutes.Suggestions
)

val libraryAllRoutes = setOf(
    Route.DashboardRoutes.LibraryRoutes.Library,
)

