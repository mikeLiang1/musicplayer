package org.example.project.ui.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import org.example.project.ui.theme.AppPreview
import org.example.project.ui.theme.DevicePreviews
import org.example.project.ui.theme.appColors

private enum class TrailingIconState { Stop, Clear, Mic, None }

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    query: String,
    onSuggestionPressed: (String) -> Unit,
    onQueryChange: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    isListening: Boolean = false,
    onVoiceSearchCancel: () -> Unit = {},
    openKeyboardOnLaunch: Boolean = false,
    onTextCleared: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var isActive by remember { mutableStateOf(false) }

    // Tracks cursor/selection locally. When query changes externally (voice transcript,
    // cleared, suggestion picked) rather than from the user typing, push the cursor to the
    // end instead of leaving it wherever it last was.
    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = query, selection = TextRange(query.length)))
    }
    LaunchedEffect(query) {
        if (textFieldValue.text != query) {
            textFieldValue = TextFieldValue(text = query, selection = TextRange(query.length))
        }
    }

    LaunchedEffect(Unit) {
        if (openKeyboardOnLaunch) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                if (newValue.text != query) {
                    onQueryChange(newValue.text)
                }
            },
            readOnly = isListening,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                color = appColors.textPrimary
            ),
            cursorBrush = SolidColor(appColors.accentPrimary),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (query.isNotEmpty()) {
                        onSuggestionPressed(query)
                    keyboardController?.hide()
                    }
                }
            ),
            decorationBox = { innerTextField ->
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(appColors.backgroundElevated)
                        .border(
                            width = 0.5.dp,
                            color = animateColorAsState(
                                targetValue = if (isActive) appColors.accentPrimary else appColors.divider,
                                label = "border"
                            ).value,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Leading search icon
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = animateColorAsState(
                            targetValue = if (isActive) appColors.accentPrimary else appColors.iconMuted,
                            label = "search_icon"
                        ).value,
                        modifier = Modifier.size(16.dp)
                    )

                    // Input + placeholder
                    Box(modifier = Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text(
                                text = if (isListening) "Listening..." else "Artists, songs, podcasts",
                                style = MaterialTheme.typography.bodyMedium,
                                color = appColors.textDim
                            )
                        }
                        innerTextField()
                    }

                    // Trailing icon: stop when listening, clear when typing, mic when active + empty
                    val trailingIconState = when {
                        isListening -> TrailingIconState.Stop
                        query.isNotEmpty() -> TrailingIconState.Clear
                        isActive -> TrailingIconState.Mic
                        else -> TrailingIconState.None
                    }
                    AnimatedContent(
                        targetState = trailingIconState,
                        label = "trailing_icon"
                    ) { state ->
                        when (state) {
                            TrailingIconState.Stop -> Icon(
                                imageVector = Icons.Rounded.Stop,
                                contentDescription = "Stop listening",
                                tint = appColors.accentPrimary,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onVoiceSearchCancel() }
                            )
                            TrailingIconState.Clear -> Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Clear search",
                                tint = appColors.iconMuted,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable { onTextCleared() }
                            )
                            TrailingIconState.Mic -> Icon(
                                imageVector = Icons.Rounded.Mic,
                                contentDescription = "Voice search",
                                tint = appColors.iconMuted,
                                modifier = Modifier
                                    .size(16.dp)
                                    .clickable {
                                        keyboardController?.hide()
                                        onVoiceSearch()
                                    }
                            )
                            TrailingIconState.None -> {}
                        }
                    }
                }
            },
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { isActive = it.isFocused }
        )

        // Cancel button slides in when active
        AnimatedVisibility(
            visible = isActive,
            enter = fadeIn() + slideInHorizontally { it },
            exit = fadeOut() + slideOutHorizontally { it }
        ) {
            Text(
                text = "Cancel",
                style = MaterialTheme.typography.bodyMedium,
                color = appColors.accentPrimary,
                modifier = Modifier
                    .clickable {
                        if (isListening) onVoiceSearchCancel()
                        onTextCleared()
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                    .padding(start = 4.dp)
            )
        }
    }
}

@DevicePreviews
@Composable
private fun SearchBarPreview() {
    AppPreview {
        Column {
            SearchBar(query = "", onQueryChange = {}, onVoiceSearch = {}, onSuggestionPressed = {},onTextCleared = {})
            SearchBar(query = "", onQueryChange = {}, onVoiceSearch = {}, onSuggestionPressed = {},onTextCleared = {})
            SearchBar(query = "asd", onQueryChange = {}, onVoiceSearch = {}, onSuggestionPressed = {} ,onTextCleared = {})
        }
    }
}
