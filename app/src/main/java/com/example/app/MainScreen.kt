package com.example.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.app.data.MusicRepository
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    songs: List<Song>,
    repository: MusicRepository,
    player: MusicPlayer
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()

    var favoriteIds by remember { mutableStateOf(setOf<Long>()) }

    suspend fun refreshFavorites() {
        repository.ensureFavoritesPlaylistExists()
        val ids = mutableSetOf<Long>()
        songs.forEach { song ->
            if (repository.isSongFavorite(song.id)) ids.add(song.id)
        }
        favoriteIds = ids
    }

    LaunchedEffect(Unit) {
        refreshFavorites()
    }

    fun toggleFavorite(songId: Long) {
        scope.launch {
            repository.toggleFavorite(songId)
            refreshFavorites()
        }
    }

    val items = listOf(Screen.Library, Screen.Playlists, Screen.Favorites, Screen.Search)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                MiniPlayer(player = player)

                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination

                    items.forEach { screen ->
                        val icon = when (screen) {
                            Screen.Library -> Icons.Filled.LibraryMusic
                            Screen.Playlists -> Icons.Filled.PlaylistPlay
                            Screen.Favorites -> Icons.Filled.Favorite
                            Screen.Search -> Icons.Filled.Search
                            else -> Icons.Filled.LibraryMusic
                        }
                        NavigationBarItem(
                            icon = { Icon(icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Library.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Library.route) {
                SongList(
                    songs = songs,
                    favoriteIds = favoriteIds,
                    onToggleFavorite = ::toggleFavorite,
                    onSongClick = { song ->
                        player.playSong(song, songs)
                    }
                )
            }
            composable(Screen.Playlists.route) {
                PlaylistsScreen(
                    repository = repository,
                    onOpenPlaylist = { playlistId ->
                        navController.navigate(Screen.PlaylistDetail.createRoute(playlistId))
                    }
                )
            }
            composable(
                route = Screen.PlaylistDetail.route,
                arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
            ) { backStackEntry ->
                val playlistId = backStackEntry.arguments?.getLong("playlistId") ?: 0L
                PlaylistDetailScreen(
                    playlistId = playlistId,
                    allSongs = songs,
                    repository = repository,
                    player = player
                )
            }
            composable(Screen.Favorites.route) {
                val favSongs = songs.filter { favoriteIds.contains(it.id) }
                SongList(
                    songs = favSongs,
                    favoriteIds = favoriteIds,
                    onToggleFavorite = ::toggleFavorite,
                    onSongClick = { song ->
                        player.playSong(song, favSongs)
                    }
                )
            }
            composable(Screen.Search.route) {
                SearchScreen(
                    songs = songs,
                    favoriteIds = favoriteIds,
                    onToggleFavorite = ::toggleFavorite,
                    onSongClick = { song ->
                        player.playSong(song, songs)
                    }
                )
            }
        }
    }
}
