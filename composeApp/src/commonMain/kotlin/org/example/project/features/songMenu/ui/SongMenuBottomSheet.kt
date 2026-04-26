package org.example.project.features.songMenu.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.example.project.ui.theme.AppPreview
import org.example.project.ui.theme.DevicePreviews
import org.example.project.ui.theme.appColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongMenuBottomSheet(
    isMenuBottomSheetVisible: Boolean,
    onCloseBottomSheet: () -> Unit,
    handleBottomSheetAction: (SongMenuAction) -> Unit,
    songMenuActions: List<SongMenuAction>
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    LaunchedEffect(isMenuBottomSheetVisible) {
        if (isMenuBottomSheetVisible) {
            sheetState.show()
        } else {
            sheetState.hide()
        }
    }
    if (isMenuBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = onCloseBottomSheet,
            sheetState = sheetState,
            containerColor = appColors.backgroundElevated,
            dragHandle = {
                Surface(
                    modifier =
                        Modifier.padding(vertical = 12.dp),
                    color = appColors.divider,
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Box(Modifier.size(width = 32.dp, height = 4.dp))
                }
            }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {

                songMenuActions.forEach { action ->
                    BottomSheetItem(
                        songMenuAction = action,
                        onClick = {
                            scope.launch { sheetState.hide() }.invokeOnCompletion {
                                if (!sheetState.isVisible) {
                                    onCloseBottomSheet() // This sets the boolean to false
                                }
                            }
                            handleBottomSheetAction(action)
                        }
                    )
                }
            }
        }
    }
}

@DevicePreviews
@Composable
private fun SongMenuBottomSheetPreview() {
    AppPreview {
        SongMenuBottomSheet(
            isMenuBottomSheetVisible = true,
            onCloseBottomSheet = {},
            handleBottomSheetAction = {},
            songMenuActions = SongMenuAction.all
        )
    }
}
