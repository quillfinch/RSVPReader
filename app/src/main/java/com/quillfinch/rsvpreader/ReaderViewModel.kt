package com.quillfinch.rsvpreader

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quillfinch.rsvpreader.data.ReaderSettings
import com.quillfinch.rsvpreader.data.SettingsRepository
import com.quillfinch.rsvpreader.io.TextExtractor
import com.quillfinch.rsvpreader.rsvp.Tokenizer
import com.quillfinch.rsvpreader.rsvp.Word
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ReaderState(
    val word: Word? = null,
    val wordCount: Int = 0,
    val index: Int = 0,
    val playing: Boolean = false,
    val finished: Boolean = false,
)

class ReaderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    val settings: StateFlow<ReaderSettings> = repository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, ReaderSettings())

    val text = MutableStateFlow("")
    val fileName = MutableStateFlow<String?>(null)
    val busy = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)

    private val _reader = MutableStateFlow(ReaderState())
    val reader: StateFlow<ReaderState> = _reader.asStateFlow()

    private var words: List<Word> = emptyList()
    private var sentenceStarts: List<Int> = emptyList()
    private var loopJob: Job? = null

    fun onTextChange(value: String) {
        text.value = value
        fileName.value = null
    }

    fun loadFile(uri: Uri) {
        viewModelScope.launch {
            busy.value = true
            error.value = null
            try {
                val extracted = TextExtractor.extract(getApplication(), uri)
                text.value = extracted
                fileName.value = TextExtractor.displayName(getApplication(), uri)
            } catch (e: Exception) {
                error.value = e.message ?: "Could not read the file"
            } finally {
                busy.value = false
            }
        }
    }

    fun beginSession() {
        words = Tokenizer.tokenize(text.value)
        sentenceStarts = buildList {
            add(0)
            words.forEachIndexed { index, word ->
                if (index < words.lastIndex && Tokenizer.endsSentence(word)) add(index + 1)
            }
        }
        _reader.value = ReaderState(wordCount = words.size, index = 0)
        play()
    }

    fun playPause() = if (_reader.value.playing) pause() else play()

    fun restart() {
        pause()
        _reader.update { it.copy(index = 0, finished = false) }
    }

    fun pause() {
        loopJob?.cancel()
        loopJob = null
        _reader.update { it.copy(playing = false) }
    }

    fun setIndexFromFraction(fraction: Float) {
        pause()
        if (words.isEmpty()) return
        val target = (fraction * words.lastIndex).toInt().coerceIn(0, words.lastIndex)
        _reader.update { it.copy(index = target, finished = false) }
    }

    fun skipSentences(delta: Int) {
        pause()
        if (words.isEmpty()) return
        val from = _reader.value.index
        val target = if (delta >= 0) {
            sentenceStarts.firstOrNull { it > from } ?: words.lastIndex
        } else {
            sentenceStarts.lastOrNull { it < from } ?: 0
        }
        _reader.update { it.copy(index = target, finished = false) }
    }

    fun setWpm(value: Int) {
        viewModelScope.launch { repository.setWpm(value) }
    }

    fun setTextColor(value: Long) = viewModelScope.launch { repository.setTextColor(value) }

    fun setBackgroundColor(value: Long) = viewModelScope.launch { repository.setBackgroundColor(value) }

    fun setFocusColor(value: Long) = viewModelScope.launch { repository.setFocusColor(value) }

    fun resetColors() = viewModelScope.launch { repository.resetColors() }

    fun clearError() {
        error.value = null
    }

    private fun play() {
        if (words.isEmpty()) return
        if (_reader.value.finished || _reader.value.index >= words.lastIndex) {
            _reader.update { it.copy(index = 0, finished = false) }
        }
        _reader.update { it.copy(playing = true) }
        loopJob = viewModelScope.launch {
            while (true) {
                val index = _reader.value.index
                val word = words.getOrNull(index) ?: break
                _reader.update { it.copy(word = word) }
                val wpm = settings.value.wpm.coerceAtLeast(60)
                var millis = 60_000f / wpm * Tokenizer.delayFactor(word.text)
                if (word.paragraphEnd) millis += 350f
                delay(millis.toLong())
                if (index >= words.lastIndex) {
                    _reader.update { it.copy(playing = false, finished = true) }
                    break
                }
                _reader.update { it.copy(index = index + 1, finished = false) }
            }
        }
    }
}
