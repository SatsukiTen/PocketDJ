package com.djapp.data.source

import javax.inject.Inject
import javax.inject.Singleton

/**
 * T-005: JNIブリッジ — KotlinからC++音声エンジンを呼び出す窓口。
 * ここで定義するexternalメソッドが djapp_jni.cpp の native関数に対応する。
 */
@Singleton
class AudioEngineDataSource @Inject constructor() {

    init {
        System.loadLibrary("djapp")
    }

    // --- エンジン初期化・破棄 ---
    external fun nativeInitEngine(): Boolean
    external fun nativeDestroyEngine()

    // --- デッキ操作 ---
    // Android 10+ 対応: content URI → fd 変換済みのファイルディスクリプタを渡す
    external fun nativeLoadTrackFd(deckId: Int, fd: Int): Boolean
    external fun nativePlay(deckId: Int)
    external fun nativePause(deckId: Int)
    external fun nativeStop(deckId: Int)

    // --- 再生位置取得（T-203 追加）---
    external fun nativeGetPlayheadSec(deckId: Int): Float
    external fun nativeGetDurationSec(deckId: Int): Float

    // --- クロスフェーダー ---
    external fun nativeSetCrossfaderPosition(position: Float)
    external fun nativeSetCrossfaderCurve(curveType: Int)

    // --- EQ ---
    external fun nativeSetEq(deckId: Int, band: Int, gainDb: Float)

    // --- ピッチ / BPM ---
    external fun nativeSetPitch(deckId: Int, pitchRatio: Float)
    external fun nativeGetBpm(deckId: Int): Float
    external fun nativeGetSampleRate(deckId: Int): Int

    // --- ループ ---
    external fun nativeSetLoop(deckId: Int, loopInSample: Long, loopOutSample: Long)
    external fun nativeClearLoop(deckId: Int)

    // --- シーク ---
    external fun nativeSeekTo(deckId: Int, positionSec: Float)

    // --- サンプルパッド ---
    external fun nativeCaptureSample(slotId: Int, deckId: Int, inFrame: Long, outFrame: Long): Boolean
    external fun nativePlaySample(slotId: Int, loop: Boolean)
    external fun nativeStopSample(slotId: Int)
    external fun nativeIsSamplePlaying(slotId: Int): Boolean
    external fun nativeClearSample(slotId: Int)

    // --- スクラッチ ---
    external fun nativeStartScratch(deckId: Int)
    external fun nativeSetScratchSpeed(deckId: Int, speed: Float)
    external fun nativeStopScratch(deckId: Int)

    // --- T-802: レイテンシ計測 ---
    external fun nativeGetLatencyMs(): Double
    external fun nativeGetXRunCount(): Int
}
