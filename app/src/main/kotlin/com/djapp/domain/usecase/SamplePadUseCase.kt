package com.djapp.domain.usecase

import com.djapp.domain.model.DeckId
import com.djapp.domain.model.LoopState
import com.djapp.domain.repository.AudioEngineRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * UC-008: サンプルパッド — ループ区間をキャプチャして独立再生する。
 * 4スロット固定。スロットはアプリ起動中はC++メモリで保持（曲変更の影響なし）。
 */
@Singleton
class SamplePadUseCase @Inject constructor(
    private val repository: AudioEngineRepository,
) {
    fun capture(slotId: Int, deckId: DeckId, loop: LoopState): Boolean {
        val sr       = repository.getSampleRate(deckId)
        val inFrame  = (loop.loopInSec  * sr).toLong()
        val outFrame = (loop.loopOutSec * sr).toLong()
        return repository.captureSample(slotId, deckId, inFrame, outFrame)
    }

    fun play(slotId: Int, loop: Boolean) = repository.playSample(slotId, loop)
    fun stop(slotId: Int)                = repository.stopSample(slotId)
    fun isPlaying(slotId: Int): Boolean  = repository.isSamplePlaying(slotId)
    fun clear(slotId: Int)               = repository.clearSample(slotId)
}
