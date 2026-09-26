package com.example.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.app.data.MusicRepository
import kotlinx.coroutines.launch

private enum class PlaylistSortMode(val label: String) {
    ADDED_NEWEST("Añadidas recientemente"),
    ADDED_OLDEST("Añadidas primero"),
    TITLE_ASC("Título A-Z"),
    ARTIST_ASC("Artista A-Z")
}

@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    allSongs: List<Song>,
    repository: MusicRepository,
    player: MusicPlayer,
    onDeleted: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val playlist = produceState<com.example.app.data.PlaylistEntity?>(
        initialValue = null,
        key1 = playlistId
    ) {
        value = repository.getPlaylistById(playlistId)
    }.value
    val isLibrary = playlist?.isLibrary == true
    val isFavorites = playlist?.isFavorites == true
    val canDelete = playlist != null && !isLibrary && !isFavorites
    val songIds by repository
        .getSongIdsForPlaylist(playlistId)
        .collectAsState(initial = emptyList())

    var showAddSongs by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(PlaylistSortMode.ADDED_NEWEST) }

    val songsById = remember(allSongs) {
        allSongs.associateBy { it.id }
    }

    val playlistSongsInAddedOrder = remember(songIds, songsById) {
        songIds.mapNotNull { songsById[it] }
    }

    val playlistSongs = remember(playlistSongsInAddedOrder, sortMode) {
        when (sortMode) {
            PlaylistSortMode.ADDED_NEWEST -> playlistSongsInAddedOrder
            PlaylistSortMode.ADDED_OLDEST -> playlistSongsInAddedOrder.asReversed()
            PlaylistSortMode.TITLE_ASC -> playlistSongsInAddedOrder.sortedBy {
                it.title.lowercase()
            }
            PlaylistSortMode.ARTIST_ASC -> playlistSongsInAddedOrder.sortedBy {
                it.artist.lowercase()
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = playlist?.name ?: "Playlist",
                        modifier = Modifier.padding(top = 16.dp)
                    )
                    Text(
                        text = sortMode.label,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Row {
                    if (canDelete) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "Borrar playlist"
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                Icons.Filled.Sort,
                                contentDescription = "Ordenar canciones"
                            )
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            PlaylistSortMode.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.label) },
                                    onClick = {
                                        sortMode = option
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!isLibrary) {
                FloatingActionButton(onClick = { showAddSongs = true }) {
                    Icon(
                        Icons.Filled.Add,
                        contentDescription = "Añadir canciones"
                    )
                }
            }
        }
    ) { innerPadding ->
        if (showAddSongs) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(
                    items = allSongs,
                    key = { it.id }
                ) { song ->
                    val inPlaylist = songIds.contains(song.id)

                    ListItem(
                        headlineContent = { Text(song.title) },
                        supportingContent = song.metadataLineOrNull()?.let { metadata ->
                            { Text(metadata) }
                        },
                        leadingContent = { SongArtwork(song) },
                        trailingContent = {
                            IconButton(onClick = {
                                scope.launch {
                                    if (inPlaylist) {
                                        repository.removeSongFromPlaylist(playlistId, song.id)
                                    } else {
                                        repository.addSongToPlaylist(playlistId, song.id)
                                    }
                                }
                            }) {
                                Icon(
                                    imageVector = if (inPlaylist) {
                                        Icons.Filled.Close
                                    } else {
                                        Icons.Filled.Add
                                    },
                                    contentDescription = if (inPlaylist) {
                                        "Quitar de la playlist"
                                    } else {
                                        "Añadir a la playlist"
                                    }
                                )
                            }
                        }
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                items(
                    items = playlistSongs,
                    key = { it.id }
                ) { song ->
                    ListItem(
                        modifier = Modifier.clickable {
                            player.playSong(song, playlistSongs)
                        },
                        headlineContent = { Text(song.title) },
                        supportingContent = song.metadataLineOrNull()?.let { metadata ->
                            { Text(metadata) }
                        },
                        leadingContent = { SongArtwork(song) }
                    )
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Borrar playlist") },
            text = {
                Text(
                    "¿Seguro que quieres borrar \"${playlist?.name}\"? Esta acción no se puede deshacer."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        repository.deletePlaylist(playlistId)
                    }
                    showDeleteConfirm = false
                    onDeleted()
                }) {
                    Text("Borrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
