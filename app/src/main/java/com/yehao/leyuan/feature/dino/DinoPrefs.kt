package com.yehao.leyuan.feature.dino

import android.content.Context

private const val PREFS_NAME = "dino_run_prefs"
private const val KEY_LEVEL = "level"

class DinoPrefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLevel(): Int = sp.getInt(KEY_LEVEL, 1).coerceIn(1, 9)

    fun setLevel(level: Int) {
        sp.edit().putInt(KEY_LEVEL, level.coerceIn(1, 9)).apply()
    }
}
