package com.example.app

sealed class Screen(val route: String, val label: String) {
    object Home : Screen("home", "Home")
    object Favorites : Screen("favorites", "Favoritos")
    object Search : Screen("search", "Buscar")
    object PlaylistDetail : Screen("playlist_detail/{playlistId}", "Playlist") {
        fun createRoute(playlistId: Long) = "playlist_detail/$playlistId"
    }
}
