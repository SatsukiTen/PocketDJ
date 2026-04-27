package com.djapp.domain.repository

interface BpmCacheRepository {
    suspend fun get(filePath: String): Float?
    suspend fun save(filePath: String, bpm: Float)
}
