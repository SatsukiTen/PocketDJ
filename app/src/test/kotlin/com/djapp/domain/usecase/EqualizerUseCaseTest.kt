package com.djapp.domain.usecase

import com.djapp.domain.model.DeckId
import com.djapp.domain.repository.AudioEngineRepository
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class EqualizerUseCaseTest {

    private lateinit var audioEngine: AudioEngineRepository
    private lateinit var useCase: EqualizerUseCase

    @Before
    fun setUp() {
        audioEngine = mockk(relaxed = true)
        useCase = EqualizerUseCase(audioEngine)
    }

    /** AC-005-01: Low 正常値がエンジンに渡る */
    @Test
    fun `setLow — 正常値をエンジンに渡す`() {
        useCase.setLow(DeckId.A, -12f)
        verify { audioEngine.setEqLow(DeckId.A, -12f) }
    }

    /** AC-005-01: Low Kill（-60dB）がエンジンに渡る */
    @Test
    fun `setLow — Kill(-60dB)をエンジンに渡す`() {
        useCase.setLow(DeckId.A, -60f)
        verify { audioEngine.setEqLow(DeckId.A, -60f) }
    }

    /** AC-005-01: Low -60dB 未満はクランプされる */
    @Test
    fun `setLow — 下限未満は -60dB にクランプ`() {
        useCase.setLow(DeckId.A, -100f)
        verify { audioEngine.setEqLow(DeckId.A, -60f) }
    }

    /** AC-005: High +6dB 超はクランプされる */
    @Test
    fun `setHigh — 上限超えは 6dB にクランプ`() {
        useCase.setHigh(DeckId.B, 12f)
        verify { audioEngine.setEqHigh(DeckId.B, 6f) }
    }

    /** AC-005-02: ダブルタップ相当（0dB リセット）がエンジンに渡る */
    @Test
    fun `setHigh — 0dB リセットがエンジンに渡る`() {
        useCase.setHigh(DeckId.A, 0f)
        verify { audioEngine.setEqHigh(DeckId.A, 0f) }
    }

    /** AC-005-03: デッキ独立性 — A と B に別々のゲインが渡る */
    @Test
    fun `setLow — デッキ A と B が独立している`() {
        useCase.setLow(DeckId.A, -60f)
        useCase.setLow(DeckId.B, 0f)
        verify { audioEngine.setEqLow(DeckId.A, -60f) }
        verify { audioEngine.setEqLow(DeckId.B,   0f) }
    }

    /** Mid 正常値がエンジンに渡る */
    @Test
    fun `setMid — 正常値をエンジンに渡す`() {
        useCase.setMid(DeckId.B, 3f)
        verify { audioEngine.setEqMid(DeckId.B, 3f) }
    }
}
