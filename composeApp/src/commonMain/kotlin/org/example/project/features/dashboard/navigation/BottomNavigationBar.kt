package org.example.project.features.dashboard.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.budget.navigation.NavigationState
import org.example.project.navigation.Route
import org.example.project.ui.theme.appColors

@Composable
fun BottomNavigationBar(
    navigationState: NavigationState,
    navigate: (Route) -> Unit,
) {
    val bottomPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .height(65.dp)
            // Lets just hardcode for we can dynamic change it later
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 32.dp)
        ) {
            dashboardTopLevelDestinations.forEach { (route, bottomNavItem) ->
                CustomBottomNavItem(
                    icon = bottomNavItem.icon,
                    label = bottomNavItem.label,
                    isSelected = navigationState.topLevelRoute == route,
                    onClick = {
                        navigate(route)
                    }
                )
            }
        }
    }
}

@Composable
fun CustomBottomNavItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, // Removes the ripple for a cleaner look, or keep it if you prefer
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) appColors.accentPrimary else appColors.iconSecondary,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) appColors.accentPrimary else appColors.textSecondary
        )
    }
}
