package com.djapp.presentation.mixer

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.withTimeoutOrNull

/**
 * T-306: クロスフェーダー UI + スクラッチモード切替
 */
@Composable
fun CrossfaderPanel(
    position: Float,
    onPositionChange: (Float) -> Unit,
    onDoubleTap: () -> Unit,
    latencyMs: Double = 0.0,
    xRunCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // --- クロスフェーダースライダー行 ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "A",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .pointerInput(Unit) {
                        val doubleTapTimeoutMs = viewConfiguration.doubleTapTimeoutMillis

                        awaitEachGesture {
                            var firstDown: PointerInputChange? = null
                            while (firstDown == null) {
                                firstDown = awaitPointerEvent(PointerEventPass.Initial)
                                    .changes.firstOrNull { it.pressed && !it.previousPressed }
                            }
                            val firstDownPos = firstDown.position
                            val firstDownId  = firstDown.id

                            var isDrag = false
                            firstTapUp@ while (true) {
                                for (ch in awaitPointerEvent(PointerEventPass.Initial).changes) {
                                    if (ch.id != firstDownId) continue
                                    if (!ch.pressed) break@firstTapUp
                                    val dist = (ch.position - firstDownPos).getDistance()
                                    if (dist > viewConfiguration.touchSlop) {
                                        isDrag = true; break@firstTapUp
                                    }
                                }
                            }
                            if (isDrag) return@awaitEachGesture

                            val secondDown: PointerInputChange = withTimeoutOrNull(doubleTapTimeoutMs) {
                                var down: PointerInputChange? = null
                                while (down == null) {
                                    down = awaitPointerEvent(PointerEventPass.Initial)
                                        .changes.firstOrNull { it.pressed && !it.previousPressed }
                                }
                                down
                            } ?: return@awaitEachGesture

                            secondDown.consume()
                            val secondId = secondDown.id

                            secondTapUp@ while (true) {
                                for (ch in awaitPointerEvent(PointerEventPass.Initial).changes) {
                                    if (ch.id != secondId) continue
                                    ch.consume()
                                    if (!ch.pressed) break@secondTapUp
                                }
                            }

                            onDoubleTap()
                        }
                    },
            ) {
                Slider(
                    value = position,
                    onValueChange = onPositionChange,
                    valueRange = 0f..1f,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Text(
                text = "B",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        // T-802: レイテンシ表示（0.0ms の間は非表示）
        if (latencyMs > 0.0) {
            val xRunColor = if (xRunCount > 0) Color(0xFFFF6B6B) else MaterialTheme.colorScheme.onSurfaceVariant
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 0.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "LATENCY %.1f ms  |  XRUN %d".format(latencyMs, xRunCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = xRunColor,
                )
            }
        }
    }
}

