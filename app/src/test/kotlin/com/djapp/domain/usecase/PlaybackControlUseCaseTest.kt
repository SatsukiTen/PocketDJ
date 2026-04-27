package com.djapp.domain.usecase

import com.djapp.domain.model.DeckId
import com.djapp.domain.repository.AudioEngineRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * T-211: PlaybackControlUseCase 単体テスト（AC-002 対応）
 */
class PlaybackControlUseCaseTest {

    private lateinit var repository: AudioEngineRepository
    private lateinit var useCase: PlaybackControlUseCase

    @Before
    fun setUp() {
        repository = mockk(relaxed = true)
        useCase = PlaybackControlUseCase(repository)
    }

    /** AC-002-01: play() が AudioEngineRepository.play() を呼ぶ */
    @Test
    fun `play — リポジトリの play を呼ぶ`() {
        useCase.play(DeckId.A)
        verify { repository.play(DeckId.A) }
    }

    /** AC-002-02: pause() が AudioEngineRepository.pause() を呼ぶ */
    @Test
    fun `pause — リポジトリの pause を呼ぶ`() {
        useCase.pause(DeckId.A)
        verify { repository.pause(DeckId.A) }
    }

    /** AC-002-03: stop() が AudioEngineRepository.stop() を呼ぶ */
    @Test
    fun `stop — リポジトリの stop を呼ぶ`() {
        useCase.stop(DeckId.A)
        verify { repository.stop(DeckId.A) }
    }

    /** デッキ B の play も正しく委譲される */
    @Test
    fun `デッキB — play を DeckId_B で委譲する`() {
        useCase.play(DeckId.B)
        verify { repository.play(DeckId.B) }
    }

    /** getPlayheadSec がリポジトリの値を返す */
    @Test
    fun `getPlayheadSec — リポジトリから再生位置を返す`() {
        every { repository.getPlayheadSec(DeckId.A) } returns 12.5f

        val result = useCase.getPlayheadSec(DeckId.A)

        assertEquals(12.5f, result, 0.001f)
    }

    /** getDurationSec がリポジトリの値を返す */
    @Test
    fun `getDurationSec — リポジトリからトラック長を返す`() {
        every { repository.getDurationSec(DeckId.B) } returns 240.0f

        val result = useCase.getDurationSec(DeckId.B)

        assertEquals(240.0f, result, 0.001f)
    }
}
