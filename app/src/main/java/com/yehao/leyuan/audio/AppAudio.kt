package com.yehao.leyuan.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

/** [Voice.GENDER_FEMALE] (API 28+). */
private const val VOICE_GENDER_FEMALE = 500

private fun Voice.safeGender(): Int? =
    try {
        javaClass.getMethod("getGender").invoke(this) as? Int
    } catch (_: ReflectiveOperationException) {
        null
    }

class AppAudio(context: Context) : TextToSpeech.OnInitListener {
    private val appContext = context.applicationContext
    private val ttsPrefs = TtsVoicePrefs(appContext)
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val toneMusic = ToneGenerator(AudioManager.STREAM_MUSIC, 100)
    private val toneNotification = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
    private var ttsReady = false

    private var currentProfile: TtsVoiceProfile = ttsPrefs.getProfile()

    private fun wordReadingLocale(): Locale = when (currentProfile) {
        TtsVoiceProfile.SYSTEM_DEFAULT -> Locale.getDefault()
        TtsVoiceProfile.ENGLISH_UK_FEMALE -> Locale.UK
        TtsVoiceProfile.ENGLISH_US -> Locale.US
    }

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val engine = tts ?: return
        currentProfile = ttsPrefs.getProfile()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
        }
        configureEngineForProfile(engine)
        ttsReady = true
    }

    fun applyVoiceProfile(profile: TtsVoiceProfile) {
        ttsPrefs.setProfile(profile)
        currentProfile = profile
        val engine = tts ?: return
        if (!ttsReady) return
        engine.stop()
        configureEngineForProfile(engine)
    }

    private fun configureEngineForProfile(engine: TextToSpeech) {
        when (currentProfile) {
            TtsVoiceProfile.SYSTEM_DEFAULT -> {
                var ok = engine.setLanguage(Locale.getDefault())
                if (ok < TextToSpeech.LANG_AVAILABLE) {
                    ok = engine.setLanguage(Locale.CHINESE)
                }
                if (ok < TextToSpeech.LANG_AVAILABLE) {
                    ok = engine.setLanguage(Locale.US)
                }
                if (ok < TextToSpeech.LANG_AVAILABLE) {
                    engine.language = Locale.getDefault()
                }
                // 不强制 setVoice，交给系统默认引擎与音色（小米等更兼容）
            }
            TtsVoiceProfile.ENGLISH_UK_FEMALE -> {
                var ok = engine.setLanguage(Locale.UK)
                if (ok < TextToSpeech.LANG_AVAILABLE) {
                    engine.setLanguage(Locale.US)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pickBritishFemaleVoice(engine)
                }
            }
            TtsVoiceProfile.ENGLISH_US -> {
                var ok = engine.setLanguage(Locale.US)
                if (ok < TextToSpeech.LANG_AVAILABLE) {
                    engine.setLanguage(Locale.UK)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    pickAmericanFemaleVoice(engine)
                }
            }
        }
    }

    private fun pickBritishFemaleVoice(engine: TextToSpeech) {
        val voices = engine.voices ?: return
        fun isBritishEnglish(v: Voice): Boolean {
            val l = v.locale
            if (l.language != "en") return false
            return l.country.equals("GB", ignoreCase = true) ||
                l.toLanguageTag().equals("en-gb", ignoreCase = true)
        }
        val gb = voices.filter(::isBritishEnglish)
        if (gb.isEmpty()) return
        val female = gb.filter { it.safeGender() == VOICE_GENDER_FEMALE }
        val byName = gb.filter { v ->
            val n = v.name.lowercase()
            n.contains("female") || n.contains("#female") || n.contains("-f-")
        }
        val preferred = female.maxByOrNull { it.quality }
            ?: byName.maxByOrNull { it.quality }
            ?: gb.maxByOrNull { it.quality }
        preferred?.let { engine.setVoice(it) }
    }

    private fun pickAmericanFemaleVoice(engine: TextToSpeech) {
        val voices = engine.voices ?: return
        fun isUsEnglish(v: Voice): Boolean {
            val l = v.locale
            if (l.language != "en") return false
            return l.country.equals("US", ignoreCase = true) ||
                l.toLanguageTag().equals("en-us", ignoreCase = true)
        }
        val us = voices.filter(::isUsEnglish)
        if (us.isEmpty()) return
        val female = us.filter { it.safeGender() == VOICE_GENDER_FEMALE }
        val byName = us.filter { v ->
            val n = v.name.lowercase()
            n.contains("female") || n.contains("#female") || n.contains("-f-")
        }
        val preferred = female.maxByOrNull { it.quality }
            ?: byName.maxByOrNull { it.quality }
            ?: us.maxByOrNull { it.quality }
        preferred?.let { engine.setVoice(it) }
    }

    fun speak(text: String) {
        if (!ttsReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    fun speakDraggingLetter(char: Char) {
        val letter = char.uppercaseChar().toString()
        if (!ttsReady) {
            playSoftClick()
            return
        }
        tts?.speak(letter, TextToSpeech.QUEUE_FLUSH, null, "drag_$letter")
    }

    fun speakLettersThenWord(word: String) {
        if (!ttsReady) return
        val loc = wordReadingLocale()
        val whole = word.lowercase(loc).filter { it.isLetter() }.ifEmpty { word.lowercase(loc) }
        tts?.speak(whole, TextToSpeech.QUEUE_FLUSH, null, "puzzle_word")
    }

    fun speakAnimal(englishName: String) {
        speak(englishName)
    }

    fun speakVictoryPraise(append: Boolean = false, leadIn: String? = null) {
        if (!ttsReady) return
        val body = listOf(
            "You are so great!",
            "I am super proud of you!",
        )
        val lines = buildList {
            leadIn?.let { add(it) }
            addAll(body)
        }
        lines.forEachIndexed { i, line ->
            val queue = if (!append && i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts?.speak(line, queue, null, "victory_$i")
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
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
        toneMusic.release()
        toneNotification.release()
    }
}
