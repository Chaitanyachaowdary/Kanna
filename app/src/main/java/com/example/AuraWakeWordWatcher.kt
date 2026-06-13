package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class AuraWakeWordWatcher(
    private val context: Context,
    private val onWakeWordDetected: () -> Unit,
    private val onStatusUpdate: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var customWakeWord = "aura" // default wake word in lowercase
    private var isTrackingActive = false

    fun setWakeWord(word: String) {
        customWakeWord = word.trim().lowercase()
    }

    fun getWakeWord(): String = customWakeWord

    fun startListening() {
        isTrackingActive = true
        if (isListening) return
        
        // Ensure running on main thread because SpeechRecognizer requires it
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    onStatusUpdate("Speech recognition is not available on this device")
                    return@post
                }

                speechRecognizer?.destroy()
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isListening = true
                            onStatusUpdate("Aura wake-word listener is armed and listening...")
                        }

                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {}

                        override fun onError(error: Int) {
                            Log.w("AuraWakeWordWatcher", "Speech error code: $error")
                            isListening = false
                            
                            if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                                stopListeningSilently()
                            }
                            
                            // Retry after a short delay if tracking is still active
                            if (isTrackingActive) {
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    restartIfNeeded()
                                }, 1500)
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            isListening = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            var detected = false
                            if (matches != null) {
                                for (match in matches) {
                                    val matchLower = match.lowercase()
                                    if (matchLower.contains(customWakeWord)) {
                                        Log.d("AuraWakeWordWatcher", "Wake word '$customWakeWord' detected in results: $match")
                                        onWakeWordDetected()
                                        detected = true
                                        break
                                    }
                                }
                            }
                            
                            // If not triggered, keep listening, or pause briefly
                            val delayMs = if (detected) 2000L else 500L
                            if (isTrackingActive) {
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    restartIfNeeded()
                                }, delayMs)
                            }
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (matches != null) {
                                for (match in matches) {
                                    val matchLower = match.lowercase()
                                    if (matchLower.contains(customWakeWord)) {
                                        Log.d("AuraWakeWordWatcher", "Wake word '$customWakeWord' detected in partials: $match")
                                        onWakeWordDetected()
                                        break
                                    }
                                }
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString())
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                }

                speechRecognizer?.startListening(intent)
                isListening = true
            } catch (e: Exception) {
                onStatusUpdate("Speech error: ${e.message}")
                isListening = false
            }
        }
    }

    fun stopListening() {
        isTrackingActive = false
        isListening = false
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                Log.e("AuraWakeWordWatcher", "Error clean destroying speech recognizer", e)
            }
            speechRecognizer = null
            onStatusUpdate("Aura wake-word listener is offline.")
        }
    }

    private fun stopListeningSilently() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("AuraWakeWordWatcher", "Error silently cancelling speech recognizer", e)
        }
        speechRecognizer = null
        isListening = false
    }

    private fun restartIfNeeded() {
        if (isTrackingActive && !isListening) {
            startListening()
        }
    }
}
