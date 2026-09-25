package com.example.app

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import coil.compose.AsyncImage
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.app.data.MusicRepository
import com.example.app.ui.theme.AppTheme
import kotlinx.coroutines.launch

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMillis: Long,
    val artworkUri: Uri?,
    val uri: Uri
)

class MainActivity : ComponentActivity() {

    private var songs = mutableStateOf<List<Song>>(emptyList())
    private var permissionStatus = mutableStateOf("Verificando permiso...")
    private lateinit var repository: MusicRepository
    private lateinit var musicPlayer: MusicPlayer

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionStatus.value = "Permiso concedido"
            songs.value = loadSongs()
        } else {
            permissionStatus.value = "Permiso denegado"
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        repository = MusicRepository(applicationContext)
        musicPlayer = MusicPlayer(applicationContext)

        lifecycleScope.launch {
            repository.ensureFavoritesPlaylistExists()
        }

        val permission = if (
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
        ) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            permissionStatus.value = "Permiso ya concedido"
            songs.value = loadSongs()
        } else {
            permissionLauncher.launch(permission)
        }

        setContent {
            AppTheme {
                MainScreen(
                    songs = songs.value,
                    repository = repository,
                    player = musicPlayer
                )
            }
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onDestroy() {
        if (::musicPlayer.isInitialized) {
            musicPlayer.release()
        }
        super.onDestroy()
    }

    private fun loadSongs(): List<Song> {
        val songList = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

        val cursor = contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn).cleanMetadata("Título desconocido")
                val artist = it.getString(artistColumn).cleanMetadata()
                val album = it.getString(albumColumn).cleanMetadata()
                val albumId = it.getLong(albumIdColumn)
                val artworkUri = albumId.takeIf { value -> value > 0 }?.let { value ->
                    Uri.parse("content://media/external/audio/albumart/$value")
                }
                val uri = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                songList.add(
                    Song(
                        id = id,
                        title = title,
                        artist = artist,
                        album = album,
                        durationMillis = it.getLong(durationColumn),
                        artworkUri = artworkUri,
                        uri = uri
                    )
                )
            }
        }

        return songList
    }
}

private fun String?.cleanMetadata(fallback: String = ""): String =
    this?.takeIf { value -> value.isNotBlank() && value != "<unknown>" } ?: fallback

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongList(
    songs: List<Song>,
    modifier: Modifier = Modifier,
    favoriteIds: Set<Long> = emptySet(),
    showHeader: Boolean = true,
    onToggleFavorite: (Long) -> Unit = {},
    onSongClick: (Song) -> Unit = {},
    onAddToQueue: (Song) -> Unit = {}
) {
    Column(modifier = modifier.fillMaxSize()) {
        if (showHeader) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.hades_logo),
                        contentDescription = "Logo de Hades",
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            text = "Hades",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Tu música",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                }
                Text(
                    text = "${songs.size} canciones",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(
                items = songs,
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
                            containerColor = Color.Transparent,
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

@Composable
fun SongArtwork(
    song: Song,
    modifier: Modifier = Modifier
) {
    val artworkModifier = modifier
        .size(56.dp)
        .clip(RoundedCornerShape(8.dp))

    if (song.artworkUri != null) {
        AsyncImage(
            model = song.artworkUri,
            contentDescription = "Portada de ${song.album}",
            modifier = artworkModifier,
            contentScale = ContentScale.Crop
        )
    } else {
        val palettes = listOf(
            listOf(Color(0xFF512DA8), Color(0xFFE040FB)),
            listOf(Color(0xFF00695C), Color(0xFF26A69A)),
            listOf(Color(0xFF1565C0), Color(0xFF42A5F5)),
            listOf(Color(0xFFAD1457), Color(0xFFFF7043)),
            listOf(Color(0xFF4E342E), Color(0xFFFFB300))
        )
        val palette = palettes[(song.id % palettes.size).toInt()]

        Box(
            modifier = artworkModifier.background(
                Brush.linearGradient(palette)
            ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.MusicNote,
                contentDescription = "Carátula generada",
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }
}

fun Song.metadataLineOrNull(): String? =
    listOf(artist, album).filter { it.isNotBlank() }.joinToString(" · ").takeIf { it.isNotBlank() }
