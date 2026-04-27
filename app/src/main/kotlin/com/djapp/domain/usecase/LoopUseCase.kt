package com.djapp.domain.usecase

import com.djapp.domain.model.DeckId
import com.djapp.domain.model.LoopState
import com.djapp.domain.repository.AudioEngineRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UC-006: マニュアルループ再生
 * IN/OUT 秒位置を直接指定してループ区間をエンジンに設定する。
 */
@Singleton
class LoopUseCase @Inject constructor(
    private val repository: AudioEngineRepository,
) {
    fun activate(deckId: DeckId, loopInSec: Float, loopOutSec: Float): LoopState {
        val sampleRate = repository.getSampleRate(deckId)
        repository.setLoop(deckId, (loopInSec * sampleRate).toLong(), (loopOutSec * sampleRate).toLong())
        return LoopState(loopInSec, loopOutSec)
    }

    fun doubleLength(deckId: DeckId, currentLoop: LoopState): LoopState {
        val dur = currentLoop.loopOutSec - currentLoop.loopInSec
        return activate(deckId, currentLoop.loopInSec, currentLoop.loopOutSec + dur)
    }

    // 最小ループ長 50ms（短すぎるとオーディオエンジンが不安定になるため）
    fun halveLength(deckId: DeckId, currentLoop: LoopState): LoopState {
        val dur = ((currentLoop.loopOutSec - currentLoop.loopInSec) / 2f).coerceAtLeast(MIN_LOOP_SEC)
        return activate(deckId, currentLoop.loopInSec, currentLoop.loopInSec + dur)
    }

    // IN 点を deltaSec だけずらす（OUT - MIN_LOOP_SEC を超えないようクランプ）
    fun nudgeIn(deckId: DeckId, currentLoop: LoopState, deltaSec: Float): LoopState {
        val newIn = (currentLoop.loopInSec + deltaSec)
            .coerceAtLeast(0f)
            .coerceAtMost(currentLoop.loopOutSec - MIN_LOOP_SEC)
        return activate(deckId, newIn, currentLoop.loopOutSec)
    }

    // OUT 点を deltaSec だけずらす（IN + MIN_LOOP_SEC を下回らないようクランプ）
    fun nudgeOut(deckId: DeckId, currentLoop: LoopState, deltaSec: Float): LoopState {
        val newOut = (currentLoop.loopOutSec + deltaSec)
            .coerceAtLeast(currentLoop.loopInSec + MIN_LOOP_SEC)
        return activate(deckId, currentLoop.loopInSec, newOut)
    }

    fun deactivate(deckId: DeckId) {
        repository.clearLoop(deckId)
    }

    companion object {
        const val MIN_LOOP_SEC = 0.05f
        const val NUDGE_SEC    = 0.05f
    }
}
