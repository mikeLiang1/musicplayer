package org.example.project.features.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import org.example.project.core.model.RecentlyPlayedItem
import org.example.project.ui.component.CoverImage
import org.example.project.ui.component.marqueeOnHover
import org.example.project.ui.component.pressableCard
import org.example.project.ui.theme.AppPreview
import org.example.project.ui.theme.DevicePreviews
import org.example.project.ui.theme.Dimens
import org.example.project.ui.theme.appColors

@Composable
fun HomeScreen(state: HomeUiState, onAction: (HomeAction) -> Unit) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Column {
                HeaderSection(onAction = onAction, state = state)
            }
        }
    ) { padding ->
        HomeContent(modifier = Modifier.padding(padding), state = state, onAction = onAction)
    }
}

@Composable
private fun HeaderSection(onAction: (HomeAction) -> Unit, state: HomeUiState) {
    Row(
        modifier = Modifier
            .padding(horizontal = Dimens.spaceL)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Greetings", color = appColors.textPrimary, style = MaterialTheme.typography.headlineLarge)
        IconButton(onClick = {}) {
            Icon(Icons.Default.Person, "Profile")
        }
    }
}

@Composable
private fun HomeContent(
    modifier: Modifier = Modifier,
    state: HomeUiState,
    onAction: (HomeAction) -> Unit
) {
    LazyColumn(modifier = modifier, contentPadding = PaddingValues(vertical = Dimens.spaceM)) {
        item {
            RecentlyPlayedSection(state, onAction)
        }
    }
}

@Composable
private fun RecentlyPlayedSection(
    state: HomeUiState,
    onAction: (HomeAction) -> Unit
) {
    Text("Recently Played", modifier = Modifier.padding(Dimens.spaceM), style = MaterialTheme.typography.bodyLarge)
    LazyRow(
        contentPadding = PaddingValues(horizontal = Dimens.spaceM, vertical = Dimens.spaceS),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceL)
    ) {
        items(state.recentlyPlayed) {
            RecentlyPlayedItem(
                recentlyPlayedItem = it,
                onClick = { onAction(HomeAction.OnRecentPlayedClicked(it)) }
            )
        }

    }
}

@Composable
private fun RecentlyPlayedItem(recentlyPlayedItem: RecentlyPlayedItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(Dimens.Size.coverCardWidth)
            .clip(RoundedCornerShape(Dimens.radiusM))
            .pressableCard(
                onClick = onClick
            )
    ) {
        CoverImage(
            data = recentlyPlayedItem.thumbnailUrl,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = Dimens.spaceS),
            shape = RoundedCornerShape(Dimens.radiusM)
        )
        Text(
            text = recentlyPlayedItem.title,
            maxLines = 1,
            style = MaterialTheme.typography.bodyMedium,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = recentlyPlayedItem.subTitle,
            style = MaterialTheme.typography.bodySmall,
            color = appColors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

    }
}

@DevicePreviews
@Composable
private fun HomeScreenPreview() {
    AppPreview {
        HomeScreen(
            state = HomeUiState(
            ),
            onAction = {}
        )
    }
}
