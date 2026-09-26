package com.example.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.text.Normalizer

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchScreen(
    songs: List<Song>,
    favoriteIds: Set<Long> = emptySet(),
    onToggleFavorite: (Long) -> Unit = {},
    onSongClick: (Song) -> Unit = {},
    onAddToQueue: (Song) -> Unit = {}
) {
    var query by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current

    val filteredSongs = remember(query, songs) {
        val normalizedQuery = normalizeSearchText(query)

        if (normalizedQuery.isBlank()) {
            songs
        } else {
            songs.filter { song ->
                normalizeSearchText(song.title).contains(normalizedQuery) ||
                        normalizeSearchText(song.artist).contains(normalizedQuery)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
                    )
                )
            )
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Filled.Search, contentDescription = "Buscar")
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Filled.Clear, contentDescription = "Limpiar búsqueda")
                    }
                }
            },
            placeholder = { Text("Buscar por canción o artista") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = { keyboardController?.hide() }
            )
        )

        if (filteredSongs.isEmpty()) {
            EmptySearchState(
                query = query,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(
                    items = filteredSongs,
                    key = { it.id }
                ) { song ->
                    val isFavorite = song.id in favoriteIds
                    var showQueueMenu by remember { mutableStateOf(false) }

                    Box {
                        ListItem(
                            modifier = Modifier.combinedClickable(
                                onClick = { onSongClick(song) },
                                onLongClick = { showQueueMenu = true }
                            ),
                            headlineContent = { Text(song.title) },
                            supportingContent = song.metadataLineOrNull()?.let { metadata ->
                                { Text(metadata) }
                            },
                            leadingContent = { SongArtwork(song) },
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                                headlineColor = MaterialTheme.colorScheme.onSurface,
                                supportingColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            trailingContent = {
                                IconButton(onClick = { onToggleFavorite(song.id) }) {
                                    Icon(
                                        imageVector = if (isFavorite) {
                                            Icons.Filled.Favorite
                                        } else {
                                            Icons.Filled.FavoriteBorder
                                        },
                                        contentDescription = if (isFavorite) {
                                            "Quitar de favoritos"
                                        } else {
                                            "Añadir a favoritos"
                                        },
                                        tint = if (isFavorite) {
                                            MaterialTheme.colorScheme.tertiary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        )

                        DropdownMenu(
                            expanded = showQueueMenu,
                            onDismissRequest = { showQueueMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Añadir a la cola") },
                                leadingIcon = {
                                    Icon(Icons.Filled.QueueMusic, contentDescription = null)
                                },
                                onClick = {
                                    onAddToQueue(song)
                                    showQueueMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySearchState(
    query: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (query.isBlank()) {
                "No hay canciones disponibles"
            } else {
                "No se encontraron canciones"
            }
        )

        if (query.isNotBlank()) {
            Text(
                text = "Prueba con otro nombre o artista",
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

private fun normalizeSearchText(value: String): String {
    return Normalizer
        .normalize(value.trim().lowercase(), Normalizer.Form.NFD)
        .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
}
