package com.djapp.domain.repository

import com.djapp.domain.model.CrossfaderCurve
import com.djapp.domain.model.DeckId

/**
 * 音声エンジン（C++ / Oboe）の操作インターフェース。
 * Domain層はこのインターフェースのみに依存し、C++の実装詳細を知らない。
 * ADR-003: Clean Architecture / Repository パターン。
 */
interface AudioEngineRepository {
    fun initEngine(): Boolean
    fun destroyEngine()

    // --- デッキ操作（UC-001 / UC-002）---
    fun loadTrack(deckId: DeckId, filePath: String): Boolean
    fun play(deckId: DeckId)
    fun pause(deckId: DeckId)
    fun stop(deckId: DeckId)

    // --- 再生位置取得（T-203 追加）---
    fun getPlayheadSec(deckId: DeckId): Float
    fun getDurationSec(deckId: DeckId): Float

    // --- クロスフェーダー（UC-003）---
    fun setCrossfaderPosition(position: Float)   // 0.0(A) 〜 1.0(B)
    fun setCrossfaderCurve(curve: CrossfaderCurve)

    // --- EQ（UC-005）---
    fun setEqLow(deckId: DeckId, gainDb: Float)
    fun setEqMid(deckId: DeckId, gainDb: Float)
    fun setEqHigh(deckId: DeckId, gainDb: Float)

    // --- ピッチ / BPM同期（UC-004）---
    fun setPitchRatio(deckId: DeckId, ratio: Float)  // 0.84〜1.16
    fun getBpm(deckId: DeckId): Float                // 0.0 = 未検出
    fun getSampleRate(deckId: DeckId): Int

    // --- ループ（UC-006）---
    fun setLoop(deckId: DeckId, loopInSample: Long, loopOutSample: Long)
    fun clearLoop(deckId: DeckId)

    // --- シーク ---
    fun seekTo(deckId: DeckId, positionSec: Float)

    // --- サンプルパッド ---
    fun captureSample(slotId: Int, deckId: DeckId, inFrame: Long, outFrame: Long): Boolean
    fun playSample(slotId: Int, loop: Boolean)
    fun stopSample(slotId: Int)
    fun isSamplePlaying(slotId: Int): Boolean
    fun clearSample(slotId: Int)

    // --- スクラッチ（UC-007）---
    fun startScratch(deckId: DeckId)
    fun setScratchSpeed(deckId: DeckId, speed: Float)
    fun stopScratch(deckId: DeckId)

    // --- T-802: レイテンシ計測 ---
    fun getLatencyMs(): Double
    fun getXRunCount(): Int
}
