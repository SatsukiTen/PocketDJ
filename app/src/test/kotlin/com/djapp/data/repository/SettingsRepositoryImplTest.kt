package com.djapp.data.repository

import app.cash.turbine.test
import com.djapp.data.source.SettingsDataSource
import com.djapp.domain.model.CrossfaderCurve
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * T-007: SettingsRepositoryImpl の単体テスト。
 * DataSourceをモック化し、CrossfaderCurveの変換ロジックを検証する。
 */
class SettingsRepositoryImplTest {

    private lateinit var dataSource: SettingsDataSource
    private lateinit var repository: SettingsRepositoryImpl

    @Before
    fun setUp() {
        dataSource = mockk(relaxed = true)
        repository = SettingsRepositoryImpl(dataSource)
    }

    @Test
    fun `getCrossfaderCurve returns EQUAL_POWER by default`() = runTest {
        every { dataSource.getCrossfaderCurve() } returns flowOf("EQUAL_POWER")

        repository.getCrossfaderCurve().test {
            assertEquals(CrossfaderCurve.EQUAL_POWER, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getCrossfaderCurve returns LINEAR when stored`() = runTest {
        every { dataSource.getCrossfaderCurve() } returns flowOf("LINEAR")

        repository.getCrossfaderCurve().test {
            assertEquals(CrossfaderCurve.LINEAR, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `getCrossfaderCurve falls back to EQUAL_POWER for unknown value`() = runTest {
        every { dataSource.getCrossfaderCurve() } returns flowOf("UNKNOWN_VALUE")

        repository.getCrossfaderCurve().test {
            assertEquals(CrossfaderCurve.EQUAL_POWER, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `setCrossfaderCurve stores curve name string`() = runTest {
        repository.setCrossfaderCurve(CrossfaderCurve.LINEAR)

        coVerify { dataSource.setCrossfaderCurve("LINEAR") }
    }

    @Test
    fun `setCrossfaderCurve stores EQUAL_POWER as string`() = runTest {
        repository.setCrossfaderCurve(CrossfaderCurve.EQUAL_POWER)

        coVerify { dataSource.setCrossfaderCurve("EQUAL_POWER") }
    }
}
