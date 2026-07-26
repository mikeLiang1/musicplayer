package org.example.project.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import org.example.project.ui.theme.AppPreview
import org.example.project.ui.theme.DevicePreviews
import org.example.project.ui.theme.Dimens
import org.example.project.ui.theme.appColors

/**
 * Yes/no confirmation for an irreversible action (today: deleting a playlist).
 *
 * A dialog rather than another bottom sheet on purpose — it is a stop sign, not a menu, and
 * it can appear over a sheet. Set [isDestructive] to tint the confirm label with
 * `appColors.error`; text buttons reuse [SheetTextButton] so they match the sheets.
 */
@Composable
fun ConfirmDialog(
    isVisible: Boolean,
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismissRequest: () -> Unit,
    isDestructive: Boolean = false,
    dismissLabel: String = "Cancel"
) {
    if (!isVisible) return

    AlertDialog(
        onDismissRequest = onDismissRequest,
        containerColor = appColors.backgroundElevated,
        titleContentColor = appColors.textPrimary,
        textContentColor = appColors.textSecondary,
        shape = MaterialTheme.shapes.large,
        tonalElevation = Dimens.elevationLow,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Text(text = message, style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            SheetTextButton(
                text = confirmLabel,
                color = if (isDestructive) appColors.error else appColors.accentPrimary,
                onClick = onConfirm
            )
        },
        dismissButton = {
            SheetTextButton(
                text = dismissLabel,
                color = appColors.textSecondary,
                onClick = onDismissRequest
            )
        }
    )
}

@DevicePreviews
@Composable
private fun ConfirmDialogPreview() {
    AppPreview {
        ConfirmDialog(
            isVisible = true,
            title = "Delete playlist?",
            message = "\"Late night drives\" and its 24 songs will be removed. This can't be undone.",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {},
            onDismissRequest = {}
        )
    }
}
