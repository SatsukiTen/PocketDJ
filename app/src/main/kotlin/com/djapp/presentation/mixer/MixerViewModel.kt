package com.djapp.presentation.mixer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djapp.domain.model.CrossfaderCurve
import com.djapp.domain.model.ScratchMode
import com.djapp.domain.repository.AudioEngineRepository
import com.djapp.domain.repository.SettingsRepository
import com.djapp.domain.usecase.CrossfaderUseCase
import kotlinx.coroutines.delay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---------------------------------------------------------------------------
// MVI: State / Intent
// ---------------------------------------------------------------------------

data class MixerUiState(
    val crossfaderPosition: Float = 0.5f,
    val scratchMode: ScratchMode = ScratchMode.CHOP_PAD,
    val latencyMs: Double = 0.0,
    val xRunCount: Int = 0,
)

sealed interface MixerIntent {
    data class SetCrossfaderPosition(val position: Float) : MixerIntent
    data object ResetCrossfader : MixerIntent
    data class SetScratchMode(val mode: ScratchMode) : MixerIntent
}

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

/**
 * T-305: クロスフェーダー／ミキサー状態を管理する ViewModel（MVI）。
 */
@HiltViewModel
class MixerViewModel @Inject constructor(
    private val crossfaderUseCase: CrossfaderUseCase,
    private val settingsRepository: SettingsRepository,
    private val audioEngineRepository: AudioEngineRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MixerUiState())
    val uiState: StateFlow<MixerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            crossfaderUseCase.setCurve(CrossfaderCurve.EQUAL_POWER)
        }
        crossfaderUseCase.syncCurve(CrossfaderCurve.EQUAL_POWER)

        // 保存済みスクラッチモードを読み込む
        viewModelScope.launch {
            settingsRepository.getScratchMode().collect { mode ->
                _uiState.update { it.copy(scratchMode = mode) }
            }
        }

        // T-802: ストリーム安定後にレイテンシを計測（3秒後に1回）
        viewModelScope.launch {
            delay(3_000L)
            _uiState.update {
                it.copy(
                    latencyMs  = audioEngineRepository.getLatencyMs(),
                    xRunCount  = audioEngineRepository.getXRunCount(),
                )
            }
        }
    }

    fun onIntent(intent: MixerIntent) {
        when (intent) {
            is MixerIntent.SetCrossfaderPosition -> {
                _uiState.update { it.copy(crossfaderPosition = intent.position) }
                crossfaderUseCase.setPosition(intent.position)
            }
            MixerIntent.ResetCrossfader -> {
                _uiState.update { it.copy(crossfaderPosition = 0.5f) }
                crossfaderUseCase.resetToCenter()
            }
            is MixerIntent.SetScratchMode -> {
                _uiState.update { it.copy(scratchMode = intent.mode) }
                viewModelScope.launch { settingsRepository.setScratchMode(intent.mode) }
            }
        }
    }
}
