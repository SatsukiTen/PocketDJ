package com.djapp.data.repository

import com.djapp.data.source.SettingsDataSource
import com.djapp.domain.model.CrossfaderCurve
import com.djapp.domain.model.ScratchMode
import com.djapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-007: SettingsRepositoryの実装。
 */
@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataSource: SettingsDataSource,
) : SettingsRepository {

    override fun getCrossfaderCurve(): Flow<CrossfaderCurve> =
        dataSource.getCrossfaderCurve().map { key ->
            CrossfaderCurve.entries.find { it.name == key } ?: CrossfaderCurve.EQUAL_POWER
        }

    override suspend fun setCrossfaderCurve(curve: CrossfaderCurve) {
        dataSource.setCrossfaderCurve(curve.name)
    }

    override fun getScratchMode(): Flow<ScratchMode> =
        dataSource.getScratchMode().map { key ->
            ScratchMode.entries.find { it.name == key } ?: ScratchMode.CHOP_PAD
        }

    override suspend fun setScratchMode(mode: ScratchMode) {
        dataSource.setScratchMode(mode.name)
    }
}
