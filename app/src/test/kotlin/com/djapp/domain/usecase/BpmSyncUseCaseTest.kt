package com.djapp.domain.usecase

import com.djapp.domain.model.DeckId
import com.djapp.domain.repository.AudioEngineRepository
import com.djapp.domain.repository.BpmCacheRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

/**
 * T-408: BpmSyncUseCase 単体テスト（AC-004 対応）
 */
class BpmSyncUseCaseTest {

    private lateinit var audioEngine: AudioEngineRepository
    private lateinit var bpmCache: BpmCacheRepository
    private lateinit var useCase: BpmSyncUseCase

    @Before
    fun setUp() {
        audioEngine = mockk(relaxed = true)
        bpmCache    = mockk(relaxed = true)
        useCase     = BpmSyncUseCase(audioEngine, bpmCache)
    }

    /** AC-004-01: BPM取得がエンジンに委譲される */
    @Test
    fun `getBpm — エンジンから BPM を取得する`() {
        every { audioEngine.getBpm(DeckId.A) } returns 128f
        assertEquals(128f, useCase.getBpm(DeckId.A), 0.001f)
    }

    /** AC-004-06: ピッチ +32% を超えない */
    @Test
    fun `setPitchRatio 1_32 — 上限に収まる`() {
        val result = useCase.setPitchRatio(DeckId.A, 1.32f)
        assertEquals(1.32f, result, 0.001f)
        verify { audioEngine.setPitchRatio(DeckId.A, 1.32f) }
    }

    /** AC-004-06: ピッチ -32% を下回らない */
    @Test
    fun `setPitchRatio 0_68 — 下限に収まる`() {
        val result = useCase.setPitchRatio(DeckId.A, 0.68f)
        assertEquals(0.68f, result, 0.001f)
        verify { audioEngine.setPitchRatio(DeckId.A, 0.68f) }
    }

    /** AC-004-06: 上限超えはクランプされる */
    @Test
    fun `setPitchRatio 上限超え — 1_32 にクランプ`() {
        val result = useCase.setPitchRatio(DeckId.A, 1.5f)
        assertEquals(1.32f, result, 0.001f)
        verify { audioEngine.setPitchRatio(DeckId.A, 1.32f) }
    }

    /** AC-004-06: 下限未満はクランプされる */
    @Test
    fun `setPitchRatio 下限未満 — 0_68 にクランプ`() {
        val result = useCase.setPitchRatio(DeckId.A, 0.5f)
        assertEquals(0.68f, result, 0.001f)
        verify { audioEngine.setPitchRatio(DeckId.A, 0.68f) }
    }

    /** AC-004-02: Sync有効化 — デッキ B が A の BPM に追従するピッチを返す */
    @Test
    fun `syncTo — デッキ B のピッチを A の BPM に合わせて返す`() {
        every { audioEngine.getBpm(DeckId.A) } returns 128f
        every { audioEngine.getBpm(DeckId.B) } returns 140f
        val result   = useCase.syncTo(DeckId.B, DeckId.A, masterPitchRatio = 1.0f)
        val expected = (128f / 140f).coerceIn(0.68f, 1.32f)
        assertEquals(expected, result!!, 0.001f)
        verify { audioEngine.setPitchRatio(DeckId.B, expected) }
    }

    /** AC-004-03: Sync中のBPM追従 — マスターのピッチ変化にスレーブが追従する */
    @Test
    fun `syncTo — マスターピッチ変化後も正しいスレーブピッチを返す`() {
        every { audioEngine.getBpm(DeckId.A) } returns 128f
        every { audioEngine.getBpm(DeckId.B) } returns 128f
        val masterPitch = 132f / 128f
        val result = useCase.syncTo(DeckId.B, DeckId.A, masterPitchRatio = masterPitch)
        assertEquals(masterPitch, result!!, 0.001f)
    }

    /** AC-004-05: マスターBPM未検出 → null を返す */
    @Test
    fun `syncTo — マスター BPM 未検出なら null を返す`() {
        every { audioEngine.getBpm(DeckId.A) } returns 0f
        every { audioEngine.getBpm(DeckId.B) } returns 128f
        assertNull(useCase.syncTo(DeckId.B, DeckId.A, masterPitchRatio = 1.0f))
    }

    /** AC-004-05: スレーブBPM未検出 → null を返す */
    @Test
    fun `syncTo — スレーブ BPM 未検出なら null を返す`() {
        every { audioEngine.getBpm(DeckId.A) } returns 128f
        every { audioEngine.getBpm(DeckId.B) } returns 0f
        assertNull(useCase.syncTo(DeckId.B, DeckId.A, masterPitchRatio = 1.0f))
    }

    /** Sync結果がクランプされる（+32% 上限）*/
    @Test
    fun `syncTo — 結果が上限をクランプする`() {
        every { audioEngine.getBpm(DeckId.A) } returns 200f
        every { audioEngine.getBpm(DeckId.B) } returns 100f
        val result = useCase.syncTo(DeckId.B, DeckId.A, masterPitchRatio = 1.0f)
        assertEquals(1.32f, result!!, 0.001f)
    }

    /** bpmMultiplier を考慮した Sync — A:128×1.0 / B:64×2.0 → pitchRatio=1.0 */
    @Test
    fun `syncTo — multiplier を考慮してピッチを算出する`() {
        every { audioEngine.getBpm(DeckId.A) } returns 128f
        every { audioEngine.getBpm(DeckId.B) } returns 64f
        // B の multiplier=2.0 で実効BPM=128 → A と同じなので ratio=1.0
        val result = useCase.syncTo(
            slaveDeckId          = DeckId.B,
            masterDeckId         = DeckId.A,
            masterPitchRatio     = 1.0f,
            masterBpmMultiplier  = 1.0f,
            slaveBpmMultiplier   = 2.0f,
        )
        assertEquals(1.0f, result!!, 0.001f)
    }

    /** getCachedBpm — キャッシュから BPM を返す */
    @Test
    fun `getCachedBpm — キャッシュにあれば値を返す`() = kotlinx.coroutines.test.runTest {
        coEvery { bpmCache.get("path/to/track.mp3") } returns 128f
        val result = useCase.getCachedBpm("path/to/track.mp3")
        assertEquals(128f, result!!, 0.001f)
    }

    /** getCachedBpm — キャッシュになければ null を返す */
    @Test
    fun `getCachedBpm — キャッシュになければ null を返す`() = kotlinx.coroutines.test.runTest {
        coEvery { bpmCache.get("path/to/track.mp3") } returns null
        assertNull(useCase.getCachedBpm("path/to/track.mp3"))
    }
}
