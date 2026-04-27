package com.djapp.domain.usecase

import com.djapp.domain.model.DeckId
import com.djapp.domain.repository.AudioEngineRepository
import javax.inject.Inject

/**
 * T-503: EQ 操作 UseCase（UC-005 対応）
 * band: 0=Low, 1=Mid, 2=High  gainDb: -60.0（Kill）〜 +6.0
 */
class EqualizerUseCase @Inject constructor(
    private val audioEngine: AudioEngineRepository,
) {
    companion object {
        const val MIN_DB = -60f
        const val MAX_DB =   6f
    }

    fun setLow (deckId: DeckId, gainDb: Float) =
        audioEngine.setEqLow (deckId, gainDb.coerceIn(MIN_DB, MAX_DB))

    fun setMid (deckId: DeckId, gainDb: Float) =
        audioEngine.setEqMid (deckId, gainDb.coerceIn(MIN_DB, MAX_DB))

    fun setHigh(deckId: DeckId, gainDb: Float) =
        audioEngine.setEqHigh(deckId, gainDb.coerceIn(MIN_DB, MAX_DB))
}
