package com.djapp.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DjDarkColorScheme = darkColorScheme(
    background               = Color(0xFF131313),
    onBackground             = Color(0xFFE5E2E1),
    surface                  = Color(0xFF131313),
    onSurface                = Color(0xFFE5E2E1),
    surfaceVariant           = Color(0xFF353534),
    onSurfaceVariant         = Color(0xFFBDC8D0),
    surfaceContainerLowest   = Color(0xFF0E0E0E),
    surfaceContainerLow      = Color(0xFF1C1B1B),
    surfaceContainer         = Color(0xFF201F1F),
    surfaceContainerHigh     = Color(0xFF2A2A2A),
    surfaceContainerHighest  = Color(0xFF353534),
    primary                  = Color(0xFF9ADBFF),
    onPrimary                = Color(0xFF003548),
    primaryContainer         = Color(0xFF4FC3F7),
    onPrimaryContainer       = Color(0xFF004E69),
    secondary                = Color(0xFF71D7CD),
    onSecondary              = Color(0xFF003733),
    secondaryContainer       = Color(0xFF32A097),
    onSecondaryContainer     = Color(0xFF00302C),
    tertiary                 = Color(0xFFF8C0FF),
    onTertiary               = Color(0xFF4B1B58),
    tertiaryContainer        = Color(0xFFDFA2E8),
    onTertiaryContainer      = Color(0xFF663471),
    error                    = Color(0xFFFFB4AB),
    onError                  = Color(0xFF690005),
    errorContainer           = Color(0xFF93000A),
    onErrorContainer         = Color(0xFFFFDAD6),
    outline                  = Color(0xFF889299),
    outlineVariant           = Color(0xFF3E484F),
)

@Composable
fun DjAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DjDarkColorScheme,
        typography  = Typography,
        content     = content
    )
}
