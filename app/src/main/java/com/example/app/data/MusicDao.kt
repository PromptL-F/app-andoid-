package com.example.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MusicDao {

    // --- Playlists ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE isFavorites = 1 LIMIT 1")
    suspend fun getFavoritesPlaylist(): PlaylistEntity?

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    // --- Canciones dentro de playlists ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("SELECT songId FROM playlist_song_cross_ref WHERE playlistId = :playlistId ORDER BY addedAt DESC")
    fun getSongIdsForPlaylist(playlistId: Long): Flow<List<Long>>

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_song_cross_ref WHERE playlistId = :playlistId AND songId = :songId)")
    suspend fun isSongInPlaylist(playlistId: Long, songId: Long): Boolean

    // --- Estadísticas ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSongStats(stats: SongStatsEntity)

    @Query("SELECT * FROM song_stats WHERE songId = :songId")
    suspend fun getSongStats(songId: Long): SongStatsEntity?

    @Query("UPDATE song_stats SET totalListenedMillis = totalListenedMillis + :millis, lastPlayedAt = :timestamp WHERE songId = :songId")
    suspend fun addListenedTime(songId: Long, millis: Long, timestamp: Long)
}