package com.djapp.domain.usecase

import com.djapp.domain.model.CrossfaderCurve
import com.djapp.domain.repository.AudioEngineRepository
import com.djapp.domain.repository.SettingsRepository
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * T-308: CrossfaderUseCase 単体テスト（AC-003 対応）
 */
class CrossfaderUseCaseTest {

    private lateinit var audioEngine: AudioEngineRepository
    private lateinit var settings: SettingsRepository
    private lateinit var useCase: CrossfaderUseCase

    @Before
    fun setUp() {
        audioEngine = mockk(relaxed = true)
        settings = mockk(relaxed = true)
        every { settings.getCrossfaderCurve() } returns flowOf(CrossfaderCurve.EQUAL_POWER)
        useCase = CrossfaderUseCase(audioEngine, settings)
    }

    /** AC-003-01: 左端（0.0）を設定する */
    @Test
    fun `setPosition 0f — エンジンに 0f を渡す`() {
        useCase.setPosition(0f)
        verify { audioEngine.setCrossfaderPosition(0f) }
    }

    /** AC-003-02: 右端（1.0）を設定する */
    @Test
    fun `setPosition 1f — エンジンに 1f を渡す`() {
        useCase.setPosition(1f)
        verify { audioEngine.setCrossfaderPosition(1f) }
    }

    /** AC-003-03: 中央（0.5）を設定する */
    @Test
    fun `setPosition 0_5f — エンジンに 0_5f を渡す`() {
        useCase.setPosition(0.5f)
        verify { audioEngine.setCrossfaderPosition(0.5f) }
    }

    /** AC-003-05: ダブルタップリセット — 位置を 0.5 に戻す */
    @Test
    fun `resetToCenter — エンジンに 0_5f を渡す`() {
        useCase.resetToCenter()
        verify { audioEngine.setCrossfaderPosition(0.5f) }
    }

    /** AC-003-06: Linear カーブへの切替がエンジンと設定に反映される */
    @Test
    fun `setCurve LINEAR — AudioEngine と Settings の両方に反映する`() = runTest {
        useCase.setCurve(CrossfaderCurve.LINEAR)
        verify { audioEngine.setCrossfaderCurve(CrossfaderCurve.LINEAR) }
        coVerify { settings.setCrossfaderCurve(CrossfaderCurve.LINEAR) }
    }

    /** AC-003-07 / AC-003-08: Equal Power カーブへの切替がエンジンと設定に反映される */
    @Test
    fun `setCurve EQUAL_POWER — AudioEngine と Settings の両方に反映する`() = runTest {
        useCase.setCurve(CrossfaderCurve.EQUAL_POWER)
        verify { audioEngine.setCrossfaderCurve(CrossfaderCurve.EQUAL_POWER) }
        coVerify { settings.setCrossfaderCurve(CrossfaderCurve.EQUAL_POWER) }
    }

    /** 範囲外の値は 0.0 にクランプされる */
    @Test
    fun `setPosition 負値 — 0f にクランプして渡す`() {
        useCase.setPosition(-0.3f)
        verify { audioEngine.setCrossfaderPosition(0f) }
    }

    /** 範囲外の値は 1.0 にクランプされる */
    @Test
    fun `setPosition 1超 — 1f にクランプして渡す`() {
        useCase.setPosition(1.5f)
        verify { audioEngine.setCrossfaderPosition(1f) }
    }

    /** getCurveFlow は SettingsRepository の Flow をそのまま返す */
    @Test
    fun `getCurveFlow — Settings の Flow を返す`() {
        val flow = flowOf(CrossfaderCurve.LINEAR)
        every { settings.getCrossfaderCurve() } returns flow
        val result = useCase.getCurveFlow()
        assert(result === flow)
    }

    /** syncCurve はエンジンにカーブを適用する（永続化なし） */
    @Test
    fun `syncCurve — エンジンにのみ適用し Settings には書き込まない`() {
        useCase.syncCurve(CrossfaderCurve.LINEAR)
        verify { audioEngine.setCrossfaderCurve(CrossfaderCurve.LINEAR) }
        coVerify(exactly = 0) { settings.setCrossfaderCurve(any()) }
    }
}
