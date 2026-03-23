package com.yehao.leyuan.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = SkyBlue,
    onPrimary = Color.White,
    secondary = SunnyYellow,
    onSecondary = DeepText,
    tertiary = GrassGreen,
    background = CreamBackground,
    surface = Color.White,
    onBackground = DeepText,
    onSurface = DeepText
)

@Composable
fun YehaoLeyuanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = YehaoTypography,
        content = content
    )
}
