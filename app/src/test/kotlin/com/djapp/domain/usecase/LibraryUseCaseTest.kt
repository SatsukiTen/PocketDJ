package com.djapp.domain.usecase

import app.cash.turbine.test
import com.djapp.domain.model.SortOrder
import com.djapp.domain.model.Track
import com.djapp.domain.repository.TrackRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * T-107: LibraryUseCase の単体テスト。
 * UC-008 / AC-001-02 対応。TrackRepositoryをモック化して検証する。
 */
class LibraryUseCaseTest {

    private lateinit var repository: TrackRepository
    private lateinit var useCase: LibraryUseCase

    private val trackA = Track(1L, "Alpha Song",   "Artist Z", "Album", 180_000L, "/a.mp3")
    private val trackB = Track(2L, "Beta Song",    "Artist A", "Album", 200_000L, "/b.mp3")
    private val trackC = Track(3L, "Gamma Song",   "Artist M", "Album", 150_000L, "/c.mp3")

    @Before
    fun setUp() {
        repository = mockk()
        useCase    = LibraryUseCase(repository)
    }

    // --- getAllTracks ---

    @Test
    fun `getAllTracks returns tracks sorted by title ascending`() = runTest {
        every { repository.getAllTracks() } returns flowOf(listOf(trackB, trackA, trackC))

        useCase.getAllTracks(SortOrder.TITLE).test {
            val result = awaitItem()
            assertEquals(listOf("Alpha Song", "Beta Song", "Gamma Song"), result.map { it.title })
            awaitComplete()
        }
    }

    @Test
    fun `getAllTracks returns tracks sorted by artist ascending`() = runTest {
        every { repository.getAllTracks() } returns flowOf(listOf(trackA, trackB, trackC))

        useCase.getAllTracks(SortOrder.ARTIST).test {
            val result = awaitItem()
            assertEquals(listOf("Artist A", "Artist M", "Artist Z"), result.map { it.artist })
            awaitComplete()
        }
    }

    @Test
    fun `getAllTracks returns tracks sorted by date added descending`() = runTest {
        every { repository.getAllTracks() } returns flowOf(listOf(trackA, trackB, trackC))

        useCase.getAllTracks(SortOrder.DATE_ADDED).test {
            val result = awaitItem()
            // id降順（新しい順）: 3, 2, 1
            assertEquals(listOf(3L, 2L, 1L), result.map { it.id })
            awaitComplete()
        }
    }

    @Test
    fun `getAllTracks returns empty list when no tracks`() = runTest {
        every { repository.getAllTracks() } returns flowOf(emptyList())

        useCase.getAllTracks().test {
            assertEquals(emptyList<Track>(), awaitItem())
            awaitComplete()
        }
    }

    // --- searchTracks ---

    @Test
    fun `searchTracks filters by title prefix and sorts results`() = runTest {
        every { repository.searchTracks("Al") } returns flowOf(listOf(trackA))

        useCase.searchTracks("Al", SortOrder.TITLE).test {
            val result = awaitItem()
            assertEquals(1, result.size)
            assertEquals("Alpha Song", result[0].title)
            awaitComplete()
        }
    }

    @Test
    fun `searchTracks with blank query returns all tracks`() = runTest {
        every { repository.getAllTracks() } returns flowOf(listOf(trackA, trackB))

        useCase.searchTracks("", SortOrder.TITLE).test {
            assertEquals(2, awaitItem().size)
            awaitComplete()
        }
    }

    @Test
    fun `searchTracks returns empty when no match`() = runTest {
        every { repository.searchTracks("XYZ") } returns flowOf(emptyList())

        useCase.searchTracks("XYZ").test {
            assertEquals(emptyList<Track>(), awaitItem())
            awaitComplete()
        }
    }
}
