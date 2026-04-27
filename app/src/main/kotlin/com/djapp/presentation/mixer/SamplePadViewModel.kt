package com.djapp.presentation.mixer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djapp.domain.model.DeckId
import com.djapp.domain.model.LoopState
import com.djapp.domain.model.SamplePadState
import com.djapp.domain.model.SamplePlayMode
import com.djapp.domain.usecase.SamplePadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SamplePadsUiState(
    val pads: List<SamplePadState> = List(4) { SamplePadState(id = it) },
)

sealed interface SamplePadIntent {
    data class Capture(val slotId: Int, val deckId: DeckId, val loop: LoopState) : SamplePadIntent
    data class TogglePlay(val slotId: Int) : SamplePadIntent
    data class TogglePlayMode(val slotId: Int) : SamplePadIntent
    data class Clear(val slotId: Int) : SamplePadIntent
}

@HiltViewModel
class SamplePadViewModel @Inject constructor(
    private val useCase: SamplePadUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SamplePadsUiState())
    val uiState: StateFlow<SamplePadsUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    fun onIntent(intent: SamplePadIntent) {
        when (intent) {
            is SamplePadIntent.Capture        -> capture(intent.slotId, intent.deckId, intent.loop)
            is SamplePadIntent.TogglePlay     -> togglePlay(intent.slotId)
            is SamplePadIntent.TogglePlayMode -> togglePlayMode(intent.slotId)
            is SamplePadIntent.Clear          -> clear(intent.slotId)
        }
    }

    private fun capture(slotId: Int, deckId: DeckId, loop: LoopState) {
        val ok = useCase.capture(slotId, deckId, loop)
        if (!ok) return
        updatePad(slotId) { pad ->
            pad.copy(
                isLoaded    = true,
                isPlaying   = false,
                durationSec = loop.loopOutSec - loop.loopInSec,
            )
        }
    }

    private fun togglePlay(slotId: Int) {
        val pad = _uiState.value.pads[slotId]
        if (!pad.isLoaded) return
        if (pad.isPlaying) {
            useCase.stop(slotId)
            updatePad(slotId) { it.copy(isPlaying = false) }
        } else {
            useCase.play(slotId, pad.playMode == SamplePlayMode.LOOP)
            updatePad(slotId) { it.copy(isPlaying = true) }
            if (pad.playMode == SamplePlayMode.SINGLE) ensurePolling()
        }
    }

    private fun togglePlayMode(slotId: Int) {
        val pad = _uiState.value.pads[slotId]
        val newMode = if (pad.playMode == SamplePlayMode.SINGLE) SamplePlayMode.LOOP
                      else SamplePlayMode.SINGLE
        updatePad(slotId) { it.copy(playMode = newMode) }
    }

    private fun clear(slotId: Int) {
        useCase.stop(slotId)
        useCase.clear(slotId)
        updatePad(slotId) { SamplePadState(id = slotId) }
    }

    // SINGLE モードで再生中のパッドが終了したかをポーリングで検出する
    private fun ensurePolling() {
        if (pollingJob?.isActive == true) return
        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(100L)
                var anySinglePlaying = false
                val newPads = _uiState.value.pads.map { pad ->
                    if (pad.isPlaying && pad.playMode == SamplePlayMode.SINGLE) {
                        val still = useCase.isPlaying(pad.id)
                        if (!still) pad.copy(isPlaying = false)
                        else { anySinglePlaying = true; pad }
                    } else pad
                }
                _uiState.update { it.copy(pads = newPads) }
                if (!anySinglePlaying) break
            }
        }
    }

    private fun updatePad(slotId: Int, transform: (SamplePadState) -> SamplePadState) {
        _uiState.update { state ->
            state.copy(pads = state.pads.mapIndexed { i, pad ->
                if (i == slotId) transform(pad) else pad
            })
        }
    }

    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }
}
