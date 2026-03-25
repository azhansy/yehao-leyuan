package com.yehao.leyuan.audio

import android.content.Context
import android.os.Build
import android.util.Log
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.ArrayDeque
import java.util.concurrent.ConcurrentHashMap
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class AppAudio(context: Context) {
    private val appContext = context.applicationContext
    private val ttsPrefs = TtsVoicePrefs(appContext)
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val toneMusic = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    private val toneNotification = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)

    private val mainHandler = Handler(Looper.getMainLooper())
    private val youdaoHttpExecutor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "youdao-tts").apply { isDaemon = true }
    }
    private val youdaoHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .build()

    /** flush 时自增，丢弃已过时下载回调 */
    private val youdaoPlaySession = AtomicInteger(0)

    private var youdaoMediaPlayer: MediaPlayer? = null

    private data class YoudaoQueuedPhrase(
        val text: String,
        /** 为 false 时（如家庭英语）有道失败不拆成单词，只拆句/逗号/过长时的词组短句 */
        val allowWordTokenFallback: Boolean = true,
    )

    private val youdaoQueue: ArrayDeque<YoudaoQueuedPhrase> = ArrayDeque()
    private var youdaoDraining = false

    private var currentProfile: TtsVoiceProfile = ttsPrefs.getProfile()

    /** 有道英文 TTS 持久缓存（按短语 + 英音/美音 分文件），在 filesDir 下 */
    private val youdaoTtsCacheDir: File by lazy {
        File(appContext.filesDir, "youdao_en_tts").apply { mkdirs() }
    }

    private val youdaoCacheKeyLocks = ConcurrentHashMap<String, Any>()

    private fun lockForYoudaoCacheKey(cacheKey: String): Any =
        youdaoCacheKeyLocks.computeIfAbsent(cacheKey) { Any() }

    private fun sha256HexForYoudaoPhrase(phrase: String, voiceType: Int): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(phrase.toByteArray(StandardCharsets.UTF_8))
        md.update(byteArrayOf(0))
        md.update(voiceType.toString().toByteArray(StandardCharsets.UTF_8))
        return md.digest().joinToString("") { b -> "%02x".format(b) }
    }

    private fun youdaoCachedAudioFile(phrase: String, voiceType: Int): File {
        val name = "${sha256HexForYoudaoPhrase(phrase, voiceType)}.mp3"
        return File(youdaoTtsCacheDir, name)
    }

    private fun isAcceptableYoudaoCacheFile(f: File): Boolean =
        f.isFile && f.length() >= MIN_YOUDAO_AUDIO_BYTES && looksLikePlayableYoudaoAudio(f)

    fun applyVoiceProfile(profile: TtsVoiceProfile) {
        ttsPrefs.setProfile(profile)
        currentProfile = profile
    }

    private fun youdaoType(): Int = when (currentProfile) {
        TtsVoiceProfile.ENGLISH_UK -> 1
        TtsVoiceProfile.ENGLISH_US -> 2
    }

    /**
     * 入队播放有道发音。[flush] 为 true 时清空队列并停止当前播放。
     */
    private fun enqueueYoudao(text: String, flush: Boolean, allowWordTokenFallback: Boolean = true) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (flush) {
            youdaoPlaySession.incrementAndGet()
            mainHandler.removeCallbacksAndMessages(null)
            youdaoMediaPlayer?.stop()
            youdaoMediaPlayer?.release()
            youdaoMediaPlayer = null
            youdaoQueue.clear()
            youdaoDraining = false
        }
        youdaoQueue.addLast(
            YoudaoQueuedPhrase(
                text = normalizePhraseForYoudao(trimmed),
                allowWordTokenFallback = allowWordTokenFallback,
            ),
        )
        if (!youdaoDraining) playNextYoudao()
    }

    private fun playNextYoudao() {
        if (youdaoQueue.isEmpty()) {
            youdaoDraining = false
            return
        }
        youdaoDraining = true
        val queued = youdaoQueue.removeFirst()
        val phrase = queued.text
        val allowWordFallback = queued.allowWordTokenFallback
        val sessionAtDownload = youdaoPlaySession.get()
        val voiceType = youdaoType()

        youdaoHttpExecutor.execute {
            val cacheFile = try {
                downloadYoudaoAudioToCache(phrase, voiceType)
            } catch (_: Exception) {
                null
            }
            // 首次落盘后立即起播偶发解码不到完整首帧；写完后再短暂 settle
            if (cacheFile != null) {
                try {
                    Thread.sleep(YOUDAO_FILE_SETTLE_MS)
                } catch (_: InterruptedException) {
                }
            }

            mainHandler.post {
                if (sessionAtDownload != youdaoPlaySession.get()) {
                    return@post
                }

                // 与落盘校验一致：pronounce/base 多为 WAV，仅认 MP3 会导致缓存有效但永远不播（电脑上可播同一文件）
                val fileOk = cacheFile != null &&
                    cacheFile.exists() &&
                    cacheFile.length() >= MIN_YOUDAO_AUDIO_BYTES &&
                    looksLikePlayableYoudaoAudio(cacheFile)

                if (!fileOk) {
                    val chunks = expandFailedYoudaoPhrase(phrase, allowWordFallback)
                    if (chunks.size > 1) {
                        chunks.asReversed().forEach { chunk ->
                            youdaoQueue.addFirst(
                                YoudaoQueuedPhrase(
                                    text = normalizePhraseForYoudao(chunk),
                                    allowWordTokenFallback = allowWordFallback,
                                ),
                            )
                        }
                        playNextYoudao()
                        return@post
                    }
                    playSoftClick()
                    playNextYoudao()
                    return@post
                }

                val audioFile = cacheFile!!
                try {
                    youdaoMediaPlayer?.release()
                    youdaoMediaPlayer = null

                    val path = audioFile.absolutePath
                    val mp = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build(),
                        )
                        setDataSource(path)
                        setOnPreparedListener { p ->
                            // prepare 完成后仍稍等再 start，减少「文件刚可见但解码器未就绪」导致的无声/截断
                            mainHandler.postDelayed({
                                if (sessionAtDownload != youdaoPlaySession.get()) {
                                    try {
                                        p.release()
                                    } catch (_: Exception) {
                                    }
                                    return@postDelayed
                                }
                                if (youdaoMediaPlayer !== p) {
                                    try {
                                        p.release()
                                    } catch (_: Exception) {
                                    }
                                    return@postDelayed
                                }
                                try {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        p.seekTo(0L, MediaPlayer.SEEK_CLOSEST_SYNC)
                                    } else {
                                        @Suppress("DEPRECATION")
                                        p.seekTo(0)
                                    }
                                } catch (_: Exception) {
                                }
                                try {
                                    p.start()
                                } catch (_: Exception) {
                                    try {
                                        p.release()
                                    } catch (_: Exception) {
                                    }
                                    if (youdaoMediaPlayer === p) youdaoMediaPlayer = null
                                    playSoftClick()
                                    playNextYoudao()
                                }
                            }, YOUDAO_START_AFTER_PREPARED_MS)
                        }
                        setOnCompletionListener { player ->
                            try {
                                player.release()
                            } catch (_: Exception) {
                            }
                            if (youdaoMediaPlayer === player) youdaoMediaPlayer = null
                            playNextYoudao()
                        }
                        setOnErrorListener { player, _, _ ->
                            try {
                                player.release()
                            } catch (_: Exception) {
                            }
                            if (youdaoMediaPlayer === player) youdaoMediaPlayer = null
                            playSoftClick()
                            playNextYoudao()
                            true
                        }
                        prepareAsync()
                    }
                    youdaoMediaPlayer = mp
                } catch (_: Exception) {
                    playSoftClick()
                    playNextYoudao()
                }
            }
        }
    }

    /**
     * 英文里常见 `hello,my` 无空格，按空格分词会丢掉 `my`。在字母与标点之间补空格，便于整句/拆词。
     */
    private fun normalizePhraseForYoudao(s: String): String {
        if (s.isBlank()) return s
        var t = s.trim().replace(Regex("\\s+"), " ")
        repeat(4) {
            val n = t.replace(Regex("([a-zA-Z])([,;.!?:])([a-zA-Z])"), "$1$2 $3")
            if (n == t) return@repeat
            t = n
        }
        return t.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * 按「空白 + 英文标点」拆成单词依次请求 dictvoice（仅 allowWordTokenFallback 为 true 时的最后手段）。
     * 整句失败通常是有道对长 query/整句返回非音频而非「空格不能读」；空格经 URL 编码后服务端可解析。
     */
    private fun tokenizeForYoudaoTts(phrase: String): List<String> {
        val normalized = normalizePhraseForYoudao(phrase)
        val delimiters = Regex("[\\s,;.!?:：，。！？…]+")
        return normalized.split(delimiters)
            .map { raw ->
                raw.trim()
                    .trim('"', '\'')
                    .trimEnd(',', '.', '!', '?', ':', ';', '，', '。', '！', '？', '…')
                    .trimStart('"', '\'')
            }
            .filter { it.isNotEmpty() }
    }

    private fun splitSentencesForYoudaoFallback(normalized: String): List<String> =
        normalized.split(Regex("(?<=[.!?…])\\s+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun splitCommaClausesForYoudaoFallback(normalized: String): List<String> =
        normalized.split(Regex("[,;]\\s*"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /**
     * 在空格处切分过长整段，每段仍是连续词组（非单字蹦词）。有道对很长 query 常返回非 MP3。
     */
    private fun splitLongPhraseByWordBoundary(normalized: String, maxChars: Int): List<String> {
        if (normalized.length <= maxChars) return listOf(normalized)
        val words = normalized.split(' ').filter { it.isNotEmpty() }
        if (words.size <= 1) return listOf(normalized)
        val out = mutableListOf<String>()
        var cur = StringBuilder()
        for (w in words) {
            val needSpace = cur.isNotEmpty()
            val addLen = (if (needSpace) 1 else 0) + w.length
            if (needSpace && cur.length + addLen > maxChars) {
                out.add(cur.toString())
                cur = StringBuilder(w)
            } else if (needSpace) {
                cur.append(' ').append(w)
            } else {
                cur.append(w)
            }
        }
        if (cur.isNotEmpty()) out.add(cur.toString())
        return if (out.size > 1) out else listOf(normalized)
    }

    /**
     * 按空格切成多段，每段最多 [maxWordsPerChunk] 个词（仍为多词短语，非单字）。
     * 有道 dictvoice 对较长整句常 HTTP 500，短一些易成功。
     */
    private fun splitIntoWordGroupsMaxWords(normalized: String, maxWordsPerChunk: Int): List<String> {
        if (maxWordsPerChunk < 2) return listOf(normalized)
        val words = normalized.split(' ').filter { it.isNotEmpty() }
        if (words.size <= maxWordsPerChunk) return listOf(normalized)
        val out = mutableListOf<String>()
        var i = 0
        while (i < words.size) {
            val end = kotlin.math.min(i + maxWordsPerChunk, words.size)
            out.add(words.subList(i, end).joinToString(" "))
            i = end
        }
        return out
    }

    /**
     * 有道整段下载失败时的降级：先按句、再按逗号分块；再按字数/词数组切短；
     * [allowWordTokenFallback] 为 true 时最后再拆单词。
     */
    private fun expandFailedYoudaoPhrase(phrase: String, allowWordTokenFallback: Boolean): List<String> {
        val n = normalizePhraseForYoudao(phrase)
        if (n.isEmpty()) return emptyList()
        val sentences = splitSentencesForYoudaoFallback(n)
        if (sentences.size > 1) return sentences
        val clauses = splitCommaClausesForYoudaoFallback(n)
        if (clauses.size > 1) return clauses
        val byLen = splitLongPhraseByWordBoundary(n, YOUDAO_PHRASE_SOFT_MAX_CHARS)
        if (byLen.size > 1) return byLen
        val maxW =
            if (allowWordTokenFallback) YOUDAO_MAX_WORDS_PER_CHUNK_GAME else YOUDAO_MAX_WORDS_PER_CHUNK_FAMILY
        val byWords = splitIntoWordGroupsMaxWords(n, maxW)
        if (byWords.size > 1) {
            Log.d(TAG, "youdao expand word-groups (max $maxW words): ${byWords.size} chunks from [$n]")
            return byWords
        }
        return if (allowWordTokenFallback) tokenizeForYoudaoTts(n) else listOf(n)
    }

    private fun looksLikeWavAudio(file: File): Boolean {
        if (file.length() < 12) return false
        val buf = ByteArray(12)
        FileInputStream(file).use { it.read(buf) }
        return buf[0] == 'R'.code.toByte() && buf[1] == 'I'.code.toByte() &&
            buf[2] == 'F'.code.toByte() && buf[3] == 'F'.code.toByte() &&
            buf[8] == 'W'.code.toByte() && buf[9] == 'A'.code.toByte() &&
            buf[10] == 'V'.code.toByte() && buf[11] == 'E'.code.toByte()
    }

    private fun looksLikeMpegOrMp3Audio(file: File): Boolean {
        val n = minOf(16, file.length().toInt())
        if (n < 4) return false
        val buf = ByteArray(n)
        FileInputStream(file).use { it.read(buf) }
        // ID3
        if (buf[0] == 'I'.code.toByte() && buf[1] == 'D'.code.toByte() && buf[2] == '3'.code.toByte()) {
            return true
        }
        // MPEG audio frame sync (11 set bits)
        for (i in 0 until n - 1) {
            val a = buf[i].toInt() and 0xFF
            val b = buf[i + 1].toInt() and 0xFF
            if (a == 0xFF && (b and 0xE0) == 0xE0) return true
        }
        // JSON / HTML error page
        if (buf[0] == '{'.code.toByte() || buf[0] == '<'.code.toByte()) return false
        return false
    }

    /** dictvoice 的 mp3 或 pronounce/base 的 wav */
    private fun looksLikePlayableYoudaoAudio(file: File): Boolean =
        looksLikeWavAudio(file) || looksLikeMpegOrMp3Audio(file)

    private fun youdaoPronounceSignInput(phrase: String, voiceType: Int, mysticTime: Long): String =
        "appVersion=1&client=web&imei=1&keyfrom=dick&keyid=voiceDictWeb&mid=1&model=1&mysticTime=$mysticTime&network=wifi&product=webdict&rate=4&screen=1&type=$voiceType&vendor=web&word=$phrase&yduuid=abcdefg&key=U3uACNRWSDWdcsKm"

    private fun md5HexUtf8(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        return md.digest(input.toByteArray(StandardCharsets.UTF_8)).joinToString("") { b -> "%02x".format(b) }
    }

    /**
     * 有道网页发音接口 `pronounce/base`（带 sign），对整句比 dictvoice 更稳；返回多为 WAV。
     * 参考 Python: word_speach_download + md5(sign_input)
     */
    private fun fetchYoudaoPronounceBaseToFile(phrase: String, voiceType: Int, outFile: File): Boolean {
        val mysticTime = System.currentTimeMillis()
        val sign = md5HexUtf8(youdaoPronounceSignInput(phrase, voiceType, mysticTime))
        val httpUrl = "https://dict.youdao.com/pronounce/base".toHttpUrl().newBuilder()
            .addQueryParameter("product", "webdict")
            .addQueryParameter("appVersion", "1")
            .addQueryParameter("client", "web")
            .addQueryParameter("mid", "1")
            .addQueryParameter("vendor", "web")
            .addQueryParameter("screen", "1")
            .addQueryParameter("model", "1")
            .addQueryParameter("imei", "1")
            .addQueryParameter("network", "wifi")
            .addQueryParameter("keyfrom", "dick")
            .addQueryParameter("keyid", "voiceDictWeb")
            .addQueryParameter("mysticTime", mysticTime.toString())
            .addQueryParameter("yduuid", "abcdefg")
            .addQueryParameter("le", "")
            .addQueryParameter("phonetic", "")
            .addQueryParameter("rate", "4")
            .addQueryParameter("word", phrase)
            .addQueryParameter("type", voiceType.toString())
            .addQueryParameter("id", "")
            .addQueryParameter("sign", sign)
            .addQueryParameter(
                "pointParam",
                "appVersion,client,imei,keyfrom,keyid,mid,model,mysticTime,network,product,rate,screen,type,vendor,word,yduuid,key",
            )
            .build()
        Log.d(TAG, "play EN (pronounce/base): $httpUrl")
        val request = Request.Builder()
            .url(httpUrl)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            )
            .header("Accept", "*/*")
            .header("Referer", "https://dict.youdao.com/")
            .header("Accept-Language", "en-US,en;q=0.9,zh-CN;q=0.8")
            .get()
            .build()
        return try {
            youdaoHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "youdao pronounce HTTP ${response.code} phrase=[$phrase] type=$voiceType")
                    return false
                }
                val body = response.body ?: run {
                    Log.w(TAG, "youdao pronounce empty body phrase=[$phrase] type=$voiceType")
                    return false
                }
                val ct = response.header("Content-Type").orEmpty()
                if (ct.isNotEmpty() && !ct.contains("mpeg", ignoreCase = true) &&
                    !ct.contains("octet-stream", ignoreCase = true) &&
                    !ct.contains("audio", ignoreCase = true) &&
                    !ct.contains("wav", ignoreCase = true)
                ) {
                    Log.w(TAG, "youdao pronounce bad Content-Type=[$ct] phrase=[$phrase] type=$voiceType")
                    return false
                }
                FileOutputStream(outFile).use { fos ->
                    body.byteStream().use { input -> input.copyTo(fos) }
                    fos.flush()
                    try {
                        fos.fd.sync()
                    } catch (_: Exception) {
                    }
                }
                val len = outFile.length()
                if (len < MIN_YOUDAO_AUDIO_BYTES) {
                    Log.w(TAG, "youdao pronounce file too small len=$len phrase=[$phrase] type=$voiceType")
                    return false
                }
                if (!looksLikePlayableYoudaoAudio(outFile)) {
                    Log.w(TAG, "youdao pronounce not audio len=$len phrase=[$phrase] type=$voiceType")
                    return false
                }
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "youdao pronounce error phrase=[$phrase] type=$voiceType: ${e.message}")
            false
        }
    }

    /** 先 pronounce/base（整句友好），失败再 dictvoice */
    private fun fetchYoudaoAudioAnyToFile(phrase: String, voiceType: Int, outFile: File): Boolean {
        if (fetchYoudaoPronounceBaseToFile(phrase, voiceType, outFile)) return true
        if (outFile.exists()) outFile.delete()
        return fetchYoudaoAudioToFile(phrase, voiceType, outFile)
    }

    /**
     * 优先使用 [filesDir]/youdao_en_tts 下已缓存（pronounce/base 多为 WAV，dictvoice 为 MP3）；首次联网后落盘，后续读盘。
     */
    private fun downloadYoudaoAudioToCache(phrase: String, voiceType: Int): File? {
        val dest = youdaoCachedAudioFile(phrase, voiceType)
        if (isAcceptableYoudaoCacheFile(dest)) {
            Log.d(TAG, "play EN (cache file): ${dest.absolutePath} | phrase=[$phrase] type=$voiceType")
            return dest
        }
        val cacheKey = sha256HexForYoudaoPhrase(phrase, voiceType)
        synchronized(lockForYoudaoCacheKey(cacheKey)) {
            if (isAcceptableYoudaoCacheFile(dest)) {
                Log.d(TAG, "play EN (cache file): ${dest.absolutePath} | phrase=[$phrase] type=$voiceType")
                return dest
            }
            val tmp = File(youdaoTtsCacheDir, "$cacheKey.download.${System.nanoTime()}.part")
            try {
                val attempts = buildYoudaoFetchAttempts(phrase, voiceType)
                var ok = false
                for ((tryPhrase, tryType) in attempts) {
                    if (tmp.exists()) tmp.delete()
                    val fetched = fetchYoudaoAudioAnyToFile(tryPhrase, tryType, tmp)
                    if (fetched && isAcceptableYoudaoCacheFile(tmp)) {
                        if (tryPhrase != phrase || tryType != voiceType) {
                            Log.d(
                                TAG,
                                "youdao retry ok: requested phrase=[$phrase] type=$voiceType -> used [$tryPhrase] type=$tryType",
                            )
                        }
                        ok = true
                        break
                    }
                    if (tmp.exists()) tmp.delete()
                }
                if (!ok) {
                    Log.e(TAG, "youdao all attempts failed for phrase=[$phrase] type=$voiceType (tried ${attempts.size} variants)")
                    return null
                }
                if (dest.exists()) {
                    dest.delete()
                }
                if (!tmp.renameTo(dest)) {
                    tmp.copyTo(dest, overwrite = true)
                    tmp.delete()
                }
                return if (isAcceptableYoudaoCacheFile(dest)) dest else null
            } catch (_: Exception) {
                tmp.delete()
                return null
            }
        }
    }

    /** 同一缓存条目下依次尝试：原词+当前口音 → 小写+当前口音 → 原词+另一口音 → 小写+另一口音 */
    private fun buildYoudaoFetchAttempts(phrase: String, voiceType: Int): List<Pair<String, Int>> {
        val order = LinkedHashSet<Pair<String, Int>>()
        order.add(phrase to voiceType)
        val lower = phrase.lowercase(Locale.US)
        if (lower != phrase) order.add(lower to voiceType)
        val alt = if (voiceType == 1) 2 else 1
        order.add(phrase to alt)
        if (lower != phrase) order.add(lower to alt)
        return order.toList()
    }

    /** 仅网络拉取到指定文件（不写持久缓存名）。 */
    private fun fetchYoudaoAudioToFile(phrase: String, voiceType: Int, outFile: File): Boolean {
        val httpUrl = "https://dict.youdao.com/dictvoice".toHttpUrl().newBuilder()
            .addQueryParameter("audio", phrase)
            .addQueryParameter("type", voiceType.toString())
            .build()
        Log.d(TAG, "play EN (download url): $httpUrl")
        val request = Request.Builder()
            .url(httpUrl)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
            )
            .header("Accept", "*/*")
            .header("Referer", "https://dict.youdao.com/")
            .header("Accept-Language", "en-US,en;q=0.9,zh-CN;q=0.8")
            .get()
            .build()

        return try {
            youdaoHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "youdao dictvoice HTTP ${response.code} phrase=[$phrase] type=$voiceType url=$httpUrl")
                    return false
                }
                val body = response.body ?: run {
                    Log.w(TAG, "youdao dictvoice empty body phrase=[$phrase] type=$voiceType")
                    return false
                }
                val ct = response.header("Content-Type").orEmpty()
                if (ct.isNotEmpty() && !ct.contains("mpeg", ignoreCase = true) &&
                    !ct.contains("octet-stream", ignoreCase = true) &&
                    !ct.contains("audio", ignoreCase = true) &&
                    !ct.contains("wav", ignoreCase = true)
                ) {
                    Log.w(TAG, "youdao dictvoice bad Content-Type=[$ct] phrase=[$phrase] type=$voiceType")
                    return false
                }
                FileOutputStream(outFile).use { fos ->
                    body.byteStream().use { input -> input.copyTo(fos) }
                    fos.flush()
                    try {
                        fos.fd.sync()
                    } catch (_: Exception) {
                    }
                }
                val len = outFile.length()
                if (len < MIN_YOUDAO_AUDIO_BYTES) {
                    Log.w(TAG, "youdao dictvoice file too small len=$len phrase=[$phrase] type=$voiceType")
                    return false
                }
                if (!looksLikePlayableYoudaoAudio(outFile)) {
                    Log.w(TAG, "youdao dictvoice not audio len=$len phrase=[$phrase] type=$voiceType")
                    return false
                }
                true
            }
        } catch (e: Exception) {
            Log.w(TAG, "youdao dictvoice error phrase=[$phrase] type=$voiceType: ${e.message}")
            false
        }
    }

    /**
     * 朗读英文短语（有道）。
     * @param append 为 true 时在当前队列后追加，不中断正在播放的内容。
     * @param allowWordTokenFallback 为 false 时（如家庭英语）失败不拆单词，只拆句/逗号/过长词组。
     */
    fun speak(text: String, append: Boolean = false, allowWordTokenFallback: Boolean = true) {
        val t = text.trim()
        if (t.isEmpty()) return
        enqueueYoudao(t, flush = !append, allowWordTokenFallback = allowWordTokenFallback)
    }

    fun speakDraggingLetter(char: Char) {
        enqueueYoudao(char.uppercaseChar().toString(), flush = true, allowWordTokenFallback = true)
    }

    fun speakLettersThenWord(word: String) {
        val whole = word.lowercase(Locale.US).filter { it.isLetter() }
            .ifEmpty { word.lowercase(Locale.US) }
        enqueueYoudao(whole, flush = true, allowWordTokenFallback = true)
    }

    fun speakAnimal(englishName: String) {
        enqueueYoudao(englishName, flush = true, allowWordTokenFallback = true)
    }

    fun speakVictoryPraise(append: Boolean = false, leadIn: String? = null) {
        val body = listOf(
            "You are so great!",
//            "I am super proud of you!",
        )
        val lines = buildList {
            leadIn?.let { add(it.trim()) }
            addAll(body)
        }.filter { it.isNotEmpty() }
        lines.forEachIndexed { i, line ->
            enqueueYoudao(line, flush = !append && i == 0, allowWordTokenFallback = true)
        }
    }

    /**
     * 停止有道朗读：停止当前播放并清空队列（退出阅读/点读页时调用）。
     */
    fun stopYoudaoPlayback() {
        youdaoPlaySession.incrementAndGet()
        mainHandler.removeCallbacksAndMessages(null)
        try {
            youdaoMediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            youdaoMediaPlayer?.release()
        } catch (_: Exception) {
        }
        youdaoMediaPlayer = null
        youdaoQueue.clear()
        youdaoDraining = false
    }

    fun playSoftClick() {
        try {
            @Suppress("DEPRECATION")
            audioManager.playSoundEffect(AudioManager.FX_KEY_CLICK)
        } catch (_: Exception) {
        }
        try {
            toneMusic.startTone(ToneGenerator.TONE_PROP_BEEP, 45)
        } catch (_: Exception) {
            try {
                toneNotification.startTone(ToneGenerator.TONE_PROP_BEEP, 45)
            } catch (_: Exception) {
            }
        }
    }

    fun playSuccess() {
        try {
            toneMusic.startTone(ToneGenerator.TONE_PROP_ACK, 200)
        } catch (_: Exception) {
            try {
                toneNotification.startTone(ToneGenerator.TONE_PROP_ACK, 200)
            } catch (_: Exception) {
            }
        }
    }

    fun playTryAgain() {
        try {
            toneMusic.startTone(ToneGenerator.TONE_PROP_NACK, 220)
        } catch (_: Exception) {
            try {
                toneNotification.startTone(ToneGenerator.TONE_PROP_NACK, 220)
            } catch (_: Exception) {
            }
        }
    }

    fun release() {
        youdaoPlaySession.incrementAndGet()
        mainHandler.removeCallbacksAndMessages(null)
        youdaoMediaPlayer?.release()
        youdaoMediaPlayer = null
        youdaoQueue.clear()
        youdaoDraining = false
        youdaoHttpExecutor.shutdownNow()
        toneMusic.release()
        toneNotification.release()
    }

    private companion object {
        private const val TAG = "YehaoLeyuanTTS"
        /** 家庭英语等：整句 500 时按词分组，每组最多词数（仍为多词朗读） */
        private const val YOUDAO_MAX_WORDS_PER_CHUNK_FAMILY = 4
        /** 其它场景：先尝试词组再拆单词 */
        private const val YOUDAO_MAX_WORDS_PER_CHUNK_GAME = 5
        /** 禁止拆单词时，超过该长度按空格切成多段再请求（仍是短语，非单字） */
        private const val YOUDAO_PHRASE_SOFT_MAX_CHARS = 52
        /** 过小视为失败页/HTML，避免当音频播 */
        private const val MIN_YOUDAO_AUDIO_BYTES = 64L
        /** 下载写入 cache 后稍等，避免立刻读文件时尚未完全落盘 */
        private const val YOUDAO_FILE_SETTLE_MS = 120L
        /** onPrepared 后再延迟一点再 start，减少首帧无声 */
        private const val YOUDAO_START_AFTER_PREPARED_MS = 100L
    }
}
