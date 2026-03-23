package com.yehao.leyuan.feature.letter

/** 拼对一词：词长、剩余时间、等级加成 */
fun scoreForSolvedWord(timeLeftSec: Int, wordLength: Int, level: Int): Int {
    val t = timeLeftSec.coerceAtLeast(0)
    val len = wordLength.coerceAtLeast(1)
    val lv = level.coerceIn(1, 9)
    return len * 15 + t * 5 + lv * 10
}
