package org.example.project.core

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer as AndroidSpeechRecognizer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class SpeechRecognizerImpl(
    private val context: Context
) : SpeechRecognizer {

    override fun startListening(): Flow<String> = callbackFlow {
        val recognizer = AndroidSpeechRecognizer.createSpeechRecognizer(context)

        if (recognizer == null) {
            close(IllegalStateException("SpeechRecognizer not available on this device"))
            return@callbackFlow
        }

        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val message = when (error) {
                    AndroidSpeechRecognizer.ERROR_NO_MATCH -> "No speech match"
                    AndroidSpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timed out"
                    AndroidSpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                    AndroidSpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Missing permission"
                    AndroidSpeechRecognizer.ERROR_CLIENT -> "Client error"
                    else -> "Recognition error code $error"
                }
                close(IllegalStateException(message))
            }

            override fun onResults(results: Bundle?) {
                val matches = results
                    ?.getStringArrayList(AndroidSpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    trySend(matches.first())
                }
                close()
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults
                    ?.getStringArrayList(AndroidSpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    trySend(matches.first())
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        recognizer.setRecognitionListener(listener)

        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        recognizer.startListening(intent)

        awaitClose {
            recognizer.destroy()
        }
    }

    override fun cancel() {
        // Cancel is handled implicitly by the flow cancellation in the callbackFlow
    }
}
