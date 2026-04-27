package com.djapp.presentation.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.djapp.domain.model.DeckId
import com.djapp.domain.model.Track
import com.djapp.presentation.deck.DeckIntent
import com.djapp.presentation.deck.DeckPanel
import com.djapp.presentation.deck.DeckViewModel
import com.djapp.presentation.deck.WaveformStrip
import com.djapp.presentation.library.LibraryBrowserScreen
import com.djapp.presentation.mixer.CrossfaderPanel
import com.djapp.presentation.mixer.MixerIntent
import com.djapp.presentation.mixer.MixerViewModel
import com.djapp.presentation.mixer.SamplePadIntent
import com.djapp.presentation.mixer.SamplePadRow
import com.djapp.presentation.mixer.SamplePadViewModel
import com.djapp.presentation.theme.DjAppTheme

/**
 * T-209 / T-306 / T-307: メイン画面 — デッキ A / B + クロスフェーダー。
 */
@Composable
fun MainScreen(
    deckViewModel: DeckViewModel = hiltViewModel(),
    mixerViewModel: MixerViewModel = hiltViewModel(),
    samplePadViewModel: SamplePadViewModel = hiltViewModel(),
) {
    val deckState by deckViewModel.uiState.collectAsState()
    val mixerState by mixerViewModel.uiState.collectAsState()
    val samplePadsState by samplePadViewModel.uiState.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    var libraryPickerFor: DeckId? by remember { mutableStateOf(null) }

    LaunchedEffect(deckState.errorMessage) {
        deckState.errorMessage?.let { msg ->
            snackbarHost.showSnackbar(msg)
            deckViewModel.onIntent(DeckIntent.ClearError)
        }
    }

    DjAppTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHost) },
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    // --- デッキパネル 2枚（横並び）波形・PAD・フェーダーの位置を固定するため weight(1f) ---
                    Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        DeckPanel(
                            deckId      = DeckId.A,
                            deckState   = deckState.deckA,
                            isLoading   = deckState.isLoadingA,
                            scratchMode = mixerState.scratchMode,
                            samplePads  = samplePadsState.pads,
                            onPlay    = { deckViewModel.onIntent(DeckIntent.Play(DeckId.A)) },
                            onPause   = { deckViewModel.onIntent(DeckIntent.Pause(DeckId.A)) },
                            onStop    = { deckViewModel.onIntent(DeckIntent.Stop(DeckId.A)) },
                            onOpenLibrary = { libraryPickerFor = DeckId.A },
                            onPitchChange = { ratio ->
                                deckViewModel.onIntent(DeckIntent.SetPitchRatio(DeckId.A, ratio))
                            },
                            onBpmHalve  = { deckViewModel.onIntent(DeckIntent.AdjustBpmMultiplier(DeckId.A, 0.5f)) },
                            onBpmDouble = { deckViewModel.onIntent(DeckIntent.AdjustBpmMultiplier(DeckId.A, 2.0f)) },
                            onEqLowChange  = { db -> deckViewModel.onIntent(DeckIntent.SetEqLow (DeckId.A, db)) },
                            onEqMidChange  = { db -> deckViewModel.onIntent(DeckIntent.SetEqMid (DeckId.A, db)) },
                            onEqHighChange = { db -> deckViewModel.onIntent(DeckIntent.SetEqHigh(DeckId.A, db)) },
                            onSetLoopIn      = { deckViewModel.onIntent(DeckIntent.SetLoopIn(DeckId.A)) },
                            onSetLoopOut     = { deckViewModel.onIntent(DeckIntent.SetLoopOut(DeckId.A)) },
                            onDoubleLoop     = { deckViewModel.onIntent(DeckIntent.DoubleLoop(DeckId.A)) },
                            onHalveLoop      = { deckViewModel.onIntent(DeckIntent.HalveLoop(DeckId.A)) },
                            onDeactivateLoop = { deckViewModel.onIntent(DeckIntent.DeactivateLoop(DeckId.A)) },
                            onNudgeLoopIn    = { d -> deckViewModel.onIntent(DeckIntent.NudgeLoopIn(DeckId.A, d)) },
                            onNudgeLoopOut   = { d -> deckViewModel.onIntent(DeckIntent.NudgeLoopOut(DeckId.A, d)) },
                            onCaptureToSample = { slotId ->
                                deckState.deckA.loop?.let { loop ->
                                    samplePadViewModel.onIntent(SamplePadIntent.Capture(slotId, DeckId.A, loop))
                                }
                            },
                            onScratchStart      = { deckViewModel.onIntent(DeckIntent.ScratchStart(DeckId.A)) },
                            onScratchMove       = { spd -> deckViewModel.onIntent(DeckIntent.ScratchMove(DeckId.A, spd)) },
                            onScratchEnd        = { deckViewModel.onIntent(DeckIntent.ScratchEnd(DeckId.A)) },
                            onScratchModeChange = { mode -> mixerViewModel.onIntent(MixerIntent.SetScratchMode(mode)) },
                            modifier  = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        DeckPanel(
                            deckId      = DeckId.B,
                            deckState   = deckState.deckB,
                            isLoading   = deckState.isLoadingB,
                            masterBpmAvailable = deckState.deckA.bpm != null,
                            scratchMode = mixerState.scratchMode,
                            samplePads  = samplePadsState.pads,
                            onPlay    = { deckViewModel.onIntent(DeckIntent.Play(DeckId.B)) },
                            onPause   = { deckViewModel.onIntent(DeckIntent.Pause(DeckId.B)) },
                            onStop    = { deckViewModel.onIntent(DeckIntent.Stop(DeckId.B)) },
                            onOpenLibrary = { libraryPickerFor = DeckId.B },
                            onPitchChange = { ratio ->
                                deckViewModel.onIntent(DeckIntent.SetPitchRatio(DeckId.B, ratio))
                            },
                            onToggleSync = { deckViewModel.onIntent(DeckIntent.ToggleSync) },
                            onBpmHalve  = { deckViewModel.onIntent(DeckIntent.AdjustBpmMultiplier(DeckId.B, 0.5f)) },
                            onBpmDouble = { deckViewModel.onIntent(DeckIntent.AdjustBpmMultiplier(DeckId.B, 2.0f)) },
                            onEqLowChange  = { db -> deckViewModel.onIntent(DeckIntent.SetEqLow (DeckId.B, db)) },
                            onEqMidChange  = { db -> deckViewModel.onIntent(DeckIntent.SetEqMid (DeckId.B, db)) },
                            onEqHighChange = { db -> deckViewModel.onIntent(DeckIntent.SetEqHigh(DeckId.B, db)) },
                            onSetLoopIn      = { deckViewModel.onIntent(DeckIntent.SetLoopIn(DeckId.B)) },
                            onSetLoopOut     = { deckViewModel.onIntent(DeckIntent.SetLoopOut(DeckId.B)) },
                            onDoubleLoop     = { deckViewModel.onIntent(DeckIntent.DoubleLoop(DeckId.B)) },
                            onHalveLoop      = { deckViewModel.onIntent(DeckIntent.HalveLoop(DeckId.B)) },
                            onDeactivateLoop = { deckViewModel.onIntent(DeckIntent.DeactivateLoop(DeckId.B)) },
                            onNudgeLoopIn    = { d -> deckViewModel.onIntent(DeckIntent.NudgeLoopIn(DeckId.B, d)) },
                            onNudgeLoopOut   = { d -> deckViewModel.onIntent(DeckIntent.NudgeLoopOut(DeckId.B, d)) },
                            onCaptureToSample = { slotId ->
                                deckState.deckB.loop?.let { loop ->
                                    samplePadViewModel.onIntent(SamplePadIntent.Capture(slotId, DeckId.B, loop))
                                }
                            },
                            onScratchStart      = { deckViewModel.onIntent(DeckIntent.ScratchStart(DeckId.B)) },
                            onScratchMove       = { spd -> deckViewModel.onIntent(DeckIntent.ScratchMove(DeckId.B, spd)) },
                            onScratchEnd        = { deckViewModel.onIntent(DeckIntent.ScratchEnd(DeckId.B)) },
                            onScratchModeChange = { mode -> mixerViewModel.onIntent(MixerIntent.SetScratchMode(mode)) },
                            modifier  = Modifier.weight(1f),
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    // --- 波形ストリップ A / B（横並び・シーク対応）---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        WaveformStrip(
                            deckId      = DeckId.A,
                            deckState   = deckState.deckA,
                            accentColor = MaterialTheme.colorScheme.primary,
                            onSeek      = { r -> deckViewModel.onIntent(DeckIntent.Seek(DeckId.A, r)) },
                            modifier    = Modifier.weight(1f),
                        )
                        WaveformStrip(
                            deckId      = DeckId.B,
                            deckState   = deckState.deckB,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            onSeek      = { r -> deckViewModel.onIntent(DeckIntent.Seek(DeckId.B, r)) },
                            modifier    = Modifier.weight(1f),
                        )
                    }

                    Spacer(Modifier.height(2.dp))

                    // --- サンプルパッド（4スロット）---
                    SamplePadRow(
                        pads = samplePadsState.pads,
                        onTogglePlay     = { samplePadViewModel.onIntent(SamplePadIntent.TogglePlay(it)) },
                        onTogglePlayMode = { samplePadViewModel.onIntent(SamplePadIntent.TogglePlayMode(it)) },
                        onClear          = { samplePadViewModel.onIntent(SamplePadIntent.Clear(it)) },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Spacer(Modifier.height(2.dp))

                    // --- クロスフェーダー + スクラッチモード切替（T-306）---
                    CrossfaderPanel(
                        position = mixerState.crossfaderPosition,
                        onPositionChange = { pos ->
                            mixerViewModel.onIntent(MixerIntent.SetCrossfaderPosition(pos))
                        },
                        onDoubleTap = {
                            mixerViewModel.onIntent(MixerIntent.ResetCrossfader)
                        },
                        latencyMs  = mixerState.latencyMs,
                        xRunCount  = mixerState.xRunCount,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // --- ライブラリピッカー ---
        libraryPickerFor?.let { targetDeck ->
            LibraryPickerOverlay(
                targetDeck = targetDeck,
                onTrackSelected = { track ->
                    deckViewModel.onIntent(DeckIntent.LoadTrack(targetDeck, track))
                    libraryPickerFor = null
                },
                onDismiss = { libraryPickerFor = null },
            )
        }

    }
}

@Composable
private fun LibraryPickerOverlay(
    targetDeck: DeckId,
    onTrackSelected: (Track) -> Unit,
    onDismiss: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        LibraryBrowserScreen(
            onTrackSelected = onTrackSelected,
            onDismiss = onDismiss,
        )
    }
}
