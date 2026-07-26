package org.example.project.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Queue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.launch
import org.example.project.ui.theme.AppPreview
import org.example.project.ui.theme.DevicePreviews
import org.example.project.ui.theme.Dimens
import org.example.project.ui.theme.appColors

/** Colour role for a menu row — see [MenuBottomSheetItem] for how each maps to icon/chip/label. */
enum class MenuAccent { Neutral, Like, Destructive }

/**
 * One row in a [MenuBottomSheet]. Implemented by `SongMenuAction` (per-song menu) and
 * `CollectionMenuAction` (playlist / liked-songs menu) so both share the sheet and row styling.
 */
interface MenuAction {
    val label: String
    val icon: ImageVector
    val accent: MenuAccent get() = MenuAccent.Neutral
}

/**
 * Generic bottom-sheet menu: a column of [MenuAction] rows.
 *
 * Selecting a row hides the sheet first, then reports [onDismissRequest] followed by
 * [onActionSelected] — so an action that opens *another* sheet (add-to-playlist, rename)
 * doesn't fight this one's exit animation.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T : MenuAction> MenuBottomSheet(
    isVisible: Boolean,
    actions: List<T>,
    onActionSelected: (T) -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(isVisible) {
        if (isVisible) sheetState.show() else sheetState.hide()
    }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
            containerColor = appColors.backgroundElevated,
            dragHandle = {
                Surface(
                    modifier = Modifier.padding(vertical = Dimens.spaceM),
                    color = appColors.divider,
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Box(Modifier.size(width = Dimens.Size.dragHandleWidth, height = Dimens.spaceXs))
                }
            }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                actions.forEach { action ->
                    MenuBottomSheetItem(
                        action = action,
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    onDismissRequest() // This sets the boolean to false
                                }
                            }
                            onActionSelected(action)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MenuBottomSheetItem(
    modifier: Modifier = Modifier,
    action: MenuAction,
    onClick: () -> Unit
) {
    val contentColor = when (action.accent) {
        MenuAccent.Neutral -> appColors.iconSecondary
        MenuAccent.Like -> appColors.rose
        MenuAccent.Destructive -> appColors.error
    }
    val chipColor = when (action.accent) {
        MenuAccent.Neutral -> appColors.accentPrimary.copy(alpha = 0.15f)
        MenuAccent.Like -> appColors.rose.copy(alpha = 0.15f)
        MenuAccent.Destructive -> appColors.error.copy(alpha = 0.15f)
    }
    Row(
        modifier = modifier
            .background(appColors.backgroundElevated)
            .clickable(onClick = { onClick() })
            .padding(horizontal = Dimens.spaceL, vertical = Dimens.spaceM)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM)
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(Dimens.radiusM))
                .background(chipColor)
                .padding(Dimens.spaceS)
        ) {
            Icon(
                action.icon,
                contentDescription = action.label,
                tint = contentColor
            )
        }
        Text(
            text = action.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (action.accent == MenuAccent.Neutral) {
                appColors.textPrimary
            } else {
                contentColor
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class PreviewMenuAction(
    override val label: String,
    override val icon: ImageVector,
    override val accent: MenuAccent = MenuAccent.Neutral
) : MenuAction

@DevicePreviews
@Composable
private fun MenuBottomSheetPreview() {
    AppPreview {
        MenuBottomSheet(
            isVisible = true,
            actions = listOf(
                PreviewMenuAction("Add to queue", Icons.Rounded.Queue),
                PreviewMenuAction("Delete", Icons.Rounded.DeleteOutline, MenuAccent.Destructive)
            ),
            onActionSelected = {},
            onDismissRequest = {}
        )
    }
}

@DevicePreviews
@Composable
private fun MenuBottomSheetItemPreview() {
    AppPreview {
        Column {
            MenuBottomSheetItem(
                action = PreviewMenuAction("Neutral row", Icons.Rounded.Queue),
                onClick = {}
            )
            MenuBottomSheetItem(
                action = PreviewMenuAction(
                    "Destructive row",
                    Icons.Rounded.DeleteOutline,
                    MenuAccent.Destructive
                ),
                onClick = {}
            )
        }
    }
}
