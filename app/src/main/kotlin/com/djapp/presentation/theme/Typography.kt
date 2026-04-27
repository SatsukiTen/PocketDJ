package com.djapp.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.djapp.R

// Space Grotesk — res/font/ にバンドルされた TTF を使用
val SpaceGroteskFamily: FontFamily = FontFamily(
    Font(R.font.space_grotesk_medium,   weight = FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, weight = FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold,     weight = FontWeight.Bold),
)

val Typography = Typography(
    // BPM・再生時間などの技術値（Space Grotesk 18sp Medium）
    titleLarge = TextStyle(
        fontFamily    = SpaceGroteskFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 18.sp,
        lineHeight    = 24.sp,
        letterSpacing = 0.05.sp,
    ),
    // セクションラベル（LOOP / PITCH / SCRATCH）
    labelLarge = TextStyle(
        fontFamily    = SpaceGroteskFamily,
        fontWeight    = FontWeight.SemiBold,
        fontSize      = 12.sp,
        letterSpacing = 2.sp,
    ),
    // チップ内テキスト・ノブ値
    labelMedium = TextStyle(
        fontFamily    = SpaceGroteskFamily,
        fontWeight    = FontWeight.Medium,
        fontSize      = 10.sp,
        letterSpacing = 0.5.sp,
    ),
    // ライブラリのアーティスト名・説明文
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
    ),
)
