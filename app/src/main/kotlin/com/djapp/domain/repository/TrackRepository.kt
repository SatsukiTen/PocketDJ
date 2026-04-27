package com.djapp.domain.repository

import com.djapp.domain.model.Track
import kotlinx.coroutines.flow.Flow

/**
 * 端末内の音楽ファイル取得インターフェース。
 * UC-008 対応。
 */
interface TrackRepository {
    /** 端末内の全トラック一覧をFlowで返す。 */
    fun getAllTracks(): Flow<List<Track>>

    /** クエリ文字列で曲名・アーティスト名を前方一致検索する。 */
    fun searchTracks(query: String): Flow<List<Track>>
}
