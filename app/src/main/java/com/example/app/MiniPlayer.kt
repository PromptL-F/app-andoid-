package com.example.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import android.net.Uri
import coil.compose.AsyncImage
import com.example.app.ui.theme.HadesBackground
import com.example.app.ui.theme.HadesBackgroundGlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.max

private const val COLLAPSED_VINYL_SIZE_DP = 40
private const val EXPANDED_VINYL_SIZE_DP = 220

@Composable
fun MiniPlayer(
    player: MusicPlayer,
    onOpenFullPlayer: () -> Unit = {}
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val repeatMode by player.repeatMode.collectAsState()
    val song by player.currentSong.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val position by player.position.collectAsState()
    val duration by player.duration.collectAsState()
    var isSeeking by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    val currentSong = song ?: return
    val safeDuration = max(duration, 1L)

    // Ángulo del disco de vinilo: gira mientras suena, se congela al pausar.
    var rotationDegrees by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isActive) {
                delay(16L)
                rotationDegrees = (rotationDegrees + 1.2f) % 360f
            }
        }
    }

    LaunchedEffect(position, isSeeking) {
        if (!isSeeking) {
            sliderPosition = position.toFloat().coerceIn(0f, safeDuration.toFloat())
        }
    }

    if (isExpanded) {
        ExpandedPlayer(
            song = currentSong,
            isPlaying = isPlaying,
            rotationDegrees = rotationDegrees,
            sliderPosition = sliderPosition,
            duration = duration,
            safeDuration = safeDuration,
            repeatMode = repeatMode,
            onCollapse = { isExpanded = false },
            onSeekChange = { newPosition ->
                isSeeking = true
                sliderPosition = newPosition
            },
            onSeekFinished = {
                player.seekTo(sliderPosition.toLong())
                isSeeking = false
            },
            player = player
        )
    } else {
        CollapsedBar(
            song = currentSong,
            isPlaying = isPlaying,
            rotationDegrees = rotationDegrees,
            onExpand = { isExpanded = true },
            player = player
        )
    }
}

@Composable
private fun CollapsedBar(
    song: Song,
    isPlaying: Boolean,
    rotationDegrees: Float,
    onExpand: () -> Unit,
    player: MusicPlayer
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onExpand() },
        color = Color.Transparent,
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                            HadesBackgroundGlow,
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    ),
                    RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                )
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            VinylDisc(
                artworkUri = song.artworkUri,
                rotationDegrees = rotationDegrees,
                size = COLLAPSED_VINYL_SIZE_DP.dp
            )

            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { player.previous() }) {
                Icon(Icons.Filled.SkipPrevious, contentDescription = "Canción anterior")
            }
            IconButton(onClick = { player.togglePlayPause() }) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pausar" else "Reproducir"
                )
            }
            IconButton(onClick = { player.next() }) {
                Icon(Icons.Filled.SkipNext, contentDescription = "Saltar canción")
            }
        }
    }
}

@Composable
private fun ExpandedPlayer(
    song: Song,
    isPlaying: Boolean,
    rotationDegrees: Float,
    sliderPosition: Float,
    duration: Long,
    safeDuration: Long,
    repeatMode: Int,
    onCollapse: () -> Unit,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    player: MusicPlayer
) {
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val expandedHeight = (screenHeightDp * 0.6f).dp

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(expandedHeight),
        color = Color.Transparent,
        tonalElevation = 12.dp,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surfaceContainerHighest,
                            MaterialTheme.colorScheme.surface,
                            HadesBackground
                        )
                    ),
                    RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
                )
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = onCollapse,
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Ocultar reproductor")
                }
                IconButton(
                    onClick = { player.cycleRepeatMode() },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (repeatMode == REPEAT_MODE_OFF) {
                                Color.Transparent
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                            }
                        )
                ) {
                    Icon(
                        imageVector = if (repeatMode == REPEAT_MODE_ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "Modo repetición",
                        tint = if (repeatMode == REPEAT_MODE_OFF) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                VinylDisc(
                    artworkUri = song.artworkUri,
                    rotationDegrees = rotationDegrees,
                    size = EXPANDED_VINYL_SIZE_DP.dp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = song.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Slider(
                value = sliderPosition.coerceIn(0f, safeDuration.toFloat()),
                onValueChange = onSeekChange,
                onValueChangeFinished = onSeekFinished,
                valueRange = 0f..safeDuration.toFloat(),
                enabled = duration > 0L,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = formatTime(sliderPosition.toLong()), style = MaterialTheme.typography.labelSmall)
                Text(text = formatTime(duration), style = MaterialTheme.typography.labelSmall)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { player.previous() }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Canción anterior")
                }
                IconButton(onClick = { player.seekBack() }) {
                    Icon(Icons.Filled.FastRewind, contentDescription = "Retroceder 15 segundos")
                }
                IconButton(
                    onClick = { player.togglePlayPause() },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                IconButton(onClick = { player.seekForward() }) {
                    Icon(Icons.Filled.FastForward, contentDescription = "Adelantar 15 segundos")
                }
                IconButton(onClick = { player.next() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Siguiente")
                }
            }
        }
    }
}

/**
 * Disco de vinilo con la carátula de la canción en el centro. Gira según [rotationDegrees],
 * que el llamador congela cuando la reproducción está en pausa.
 */
@Composable
private fun VinylDisc(
    artworkUri: Uri?,
    rotationDegrees: Float,
    size: Dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .rotate(rotationDegrees)
            .clip(CircleShape)
            .background(HadesBackground),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val ringCount = 5
            val maxRadius = this.size.minDimension / 2f
            for (i in 1..ringCount) {
                val radius = maxRadius * (0.55f + 0.08f * i)
                if (radius < maxRadius) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.06f),
                        radius = radius,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }

        val artworkSize = size * 0.55f
        if (artworkUri != null) {
            AsyncImage(
                model = artworkUri,
                contentDescription = null,
                modifier = Modifier
                    .size(artworkSize)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(artworkSize)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(artworkSize * 0.4f)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(size * 0.08f)
                .clip(CircleShape)
                .background(HadesBackground)
        )
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
