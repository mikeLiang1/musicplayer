package org.example.project.features.dashboard.navigation

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.budget.navigation.NavigationState
import org.example.project.navigation.Navigator
import org.example.project.ui.theme.appColors

@Composable
fun BottomNavigationBar(
    navigationState: NavigationState,
    navigator: Navigator
) {
    HorizontalDivider(color = appColors.divider)
    NavigationBar(
        containerColor = appColors.backgroundElevated
    ) {
        dashboardTopLevelDestinations.forEach { (route, bottomNavItem) ->

            NavigationBarItem(
                icon = { Icon(bottomNavItem.icon, contentDescription = bottomNavItem.label) },
                label = { Text(bottomNavItem.label) },
                selected = navigationState.topLevelRoute == route,
                onClick = {
                    navigator.navigateToTopLevelRoute(route)
                },
                colors = NavigationBarItemColors(
                    selectedIconColor = appColors.accentPrimary,
                    selectedTextColor = appColors.accentPrimary,
                    selectedIndicatorColor = Color.Transparent,
                    unselectedIconColor = appColors.iconSecondary,
                    unselectedTextColor = appColors.textSecondary,
                    disabledIconColor = appColors.iconMuted,
                    disabledTextColor = appColors.textMuted,
                )
            )
        }
    }
}
