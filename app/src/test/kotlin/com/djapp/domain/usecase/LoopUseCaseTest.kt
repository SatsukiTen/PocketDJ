package com.djapp.domain.usecase

import com.djapp.domain.model.DeckId
import com.djapp.domain.model.LoopState
import com.djapp.domain.repository.AudioEngineRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LoopUseCaseTest {

    private lateinit var audioEngine: AudioEngineRepository
    private lateinit var useCase: LoopUseCase

    @Before
    fun setUp() {
        audioEngine = mockk(relaxed = true)
        every { audioEngine.getSampleRate(any()) } returns 48000
        useCase = LoopUseCase(audioEngine)
    }

    /** AC-006-01: IN/OUT 秒位置から正しいサンプル範囲で setLoop が呼ばれる */
    @Test
    fun `activate — 正しいサンプル範囲でsetLoopを呼ぶ`() {
        // loopIn=10s, loopOut=12s @ 48000Hz → samples: 480000, 576000
        val result = useCase.activate(DeckId.A, 10.0f, 12.0f)
        verify { audioEngine.setLoop(DeckId.A, 480000L, 576000L) }
        assertEquals(10.0f, result.loopInSec,  0.001f)
        assertEquals(12.0f, result.loopOutSec, 0.001f)
    }

    /** AC-006-02: ×2 でループ長が2倍になる（IN点固定）*/
    @Test
    fun `doubleLength — ループ長を2倍にする`() {
        // 10s-12s (2s) → 10s-14s (4s)
        val result = useCase.doubleLength(DeckId.A, LoopState(10.0f, 12.0f))
        verify { audioEngine.setLoop(DeckId.A, 480000L, 672000L) }
        assertEquals(10.0f, result.loopInSec,  0.001f)
        assertEquals(14.0f, result.loopOutSec, 0.001f)
    }

    /** AC-006-03: ÷½ でループ長が半分になる（IN点固定）*/
    @Test
    fun `halveLength — ループ長を半分にする`() {
        // 10s-12s (2s) → 10s-11s (1s)
        val result = useCase.halveLength(DeckId.A, LoopState(10.0f, 12.0f))
        verify { audioEngine.setLoop(DeckId.A, 480000L, 528000L) }
        assertEquals(10.0f, result.loopInSec,  0.001f)
        assertEquals(11.0f, result.loopOutSec, 0.001f)
    }

    /** AC-006-04: ÷½ は最小50msでクランプされる */
    @Test
    fun `halveLength — 最小50msでクランプされる`() {
        // 0.06s → 0.03s → clamp to 0.05s
        val result = useCase.halveLength(DeckId.A, LoopState(10.0f, 10.06f))
        val expectedOutSample = ((10.0f + LoopUseCase.MIN_LOOP_SEC) * 48000).toLong()
        verify { audioEngine.setLoop(DeckId.A, 480000L, expectedOutSample) }
        assertEquals(LoopUseCase.MIN_LOOP_SEC, result.loopOutSec - result.loopInSec, 0.001f)
    }

    /** AC-006-05: deactivate — clearLoop が呼ばれる */
    @Test
    fun `deactivate — clearLoopを呼ぶ`() {
        useCase.deactivate(DeckId.A)
        verify { audioEngine.clearLoop(DeckId.A) }
    }

    /** デッキ独立性 — デッキBに正しく設定される */
    @Test
    fun `activate — デッキBに正しく設定される`() {
        // 0s-0.5s @ 48000Hz → 0, 24000
        useCase.activate(DeckId.B, 0.0f, 0.5f)
        verify { audioEngine.setLoop(DeckId.B, 0L, 24000L) }
    }
}
