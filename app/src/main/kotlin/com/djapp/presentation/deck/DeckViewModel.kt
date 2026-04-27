package com.djapp.presentation.deck

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.djapp.domain.model.DeckId
import com.djapp.domain.model.DeckState
import com.djapp.domain.model.Track
import com.djapp.domain.usecase.BpmSyncUseCase
import com.djapp.domain.usecase.EqualizerUseCase
import com.djapp.domain.usecase.LoadTrackUseCase
import com.djapp.domain.usecase.LoopUseCase
import com.djapp.domain.usecase.PlaybackControlUseCase
import com.djapp.domain.usecase.ScratchUseCase
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

enum class EqBand { LOW, MID, HIGH }

// ---------------------------------------------------------------------------
// MVI: State / Intent
// ---------------------------------------------------------------------------
data class DeckUiState(
    val deckA: DeckState = DeckState(),
    val deckB: DeckState = DeckState(),
    val isLoadingA: Boolean = false,
    val isLoadingB: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface DeckIntent {
    data class LoadTrack(val deckId: DeckId, val track: Track) : DeckIntent
    data class Play(val deckId: DeckId) : DeckIntent
    data class Pause(val deckId: DeckId) : DeckIntent
    data class Stop(val deckId: DeckId) : DeckIntent
    data class SetPitchRatio(val deckId: DeckId, val ratio: Float) : DeckIntent
    data class AdjustBpmMultiplier(val deckId: DeckId, val factor: Float) : DeckIntent
    data class SetEqLow (val deckId: DeckId, val gainDb: Float) : DeckIntent
    data class SetEqMid (val deckId: DeckId, val gainDb: Float) : DeckIntent
    data class SetEqHigh(val deckId: DeckId, val gainDb: Float) : DeckIntent
    data class SetLoopIn    (val deckId: DeckId) : DeckIntent
    data class SetLoopOut   (val deckId: DeckId) : DeckIntent
    data class DoubleLoop   (val deckId: DeckId) : DeckIntent
    data class HalveLoop    (val deckId: DeckId) : DeckIntent
    data class DeactivateLoop(val deckId: DeckId) : DeckIntent
    data class Seek(val deckId: DeckId, val positionRatio: Float) : DeckIntent
    data class NudgeLoopIn (val deckId: DeckId, val deltaSec: Float) : DeckIntent
    data class NudgeLoopOut(val deckId: DeckId, val deltaSec: Float) : DeckIntent
    data class ScratchStart(val deckId: DeckId) : DeckIntent
    data class ScratchMove(val deckId: DeckId, val speed: Float) : DeckIntent
    data class ScratchEnd(val deckId: DeckId) : DeckIntent
    data object ToggleSync : DeckIntent
    data object ClearError : DeckIntent
}

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------
@HiltViewModel
class DeckViewModel @Inject constructor(
    private val loadTrackUseCase: LoadTrackUseCase,
    private val playbackControlUseCase: PlaybackControlUseCase,
    private val bpmSyncUseCase: BpmSyncUseCase,
    private val equalizerUseCase: EqualizerUseCase,
    private val loopUseCase: LoopUseCase,
    private val scratchUseCase: ScratchUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeckUiState())
    val uiState: StateFlow<DeckUiState> = _uiState.asStateFlow()

    private var playheadPollingJob: Job? = null

    fun onIntent(intent: DeckIntent) {
        when (intent) {
            is DeckIntent.LoadTrack           -> loadTrack(intent.deckId, intent.track)
            is DeckIntent.Play                -> play(intent.deckId)
            is DeckIntent.Pause               -> pause(intent.deckId)
            is DeckIntent.Stop                -> stop(intent.deckId)
            is DeckIntent.SetPitchRatio       -> setPitchRatio(intent.deckId, intent.ratio)
            is DeckIntent.AdjustBpmMultiplier -> adjustBpmMultiplier(intent.deckId, intent.factor)
            is DeckIntent.SetEqLow            -> setEq(intent.deckId, EqBand.LOW,  intent.gainDb)
            is DeckIntent.SetEqMid            -> setEq(intent.deckId, EqBand.MID,  intent.gainDb)
            is DeckIntent.SetEqHigh           -> setEq(intent.deckId, EqBand.HIGH, intent.gainDb)
            is DeckIntent.SetLoopIn           -> setLoopIn(intent.deckId)
            is DeckIntent.SetLoopOut          -> setLoopOut(intent.deckId)
            is DeckIntent.DoubleLoop          -> doubleLoop(intent.deckId)
            is DeckIntent.HalveLoop           -> halveLoop(intent.deckId)
            is DeckIntent.DeactivateLoop      -> deactivateLoop(intent.deckId)
            is DeckIntent.Seek                -> seek(intent.deckId, intent.positionRatio)
            is DeckIntent.NudgeLoopIn         -> nudgeLoopIn(intent.deckId, intent.deltaSec)
            is DeckIntent.NudgeLoopOut        -> nudgeLoopOut(intent.deckId, intent.deltaSec)
            is DeckIntent.ScratchStart        -> scratchUseCase.start(intent.deckId)
            is DeckIntent.ScratchMove         -> scratchUseCase.setSpeed(intent.deckId, intent.speed)
            is DeckIntent.ScratchEnd          -> scratchUseCase.stop(intent.deckId)
            DeckIntent.ToggleSync             -> toggleSync()
            DeckIntent.ClearError             -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    // --- ロード ---
    private fun loadTrack(deckId: DeckId, track: Track) {
        viewModelScope.launch {
            setLoading(deckId, true)

            // キャッシュに精度の高いBPMがあれば即座に表示
            val cachedBpm = bpmSyncUseCase.getCachedBpm(track.filePath)

            val ok = loadTrackUseCase(deckId, track)
            setLoading(deckId, false)

            if (ok) {
                // Phase 1完了直後のBPM（キャッシュあれば上書き）
                val initialBpm = cachedBpm
                    ?: bpmSyncUseCase.getBpm(deckId).takeIf { it > 0f }

                updateDeckState(deckId) { current ->
                    current.copy(
                        track             = track,
                        isPlaying         = false,
                        playheadSec       = 0f,
                        bpm               = initialBpm,
                        pitchRatio        = 1.0f,
                        bpmMultiplier     = 1.0f,
                        isSynced          = false,
                        loop              = null,
                        pendingLoopInSec  = null,
                    )
                }

                if (cachedBpm != null) {
                    // キャッシュ済み: ポーリング不要。Phase 2完了後に精度向上BPMを再保存。
                    waitForPhase2AndRefine(deckId, track)
                } else {
                    // 未キャッシュ: C++の非同期検出完了を待ってUIに反映 + キャッシュ保存
                    pollForBpmAndCache(deckId, track)
                }
            } else {
                _uiState.update { it.copy(errorMessage = "「${track.title}」のロードに失敗しました") }
            }
        }
    }

    // --- 再生 ---
    private fun play(deckId: DeckId) {
        playbackControlUseCase.play(deckId)
        updateDeckState(deckId) { it.copy(isPlaying = true) }
        ensurePolling()
    }

    // --- 一時停止 ---
    private fun pause(deckId: DeckId) {
        playbackControlUseCase.pause(deckId)
        updateDeckState(deckId) { it.copy(isPlaying = false) }
        stopPollingIfIdle()
    }

    // --- 停止 ---
    private fun stop(deckId: DeckId) {
        playbackControlUseCase.stop(deckId)
        updateDeckState(deckId) { it.copy(isPlaying = false, playheadSec = 0f) }
        stopPollingIfIdle()
    }

    // ---------------------------------------------------------------------------
    // BPMポーリング（未キャッシュ時）
    // Phase 1のBPM推定値が出るまで待ち、その後Phase 2完了まで待って精度向上版を保存する。
    // ---------------------------------------------------------------------------
    private fun pollForBpmAndCache(deckId: DeckId, track: Track) {
        viewModelScope.launch {
            // Phase 1 BPM取得（最大10秒）
            var phase1Bpm: Float? = null
            repeat(20) {
                delay(500L)
                val bpm = bpmSyncUseCase.getBpm(deckId).takeIf { it > 0f }
                if (bpm != null) {
                    phase1Bpm = bpm
                    updateDeckState(deckId) { it.copy(bpm = bpm) }
                    return@repeat
                }
            }
            if (phase1Bpm == null) return@launch

            // Phase 2完了を待って精度の高いBPMで上書き・キャッシュ保存
            waitForPhase2AndRefine(deckId, track)
        }
    }

    // Phase 2完了（BPMが安定）を待ってUIを更新し、キャッシュに保存する。
    // Phase 2デコードは最大約15秒で完了する想定。
    private fun waitForPhase2AndRefine(deckId: DeckId, track: Track) {
        viewModelScope.launch {
            var prevBpm = bpmSyncUseCase.getBpm(deckId)
            // 最大30秒・1秒ごとにBPMが変化したか確認する
            // Phase 2完了後にstartBpmDetectionAsyncが再実行され値が更新される
            repeat(30) {
                delay(1000L)
                val bpm = bpmSyncUseCase.getBpm(deckId).takeIf { it > 0f } ?: return@repeat
                if (bpm != prevBpm) {
                    updateDeckState(deckId) { it.copy(bpm = bpm) }
                    bpmSyncUseCase.saveDetectedBpm(track.filePath, deckId)
                    return@launch
                }
                prevBpm = bpm
            }
            // 変化がなくてもPhase 2完了後の値をキャッシュ保存（初回キャッシュ作成）
            val finalBpm = bpmSyncUseCase.getBpm(deckId)
            if (finalBpm > 0f) bpmSyncUseCase.saveDetectedBpm(track.filePath, deckId)
        }
    }

    // --- ピッチ操作 ---
    private fun setPitchRatio(deckId: DeckId, ratio: Float) {
        val applied = bpmSyncUseCase.setPitchRatio(deckId, ratio)
        if (deckId == DeckId.A) {
            updateDeckState(DeckId.A) { it.copy(pitchRatio = applied) }
            if (_uiState.value.deckB.isSynced) resyncDeckB()
        } else {
            updateDeckState(DeckId.B) { it.copy(pitchRatio = applied, isSynced = false) }
        }
    }

    // --- EQ ---
    private fun setEq(deckId: DeckId, band: EqBand, gainDb: Float) {
        when (band) {
            EqBand.LOW  -> equalizerUseCase.setLow (deckId, gainDb)
            EqBand.MID  -> equalizerUseCase.setMid (deckId, gainDb)
            EqBand.HIGH -> equalizerUseCase.setHigh(deckId, gainDb)
        }
        updateDeckState(deckId) { state ->
            val eq = state.eq
            state.copy(eq = when (band) {
                EqBand.LOW  -> eq.copy(lowDb  = gainDb)
                EqBand.MID  -> eq.copy(midDb  = gainDb)
                EqBand.HIGH -> eq.copy(highDb = gainDb)
            })
        }
    }

    // --- BPM表示倍率補正（÷2 / ×2）---
    private fun adjustBpmMultiplier(deckId: DeckId, factor: Float) {
        val wasBSynced = _uiState.value.deckB.isSynced
        updateDeckState(deckId) { state ->
            val newMult = (state.bpmMultiplier * factor).coerceIn(0.25f, 4.0f)
            state.copy(bpmMultiplier = newMult, isSynced = false)
        }
        // デッキAのmultiplier変更時にBが同期中なら即座に再計算
        // （デッキB変更時は意図的な手動操作なのでsync解除のまま）
        if (deckId == DeckId.A && wasBSynced) resyncDeckB()
    }

    // デッキBの同期ピッチを現在のデッキA状態から再計算して適用する。
    private fun resyncDeckB() {
        val stateA = _uiState.value.deckA
        val stateB = _uiState.value.deckB
        val syncPitch = bpmSyncUseCase.syncTo(
            slaveDeckId         = DeckId.B,
            masterDeckId        = DeckId.A,
            masterPitchRatio    = stateA.pitchRatio,
            masterBpmMultiplier = stateA.bpmMultiplier,
            slaveBpmMultiplier  = stateB.bpmMultiplier,
        ) ?: return
        updateDeckState(DeckId.B) { it.copy(pitchRatio = syncPitch) }
    }

    // --- SYNCトグル（B → A）---
    private fun toggleSync() {
        val stateA = _uiState.value.deckA
        val stateB = _uiState.value.deckB
        if (stateB.isSynced) {
            updateDeckState(DeckId.B) { it.copy(isSynced = false) }
        } else {
            val syncPitch = bpmSyncUseCase.syncTo(
                slaveDeckId         = DeckId.B,
                masterDeckId        = DeckId.A,
                masterPitchRatio    = stateA.pitchRatio,
                masterBpmMultiplier = stateA.bpmMultiplier,
                slaveBpmMultiplier  = stateB.bpmMultiplier,
            ) ?: return
            updateDeckState(DeckId.B) { it.copy(pitchRatio = syncPitch, isSynced = true) }
        }
    }

    // ---------------------------------------------------------------------------
    // 再生位置ポーリング（100msごとにC++から取得してStateを更新）
    // ---------------------------------------------------------------------------
    private fun ensurePolling() {
        if (playheadPollingJob?.isActive == true) return
        playheadPollingJob = viewModelScope.launch {
            while (isActive) {
                delay(100L)
                val stateA = _uiState.value.deckA
                val stateB = _uiState.value.deckB

                val newA = if (stateA.isPlaying)
                    stateA.copy(playheadSec = playbackControlUseCase.getPlayheadSec(DeckId.A))
                else stateA

                val newB = if (stateB.isPlaying)
                    stateB.copy(playheadSec = playbackControlUseCase.getPlayheadSec(DeckId.B))
                else stateB

                _uiState.update { it.copy(deckA = newA, deckB = newB) }

                if (!newA.isPlaying && !newB.isPlaying) break
            }
        }
    }

    private fun stopPollingIfIdle() {
        val state = _uiState.value
        if (!state.deckA.isPlaying && !state.deckB.isPlaying) {
            playheadPollingJob?.cancel()
        }
    }

    // --- シーク ---
    private fun seek(deckId: DeckId, positionRatio: Float) {
        val state = if (deckId == DeckId.A) _uiState.value.deckA else _uiState.value.deckB
        val durationSec = state.track?.durationMs?.div(1000f) ?: return
        val positionSec = (positionRatio * durationSec).coerceIn(0f, durationSec)
        playbackControlUseCase.seekTo(deckId, positionSec)
        updateDeckState(deckId) { it.copy(playheadSec = positionSec) }
    }

    // --- マニュアルループ ---
    private fun setLoopIn(deckId: DeckId) {
        val state = if (deckId == DeckId.A) _uiState.value.deckA else _uiState.value.deckB
        // 既存ループがあれば解除してからIN点をセット
        if (state.loop != null) loopUseCase.deactivate(deckId)
        updateDeckState(deckId) { it.copy(loop = null, pendingLoopInSec = state.playheadSec) }
    }

    private fun setLoopOut(deckId: DeckId) {
        val state = if (deckId == DeckId.A) _uiState.value.deckA else _uiState.value.deckB
        val inSec = state.pendingLoopInSec ?: return
        val outSec = state.playheadSec
        if (outSec <= inSec) return  // OUT が IN より前の場合は無視
        val loop = loopUseCase.activate(deckId, inSec, outSec)
        updateDeckState(deckId) { it.copy(loop = loop, pendingLoopInSec = null) }
    }

    private fun doubleLoop(deckId: DeckId) {
        val loop = (if (deckId == DeckId.A) _uiState.value.deckA else _uiState.value.deckB).loop ?: return
        val newLoop = loopUseCase.doubleLength(deckId, loop)
        updateDeckState(deckId) { it.copy(loop = newLoop) }
    }

    private fun halveLoop(deckId: DeckId) {
        val loop = (if (deckId == DeckId.A) _uiState.value.deckA else _uiState.value.deckB).loop ?: return
        val newLoop = loopUseCase.halveLength(deckId, loop)
        updateDeckState(deckId) { it.copy(loop = newLoop) }
    }

    private fun nudgeLoopIn(deckId: DeckId, deltaSec: Float) {
        val loop = (if (deckId == DeckId.A) _uiState.value.deckA else _uiState.value.deckB).loop ?: return
        val newLoop = loopUseCase.nudgeIn(deckId, loop, deltaSec)
        updateDeckState(deckId) { it.copy(loop = newLoop) }
    }

    private fun nudgeLoopOut(deckId: DeckId, deltaSec: Float) {
        val loop = (if (deckId == DeckId.A) _uiState.value.deckA else _uiState.value.deckB).loop ?: return
        val newLoop = loopUseCase.nudgeOut(deckId, loop, deltaSec)
        updateDeckState(deckId) { it.copy(loop = newLoop) }
    }

    private fun deactivateLoop(deckId: DeckId) {
        loopUseCase.deactivate(deckId)
        updateDeckState(deckId) { it.copy(loop = null, pendingLoopInSec = null) }
    }

    // ---------------------------------------------------------------------------
    // ヘルパー
    // ---------------------------------------------------------------------------
    private fun setLoading(deckId: DeckId, loading: Boolean) {
        _uiState.update {
            if (deckId == DeckId.A) it.copy(isLoadingA = loading)
            else it.copy(isLoadingB = loading)
        }
    }

    private fun updateDeckState(deckId: DeckId, transform: (DeckState) -> DeckState) {
        _uiState.update {
            if (deckId == DeckId.A) it.copy(deckA = transform(it.deckA))
            else it.copy(deckB = transform(it.deckB))
        }
    }

    override fun onCleared() {
        super.onCleared()
        playheadPollingJob?.cancel()
    }
}
