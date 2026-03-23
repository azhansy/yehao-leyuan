package com.yehao.leyuan.ui.theme

import android.app.Activity
import android.graphics.Color as AndroidColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

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
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        window.statusBarColor = AndroidColor.TRANSPARENT
        window.navigationBarColor = AndroidColor.TRANSPARENT
        val c = WindowCompat.getInsetsController(window, view)
        c.isAppearanceLightStatusBars = true
        c.isAppearanceLightNavigationBars = true
    }
    MaterialTheme(
        colorScheme = LightColors,
        typography = YehaoTypography,
        content = content,
    )
}
