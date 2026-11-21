package com.example.engine

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

data class TtsState(
    val isReady: Boolean = false,
    val isSpeaking: Boolean = false,
    val currentParagraphIndex: Int = -1,
    val sleepMinutesRemaining: Int = 0
)

class TtsManager(
    context: Context,
    private val scope: CoroutineScope,
    private val onRequestNextChapter: () -> Unit
) {
    private val _state = MutableStateFlow(TtsState())
    val state: StateFlow<TtsState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var paragraphs: List<String> = emptyList()
    private var currentIndex: Int = 0
    private var speechRate: Float = 1.0f
    private var speechPitch: Float = 1.0f
    private var sleepJob: Job? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {}
                    override fun onError(utteranceId: String?, errorCode: Int) {}
                    override fun onDone(utteranceId: String?) {
                        scope.launch(Dispatchers.Main) {
                            if (_state.value.isSpeaking) {
                                val next = currentIndex + 1
                                if (next < paragraphs.size) {
                                    speakParagraph(next)
                                } else {
                                    _state.value = _state.value.copy(isSpeaking = false, currentParagraphIndex = -1)
                                    onRequestNextChapter()
                                }
                            }
                        }
                    }
                })
                _state.value = _state.value.copy(isReady = true)
            }
        }
    }

    fun updateVoiceParams(rate: Float, pitch: Float) {
        speechRate = rate.coerceIn(0.5f, 2.5f)
        speechPitch = pitch.coerceIn(0.5f, 2.0f)
        tts?.setSpeechRate(speechRate)
        tts?.setPitch(speechPitch)
    }

    fun startReading(chapterParagraphs: List<String>, startIndex: Int = 0) {
        paragraphs = chapterParagraphs.filter { it.isNotBlank() }
        if (paragraphs.isEmpty() || !_state.value.isReady) return
        speakParagraph(startIndex.coerceIn(0, paragraphs.lastIndex))
    }

    private fun speakParagraph(index: Int) {
        if (index !in paragraphs.indices) {
            stop()
            return
        }
        currentIndex = index
        _state.value = _state.value.copy(isSpeaking = true, currentParagraphIndex = index)
        tts?.setSpeechRate(speechRate)
        tts?.setPitch(speechPitch)
        tts?.speak(paragraphs[index], TextToSpeech.QUEUE_FLUSH, null, "folio_p_$index")
    }

    fun nextParagraph() {
        if (currentIndex + 1 < paragraphs.size) speakParagraph(currentIndex + 1)
    }

    fun prevParagraph() {
        if (currentIndex - 1 >= 0) speakParagraph(currentIndex - 1)
    }

    fun stop() {
        tts?.stop()
        _state.value = _state.value.copy(isSpeaking = false, currentParagraphIndex = -1)
    }

    fun setSleepTimer(minutes: Int) {
        sleepJob?.cancel()
        if (minutes <= 0) {
            _state.value = _state.value.copy(sleepMinutesRemaining = 0)
            return
        }
        _state.value = _state.value.copy(sleepMinutesRemaining = minutes)
        sleepJob = scope.launch {
            var remaining = minutes
            while (remaining > 0) {
                delay(60_000L)
                remaining--
                _state.value = _state.value.copy(sleepMinutesRemaining = remaining)
            }
            stop()
        }
    }

    fun release() {
        sleepJob?.cancel()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
