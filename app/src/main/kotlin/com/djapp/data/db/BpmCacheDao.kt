package com.djapp.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface BpmCacheDao {

    @Query("SELECT * FROM bpm_cache WHERE filePath = :filePath LIMIT 1")
    suspend fun getByPath(filePath: String): BpmCacheEntity?

    @Upsert
    suspend fun upsert(entity: BpmCacheEntity)
}
