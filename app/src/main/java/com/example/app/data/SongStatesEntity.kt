package com.example.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "song_stats")
data class SongStatsEntity(
    @PrimaryKey
    val songId: Long,
    val totalListenedMillis: Long = 0,
    val lastPlayedAt: Long? = null
)