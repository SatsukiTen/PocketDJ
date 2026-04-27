package com.djapp.domain.usecase

import com.djapp.domain.model.DeckId
import com.djapp.domain.model.Track
import com.djapp.domain.repository.AudioEngineRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * T-211: LoadTrackUseCase 単体テスト（AC-001 対応）
 */
class LoadTrackUseCaseTest {

    private lateinit var repository: AudioEngineRepository
    private lateinit var useCase: LoadTrackUseCase

    private val sampleTrack = Track(
        id = 1L,
        title = "Test Track",
        artist = "Test Artist",
        album = "Test Album",
        durationMs = 180_000L,
        filePath = "/storage/emulated/0/Music/test.mp3",
    )

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = LoadTrackUseCase(repository)
    }

    /** AC-001-01: ロードに成功すると true を返す */
    @Test
    fun `正常パス — ロードに成功すると true を返す`() = runTest {
        every { repository.loadTrack(DeckId.A, sampleTrack.filePath) } returns true

        val result = useCase(DeckId.A, sampleTrack)

        assertTrue(result)
        verify { repository.loadTrack(DeckId.A, sampleTrack.filePath) }
    }

    /** AC-001-02: 存在しないパス（エンジンが false を返す）の場合 false を返す */
    @Test
    fun `代替フロー — エンジンが false を返すと false を返す`() = runTest {
        every { repository.loadTrack(DeckId.A, sampleTrack.filePath) } returns false

        val result = useCase(DeckId.A, sampleTrack)

        assertFalse(result)
    }

    /** filePath が空の場合はリポジトリを呼ばずに false を返す */
    @Test
    fun `空パス — リポジトリを呼ばずに false を返す`() = runTest {
        val emptyPathTrack = sampleTrack.copy(filePath = "")

        val result = useCase(DeckId.A, emptyPathTrack)

        assertFalse(result)
        verify(exactly = 0) { repository.loadTrack(any(), any()) }
    }

    /** デッキ B にもロードできる */
    @Test
    fun `デッキB — DeckId_B にロードできる`() = runTest {
        every { repository.loadTrack(DeckId.B, sampleTrack.filePath) } returns true

        val result = useCase(DeckId.B, sampleTrack)

        assertTrue(result)
        verify { repository.loadTrack(DeckId.B, sampleTrack.filePath) }
    }
}
