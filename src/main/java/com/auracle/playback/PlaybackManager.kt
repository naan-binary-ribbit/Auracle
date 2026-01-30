package com.auracle.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.auracle.data.AudioFile
import com.auracle.data.Audiobook
import com.auracle.util.M4BChapterParser
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.net.Uri

class PlaybackManager(private val context: Context) {
    private val m4bParser = M4BChapterParser(context)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState = _playbackState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration = _duration.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying = _isPlaying.asStateFlow()

    private val _currentAudiobook = MutableStateFlow<Audiobook?>(null)
    val currentAudiobook = _currentAudiobook.asStateFlow()

    private val _currentChapter = MutableStateFlow<String?>("Chapter 1")
    val currentChapter = _currentChapter.asStateFlow()
    
    private val _currentChapterIndex = MutableStateFlow(0)
    val currentChapterIndex = _currentChapterIndex.asStateFlow()

    private val _chapters = MutableStateFlow<List<AudioFile>>(emptyList())
    val chapters = _chapters.asStateFlow()

    init {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            setupController()
        }, MoreExecutors.directExecutor())
    }

    private fun setupController() {
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = controller?.currentMediaItemIndex ?: 0
                _currentChapterIndex.value = index
                val currentFile = _chapters.value.getOrNull(index)
                _currentChapter.value = currentFile?.name ?: "File ${index + 1}"
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                _currentPosition.value = controller?.currentPosition ?: 0L
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> {
                        _duration.value = controller?.duration ?: 0L
                    }
                }
                _playbackState.value = when (state) {
                    Player.STATE_BUFFERING -> PlaybackState.Buffering
                    Player.STATE_READY -> PlaybackState.Ready
                    Player.STATE_ENDED -> PlaybackState.Ended
                    else -> PlaybackState.Idle
                }
            }

            override fun onMetadata(metadata: androidx.media3.common.Metadata) {
                val internalChapters = mutableListOf<AudioFile>()
                for (i in 0 until metadata.length()) {
                    val entry = metadata[i]
                    if (entry is androidx.media3.extractor.metadata.id3.TextInformationFrame) {
                        if (entry.id == "TIT2" || entry.id == "TIT3") {
                            _currentChapter.value = entry.values.firstOrNull() ?: ""
                        }
                    } else if (entry is androidx.media3.extractor.metadata.id3.ChapterFrame) {
                        // M4B/ID3 Chapters
                        internalChapters.add(
                            AudioFile(
                                name = entry.chapterId,
                                uri = _currentAudiobook.value?.audioFiles?.firstOrNull()?.uri ?: "",
                                duration = (entry.endTimeMs - entry.startTimeMs).toLong()
                            )
                        )
                    }
                }
                
                if (internalChapters.isNotEmpty()) {
                    _chapters.value = internalChapters
                    // Note: This replaces the file-list chapters for M4B/single file books
                }
            }
        })
    }
    
    // Update progress also checks for chapter changes in M4B
    // preParsedM4bChapters: when non-null, caller already parsed M4B chapters on background thread; use instead of parsing here (keeps MediaController on main thread).
    // autoPlay: when false, restores position but stays paused (e.g. after app restart).
    fun playAudiobook(audiobook: Audiobook, startIndex: Int = 0, startPosition: Long = 0L, preParsedM4bChapters: List<AudioFile>? = null, autoPlay: Boolean = true) {
        _currentAudiobook.value = audiobook
        
        val firstFile = audiobook.audioFiles.firstOrNull()
        val isSingleM4b = audiobook.audioFiles.size == 1 && firstFile?.name?.lowercase()?.endsWith(".m4b") == true
        if (isSingleM4b) {
            val chaptersToUse = if (preParsedM4bChapters != null) {
                if (preParsedM4bChapters.isNotEmpty()) preParsedM4bChapters else audiobook.audioFiles
            } else {
                val internalChapters = firstFile?.uri?.let { m4bParser.parseChapters(Uri.parse(it)) } ?: emptyList()
                if (internalChapters.isNotEmpty()) internalChapters else audiobook.audioFiles
            }
            _chapters.value = chaptersToUse
        } else {
            _chapters.value = audiobook.audioFiles
        }
        
        val mediaItems = audiobook.audioFiles.map { file ->
            MediaItem.Builder()
                .setMediaId(file.uri)
                .setUri(file.uri)
                .build()
        }

        // startIndex is MEDIA ITEM index (file index), not chapter index. For single-file (e.g. m4b)
        // we have only one media item, so startIndex must be 0 or ExoPlayer crashes.
        val mediaStartIndex = if (mediaItems.size == 1) 0 else startIndex.coerceIn(0, mediaItems.size - 1)
        // Clamp startPosition to avoid seeking past end (crashes when reopening finished books)
        val durationMs = audiobook.duration.coerceAtLeast(0L)
        val mediaStartPosition = if (durationMs > 0) startPosition.coerceIn(0L, durationMs - 1) else 0L

        controller?.setMediaItems(mediaItems, mediaStartIndex, mediaStartPosition)
        controller?.prepare()
        if (autoPlay) controller?.play() else controller?.pause()
    }

    fun pause() {
        controller?.pause()
    }

    fun play() {
        controller?.play()
    }

    fun seekTo(position: Long) {
        controller?.seekTo(position)
    }
    
    fun seekToChapter(index: Int) {
        if (_chapters.value.size > 1 && _currentAudiobook.value?.audioFiles?.size == 1) {
            // Seek to internal timestamp (guard against negative from bad chapter data)
            var timestamp = 0L
            for (i in 0 until index.coerceIn(0, _chapters.value.size - 1)) {
                timestamp += _chapters.value[i].duration.coerceAtLeast(0L)
            }
            controller?.seekTo(timestamp.coerceAtLeast(0L))
        } else {
            // Seek to file index
            controller?.seekTo(index.coerceIn(0, (_chapters.value.size - 1).coerceAtLeast(0)), 0L)
        }
    }

    fun skipForward() {
        controller?.seekTo((controller?.currentPosition ?: 0L) + 15000L)
    }

    fun skipBackward() {
        controller?.seekTo((controller?.currentPosition ?: 0L) - 15000L)
    }
    
    fun setPlaybackSpeed(speed: Float) {
        controller?.setPlaybackSpeed(speed)
    }

    fun updateProgress() {
        val currentMs = controller?.currentPosition ?: 0L
        _currentPosition.value = currentMs
        _duration.value = controller?.duration ?: 0L
        
        if (_chapters.value.size > 1 && _currentAudiobook.value?.audioFiles?.size == 1) {
            var accumulated = 0L
            for (i in _chapters.value.indices) {
                accumulated += _chapters.value[i].duration.coerceAtLeast(0L)
                if (currentMs < accumulated) {
                    _currentChapterIndex.value = i
                    _currentChapter.value = _chapters.value[i].name
                    break
                }
            }
        }
    }

    sealed class PlaybackState {
        object Idle : PlaybackState()
        object Buffering : PlaybackState()
        object Ready : PlaybackState()
        object Ended : PlaybackState()
    }
}
