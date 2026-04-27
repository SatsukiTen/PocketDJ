package com.djapp.domain.usecase

import com.djapp.domain.model.DeckId
import com.djapp.domain.repository.AudioEngineRepository
import javax.inject.Inject

/**
 * T-206: UC-002 デッキの再生・一時停止・停止を制御する。
 *
 * AC-002-01: play() → 再生開始
 * AC-002-02: pause() → 一時停止（位置は保持）
 * AC-002-03: stop() → 停止（位置を先頭に戻す）
 */
class PlaybackControlUseCase @Inject constructor(
    private val audioEngineRepository: AudioEngineRepository,
) {
    fun play(deckId: DeckId)  = audioEngineRepository.play(deckId)
    fun pause(deckId: DeckId) = audioEngineRepository.pause(deckId)
    fun stop(deckId: DeckId)  = audioEngineRepository.stop(deckId)

    fun seekTo(deckId: DeckId, positionSec: Float) = audioEngineRepository.seekTo(deckId, positionSec)

    fun getPlayheadSec(deckId: DeckId): Float = audioEngineRepository.getPlayheadSec(deckId)
    fun getDurationSec(deckId: DeckId): Float = audioEngineRepository.getDurationSec(deckId)
}
