package com.example.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.app.data.MusicRepository
import kotlinx.coroutines.launch

@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    allSongs: List<Song>,
    repository: MusicRepository
) {
    val scope = rememberCoroutineScope()
    val songIds by repository.getSongIdsForPlaylist(playlistId).collectAsState(initial = emptyList())
    var showAddSongs by remember { mutableStateOf(false) }

    val playlistSongs = allSongs.filter { songIds.contains(it.id) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddSongs = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Añadir canciones")
            }
        }
    ) { innerPadding ->
        if (showAddSongs) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                items(allSongs) { song ->
                    val inPlaylist = songIds.contains(song.id)
                    ListItem(
                        headlineContent = { Text(song.title) },
                        supportingContent = { Text(song.artist) },
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
                                    imageVector = if (inPlaylist) Icons.Filled.Close else Icons.Filled.Add,
                                    contentDescription = if (inPlaylist) "Quitar" else "Añadir"
                                )
                            }
                        }
                    )
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                items(playlistSongs) { song ->
                    ListItem(
                        headlineContent = { Text(song.title) },
                        supportingContent = { Text(song.artist) }
                    )
                }
            }
        }
    }
}