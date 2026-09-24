package com.example.app

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

class MusicPlayer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val exoPlayer = ExoPlayer.Builder(context).build()
    private var progressJob: Job? = null
    private var queue: List<Song> = emptyList()

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _isPlaying.value = isPlaying
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateCurrentSong()
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updateProgress()
            }
        })

        progressJob = scope.launch {
            while (isActive) {
                updateProgress()
                delay(500)
            }
        }
    }

    fun playSong(song: Song, songs: List<Song> = listOf(song)) {
        val safeQueue = if (songs.isEmpty()) listOf(song) else songs
        val index = safeQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playQueue(safeQueue, index)
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return

        queue = songs
        val safeIndex = startIndex.coerceIn(0, songs.lastIndex)
        val mediaItems = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.uri)
                .build()
        }

        exoPlayer.setMediaItems(mediaItems, safeIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.play()
        updateCurrentSong()
    }

    fun togglePlayPause() {
        if (_currentSong.value == null) return
        if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
    }

    fun seekForward(seconds: Long = 15L) {
        seekTo(exoPlayer.currentPosition + seconds * 1_000L)
    }

    fun seekBack(seconds: Long = 15L) {
        seekTo(exoPlayer.currentPosition - seconds * 1_000L)
    }

    fun seekTo(positionMillis: Long) {
        val maxPosition = exoPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        exoPlayer.seekTo(positionMillis.coerceIn(0L, maxPosition))
        updateProgress()
    }

    fun previous() {
        exoPlayer.seekToPreviousMediaItem()
        exoPlayer.play()
    }

    fun next() {
        exoPlayer.seekToNextMediaItem()
        exoPlayer.play()
    }

    fun release() {
        progressJob?.cancel()
        scope.cancel()
        exoPlayer.release()
    }

    private fun updateCurrentSong() {
        _currentSong.value = queue.getOrNull(exoPlayer.currentMediaItemIndex)
        updateProgress()
    }

    private fun updateProgress() {
        _position.value = max(0L, exoPlayer.currentPosition)
        _duration.value = max(0L, exoPlayer.duration)
        _isPlaying.value = exoPlayer.isPlaying
    }
}
