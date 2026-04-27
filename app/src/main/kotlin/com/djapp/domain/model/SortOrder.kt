package com.djapp.domain.model

/** ライブラリの並び替え順。UC-008 代替フロー A2 対応。 */
enum class SortOrder {
    TITLE,       // 曲名順（昇順）
    ARTIST,      // アーティスト名順（昇順）
    DATE_ADDED,  // 追加日順（新しい順）
}
