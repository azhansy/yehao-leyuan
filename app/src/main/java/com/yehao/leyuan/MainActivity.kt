package com.yehao.leyuan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.yehao.leyuan.audio.AppAudio
import com.yehao.leyuan.ui.LocalAppAudio
import com.yehao.leyuan.ui.YehaoLeyuanApp
import com.yehao.leyuan.ui.theme.YehaoLeyuanTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val audio = remember { AppAudio(this) }
            DisposableEffect(Unit) {
                onDispose { audio.release() }
            }
            YehaoLeyuanTheme {
                CompositionLocalProvider(LocalAppAudio provides audio) {
                    YehaoLeyuanApp()
                }
            }
        }
    }
}
