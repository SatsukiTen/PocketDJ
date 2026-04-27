package com.djapp.domain.usecase

import com.djapp.domain.model.DeckId
import com.djapp.domain.repository.AudioEngineRepository
import javax.inject.Inject

/**
 * UC-007: スクラッチ操作の UseCaseo
 * VirtualPlatter の drag 量（速度倍率）を C++ エンジンに橋渡しする。
 */
class ScratchUseCase @Inject constructor(
    private val audioEngine: AudioEngineRepository,
) {
    fun start(deckId: DeckId) = audioEngine.startScratch(deckId)
    fun setSpeed(deckId: DeckId, speed: Float) = audioEngine.setScratchSpeed(deckId, speed)
    fun stop(deckId: DeckId) = audioEngine.stopScratch(deckId)
}
