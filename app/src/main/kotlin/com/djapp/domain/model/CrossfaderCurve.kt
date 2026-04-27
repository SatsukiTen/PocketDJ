package com.djapp.domain.model

/** クロスフェーダーのカーブ種別。UC-003 / AC-003-06〜08 対応。 */
enum class CrossfaderCurve {
    /** スライダー移動量に比例して音量変化。スクラッチ・カット向き。 */
    LINEAR,
    /** 中央付近でゆっくり、両端で急変。なめらかなミックス向き（デフォルト）。 */
    EQUAL_POWER,
}
