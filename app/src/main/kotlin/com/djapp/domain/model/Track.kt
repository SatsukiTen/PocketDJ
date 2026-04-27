package com.djapp.domain.model

/**
 * ドメインモデル: トラック（端末内の音楽ファイル1曲）。
 * UC-001 / UC-008 対応。
 */
data class Track(
    val id         : Long,
    val title      : String,
    val artist     : String,
    val album      : String,
    val durationMs : Long,
    val filePath   : String,
)
