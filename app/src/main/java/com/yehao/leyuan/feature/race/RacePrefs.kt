package com.yehao.leyuan.feature.race

import android.content.Context

private const val PREFS_NAME = "lane_race_prefs"
private const val KEY_LEVEL = "game_level"
private const val KEY_HIGH_SCORE = "high_score"

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
}
