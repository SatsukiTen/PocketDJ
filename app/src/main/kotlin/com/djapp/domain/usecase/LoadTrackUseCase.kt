package com.djapp.domain.usecase

import com.djapp.domain.model.DeckId
import com.djapp.domain.model.Track
import com.djapp.domain.repository.AudioEngineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * T-205: UC-001 トラックをデッキにロードする。
 * ファイルパスを C++ 音声エンジンに渡してデコードを開始する。
 *
 * AC-001-01: ロードに成功すると true を返す
 * AC-001-02: 存在しないパスの場合 false を返す
 */
class LoadTrackUseCase @Inject constructor(
    private val audioEngineRepository: AudioEngineRepository,
) {
    /**
     * @param deckId   ロード先デッキ
     * @param track    ライブラリから選択したトラック
     * @return         ロード成功なら true
     */
    suspend operator fun invoke(deckId: DeckId, track: Track): Boolean =
        withContext(Dispatchers.IO) {
            val path = track.filePath
            if (path.isBlank()) return@withContext false
            audioEngineRepository.loadTrack(deckId, path)
        }
}
