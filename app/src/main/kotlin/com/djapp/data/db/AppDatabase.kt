package com.djapp.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [BpmCacheEntity::class],
    version  = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bpmCacheDao(): BpmCacheDao
}
