package com.yehao.leyuan.feature.race

import android.content.Context

private const val PREFS_NAME = "lane_race_prefs"
private const val KEY_LEVEL = "game_level"
private const val KEY_HIGH_SCORE = "high_score"
/** 点左/右时是否朗读 left / right，默认关闭 */
private const val KEY_PLAY_STEER_TTS = "play_steer_tts"

class RacePrefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLevel(): Int = sp.getInt(KEY_LEVEL, 1).coerceIn(1, 9)

    fun setLevel(level: Int) {
        sp.edit().putInt(KEY_LEVEL, level.coerceIn(1, 9)).apply()
    }

    fun getHighScore(): Int = sp.getInt(KEY_HIGH_SCORE, 0)

    fun updateHighScore(score: Int) {
        val cur = getHighScore()
        if (score > cur) sp.edit().putInt(KEY_HIGH_SCORE, score).apply()
    }

    fun getPlaySteerTts(): Boolean = sp.getBoolean(KEY_PLAY_STEER_TTS, false)

    fun setPlaySteerTts(enabled: Boolean) {
        sp.edit().putBoolean(KEY_PLAY_STEER_TTS, enabled).apply()
    }
}
