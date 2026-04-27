package com.djapp.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// DJアプリは暗い環境での使用を想定し、ダークテーマを基本とする
private val DjDarkColorScheme = darkColorScheme(
    primary         = Color(0xFFE53935),   // 赤：アクティブ要素（再生・Sync ON等）
    onPrimary       = Color(0xFFFFFFFF),
    secondary       = Color(0xFF1E88E5),   // 青：デッキB系の強調色
    onSecondary     = Color(0xFFFFFFFF),
    background      = Color(0xFF121212),   // 黒背景
    onBackground    = Color(0xFFE0E0E0),
    surface         = Color(0xFF1E1E1E),   // カード・パネル背景
    onSurface       = Color(0xFFE0E0E0),
)

@Composable
fun DjAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DjDarkColorScheme,
        typography  = Typography,
        content     = content
    )
}
