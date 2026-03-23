package com.yehao.leyuan.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import java.net.URLEncoder
import java.util.ArrayDeque
import java.util.Locale

class AppAudio(context: Context) {
    private val appContext = context.applicationContext
    private val ttsPrefs = TtsVoicePrefs(appContext)
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val toneMusic = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    private val toneNotification = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)

    private var youdaoMediaPlayer: MediaPlayer? = null
    private val youdaoQueue: ArrayDeque<String> = ArrayDeque()
    private var youdaoDraining = false

    private var currentProfile: TtsVoiceProfile = ttsPrefs.getProfile()

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
    private fun enqueueYoudao(text: String, flush: Boolean) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        if (flush) {
            youdaoMediaPlayer?.stop()
            youdaoMediaPlayer?.release()
            youdaoMediaPlayer = null
            youdaoQueue.clear()
            youdaoDraining = false
        }
        youdaoQueue.addLast(trimmed)
        if (!youdaoDraining) playNextYoudao()
    }

    private fun playNextYoudao() {
        if (youdaoQueue.isEmpty()) {
            youdaoDraining = false
            return
        }
        youdaoDraining = true
        val phrase = youdaoQueue.removeFirst()
        try {
            youdaoMediaPlayer?.release()
            youdaoMediaPlayer = null

            val encoded = URLEncoder.encode(phrase, "UTF-8")
            val url = "https://dict.youdao.com/dictvoice?audio=$encoded&type=${youdaoType()}"

            val mp = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build(),
                )
                setDataSource(appContext, Uri.parse(url))
                setOnPreparedListener { it.start() }
                setOnCompletionListener {
                    it.release()
                    if (youdaoMediaPlayer === it) youdaoMediaPlayer = null
                    playNextYoudao()
                }
                setOnErrorListener { player, _, _ ->
                    player.release()
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

    /**
     * 朗读英文短语（有道）。
     * @param append 为 true 时在当前队列后追加，不中断正在播放的内容。
     */
    fun speak(text: String, append: Boolean = false) {
        val t = text.trim()
        if (t.isEmpty()) return
        enqueueYoudao(t, flush = !append)
    }

    fun speakDraggingLetter(char: Char) {
        enqueueYoudao(char.uppercaseChar().toString(), flush = true)
    }

    fun speakLettersThenWord(word: String) {
        val whole = word.lowercase(Locale.US).filter { it.isLetter() }
            .ifEmpty { word.lowercase(Locale.US) }
        enqueueYoudao(whole, flush = true)
    }

    fun speakAnimal(englishName: String) {
        enqueueYoudao(englishName, flush = true)
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
            enqueueYoudao(line, flush = !append && i == 0)
        }
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
        youdaoMediaPlayer?.release()
        youdaoMediaPlayer = null
        youdaoQueue.clear()
        youdaoDraining = false
        toneMusic.release()
        toneNotification.release()
    }
}
