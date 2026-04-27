package com.djapp.domain.usecase

import com.djapp.domain.model.CrossfaderCurve
import com.djapp.domain.repository.AudioEngineRepository
import com.djapp.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * T-303: クロスフェーダー操作ユースケース（UC-003）
 */
class CrossfaderUseCase @Inject constructor(
    private val audioEngine: AudioEngineRepository,
    private val settings: SettingsRepository,
) {
    /** AC-003-01〜03: フェーダー位置を [0.0, 1.0] の範囲でエンジンに設定する。 */
    fun setPosition(position: Float) {
        audioEngine.setCrossfaderPosition(position.coerceIn(0f, 1f))
    }

    /** AC-003-05: ダブルタップリセット — 位置を中央（0.5）に戻す。 */
    fun resetToCenter() = setPosition(0.5f)

    /**
     * AC-003-06〜09: カーブを変更し、エンジンへ即時反映かつ DataStore に永続化する。
     * エンジンへの適用が先に行われるため、設定変更は次の操作から反映される (AC-003-09)。
     */
    suspend fun setCurve(curve: CrossfaderCurve) {
        audioEngine.setCrossfaderCurve(curve)
        settings.setCrossfaderCurve(curve)
    }

    /**
     * DataStore から保存済みカーブを取得する Flow。
     * MixerViewModel が起動時・変更時に購読し、UI 状態と Audio Engine を同期する。
     */
    fun getCurveFlow(): Flow<CrossfaderCurve> = settings.getCrossfaderCurve()

    /** 現在の設定カーブをエンジンに適用する（起動時の同期用）。 */
    fun syncCurve(curve: CrossfaderCurve) {
        audioEngine.setCrossfaderCurve(curve)
    }
}
