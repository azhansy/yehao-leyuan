package com.yehao.leyuan.feature.letter

/**
 * 1～9 级：字数上限递增，奇数级池内字母顺序与单词一致，偶数级起打乱（第 9 级最长最难）。
 */
data class LetterPuzzleDifficulty(
    val level: Int,
    val maxLetters: Int,
    val shufflePool: Boolean,
    /** 每词倒计时秒数（等级越高越紧） */
    val timeLimitSeconds: Int,
    /** 简短说明（设置页） */
    val summaryZh: String
)

fun difficultyForLevel(level: Int): LetterPuzzleDifficulty {
    val lv = level.coerceIn(1, 9)
    val maxLetters = when (lv) {
        1, 2 -> 4
        3, 4 -> 5
        5, 6 -> 6
        7, 8 -> 7
        else -> 8
    }
    val shufflePool = when (lv) {
        1, 3, 5, 7 -> false
        else -> true
    }
    // 以 1 级 10 秒为基准，高级略紧（由原 45→20 档按比例压缩）
    val timeLimitSeconds = when (lv) {
        1 -> 10
        2 -> 9
        3 -> 8
        4 -> 8
        5 -> 7
        6 -> 6
        7 -> 6
        8 -> 5
        else -> 4
    }
    val summaryZh = buildString {
        append("最多${maxLetters}个字母")
        append(if (shufflePool) "，下方字母打乱" else "，下方顺序与单词一致")
        append("，限时${timeLimitSeconds}秒")
    }
    return LetterPuzzleDifficulty(lv, maxLetters, shufflePool, timeLimitSeconds, summaryZh)
}
