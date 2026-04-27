package com.djapp.domain.repository

import com.djapp.domain.model.CrossfaderCurve
import com.djapp.domain.model.ScratchMode
import kotlinx.coroutines.flow.Flow

/**
 * アプリ設定の永続化インターフェース。
 */
interface SettingsRepository {
    fun getCrossfaderCurve(): Flow<CrossfaderCurve>
    suspend fun setCrossfaderCurve(curve: CrossfaderCurve)

    fun getScratchMode(): Flow<ScratchMode>
    suspend fun setScratchMode(mode: ScratchMode)
}
