package org.example.project.features.library.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import org.example.project.ui.theme.AppPreview
import org.example.project.ui.theme.DevicePreviews
import org.example.project.ui.theme.Dimens
import org.example.project.ui.theme.appColors

/**
 * Sheet behind the Library "+" button. Opens with a suggested name already selected, so a single
 * confirm keeps the old one-tap speed while typing immediately replaces the suggestion.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePlaylistBottomSheet(
    isVisible: Boolean,
    name: String,
    isSaving: Boolean,
    onNameChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(isVisible) {
        if (isVisible) sheetState.show() else sheetState.hide()
    }

    if (isVisible) {
        val canCreate = name.isNotBlank() && !isSaving

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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = Dimens.spaceL)
                    .padding(bottom = Dimens.spaceL)
            ) {
                Text(
                    text = "New playlist",
                    style = MaterialTheme.typography.titleMedium,
                    color = appColors.textPrimary,
                    modifier = Modifier.padding(vertical = Dimens.spaceM)
                )

                PlaylistNameField(
                    name = name,
                    onNameChange = onNameChange,
                    onDone = { if (canCreate) onConfirm() }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Dimens.spaceL),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SheetTextButton(
                        text = "Cancel",
                        color = appColors.textSecondary,
                        onClick = onDismissRequest
                    )
                    SheetTextButton(
                        text = "Create",
                        color = if (canCreate) appColors.accentPrimary else appColors.textDim,
                        onClick = onConfirm,
                        enabled = canCreate
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetTextButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(Dimens.radiusM))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = Dimens.spaceL, vertical = Dimens.spaceM)
    )
}

@Composable
private fun PlaylistNameField(
    name: String,
    onNameChange: (String) -> Unit,
    onDone: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var isFocused by remember { mutableStateOf(false) }

    // Seeded with the suggestion fully selected so the first keystroke replaces it. Later external
    // changes (the clear button) push the cursor to the end instead.
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = name, selection = TextRange(0, name.length)))
    }
    LaunchedEffect(name) {
        if (textFieldValue.text != name) {
            textFieldValue = TextFieldValue(text = name, selection = TextRange(name.length))
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    BasicTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            textFieldValue = newValue
            if (newValue.text != name) {
                onNameChange(newValue.text)
            }
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = appColors.textPrimary),
        cursorBrush = SolidColor(appColors.accentPrimary),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            autoCorrectEnabled = false,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                keyboardController?.hide()
                onDone()
            }
        ),
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(Dimens.radiusL))
                    .background(appColors.backgroundSurface)
                    .border(
                        width = Dimens.borderHairline,
                        color = animateColorAsState(
                            targetValue = if (isFocused) appColors.accentPrimary else appColors.divider,
                            label = "border"
                        ).value,
                        shape = RoundedCornerShape(Dimens.radiusL)
                    )
                    .padding(horizontal = Dimens.spaceM, vertical = Dimens.spaceM),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS)
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (name.isEmpty()) {
                        Text(
                            text = "Playlist name",
                            style = MaterialTheme.typography.bodyMedium,
                            color = appColors.textDim
                        )
                    }
                    innerTextField()
                }

                if (name.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Clear name",
                        tint = appColors.iconMuted,
                        modifier = Modifier
                            .size(Dimens.iconXs)
                            .clickable { onNameChange("") }
                    )
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { isFocused = it.isFocused }
    )
}

@DevicePreviews
@Composable
private fun CreatePlaylistBottomSheetPreview() {
    AppPreview {
        CreatePlaylistBottomSheet(
            isVisible = true,
            name = "My playlist",
            isSaving = false,
            onNameChange = {},
            onConfirm = {},
            onDismissRequest = {}
        )
    }
}
