package com.djapp.presentation.mixer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.djapp.domain.model.DeckId
import com.djapp.domain.model.LoopState
import com.djapp.domain.model.SamplePadState
import com.djapp.domain.model.SamplePlayMode

/**
 * サンプルパッド行（4スロット）。
 * デッキ行とクロスフェーダーの間に配置する。
 *
 * ・LOOP ACTIVE 時: キャプチャ操作は DeckPanel 側から onCaptureToSample() で行う
 * ・各パッドタップ → 再生 / 停止トグル
 * ・モードアイコン（▶/↺）タップ → SINGLE / LOOP 切替
 */
@Composable
fun SamplePadRow(
    pads: List<SamplePadState>,
    onTogglePlay: (slotId: Int) -> Unit,
    onTogglePlayMode: (slotId: Int) -> Unit,
    onClear: (slotId: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "PAD",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
            modifier = Modifier.padding(end = 2.dp),
        )
        pads.forEach { pad ->
            SamplePadButton(
                pad = pad,
                onTogglePlay = { onTogglePlay(pad.id) },
                onTogglePlayMode = { onTogglePlayMode(pad.id) },
                onClear = { onClear(pad.id) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SamplePadButton(
    pad: SamplePadState,
    onTogglePlay: () -> Unit,
    onTogglePlayMode: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val padColor = MaterialTheme.colorScheme.tertiary

    val bgColor = when {
        !pad.isLoaded -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        pad.isPlaying -> padColor.copy(alpha = 0.85f)
        else          -> padColor.copy(alpha = 0.25f)
    }
    val textColor = when {
        !pad.isLoaded -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        pad.isPlaying -> Color.White
        else          -> padColor
    }

    Box(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .clickable(enabled = pad.isLoaded) { onTogglePlay() },
        contentAlignment = Alignment.Center,
    ) {
        if (!pad.isLoaded) {
            Text(
                text = "${pad.id + 1}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // 再生モードアイコン（タップで切替）
                Text(
                    text = if (pad.playMode == SamplePlayMode.SINGLE) "▶" else "↺",
                    fontSize = 9.sp,
                    color = textColor.copy(alpha = 0.8f),
                    modifier = Modifier.clickable { onTogglePlayMode() },
                )
                // 尺
                Text(
                    text = "%.1fs".format(pad.durationSec),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.Center,
                )
                // スロット番号 + クリアボタン（[P1 ×] スタイル）
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = "P${pad.id + 1}",
                        fontSize = 8.sp,
                        color = textColor.copy(alpha = 0.65f),
                    )
                    Text(
                        text = "×",
                        fontSize = 8.sp,
                        color = textColor.copy(alpha = 0.65f),
                        modifier = Modifier
                            .size(14.dp)
                            .clickable { onClear() },
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// DeckPanel の LOOP ACTIVE 状態に表示するキャプチャ行
// ---------------------------------------------------------------------------
@Composable
fun SampleCaptureRow(
    pads: List<SamplePadState>,
    deckId: DeckId,
    loop: LoopState,
    onCapture: (slotId: Int, deckId: DeckId, loop: LoopState) -> Unit,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = "→",
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        )
        pads.forEach { pad ->
            val occupied = pad.isLoaded
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        if (occupied) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                        else          accentColor.copy(alpha = 0.2f),
                    )
                    .clickable { onCapture(pad.id, deckId, loop) }
                    .padding(horizontal = 5.dp, vertical = 2.dp),
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
