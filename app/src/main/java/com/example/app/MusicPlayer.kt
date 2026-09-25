package com.example.app

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
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

/** UI-facing controller for the MediaSession hosted by PlaybackService. */
class MusicPlayer(context: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val appContext = context.applicationContext
    private var controller: MediaController? = null
    private var connectionJob: Job? = null
    private var progressJob: Job? = null
    private var queue: List<Song> = emptyList()

    private val _repeatMode = MutableStateFlow(REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    init {
        val token = SessionToken(appContext, ComponentName(appContext, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture.addListener({
            runCatching { controllerFuture.get() }.onSuccess { connectedController ->
                controller = connectedController
                connectedController.addListener(object : androidx.media3.common.Player.Listener {
                    override fun onIsPlayingChanged(isPlaying: Boolean) = updateState()
                    override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) = updateState()
                    override fun onPlaybackStateChanged(playbackState: Int) = updateState()
                    override fun onRepeatModeChanged(repeatMode: Int) {
                        _repeatMode.value = repeatMode
                    }
                })
                updateState()
            }
        }, MoreExecutors.directExecutor())
        progressJob = scope.launch {
            while (isActive) {
                updateState()
                delay(500)
            }
        }
    }

    fun playSong(song: Song, songs: List<Song> = listOf(song)) {
        val safeQueue = if (songs.isEmpty()) listOf(song) else songs
        playQueue(safeQueue, safeQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0))
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        val mediaController = controller ?: return
        if (songs.isEmpty()) return
        queue = songs
        val items = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri(song.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(song.title)
                        .setArtist(song.artist)
                        .setAlbumTitle(song.album)
                        .setArtworkUri(song.artworkUri)
                        .build()
                )
                .build()
        }
        val safeIndex = startIndex.coerceIn(0, items.lastIndex)
        mediaController.setMediaItems(items, safeIndex, 0L)
        mediaController.prepare()
        mediaController.play()
        updateState()
    }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
    }

    fun cycleRepeatMode() {
        val mediaController = controller ?: return
        val next = when (mediaController.repeatMode) {
            REPEAT_MODE_OFF -> REPEAT_MODE_ALL
            REPEAT_MODE_ALL -> REPEAT_MODE_ONE
            else -> REPEAT_MODE_OFF
        }
        mediaController.repeatMode = next
        _repeatMode.value = next
    }

    fun seekForward(seconds: Long = 15L) = seekTo((controller?.currentPosition ?: 0L) + seconds * 1_000L)
    fun seekBack(seconds: Long = 15L) = seekTo((controller?.currentPosition ?: 0L) - seconds * 1_000L)

    fun seekTo(positionMillis: Long) {
        controller?.let {
            val maxPosition = it.duration.takeIf { duration -> duration > 0 } ?: Long.MAX_VALUE
            it.seekTo(positionMillis.coerceIn(0L, maxPosition))
            updateState()
        }
    }

    fun previous() { controller?.seekToPreviousMediaItem(); controller?.play() }
    fun next() { controller?.seekToNextMediaItem(); controller?.play() }

    fun release() {
        progressJob?.cancel()
        connectionJob?.cancel()
        controller?.release()
        controller = null
        scope.cancel()
    }

    private fun updateState() {
        val mediaController = controller ?: return
        _isPlaying.value = mediaController.isPlaying
        _position.value = max(0L, mediaController.currentPosition)
        _duration.value = max(0L, mediaController.duration)
        _repeatMode.value = mediaController.repeatMode
        _currentSong.value = queue.getOrNull(mediaController.currentMediaItemIndex)
    }
}
