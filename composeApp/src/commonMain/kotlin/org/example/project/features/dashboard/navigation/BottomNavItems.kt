package org.example.project.features.dashboard.navigation

import android.R.attr.label
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.vector.ImageVector
import org.example.project.navigation.Route
import java.lang.ProcessBuilder.Redirect.to
import kotlin.to

data class BottomNavItem(
    val label: String,
    val icon: ImageVector
)

val dashboardTopLevelDestinations = mapOf(
    Route.DashboardRoutes.Home to BottomNavItem(
        label = "Home",
        icon = Icons.Filled.Home
    ),
    Route.DashboardRoutes.SearchRoutes to BottomNavItem(
        label = "Serach",
        icon = Icons.Filled.Search
    ),
    Route.DashboardRoutes.Profile to BottomNavItem(
        label = "Profile",
        icon = Icons.Filled.AccountCircle
    )
)
