package com.example.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.app.data.MusicRepository
import com.example.app.ui.theme.HadesBackground
import com.example.app.ui.theme.HadesSurfaceVariant
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    songs: List<Song>,
    repository: MusicRepository,
    player: MusicPlayer,
    favoriteIds: Set<Long>,
    onToggleFavorite: (Long) -> Unit,
    onOpenPlaylist: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()
    val playlists by repository.getAllPlaylists().collectAsState(initial = emptyList())
    val userPlaylists = playlists.filter { !it.isFavorites && !it.isLibrary }
    val libraryPlaylist = playlists.firstOrNull { it.isLibrary }
    val librarySongIds by if (libraryPlaylist != null) {
        repository.getSongIdsForPlaylist(libraryPlaylist.id).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }
    val librarySongs = librarySongIds
        .mapNotNull { id -> songs.firstOrNull { song -> song.id == id } }
    var showCreateDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.hades_logo),
                contentDescription = "Logo de Hades",
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Text(
                text = "Ludwin F.",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 12.dp)
            )
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "all_songs") {
                PlaylistCard(
                    title = "Toda la música",
                    subtitle = "${librarySongs.size} canciones",
                    selected = true,
                    useLibraryIcon = true,
                    onClick = {
                        libraryPlaylist?.id?.let(onOpenPlaylist)
                    }
                )
            }

            items(items = userPlaylists, key = { it.id }) { playlist ->
                PlaylistCard(
                    title = playlist.name,
                    subtitle = "Playlist",
                    selected = false,
                    onClick = { onOpenPlaylist(playlist.id) },
                    playlistId = playlist.id,
                    repository = repository,
                    allSongs = songs
                )
            }

            item(key = "add_playlist") {
                AddPlaylistCard(onClick = { showCreateDialog = true })
            }
        }

    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Nueva playlist") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isNotBlank()) {
                        scope.launch { repository.createPlaylist(name) }
                    }
                    showCreateDialog = false
                }) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun PlaylistCard(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    useLibraryIcon: Boolean = false,
    playlistId: Long? = null,
    repository: MusicRepository? = null,
    allSongs: List<Song> = emptyList()
) {
    val playlistSongIds by if (playlistId != null && repository != null) {
        repository.getSongIdsForPlaylist(playlistId).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }
    val coverSongs = playlistSongIds
        .mapNotNull { id -> allSongs.firstOrNull { song -> song.id == id } }
        .take(4)

    Column(
        modifier = Modifier
            .width(128.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(HadesSurfaceVariant)
            .then(
                if (selected) {
                    Modifier.border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    )
                } else Modifier
            )
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (useLibraryIcon) {
                        Brush.linearGradient(listOf(HadesSurfaceVariant, HadesBackground))
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (useLibraryIcon) {
                LibraryIcon(
                    modifier = Modifier.size(width = 40.dp, height = 30.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                PlaylistCoverGrid(coverSongs)
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PlaylistCoverGrid(songs: List<Song>) {
    Column(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.Row(modifier = Modifier.weight(1f)) {
            PlaylistCoverTile(songs.getOrNull(0), Modifier.weight(1f))
            PlaylistCoverTile(songs.getOrNull(1), Modifier.weight(1f))
        }
        androidx.compose.foundation.layout.Row(modifier = Modifier.weight(1f)) {
            PlaylistCoverTile(songs.getOrNull(2), Modifier.weight(1f))
            PlaylistCoverTile(songs.getOrNull(3), Modifier.weight(1f))
        }
    }
}

@Composable
private fun PlaylistCoverTile(song: Song?, modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(0.5.dp),
        contentAlignment = Alignment.Center
    ) {
        if (song != null) {
            SongArtwork(song, modifier = Modifier.fillMaxSize(), fixedSize = false)
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(HadesBackground)
            )
        }
    }
}

@Composable
private fun AddPlaylistCard(onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(128.dp)
            .height(124.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = "Crear playlist",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Añadir",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/**
 * Logo de "biblioteca": tres barras verticales, con la última inclinada apoyándose
 * sobre las demás (como libros en un estante).
 */
@Composable
private fun LibraryIcon(
    modifier: Modifier = Modifier,
    tint: Color = Color.White
) {
    Canvas(modifier = modifier) {
        val barWidth = size.width * 0.18f
        val barSpacing = size.width * 0.10f
        val barHeight = size.height * 0.9f
        val cornerRadius = CornerRadius(barWidth * 0.35f, barWidth * 0.35f)
        val totalWidth = barWidth * 3 + barSpacing * 2
        val startX = (size.width - totalWidth) / 2f
        val bottomY = size.height

        for (i in 0..1) {
            val x = startX + i * (barWidth + barSpacing)
            drawRoundRect(
                color = tint,
                topLeft = Offset(x, bottomY - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius
            )
        }

        val thirdX = startX + 2 * (barWidth + barSpacing)
        val pivot = Offset(thirdX + barWidth / 2f, bottomY)
        rotate(degrees = 22f, pivot = pivot) {
            drawRoundRect(
                color = tint,
                topLeft = Offset(thirdX, bottomY - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = cornerRadius
            )
        }
    }
}
