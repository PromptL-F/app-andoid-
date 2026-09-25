package com.example.app.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class MusicRepository(context: Context) {

    private val dao = AppDatabase.getInstance(context).musicDao()

    suspend fun ensureFavoritesPlaylistExists(): Long {
        val existing = dao.getFavoritesPlaylist()
        if (existing != null) return existing.id
        return dao.insertPlaylist(
            PlaylistEntity(name = "Favoritos", isFavorites = true)
        )
    }

    suspend fun ensureLibraryPlaylistContains(songIds: List<Long>): Long {
        val library = dao.getLibraryPlaylist() ?: PlaylistEntity(
            name = "Toda la música",
            isLibrary = true
        ).let { playlist ->
            val id = dao.insertPlaylist(playlist)
            playlist.copy(id = id)
        }

        songIds.forEach { songId ->
            if (!dao.isSongInPlaylist(library.id, songId)) {
                dao.addSongToPlaylist(PlaylistSongCrossRef(library.id, songId))
            }
        }
        return library.id
    }

    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = dao.getAllPlaylists()

    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity? =
        dao.getPlaylistById(playlistId)

    suspend fun createPlaylist(name: String): Long {
        return dao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        dao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        dao.addSongToPlaylist(PlaylistSongCrossRef(playlistId, songId))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        dao.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun isSongFavorite(songId: Long): Boolean {
        val favorites = dao.getFavoritesPlaylist() ?: return false
        return dao.isSongInPlaylist(favorites.id, songId)
    }

    suspend fun toggleFavorite(songId: Long) {
        val favorites = dao.getFavoritesPlaylist() ?: return
        if (dao.isSongInPlaylist(favorites.id, songId)) {
            dao.removeSongFromPlaylist(favorites.id, songId)
        } else {
            dao.addSongToPlaylist(PlaylistSongCrossRef(favorites.id, songId))
        }
    }

    fun getSongIdsForPlaylist(playlistId: Long): Flow<List<Long>> =
        dao.getSongIdsForPlaylist(playlistId)

    suspend fun addListenedTime(songId: Long, millis: Long) {
        dao.addListenedTime(songId, millis, System.currentTimeMillis())
    }
}
