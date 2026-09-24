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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.app.data.MusicRepository
import com.example.app.ui.theme.AppTheme
import kotlinx.coroutines.launch

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
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
            MediaStore.Audio.Media.ARTIST
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

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn) ?: "Desconocido"
                val artist = it.getString(artistColumn) ?: "Artista desconocido"
                val uri = Uri.withAppendedPath(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
                songList.add(Song(id, title, artist, uri))
            }
        }

        return songList
    }
}

@Composable
fun SongList(
    songs: List<Song>,
    modifier: Modifier = Modifier,
    favoriteIds: Set<Long> = emptySet(),
    onToggleFavorite: (Long) -> Unit = {},
    onSongClick: (Song) -> Unit = {}
) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(
            items = songs,
            key = { it.id }
        ) { song ->
            val isFavorite = song.id in favoriteIds

            ListItem(
                modifier = Modifier.clickable {
                    onSongClick(song)
                },
                headlineContent = { Text(song.title) },
                supportingContent = { Text(song.artist) },
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
                            }
                        )
                    }
                }
            )
        }
    }
}
