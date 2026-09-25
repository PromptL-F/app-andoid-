package com.example.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isFavorites: Boolean = false,
    val isLibrary: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
