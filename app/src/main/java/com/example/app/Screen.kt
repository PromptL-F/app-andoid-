package com.example.app

sealed class Screen(val route: String, val label: String) {
    object Library : Screen("library", "Biblioteca")
    object Playlists : Screen("playlists", "Playlists")
    object Favorites : Screen("favorites", "Favoritos")
    object Search : Screen("search", "Buscar")
    object PlaylistDetail : Screen("playlist_detail/{playlistId}", "Playlist") {
        fun createRoute(playlistId: Long) = "playlist_detail/$playlistId"
    }
}
