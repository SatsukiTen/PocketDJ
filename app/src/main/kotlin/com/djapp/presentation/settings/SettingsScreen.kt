package com.djapp.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.djapp.domain.model.CrossfaderCurve

/**
 * T-307: 設定画面 — クロスフェーダーカーブ切替 UI（AC-003-06〜09）。
 */
@Composable
fun SettingsScreen(
    crossfaderCurve: CrossfaderCurve,
    onCurveChange: (CrossfaderCurve) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "設定",
            style = MaterialTheme.typography.titleLarge,
        )

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        Text(
            text = "クロスフェーダーカーブ",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        Column(Modifier.selectableGroup()) {
            CrossfaderCurveOption(
                label = "Linear",
                description = "移動量に比例して音量変化（スクラッチ向き）",
                selected = crossfaderCurve == CrossfaderCurve.LINEAR,
                onClick = { onCurveChange(CrossfaderCurve.LINEAR) },
            )
            CrossfaderCurveOption(
                label = "Equal Power",
                description = "中央付近でゆっくり変化（なめらかなミックス向き）",
                selected = crossfaderCurve == CrossfaderCurve.EQUAL_POWER,
                onClick = { onCurveChange(CrossfaderCurve.EQUAL_POWER) },
            )
        }
    }
}

@Composable
private fun CrossfaderCurveOption(
    label: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
