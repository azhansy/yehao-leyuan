package com.yehao.leyuan.feature.race

/**
 * 1～9 级：车速与出障碍频率递增（三车道躲避）。
 */
data class RaceDifficulty(
    val level: Int,
    /** 障碍下移速度（屏高/秒，约 0.35～1.05） */
    val scrollSpeed: Float,
    /** 两次生成障碍的最小间隔 ms */
    val spawnMinMs: Long,
    /** 两次生成障碍的最大间隔 ms */
    val spawnMaxMs: Long,
    val summaryZh: String,
)

fun raceDifficultyForLevel(level: Int): RaceDifficulty {
    val lv = level.coerceIn(1, 9)
    val scrollSpeed = 0.35f + (lv - 1) * 0.0875f
    val spawnMinMs = (1950L - (lv - 1) * 145L).coerceAtLeast(420L)
    val spawnMaxMs = spawnMinMs + 380L
    val summaryZh = when (lv) {
        in 1..2 -> "起步慢、障碍稀；局中会越来越快"
        in 3..5 -> "中等起步；局中会越来越快"
        in 6..7 -> "起步就快；局中还会再加速"
        else -> "高难度起步；局中持续加速"
    }
    return RaceDifficulty(lv, scrollSpeed, spawnMinMs, spawnMaxMs, summaryZh)
}
