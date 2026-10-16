package com.sonora.ai.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

sealed class VoiceState {
    data object Idle : VoiceState()
    data class Listening(val rmsDb: Float = 0f) : VoiceState()
    data object Processing : VoiceState()
    data class Success(val spokenText: String, val command: SonoraCommand) : VoiceState()
    data class Error(val message: String) : VoiceState()
}

class VoiceRecognitionManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(languageCode: String = "auto") {
        stopListening()

        if (!isAvailable()) {
            _voiceState.value = VoiceState.Error("Speech recognition is not available on this device.")
            return
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _voiceState.value = VoiceState.Listening(0f)
                    }

                    override fun onBeginningOfSpeech() {
                        _voiceState.value = VoiceState.Listening(2f)
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        if (_voiceState.value is VoiceState.Listening) {
                            _voiceState.value = VoiceState.Listening(rmsdB)
                        }
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _voiceState.value = VoiceState.Processing
                    }

                    override fun onError(error: Int) {
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required"
                            SpeechRecognizer.ERROR_NETWORK -> "Network connection error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please try again."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected"
                            else -> "Recognition error ($error)"
                        }
                        _voiceState.value = VoiceState.Error(message)
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()
                        if (!text.isNullOrBlank()) {
                            val command = VoiceCommandParser.parse(text)
                            _voiceState.value = VoiceState.Success(text, command)
                        } else {
                            _voiceState.value = VoiceState.Error("Could not understand voice command.")
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)

                when (languageCode.lowercase()) {
                    "tamil", "ta-in" -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ta-IN")
                    "english", "en-in" -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                    "en-us" -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    else -> putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                }
            }

            speechRecognizer?.startListening(intent)
            _voiceState.value = VoiceState.Listening(0f)
        } catch (e: Exception) {
            _voiceState.value = VoiceState.Error(e.localizedMessage ?: "Failed to start voice listener.")
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
    }

    fun resetState() {
        _voiceState.value = VoiceState.Idle
    }
}
