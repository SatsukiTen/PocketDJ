package com.djapp.presentation.deck

import com.djapp.domain.model.DeckId
import com.djapp.domain.model.DeckState
import com.djapp.domain.model.Track
import com.djapp.domain.usecase.BpmSyncUseCase
import com.djapp.domain.usecase.EqualizerUseCase
import com.djapp.domain.usecase.LoadTrackUseCase
import com.djapp.domain.usecase.LoopUseCase
import com.djapp.domain.usecase.PlaybackControlUseCase
import com.djapp.domain.usecase.ScratchUseCase
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeckViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var loadTrackUseCase: LoadTrackUseCase
    private lateinit var playbackControlUseCase: PlaybackControlUseCase
    private lateinit var bpmSyncUseCase: BpmSyncUseCase
    private lateinit var equalizerUseCase: EqualizerUseCase
    private lateinit var loopUseCase: LoopUseCase
    private lateinit var scratchUseCase: ScratchUseCase
    private lateinit var viewModel: DeckViewModel

    private val trackA = Track(
        id = 1L, title = "Track A", artist = "Artist A",
        album = "Album", durationMs = 240_000L,
        filePath = "/music/track_a.mp3",
    )
    private val trackB = Track(
        id = 2L, title = "Track B", artist = "Artist B",
        album = "Album", durationMs = 240_000L,
        filePath = "/music/track_b.mp3",
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        loadTrackUseCase     = mockk(relaxed = true)
        playbackControlUseCase = mockk(relaxed = true)
        bpmSyncUseCase       = mockk(relaxed = true)
        equalizerUseCase     = mockk(relaxed = true)
        loopUseCase          = mockk(relaxed = true)
        scratchUseCase       = mockk(relaxed = true)

        coEvery { loadTrackUseCase(any(), any()) } returns true
        every  { bpmSyncUseCase.getBpm(any())   } returns 128f
        coEvery { bpmSyncUseCase.getCachedBpm(any()) } returns null

        viewModel = DeckViewModel(loadTrackUseCase, playbackControlUseCase, bpmSyncUseCase, equalizerUseCase, loopUseCase, scratchUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** 新しいトラックをロードすると bpmMultiplier が 1.0f にリセットされる */
    @Test
    fun `loadTrack — bpmMultiplier が 1_0f にリセットされる`() = runTest {
        // デッキ B に最初のトラックをロードし multiplier を変える
        viewModel.onIntent(DeckIntent.LoadTrack(DeckId.B, trackA))
        advanceUntilIdle()
        viewModel.onIntent(DeckIntent.AdjustBpmMultiplier(DeckId.B, 0.5f))

        assertEquals(0.5f, viewModel.uiState.value.deckB.bpmMultiplier, 0.001f)

        // 別のトラックをロード → multiplier がリセットされるはず
        viewModel.onIntent(DeckIntent.LoadTrack(DeckId.B, trackB))
        advanceUntilIdle()

        assertEquals(1.0f, viewModel.uiState.value.deckB.bpmMultiplier, 0.001f)
    }

    /** ロード成功後に bpm が State に反映される */
    @Test
    fun `loadTrack — ロード成功後に bpm が State に反映される`() = runTest {
        every { bpmSyncUseCase.getBpm(DeckId.A) } returns 140f

        viewModel.onIntent(DeckIntent.LoadTrack(DeckId.A, trackA))
        advanceUntilIdle()

        assertEquals(140f, viewModel.uiState.value.deckA.bpm!!, 0.001f)
    }

    /** キャッシュ済みBPMがあれば即座に表示される */
    @Test
    fun `loadTrack — キャッシュ済み BPM は即座に State に反映される`() = runTest {
        coEvery { bpmSyncUseCase.getCachedBpm(trackA.filePath) } returns 128f

        viewModel.onIntent(DeckIntent.LoadTrack(DeckId.A, trackA))
        advanceUntilIdle()

        assertEquals(128f, viewModel.uiState.value.deckA.bpm!!, 0.001f)
    }

    /** ロード失敗時は errorMessage が設定される */
    @Test
    fun `loadTrack — 失敗時に errorMessage が設定される`() = runTest {
        coEvery { loadTrackUseCase(DeckId.A, trackA) } returns false

        viewModel.onIntent(DeckIntent.LoadTrack(DeckId.A, trackA))
        advanceUntilIdle()

        assertEquals("「${trackA.title}」のロードに失敗しました",
            viewModel.uiState.value.errorMessage)
    }

    /** デッキAのmultiplierを×2にしてもBの同期ピッチが即座に更新される */
    @Test
    fun `adjustBpmMultiplier — デッキA ×2 時にBの同期ピッチが即再計算される`() = runTest {
        every { bpmSyncUseCase.getBpm(DeckId.A) } returns 128f
        every { bpmSyncUseCase.getBpm(DeckId.B) } returns 128f
        // 同BPM同士のSYNC → pitchB = 1.0
        every { bpmSyncUseCase.syncTo(any(), any(), any(), any(), any()) } returns 1.0f
        viewModel.onIntent(DeckIntent.ToggleSync)
        assertEquals(true,  viewModel.uiState.value.deckB.isSynced)
        assertEquals(1.0f,  viewModel.uiState.value.deckB.pitchRatio, 0.001f)

        // デッキAを×2 → masterBpm が 256 になるため syncTo が呼ばれ即座に再計算
        every { bpmSyncUseCase.syncTo(any(), any(), any(), any(), any()) } returns 1.32f
        viewModel.onIntent(DeckIntent.AdjustBpmMultiplier(DeckId.A, 2.0f))

        // Bはまだ isSynced=true のまま、pitchRatioが新しい値に更新されている
        assertEquals(true,  viewModel.uiState.value.deckB.isSynced)
        assertEquals(1.32f, viewModel.uiState.value.deckB.pitchRatio, 0.001f)
    }

    /** デッキBのmultiplier変更はsync解除（意図的な手動操作）*/
    @Test
    fun `adjustBpmMultiplier — デッキB 変更時は isSynced が false になる`() = runTest {
        every { bpmSyncUseCase.getBpm(DeckId.A) } returns 128f
        every { bpmSyncUseCase.getBpm(DeckId.B) } returns 128f
        every { bpmSyncUseCase.syncTo(any(), any(), any(), any(), any()) } returns 1.0f
        viewModel.onIntent(DeckIntent.ToggleSync)
        assertEquals(true, viewModel.uiState.value.deckB.isSynced)

        viewModel.onIntent(DeckIntent.AdjustBpmMultiplier(DeckId.B, 2.0f))

        assertEquals(false, viewModel.uiState.value.deckB.isSynced)
    }

    /** isSynced は新しいトラックロード時にリセットされる */
    @Test
    fun `loadTrack — isSynced がリセットされる`() = runTest {
        // SYNC を有効にする
        every { bpmSyncUseCase.getBpm(DeckId.A) } returns 128f
        every { bpmSyncUseCase.getBpm(DeckId.B) } returns 128f
        every { bpmSyncUseCase.syncTo(any(), any(), any(), any(), any()) } returns 1.0f

        viewModel.onIntent(DeckIntent.LoadTrack(DeckId.A, trackA))
        viewModel.onIntent(DeckIntent.LoadTrack(DeckId.B, trackB))
        advanceUntilIdle()
        viewModel.onIntent(DeckIntent.ToggleSync)

        assertEquals(true, viewModel.uiState.value.deckB.isSynced)

        // B に新しいトラックをロード → isSynced がリセットされるはず
        viewModel.onIntent(DeckIntent.LoadTrack(DeckId.B, trackA))
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.deckB.isSynced)
    }
}
