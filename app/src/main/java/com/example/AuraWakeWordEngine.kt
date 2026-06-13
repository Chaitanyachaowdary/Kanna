package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

class AuraWakeWordEngine(
    private val context: Context,
    private var wakeWord: String,
    private val onWakeWordDetected: (String) -> Unit,
    private val onStateChanged: (Boolean) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isEngineRunning = false
    private var isListeningToSpeech = false

    fun updateWakeWord(newWord: String) {
        wakeWord = newWord
        Log.d("AuraWakeWord", "Updated engine wake word target to: $wakeWord")
    }

    fun start() {
        if (isEngineRunning) return
        isEngineRunning = true
        onStateChanged(true)
        initializeAndListen()
    }

    fun stop() {
        isEngineRunning = false
        onStateChanged(false)
        destroyRecognizer()
    }

    fun isActive(): Boolean = isEngineRunning

    private fun destroyRecognizer() {
        handler.removeCallbacksAndMessages(null)
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("AuraWakeWord", "Error destroying speech recognizer", e)
        }
        speechRecognizer = null
        isListeningToSpeech = false
    }

    private fun initializeAndListen() {
        if (!isEngineRunning) return
        destroyRecognizer()

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("AuraWakeWord", "Speech recognition not available on this device!")
            return
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListeningToSpeech = true
                        Log.d("AuraWakeWord", "Ready for voice wake word input...")
                    }

                    override fun onBeginningOfSpeech() {
                        Log.d("AuraWakeWord", "User started speaking...")
                    }

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListeningToSpeech = false
                    }

                    override fun onError(error: Int) {
                        isListeningToSpeech = false
                        Log.w("AuraWakeWord", "Speech recognizer error: $error")
                        // Wait a little and restart to keep continuous listening alive
                        if (isEngineRunning) {
                            handler.postDelayed({ initializeAndListen() }, 600)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        isListeningToSpeech = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (matches != null) {
                            for (match in matches) {
                                Log.d("AuraWakeWord", "Recognized speech: $match")
                                val target = wakeWord.lowercase().trim()
                                val speech = match.lowercase()
                                if (speech.contains(target)) {
                                    Log.i("AuraWakeWord", "WAKE WORD MATCHED: $wakeWord")
                                    val index = speech.indexOf(target)
                                    val remaining = if (index != -1) {
                                        match.substring(index + target.length).trim()
                                    } else {
                                        ""
                                    }
                                    onWakeWordDetected(remaining)
                                    break
                                }
                            }
                        }
                        // Continue continuous hotword loop
                        if (isEngineRunning) {
                            handler.postDelayed({ initializeAndListen() }, 500)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (matches != null) {
                            for (match in matches) {
                                val target = wakeWord.lowercase().trim()
                                val speech = match.lowercase()
                                if (speech.contains(target)) {
                                    Log.i("AuraWakeWord", "PARTIAL MATCH DETECTED FOR WAKE WORD: $wakeWord")
                                    val index = speech.indexOf(target)
                                    val remaining = if (index != -1) {
                                        match.substring(index + target.length).trim()
                                    } else {
                                        ""
                                    }
                                    onWakeWordDetected(remaining)
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
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("AuraWakeWord", "Error starting SpeechRecognizer", e)
            if (isEngineRunning) {
                handler.postDelayed({ initializeAndListen() }, 1000)
            }
        }
    }
}
