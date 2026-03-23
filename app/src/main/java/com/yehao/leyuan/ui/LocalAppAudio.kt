package com.yehao.leyuan.ui

import androidx.compose.runtime.staticCompositionLocalOf
import com.yehao.leyuan.audio.AppAudio

val LocalAppAudio = staticCompositionLocalOf<AppAudio> {
    error("AppAudio not provided")
}
