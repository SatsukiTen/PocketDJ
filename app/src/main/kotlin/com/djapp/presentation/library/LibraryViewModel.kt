package com.djapp.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djapp.domain.model.SortOrder
import com.djapp.domain.model.Track
import com.djapp.domain.usecase.LibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

// -----------------------------------------------------------------------
// MVI: State / Intent
// -----------------------------------------------------------------------

data class LibraryUiState(
    val tracks     : List<Track>  = emptyList(),
    val searchQuery: String       = "",
    val sortOrder  : SortOrder    = SortOrder.TITLE,
    val isLoading  : Boolean      = true,
    val hasPermission: Boolean    = false,
)

sealed class LibraryIntent {
    data class Search(val query: String)         : LibraryIntent()
    data class SetSortOrder(val order: SortOrder): LibraryIntent()
    data class SetPermission(val granted: Boolean): LibraryIntent()
}

// -----------------------------------------------------------------------
// ViewModel
// -----------------------------------------------------------------------

/**
 * T-105: ライブラリ画面のViewModel。UC-008 対応。
 * 検索クエリ・ソート順の変化をFlatMapLatestで効率よく処理する。
 */
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val libraryUseCase: LibraryUseCase,
) : ViewModel() {

    private val _searchQuery   = MutableStateFlow("")
    private val _sortOrder     = MutableStateFlow(SortOrder.TITLE)
    private val _hasPermission = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val uiState: StateFlow<LibraryUiState> = combine(
        _searchQuery.debounce(200),   // 入力の度にMediaStoreを叩かないよう200msデバウンス
        _sortOrder,
        _hasPermission,
    ) { query, sort, permission -> Triple(query, sort, permission) }
        .flatMapLatest { (query, sort, permission) ->
            val trackFlow = if (query.isBlank()) {
                libraryUseCase.getAllTracks(sort)
            } else {
                libraryUseCase.searchTracks(query, sort)
            }
            // 権限なし時は空リストを返す
            if (!permission) {
                kotlinx.coroutines.flow.flowOf(
                    LibraryUiState(
                        tracks        = emptyList(),
                        searchQuery   = query,
                        sortOrder     = sort,
                        isLoading     = false,
                        hasPermission = false,
                    )
                )
            } else {
                trackFlow.let { flow ->
                    kotlinx.coroutines.flow.flow {
                        flow.collect { tracks ->
                            emit(
                                LibraryUiState(
                                    tracks        = tracks,
                                    searchQuery   = query,
                                    sortOrder     = sort,
                                    isLoading     = false,
                                    hasPermission = true,
                                )
                            )
                        }
                    }
                }
            }
        }
        .stateIn(
            scope         = viewModelScope,
            started       = SharingStarted.WhileSubscribed(5_000),
            initialValue  = LibraryUiState(),
        )

    fun onIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.Search        -> _searchQuery.value   = intent.query
            is LibraryIntent.SetSortOrder  -> _sortOrder.value     = intent.order
            is LibraryIntent.SetPermission -> _hasPermission.value = intent.granted
        }
    }
}
