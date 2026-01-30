package com.auracle.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.auracle.data.Audiobook
import com.auracle.data.PreferenceManager
import com.auracle.playback.PlaybackManager
import com.auracle.util.M4BChapterParser
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

class PlaybackViewModel(application: Application) : AndroidViewModel(application) {
    val playbackManager = PlaybackManager(application)
    private val preferenceManager = PreferenceManager(application)
    private val m4bParser = M4BChapterParser(application)

    init {
        // Save state periodically while playing
        viewModelScope.launch {
            while (true) {
                delay(2500)
                saveCurrentState()
            }
        }

        // Save immediately when user pauses
        viewModelScope.launch {
            var wasPlaying = playbackManager.isPlaying.value
            playbackManager.isPlaying.collect { isPlaying ->
                if (wasPlaying && !isPlaying) saveCurrentState()
                wasPlaying = isPlaying
            }
        }
    }

    private fun saveCurrentState() {
        viewModelScope.launch {
            val currentAudiobook = playbackManager.currentAudiobook.value
            if (currentAudiobook != null) {
                preferenceManager.savePlaybackState(
                    currentAudiobook.id,
                    playbackManager.currentChapterIndex.value,
                    playbackManager.currentPosition.value
                )
            }
        }
    }

    fun playAudiobook(audiobook: Audiobook) {
        viewModelScope.launch {
            val progress = preferenceManager.getBookProgress(audiobook.id).first()
            val firstFile = audiobook.audioFiles.firstOrNull()
            val isSingleM4b = audiobook.audioFiles.size == 1 && firstFile?.name?.lowercase()?.endsWith(".m4b") == true
            val preParsedChapters = if (isSingleM4b) withContext(Dispatchers.IO) {
                firstFile?.uri?.let { m4bParser.parseChapters(Uri.parse(it)) } ?: emptyList()
            } else null
            playbackManager.playAudiobook(audiobook, progress.first, progress.second, preParsedChapters, autoPlay = true)
        }
    }

    /** Restore last playing audiobook and position (paused) after app restart. */
    fun resumeLastPlayed(audiobooks: List<Audiobook>) {
        viewModelScope.launch {
            val lastId = preferenceManager.lastAudiobookId.first() ?: return@launch
            val lastState = preferenceManager.getLastPlaybackState().first()
            val book = audiobooks.find { it.id == lastId } ?: return@launch
            val firstFile = book.audioFiles.firstOrNull()
            val isSingleM4b = book.audioFiles.size == 1 && firstFile?.name?.lowercase()?.endsWith(".m4b") == true
            val preParsedChapters = if (isSingleM4b) withContext(Dispatchers.IO) {
                firstFile?.uri?.let { m4bParser.parseChapters(Uri.parse(it)) } ?: emptyList()
            } else null
            playbackManager.playAudiobook(book, lastState.first, lastState.second, preParsedChapters, autoPlay = false)
        }
    }
}
