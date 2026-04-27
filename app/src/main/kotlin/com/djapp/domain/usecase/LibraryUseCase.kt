package com.djapp.domain.usecase

import com.djapp.domain.model.SortOrder
import com.djapp.domain.model.Track
import com.djapp.domain.repository.TrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * T-102: ライブラリ操作のユースケース。UC-008 対応。
 * 一覧取得・検索・ソートのロジックをDomain層に集約する。
 */
class LibraryUseCase @Inject constructor(
    private val trackRepository: TrackRepository,
) {
    /**
     * 全トラックを取得してソートして返す。
     * UC-008 基本フロー、代替フロー A2 対応。
     */
    fun getAllTracks(sortOrder: SortOrder = SortOrder.TITLE): Flow<List<Track>> =
        trackRepository.getAllTracks().map { tracks ->
            tracks.sortedWith(sortOrder.comparator())
        }

    /**
     * クエリで曲名・アーティスト名を前方一致検索してソートして返す。
     * blank の場合は全件取得と同じ挙動にする。UC-008 代替フロー A1 対応。
     */
    fun searchTracks(query: String, sortOrder: SortOrder = SortOrder.TITLE): Flow<List<Track>> =
        if (query.isBlank()) {
            getAllTracks(sortOrder)
        } else {
            trackRepository.searchTracks(query).map { tracks ->
                tracks.sortedWith(sortOrder.comparator())
            }
        }
}

private fun SortOrder.comparator(): Comparator<Track> = when (this) {
    SortOrder.TITLE      -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title }
    SortOrder.ARTIST     -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.artist }
    SortOrder.DATE_ADDED -> compareByDescending { it.id }  // MediaStore id は追加順と相関
}
