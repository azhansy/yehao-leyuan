package com.yehao.leyuan.audio

import android.content.Context
import androidx.core.content.edit

private const val PREFS = "tts_voice_prefs"
private const val KEY_PROFILE = "voice_profile"

/** 有道词典发音口音（dictvoice type：1 英音，2 美音） */
enum class TtsVoiceProfile(
    val storageKey: String,
    val labelZh: String,
    val descriptionZh: String,
) {
    ENGLISH_UK(
        storageKey = "en_uk_female",
        labelZh = "英式",
        descriptionZh = "有道词典 · 英音",
    ),
    ENGLISH_US(
        storageKey = "en_us",
        labelZh = "美式",
        descriptionZh = "有道词典 · 美音",
    ),
    ;

    companion object {
        fun fromStorageKey(key: String?): TtsVoiceProfile =
            when (key) {
                ENGLISH_UK.storageKey -> ENGLISH_UK
                ENGLISH_US.storageKey -> ENGLISH_US
                "system" -> ENGLISH_US
                else -> ENGLISH_US
            }
    }
}

class TtsVoicePrefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getProfile(): TtsVoiceProfile =
        TtsVoiceProfile.fromStorageKey(sp.getString(KEY_PROFILE, null))

    fun setProfile(profile: TtsVoiceProfile) {
        sp.edit { putString(KEY_PROFILE, profile.storageKey) }
    }
}
