package com.yehao.leyuan.feature.letter

import android.content.Context

private const val PREFS_NAME = "letter_puzzle_prefs"
private const val KEY_LEVEL = "game_level"

class LetterPuzzlePrefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLevel(): Int = sp.getInt(KEY_LEVEL, 1).coerceIn(1, 9)

    fun setLevel(level: Int) {
        sp.edit().putInt(KEY_LEVEL, level.coerceIn(1, 9)).apply()
    }
}
