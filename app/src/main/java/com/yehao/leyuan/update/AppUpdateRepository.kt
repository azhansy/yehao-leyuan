package com.yehao.leyuan.update

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

private val client = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(120, TimeUnit.SECONDS)
    .writeTimeout(20, TimeUnit.SECONDS)
    .build()

private fun normalizedBase(base: String): String {
    val t = base.trim()
    return if (t.endsWith("/")) t else "$t/"
}

suspend fun fetchRemoteUpdateInfo(baseUrl: String): RemoteUpdateInfo? = withContext(Dispatchers.IO) {
    val url = normalizedBase(baseUrl) + "update.json"
    val req = Request.Builder().url(url).get().build()
    runCatching {
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@use null
            val body = resp.body?.string() ?: return@use null
            parseUpdateJson(body)
        }
    }.getOrNull()
}

suspend fun downloadApkToFile(
    baseUrl: String,
    apkFileName: String,
    dest: File,
    onProgress: (Float) -> Unit,
): Boolean = withContext(Dispatchers.IO) {
    val url = normalizedBase(baseUrl) + apkFileName.trimStart('/')
    val req = Request.Builder().url(url).get().build()
    runCatching {
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return@use false
            val body = resp.body ?: return@use false
            val len = body.contentLength()
            dest.parentFile?.mkdirs()
            var lastPctInt = -1
            FileOutputStream(dest).use { out ->
                body.byteStream().buffered(8192).use { input ->
                    val buf = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val r = input.read(buf)
                        if (r == -1) break
                        out.write(buf, 0, r)
                        total += r
                        if (len > 0) {
                            val p = (total.toFloat() / len).coerceIn(0f, 1f)
                            val pctInt = (p * 100).toInt()
                            if (pctInt != lastPctInt) {
                                lastPctInt = pctInt
                                withContext(Dispatchers.Main) { onProgress(p) }
                            }
                        }
                    }
                }
            }
            withContext(Dispatchers.Main) { onProgress(1f) }
            true
        }
    }.getOrDefault(false)
}
