package com.yehao.leyuan.audio

import android.content.Context

private const val PREFS = "tts_voice_prefs"
private const val KEY_PROFILE = "voice_profile"

enum class TtsVoiceProfile(
    val storageKey: String,
    val labelZh: String,
    val descriptionZh: String,
) {
    SYSTEM_DEFAULT(
        storageKey = "system",
        labelZh = "跟随系统",
        descriptionZh = "使用系统默认语言与朗读音色，部分小米等机型更稳定",
    ),
    ENGLISH_UK_FEMALE(
        storageKey = "en_uk_female",
        labelZh = "英语 · 英式女声",
        descriptionZh = "优先英音女声（与原设置接近）",
    ),
    ENGLISH_US(
        storageKey = "en_us",
        labelZh = "英语 · 美式",
        descriptionZh = "美式英语发音",
    ),
    ;

    companion object {
        fun fromStorageKey(key: String?): TtsVoiceProfile =
            entries.find { it.storageKey == key } ?: SYSTEM_DEFAULT
    }
}

class TtsVoicePrefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getProfile(): TtsVoiceProfile =
        TtsVoiceProfile.fromStorageKey(sp.getString(KEY_PROFILE, null))

    fun setProfile(profile: TtsVoiceProfile) {
        sp.edit().putString(KEY_PROFILE, profile.storageKey).apply()
    }
}
