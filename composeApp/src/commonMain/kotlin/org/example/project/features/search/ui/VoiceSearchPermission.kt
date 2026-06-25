package org.example.project.features.search.ui

import androidx.compose.runtime.Composable

/**
 * Requests the microphone/voice permission needed for speech recognition, then invokes
 * [onGranted] once it is available.
 *
 * On Android this requests RECORD_AUDIO. Other platforms should provide an `actual` that
 * invokes [onGranted] directly (or performs their own permission flow).
 */
@Composable
expect fun RequestVoicePermissionEffect(onGranted: () -> Unit)
