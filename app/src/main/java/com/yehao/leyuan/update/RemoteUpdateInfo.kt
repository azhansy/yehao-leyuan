package com.yehao.leyuan.update

import org.json.JSONObject

data class RemoteUpdateInfo(
    val versionCode: Int,
    val versionName: String?,
    val apkFileName: String,
)

fun parseUpdateJson(json: String): RemoteUpdateInfo? = runCatching {
    val o = JSONObject(json)
    RemoteUpdateInfo(
        versionCode = o.getInt("versionCode"),
        versionName = when {
            o.isNull("versionName") -> null
            else -> o.optString("versionName").takeIf { it.isNotEmpty() }
        },
        apkFileName = o.getString("apkFileName"),
    )
}.getOrNull()
