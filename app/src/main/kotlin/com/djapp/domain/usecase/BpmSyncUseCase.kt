package com.djapp.domain.usecase

import com.djapp.domain.model.DeckId
import com.djapp.domain.repository.AudioEngineRepository
import com.djapp.domain.repository.BpmCacheRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-403: BPM検出・Sync ユースケース（UC-004 / AC-004）
 */
@Singleton
class BpmSyncUseCase @Inject constructor(
    private val audioEngine: AudioEngineRepository,
    private val bpmCache   : BpmCacheRepository,
) {
    /** C++エンジンから現在のBPMを取得する（0.0 = 未検出）。 */
    fun getBpm(deckId: DeckId): Float = audioEngine.getBpm(deckId)

    /** キャッシュからBPMを取得する。未登録なら null。 */
    suspend fun getCachedBpm(filePath: String): Float? = bpmCache.get(filePath)

    /** Phase 2完了後にC++から精度の高いBPMを取得してキャッシュに保存する。 */
    suspend fun saveDetectedBpm(filePath: String, deckId: DeckId) {
        val bpm = audioEngine.getBpm(deckId)
        if (bpm > 0f) bpmCache.save(filePath, bpm)
    }

    /**
     * ピッチ比率を ±16% の範囲にクランプして適用する。
     * @return 実際に適用されたクランプ後の値
     */
    fun setPitchRatio(deckId: DeckId, ratio: Float): Float {
        val clamped = ratio.coerceIn(0.68f, 1.32f)
        audioEngine.setPitchRatio(deckId, clamped)
        return clamped
    }

    /**
     * slave デッキを master デッキの実効BPMに同期させるピッチ比率を計算して適用する。
     * multiplier: 手動補正倍率（0.5 / 1.0 / 2.0）。検出BPMに乗算して実効BPMを得る。
     * @return 適用されたピッチ比率。いずれかのBPMが未検出なら null。
     */
    fun syncTo(
        slaveDeckId: DeckId,
        masterDeckId: DeckId,
        masterPitchRatio: Float,
        masterBpmMultiplier: Float = 1.0f,
        slaveBpmMultiplier: Float  = 1.0f,
    ): Float? {
        val masterBpm = audioEngine.getBpm(masterDeckId) * masterBpmMultiplier
        val slaveBpm  = audioEngine.getBpm(slaveDeckId)  * slaveBpmMultiplier
        if (masterBpm <= 0f || slaveBpm <= 0f) return null
        val targetRatio = (masterBpm * masterPitchRatio / slaveBpm).coerceIn(0.68f, 1.32f)
        audioEngine.setPitchRatio(slaveDeckId, targetRatio)
        return targetRatio
    }
}
