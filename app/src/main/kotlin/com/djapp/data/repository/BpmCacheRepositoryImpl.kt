package com.djapp.data.repository

import com.djapp.data.db.BpmCacheDao
import com.djapp.data.db.BpmCacheEntity
import com.djapp.domain.repository.BpmCacheRepository
import javax.inject.Inject

class BpmCacheRepositoryImpl @Inject constructor(
    private val dao: BpmCacheDao,
) : BpmCacheRepository {

    override suspend fun get(filePath: String): Float? =
        dao.getByPath(filePath)?.bpm

    override suspend fun save(filePath: String, bpm: Float) {
        dao.upsert(BpmCacheEntity(filePath = filePath, bpm = bpm))
    }
}
