package com.example.app

import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import kotlin.math.max

@Composable
fun MiniPlayer(
    player: MusicPlayer,
    onOpenFullPlayer: () -> Unit = {}
) {
    var isExpanded by rememberSaveable { mutableStateOf(true) }
    val repeatMode by player.repeatMode.collectAsState()
    val song by player.currentSong.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val position by player.position.collectAsState()
    val duration by player.duration.collectAsState()
    var isSeeking by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    val currentSong = song ?: return
    val safeDuration = max(duration, 1L)

    LaunchedEffect(position, isSeeking) {
        if (!isSeeking) {
            sliderPosition = position.toFloat().coerceIn(0f, safeDuration.toFloat())
        }
    }

    Column {
        // Barra delgada siempre visible: título + botón de colapsar/expandir
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded },
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${currentSong.title} · ${currentSong.artist}",
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowUp,
                    contentDescription = if (isExpanded) "Ocultar reproductor" else "Mostrar reproductor"
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 128.dp),
                tonalElevation = 6.dp,
                onClick = onOpenFullPlayer
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = currentSong.title, maxLines = 1)
                            Text(text = currentSong.artist, maxLines = 1)
                        }

                        Text(
                            text = "${formatTime(sliderPosition.toLong())} / ${formatTime(duration)}",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Slider(
                        value = sliderPosition.coerceIn(0f, safeDuration.toFloat()),
                        onValueChange = { newPosition ->
                            isSeeking = true
                            sliderPosition = newPosition
                        },
                        onValueChangeFinished = {
                            player.seekTo(sliderPosition.toLong())
                            isSeeking = false
                        },
                        valueRange = 0f..safeDuration.toFloat(),
                        enabled = duration > 0L,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        IconButton(onClick = { player.previous() }) {
                            Icon(Icons.Filled.SkipPrevious, contentDescription = "Canción anterior")
                        }
                        IconButton(onClick = { player.seekBack() }) {
                            Icon(Icons.Filled.FastRewind, contentDescription = "Retroceder 15 segundos")
                        }
                        IconButton(onClick = { player.togglePlayPause() }) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pausar" else "Reproducir"
                            )
                        }
                        IconButton(onClick = { player.seekForward() }) {
                            Icon(Icons.Filled.FastForward, contentDescription = "Adelantar 15 segundos")
                        }
                        IconButton(onClick = { player.next() }) {
                            Icon(Icons.Filled.SkipNext, contentDescription = "Siguiente")
                        }
                        IconButton(onClick = { player.cycleRepeatMode() }) {
                            Icon(
                                imageVector = if (repeatMode == REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                                contentDescription = "Modo repetición",
                                tint = if (repeatMode == REPEAT_MODE_OFF) Color.Gray else Color.Unspecified
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PlayerControls(
    player: MusicPlayer,
    modifier: Modifier = Modifier
) {
    val isPlaying by player.isPlaying.collectAsState()
    val repeatMode by player.repeatMode.collectAsState()

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        IconButton(onClick = { player.previous() }) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Canción anterior")
        }
        IconButton(onClick = { player.seekBack() }) {
            Icon(Icons.Filled.FastRewind, contentDescription = "Retroceder 15 segundos")
        }
        IconButton(onClick = { player.togglePlayPause() }) {
            Icon(
                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir"
            )
        }
        IconButton(onClick = { player.seekForward() }) {
            Icon(Icons.Filled.FastForward, contentDescription = "Adelantar 15 segundos")
        }
        IconButton(onClick = { player.next() }) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Siguiente")
        }
        IconButton(onClick = { player.cycleRepeatMode() }) {
            Icon(
                imageVector = if (repeatMode == REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                contentDescription = "Modo repetición",
                tint = if (repeatMode == REPEAT_MODE_OFF) Color.Gray else Color.Unspecified
            )
        }
    }
}

private fun formatTime(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1_000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}