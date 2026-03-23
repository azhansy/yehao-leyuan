package com.yehao.leyuan.audio

import android.content.Context
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
    private var tts: TextToSpeech? = TextToSpeech(appContext, this)
    private val tone = ToneGenerator(AudioManager.STREAM_MUSIC, 85)
    private var ttsReady = false

    /** British English locale for TTS and string casing. */
    private val speechLocale: Locale = Locale.UK

    override fun onInit(status: Int) {
        if (status != TextToSpeech.SUCCESS) return
        val engine = tts ?: return
        val langOk = engine.setLanguage(speechLocale)
        if (langOk < TextToSpeech.LANG_AVAILABLE) {
            engine.language = speechLocale
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pickBritishFemaleVoice(engine)
        }
        ttsReady = true
    }

    /**
     * Prefer an en-GB voice marked female (Google / device engines on API 28+).
     * Falls back to any en-GB voice, then leaves [setLanguage] default.
     */
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

    fun speak(text: String) {
        if (!ttsReady) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, text.hashCode().toString())
    }

    /** Single letter while dragging (letter name, e.g. "C"). */
    fun speakDraggingLetter(char: Char) {
        if (!ttsReady) return
        val letter = char.uppercaseChar().toString()
        tts?.speak(letter, TextToSpeech.QUEUE_FLUSH, null, "drag_$letter")
    }

    /**
     * After puzzle complete: read each letter then the whole word, e.g. C → A → T → "cat".
     */
    fun speakLettersThenWord(word: String) {
        if (!ttsReady) return
//        val upper = word.uppercase(speechLocale).filter { it.isLetter() }
//        if (upper.isEmpty()) return
//        tts?.speak(upper.first().toString(), TextToSpeech.QUEUE_FLUSH, null, "puzzle_0")
//        for (i in 1 until upper.length) {
//            tts?.speak(upper[i].toString(), TextToSpeech.QUEUE_ADD, null, "puzzle_$i")
//        }
        val whole = word.lowercase(speechLocale).filter { it.isLetter() }.ifEmpty { word.lowercase(speechLocale) }
        tts?.speak(whole, TextToSpeech.QUEUE_ADD, null, "puzzle_word")
    }

    fun speakAnimal(englishName: String) {
        speak(englishName)
    }

    /**
     * Encouraging English lines after a win.
     * @param append If true, all lines use [QUEUE_ADD] (e.g. after [speakLettersThenWord]).
     * @param leadIn Optional extra English sentence spoken first (still respects [append] for queue mode).
     */
    fun speakVictoryPraise(append: Boolean = false, leadIn: String? = null) {
        if (!ttsReady) return
        val body = listOf(
            "You are so great!",
            "I am super proud of you!",
//
//            "You're awesome!",
//            "Wonderful job!",
//            "That was fantastic!",
//            "You did it!",
//            "You are a superstar!",
//            "Keep going, you are amazing!",
//            "I love how you keep trying!",
//            "You make learning look fun!"
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
        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 40)
    }

    fun playSuccess() {
        tone.startTone(ToneGenerator.TONE_PROP_ACK, 180)
    }

    fun playTryAgain() {
        tone.startTone(ToneGenerator.TONE_PROP_NACK, 220)
    }

    fun release() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsReady = false
        tone.release()
    }
}
