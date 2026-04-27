package com.djapp.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bpm_cache")
data class BpmCacheEntity(
    @PrimaryKey
    val filePath  : String,
    val bpm       : Float,
    val cachedAt  : Long = System.currentTimeMillis(),
)
