package com.djapp.domain.model

/**
 * ドメインモデル: デッキの状態。MVI の State として使用する。
 * UC-001〜007 対応。
 */
data class DeckState(
    val track          : Track?   = null,
    val isPlaying      : Boolean  = false,
    val playheadSec    : Float    = 0f,
    val bpm            : Float?   = null,        // null = 未検出
    val bpmMultiplier  : Float    = 1.0f,        // 表示BPM補正: 0.5(÷2) / 1.0 / 2.0(×2)
    val pitchRatio     : Float    = 1.0f,        // 1.0 = 等速（±16% = 0.84〜1.16）
    val isSynced       : Boolean  = false,
    val eq             : EqState  = EqState(),
    val loop             : LoopState? = null,    // null = ループなし
    val pendingLoopInSec : Float?   = null,      // IN点タップ済み、OUTタップ待ち
    val isScratching     : Boolean  = false,
)

/** 3バンドEQの状態（dB値）。0.0 = デフォルト（変化なし）。 */
data class EqState(
    val lowDb  : Float = 0f,
    val midDb  : Float = 0f,
    val highDb : Float = 0f,
)

/** ループ区間の状態（秒単位）。UI層での表示・WaveformView でのハイライトに使用。 */
data class LoopState(
    val loopInSec  : Float,
    val loopOutSec : Float,
)
