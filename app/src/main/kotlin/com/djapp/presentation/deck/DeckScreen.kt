package com.djapp.presentation.deck

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djapp.domain.model.DeckId
import com.djapp.domain.model.DeckState
import com.djapp.domain.model.EqState
import com.djapp.domain.model.LoopState
import com.djapp.domain.model.SamplePadState
import com.djapp.domain.model.ScratchMode
import com.djapp.domain.usecase.LoopUseCase
import com.djapp.presentation.mixer.SampleCaptureRow

/**
 * T-209 / T-210 / T-407: デッキUI
 * BPM表示・SYNCボタン（デッキBのみ）・ピッチスライダーを含む。
 */
@Composable
fun DeckPanel(
    deckId: DeckId,
    deckState: DeckState,
    isLoading: Boolean,
    masterBpmAvailable: Boolean = false,
    scratchMode: ScratchMode = ScratchMode.CHOP_PAD,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onOpenLibrary: () -> Unit,
    onPitchChange: (Float) -> Unit = {},
    onToggleSync: () -> Unit = {},
    onBpmHalve: () -> Unit = {},
    onBpmDouble: () -> Unit = {},
    onEqLowChange: (Float) -> Unit = {},
    onEqMidChange: (Float) -> Unit = {},
    onEqHighChange: (Float) -> Unit = {},
    onSetLoopIn: () -> Unit = {},
    onSetLoopOut: () -> Unit = {},
    onDoubleLoop: () -> Unit = {},
    onHalveLoop: () -> Unit = {},
    onDeactivateLoop: () -> Unit = {},
    onNudgeLoopIn: (Float) -> Unit = {},
    onNudgeLoopOut: (Float) -> Unit = {},
    onCaptureToSample: (slotId: Int) -> Unit = {},
    samplePads: List<com.djapp.domain.model.SamplePadState> = emptyList(),
    onScratchStart: () -> Unit = {},
    onScratchMove: (Float) -> Unit = {},
    onScratchEnd: () -> Unit = {},
    onScratchModeChange: (ScratchMode) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val deckLabel = if (deckId == DeckId.A) "A" else "B"
    val deckColor = if (deckId == DeckId.A)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.secondary

    Column(
        modifier = modifier
            .fillMaxHeight()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(28.dp)
                    .background(deckColor, CircleShape),
            ) {
                Text(
                    text = deckLabel,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                )
            }

            Spacer(Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deckState.track?.title ?: "--- トラック未選択 ---",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (deckState.track == null)
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    else
                        MaterialTheme.colorScheme.onSurface,
                )
                // BPM + ÷2/×2 + SYNC をヘッダー2行目に統合
                val effectiveBpm = deckState.bpm?.let { it * deckState.bpmMultiplier * deckState.pitchRatio }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (effectiveBpm != null) "%.1f BPM".format(effectiveBpm) else "--.- BPM",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (effectiveBpm != null) deckColor
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    )
                    if (deckState.bpm != null) {
                        androidx.compose.material3.TextButton(
                            onClick = onBpmHalve,
                            modifier = Modifier.height(20.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 3.dp, vertical = 0.dp),
                        ) { Text("÷2", fontSize = 9.sp, color = deckColor.copy(alpha = 0.7f)) }
                        androidx.compose.material3.TextButton(
                            onClick = onBpmDouble,
                            modifier = Modifier.height(20.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 3.dp, vertical = 0.dp),
                        ) { Text("×2", fontSize = 9.sp, color = deckColor.copy(alpha = 0.7f)) }
                    }
                    if (deckId == DeckId.B) {
                        Spacer(Modifier.width(4.dp))
                        val syncEnabled = deckState.bpm != null && masterBpmAvailable
                        Button(
                            onClick = onToggleSync,
                            enabled = syncEnabled,
                            modifier = Modifier.height(20.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (deckState.isSynced) deckColor else deckColor.copy(alpha = 0.35f),
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                            ),
                        ) {
                            Text(
                                text = "SYNC",
                                fontSize = 9.sp,
                                color = if (syncEnabled) Color.White
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onOpenLibrary,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "ライブラリから選択",
                    tint = deckColor,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Spacer(Modifier.height(2.dp))

        // --- 2カラム: LEFT=ループ(3行) / RIGHT=EQ+ピッチ+再生 ---
        Row(modifier = Modifier.fillMaxWidth()) {

            // LEFT: ManualLoopSection（Column・3行）
            ManualLoopSection(
                loop = deckState.loop,
                pendingLoopInSec = deckState.pendingLoopInSec,
                enabled = deckState.track != null,
                accentColor = deckColor,
                deckId = deckId,
                samplePads = samplePads,
                onSetLoopIn = onSetLoopIn,
                onSetLoopOut = onSetLoopOut,
                onDoubleLoop = onDoubleLoop,
                onHalveLoop = onHalveLoop,
                onDeactivateLoop = onDeactivateLoop,
                onNudgeLoopIn = onNudgeLoopIn,
                onNudgeLoopOut = onNudgeLoopOut,
                onCaptureToSample = onCaptureToSample,
                modifier = Modifier.weight(0.38f),
            )

            Spacer(Modifier.width(4.dp))

            // RIGHT: EQ + ピッチ（再生ボタンは下段に移動）
            Column(modifier = Modifier.weight(0.62f)) {
                EqSection(
                    eqState = deckState.eq,
                    accentColor = deckColor,
                    onLowChange  = onEqLowChange,
                    onMidChange  = onEqMidChange,
                    onHighChange = onEqHighChange,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(2.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "PITCH",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                        Text(
                            text = "%+.1f%%".format((deckState.pitchRatio - 1f) * 100f),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (deckState.pitchRatio != 1.0f) deckColor
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                    DoubleTapResetSlider(
                        value = deckState.pitchRatio,
                        onValueChange = onPitchChange,
                        onDoubleTap = { onPitchChange(1.0f) },
                        valueRange = 0.68f..1.32f,
                        enabled = deckState.track != null,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        // --- 下段: LEFT=再生ボタン+スクラッチ選択 / RIGHT=スクラッチコントロール ---
        val scratchEnabled = deckState.track != null
        Spacer(Modifier.height(2.dp))

        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
        ) {
            // LEFT: 再生/停止（上）+ スクラッチモード選択（下）
            Column(
                modifier = Modifier.weight(0.4f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp), color = deckColor)
                    } else {
                        IconButton(
                            onClick = onStop,
                            enabled = deckState.track != null,
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "停止",
                                modifier = Modifier.size(24.dp),
                                tint = if (deckState.track != null) deckColor
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = if (deckState.track != null) deckColor
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    shape = CircleShape,
                                )
                                .clickable(enabled = deckState.track != null) {
                                    if (deckState.isPlaying) onPause() else onPlay()
                                },
                        ) {
                            Icon(
                                imageVector = if (deckState.isPlaying) Icons.Default.Pause
                                else Icons.Default.PlayArrow,
                                contentDescription = if (deckState.isPlaying) "一時停止" else "再生",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp),
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "SCRATCH",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    )
                    ScratchMode.entries.forEach { mode ->
                        val label = when (mode) {
                            ScratchMode.CHOP_PAD       -> "CHOP"
                            ScratchMode.VELOCITY_WHEEL -> "WHEEL"
                            ScratchMode.FULL_STRIP     -> "STRIP"
                        }
                        val selected = mode == scratchMode
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (selected) deckColor
                                    else MaterialTheme.colorScheme.surfaceVariant,
                                )
                                .clickable { onScratchModeChange(mode) }
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) Color.White
                                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(4.dp))

            // RIGHT: スクラッチコントロール（残りスペースを全て使用）
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(0.6f).fillMaxHeight(),
            ) {
                when (scratchMode) {
                    ScratchMode.CHOP_PAD -> ChopPadScratch(
                        enabled        = scratchEnabled,
                        accentColor    = deckColor,
                        onScratchStart = onScratchStart,
                        onScratchMove  = onScratchMove,
                        onScratchEnd   = onScratchEnd,
                        modifier       = Modifier.fillMaxWidth().fillMaxHeight(),
                    )
                    ScratchMode.VELOCITY_WHEEL -> VelocityWheelScratch(
                        enabled        = scratchEnabled,
                        accentColor    = deckColor,
                        onScratchStart = onScratchStart,
                        onScratchMove  = onScratchMove,
                        onScratchEnd   = onScratchEnd,
                        modifier       = Modifier.fillMaxHeight().aspectRatio(1f),
                    )
                    ScratchMode.FULL_STRIP -> FullStripScratch(
                        enabled        = scratchEnabled,
                        accentColor    = deckColor,
                        onScratchStart = onScratchStart,
                        onScratchMove  = onScratchMove,
                        onScratchEnd   = onScratchEnd,
                        modifier       = Modifier.fillMaxWidth().fillMaxHeight(),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// T-505: EQセクション（High / Mid / Low ロータリーノブ）
// ---------------------------------------------------------------------------
@Composable
private fun EqSection(
    eqState: EqState,
    accentColor: Color,
    onLowChange: (Float) -> Unit,
    onMidChange: (Float) -> Unit,
    onHighChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        EqKnob("HIGH", eqState.highDb, accentColor, onHighChange) { onHighChange(0f) }
        EqKnob("MID",  eqState.midDb,  accentColor, onMidChange)  { onMidChange(0f)  }
        EqKnob("LOW",  eqState.lowDb,  accentColor, onLowChange)  { onLowChange(0f)  }
    }
}

@Composable
private fun EqKnob(
    label: String,
    gainDb: Float,
    accentColor: Color,
    onValueChange: (Float) -> Unit,
    onReset: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(1.dp))
        RotaryKnob(
            gainDb = gainDb,
            onValueChange = onValueChange,
            onReset = onReset,
            accentColor = accentColor,
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = if (gainDb <= -60f) "KILL" else "%+.1f".format(gainDb),
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = when {
                gainDb <= -60f -> MaterialTheme.colorScheme.error
                gainDb != 0f   -> accentColor
                else           -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            },
        )
    }
}

// RotaryKnob: 270° sweep。
//   7:30位置(Canvas 135°)がKill(-60dB)、12時(270°)が 0dB、4:30(45°)が +6dB。
//   非線形: Kill〜0dBが前半135°、0dB〜+6dBが後半135°。
//   ドラッグ上方向でブースト。ダブルタップで 0dB リセット。
@Composable
private fun RotaryKnob(
    gainDb: Float,
    onValueChange: (Float) -> Unit,
    onReset: () -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val currentGain   by rememberUpdatedState(gainDb)
    val bgArcColor     = Color.White.copy(alpha = 0.12f)
    val knobColor      = MaterialTheme.colorScheme.surfaceVariant
    val errorColor     = MaterialTheme.colorScheme.error
    val isKill         = gainDb <= -60f
    val activeColor    = if (isKill) errorColor else accentColor
    val indicatorColor = if (isKill) errorColor else Color.White

    Canvas(
        modifier = modifier
            .size(26.dp)
            .pointerInput(onReset) {
                var lastTapTime = 0L
                awaitEachGesture {
                    val down = awaitPointerEvent(PointerEventPass.Initial)
                    val downChanges = down.changes.filter { it.pressed && !it.previousPressed }
                    if (downChanges.isEmpty()) return@awaitEachGesture
                    val now = System.currentTimeMillis()
                    if (now - lastTapTime in 40L..400L) {
                        downChanges.forEach { it.consume() }
                        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                        onReset()
                        lastTapTime = 0L
                    } else {
                        lastTapTime = now
                    }
                }
            }
            .pointerInput(onValueChange) {
                detectDragGestures { _, dragAmount ->
                    val deltaDp = -dragAmount.y / density
                    val deltaDb = deltaDp * (66f / 200f)
                    onValueChange((currentGain + deltaDb).coerceIn(-60f, 6f))
                }
            },
    ) {
        val r      = size.minDimension / 2f
        val cx     = size.width  / 2f
        val cy     = size.height / 2f
        val stroke = r * 0.18f
        val arcR   = r - stroke / 2f

        drawArc(
            color      = bgArcColor,
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter  = false,
            topLeft    = Offset(cx - arcR, cy - arcR),
            size       = Size(arcR * 2f, arcR * 2f),
            style      = Stroke(width = stroke),
        )

        val activeSweep = if (gainDb <= 0f) ((gainDb + 60f) / 60f) * 135f
                          else              135f + (gainDb / 6f) * 135f
        if (activeSweep > 0f) {
            drawArc(
                color      = activeColor,
                startAngle = 135f,
                sweepAngle = activeSweep,
                useCenter  = false,
                topLeft    = Offset(cx - arcR, cy - arcR),
                size       = Size(arcR * 2f, arcR * 2f),
                style      = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }

        drawCircle(
            color  = knobColor,
            radius = r * 0.62f,
            center = Offset(cx, cy),
        )

        val indicatorAngleRad = (135f + activeSweep) * PI.toFloat() / 180f
        drawLine(
            color       = indicatorColor,
            start       = Offset(cx, cy),
            end         = Offset(
                cx + r * 0.42f * cos(indicatorAngleRad),
                cy + r * 0.42f * sin(indicatorAngleRad),
            ),
            strokeWidth = stroke * 0.55f,
            cap         = StrokeCap.Round,
        )
    }
}

// ---------------------------------------------------------------------------
// マニュアルループセクション
//   IDLE    : [IN →] ボタン
//   IN_SET  : [→ OUT] ボタン（ハイライト）
//   ACTIVE  : [÷½] [尺表示 = タップで解除] [×2]
// ---------------------------------------------------------------------------
@Composable
private fun ManualLoopSection(
    loop: LoopState?,
    pendingLoopInSec: Float?,
    enabled: Boolean,
    accentColor: Color,
    deckId: DeckId = DeckId.A,
    samplePads: List<SamplePadState> = emptyList(),
    onSetLoopIn: () -> Unit,
    onSetLoopOut: () -> Unit,
    onDoubleLoop: () -> Unit,
    onHalveLoop: () -> Unit,
    onDeactivateLoop: () -> Unit,
    onNudgeLoopIn: (Float) -> Unit = {},
    onNudgeLoopOut: (Float) -> Unit = {},
    onCaptureToSample: (slotId: Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = "LOOP",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )

        when {
            loop != null -> {
                // ACTIVE: 3行レイアウト
                val duration = loop.loopOutSec - loop.loopInSec
                val durationText = "%.1fs".format(duration.coerceAtLeast(0.05f))

                // 行1: [÷½] [尺=タップで解除] [×2]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    LoopChipButton("÷½", accentColor, enabled, onHalveLoop)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(accentColor.copy(alpha = 0.25f))
                            .clickable(enabled = enabled) { onDeactivateLoop() }
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = durationText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                        )
                    }
                    LoopChipButton("×2", accentColor, enabled, onDoubleLoop)
                }

                // 行2: [◀IN] [IN▶] [◀OUT] [OUT▶]
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    NudgeChipButton("◀IN",  accentColor, enabled) { onNudgeLoopIn(-LoopUseCase.NUDGE_SEC) }
                    NudgeChipButton("IN▶",  accentColor, enabled) { onNudgeLoopIn(+LoopUseCase.NUDGE_SEC) }
                    NudgeChipButton("◀OUT", accentColor, enabled) { onNudgeLoopOut(-LoopUseCase.NUDGE_SEC) }
                    NudgeChipButton("OUT▶", accentColor, enabled) { onNudgeLoopOut(+LoopUseCase.NUDGE_SEC) }
                }

                // 行3: → [P1] [P2] [P3] [P4]
                if (samplePads.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = "→",
                            fontSize = 9.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        )
                        samplePads.forEach { pad ->
                            val occupied = pad.isLoaded
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(
                                        if (occupied) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                                        else          accentColor.copy(alpha = 0.2f),
                                    )
                                    .clickable { onCaptureToSample(pad.id) }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = if (occupied) "P${pad.id + 1}●" else "P${pad.id + 1}",
                                    fontSize = 8.sp,
                                    color = if (occupied) MaterialTheme.colorScheme.tertiary
                                            else          accentColor.copy(alpha = 0.8f),
                                )
                            }
                        }
                    }
                }
            }
            pendingLoopInSec != null -> {
                // IN_SET: [→ OUT]（アクセントカラー強調）
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(accentColor)
                        .clickable(enabled = enabled) { onSetLoopOut() }
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "→ OUT",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
            }
            else -> {
                // IDLE: [IN →]
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(enabled = enabled) { onSetLoopIn() }
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                ) {
                    Text(
                        text = "IN →",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (enabled) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    )
                }
            }
        }
    }
}

@Composable
private fun LoopChipButton(
    label: String,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (enabled) accentColor.copy(alpha = 0.85f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
        )
    }
}

@Composable
private fun NudgeChipButton(
    label: String,
    accentColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 5.dp, vertical = 2.dp),
    ) {
        Text(
            text = label,
            fontSize = 8.sp,
            color = if (enabled) accentColor.copy(alpha = 0.75f)
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
        )
    }
}

// ---------------------------------------------------------------------------
// T-210: 波形ビュー（Canvas ベース再生位置インジケーター）
// ---------------------------------------------------------------------------
@Composable
fun WaveformView(
    playheadSec: Float,
    durationSec: Float,
    isPlaying: Boolean,
    accentColor: Color,
    loopState: LoopState? = null,
    pendingLoopInSec: Float? = null,
    onSeek: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val bgColor         = Color(0xFF1E1E1E)
    val waveColorPast   = accentColor.copy(alpha = 0.8f)
    val waveColorFuture = accentColor.copy(alpha = 0.25f)
    val playheadColor   = Color.White

    val seekModifier = if (onSeek != null && durationSec > 0f) {
        Modifier.pointerInput(onSeek) {
            detectDragGestures(
                onDragStart = { offset ->
                    onSeek((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                },
                onDrag = { change, _ ->
                    change.consume()
                    onSeek((change.position.x / size.width.toFloat()).coerceIn(0f, 1f))
                },
            )
        }
    } else Modifier

    Canvas(modifier = modifier.background(bgColor).then(seekModifier)) {
        val w = size.width
        val h = size.height
        val midY = h / 2f

        val progress = if (durationSec > 0f) (playheadSec / durationSec).coerceIn(0f, 1f) else 0f
        val playheadX = w * progress

        // 疑似波形（Sprint 8 で実波形に差し替え予定）
        val barCount = 80
        val barWidth = w / barCount
        val rng = java.util.Random(0x5EED_CAFE)

        for (i in 0 until barCount) {
            val x = i * barWidth + barWidth / 2f
            val amplitude = (0.2f + rng.nextFloat() * 0.8f) * (h / 2f)
            val color = if (x < playheadX) waveColorPast else waveColorFuture
            drawLine(
                color = color,
                start = Offset(x, midY - amplitude),
                end   = Offset(x, midY + amplitude),
                strokeWidth = (barWidth * 0.6f).coerceAtLeast(1f),
                cap = StrokeCap.Round,
            )
        }

        // IN点保留マーカー（破線縦線）
        if (durationSec > 0f && pendingLoopInSec != null) {
            val inX = w * (pendingLoopInSec / durationSec).coerceIn(0f, 1f)
            var y = 0f
            while (y < h) {
                drawLine(
                    color = accentColor.copy(alpha = 0.85f),
                    start = Offset(inX, y),
                    end   = Offset(inX, (y + 5f).coerceAtMost(h)),
                    strokeWidth = 2f,
                )
                y += 9f
            }
        }

        // ループ領域ハイライト
        if (durationSec > 0f && loopState != null) {
            val loopInX  = w * (loopState.loopInSec  / durationSec).coerceIn(0f, 1f)
            val loopOutX = w * (loopState.loopOutSec / durationSec).coerceIn(0f, 1f)
            drawRect(
                color    = accentColor.copy(alpha = 0.18f),
                topLeft  = Offset(loopInX, 0f),
                size     = Size(loopOutX - loopInX, h),
            )
            drawLine(color = accentColor.copy(alpha = 0.75f),
                start = Offset(loopInX, 0f), end = Offset(loopInX, h), strokeWidth = 2f)
            drawLine(color = accentColor.copy(alpha = 0.75f),
                start = Offset(loopOutX, 0f), end = Offset(loopOutX, h), strokeWidth = 2f)
        }

        if (durationSec > 0f) {
            drawLine(
                color = playheadColor,
                start = Offset(playheadX, 0f),
                end   = Offset(playheadX, h),
                strokeWidth = 2f,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// ダブルタップでリセット可能なスライダー
// Sliderは Main pass で DOWN を consume するため、親の detectTapGestures には届かない。
// Initial pass で2回目タップの DOWN/UP を両方 consume することで Slider の誤反応を防ぐ。
// ---------------------------------------------------------------------------
@Composable
private fun DoubleTapResetSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onDoubleTap: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var lastTapMs by remember { mutableLongStateOf(0L) }

    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        enabled = enabled,
        modifier = modifier.pointerInput(onDoubleTap) {
            awaitEachGesture {
                val down = awaitPointerEvent(PointerEventPass.Initial)
                val downChanges = down.changes.filter { it.pressed && !it.previousPressed }
                if (downChanges.isEmpty()) return@awaitEachGesture

                val now = System.currentTimeMillis()
                val dt  = now - lastTapMs

                if (dt in 40L..400L) {
                    // 2回目タップ: DOWN と UP を consume して Slider の tap-to-position を阻止
                    downChanges.forEach { it.consume() }
                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    onDoubleTap()
                    lastTapMs = 0L
                } else {
                    lastTapMs = now
                }
            }
        },
    )
}

// ---------------------------------------------------------------------------
// A案: チョップパッドスクラッチ
// 左ボタン押し続け = 逆再生 (-2.0x)、右ボタン押し続け = 順再生 (2.5x)
// ---------------------------------------------------------------------------
@Composable
private fun ChopPadScratch(
    enabled: Boolean,
    accentColor: Color,
    onScratchStart: () -> Unit,
    onScratchMove: (Float) -> Unit,
    onScratchEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        ChopButton(
            label = "◀",
            speed = -2.0f,
            accentColor = accentColor,
            enabled = enabled,
            isLeft = true,
            onScratchStart = onScratchStart,
            onScratchMove = onScratchMove,
            onScratchEnd = onScratchEnd,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
        ChopButton(
            label = "▶",
            speed = 2.5f,
            accentColor = accentColor,
            enabled = enabled,
            isLeft = false,
            onScratchStart = onScratchStart,
            onScratchMove = onScratchMove,
            onScratchEnd = onScratchEnd,
            modifier = Modifier.weight(1f).fillMaxHeight(),
        )
    }
}

@Composable
private fun ChopButton(
    label: String,
    speed: Float,
    accentColor: Color,
    enabled: Boolean,
    isLeft: Boolean,
    onScratchStart: () -> Unit,
    onScratchMove: (Float) -> Unit,
    onScratchEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isPressed by remember { mutableStateOf(false) }
    val shape = if (isLeft)
        RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
    else
        RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(shape)
            .background(
                if (isPressed && enabled) accentColor.copy(alpha = 0.5f)
                else accentColor.copy(alpha = 0.15f),
            )
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    onScratchStart()
                    onScratchMove(speed)
                    waitForUpOrCancellation()
                    isPressed = false
                    onScratchEnd()
                }
            },
    ) {
        Text(
            text = label,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (enabled) accentColor else accentColor.copy(alpha = 0.3f),
        )
    }
}

// ---------------------------------------------------------------------------
// B案: ベロシティーホイールスクラッチ（120dp 回転プラッター）
// 指の瞬間速度 → 速度倍率。250 dp/s ≈ 1.0x。回転アニメーション付き。
// ---------------------------------------------------------------------------
@Composable
private fun VelocityWheelScratch(
    enabled: Boolean,
    accentColor: Color,
    onScratchStart: () -> Unit,
    onScratchMove: (Float) -> Unit,
    onScratchEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var rotationDeg by remember { mutableFloatStateOf(0f) }

    val rimColor    = if (enabled) accentColor.copy(alpha = 0.8f) else Color.Gray.copy(alpha = 0.3f)
    val grooveColor = if (enabled) accentColor.copy(alpha = 0.12f) else Color.Gray.copy(alpha = 0.06f)
    val dotColor    = if (enabled) accentColor else Color.Gray.copy(alpha = 0.3f)

    Canvas(
        modifier = modifier
            .clip(CircleShape)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                val velocityTracker = VelocityTracker()
                awaitEachGesture {
                    velocityTracker.resetTracking()
                    val down = awaitFirstDown(requireUnconsumed = false)
                    onScratchStart()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        // 1dp = 1° の回転アニメーション
                        rotationDeg = (rotationDeg + (change.position.x - change.previousPosition.x) / density) % 360f
                        val velocity = velocityTracker.calculateVelocity()
                        val speed = (velocity.x / (density * 250f)).coerceIn(-4f, 4f)
                        onScratchMove(speed)
                        change.consume()
                        if (!change.pressed) break
                    }
                    onScratchEnd()
                }
            },
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r  = size.minDimension / 2f

        drawCircle(color = Color(0xFF1E1E1E), radius = r)
        drawCircle(color = rimColor, radius = r, style = Stroke(width = r * 0.10f))

        for (factor in listOf(0.78f, 0.56f, 0.34f)) {
            drawCircle(color = grooveColor, radius = r * factor, style = Stroke(width = r * 0.065f))
        }

        // 回転インジケーターライン
        val rad = Math.toRadians(rotationDeg.toDouble()).toFloat()
        drawLine(
            color       = dotColor.copy(alpha = 0.9f),
            start       = Offset(cx + r * 0.10f * cos(rad), cy + r * 0.10f * sin(rad)),
            end         = Offset(cx + r * 0.72f * cos(rad), cy + r * 0.72f * sin(rad)),
            strokeWidth = r * 0.08f,
            cap         = StrokeCap.Round,
        )
        drawCircle(
            color  = dotColor,
            radius = r * 0.09f,
            center = Offset(cx + r * 0.72f * cos(rad), cy + r * 0.72f * sin(rad)),
        )
        drawCircle(color = Color(0xFF444444), radius = r * 0.12f)
    }
}

// ---------------------------------------------------------------------------
// C案: フルワイドスクラッチストリップ
// 全幅の横スワイプエリア。指速度→速度倍率。タッチ中は針を表示。
// ---------------------------------------------------------------------------
@Composable
private fun FullStripScratch(
    enabled: Boolean,
    accentColor: Color,
    onScratchStart: () -> Unit,
    onScratchMove: (Float) -> Unit,
    onScratchEnd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var needleXPx by remember { mutableFloatStateOf(-1f) }
    var isActive  by remember { mutableStateOf(false) }

    val grooveColor = if (enabled) accentColor.copy(alpha = 0.08f) else Color.Gray.copy(alpha = 0.04f)
    val rimColor    = if (enabled) accentColor.copy(alpha = 0.4f)  else Color.Gray.copy(alpha = 0.15f)

    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                val velocityTracker = VelocityTracker()
                awaitEachGesture {
                    velocityTracker.resetTracking()
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isActive  = true
                    needleXPx = down.position.x
                    onScratchStart()
                    velocityTracker.addPosition(down.uptimeMillis, down.position)

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        velocityTracker.addPosition(change.uptimeMillis, change.position)
                        needleXPx = change.position.x
                        val velocity = velocityTracker.calculateVelocity()
                        val speed = (velocity.x / (density * 250f)).coerceIn(-4f, 4f)
                        onScratchMove(speed)
                        change.consume()
                        if (!change.pressed) break
                    }
                    isActive  = false
                    needleXPx = -1f
                    onScratchEnd()
                }
            },
    ) {
        val w = size.width
        val h = size.height

        drawRect(color = Color(0xFF1E1E1E))

        // グルーブライン
        for (i in 1..5) {
            val y = h * i / 6f
            drawLine(color = grooveColor, start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1.5f)
        }
        // 中心ライン
        drawLine(
            color = rimColor.copy(alpha = 0.5f),
            start = Offset(0f, h / 2f), end = Offset(w, h / 2f), strokeWidth = 1.5f,
        )
        // 左右端マーカー
        drawLine(color = rimColor, start = Offset(4f, 0f), end = Offset(4f, h), strokeWidth = 2f)
        drawLine(color = rimColor, start = Offset(w - 4f, 0f), end = Offset(w - 4f, h), strokeWidth = 2f)

        // 針（タッチ中のみ）
        if (isActive && needleXPx >= 0f) {
            val nx = needleXPx.coerceIn(0f, w)
            drawLine(
                color = accentColor.copy(alpha = 0.9f),
                start = Offset(nx, 0f), end = Offset(nx, h),
                strokeWidth = 3f,
            )
            drawCircle(color = accentColor, radius = h * 0.28f, center = Offset(nx, h / 2f))
        }
    }
}

// ---------------------------------------------------------------------------
// Sprint 8: WaveformStrip — 全幅波形エリア（デッキパネル外に配置）
// ---------------------------------------------------------------------------
@Composable
fun WaveformStrip(
    deckId: DeckId,
    deckState: DeckState,
    accentColor: Color,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val durationSec = deckState.track?.durationMs?.div(1000f) ?: 0f

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // デッキラベルバッジ
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(18.dp)
                .background(accentColor, CircleShape),
        ) {
            Text(
                text = if (deckId == DeckId.A) "A" else "B",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
            )
        }
        Spacer(Modifier.width(3.dp))
        // 再生位置
        Text(
            text = deckState.playheadSec.toTimeString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.width(28.dp),
            fontSize = 9.sp,
        )
        // 全幅波形
        WaveformView(
            playheadSec      = deckState.playheadSec,
            durationSec      = durationSec,
            isPlaying        = deckState.isPlaying,
            accentColor      = accentColor,
            loopState        = deckState.loop,
            pendingLoopInSec = deckState.pendingLoopInSec,
            onSeek           = if (deckState.track != null) onSeek else null,
            modifier = Modifier
                .weight(1f)
                .height(38.dp)
                .clip(RoundedCornerShape(5.dp)),
        )
        Spacer(Modifier.width(3.dp))
        // 総時間
        Text(
            text = durationSec.toTimeString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.width(28.dp),
            fontSize = 9.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
        )
    }
}

// ---------------------------------------------------------------------------
// ユーティリティ
// ---------------------------------------------------------------------------
internal fun Float.toTimeString(): String {
    val totalSec = this.toInt()
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%d:%02d".format(min, sec)
}
