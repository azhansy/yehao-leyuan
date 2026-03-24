package com.yehao.leyuan.feature.family

import com.github.promeg.pinyinhelper.Pinyin
import java.util.Locale

private fun Char.isCjk(): Boolean = code in 0x4E00..0x9FFF

/**
 * 家庭英语朗读：模板是英文，名字可能是中文。
 * 含汉字的片段转为**空格分隔的拼音**（首字母大写），便于有道英文发音（type=1/2）稳定朗读。
 * 已是英文、数字、标点则保持原样（仅 trim）。
 */
fun String.toSpeechNameForEnglishTts(): String {
    val t = trim()
    if (t.isEmpty()) return t
    if (t.none { it.isCjk() }) return t

    val sb = StringBuilder()
    var i = 0
    while (i < t.length) {
        val ch = t[i]
        when {
            ch.isWhitespace() -> {
                sb.append(' ')
                i++
            }
            ch.isCjk() -> {
                var j = i
                while (j < t.length && t[j].isCjk()) j++
                val chunk = t.substring(i, j)
                val py = try {
                    Pinyin.toPinyin(chunk, " ")
                } catch (_: Exception) {
                    ""
                }
                if (py.isNotBlank()) {
                    if (sb.isNotEmpty() && sb.last() != ' ') sb.append(' ')
                    for (part in py.trim().split(Regex("\\s+"))) {
                        if (part.isBlank()) continue
                        val word = part.lowercase(Locale.US).replaceFirstChar { c -> c.titlecase(Locale.US) }
                        sb.append(word).append(' ')
                    }
                    if (sb.isNotEmpty() && sb.last() == ' ') sb.deleteCharAt(sb.length - 1)
                }
                i = j
            }
            else -> {
                if (sb.isNotEmpty() && sb.last() != ' ') sb.append(' ')
                sb.append(ch)
                i++
            }
        }
    }
    return sb.toString().replace(Regex("\\s+"), " ").trim()
}
