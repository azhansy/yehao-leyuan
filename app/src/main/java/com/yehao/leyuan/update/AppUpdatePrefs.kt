package com.yehao.leyuan.update

import android.content.Context

class AppUpdatePrefs(context: Context) {
    private val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var wifiOnlyDownload: Boolean
        get() = sp.getBoolean(KEY_WIFI_ONLY, false)
        set(value) {
            sp.edit().putBoolean(KEY_WIFI_ONLY, value).apply()
        }

    /** Dismissed remote versionCode; skip prompts until server exceeds this. */
    var ignoredVersionCode: Int
        get() = sp.getInt(KEY_IGNORED_VERSION, -1)
        set(value) {
            sp.edit().putInt(KEY_IGNORED_VERSION, value).apply()
        }

    companion object {
        private const val PREFS = "app_update"
        private const val KEY_WIFI_ONLY = "wifi_only_download"
        private const val KEY_IGNORED_VERSION = "ignored_version_code"
    }
}
