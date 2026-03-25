package com.yehao.leyuan.feature.dino

import kotlin.math.sqrt

/**
 * 1～9：整体偏慢、障碍间隔大；级越高略快、间隔略短。
 * 起跳速度按屏高计算，使顶点高度稳定高于最高障碍（障碍约屏高 4%～6%，为原体量再减半后）。
 */
data class DinoDifficulty(
    val level: Int,
    /** 障碍向左移动速度（像素/秒，与屏宽无关的绝对感由调用方乘系数） */
    val scrollSpeedPx: Float,
    /** 新障碍生成时，与「当前最右障碍」的最小间距（屏宽比例） */
    val spawnGapMin: Float,
    val spawnGapMax: Float,
    /** 重力（像素/秒²） */
    val gravity: Float,
    /** 起跳初速度（像素/秒，向上为负） */
    val jumpVelocity: Float,
    val summaryZh: String,
)

fun dinoDifficultyForLevel(level: Int, screenWidth: Float, screenHeight: Float): DinoDifficulty {
    val lv = level.coerceIn(1, 9)
    val w = screenWidth.coerceAtLeast(320f)
    val h = screenHeight.coerceAtLeast(200f)
    val baseScroll = w * (0.11f + (lv - 1) * 0.012f)
    val gapMin = 0.52f - (lv - 1) * 0.024f
    val gapMax = 0.72f - (lv - 1) * 0.022f
    val gravity = 1900f + (lv - 1) * 65f
    // 与 DinoRunScreen 生成逻辑一致：最高约 6% 屏高（spawn 上限 0.0425+0.0175）
    val tallestObstacle = h * (0.055f + (lv - 1) * 0.001f).coerceAtMost(h * 0.063f)
    val peakMargin = h * 0.032f + 22f
    // 略抬高跳跃顶点，配合更高天空区与完整恐龙显示；系数略大让用户跳得更高一点
    val targetPeak = (tallestObstacle + peakMargin) * 3.48f
    val jumpV = -sqrt(2f * gravity * targetPeak)
    val summary = when (lv) {
        in 1..2 -> "最慢、障碍很远"
        in 3..4 -> "较慢、空隙大"
        in 5..6 -> "中等节奏"
        in 7..8 -> "偏快"
        else -> "最快、最密"
    }
    return DinoDifficulty(lv, baseScroll, gapMin.coerceAtLeast(0.28f), gapMax.coerceAtLeast(0.4f), gravity, jumpV, summary)
}
