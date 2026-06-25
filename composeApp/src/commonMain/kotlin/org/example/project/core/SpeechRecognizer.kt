package org.example.project.core

import kotlinx.coroutines.flow.Flow

/**
 * Platform-agnostic interface for voice/speech recognition.
 * On Android this wraps [android.speech.SpeechRecognizer].
 */
interface SpeechRecognizer {
    /** Emits partial transcriptions, then the final result, then stops. */
    fun startListening(): Flow<String>

    /** Cancels the current recognition session. */
    fun cancel()
}
